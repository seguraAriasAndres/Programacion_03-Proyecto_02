package cr.ac.una.est.programacion03proyecto02.model.inventario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class recetaItemTest {

    @Test
    void creaRecetaItemValido() {
        recetaItem resultado = new recetaItem(3, 125.5);

        assertAll(
                () -> assertEquals(3, resultado.getIngredienteId()),
                () -> assertEquals(125.5, resultado.getCantidadRequerida())
        );
    }

    @Test
    void permiteIdCeroComoEnElModeloIngrediente() {
        recetaItem resultado = new recetaItem(0, 1.0);

        assertEquals(0, resultado.getIngredienteId());
    }

    @Test
    void rechazaIdNegativoYCantidadInvalida() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new recetaItem(-1, 1.0)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new recetaItem(1, 0.0)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new recetaItem(1, -1.0)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new recetaItem(1, Double.NaN)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new recetaItem(1, Double.POSITIVE_INFINITY)
                )
        );
    }
}
