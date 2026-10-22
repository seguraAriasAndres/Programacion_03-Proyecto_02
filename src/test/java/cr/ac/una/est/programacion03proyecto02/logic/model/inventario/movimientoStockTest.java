package cr.ac.una.est.programacion03proyecto02.logic.model.inventario;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class movimientoStockTest {

    private ingrediente crearIngrediente() {
        return new ingrediente(
                1,
                "Leche",
                10.0,
                unidadMedida.MILILITRO,
                2.0,
                true
        );
    }

    private LocalDateTime fechaValida() {
        return LocalDateTime.of(2026, 10, 17, 12, 0);
    }

    @Test
    void rechazaIdNegativoYCantidadInvalida() {
        ingrediente ingredienteValido = crearIngrediente();
        LocalDateTime fecha = fechaValida();

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new reabastecimiento(
                                -1, ingredienteValido, 1.0,
                                fecha, "admin", "Compra"
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new reabastecimiento(
                                1, ingredienteValido, 0.0,
                                fecha, "admin", "Compra"
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new reabastecimiento(
                                1, ingredienteValido, Double.POSITIVE_INFINITY,
                                fecha, "admin", "Compra"
                        )
                )
        );
    }

    @Test
    void rechazaIngredienteYFechaNull() {
        LocalDateTime fecha = fechaValida();
        ingrediente ingredienteValido = crearIngrediente();

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new reabastecimiento(
                                1, null, 1.0, fecha, "admin", "Compra"
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new reabastecimiento(
                                1, ingredienteValido, 1.0,
                                null, "admin", "Compra"
                        )
                )
        );
    }

    @Test
    void rechazaUsuarioYMotivoVacios() {
        ingrediente ingredienteValido = crearIngrediente();
        LocalDateTime fecha = fechaValida();

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new reabastecimiento(
                                1, ingredienteValido, 1.0,
                                fecha, "   ", "Compra"
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new reabastecimiento(
                                1, ingredienteValido, 1.0,
                                fecha, "admin", "   "
                        )
                )
        );
    }
}
