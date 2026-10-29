package cr.ac.una.est.programacion03proyecto02.persistence.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Unica puerta de acceso a MySQL en Aiven.
 * Cada getConnection presta una conexión exclusiva del pool; close la devuelve
 * y try-with-resources sigue siendo obligatorio. En una operacion coordinada el servicio abre la
 * transaccion (autoCommit=false), comparte su Connection con los DAOs y es el
 * unico que hace commit, rollback o close.
 * Ejecutar desde la raiz del proyecto para que file:config/... resuelva.
 */
public class ConexionDB {

    private static final Path ARCHIVO = Path.of("config/database.local.properties");
    private static volatile HikariDataSource pool;

    /**
     * Lee config/database.local.properties, retira la propiedad url y entrega
     * el resto como propiedades JDBC al driver de MySQL.
     */
    public Connection getConnection() throws SQLException {
        if (Boolean.parseBoolean(System.getProperty("restaurante.pool", "true"))) {
            return obtenerPool().getConnection();
        }
        Properties config = leerConfiguracion();
        String url = (String) config.remove("url");
        return DriverManager.getConnection(url, config);
    }

    private static Properties leerConfiguracion() throws SQLException {
        Properties config = new Properties();
        try (InputStream archivo = Files.newInputStream(ARCHIVO)) {
            config.load(archivo);
        } catch (IOException error) {
            throw new SQLException(
                    "No se pudo leer " + ARCHIVO.toAbsolutePath()
                            + ". Copia config/database.properties.example a ese archivo"
                            + " y agrega tus credenciales de Aiven.",
                    error);
        }
        String url = config.getProperty("url");
        if (url == null || url.isBlank()) {
            throw new SQLException("Falta la propiedad url en " + ARCHIVO.toAbsolutePath() + ".");
        }
        return config;
    }

    private static HikariDataSource obtenerPool() throws SQLException {
        HikariDataSource actual = pool;
        if (actual == null) {
            synchronized (ConexionDB.class) {
                actual = pool;
                if (actual == null) {
                    Properties propiedades = leerConfiguracion();
                    HikariConfig configuracion = new HikariConfig();
                    configuracion.setJdbcUrl((String) propiedades.remove("url"));
                    configuracion.setDataSourceProperties(propiedades);
                    configuracion.setMaximumPoolSize(3);
                    configuracion.setMinimumIdle(0);
                    configuracion.setConnectionTimeout(12000);
                    configuracion.setValidationTimeout(3000);
                    configuracion.setIdleTimeout(60000);
                    configuracion.setMaxLifetime(600000);
                    configuracion.setInitializationFailTimeout(-1);
                    configuracion.setPoolName("Restaurante");
                    pool = actual = new HikariDataSource(configuracion);
                }
            }
        }
        return actual;
    }

    /** Cierra las conexiones físicas al terminar la aplicación. */
    public static synchronized void cerrarPool() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }
}
