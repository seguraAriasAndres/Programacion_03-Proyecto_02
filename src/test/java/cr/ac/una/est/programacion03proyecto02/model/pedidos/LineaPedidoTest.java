package cr.ac.una.est.programacion03proyecto02.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.model.producto.Categoria;
import cr.ac.una.est.programacion03proyecto02.model.producto.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.model.producto.Producto;
import cr.ac.una.est.programacion03proyecto02.model.producto.enum_class.TipoOpcion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineaPedidoTest {
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
        lineaPedido linea = new lineaPedido(8, producto, 2);
        producto.getOpciones().clear();
        producto.setPrecioBase(new BigDecimal("3000"));

        assertAll(
                () -> assertEquals(8, linea.getId()),
                () -> assertEquals("Producto", linea.getNombreProducto()),
                () -> assertEquals(2, linea.getCantidad()),
                () -> assertEquals(0, linea.getPrecioUnitario().compareTo(new BigDecimal("1000"))),
                () -> assertEquals(1, linea.getExtras().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> linea.getExtras().clear())
        );
    }
}
