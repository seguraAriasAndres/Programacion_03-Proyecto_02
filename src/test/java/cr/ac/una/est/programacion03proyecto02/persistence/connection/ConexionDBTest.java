package cr.ac.una.est.programacion03proyecto02.persistence.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/** MySQL real de Aiven; solo lecturas. Se activa con -Dpruebas.mysql=true. */
@EnabledIfSystemProperty(named = "pruebas.mysql", matches = "true")
class ConexionDBTest {

    private final ConexionDB bd = new ConexionDB();

    @Test
    void abreConexionAlSchemaRestauranteYLaPuedeCerrar() throws Exception {
        try (Connection conexion = bd.getConnection()) {
            assertTrue(conexion.isValid(10));
            assertEquals("restaurante", conexion.getCatalog());
            try (Statement sentencia = conexion.createStatement();
                 ResultSet resultado = sentencia.executeQuery("SELECT 1")) {
                assertTrue(resultado.next());
                assertEquals(1, resultado.getInt(1));
            }
        }
    }

    @Test
    void cadaLlamadaDevuelveUnaConexionNueva() throws Exception {
        try (Connection primera = bd.getConnection();
             Connection segunda = bd.getConnection()) {
            assertNotSame(primera, segunda);
        }
    }
}
