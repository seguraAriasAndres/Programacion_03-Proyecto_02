package cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoRepositoryTest {
    private final PedidoRepository repositorio = new PedidoRepository(new ConexionDB() {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("No hay conexión disponible para esta prueba.");
        }
    });

    @Test
    void actualizarYConsultarLaCacheNoAbreConexionesNiExponeElEstadoMutable() {
        Pedido original = pedido(3_000_000_001L);
        repositorio.actualizarTrasCommit(original);
        original.confirmar();

        Pedido copia = repositorio.obtenerPedidoPorId(original.getIdPedido());
        assertFalse(copia.isConfirmado());
        copia.confirmar();
        assertFalse(repositorio.obtenerPedidoPorId(original.getIdPedido()).isConfirmado());
        List<Pedido> lista = repositorio.listarPedidosActivos();
        lista.getFirst().cancelarPedido();
        assertEquals(1, repositorio.listarPedidosActivos().size());
        assertNotEquals(lista.getFirst().getEstadoPreparacion(),
                repositorio.obtenerPedidoPorId(original.getIdPedido()).getEstadoPreparacion());
        assertThrows(UnsupportedOperationException.class, lista::clear);
    }

    @Test
    void eliminaCanceladosYEntregadosPagadosPeroConservaLosEntregadosSinPagar() {
        Pedido cancelado = pedido(1);
        repositorio.actualizarTrasCommit(cancelado);
        cancelado.cancelarPedido();
        repositorio.actualizarTrasCommit(cancelado);
        assertNull(repositorio.obtenerPedidoPorId(1));

        Pedido entregado = pedido(2);
        entregado.confirmar();
        entregado.iniciarPreparacion();
        entregado.entregarPedido();
        repositorio.actualizarTrasCommit(entregado);
        assertNotNull(repositorio.obtenerPedidoPorId(2));
        Pago pago = new Pago(MetodoPago.TARJETA, entregado.calcularTotal(), entregado.getFechaPedido(), "cajero");
        entregado.registrarCobro(new Recibo(8_000_000_001L, 2, pago, entregado.getLineas()));
        repositorio.actualizarTrasCommit(entregado);
        assertTrue(repositorio.listarPedidosActivos().isEmpty());
    }

    @Test
    void unaRecargaFallidaConservaLaCacheAnterior() {
        repositorio.actualizarTrasCommit(pedido(3_000_000_001L));
        assertThrows(SQLException.class, repositorio::recargarActivos);
        assertNotNull(repositorio.obtenerPedidoPorId(3_000_000_001L));
    }

    @Test
    void validaDependenciasEIdsYOrdenaPorFechaEId() {
        assertThrows(NullPointerException.class, () -> new PedidoRepository(null));
        assertThrows(IllegalArgumentException.class, () -> repositorio.obtenerPedidoPorId(0));
        assertThrows(NullPointerException.class, () -> repositorio.actualizarTrasCommit(null));
        repositorio.actualizarTrasCommit(pedido(3));
        repositorio.actualizarTrasCommit(pedido(2));
        assertEquals(List.of(2L, 3L), repositorio.listarPedidosActivos().stream().map(Pedido::getIdPedido).toList());
        assertNull(repositorio.obtenerPedidoPorId(99));
    }

    private Pedido pedido(long id) {
        Pedido pedido = new Pedido(id, "Ana", LocalDateTime.of(2026, 10, 22, 12, 0));
        pedido.agregarLinea(lineaPedido.reconstruirPersistida(id + 4_000_000_000L, 6_000_000_001L,
                List.of(), "Producto", "Comidas", "", new BigDecimal("1000.00"), 1));
        return pedido;
    }
}
