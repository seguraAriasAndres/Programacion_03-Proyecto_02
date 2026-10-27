package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.ICatalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.PedidoDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

// Coordina pedidos, catalogo e inventario
public class PedidoService implements IConsultaVentas {
    private final ICatalogoService catalogo;
    private final iinventarioService inventario;
    final PedidoRepository repositorio;
    private final ConexionDB conexion;

    public PedidoService(ConexionDB conexion, ICatalogoService catalogo,
                         iinventarioService inventario) throws SQLException {
        this(conexion, catalogo, inventario, new PedidoRepository(conexion));
    }

    //Permite compartir el mismo repositorio con CobroService.
    public PedidoService(ConexionDB conexion, ICatalogoService catalogo,
                         iinventarioService inventario, PedidoRepository repositorio) throws SQLException {
        this.conexion = Objects.requireNonNull(conexion, "Falta el proveedor de conexiones.");
        this.catalogo = Objects.requireNonNull(catalogo, "Falta el servicio de catalogo.");
        this.inventario = Objects.requireNonNull(inventario, "Falta el servicio de inventario.");
        this.repositorio = Objects.requireNonNull(repositorio, "Falta el repositorio de pedidos.");
        this.repositorio.recargarActivos();
    }

    public Pedido crear (String cliente, String usuario) throws SQLException {
        // Devuelve el borrador con su ID para que la interfaz pueda seguir editandolo.
        if (cliente == null || cliente.isBlank() || cliente.length() > 150) {
            throw new IllegalArgumentException("Se requiere un cliente de 1 a 150 caracteres.");
        }
        validarUsuario(usuario);
        Pedido creado = ejecutarTransaccion(c -> new PedidoDAO(c).insertarBorrador(cliente, usuario));
        repositorio.actualizarTrasCommit(creado);
        return creado;
    }


    public void agregarLinea(long pedidoId, Long productoId, List<Long> opcionesId, int cantidad, String usuario) throws SQLException {
        validarId(pedidoId);
        validarId(productoId);
        validarUsuario(usuario);

        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");

        List<Long> seleccion = validarOpciones(opcionesId);

        modificarPedido(pedidoId, usuario, true, (pedido, c) -> {
            Producto producto = catalogo.obtenerProducto(productoId, c);
            List<OpcionProducto> opciones = seleccionarOpciones(producto, seleccion);
            pedido.agregarLinea(new lineaPedido(0, producto, opciones, cantidad));
        });
    }

    public void quitarLinea(Long idPedido, Long lineaId, String usuario) throws SQLException {
        validarId(lineaId);
        modificarPedido(idPedido, usuario, true, (pedido, c) -> {
            lineaPedido encontrada = null;
            for (lineaPedido linea : pedido.getLineas()) {
                if (linea.getId() == lineaId.longValue()) {
                    encontrada = linea;
                    break;
                }
            }
            if (encontrada == null) {
                throw new IllegalArgumentException("La linea no pertenece al pedido.");
            }
            pedido.eliminarLinea(encontrada);
        });
    }

    public void confirmar(Long idPedido, String usuario) throws SQLException {
        modificarPedido(idPedido, usuario, false, (pedido, c) -> {
            exigirBorrador(pedido);

            if (pedido.getLineas().isEmpty()) {
                throw new IllegalStateException("No se puede confirmar un pedido sin lineas.");
            }
            // Las lineas recuperadas de SQL conservan IDs
            Map<Long, Double> cantidades = calcularIngredientes(pedido, c);
            pedido.registrarIngredientesDescontados(cantidades);
            pedido.confirmar();
            if (!cantidades.isEmpty()) {
                inventario.descontar(cantidades, pedido.getIdPedido(), usuario, c);
            }
        });
    }

    public void cancelar(Long idPedido, String usuario) throws SQLException {
        modificarPedido(idPedido, usuario, false, (pedido, c) -> {
            // Validar primero para evitar devolver existencias de un pedido ya cancelado o pagado.
            pedido.cancelarPedido();
            Map<Long, Double> descontados = pedido.getIngredientesDescontados();
            if (pedido.isConfirmado() && !descontados.isEmpty()) {
                // Se devuelven las cantidades historicas; no se recalculan recetas actuales.
                inventario.restituir(descontados, pedido.getIdPedido(), usuario, c);
            }
        });
    }

    public void iniciarPreparacion(Long idPedido, String usuario) throws SQLException {
        modificarPedido(idPedido, usuario, false, (pedido, c) -> pedido.iniciarPreparacion());
    }

    public void entregar(Long idPedido, String usuario) throws SQLException {
        modificarPedido(idPedido, usuario, false, (pedido, c) -> pedido.entregarPedido());
    }

    @Override
    public List<Pedido> pedidosPagadosEntre(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        validarRango(desde, hasta);
        return ejecutarTransaccion(c -> new PedidoDAO(c).listarPagados(desde, hasta));
    }

    @Override
    public Map<Long, Long> rankingProductos(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        // ID del producto -> unidades vendidas
        validarRango(desde, hasta);
        return ejecutarTransaccion(c -> new PedidoDAO(c).ranking(desde, hasta));
    }

    public Pedido obtener(Long idPedido) throws SQLException {
        validarId(idPedido);
        return ejecutarTransaccion(c -> new PedidoDAO(c).buscarBloqueado(idPedido));
    }

    public List<Pedido> listarActivos() throws SQLException {
        repositorio.recargarActivos();
        return repositorio.listarPedidosActivos();
    }

