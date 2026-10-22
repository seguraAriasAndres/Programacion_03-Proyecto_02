package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SesionActualTest {

    private static final String HASH = "00000000000000000000000000000000:0000000000000000000000000000000000000000000000000000000000000000";

    private final SesionActual sesion = new SesionActual();

    @Test
    void iniciarYCerrarGestionanElUsuario() {
        Usuario usuario = new Usuario(1, "erving", HASH, Rol.ADMIN, true);

        sesion.iniciar(usuario);
        assertSame(usuario, sesion.getUsuario());

        sesion.cerrar();
        assertNull(sesion.getUsuario());
    }

    @Test
    void iniciarSinUsuarioEsUnError() {
        assertThrows(NullPointerException.class, () -> sesion.iniciar(null));
    }

    @Test
    void exigirRolPermiteElRolPropio() {
        sesion.iniciar(new Usuario(1, "cajero", HASH, Rol.CAJERO, true));

        assertDoesNotThrow(() -> sesion.exigirRol(Rol.CAJERO));
    }

    @Test
    void exigirRolRechazaOtroRol() {
        sesion.iniciar(new Usuario(1, "cajero", HASH, Rol.CAJERO, true));

        assertThrows(SecurityException.class, () -> sesion.exigirRol(Rol.ADMIN));
    }

    @Test
    void exigirRolSinSesionEsUnErrorDeEstado() {
        assertThrows(IllegalStateException.class, () -> sesion.exigirRol(Rol.ADMIN));
    }
}
