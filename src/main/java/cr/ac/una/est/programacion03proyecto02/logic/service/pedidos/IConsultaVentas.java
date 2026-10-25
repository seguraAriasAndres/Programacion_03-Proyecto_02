package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Consultas de venta que ReporteService consume. La implementa PedidoService;
 * solo consulta pedidos con pago PAGADO dentro del rango indicado.
 */
public interface IConsultaVentas {

    List<Pedido> pedidosPagadosEntre(LocalDateTime desde, LocalDateTime hasta) throws SQLException;

    /** productoId -> unidades vendidas. */
    Map<Long, Long> rankingProductos(LocalDateTime desde, LocalDateTime hasta) throws SQLException;
}
