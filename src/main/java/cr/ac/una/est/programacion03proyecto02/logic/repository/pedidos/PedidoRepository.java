package cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.PedidoDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;

public class PedidoRepository {
    private final Map<Long, Pedido> pedidosActivos = new HashMap<>();
    private final ConexionDB conexionDB;

    public PedidoRepository(ConexionDB conexionDB) {
        // Conserva el proveedor para abrir una conexión nueva solo al recargar desde SQL.
        this.conexionDB = Objects.requireNonNull(conexionDB, "Falta el proveedor de conexiones.");
    }

    public synchronized void recargarActivos() throws SQLException {
        // Recarga la caché completa tras confirmar la lectura y conserva la anterior si ocurre un error.
        Map<Long, Pedido> nuevosPedidos = new HashMap<>();
        try (Connection conexion = conexionDB.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                for (Pedido pedido : new PedidoDAO(conexion).listarActivos()) {
                    Pedido copia = copiar(pedido);
                    if (nuevosPedidos.putIfAbsent(copia.getIdPedido(), copia) != null) {
                        throw new SQLException("La consulta devolvió un pedido activo repetido.");
                    }
                }
                conexion.commit();
            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.rollback();
                } catch (SQLException falloRollback) {
                    error.addSuppressed(falloRollback);
                }
                throw error;
            }
        }
        pedidosActivos.clear();
        pedidosActivos.putAll(nuevosPedidos);
    }

    public synchronized Pedido obtenerPedidoPorId(long id) {
        // Devuelve una copia del pedido en caché o null si no está entre los activos.
        if (id <= 0){
            throw new IllegalArgumentException("Id invalido");
        }
        Pedido pedido = pedidosActivos.get(id);
        return pedido == null ? null : copiar(pedido);
    }

    public synchronized void actualizarTrasCommit(Pedido pedido) {
        // Actualiza únicamente la caché con una copia del pedido que el servicio ya confirmó en SQL.
        Pedido copia = copiar(pedido);
        if (copia.getEstadoPreparacion() == EstadoPreparacion.CANCELADO
                || copia.getEstadoPreparacion() == EstadoPreparacion.ENTREGADO
                && copia.getEstadoPago() == EstadoPago.PAGADO) {
            pedidosActivos.remove(copia.getIdPedido());
        } else {
            pedidosActivos.put(copia.getIdPedido(), copia);
        }
    }

    public synchronized List<Pedido> listarPedidosActivos() {
        // Devuelve copias de los pedidos en memoria, ordenadas por fecha e ID, sin consultar SQL.
        return pedidosActivos.values().stream()
                .sorted(Comparator.comparing(Pedido::getFechaPedido).thenComparingLong(Pedido::getIdPedido))
                .map(this::copiar)
                .toList();
    }

    private Pedido copiar(Pedido pedido) {
        // Aísla el estado mutable del pedido para que una edición externa no modifique la caché.
        Objects.requireNonNull(pedido, "El pedido no puede ser null.");
        return Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), pedido.getFechaPedido(),
                pedido.isConfirmado(), pedido.getEstadoPreparacion(), pedido.getEstadoPago(),
                pedido.getLineas(), pedido.getIngredientesDescontados(), pedido.getRecibo());
    }
}
