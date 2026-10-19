package cr.ac.una.est.programacion03proyecto02.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.EstadoPreparacion;

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
    private final Map<Integer, Double> ingredientesDescontados;
    private Recibo recibo;


    public Pedido (long idPedido, String nombreCliente) {
        this.idPedido = idPedido;
        this.nombreCliente = nombreCliente;
        this.fechaPedido = LocalDateTime.now();
        this.confirmado = false;
        this.estadoPreparacion = EstadoPreparacion.PENDIENTE;
        this.estadoPago = EstadoPago.PENDIENTE;
        this.lineas = new ArrayList<>();
        this.ingredientesDescontados = new HashMap<>();
        this.recibo = null;
    }

    public long getIdPedido() { return idPedido; }
    public String getNombreCliente() { return nombreCliente; }
    public LocalDateTime getFechaPedido() { return fechaPedido; }
    public boolean isConfirmado() { return confirmado; }
    public EstadoPreparacion getEstadoPreparacion() { return estadoPreparacion; }
    public EstadoPago getEstadoPago() { return estadoPago; }
    public List<lineaPedido> getLineas() { return List.copyOf(lineas); }
    public Map<Integer, Double> getIngredientesDescontados() {
        return Map.copyOf(ingredientesDescontados);
    }
    public Recibo getRecibo() { return recibo; }

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
        if (estadoPreparacion != EstadoPreparacion.PENDIENTE
                || estadoPago != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede cancelar un pedido pendiente y sin pagar.");
        }
        estadoPreparacion = EstadoPreparacion.CANCELADO;
    }

    /** Registra el estado del modelo; la transaccion JDBC corresponde al servicio. */
    public void registrarCobro(Recibo recibo) {
        if (!confirmado || estadoPreparacion == EstadoPreparacion.CANCELADO
                || estadoPago != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("El pedido debe estar confirmado, sin cancelar y sin pagar.");
        }
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
        this.recibo = recibo;
        estadoPago = EstadoPago.PAGADO;
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
