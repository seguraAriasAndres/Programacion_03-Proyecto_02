package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;

import java.sql.SQLException;

public interface IAutenticacionService {

    /** Devuelve el usuario activo si la clave coincide; null si no coincide. */
    Usuario autenticar(String nombre, String clave) throws SQLException;
}
