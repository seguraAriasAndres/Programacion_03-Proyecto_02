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
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.calculadorReceta;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.PedidoDAO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Objects;

public class PedidoService implements IConsultaVentas{

    private final ICatalogoService catalogo;
    private final iinventarioService inventario;
    final PedidoRepository repositorio;
    private final ConexionDB conexion;

    public PedidoService(ConexionDB conexion, ICatalogoService catalogo, iinventarioService inventario) throws SQLException{
        this(conexion, catalogo, inventario, new PedidoRepository(conexion));
    }

    public PedidoService(ConexionDB conexion, ICatalogoService catalogo, iinventarioService inventario, PedidoRepository repositorio) throws SQLException{
        this.conexion = Objects.requireNonNull(conexion, "Falta el proveedor de conexiones.");
        this.catalogo = Objects.requireNonNull(catalogo, "Falta el servicio de catálogo.");
        this.inventario = Objects.requireNonNull(inventario, "Falta el servicio de inventario.");
        this.repositorio = Objects.requireNonNull(repositorio, "Falta el repositorio de pedidos.");
        this.repositorio.recargarActivos();
    }

    // Crea un borrador y lo publica en la caché después de confirmar.
    public Pedido crear(String cliente, String usuario) throws SQLException{
        if (cliente == null || cliente.isBlank() || cliente.length() > 150){
            throw new IllegalArgumentException("Se requiere un cliente de 1 a 150 caracteres.");
        }

        validarUsuario(usuario);

        Pedido creado = ejecutarTransaccion(c -> new PedidoDAO(c).insertarBorrador(cliente.trim(), usuario.trim()));

        repositorio.actualizarTrasCommit(creado);
        return creado;
    }

    // Agrega una línea solo si el pedido acumulado puede cubrirse con el stock actual.
    public void agregarLinea(long pedidoId, Long productoId, List<Long> opcionesId, int cantidad, String usuario) throws SQLException{
        validarId(pedidoId);
        validarId(productoId);
        validarUsuario(usuario);

        if (cantidad <= 0){
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }

        List<Long> seleccion = validarOpciones(opcionesId);

        modificarPedido(pedidoId, usuario, true, (pedido, c) -> {
            Producto producto = catalogo.obtenerProducto(productoId, c);
            List<OpcionProducto> opciones = seleccionarOpciones(producto, seleccion);

            pedido.agregarLinea(new lineaPedido(0, producto, opciones, cantidad));

            Map<Long, Double> cantidades = calcularIngredientes(pedido, c);

            if (!inventario.hayStockSuficiente(cantidades, c)){
                throw new IllegalStateException("No hay stock suficiente para el pedido completo.");
            }
        });
    }

    // Elimina una línea de un borrador y conserva intacto el pedido confirmado.
    public void quitarLinea(Long idPedido, Long lineaId, String usuario) throws SQLException{
        validarId(lineaId);

        modificarPedido(idPedido, usuario, true, (pedido, c) -> {
            lineaPedido encontrada = null;

            for (lineaPedido linea : pedido.getLineas()){
                if (linea.getId() == lineaId.longValue()){
                    encontrada = linea;
                    break;
                }
            }

            if (encontrada == null){
                throw new IllegalArgumentException("La línea no pertenece al pedido.");
            }

            pedido.eliminarLinea(encontrada);
        });
    }

    // Confirma el pedido y descuenta sus ingredientes en la misma transacción.
    public void confirmar(Long idPedido, String usuario) throws SQLException{
        modificarPedido(idPedido, usuario, false, (pedido, c) -> {
            exigirBorrador(pedido);

            if (pedido.getLineas().isEmpty()){
                throw new IllegalStateException("No se puede confirmar un pedido sin líneas.");
            }

            Map<Long, Double> cantidades = calcularIngredientes(pedido, c);
            pedido.registrarIngredientesDescontados(cantidades);
            pedido.confirmar();

            if (!cantidades.isEmpty()){
                inventario.descontar(cantidades, pedido.getIdPedido(), usuario, c);
            }
        });
    }

    // Cancela un pedido pendiente y devuelve el descuento histórico de ingredientes.
    public void cancelar(Long idPedido, String usuario) throws SQLException{
        modificarPedido(idPedido, usuario, false, (pedido, c) -> {
            pedido.cancelarPedido();

            Map<Long, Double> descontados = pedido.getIngredientesDescontados();

            if (pedido.isConfirmado() && !descontados.isEmpty()){
                inventario.restituir(descontados, pedido.getIdPedido(), usuario, c);
            }
        });
    }

    // Cambia un pedido confirmado al estado de preparación.
    public void iniciarPreparacion(Long idPedido, String usuario) throws SQLException{
        modificarPedido(idPedido, usuario, false, (pedido, c) -> pedido.iniciarPreparacion());
    }

    // Marca como entregado un pedido que está en preparación.
    public void entregar(Long idPedido, String usuario) throws SQLException{
        modificarPedido(idPedido, usuario, false, (pedido, c) -> pedido.entregarPedido());
    }

    // Lista ventas pagadas dentro del rango semiabierto solicitado.
    @Override
    public List<Pedido> pedidosPagadosEntre(LocalDateTime desde, LocalDateTime hasta) throws SQLException{
        validarRango(desde, hasta);
        return ejecutarTransaccion(c -> new PedidoDAO(c).listarPagados(desde, hasta));
    }

    // Devuelve el número de unidades vendidas por ID de producto.
    @Override
    public Map<Long, Long> rankingProductos(LocalDateTime desde, LocalDateTime hasta) throws SQLException{
        validarRango(desde, hasta);
        return ejecutarTransaccion(c -> new PedidoDAO(c).ranking(desde, hasta));
    }

