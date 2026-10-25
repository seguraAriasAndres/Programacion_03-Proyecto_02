package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;

import java.util.Objects;

/**
 * Usuario en sesion. La fabrica de controladores reparte una unica instancia
 * compartida; exigirRol es la comprobacion real, ocultar un boton no lo sustituye.
 */
public class SesionActual {

    private Usuario usuario;

    public void iniciar(Usuario usuario) {
        Objects.requireNonNull(usuario, "No se puede iniciar sesion sin usuario.");
        if (!usuario.isActivo()) {
            throw new SecurityException("No se puede iniciar sesion con un usuario inactivo.");
        }
        this.usuario = usuario;
    }

    public void cerrar() {
        this.usuario = null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void exigirRol(Rol rol) {
        Objects.requireNonNull(rol, "Falta el rol exigido.");
        if (usuario == null) {
            throw new IllegalStateException("No hay una sesion iniciada.");
        }
        if (usuario.getRol() != rol) {
            throw new SecurityException("La accion requiere el rol " + rol + ".");
        }
    }
}
