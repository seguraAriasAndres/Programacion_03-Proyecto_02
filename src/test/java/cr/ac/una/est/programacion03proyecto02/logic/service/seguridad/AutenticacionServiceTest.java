package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Logica de hash sin base de datos: hashear y coincide son estaticos. */
class AutenticacionServiceTest {

    @Test
    void hashearGeneraSalUnicaEnFormatoSalHash() {
        String primero = AutenticacionService.hashear("ClaveSecreta1");
        String segundo = AutenticacionService.hashear("ClaveSecreta1");

        String[] partes = primero.split(":");
        assertAll(
                () -> assertEquals(2, partes.length),
                () -> assertEquals(32, partes[0].length()),
                () -> assertEquals(64, partes[1].length()),
                () -> assertNotEquals(primero, segundo,
                        "Cada hash debe usar una sal distinta."));
    }

    @Test
    void coincideAceptaLaClaveCorrectaYRechazaLasDemas() {
        String hash = AutenticacionService.hashear("ClaveSecreta1");

        assertAll(
                () -> assertTrue(AutenticacionService.coincide("ClaveSecreta1", hash)),
                () -> assertFalse(AutenticacionService.coincide("claveIncorrecta", hash)),
                () -> assertFalse(AutenticacionService.coincide("ClaveSecreta1 ", hash)));
    }

    @Test
    void rechazaCualquierHashCorruptoSinLanzarError() {
        assertAll(
                () -> assertFalse(AutenticacionService.coincide("x", "abc")),
                () -> assertFalse(AutenticacionService.coincide("x", "")),
                () -> assertFalse(AutenticacionService.coincide("x", "a:b:c")),
                () -> assertFalse(AutenticacionService.coincide("x", "zz:zz")),
                () -> assertFalse(AutenticacionService.coincide("x", "00:00")),
                () -> assertFalse(AutenticacionService.coincide(null, "00:00")),
                () -> assertFalse(AutenticacionService.coincide("x", null)));
    }

    @Test
    void hashearRechazaClaveNull() {
        assertThrows(NullPointerException.class, () -> AutenticacionService.hashear(null));
    }
}
