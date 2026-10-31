package cr.ac.una.est.programacion03proyecto02.logic.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Pedido {
    private long idPedido;
    private String nombreCliente;
    private LocalDateTime fechaPedido;
    private boolean confirmado;
    private EstadoPreparacion estadoPreparacion;
    private EstadoPago estadoPago;

    private final List<lineaPedido> lineas;
    private final Map<Long, Double> ingredientesDescontados;
    private Recibo recibo;


    public Pedido (long idPedido, String nombreCliente) {
        this(idPedido, nombreCliente, LocalDateTime.now());
    }

    /** Permite cargar un borrador sin reemplazar su fecha original por la actual. */
    public Pedido(long idPedido, String nombreCliente, LocalDateTime fechaPedido) {
        if (idPedido < 0 || nombreCliente == null || nombreCliente.isBlank()) {
            throw new IllegalArgumentException("El pedido requiere id no negativo y nombre del cliente.");
        }
        this.idPedido = idPedido;
        this.nombreCliente = nombreCliente;
        this.fechaPedido = Objects.requireNonNull(fechaPedido, "La fecha del pedido no puede ser null.");
        this.confirmado = false;
        this.estadoPreparacion = EstadoPreparacion.PENDIENTE;
        this.estadoPago = EstadoPago.PENDIENTE;
        this.lineas = new ArrayList<>();
        this.ingredientesDescontados = new HashMap<>();
        this.recibo = null;
    }

    /** Restaura datos persistidos sin ejecutar transiciones ni operaciones de stock. */
    public static Pedido reconstruir(long id, String nombre, LocalDateTime fecha,
            boolean confirmado, EstadoPreparacion preparacion, EstadoPago pago,
            List<lineaPedido> lineas, Map<Long, Double> descuentos, Recibo recibo) {
        if (id <= 0) {
            throw new IllegalArgumentException("Un pedido persistido debe tener un id positivo.");
        }
        Objects.requireNonNull(preparacion, "Falta el estado de preparacion.");
        Objects.requireNonNull(pago, "Falta el estado de pago.");
        List<lineaPedido> copiaLineas = List.copyOf(lineas);
        Map<Long, Double> copiaDescuentos = validarDescuentos(descuentos);
        if (!confirmado && (preparacion == EstadoPreparacion.EN_PREPARACION
                || preparacion == EstadoPreparacion.ENTREGADO || pago == EstadoPago.PAGADO
                || !copiaDescuentos.isEmpty())
                || confirmado && copiaLineas.isEmpty()
                || preparacion == EstadoPreparacion.CANCELADO && pago == EstadoPago.PAGADO
                || (pago == EstadoPago.PAGADO) != (recibo != null)) {
            throw new IllegalArgumentException("Los estados persistidos del pedido son inconsistentes.");
        }
        var ids = new java.util.HashSet<Long>();
        for (lineaPedido linea : copiaLineas) {
            if (linea.getId() <= 0 || !ids.add(linea.getId())) {
                throw new IllegalArgumentException("Las lineas persistidas requieren ids positivos y unicos.");
            }
        }
        Pedido pedido = new Pedido(id, nombre, fecha);
        pedido.lineas.addAll(copiaLineas);
        pedido.ingredientesDescontados.putAll(copiaDescuentos);
        pedido.confirmado = confirmado;
        pedido.estadoPreparacion = preparacion;
        pedido.estadoPago = pago;
        if (recibo != null) {
            if (recibo.getId() <= 0) {
                throw new IllegalArgumentException("El recibo persistido debe tener un id positivo.");
            }
            pedido.validarRecibo(recibo);
        }
        pedido.recibo = recibo;
        return pedido;
    }

    public long getIdPedido() { return idPedido; }
    public String getNombreCliente() { return nombreCliente; }
    public LocalDateTime getFechaPedido() { return fechaPedido; }
    public boolean isConfirmado() { return confirmado; }
    public EstadoPreparacion getEstadoPreparacion() { return estadoPreparacion; }
    public EstadoPago getEstadoPago() { return estadoPago; }
    public List<lineaPedido> getLineas() { return List.copyOf(lineas); }
    public Map<Long, Double> getIngredientesDescontados() {
        return Map.copyOf(ingredientesDescontados);
    }
    public Recibo getRecibo() { return recibo; }

    /** Registra las cantidades efectivamente descontadas por el servicio en su transaccion. */
    public void registrarIngredientesDescontados(Map<Long, Double> cantidades) {
        exigirBorrador();
        Map<Long, Double> copia = validarDescuentos(cantidades);
        ingredientesDescontados.clear();
        ingredientesDescontados.putAll(copia);
    }

    private static Map<Long, Double> validarDescuentos(Map<Long, Double> cantidades) {
        Map<Long, Double> copia = Map.copyOf(cantidades);
        for (var entrada : copia.entrySet()) {
            if (entrada.getKey() <= 0 || !Double.isFinite(entrada.getValue()) || entrada.getValue() <= 0) {
                throw new IllegalArgumentException("Cada descuento requiere id positivo y cantidad positiva y finita.");
            }
        }
        return copia;
    }

    public void agregarLinea(lineaPedido linea) {
        exigirBorrador();
        Objects.requireNonNull(linea, "La linea no puede ser null.");
        if (linea.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        lineas.add(linea);
    }

    public void eliminarLinea(lineaPedido linea) {
        exigirBorrador();
        lineas.remove(Objects.requireNonNull(linea, "La linea no puede ser null."));
    }

    /** El servicio coordinara el descuento de stock dentro de su transaccion. */
    public void confirmar() {
        exigirBorrador();
        if (lineas.isEmpty()) {
            throw new IllegalStateException("No se puede confirmar un pedido sin lineas.");
        }
        confirmado = true;
    }

    public void iniciarPreparacion() {
        if (!confirmado || estadoPreparacion != EstadoPreparacion.PENDIENTE) {
            throw new IllegalStateException("Solo se puede preparar un pedido confirmado y pendiente.");
        }
        estadoPreparacion = EstadoPreparacion.EN_PREPARACION;
    }

    public void entregarPedido() {
        if (!confirmado || estadoPreparacion != EstadoPreparacion.EN_PREPARACION) {
            throw new IllegalStateException("Solo se puede entregar un pedido en preparacion.");
        }
        estadoPreparacion = EstadoPreparacion.ENTREGADO;
    }

    /** La restitucion de ingredientes corresponde a la transaccion del servicio. */
    public void cancelarPedido() {
        if (estadoPago != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede cancelar un pedido sin pagar.");
        }
        if (estadoPreparacion == EstadoPreparacion.ENTREGADO
                || estadoPreparacion == EstadoPreparacion.CANCELADO) {
            throw new IllegalStateException("No se puede cancelar un pedido entregado o ya cancelado.");
        }
        estadoPreparacion = EstadoPreparacion.CANCELADO;
    }

    /** Registra el estado del modelo; la transaccion JDBC corresponde al servicio. */
    public void registrarCobro(Recibo recibo) {
        if (!confirmado || estadoPreparacion == EstadoPreparacion.CANCELADO
                || estadoPago != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("El pedido debe estar confirmado, sin cancelar y sin pagar.");
        }
        validarRecibo(recibo);
        this.recibo = recibo;
        estadoPago = EstadoPago.PAGADO;
    }

    private void validarRecibo(Recibo recibo) {
        Objects.requireNonNull(recibo, "El recibo no puede ser null.");
        if (recibo.getIdPedido() != idPedido) {
            throw new IllegalArgumentException("El recibo pertenece a otro pedido.");
        }
        BigDecimal total = calcularTotal();
        Pago pago = recibo.getPago();
        if (total.signum() <= 0 || recibo.getTotal().compareTo(total) != 0
                || !coincideDesglose(recibo.getLineas())
                || pago.getMonto() == null || pago.getMonto().compareTo(total) != 0) {
            throw new IllegalArgumentException("El recibo y el pago deben coincidir con el total y desglose del pedido.");
        }
        if (pago.getMetodo() == null || pago.getFecha() == null
                || pago.getUsuario() == null || pago.getUsuario().isBlank()) {
            throw new IllegalArgumentException("El pago debe indicar metodo, fecha y usuario.");
        }
    }

    private boolean coincideDesglose(List<lineaPedido> desglose) {
        if (desglose.size() != lineas.size()) {
            return false;
        }
        for (int i = 0; i < lineas.size(); i++) {
            lineaPedido original = lineas.get(i);
            lineaPedido cobrada = desglose.get(i);
            if (original.getId() != cobrada.getId() || original.getCantidad() != cobrada.getCantidad()
                    || original.getPrecioUnitario().compareTo(cobrada.getPrecioUnitario()) != 0
                    || !Objects.equals(original.getNombreProducto(), cobrada.getNombreProducto())
                    || !Objects.equals(original.getNombreCategoria(), cobrada.getNombreCategoria())
                    || !Objects.equals(original.getDescripcionExtras(), cobrada.getDescripcionExtras())) {
                return false;
            }
        }
        return true;
    }

    private void exigirBorrador() {
        if (confirmado || estadoPreparacion != EstadoPreparacion.PENDIENTE
                || estadoPago != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede editar o confirmar un borrador pendiente.");
        }
    }

    public BigDecimal calcularTotal () {
        BigDecimal total = BigDecimal.ZERO;
        for (lineaPedido linea: this.lineas) {
            total = total.add(linea.calcularSubTotal());
        }
        return total;
    }
}
