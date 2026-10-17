package cr.ac.una.est.programacion03proyecto02.model.inventario;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class mermaTest {

    @Test
    void variacionEsNegativa() {
        ingrediente ingredienteUsado = new ingrediente(
                2,
                "Leche",
                10.0,
                unidadMedida.mililitro,
                2.0,
                true
        );
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 17, 13, 0);

        merma resultado = new merma(
                8,
                ingredienteUsado,
                3.5,
                fecha,
                "manager",
                "Producto vencido"
        );

        assertAll(
                () -> assertEquals(-3.5, resultado.variacion()),
                () -> assertEquals(3.5, resultado.getCantidad()),
                () -> assertSame(ingredienteUsado, resultado.getIngrediente()),
                () -> assertEquals("Producto vencido", resultado.getMotivo())
        );
    }
  
}
