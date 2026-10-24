package cr.ac.una.est.programacion03proyecto02.logic.model.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    private static final String HASH = "0123456789abcdef0123456789abcdef:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void construyeYConservaLosValores() {
        Usuario usuario = new Usuario(5, "erving", HASH, Rol.ADMIN, true);

        assertAll(
                () -> assertEquals(5, usuario.getId()),
                () -> assertEquals("erving", usuario.getNombreUsuario()),
                () -> assertEquals(HASH, usuario.getHash()),
                () -> assertEquals(Rol.ADMIN, usuario.getRol()),
                () -> assertTrue(usuario.isActivo()));
    }

    @Test
    void rechazaNombresInvalidos() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Usuario(1, "   ", HASH, Rol.CAJERO, true)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Usuario(1, "x".repeat(101), HASH, Rol.CAJERO, true)),
                () -> assertThrows(NullPointerException.class,
                        () -> new Usuario(1, null, HASH, Rol.CAJERO, true)));
    }

    @Test
    void rechazaHashOIdInvalidos() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Usuario(1, "erving", " ", Rol.CAJERO, true)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Usuario(1, "erving", "x".repeat(256), Rol.CAJERO, true)),
                () -> assertThrows(NullPointerException.class,
                        () -> new Usuario(1, "erving", null, Rol.CAJERO, true)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Usuario(-1, "erving", HASH, Rol.CAJERO, true)));
    }

    @Test
    void rechazaRolInexistente() {
        assertThrows(NullPointerException.class,
                () -> new Usuario(1, "erving", HASH, null, true));
    }
}