    //Funcion auxiliar que trabajara modificando un pedido con ayuda de una funcion lambda que dictara cual es el cambio
    private void modificarPedido(Long idPedido, String usuario, boolean guardarBorrador,
                                 CambioPedido cambio) throws SQLException {
        // Leer, bloquear, modificar y guardar.
        validarId(idPedido);
        validarUsuario(usuario);
        Pedido guardado = ejecutarTransaccion(c -> {
            PedidoDAO pedidos = new PedidoDAO(c);
            Pedido pedido = pedidos.buscarBloqueado(idPedido);
            if (pedido == null) {
                throw new NoSuchElementException("No existe el pedido con ID " + idPedido + ".");
            }
            if (guardarBorrador) exigirBorrador(pedido);
            cambio.aplicar(pedido, c);
            if (guardarBorrador) return pedidos.guardarBorrador(pedido);
            pedidos.actualizar(pedido);
            return pedido;
        });
        repositorio.actualizarTrasCommit(guardado);
    }

    private Map<Long, Double> calcularIngredientes(Pedido pedido, Connection c) throws SQLException {
        Map<Long, BigDecimal> totales = new TreeMap<>();
        Map<Long, Producto> productos = new HashMap<>();
        for (lineaPedido linea : pedido.getLineas()) {
            long productoId = linea.getProductoId();
            Producto producto = productos.get(productoId);
            if (producto == null) {
                producto = catalogo.obtenerProducto(productoId, c);
                productos.put(productoId, producto);
            }
            sumarReceta(totales, producto.getReceta(), linea.getCantidad());
            for (OpcionProducto opcion : seleccionarOpciones(producto, linea.getOpcionIds())) {
                sumarReceta(totales, opcion.getReceta(), linea.getCantidad());
            }
        }
        Map<Long, Double> resultado = new TreeMap<>();
        for (var entrada : totales.entrySet()) {
            BigDecimal cantidad;
            try {
                cantidad = entrada.getValue().setScale(6, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException error) {
                throw new IllegalArgumentException("Las cantidades admiten hasta seis decimales.", error);
            }
            double valor = cantidad.doubleValue();
            if (cantidad.signum() <= 0 || cantidad.precision() > 15
                    || !Double.isFinite(valor) || BigDecimal.valueOf(valor).compareTo(cantidad) != 0) {
                throw new IllegalArgumentException("La cantidad requerida excede el rango admitido.");
            }
            resultado.put(entrada.getKey(), valor);
        }
        return resultado;
    }

    private void sumarReceta(Map<Long, BigDecimal> totales, List<recetaItem> receta, int cantidad) {
        for (recetaItem item : receta) {
            validarId(item.getIngredienteId());
            BigDecimal requerida = BigDecimal.valueOf(item.getCantidadRequerida())
                    .multiply(BigDecimal.valueOf(cantidad));
            totales.merge(item.getIngredienteId(), requerida, BigDecimal::add);
        }
    }

    private List<OpcionProducto> seleccionarOpciones(Producto producto, List<Long> ids) {
        List<OpcionProducto> seleccion = new ArrayList<>();
        for (Long id : validarOpciones(ids)) {
            OpcionProducto encontrada = null;
            for (OpcionProducto opcion : producto.getOpciones()) {
                if (opcion.getId() == id.longValue() && opcion.isActiva()) {
                    encontrada = opcion;
                    break;
                }
            }
            if (encontrada == null) {
                throw new IllegalArgumentException("La opcion no pertenece al producto o esta inactiva.");
            }
            seleccion.add(encontrada);
        }
        return seleccion;
    }

    private List<Long> validarOpciones(List<Long> ids) {
        if (ids == null) throw new IllegalArgumentException("Usa una lista vacia si no hay opciones.");
        List<Long> copia = new ArrayList<>(ids);
        Set<Long> revisados = new HashSet<>();
        for (Long id : copia) {
            validarId(id);
            if (!revisados.add(id)) throw new IllegalArgumentException("Las opciones no pueden repetirse.");
        }
        return copia;
    }

    private void exigirBorrador(Pedido pedido) {
        if (pedido.isConfirmado() || pedido.getEstadoPreparacion() != EstadoPreparacion.PENDIENTE
                || pedido.getEstadoPago() != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede editar un borrador pendiente.");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El ID debe ser positivo.");
    }

    private void validarUsuario(String usuario) {
        if (usuario == null || usuario.isBlank() || usuario.length() > 100) {
            throw new IllegalArgumentException("Se requiere un usuario de 1 a 100 caracteres.");
        }
    }

    private void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || !desde.isBefore(hasta)) {
            throw new IllegalArgumentException("El rango requiere desde < hasta.");
        }
    }

    private <T> T ejecutarTransaccion(ConsultaSQL<T> operacion) throws SQLException {
        // El servicio es propietario de la conexion, el commit y el rollback.
        try (Connection c = conexion.getConnection()) {
            c.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException error) {
                try {
                    c.rollback();
                } catch (SQLException falloRollback) {
                    error.addSuppressed(falloRollback);
                }
                throw error;
            }
        }
    }

    @FunctionalInterface
    private interface ConsultaSQL<T> {
        T ejecutar(Connection c) throws SQLException;
    }

    @FunctionalInterface
    private interface CambioPedido {
        void aplicar(Pedido pedido, Connection c) throws SQLException;
    }
}
