package cr.ac.una.est.programacion03proyecto02.logic.model.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;

/** Datos administrativos de una cuenta; nunca incluye su clave ni su hash. */
public record ResumenUsuario(long id, String nombreUsuario, Rol rol, boolean activo) { }
