package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public interface IConsultaVentas {
     List<Pedido> pedidosPagados(LocalDateTime desde, LocalDateTime hasta);
     Map<Categoria, Producto> rankingProductos (LocalDateTime desde, LocalDateTime hasta);
}
