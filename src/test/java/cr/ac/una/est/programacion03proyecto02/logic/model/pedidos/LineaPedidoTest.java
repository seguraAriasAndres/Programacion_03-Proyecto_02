package cr.ac.una.est.programacion03proyecto02.logic.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineaPedidoTest {
    @Test
    void reconstruyeReferenciasPersistidasSinInventarCatalogoYProtegeSusIds() {
        var ids = new ArrayList<>(List.of(5_000_000_001L));
        lineaPedido linea = lineaPedido.reconstruirPersistida(4_000_000_001L, 6_000_000_001L, ids,
                "Nombre original", "Comidas", "Queso", new BigDecimal("1200.00"), 2);
        ids.clear();
        assertNull(linea.getProducto());
        assertEquals(6_000_000_001L, linea.getProductoId());
        assertEquals(List.of(5_000_000_001L), linea.getOpcionIds());
        assertEquals(new BigDecimal("2400.00"), linea.calcularSubtotal());
        assertThrows(UnsupportedOperationException.class, () -> linea.getOpcionIds().clear());
        assertThrows(IllegalArgumentException.class, () -> linea.conIdPersistido(99));
        assertThrows(IllegalArgumentException.class, () -> lineaPedido.reconstruirPersistida(1, 0, List.of(),
                "Producto", "Comidas", "", BigDecimal.ONE, 1));
        assertThrows(IllegalArgumentException.class, () -> lineaPedido.reconstruirPersistida(1, 2, List.of(3L, 3L),
                "Producto", "Comidas", "", BigDecimal.ONE, 1));
    }

    @Test
    void cambiosEnIdsDelCatalogoNoModificanLasReferenciasDeLaLinea() {
        Producto producto = new Producto(6, "Producto", BigDecimal.TEN, new Categoria(1, "Comidas", true), true);
        OpcionProducto queso = new OpcionProducto(5, "Queso", TipoOpcion.EXTRA, BigDecimal.ONE, true);
        producto.setOpciones(List.of(queso));
        lineaPedido original = new lineaPedido(0, producto, List.of(queso), 1);
        producto.setId(99);
        queso.setId(98);
        lineaPedido persistida = original.conIdPersistido(4_000_000_001L);
        assertEquals(0, original.getId());
        assertEquals(6, persistida.getProductoId());
        assertEquals(List.of(5L), persistida.getOpcionIds());
        assertEquals(new BigDecimal("11"), persistida.getPrecioUnitario());
    }
    @Test
    void recuperaElDesgloseHistoricoSinUnProductoDelCatalogoActual() {
        long id = (long) Integer.MAX_VALUE + 200;
        lineaPedido historica = lineaPedido.reconstruirHistorica(id, "Nombre original",
                "Categoria original", "Con queso", new BigDecimal("1250.50"), 2);

        assertAll(
                () -> assertEquals(id, historica.getId()),
                () -> assertEquals("Nombre original", historica.getNombreProducto()),
                () -> assertEquals("Categoria original", historica.getNombreCategoria()),
                () -> assertEquals("Con queso", historica.getDescripcionExtras()),
                () -> assertEquals(new BigDecimal("2501.00"), historica.calcularSubTotal()),
                () -> assertNull(historica.getProducto()),
                () -> assertTrue(historica.getExtras().isEmpty())
        );
    }

    @Test
    void protegeLaListaDeExtrasYConservaElPrecioOriginal() {
        Producto producto = new Producto(3, "Producto", new BigDecimal("1000"),
                new Categoria(1, "Comidas", true), true);
        OpcionProducto extra = new OpcionProducto(1, "Queso", TipoOpcion.EXTRA,
                new BigDecimal("200"), true);
        producto.setOpciones(new ArrayList<>(List.of(extra)));
        lineaPedido linea = new lineaPedido(8, producto, List.of(extra), 2);
        producto.getOpciones().clear();
        producto.setPrecioBase(new BigDecimal("3000"));
        producto.setNombre("Producto nuevo");
        producto.getCategoria().setNombre("Categoria nueva");
        extra.setNombre("Extra nuevo");
        extra.setPrecioAdicional(new BigDecimal("500"));

        assertAll(
                () -> assertEquals(8, linea.getId()),
                () -> assertEquals("Producto", linea.getNombreProducto()),
                () -> assertEquals(2, linea.getCantidad()),
                () -> assertEquals("Comidas", linea.getNombreCategoria()),
                () -> assertEquals("Queso", linea.getDescripcionExtras()),
                () -> assertEquals(new BigDecimal("1200"), linea.getPrecioUnitario()),
                () -> assertEquals(new BigDecimal("2400"), linea.calcularSubtotal()),
                () -> assertEquals(1, linea.getExtras().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> linea.getExtras().clear())
        );
    }

    @Test
    void cobraSoloLasOpcionesSeleccionadasYNoSeleccionaTodoPorDefecto() {
        Producto producto = new Producto(3, "Producto", new BigDecimal("1000"),
                new Categoria(1, "Comidas", true), true);
        OpcionProducto queso = new OpcionProducto(1, "Queso", TipoOpcion.EXTRA, new BigDecimal("200"), true);
        OpcionProducto papas = new OpcionProducto(2, "Papas", TipoOpcion.ACOMPANAMIENTO, new BigDecimal("500"), true);
        producto.setOpciones(List.of(queso, papas));

        lineaPedido sinOpciones = new lineaPedido(1, producto, 1);
        lineaPedido conQueso = new lineaPedido(2, producto, List.of(queso), 2);
        assertEquals(new BigDecimal("1000"), sinOpciones.getPrecioUnitario());
        assertTrue(sinOpciones.getExtras().isEmpty());
        assertEquals("", sinOpciones.getDescripcionExtras());
        assertEquals(new BigDecimal("2400"), conQueso.calcularSubtotal());
        assertEquals(List.of(queso), conQueso.getExtras());

        assertThrows(IllegalArgumentException.class,
                () -> new lineaPedido(3, producto, List.of(queso, queso), 1));
        OpcionProducto ajena = new OpcionProducto(1, "Queso", TipoOpcion.EXTRA, BigDecimal.ZERO, true);
        assertThrows(IllegalArgumentException.class,
                () -> new lineaPedido(3, producto, List.of(ajena), 1));
        papas.setActiva(false);
        assertThrows(IllegalArgumentException.class,
                () -> new lineaPedido(3, producto, List.of(papas), 1));
        assertThrows(IllegalArgumentException.class, () -> new lineaPedido(3, producto, 0));
    }
}