    // Consulta un pedido por ID.
    public Pedido obtener(Long idPedido) throws SQLException{
        validarId(idPedido);
        return ejecutarTransaccion(c -> new PedidoDAO(c).buscarBloqueado(idPedido));
    }

    // Recarga y devuelve los pedidos que siguen activos.
    public List<Pedido> listarActivos() throws SQLException{
        repositorio.recargarActivos();
        return repositorio.listarPedidosActivos();
    }

    // Modifica el pedido bajo bloqueo y actualiza la caché solo después del commit.
    private void modificarPedido(Long idPedido, String usuario, boolean guardarBorrador, CambioPedido cambio) throws SQLException{
        validarId(idPedido);
        validarUsuario(usuario);

        Pedido guardado = ejecutarTransaccion(c -> {
            PedidoDAO pedidos = new PedidoDAO(c);
            Pedido pedido = pedidos.buscarBloqueado(idPedido);

            if (pedido == null){
                throw new NoSuchElementException("No existe el pedido con ID " + idPedido + ".");
            }

            if (guardarBorrador){
                exigirBorrador(pedido);
            }

            cambio.aplicar(pedido, c);

            if (guardarBorrador) {
                return pedidos.guardarBorrador(pedido);
            }

            pedidos.actualizar(pedido);
            return pedido;
        });

        repositorio.actualizarTrasCommit(guardado);
    }

    // Recalcula recetas vigentes y suma cantidades repetidas en todas las líneas.
    private Map<Long, Double> calcularIngredientes(Pedido pedido, Connection conexion) throws SQLException{
        Map<Long, BigDecimal> totales = new TreeMap<>();
        Map<Long, Producto> productosCargados = new HashMap<>();

        for (lineaPedido linea : pedido.getLineas()){
            long productoId = linea.getProductoId();
            Producto producto = productosCargados.get(productoId);

            if (producto == null){
                producto = catalogo.obtenerProducto(productoId, conexion);
                productosCargados.put(productoId, producto);
            }

            List<OpcionProducto> opciones = seleccionarOpciones(producto, linea.getOpcionIds());

            calculadorReceta.acumular(totales, producto, opciones, linea.getCantidad());
        }

        return calculadorReceta.convertir(totales);
    }

    // Busca las opciones elegidas y rechaza opciones ajenas, inactivas o repetidas.
    private List<OpcionProducto> seleccionarOpciones(Producto producto, List<Long> ids){
        List<Long> idsValidados = validarOpciones(ids);
        List<OpcionProducto> seleccionadas = new ArrayList<>();

        for (Long id : idsValidados){
            OpcionProducto encontrada = null;

            for (OpcionProducto opcion : producto.getOpciones()){
                if (opcion.getId() == id && opcion.isActiva()){
                    encontrada = opcion;
                    break;
                }
            }

            if (encontrada == null){
                throw new IllegalArgumentException("La opción no pertenece al producto o está inactiva.");
            }

            seleccionadas.add(encontrada);
        }

        return List.copyOf(seleccionadas);
    }

    // Valida que los IDs de opciones sean positivos, únicos y no null.
    private List<Long> validarOpciones(List<Long> ids){
        if (ids == null) {throw new IllegalArgumentException("Use una lista vacía si no hay opciones.");
        }

        List<Long> copia = new ArrayList<>(ids);
        Set<Long> revisados = new HashSet<>();

        for (Long id : copia){
            validarId(id);

            if (!revisados.add(id)){
                throw new IllegalArgumentException("Las opciones no pueden repetirse.");
            }
        }

        return List.copyOf(copia);
    }

    // Rechaza cambios en pedidos que ya no sean borradores pendientes.
    private void exigirBorrador(Pedido pedido){
        if (pedido.isConfirmado() || pedido.getEstadoPreparacion() != EstadoPreparacion.PENDIENTE || pedido.getEstadoPago() != EstadoPago.PENDIENTE){
            throw new IllegalStateException("Solo se puede editar un borrador pendiente.");
        }
    }

    // Valida que el ID exista y sea positivo.
    private void validarId(Long id){
        if (id == null || id <= 0){
            throw new IllegalArgumentException("El ID debe ser positivo.");
        }
    }

    // Valida el nombre que quedará registrado como responsable de la operación.
    private void validarUsuario(String usuario){
        if (usuario == null || usuario.isBlank() || usuario.length() > 100){
            throw new IllegalArgumentException("Se requiere un usuario de 1 a 100 caracteres.");
        }
    }

    // Valida el rango de fechas semiabierto usado por reportes.
    private void validarRango(LocalDateTime desde, LocalDateTime hasta){
        if (desde == null || hasta == null || !desde.isBefore(hasta)){
            throw new IllegalArgumentException("El rango requiere desde < hasta.");
        }
    }

    // El servicio posee la conexión, el commit y el rollback.
    private <T> T ejecutarTransaccion(ConsultaSQL<T> operacion) throws SQLException{
        try (Connection c = conexion.getConnection()) {
            c.setAutoCommit(false);

            try{
                T resultado = operacion.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException error){
                try {
                    c.rollback();
                } catch (SQLException errorRollback){
                    error.addSuppressed(errorRollback);
                }

                throw error;
            }
        }
    }

    @FunctionalInterface
    private interface ConsultaSQL<T>{
        T ejecutar(Connection c) throws SQLException;
    }

    @FunctionalInterface
    private interface CambioPedido{
        void aplicar(Pedido pedido, Connection c) throws SQLException;
    }
}
