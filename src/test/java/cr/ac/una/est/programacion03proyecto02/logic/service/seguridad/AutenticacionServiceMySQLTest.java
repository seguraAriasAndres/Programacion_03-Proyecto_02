package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.seguridad.UsuarioDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Login completo en tablas TEMPORARY de una sesion. El proveedor inyectado permite
 * compartirlas entre operaciones del DAO. No inserta ni elimina filas permanentes.
 */
@EnabledIfSystemProperty(named = "pruebas.mysql", matches = "true")
class AutenticacionServiceMySQLTest {

    private Connection conexion;
    private AutenticacionService servicio;
    private UsuarioDAO dao;

    @BeforeEach
    void abrirSesionTemporal() throws Exception {
        conexion = new ConexionDB().getConnection();
        try (var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE usuario (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        nombre_usuario VARCHAR(100) NOT NULL UNIQUE, hash VARCHAR(255) NOT NULL,
                        rol VARCHAR(20) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE
                    ) ENGINE=InnoDB AUTO_INCREMENT=3000000000
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE auditoria_login (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nombre_usuario VARCHAR(100) NOT NULL,
                        exitoso BOOLEAN NOT NULL, fecha DATETIME(6) NOT NULL
                    ) ENGINE=InnoDB
                    """);
        }
        dao = new UsuarioDAO(new ConexionDB() {
            @Override
            public Connection getConnection() {
                // El teardown cierra la sesion; cada operacion del DAO sigue cerrando sus sentencias.
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class}, (proxy, metodo, argumentos) -> {
                            if (metodo.getName().equals("close")) return null;
                            try {
                                return metodo.invoke(conexion, argumentos);
                            } catch (InvocationTargetException error) {
                                throw error.getCause();
                            }
                        });
            }
        });
        servicio = new AutenticacionService(dao);
    }

    @AfterEach
    void cerrarSesionTemporal() throws SQLException {
        if (conexion != null) {
            conexion.close();
        }
    }

    private void crearUsuario(String nombre, String hash, Rol rol, boolean activo) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "INSERT INTO usuario (nombre_usuario, hash, rol, activo) VALUES (?, ?, ?, ?)")) {
            sentencia.setString(1, nombre);
            sentencia.setString(2, hash);
            sentencia.setString(3, rol.name());
            sentencia.setBoolean(4, activo);
            sentencia.executeUpdate();
        }
    }

    /** exitoso del ultimo intento registrado por el servicio; null si no hay intentos. */
    private Boolean ultimoIntento(String nombre) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT exitoso FROM auditoria_login WHERE nombre_usuario = ? ORDER BY id DESC")) {
            sentencia.setString(1, nombre);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getBoolean("exitoso") : null;
            }
        }
    }

    @Test
    void loginExitosoDevuelveElUsuarioYRegistraElIntento() throws SQLException {
        crearUsuario("zztest_admin", AutenticacionService.hashear("ClaveSecreta1"), Rol.ADMIN, true);

        Usuario usuario = servicio.autenticar("zztest_admin", "ClaveSecreta1");

        assertAll(
                () -> assertNotNull(usuario),
                () -> assertEquals("zztest_admin", usuario.getNombreUsuario()),
                () -> assertEquals(Rol.ADMIN, usuario.getRol()),
                () -> assertTrue(usuario.getId() > Integer.MAX_VALUE),
                () -> assertEquals(Boolean.TRUE, ultimoIntento("zztest_admin")));
    }

    @Test
    void claveIncorrectaDevuelveNullYQuedaEnLaAuditoria() throws SQLException {
        crearUsuario("zztest_cajero", AutenticacionService.hashear("ClaveSecreta1"), Rol.CAJERO, true);

        assertNull(servicio.autenticar("zztest_cajero", "otraClave"));
        assertEquals(Boolean.FALSE, ultimoIntento("zztest_cajero"));
    }

    @Test
    void usuarioInactivoNoPuedeEntrar() throws SQLException {
        crearUsuario("zztest_inactivo", AutenticacionService.hashear("ClaveSecreta1"), Rol.CAJERO, false);

        assertNull(servicio.autenticar("zztest_inactivo", "ClaveSecreta1"));
        assertEquals(Boolean.FALSE, ultimoIntento("zztest_inactivo"));
    }

    @Test
    void usuarioDesconocidoNoPuedeEntrarYIgualDejaRastro() throws SQLException {
        assertNull(servicio.autenticar("zztest_nobody", "ClaveSecreta1"));
        assertEquals(Boolean.FALSE, ultimoIntento("zztest_nobody"));
    }

    @Test
    void lasColumnasPermanentesCoincidenConElDAO() throws SQLException {
        assertEquals(List.of("id", "nombre_usuario", "hash", "rol", "activo"),
                columnas("usuario"));
        assertEquals(List.of("id", "nombre_usuario", "exitoso", "fecha"),
                columnas("auditoria_login"));
    }

    @Test
    void datosCorruptosDelUsuarioSeReportanComoErroresSQL() throws SQLException {
        crearUsuario("zztest_corrupto", "", Rol.CAJERO, true);
        assertThrows(SQLException.class, () -> dao.buscar("zztest_corrupto"));
        try (var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("UPDATE usuario SET rol = 'DESCONOCIDO'");
        }
        assertThrows(SQLException.class, () -> dao.buscar("zztest_corrupto"));
    }

    private List<String> columnas(String tabla) throws SQLException {
        List<String> nombres = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement("""
                SELECT COLUMN_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION
                """)) {
            sentencia.setString(1, "restaurante");
            sentencia.setString(2, tabla);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    nombres.add(resultado.getString(1));
                }
            }
        }
        return nombres;
    }
}
