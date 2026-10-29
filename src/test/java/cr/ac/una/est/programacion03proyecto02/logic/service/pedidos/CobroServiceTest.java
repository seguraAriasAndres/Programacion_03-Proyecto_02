package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.PedidoDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Todas las escrituras se hacen en tablas TEMPORARY, que desaparecen al cerrar la sesión real. */
@EnabledIfSystemProperty(named = "pruebas.mysql", matches = "true")
class CobroServiceTest {
    private Connection real;
    private ConexionDB proveedor;
    private PedidoRepository repositorio;
    private CobroService servicio;
    private Pedido pedido;
    private int aperturas;
    private int cierres;
    private int commits;
    private int rollbacks;
    private boolean fallarCommit;
    private Runnable antesCommit;

    @BeforeEach
    void prepararSesionTemporal() throws Exception {
        real = new ConexionDB().getConnection();
        try (var sentencia = real.createStatement()) {
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE pedido (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        nombre_cliente VARCHAR(150) NOT NULL, fecha_pedido DATETIME(6) NOT NULL,
                        confirmado BOOLEAN NOT NULL DEFAULT 0,
                        estado_preparacion VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                        estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                        usuario_creacion VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB AUTO_INCREMENT=3000000000
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE linea_pedido (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, pedido_id BIGINT NOT NULL,
                        producto_id BIGINT NOT NULL, nombre_producto VARCHAR(150) NOT NULL,
                        nombre_categoria VARCHAR(100) NOT NULL, descripcion_opciones VARCHAR(1000) NOT NULL,
                        precio_unitario DECIMAL(15,2) NOT NULL, cantidad INT NOT NULL
                    ) ENGINE=InnoDB AUTO_INCREMENT=4000000000
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE linea_pedido_opcion (
                        linea_pedido_id BIGINT NOT NULL, opcion_producto_id BIGINT NOT NULL,
                        cantidad INT NOT NULL DEFAULT 1,
                        PRIMARY KEY (linea_pedido_id, opcion_producto_id)
                    ) ENGINE=InnoDB
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE ingredientes_descontados (
                        pedido_id BIGINT NOT NULL, ingrediente_id BIGINT NOT NULL,
                        cantidad DECIMAL(15,6) NOT NULL, PRIMARY KEY (pedido_id, ingrediente_id)
                    ) ENGINE=InnoDB
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE Recibo (
                        id_recibo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, id_pedido BIGINT NOT NULL UNIQUE,
                        total_pago DECIMAL(15,2) NOT NULL, metodo_pago VARCHAR(20) NOT NULL,
                        fecha_recibo DATETIME(6) NOT NULL, usuario_recibo VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB AUTO_INCREMENT=8000000000
                    """);
        }
        real.setAutoCommit(false);
        PedidoDAO dao = new PedidoDAO(real);
        pedido = dao.insertarBorrador("Ana", "cajero_creador");
        pedido.agregarLinea(new lineaPedido(0,
                new Producto(6_000_000_001L, "Hamburguesa", new BigDecimal("1200.00"),
                        new Categoria(7_000_000_001L, "Comidas", true), true), 1));
        pedido = dao.guardarBorrador(pedido);
        pedido.registrarIngredientesDescontados(Map.of(9_000_000_001L, 0.25));
        pedido.confirmar();
        dao.actualizar(pedido);
        real.commit();

        proveedor = new ConexionDB() {
            @Override
            public Connection getConnection() {
                aperturas++;
                return conexionObservada();
            }
        };
        repositorio = new PedidoRepository(proveedor);
        repositorio.actualizarTrasCommit(pedido);
        servicio = new CobroService(repositorio, new ReciboDAO(), proveedor);
    }

    @AfterEach
    void cerrarSesionReal() throws Exception {
        if (real != null) {
            try {
                real.rollback();
            } finally {
                real.close();
            }
        }
    }

    @Test
    void cobraConUnaConexionFechaActualYCachePublicadaSoloDespuesDelCommit() throws Exception {
        antesCommit = () -> assertEquals(EstadoPago.PENDIENTE,
                repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getEstadoPago());
        LocalDateTime inicio = LocalDateTime.now().withNano(0);

        Recibo recibo = servicio.cobrar(pedido.getIdPedido(), MetodoPago.TARJETA, "otro_cajero");

        assertTrue(recibo.getId() > Integer.MAX_VALUE);
        assertEquals(1, aperturas);
        assertEquals(1, cierres);
        assertEquals(1, commits);
        assertEquals(0, rollbacks);
        assertEquals(1, contarRecibos());
        Pedido cobrado = new PedidoDAO(real).buscarBloqueado(pedido.getIdPedido());
        assertEquals(EstadoPago.PAGADO, cobrado.getEstadoPago());
        assertEquals(recibo.getId(), cobrado.getRecibo().getId());
        assertEquals(recibo.getPago().getFecha(), cobrado.getRecibo().getPago().getFecha());
        assertFalse(recibo.getPago().getFecha().isBefore(inicio));
        assertEquals("otro_cajero", cobrado.getRecibo().getPago().getUsuario());
        assertEquals(EstadoPago.PAGADO, repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getEstadoPago());
        assertEquals(EstadoPago.PENDIENTE, pedido.getEstadoPago()); // No se modificó el objeto anterior.
    }

    @Test
    void cobraAunqueElPedidoNoEsteEnCache() throws Exception {
        PedidoRepository vacio = new PedidoRepository(proveedor);
        CobroService sinCache = new CobroService(vacio, new ReciboDAO(), proveedor);
        sinCache.cobrar(pedido.getIdPedido(), MetodoPago.EFECTIVO, "cajero");
        assertEquals(1, contarRecibos());
        assertEquals(EstadoPago.PAGADO, vacio.obtenerPedidoPorId(pedido.getIdPedido()).getEstadoPago());
    }

    @Test
    void calculaElTotalDesdeSQLAunqueLaCacheEsteDesactualizada() throws Exception {
        ejecutar("UPDATE linea_pedido SET cantidad = 2");
        real.commit();
        assertEquals(new BigDecimal("1200.00"), repositorio.obtenerPedidoPorId(pedido.getIdPedido()).calcularTotal());

        Recibo recibo = servicio.cobrar(pedido.getIdPedido(), MetodoPago.EFECTIVO, "cajero");

        assertEquals(new BigDecimal("2400.00"), recibo.getTotal());
        assertEquals(2, repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getLineas().getFirst().getCantidad());
    }

    @Test
    void unSegundoCobroSeRechazaAunqueLaCacheTodaviaLoMuestrePendiente() throws Exception {
        servicio.cobrar(pedido.getIdPedido(), MetodoPago.TARJETA, "cajero");
        repositorio.actualizarTrasCommit(pedido); // Simula una caché atrasada de otra operación.

        assertThrows(IllegalStateException.class,
                () -> servicio.cobrar(pedido.getIdPedido(), MetodoPago.TARJETA, "cajero"));

        assertEquals(1, contarRecibos());
        assertEquals(1, commits);
        assertEquals(1, rollbacks);
        assertEquals(2, cierres);
    }

    @Test
    void rechazaPedidosCanceladosSinInsertarRecibo() throws Exception {
        pedido.cancelarPedido();
        new PedidoDAO(real).actualizar(pedido);
        real.commit();

        assertThrows(IllegalStateException.class,
                () -> servicio.cobrar(pedido.getIdPedido(), MetodoPago.TARJETA, "cajero"));

        assertEquals(0, contarRecibos());
        assertEquals(1, rollbacks);
        assertEquals(1, cierres);
    }

    @Test
    void rechazaBorradoresYPedidosInexistentesYLiberaSusConexiones() throws Exception {
        Pedido borrador = new PedidoDAO(real).insertarBorrador("Sin confirmar", "cajero");
        real.commit();
        assertThrows(IllegalStateException.class,
                () -> servicio.cobrar(borrador.getIdPedido(), MetodoPago.TARJETA, "cajero"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cobrar(9_000_000_099L, MetodoPago.TARJETA, "cajero"));
        assertEquals(0, contarRecibos());
        assertEquals(2, rollbacks);
        assertEquals(2, cierres);
    }

    @Test
    void unFalloAlActualizarRevierteTambienElReciboYConservaLaCache() throws Exception {
        // Restricción exclusiva de esta tabla temporal para provocar un fallo después del INSERT del recibo.
        ejecutar("ALTER TABLE pedido ADD CONSTRAINT chk_prueba_pago CHECK (estado_pago = 'PENDIENTE')");

        assertThrows(SQLException.class,
                () -> servicio.cobrar(pedido.getIdPedido(), MetodoPago.TARJETA, "cajero"));

        comprobarCobroRevertido();
    }

    @Test
    void unCommitFallidoRevierteTodoSinPublicarElCobroEnCache() throws Exception {
        fallarCommit = true;

        assertThrows(SQLException.class,
                () -> servicio.cobrar(pedido.getIdPedido(), MetodoPago.TARJETA, "cajero"));

        comprobarCobroRevertido();
    }

    @Test
    void cobrarUnEntregadoLoQuitaDeActivos() throws Exception {
        pedido.iniciarPreparacion();
        new PedidoDAO(real).actualizar(pedido);
        pedido.entregarPedido();
        new PedidoDAO(real).actualizar(pedido);
        real.commit();
        repositorio.actualizarTrasCommit(pedido);

        servicio.cobrar(pedido.getIdPedido(), MetodoPago.SINPE_MOVIL, "cajero");

        assertNull(repositorio.obtenerPedidoPorId(pedido.getIdPedido()));
        assertTrue(repositorio.listarPedidosActivos().isEmpty());
        assertEquals(1, contarRecibos());
    }

    @Test
    void recargaActivosTrasCommitYConservaLaCacheCuandoLaRecargaFalla() throws Exception {
        ejecutar("UPDATE pedido SET nombre_cliente = 'Ana desde SQL'");
        real.commit();
        fallarCommit = true;
        assertThrows(SQLException.class, repositorio::recargarActivos);
        assertEquals("Ana", repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getNombreCliente());
        fallarCommit = false;

        repositorio.recargarActivos();

        assertEquals("Ana desde SQL", repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getNombreCliente());
        assertEquals(2, cierres);
        assertEquals(1, commits);
        assertEquals(1, rollbacks);
    }

    private void comprobarCobroRevertido() throws SQLException {
        assertEquals(0, contarRecibos());
        assertEquals(EstadoPago.PENDIENTE, new PedidoDAO(real).buscarBloqueado(pedido.getIdPedido()).getEstadoPago());
        assertEquals(EstadoPago.PENDIENTE, repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getEstadoPago());
        assertEquals(0, commits);
        assertEquals(1, rollbacks);
        assertEquals(1, cierres);
    }

    private long contarRecibos() throws SQLException {
        try (var sentencia = real.createStatement(); var resultado = sentencia.executeQuery("SELECT COUNT(*) FROM Recibo")) {
            assertTrue(resultado.next());
            return resultado.getLong(1);
        }
    }

    private void ejecutar(String sql) throws SQLException {
        try (var sentencia = real.createStatement()) {
            sentencia.executeUpdate(sql);
        }
    }

    private Connection conexionObservada() {
        // Intercepta close para contar su uso y poder inspeccionar las tablas temporales antes del teardown.
        boolean[] cerrada = {false};
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("close")) {
                        cerrada[0] = true;
                        cierres++;
                        return null;
                    }
                    if (metodo.getName().equals("isClosed")) {
                        return cerrada[0];
                    }
                    if (cerrada[0]) {
                        throw new SQLException("La conexión de la operación ya fue cerrada.");
                    }
                    if (metodo.getName().equals("commit")) {
                        if (antesCommit != null) antesCommit.run();
                        if (fallarCommit) throw new SQLException("Fallo de commit provocado por la prueba.");
                        real.commit();
                        commits++;
                        return null;
                    }
                    if (metodo.getName().equals("rollback") && (argumentos == null || argumentos.length == 0)) {
                        rollbacks++;
                    }
                    try {
                        return metodo.invoke(real, argumentos);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                });
    }
}
