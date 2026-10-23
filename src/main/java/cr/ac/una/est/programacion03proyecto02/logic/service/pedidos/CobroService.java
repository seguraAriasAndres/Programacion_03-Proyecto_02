package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.PedidoDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Coordina el cobro completo; el SQL pertenece a los DAO y la caché se actualiza tras commit. */
public class CobroService {
    private final PedidoRepository pedidoRepository;
    private final ReciboDAO reciboDAO;
    private final ConexionDB conexionDB;

    public CobroService(PedidoRepository repository, ReciboDAO reciboDAO, ConexionDB conexionDB) {
        this.pedidoRepository = Objects.requireNonNull(repository, "Falta el repositorio de pedidos.");
        this.reciboDAO = Objects.requireNonNull(reciboDAO, "Falta el DAO de recibos.");
        this.conexionDB = Objects.requireNonNull(conexionDB, "Falta el proveedor de conexiones.");
    }

    public Recibo cobrar(long id, MetodoPago metodo, String usuario) throws SQLException {
        // Bloquea el pedido, guarda recibo y estado PAGADO en una transacción y luego actualiza la caché.
        if (id <= 0 || metodo == null || usuario == null || usuario.isBlank() || usuario.length() > 100) {
            throw new IllegalArgumentException("El cobro requiere ID positivo, método y usuario de 1 a 100 caracteres.");
        }
        try (Connection conexion = conexionDB.getConnection()) {
            conexion.setAutoCommit(false);
            Pedido pedido;
            Recibo recibo;
            try {
                PedidoDAO pedidos = new PedidoDAO(conexion);
                pedido = pedidos.buscarBloqueado(id);

                if (pedido == null) {
                    throw new IllegalArgumentException("El pedido no existe.");
                }
                if (!pedido.isConfirmado() || pedido.getEstadoPreparacion() == EstadoPreparacion.CANCELADO
                        || pedido.getEstadoPago() != EstadoPago.PENDIENTE) {
                    throw new IllegalStateException("Solo se puede cobrar un pedido confirmado, sin cancelar y sin pagar.");
                }
                Pago pago = new Pago(metodo, pedido.calcularTotal(),
                        LocalDateTime.now().truncatedTo(ChronoUnit.MICROS), usuario);
                Recibo nuevo = new Recibo(0, id, pago, pedido.getLineas());
                // Validar el cobro antes de escribir
                pedido.registrarCobro(nuevo);
                long reciboId = reciboDAO.insertar(nuevo, conexion);
                recibo = new Recibo(reciboId, id, pago, pedido.getLineas());
                // Reconstruir con el ID
                pedido = Pedido.reconstruir(id, pedido.getNombreCliente(), pedido.getFechaPedido(),
                        pedido.isConfirmado(), pedido.getEstadoPreparacion(), pedido.getEstadoPago(),
                        pedido.getLineas(), pedido.getIngredientesDescontados(), recibo);
                pedidos.actualizar(pedido);
                conexion.commit();

            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.rollback();
                } catch (SQLException falloRollback) {
                    error.addSuppressed(falloRollback);
                }
                throw error;
            }
            // Fuera del catch de la transacción.
            pedidoRepository.actualizarTrasCommit(pedido);
            return recibo;
        }
    }
}
