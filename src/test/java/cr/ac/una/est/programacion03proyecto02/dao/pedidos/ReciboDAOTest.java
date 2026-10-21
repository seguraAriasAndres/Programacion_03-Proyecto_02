package cr.ac.una.est.programacion03proyecto02.dao.pedidos;

import cr.ac.una.est.programacion03proyecto02.model.pedidos.Pago;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.lineaPedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** MySQL real; todas las escrituras se limitan a tablas TEMPORARY de esta sesion. */
@EnabledIfSystemProperty(named = "pruebas.mysql", matches = "true")
class ReciboDAOTest {
    private static final long PEDIDO_ID = 3_000_000_001L;
    private static final long LINEA_ID = 3_000_000_002L;
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 21, 15, 43, 12, 123456000);
    private Connection conexion;
    private final ReciboDAO dao = new ReciboDAO();

    @BeforeEach
    void abrirSesionConTablasTemporales() throws Exception {
        Properties config = new Properties();
        try (var archivo = Files.newInputStream(Path.of("config/database.local.properties"))) {
            config.load(archivo);
        }
        String url = (String) config.remove("url");
        conexion = DriverManager.getConnection(url, config);
        try (var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE Recibo (
                        id_recibo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        id_Pedido BIGINT NOT NULL UNIQUE,
                        total_pago DECIMAL(15,2) NOT NULL,
                        metodo_pago VARCHAR(20) NOT NULL,
                        fecha_recibo DATETIME(6) NOT NULL,
                        usuario_recibo VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB AUTO_INCREMENT=3000000000
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE linea_pedido (
                        id BIGINT NOT NULL PRIMARY KEY,
                        pedido_id BIGINT NOT NULL,
                        nombre_producto VARCHAR(150) NOT NULL,
                        nombre_categoria VARCHAR(100) NOT NULL,
                        descripcion_opciones TEXT NOT NULL,
                        precio_unitario DECIMAL(15,2) NOT NULL,
                        cantidad INT NOT NULL
                    ) ENGINE=InnoDB
                    """);
        }
        conexion.setAutoCommit(false);
    }

    @AfterEach
    void cerrarSesion() throws SQLException {
        if (conexion != null) {
            try {
                conexion.rollback();
            } finally {
                conexion.close();
            }
        }
    }

    private Recibo recibo() {
        lineaPedido linea = lineaPedido.reconstruirHistorica(LINEA_ID,
                "Producto al vender", "Categoria al vender", "Extra queso",
                new BigDecimal("1250.50"), 2);
        return new Recibo(0, PEDIDO_ID,
                new Pago(MetodoPago.TARJETA, new BigDecimal("2501.00"), FECHA, "cajero"),
                List.of(linea));
    }

    private void guardarLineaHistorica() throws SQLException {
        try (var sentencia = conexion.prepareStatement("""
                INSERT INTO linea_pedido
                    (id, pedido_id, nombre_producto, nombre_categoria,
                     descripcion_opciones, precio_unitario, cantidad)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            sentencia.setLong(1, LINEA_ID);
            sentencia.setLong(2, PEDIDO_ID);
            sentencia.setString(3, "Producto al vender");
            sentencia.setString(4, "Categoria al vender");
            sentencia.setString(5, "Extra queso");
            sentencia.setBigDecimal(6, new BigDecimal("1250.50"));
            sentencia.setInt(7, 2);
            sentencia.executeUpdate();
        }
    }

    @Test
    void recuperaIdsLongHoraMicrosegundosYDesgloseSinCatalogo() throws SQLException {
        guardarLineaHistorica();
        long id = dao.insertar(recibo(), conexion);
        Recibo recuperado = dao.buscarPorPedido(PEDIDO_ID, conexion);

        assertAll(
                () -> assertTrue(id > Integer.MAX_VALUE),
                () -> assertEquals(id, recuperado.getId()),
                () -> assertEquals(PEDIDO_ID, recuperado.getIdPedido()),
                () -> assertEquals(FECHA, recuperado.getPago().getFecha()),
                () -> assertEquals(MetodoPago.TARJETA, recuperado.getPago().getMetodo()),
                () -> assertEquals(new BigDecimal("2501.00"), recuperado.getTotal()),
                () -> assertEquals(recuperado.getTotal(), recuperado.getPago().getMonto()),
                () -> assertEquals(LINEA_ID, recuperado.getLineas().getFirst().getId()),
                () -> assertEquals("Producto al vender", recuperado.getLineas().getFirst().getNombreProducto()),
                () -> assertEquals("Categoria al vender", recuperado.getLineas().getFirst().getNombreCategoria()),
                () -> assertEquals("Extra queso", recuperado.getLineas().getFirst().getDescripcionExtras()),
                () -> assertNull(recuperado.getLineas().getFirst().getProducto()),
                () -> assertFalse(conexion.isClosed()),
                () -> assertFalse(conexion.getAutoCommit())
        );
    }

    @Test
    void elServicioPuedeRevertirElReciboYLasLineasJuntos() throws SQLException {
        guardarLineaHistorica();
        dao.insertar(recibo(), conexion);
        conexion.rollback();

        assertNull(dao.buscarPorPedido(PEDIDO_ID, conexion));
        try (var sentencia = conexion.createStatement();
             var resultado = sentencia.executeQuery("SELECT COUNT(*) FROM linea_pedido")) {
            assertTrue(resultado.next());
            assertEquals(0, resultado.getInt(1));
        }
    }

    @Test
    void uniqueImpideUnSegundoReciboSinCerrarLaConexion() throws SQLException {
        dao.insertar(recibo(), conexion);
        SQLException error = assertThrows(SQLException.class, () -> dao.insertar(recibo(), conexion));
        assertEquals(1062, error.getErrorCode());
        assertFalse(conexion.isClosed());
        conexion.rollback();
        assertNull(dao.buscarPorPedido(PEDIDO_ID, conexion));
    }

    @Test
    void devuelveNullSiNoHayReciboSinNecesitarLaTablaDeLineas() throws SQLException {
        try (var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("DROP TEMPORARY TABLE linea_pedido");
        }
        assertNull(dao.buscarPorPedido(PEDIDO_ID, conexion));
    }

    @Test
    void rechazaUnReciboPersistidoSinDesglose() throws SQLException {
        dao.insertar(recibo(), conexion);
        assertThrows(SQLException.class, () -> dao.buscarPorPedido(PEDIDO_ID, conexion));
    }

    @Test
    void detectaUnTotalQueYaNoCoincideConLasLineasHistoricas() throws SQLException {
        guardarLineaHistorica();
        dao.insertar(recibo(), conexion);
        try (var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("UPDATE linea_pedido SET precio_unitario = 1.00");
        }
        assertThrows(SQLException.class, () -> dao.buscarPorPedido(PEDIDO_ID, conexion));
    }

    @Test
    void exigeLaTransaccionDelServicioAntesDeInsertar() throws SQLException {
        conexion.setAutoCommit(true);
        assertThrows(SQLException.class, () -> dao.insertar(recibo(), conexion));
        assertNull(dao.buscarPorPedido(PEDIDO_ID, conexion));
        conexion.setAutoCommit(false);
    }

    @Test
    void noRedondeaImportesAlGuardar() {
        lineaPedido linea = lineaPedido.reconstruirHistorica(LINEA_ID,
                "Producto", "Categoria", "", new BigDecimal("10.001"), 1);
        Recibo recibo = new Recibo(0, PEDIDO_ID,
                new Pago(MetodoPago.EFECTIVO, new BigDecimal("10.001"), FECHA, "cajero"), List.of(linea));
        assertThrows(IllegalArgumentException.class, () -> dao.insertar(recibo, conexion));
    }

    @Test
    void rechazaUnPagoDiferenteDelTotalAunqueSoloSeGuardeUnImporte() {
        Recibo original = recibo();
        Recibo pagoIncorrecto = new Recibo(0, PEDIDO_ID,
                new Pago(MetodoPago.TARJETA, new BigDecimal("1.00"), FECHA, "cajero"),
                original.getLineas());

        assertThrows(IllegalArgumentException.class, () -> dao.insertar(pagoIncorrecto, conexion));
    }

    @Test
    void elSchemaYLasColumnasPermanentesCoincidenConElDAO() throws SQLException {
        assertEquals("restaurante", conexion.getCatalog());
        List<String> columnas = new ArrayList<>();
        try (var sentencia = conexion.prepareStatement("""
                SELECT COLUMN_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION
                """)) {
            sentencia.setString(1, "restaurante");
            sentencia.setString(2, "Recibo");
            try (var resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    columnas.add(resultado.getString(1));
                }
            }
        }
        assertEquals(List.of("id_recibo", "id_Pedido", "total_pago", "metodo_pago",
                "fecha_recibo", "usuario_recibo"), columnas);
    }
}
