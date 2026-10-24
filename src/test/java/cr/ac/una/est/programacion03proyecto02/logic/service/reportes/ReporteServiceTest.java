package cr.ac.una.est.programacion03proyecto02.logic.service.reportes;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.reportes.ProductoRanking;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.IConsultaVentas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Agregaciones con una consulta de ventas en memoria; no necesita base de datos. */
class ReporteServiceTest {

    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 10, 1, 12, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2026, 10, 31, 12, 0);

    /** Devuelve siempre los mismos datos, sin importar el rango. */
    private static final class VentasDePrueba implements IConsultaVentas {
        private final List<Pedido> pedidos;
        private final Map<Long, Long> unidades;

        private VentasDePrueba(List<Pedido> pedidos, Map<Long, Long> unidades) {
            this.pedidos = pedidos;
            this.unidades = unidades;
        }

        @Override
        public List<Pedido> pedidosPagadosEntre(LocalDateTime desde, LocalDateTime hasta) {
            return pedidos;
        }

        @Override
        public Map<Long, Long> rankingProductos(LocalDateTime desde, LocalDateTime hasta) {
            return unidades;
        }
    }

    private static lineaPedido historica(long id, String categoria,
            String producto, String precio, int cantidad) {
        return lineaPedido.reconstruirHistorica(id, producto, categoria, "",
                new BigDecimal(precio), cantidad);
    }

    private static Pedido pedidoCon(lineaPedido... lineas) {
        Pedido pedido = new Pedido(900, "Cliente de prueba");
        for (lineaPedido linea : lineas) {
            pedido.agregarLinea(linea);
        }
        return pedido;
    }

    private static ReporteService servicio(List<Pedido> pedidos, Map<Long, Long> unidades) {
        return new ReporteService(new VentasDePrueba(pedidos, unidades));
    }

    @Test
    void ingresosBrutosSumaLosTotalesDeLosPedidosPagados() throws SQLException {
        ReporteService reportes = servicio(List.of(
                pedidoCon(historica(1, "Comida", "Hamburguesa", "2500.00", 2)),
                pedidoCon(historica(2, "Bebidas", "Refresco", "1500.00", 2))),
                Map.of());

        BigDecimal total = reportes.ingresosBrutos(DESDE, HASTA);

        assertEquals(0, total.compareTo(new BigDecimal("8000.00")));
    }

    @Test
    void ingresosBrutosSinPedidosEsCero() throws SQLException {
        BigDecimal total = servicio(List.of(), Map.of()).ingresosBrutos(DESDE, HASTA);

        assertEquals(0, total.compareTo(BigDecimal.ZERO));
    }

    @Test
    void porCategoriaAgrupaLosIngresosPorNombreDeCategoria() throws SQLException {
        ReporteService reportes = servicio(List.of(
                pedidoCon(historica(1, "Comida", "Hamburguesa", "2500.00", 2),
                        historica(2, "Bebidas", "Refresco", "1500.00", 1))),
                Map.of());

        Map<String, BigDecimal> porCategoria = reportes.porCategoria(DESDE, HASTA);

        assertAll(
                () -> assertEquals(List.of("Bebidas", "Comida"),
                        List.copyOf(porCategoria.keySet())),
                () -> assertEquals(0, porCategoria.get("Comida").compareTo(new BigDecimal("5000.00"))),
                () -> assertEquals(0, porCategoria.get("Bebidas").compareTo(new BigDecimal("1500.00"))));
    }

    @Test
    void rankingOrdenaDescendienteYNombresDelHistorico() throws SQLException {
        Producto producto = new Producto(7, "Hamburguesa",
                new BigDecimal("2500.00"), new Categoria(1, "Comida", true), true);
        ReporteService reportes = servicio(
                List.of(
                        pedidoCon(new lineaPedido(10, producto, 2)),
                        pedidoCon(lineaPedido.reconstruirPersistida(11, 8L, List.of(),
                                "Refresco", "Bebidas", "", new BigDecimal("1500.00"), 1))),
                Map.of(7L, 2L, 9L, 5L, 8L, 1L));

        List<ProductoRanking> ranking = reportes.ranking(DESDE, HASTA);

        assertAll(
                () -> assertEquals(3, ranking.size()),
                () -> assertEquals(9, ranking.get(0).getProductoId()),
                () -> assertEquals(5, ranking.get(0).getUnidades()),
                () -> assertEquals("Producto 9", ranking.get(0).getNombreProducto()),
                () -> assertEquals(7, ranking.get(1).getProductoId()),
                () -> assertEquals("Hamburguesa", ranking.get(1).getNombreProducto()),
                () -> assertEquals(8, ranking.get(2).getProductoId()),
                () -> assertEquals("Refresco", ranking.get(2).getNombreProducto()));
    }

    @Test
    void historialDevuelveLosPedidosPagados() throws SQLException {
        List<Pedido> pedidos = List.of(pedidoCon(historica(1, "Comida", "Hamburguesa", "100.00", 1)));

        assertEquals(pedidos,
                servicio(pedidos, Map.of()).historial(DESDE, HASTA));
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        ReporteService reportes = servicio(List.of(), Map.of());

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> reportes.ingresosBrutos(HASTA, DESDE)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> reportes.porCategoria(HASTA, DESDE)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> reportes.ranking(HASTA, DESDE)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> reportes.historial(HASTA, DESDE)));
    }

    @Test
    void rechazaUnRangoVacioIgualQuePedidoDAO() {
        ReporteService reportes = servicio(List.of(), Map.of());
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> reportes.ingresosBrutos(DESDE, DESDE)),
                () -> assertThrows(IllegalArgumentException.class, () -> reportes.porCategoria(DESDE, DESDE)),
                () -> assertThrows(IllegalArgumentException.class, () -> reportes.ranking(DESDE, DESDE)),
                () -> assertThrows(IllegalArgumentException.class, () -> reportes.historial(DESDE, DESDE)));
    }

    @Test
    void rankingConservaAcumuladosLongYDesempataPorId() throws SQLException {
        List<ProductoRanking> ranking = servicio(List.of(), Map.of(9L, 4_000_000_000L, 7L, 4_000_000_000L))
                .ranking(DESDE, HASTA);
        assertEquals(List.of(7L, 9L), ranking.stream().map(ProductoRanking::getProductoId).toList());
        assertEquals(4_000_000_000L, ranking.getFirst().getUnidades());
    }

    @Test
    void resultadosNoPermitenModificarSusColecciones() throws SQLException {
        ReporteService reportes = servicio(List.of(pedidoCon(historica(1, "Comida", "Producto", "100.00", 1))),
                Map.of(7L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> reportes.porCategoria(DESDE, HASTA).clear());
        assertThrows(UnsupportedOperationException.class, () -> reportes.ranking(DESDE, HASTA).clear());
        assertThrows(UnsupportedOperationException.class, () -> reportes.historial(DESDE, HASTA).clear());
    }
}
