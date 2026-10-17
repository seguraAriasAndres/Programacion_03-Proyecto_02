package cr.ac.una.est.programacion03proyecto02.model.inventario;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class reabastecimientoTest {

    @Test
    void variacionEsPositivaYConservaSusDatos() {
        ingrediente ingredienteUsado = new ingrediente(
                1,
                "Arroz",
                10.0,
                unidadMedida.gramo,
                2.0,
                true
        );
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 17, 12, 0);

        reabastecimiento resultado = new reabastecimiento(
                7,
                ingredienteUsado,
                25.0,
                fecha,
                "admin",
                "Compra semanal"
        );

        assertAll(
                () -> assertEquals(25.0, resultado.variacion()),
                () -> assertEquals(7, resultado.getId()),
                () -> assertSame(ingredienteUsado, resultado.getIngrediente()),
                () -> assertEquals(25.0, resultado.getCantidad()),
                () -> assertEquals(fecha, resultado.getFecha()),
                () -> assertEquals("admin", resultado.getUsuario()),
                () -> assertEquals("Compra semanal", resultado.getMotivo())
        );
    }

}
