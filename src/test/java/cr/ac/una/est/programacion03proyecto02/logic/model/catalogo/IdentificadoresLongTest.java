package cr.ac.una.est.programacion03proyecto02.logic.model.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.unidadMedida;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class IdentificadoresLongTest {
    @Test
    void conservaIdsMayoresQueIntegerMaxValueEnCatalogoYRecetas() {
        long id = 3_000_000_000L;
        Categoria categoria = new Categoria(id, "Comidas", true);
        Producto producto = new Producto(id + 1, "Producto", BigDecimal.TEN, categoria, true);
        OpcionProducto opcion = new OpcionProducto(id + 2, "Extra", TipoOpcion.EXTRA, BigDecimal.ONE, true);
        ingrediente ingrediente = new ingrediente(id + 3, "Arroz", 10, unidadMedida.GRAMO, 2, true);
        recetaItem receta = new recetaItem(ingrediente.getId(), 1);

        assertAll(
                () -> assertEquals(id, categoria.getId()),
                () -> assertEquals(id + 1, producto.getId()),
                () -> assertEquals(id + 2, opcion.getId()),
                () -> assertEquals(id + 3, receta.getIngredienteId())
        );
    }
}
