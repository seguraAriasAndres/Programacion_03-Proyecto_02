package cr.ac.una.est.programacion03proyecto02.persistence.dao.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Persistencia de usuarios y de la auditoria de login.
 * Estas operaciones son independientes entre si: este DAO abre y cierra su
 * propia conexion porque no participan en transacciones coordinadas.
 */
public class UsuarioDAO {
    private static final String BUSCAR = """
            SELECT id, nombre_usuario, hash, rol, activo
            FROM usuario WHERE nombre_usuario = ?
            """;
    private static final String REGISTRAR_INTENTO = """
            INSERT INTO auditoria_login (nombre_usuario, exitoso, fecha)
            VALUES (?, ?, ?)
            """;

    private final ConexionDB bd = new ConexionDB();

    /** Devuelve null si el nombre no existe o si hay mas de una fila (deberia ser UNIQUE). */
    public Usuario buscar(String nombre) throws SQLException {
        validarNombre(nombre);
        try (Connection conexion = bd.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(BUSCAR)) {
            sentencia.setString(1, nombre);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                Usuario usuario = new Usuario(
                        resultado.getLong("id"),
                        resultado.getString("nombre_usuario"),
                        resultado.getString("hash"),
                        leerRol(resultado.getString("nombre_usuario"), resultado.getString("rol")),
                        resultado.getBoolean("activo"));
                if (resultado.next()) {
                    throw new SQLException("Hay mas de un usuario llamado "
                            + usuario.getNombreUsuario() + "; falta UNIQUE(nombre_usuario).");
                }
                return usuario;
            }
        }
    }

    /** Deja constancia del intento, tambien cuando el usuario no existe. */
    public void registrarIntento(String nombre, boolean exitoso) throws SQLException {
        validarNombre(nombre);
        try (Connection conexion = bd.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(REGISTRAR_INTENTO)) {
            sentencia.setString(1, nombre);
            sentencia.setBoolean(2, exitoso);
            sentencia.setObject(3, LocalDateTime.now());
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No se registro el intento de login.");
            }
        }
    }

    private void validarNombre(String nombre) {
        Objects.requireNonNull(nombre, "Falta el nombre de usuario.");
        if (nombre.isBlank() || nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 1 y 100 caracteres.");
        }
    }

    private Rol leerRol(String nombre, String rol) throws SQLException {
        try {
            return Rol.valueOf(rol);
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new SQLException("El usuario " + nombre + " tiene un rol invalido: " + rol, error);
        }
    }
}
