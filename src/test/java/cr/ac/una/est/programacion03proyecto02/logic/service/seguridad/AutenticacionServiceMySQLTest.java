package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL real de Aiven; el login completo abre sus propias conexiones porque
 * UsuarioDAO las gestiona (diseno del UML), asi que las pruebas escriben en las
 * tablas reales unicamente filas zztest_*, borradas antes y despues de cada test.
 */
@EnabledIfSystemProperty(named = "pruebas.mysql", matches = "true")
class AutenticacionServiceMySQLTest {

    private Connection conexion;
    private final AutenticacionService servicio = new AutenticacionService();

    @BeforeEach
    void abrirSesionYLimpiar() throws Exception {
        Properties config = new Properties();
        try (var archivo = Files.newInputStream(Path.of("config/database.local.properties"))) {
            config.load(archivo);
        }
        String url = (String) config.remove("url");
        conexion = DriverManager.getConnection(url, config);
        limpiar();
    }

    @AfterEach
    void cerrarSesionYLimpiar() throws SQLException {
        if (conexion != null) {
            try {
                limpiar();
            } finally {
                conexion.close();
            }
        }
    }

    private void limpiar() throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "DELETE FROM auditoria_login WHERE nombre_usuario LIKE 'zztest_%'")) {
            sentencia.executeUpdate();
        }
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "DELETE FROM usuario WHERE nombre_usuario LIKE 'zztest_%'")) {
            sentencia.executeUpdate();
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
                () -> assertTrue(usuario.getId() > 0),
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
