package cr.ac.una.est.programacion03proyecto02.persistence.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Unica puerta de acceso a MySQL en Aiven.
 * Cada getConnection devuelve una conexion NUEVA; quien la recibe la cierra
 * con try-with-resources. En una operacion coordinada el servicio abre la
 * transaccion (autoCommit=false), comparte su Connection con los DAOs y es el
 * unico que hace commit, rollback o close.
 * Ejecutar desde la raiz del proyecto para que file:config/... resuelva.
 */
public class ConexionDB {

    private static final Path ARCHIVO = Path.of("config/database.local.properties");

    /**
     * Lee config/database.local.properties, retira la propiedad url y entrega
     * el resto como propiedades JDBC al driver de MySQL.
     */
    public Connection getConnection() throws SQLException {
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
        String url = (String) config.remove("url");
        if (url == null || url.isBlank()) {
            throw new SQLException("Falta la propiedad url en " + ARCHIVO.toAbsolutePath() + ".");
        }
        return DriverManager.getConnection(url, config);
    }
}
