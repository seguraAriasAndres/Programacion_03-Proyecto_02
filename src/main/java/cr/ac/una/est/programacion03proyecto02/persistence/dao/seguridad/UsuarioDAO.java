package cr.ac.una.est.programacion03proyecto02.persistence.dao.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.ResumenUsuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    private final ConexionDB bd;

    public UsuarioDAO() {
        this(new ConexionDB());
    }

    public UsuarioDAO(ConexionDB bd) {
        this.bd = Objects.requireNonNull(bd, "Falta el proveedor de conexiones.");
    }

    /** Lista cuentas sin recuperar los hashes de sus contraseñas. */
    public java.util.List<ResumenUsuario> listar() throws SQLException {
        java.util.List<ResumenUsuario> resultado = new java.util.ArrayList<>();
        try (Connection c = bd.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT id,nombre_usuario,rol,activo FROM usuario ORDER BY nombre_usuario");
             ResultSet r = s.executeQuery()) {
            while (r.next()) resultado.add(new ResumenUsuario(r.getLong("id"),r.getString("nombre_usuario"),
                    leerRol(r.getString("nombre_usuario"),r.getString("rol")),r.getBoolean("activo")));
        }
        return java.util.List.copyOf(resultado);
    }

    /** Inserta una cuenta con hash generado por el servicio y devuelve su ID automático. */
    public long crear(String nombre, String hash, Rol rol, boolean activo) throws SQLException {
        validarNombre(nombre);
        Objects.requireNonNull(hash); Objects.requireNonNull(rol);
        try (Connection c = bd.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO usuario(nombre_usuario,hash,rol,activo) VALUES(?,?,?,?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1,nombre); s.setString(2,hash); s.setString(3,rol.name()); s.setBoolean(4,activo);
            if (s.executeUpdate()!=1) throw new SQLException("No se creó la cuenta.");
            try (ResultSet r=s.getGeneratedKeys()) {
                if (!r.next() || r.getLong(1)<=0) throw new SQLException("No se recibió el ID de la cuenta.");
                return r.getLong(1);
            }
        }
    }

    /** Devuelve null si el nombre no existe; rechaza filas corruptas o nombres duplicados. */
    public Usuario buscar(String nombre) throws SQLException {
        validarNombre(nombre);
        try (Connection conexion = bd.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(BUSCAR)) {
            sentencia.setString(1, nombre);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                Usuario usuario;
                try {
                    long id = resultado.getLong("id");
                    if (id <= 0) {
                        throw new IllegalArgumentException("El usuario guardado requiere un ID positivo.");
                    }
                    usuario = new Usuario(
                        id,
                        resultado.getString("nombre_usuario"),
                        resultado.getString("hash"),
                        leerRol(resultado.getString("nombre_usuario"), resultado.getString("rol")),
                        resultado.getBoolean("activo"));
                } catch (IllegalArgumentException | NullPointerException error) {
                    throw new SQLException("El usuario guardado contiene datos invalidos.", error);
                }
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
            sentencia.setObject(3, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
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
