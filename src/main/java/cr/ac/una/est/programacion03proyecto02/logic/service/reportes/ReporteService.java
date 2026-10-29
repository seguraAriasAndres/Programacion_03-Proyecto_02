package cr.ac.una.est.programacion03proyecto02.logic.service.reportes;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.reportes.ProductoRanking;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.IConsultaVentas;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Ventas pagadas en un rango de fechas. Consume IConsultaVentas (PedidoService)
 * y agrega los totales; no abre conexiones ni ejecuta SQL.
 */
public class ReporteService {
    public record Resumen(BigDecimal ingresos,Map<String,BigDecimal> categorias,List<ProductoRanking> ranking,List<Pedido> historial) { }

    private final IConsultaVentas ventas;

    public ReporteService(IConsultaVentas ventas) {
        this.ventas = Objects.requireNonNull(ventas, "Falta la consulta de ventas.");
    }

    /** Una sola lectura y una sola instantánea para los cuatro componentes del reporte. */
    public Resumen resumen(LocalDateTime desde,LocalDateTime hasta)throws SQLException {
        validarRango(desde,hasta);
        List<Pedido> historial=List.copyOf(ventas.pedidosPagadosEntre(desde,hasta));
        BigDecimal ingresos=BigDecimal.ZERO;
        Map<String,BigDecimal> categorias=new TreeMap<>();
        Map<Long,Long> unidades=new HashMap<>(); Map<Long,String> nombres=new HashMap<>();
        for(Pedido pedido:historial) {
            ingresos=ingresos.add(pedido.calcularTotal());
            for(lineaPedido linea:pedido.getLineas()) {
                categorias.merge(linea.getNombreCategoria(),linea.calcularSubtotal(),BigDecimal::add);
                unidades.merge(linea.getProductoId(),(long)linea.getCantidad(),Math::addExact);
                nombres.put(linea.getProductoId(),linea.getNombreProducto());
            }
        }
        List<ProductoRanking> ranking=new ArrayList<>();
        unidades.forEach((id,cantidad)->ranking.add(new ProductoRanking(id,nombres.get(id),cantidad)));
        ranking.sort(Comparator.comparingLong(ProductoRanking::getUnidades).reversed().thenComparingLong(ProductoRanking::getProductoId));
        return new Resumen(ingresos,java.util.Collections.unmodifiableMap(categorias),List.copyOf(ranking),historial);
    }

    public BigDecimal ingresosBrutos(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        validarRango(desde, hasta);
        BigDecimal total = BigDecimal.ZERO;
        for (Pedido pedido : ventas.pedidosPagadosEntre(desde, hasta)) {
            total = total.add(pedido.calcularTotal());
        }
        return total;
    }

    /** Ingresos pagados agrupados por nombre de categoria, en orden alfabetico. */
    public Map<String, BigDecimal> porCategoria(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        validarRango(desde, hasta);
        Map<String, BigDecimal> porCategoria = new TreeMap<>();
        for (Pedido pedido : ventas.pedidosPagadosEntre(desde, hasta)) {
            for (lineaPedido linea : pedido.getLineas()) {
                porCategoria.merge(linea.getNombreCategoria(),
                        linea.calcularSubtotal(), BigDecimal::add);
            }
        }
        return java.util.Collections.unmodifiableMap(porCategoria);
    }

    /**
     * Mas vendidos primero. El nombre sale del historico de la linea: usa el
     * productoId persistido; si la linea es anterior y no lo conserva, usa el
     * producto en memoria; si tampoco existe, "Producto &lt;id&gt;".
     */
    public List<ProductoRanking> ranking(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        validarRango(desde, hasta);
        Map<Long, Long> unidades = ventas.rankingProductos(desde, hasta);
        Map<Long, String> nombres = new HashMap<>();
        for (Pedido pedido : ventas.pedidosPagadosEntre(desde, hasta)) {
            for (lineaPedido linea : pedido.getLineas()) {
                long productoId = linea.getProductoId() > 0
                        ? linea.getProductoId()
                        : (linea.getProducto() != null ? linea.getProducto().getId() : 0);
                if (productoId > 0) {
                    nombres.put(productoId, linea.getNombreProducto());
                }
            }
        }
        List<ProductoRanking> ranking = new ArrayList<>();
        unidades.forEach((productoId, unidadesVendidas) ->
                ranking.add(new ProductoRanking(productoId,
                        nombres.getOrDefault(productoId, "Producto " + productoId),
                        unidadesVendidas)));
        ranking.sort(Comparator.comparingLong(ProductoRanking::getUnidades).reversed()
                .thenComparingLong(ProductoRanking::getProductoId));
        return List.copyOf(ranking);
    }

    public List<Pedido> historial(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        validarRango(desde, hasta);
        return List.copyOf(ventas.pedidosPagadosEntre(desde, hasta));
    }

    private void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        Objects.requireNonNull(desde, "Falta la fecha inicial.");
        Objects.requireNonNull(hasta, "Falta la fecha final.");
        if (!desde.isBefore(hasta)) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior a la final; hasta es excluyente.");
        }
    }
}
