package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;

import java.util.Objects;

public class SesionActual{

    private Usuario usuario;

    // Inicia la sesión con un usuario existente y activo.
    public void iniciar(Usuario usuario){
        Objects.requireNonNull(usuario, "No se puede iniciar sesion sin usuario.");
        if (!usuario.isActivo()){
            throw new SecurityException("No se puede iniciar sesion con un usuario inactivo.");
        }
        if (usuario.getId() <= 0) {
            throw new SecurityException("La cuenta debe estar registrada y autenticada.");
        }
        this.usuario = usuario;
    }

    // Compatibilidad con clientes anteriores: ya no se permite acceso sin clave.
    @Deprecated
    public void iniciarComoCajero(String nombreCajero){
        throw new SecurityException("El cajero debe iniciar sesión con usuario y clave.");
    }

    // Cierra la sesión actual.
    public void cerrar(){
        this.usuario = null;
    }

    // Devuelve el usuario de la sesión o null cuando no hay una sesión activa.
    public Usuario getUsuario(){
        return usuario;
    }

    // Comprueba que la sesión tenga exactamente el rol solicitado.
    public void exigirRol(Rol rol){
        exigirAlgunoDe(rol);
    }

    // Comprueba que la sesión tenga uno de los roles permitidos.
    public void exigirAlgunoDe(Rol... roles){ //los "puntos suspensivos" son varargs que permiten que la clase reciba uno, multiples o hasta 0 roles.
        Objects.requireNonNull(roles, "Faltan los roles permitidos.");
        if (roles.length == 0){
            throw new IllegalArgumentException("Debe indicarse al menos un rol permitido.");
        }

        for (Rol rol : roles){
            Objects.requireNonNull(rol, "Uno de los roles permitidos es null.");
        }

        if (usuario == null){
            throw new IllegalStateException("No hay una sesion iniciada.");
        }
        if (!usuario.isActivo()){
            throw new SecurityException("La sesion pertenece a un usuario inactivo.");
        }

        for (Rol rol : roles) {
            if (usuario.getRol() == rol){
                return;
            }
        }

        throw new SecurityException("La sesion no tiene ninguno de los roles permitidos.");
    }
}
