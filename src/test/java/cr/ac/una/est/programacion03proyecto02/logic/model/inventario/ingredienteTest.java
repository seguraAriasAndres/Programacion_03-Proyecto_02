package cr.ac.una.est.programacion03proyecto02.logic.model.inventario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ingredienteTest {

    @Test
    void creaIngredienteValidoYRecortaElNombre() {
        ingrediente resultado = new ingrediente(
                1,
                " Leche ",
                12.5,
                unidadMedida.MILILITRO,
                2.0,
                true
        );

        assertAll(
                () -> assertEquals(1, resultado.getId()),
                () -> assertEquals("Leche", resultado.getNombre()),
                () -> assertEquals(12.5, resultado.getCantidadDisponible()),
                () -> assertEquals(unidadMedida.MILILITRO, resultado.getUnidadMedida()),
                () -> assertEquals(2.0, resultado.getStockMinimo()),
                () -> assertTrue(resultado.isActivo())
        );
    }

    @Test
    void estaBajoMinimoCuandoLaCantidadEsMenorOIgualAlMinimo() {
        ingrediente bajoMinimo = new ingrediente(
                1, "Arroz", 4.0, unidadMedida.GRAMO, 5.0, true
        );
        ingrediente enMinimo = new ingrediente(
                2, "Frijoles", 5.0, unidadMedida.GRAMO, 5.0, true
        );
        ingrediente sobreMinimo = new ingrediente(
                3, "Azúcar", 6.0, unidadMedida.GRAMO, 5.0, true
        );

        assertTrue(bajoMinimo.estaBajoMinimo());
        assertTrue(enMinimo.estaBajoMinimo());
        assertFalse(sobreMinimo.estaBajoMinimo());
    }

    @Test
    void rechazaDatosInvalidos() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ingrediente(-1, "Leche", 1.0,
                                unidadMedida.MILILITRO, 0.0, true)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ingrediente(1, "   ", 1.0,
                                unidadMedida.MILILITRO, 0.0, true)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ingrediente(1, "Leche", Double.NaN,
                                unidadMedida.MILILITRO, 0.0, true)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ingrediente(1, "Leche", 1.0,
                                unidadMedida.MILILITRO, -1.0, true)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ingrediente(1, "Leche", 1.0,
                                null, 0.0, true)
                )
        );
    }
}
