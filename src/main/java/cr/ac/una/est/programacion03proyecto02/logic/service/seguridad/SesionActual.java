package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;

import java.util.Objects;

public class SesionActual{

    private static final String hashCajeroTemporal = "sesion-cajero-temporal-sin-clave";

    private Usuario usuario;

    // Inicia una sesión con un usuario existente y activo.
    public void iniciar(Usuario usuario){
        Objects.requireNonNull(usuario, "No se puede iniciar sesion sin usuario.");
        if (!usuario.isActivo()){
            throw new SecurityException("No se puede iniciar sesion con un usuario inactivo.");
        }
        this.usuario = usuario;
    }

    // Crea una identidad temporal de cajero para usar su nombre en los registros.
    public void iniciarComoCajero(String nombreCajero){
        Objects.requireNonNull(nombreCajero, "Falta el nombre del cajero.");

        String nombreLimpio = nombreCajero.trim();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > 100){
            throw new IllegalArgumentException("El nombre del cajero debe tener entre 1 y 100 caracteres.");
        }

        Usuario cajero = new Usuario(0, nombreLimpio, hashCajeroTemporal, Rol.CAJERO, true);

        iniciar(cajero);
    }

    // Cierra la sesión actual.
    public void cerrar(){
        this.usuario = null;
    }

    // Devuelve el usuario asociado a la sesión o null si no hay sesión.
    public Usuario getUsuario(){
        return usuario;
    }

    // Verifica que exista una sesión y que tenga el rol solicitado.
    public void exigirRol(Rol rol){
        Objects.requireNonNull(rol, "Falta el rol exigido.");
        if (usuario == null){
            throw new IllegalStateException("No hay una sesion iniciada.");
        }
        if (usuario.getRol() != rol){
            throw new SecurityException("La accion requiere el rol " + rol + ".");
        }
    }
    
}
