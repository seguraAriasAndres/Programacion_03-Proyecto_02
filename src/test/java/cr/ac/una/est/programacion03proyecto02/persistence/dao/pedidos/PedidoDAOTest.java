package cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Usa tablas TEMPORARY de esta sesion; no escribe pedidos ni lineas reales. */
@EnabledIfSystemProperty(named = "pruebas.mysql", matches = "true")
class PedidoDAOTest {
    private Connection conexion;
    private PedidoDAO dao;

    @BeforeEach
    void abrirSesionConPedidoTemporal() throws Exception {
        Properties config = new Properties();
        try (var archivo = Files.newInputStream(Path.of("config/database.local.properties"))) {
            config.load(archivo);
        }
        String url = (String) config.remove("url");
        // Comprueba tambien el caso de UPDATE que coincide, pero no cambia valores.
        config.setProperty("useAffectedRows", "true");
        conexion = DriverManager.getConnection(url, config);
        try (var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE pedido (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        nombre_cliente VARCHAR(150) NOT NULL,
                        fecha_pedido DATETIME(6) NOT NULL,
                        confirmado TINYINT(1) NOT NULL DEFAULT 0,
                        estado_preparacion VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                        estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                        usuario_creacion VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB AUTO_INCREMENT=3000000000
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE linea_pedido (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        pedido_id BIGINT NOT NULL,
                        producto_id BIGINT NOT NULL,
                        nombre_producto VARCHAR(150) NOT NULL,
                        nombre_categoria VARCHAR(100) NOT NULL,
                        descripcion_opciones VARCHAR(1000) NOT NULL,
                        precio_unitario DECIMAL(15,2) NOT NULL,
                        cantidad INT NOT NULL,
                        CHECK (cantidad BETWEEN 1 AND 100)
                    ) ENGINE=InnoDB AUTO_INCREMENT=4000000000
                    """);
            // El limite de cantidad permite simular un fallo SQL a mitad del guardado.
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE linea_pedido_opcion (
                        linea_pedido_id BIGINT NOT NULL,
                        opcion_producto_id BIGINT NOT NULL,
                        PRIMARY KEY (linea_pedido_id, opcion_producto_id)
                    ) ENGINE=InnoDB
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE ingredientes_descontados (
                        pedido_id BIGINT NOT NULL,
                        ingrediente_id BIGINT NOT NULL,
                        cantidad DECIMAL(15,6) NOT NULL,
                        PRIMARY KEY (pedido_id, ingrediente_id)
                    ) ENGINE=InnoDB
                    """);
            sentencia.executeUpdate("""
                    CREATE TEMPORARY TABLE Recibo (
                        id_recibo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        id_pedido BIGINT NOT NULL UNIQUE,
                        total_pago DECIMAL(15,2) NOT NULL,
                        metodo_pago VARCHAR(20) NOT NULL,
                        fecha_recibo DATETIME(6) NOT NULL,
                        usuario_recibo VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB AUTO_INCREMENT=8000000000
                    """);
        }
        conexion.setAutoCommit(false);
        dao = new PedidoDAO(conexion);
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

    @Test
    void devuelveElIdLongGeneradoYLosMismosDatosGuardadosEnMySQL() throws Exception {
        Pedido pedido = dao.insertarBorrador("Maria", "cajero_daniel");

        assertAll(
                () -> assertTrue(pedido.getIdPedido() > Integer.MAX_VALUE),
                () -> assertFalse(pedido.isConfirmado()),
                () -> assertEquals(EstadoPreparacion.PENDIENTE, pedido.getEstadoPreparacion()),
                () -> assertEquals(EstadoPago.PENDIENTE, pedido.getEstadoPago()),
                () -> assertTrue(pedido.getLineas().isEmpty()),
                () -> assertNull(pedido.getRecibo())
        );
        try (var sentencia = conexion.prepareStatement("SELECT * FROM pedido WHERE id = ?")) {
            sentencia.setLong(1, pedido.getIdPedido());
            try (var resultado = sentencia.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals(pedido.getIdPedido(), resultado.getLong("id"));
                assertEquals(pedido.getNombreCliente(), resultado.getString("nombre_cliente"));
                assertEquals("cajero_daniel", resultado.getString("usuario_creacion"));
                assertEquals(pedido.getFechaPedido(), resultado.getObject("fecha_pedido", LocalDateTime.class));
                assertFalse(resultado.getBoolean("confirmado"));
                assertEquals("PENDIENTE", resultado.getString("estado_preparacion"));
                assertEquals("PENDIENTE", resultado.getString("estado_pago"));
                assertFalse(resultado.next());
            }
        }
    }

    @Test
    void cadaInsercionDevuelveSuPropioIdSinUsarUnIdProvisional() throws Exception {
        Pedido primero = dao.insertarBorrador("Ana", "cajero");
        Pedido segundo = dao.insertarBorrador("Luis", "cajero");

        assertNotEquals(primero.getIdPedido(), segundo.getIdPedido());
        assertEquals(2, contarPedidos());
    }

    @Test
    void rechazaCamposNulosEnBlancoODemasiadoLargosAntesDeInsertar() throws Exception {
        for (String nombre : new String[]{null, "", "   ", "a".repeat(151)}) {
            assertThrows(IllegalArgumentException.class, () -> dao.insertarBorrador(nombre, "cajero"));
        }
        for (String usuario : new String[]{null, "", "   ", "a".repeat(101)}) {
            assertThrows(IllegalArgumentException.class, () -> dao.insertarBorrador("Ana", usuario));
        }
        assertEquals(0, contarPedidos());
    }

    @Test
    void dejaAlLlamadorCerrarLaConexionYConfirmarORevertirLaTransaccion() throws Exception {
        dao.insertarBorrador("Ana", "cajero");

        assertFalse(conexion.isClosed());
        assertFalse(conexion.getAutoCommit());
        assertEquals(1, contarPedidos());
        conexion.rollback();
        assertEquals(0, contarPedidos());
    }

    @Test
    void guardarActualizaSoloElPedidoIndicadoYConservaFechaUsuarioYEstados() throws Exception {
        Pedido original = dao.insertarBorrador("Ana", "cajero_original");
        Pedido otro = dao.insertarBorrador("Luis", "otro_cajero");
        Pedido editado = new Pedido(original.getIdPedido(), "Ana Maria", original.getFechaPedido().plusDays(1));

        dao.guardarBorrador(editado);

        try (var sentencia = conexion.prepareStatement("SELECT * FROM pedido WHERE id = ?")) {
            sentencia.setLong(1, original.getIdPedido());
            try (var resultado = sentencia.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals("Ana Maria", resultado.getString("nombre_cliente"));
                assertEquals(original.getFechaPedido(), resultado.getObject("fecha_pedido", LocalDateTime.class));
                assertEquals("cajero_original", resultado.getString("usuario_creacion"));
                assertFalse(resultado.getBoolean("confirmado"));
                assertEquals("PENDIENTE", resultado.getString("estado_preparacion"));
                assertEquals("PENDIENTE", resultado.getString("estado_pago"));
            }
        }
        assertEquals("Luis", consultarNombre(otro.getIdPedido()));
        assertEquals(2, contarPedidos());
    }

    @Test
    void guardarElMismoBorradorSinCambiosNoEsUnError() throws Exception {
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");

        dao.guardarBorrador(pedido);

        assertEquals("Ana", consultarNombre(pedido.getIdPedido()));
        assertEquals(1, contarPedidos());
    }

    @Test
    void rechazaUnPedidoConfirmadoEnLaBaseAunqueElObjetoSigaComoBorrador() throws Exception {
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        try (var sentencia = conexion.prepareStatement("UPDATE pedido SET confirmado = 1 WHERE id = ?")) {
            sentencia.setLong(1, pedido.getIdPedido());
            sentencia.executeUpdate();
        }
        Pedido desactualizado = new Pedido(pedido.getIdPedido(), "Nombre incorrecto");

        assertThrows(SQLException.class, () -> dao.guardarBorrador(desactualizado));
        assertEquals("Ana", consultarNombre(pedido.getIdPedido()));
        try (var sentencia = conexion.createStatement();
             var resultado = sentencia.executeQuery("SELECT confirmado FROM pedido")) {
            assertTrue(resultado.next());
            assertTrue(resultado.getBoolean(1));
        }
    }

    @Test
    void rechazaObjetosNoEditablesEIdsNoPersistidos() throws Exception {
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        Pedido cancelado = new Pedido(pedido.getIdPedido(), "Cancelado");
        cancelado.cancelarPedido();
        assertThrows(IllegalStateException.class, () -> dao.guardarBorrador(cancelado));

        pedido.agregarLinea(lineaPedido.reconstruirHistorica(1, "Producto", "Comidas", "", BigDecimal.TEN, 1));
        pedido.confirmar();
        assertThrows(IllegalStateException.class, () -> dao.guardarBorrador(pedido));
        assertThrows(NullPointerException.class, () -> dao.guardarBorrador(null));
        assertThrows(IllegalArgumentException.class, () -> dao.guardarBorrador(new Pedido(0, "Ana")));
        assertThrows(IllegalArgumentException.class,
                () -> dao.guardarBorrador(new Pedido(pedido.getIdPedido(), "a".repeat(151))));
        assertEquals("Ana", consultarNombre(pedido.getIdPedido()));
    }

    @Test
    void rechazaUnPedidoInexistente() {
        assertThrows(SQLException.class, () -> dao.guardarBorrador(new Pedido(9_000_000_000L, "Ana")));
    }

    @Test
    void guardarDejaLaTransaccionAbiertaYPermiteRollback() throws Exception {
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        conexion.commit(); // Solo fija la fila temporal inicial para probar el rollback de la edicion.
        dao.guardarBorrador(new Pedido(pedido.getIdPedido(), "Nombre editado"));

        assertFalse(conexion.isClosed());
        assertFalse(conexion.getAutoCommit());
        assertEquals("Nombre editado", consultarNombre(pedido.getIdPedido()));
        conexion.rollback();
        assertEquals("Ana", consultarNombre(pedido.getIdPedido()));
    }

    @Test
    void guardaLineasYOpcionesConIdsLongYConservaLosDatosHistoricos() throws Exception {
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        Producto producto = producto();
        lineaPedido original = new lineaPedido(0, producto, List.of(producto.getOpciones().getFirst()), 2);
        pedido.agregarLinea(original);
        producto.setNombre("Nombre cambiado en catalogo");
        producto.setPrecioBase(new BigDecimal("9000"));
        producto.getCategoria().setNombre("Categoria cambiada");

        Pedido guardado = dao.guardarBorrador(pedido);

        lineaPedido linea = guardado.getLineas().getFirst();
        assertTrue(linea.getId() > Integer.MAX_VALUE);
        assertEquals(0, original.getId());
        assertEquals("Hamburguesa", linea.getNombreProducto());
        assertEquals("Comidas", linea.getNombreCategoria());
        assertEquals("Queso", linea.getDescripcionExtras());
        assertEquals(0, new BigDecimal("1200").compareTo(linea.getPrecioUnitario()));
        assertEquals(pedido.getFechaPedido(), guardado.getFechaPedido());
        assertEquals(Set.of(5_000_000_001L), consultarOpciones(linea.getId()));
        try (var sentencia = conexion.prepareStatement("SELECT * FROM linea_pedido WHERE id = ?")) {
            sentencia.setLong(1, linea.getId());
            try (var r = sentencia.executeQuery()) {
                assertTrue(r.next());
                assertEquals(pedido.getIdPedido(), r.getLong("pedido_id"));
                assertEquals(producto.getId(), r.getLong("producto_id"));
                assertEquals("Hamburguesa", r.getString("nombre_producto"));
                assertEquals("Comidas", r.getString("nombre_categoria"));
                assertEquals("Queso", r.getString("descripcion_opciones"));
                assertEquals(new BigDecimal("1200.00"), r.getBigDecimal("precio_unitario"));
                assertEquals(2, r.getInt("cantidad"));
            }
        }
        dao.guardarBorrador(guardado);
        assertEquals(1, contarLineas());
    }

    @Test
    void actualizaCantidadYOpcionesConservandoElIdDeLaLinea() throws Exception {
        Producto producto = producto();
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        pedido.agregarLinea(new lineaPedido(0, producto, List.of(producto.getOpciones().getFirst()), 1));
        Pedido guardado = dao.guardarBorrador(pedido);
        long id = guardado.getLineas().getFirst().getId();
        Pedido editado = new Pedido(pedido.getIdPedido(), "Ana", pedido.getFechaPedido());
        editado.agregarLinea(new lineaPedido(id, producto, List.of(producto.getOpciones().get(1)), 3));

        Pedido actualizado = dao.guardarBorrador(editado);

        assertEquals(id, actualizado.getLineas().getFirst().getId());
        assertEquals(1, contarLineas());
        assertEquals(Set.of(5_000_000_002L), consultarOpciones(id));
        try (var sentencia = conexion.createStatement(); var r = sentencia.executeQuery("SELECT cantidad, precio_unitario FROM linea_pedido")) {
            assertTrue(r.next());
            assertEquals(3, r.getInt("cantidad"));
            assertEquals(new BigDecimal("1500.00"), r.getBigDecimal("precio_unitario"));
        }
    }

    @Test
    void eliminaLineasRetiradasYSusOpcionesSinAfectarOtroPedido() throws Exception {
        Producto producto = producto();
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        pedido.agregarLinea(new lineaPedido(0, producto, List.of(producto.getOpciones().getFirst()), 1));
        pedido.agregarLinea(new lineaPedido(0, producto, 2));
        Pedido guardado = dao.guardarBorrador(pedido);
        long eliminado = guardado.getLineas().getFirst().getId();
        Pedido otro = dao.insertarBorrador("Luis", "cajero");
        otro.agregarLinea(new lineaPedido(0, producto, List.of(producto.getOpciones().getFirst()), 1));
        otro = dao.guardarBorrador(otro);
        long idAjeno = otro.getLineas().getFirst().getId();

        guardado.eliminarLinea(guardado.getLineas().getFirst());
        guardado = dao.guardarBorrador(guardado);
        assertEquals(2, contarLineas());
        assertTrue(consultarOpciones(eliminado).isEmpty());
        assertEquals(Set.of(5_000_000_001L), consultarOpciones(idAjeno));
        guardado.eliminarLinea(guardado.getLineas().getFirst());
        dao.guardarBorrador(guardado);
        assertEquals(1, contarLineas());
        assertEquals(2, contarPedidos());
    }

    @Test
    void unFalloSQLRevierteCabeceraLineasYOpcionesPeroNoTrabajoPrevio() throws Exception {
        Producto producto = producto();
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        pedido.agregarLinea(new lineaPedido(0, producto, List.of(producto.getOpciones().getFirst()), 1));
        Pedido original = dao.guardarBorrador(pedido);
        conexion.commit();
        dao.insertarBorrador("Trabajo previo", "cajero");
        long id = original.getLineas().getFirst().getId();
        Pedido editado = new Pedido(original.getIdPedido(), "Nombre que debe revertirse");
        editado.agregarLinea(new lineaPedido(id, producto, List.of(producto.getOpciones().get(1)), 2));
        editado.agregarLinea(new lineaPedido(0, producto, 101)); // Invalido para el CHECK de la tabla temporal.

        assertThrows(SQLException.class, () -> dao.guardarBorrador(editado));

        assertEquals("Ana", consultarNombre(original.getIdPedido()));
        assertEquals(1, contarLineas());
        assertEquals(Set.of(5_000_000_001L), consultarOpciones(id));
        assertEquals(2, contarPedidos()); // El savepoint no deshace el INSERT anterior al guardado.
        try (var sentencia = conexion.createStatement(); var r = sentencia.executeQuery("SELECT cantidad FROM linea_pedido")) {
            assertTrue(r.next());
            assertEquals(1, r.getInt(1));
        }
        assertEquals(0, editado.getLineas().get(1).getId());
    }

    @Test
    void rechazaLineasDeOtroPedidoSinModificarNinguno() throws Exception {
        Producto producto = producto();
        Pedido primero = dao.insertarBorrador("Ana", "cajero");
        Pedido segundo = dao.insertarBorrador("Luis", "cajero");
        segundo.agregarLinea(new lineaPedido(0, producto, 1));
        segundo = dao.guardarBorrador(segundo);
        Pedido invalido = new Pedido(primero.getIdPedido(), "Nombre incorrecto");
        invalido.agregarLinea(segundo.getLineas().getFirst());

        assertThrows(IllegalArgumentException.class, () -> dao.guardarBorrador(invalido));
        assertEquals("Ana", consultarNombre(primero.getIdPedido()));
        assertEquals("Luis", consultarNombre(segundo.getIdPedido()));
        assertEquals(1, contarLineas());
    }

    @Test
    void rechazaIdsDuplicadosYPreciosQueNecesitarianRedondeo() throws Exception {
        Producto producto = producto();
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        pedido.agregarLinea(new lineaPedido(0, producto, 1));
        Pedido guardado = dao.guardarBorrador(pedido);
        guardado.agregarLinea(guardado.getLineas().getFirst());
        assertThrows(IllegalArgumentException.class, () -> dao.guardarBorrador(guardado));
        Producto conDecimales = producto();
        conDecimales.setPrecioBase(new BigDecimal("10.001"));
        Pedido invalido = new Pedido(pedido.getIdPedido(), "Nombre que no debe guardarse");
        invalido.agregarLinea(new lineaPedido(0, conDecimales, 1));
        assertThrows(IllegalArgumentException.class, () -> dao.guardarBorrador(invalido));
        assertEquals("Ana", consultarNombre(pedido.getIdPedido()));
        assertEquals(1, contarLineas());
    }

    @Test
    void exigeUnaTransaccionParaGuardarElBorradorCompleto() throws Exception {
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        conexion.commit();
        conexion.setAutoCommit(true);
        try {
            assertThrows(SQLException.class, () -> dao.guardarBorrador(pedido));
        } finally {
            conexion.setAutoCommit(false);
        }
        assertEquals("Ana", consultarNombre(pedido.getIdPedido()));
    }

    @Test
    void recuperaUnBorradorCompletoYPermiteGuardarloSinCatalogo() throws Exception {
        Pedido guardado = borradorConLinea("Ana", producto(), 2);
        long idLinea = guardado.getLineas().getFirst().getId();
        conexion.commit();

        Pedido recuperado = dao.buscarBloqueado(guardado.getIdPedido(), conexion);

        assertEquals(guardado.getFechaPedido(), recuperado.getFechaPedido());
        lineaPedido linea = recuperado.getLineas().getFirst();
        assertNull(linea.getProducto());
        assertTrue(linea.getExtras().isEmpty());
        assertEquals(6_000_000_001L, linea.getProductoId());
        assertEquals(List.of(5_000_000_001L), linea.getOpcionIds());
        assertEquals("Queso", linea.getDescripcionExtras());
        Pedido otraVez = dao.guardarBorrador(recuperado);
        assertEquals(idLinea, otraVez.getLineas().getFirst().getId());
        assertEquals(1, contarLineas());
        assertEquals(Set.of(5_000_000_001L), consultarOpciones(idLinea));
    }

    @Test
    void confirmaConDescuentosYRecuperaPreparacionEntregaYPago() throws Exception {
        Pedido pedido = borradorConLinea("Ana", producto(), 2);
        pedido.registrarIngredientesDescontados(Map.of(9_000_000_001L, 1.234567));
        pedido.confirmar();
        dao.actualizar(pedido, conexion);
        Pedido confirmado = dao.buscarBloqueado(pedido.getIdPedido(), conexion);
        assertTrue(confirmado.isConfirmado());
        assertEquals(Map.of(9_000_000_001L, 1.234567), confirmado.getIngredientesDescontados());
        assertEquals(EstadoPreparacion.PENDIENTE, confirmado.getEstadoPreparacion());

        confirmado.iniciarPreparacion();
        dao.actualizar(confirmado, conexion);
        confirmado.entregarPedido();
        dao.actualizar(confirmado, conexion);
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 22, 12, 0, 0, 123456000);
        cobrar(confirmado, fecha);
        conexion.commit();

        Pedido recuperado = dao.buscarBloqueado(pedido.getIdPedido(), conexion);
        assertEquals(EstadoPreparacion.ENTREGADO, recuperado.getEstadoPreparacion());
        assertEquals(EstadoPago.PAGADO, recuperado.getEstadoPago());
        assertTrue(recuperado.getRecibo().getId() > Integer.MAX_VALUE);
        assertEquals(fecha, recuperado.getRecibo().getPago().getFecha());
        assertEquals(0, pedido.calcularTotal().compareTo(recuperado.getRecibo().getTotal()));
        assertEquals(pedido.getFechaPedido(), recuperado.getFechaPedido());
        assertEquals(confirmado.getIngredientesDescontados(), recuperado.getIngredientesDescontados());
    }

    @Test
    void actualizarNoConfirmaNiCierraYRollbackRevierteEstadoYDescuentos() throws Exception {
        Pedido pedido = borradorConLinea("Ana", producto(), 1);
        conexion.commit();
        pedido.registrarIngredientesDescontados(Map.of(9_000_000_001L, 0.125));
        pedido.confirmar();
        dao.actualizar(pedido, conexion);
        assertFalse(conexion.isClosed());
        assertFalse(conexion.getAutoCommit());
        assertTrue(dao.buscarBloqueado(pedido.getIdPedido(), conexion).isConfirmado());

        conexion.rollback();

        Pedido recuperado = dao.buscarBloqueado(pedido.getIdPedido(), conexion);
        assertFalse(recuperado.isConfirmado());
        assertTrue(recuperado.getIngredientesDescontados().isEmpty());
    }

    @Test
    void cobrarAdmiteNanosegundosYConservaLosMicrosegundosDeMySQL() throws Exception {
        Pedido pedido = confirmado("Ana", producto(), 1);
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 22, 12, 0, 0, 123456789);

        cobrar(pedido, fecha);

        Pedido recuperado = dao.buscarBloqueado(pedido.getIdPedido(), conexion);
        assertEquals(EstadoPago.PAGADO, recuperado.getEstadoPago());
        assertEquals(fecha.withNano(123456000), recuperado.getRecibo().getPago().getFecha());
    }

    @Test
    void rechazaDescuentosQueNecesitanRedondeoSinDejarInsercionesParciales() throws Exception {
        Pedido pedido = borradorConLinea("Ana", producto(), 1);
        pedido.registrarIngredientesDescontados(Map.of(9_000_000_001L, 0.125, 9_000_000_002L, 0.1234567));
        pedido.confirmar();

        assertThrows(IllegalArgumentException.class, () -> dao.actualizar(pedido, conexion));

        Pedido recuperado = dao.buscarBloqueado(pedido.getIdPedido(), conexion);
        assertFalse(recuperado.isConfirmado());
        assertTrue(recuperado.getIngredientesDescontados().isEmpty());
        assertEquals(1, contarPedidos());
    }

    @Test
    void cancelarConservaElDescuentoHistoricoYNoPermiteReabrir() throws Exception {
        Pedido pedido = confirmado("Ana", producto(), 1);
        pedido.cancelarPedido();
        dao.actualizar(pedido, conexion);
        Pedido cancelado = dao.buscarBloqueado(pedido.getIdPedido(), conexion);
        assertEquals(EstadoPreparacion.CANCELADO, cancelado.getEstadoPreparacion());
        assertEquals(pedido.getIngredientesDescontados(), cancelado.getIngredientesDescontados());
        Pedido reabierto = Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), pedido.getFechaPedido(),
                true, EstadoPreparacion.PENDIENTE, EstadoPago.PENDIENTE,
                pedido.getLineas(), pedido.getIngredientesDescontados(), null);
        assertThrows(IllegalStateException.class, () -> dao.actualizar(reabierto, conexion));
        assertEquals(EstadoPreparacion.CANCELADO, dao.buscarBloqueado(pedido.getIdPedido(), conexion).getEstadoPreparacion());
    }

    @Test
    void rechazaCambiosEnLineasHistoricasYDescuentosTrasConfirmar() throws Exception {
        Pedido pedido = confirmado("Ana", producto(), 1);
        lineaPedido linea = pedido.getLineas().getFirst();
        lineaPedido alterada = lineaPedido.reconstruirPersistida(linea.getId(), linea.getProductoId(), linea.getOpcionIds(),
                linea.getNombreProducto(), linea.getNombreCategoria(), linea.getDescripcionExtras(), BigDecimal.ONE, 1);
        Pedido invalido = Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), pedido.getFechaPedido(),
                true, EstadoPreparacion.PENDIENTE, EstadoPago.PENDIENTE,
                List.of(alterada), pedido.getIngredientesDescontados(), null);
        assertThrows(IllegalArgumentException.class, () -> dao.actualizar(invalido, conexion));
        Pedido otroDescuento = Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), pedido.getFechaPedido(),
                true, EstadoPreparacion.PENDIENTE, EstadoPago.PENDIENTE,
                pedido.getLineas(), Map.of(9_000_000_001L, 100.0), null);
        assertThrows(IllegalStateException.class, () -> dao.actualizar(otroDescuento, conexion));
        Pedido recuperado = dao.buscarBloqueado(pedido.getIdPedido(), conexion);
        assertEquals(0, pedido.calcularTotal().compareTo(recuperado.calcularTotal()));
        assertEquals(pedido.getIngredientesDescontados(), recuperado.getIngredientesDescontados());
    }

    @Test
    void rechazaRetrocederPreparacionODeshacerLaConfirmacion() throws Exception {
        Pedido pedido = confirmado("Ana", producto(), 1);
        pedido.iniciarPreparacion();
        dao.actualizar(pedido, conexion);
        Pedido anterior = Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), pedido.getFechaPedido(),
                true, EstadoPreparacion.PENDIENTE, EstadoPago.PENDIENTE,
                pedido.getLineas(), pedido.getIngredientesDescontados(), null);
        assertThrows(IllegalStateException.class, () -> dao.actualizar(anterior, conexion));
        Pedido sinConfirmar = Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), pedido.getFechaPedido(),
                false, EstadoPreparacion.PENDIENTE, EstadoPago.PENDIENTE, pedido.getLineas(), Map.of(), null);
        assertThrows(IllegalStateException.class, () -> dao.actualizar(sinConfirmar, conexion));
        assertEquals(EstadoPreparacion.EN_PREPARACION, dao.buscarBloqueado(pedido.getIdPedido(), conexion).getEstadoPreparacion());
    }

    @Test
    void exigeElReciboPersistidoAntesDeMarcarPagado() throws Exception {
        Pedido pedido = confirmado("Ana", producto(), 1);
        Pago pago = new Pago(MetodoPago.TARJETA, pedido.calcularTotal(),
                LocalDateTime.of(2026, 10, 22, 12, 0), "cajero");
        pedido.registrarCobro(new Recibo(8_000_000_099L, pedido.getIdPedido(), pago, pedido.getLineas()));

        assertThrows(IllegalStateException.class, () -> dao.actualizar(pedido, conexion));

        assertEquals(EstadoPago.PENDIENTE, dao.buscarBloqueado(pedido.getIdPedido(), conexion).getEstadoPago());
    }

    @Test
    void activosIncluyeBorradoresYEntregadosSinPagarPeroExcluyeTerminadosYCancelados() throws Exception {
        Pedido borrador = dao.insertarBorrador("Borrador", "cajero");
        Pedido entregado = confirmado("Entregado sin pagar", producto(), 1);
        entregar(entregado);
        Pedido pagado = confirmado("Pagado pendiente de preparar", producto(), 1);
        cobrar(pagado, LocalDateTime.of(2026, 10, 22, 12, 0));
        Pedido terminado = confirmado("Terminado", producto(), 1);
        entregar(terminado);
        cobrar(terminado, LocalDateTime.of(2026, 10, 22, 12, 0));
        Pedido cancelado = dao.insertarBorrador("Cancelado", "cajero");
        cancelado.cancelarPedido();
        dao.actualizar(cancelado, conexion);

        List<Pedido> activos = dao.listarActivos();

        assertEquals(List.of(borrador.getIdPedido(), entregado.getIdPedido(), pagado.getIdPedido()),
                activos.stream().map(Pedido::getIdPedido).toList());
        assertThrows(UnsupportedOperationException.class, activos::clear);
        assertFalse(conexion.isClosed());
    }

    @Test
    void pagadosYRankingUsanLaFechaDeCobroConRangoSemiabiertoYExcluyenOtrosEstados() throws Exception {
        LocalDateTime desde = LocalDateTime.of(2026, 10, 22, 0, 0);
        LocalDateTime hasta = desde.plusDays(1);
        Pedido antes = confirmado("Antes", producto(), 10);
        cobrar(antes, desde.minusSeconds(1));
        Pedido incluido = confirmado("Incluido", producto(), 3);
        cobrar(incluido, desde);
        Producto otro = producto();
        otro.setId(6_000_000_002L);
        Pedido masVendido = confirmado("Mas vendido", otro, 5);
        cobrar(masVendido, hasta.minusNanos(1000));
        Pedido limite = confirmado("Limite excluido", producto(), 20);
        cobrar(limite, hasta);
        confirmado("Sin pagar", producto(), 50);
        Pedido cancelado = confirmado("Cancelado", producto(), 40);
        cancelado.cancelarPedido();
        dao.actualizar(cancelado, conexion);
        try (var sentencia = conexion.prepareStatement("UPDATE pedido SET fecha_pedido = ?")) {
            sentencia.setObject(1, desde.minusMonths(1)); // La fecha de creacion no define el periodo de ventas.
            sentencia.executeUpdate();
        }

        List<Pedido> pagados = dao.listarPagados(desde, hasta);
        Map<Long, Long> ranking = dao.ranking(desde, hasta);

        assertEquals(List.of(incluido.getIdPedido(), masVendido.getIdPedido()), pagados.stream().map(Pedido::getIdPedido).toList());
        assertEquals(List.of(6_000_000_002L, 6_000_000_001L), List.copyOf(ranking.keySet()));
        assertEquals(Map.of(6_000_000_001L, 3L, 6_000_000_002L, 5L), ranking);
        assertThrows(UnsupportedOperationException.class, ranking::clear);
        assertTrue(dao.listarPagados(hasta.plusDays(1), hasta.plusDays(2)).isEmpty());
        assertTrue(dao.ranking(hasta.plusDays(1), hasta.plusDays(2)).isEmpty());
    }

    @Test
    void consultasValidanRangosIdsYTransaccionYDevuelvenNullSiNoExiste() throws Exception {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 22, 0, 0);
        assertNull(dao.buscarBloqueado(9_000_000_099L, conexion));
        assertThrows(IllegalArgumentException.class, () -> dao.buscarBloqueado(0, conexion));
        assertThrows(IllegalArgumentException.class, () -> dao.listarPagados(fecha, fecha));
        assertThrows(IllegalArgumentException.class, () -> dao.ranking(null, fecha));
        assertThrows(IllegalArgumentException.class, () -> dao.ranking(fecha, fecha.minusDays(1)));
        assertThrows(SQLException.class, () -> dao.actualizar(new Pedido(9_000_000_099L, "No existe"), conexion));
        conexion.commit();
        conexion.setAutoCommit(true);
        try {
            assertThrows(SQLException.class, () -> dao.buscarBloqueado(1, conexion));
            assertThrows(SQLException.class, dao::listarActivos);
            assertThrows(SQLException.class, () -> dao.listarPagados(fecha, fecha.plusDays(1)));
            assertThrows(SQLException.class, () -> dao.actualizar(new Pedido(1, "Ana"), conexion));
        } finally {
            conexion.setAutoCommit(false);
        }
    }

    @Test
    void rechazaEstadosCorruptosAlCargar() throws Exception {
        Pedido pedido = dao.insertarBorrador("Ana", "cajero");
        try (var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("UPDATE pedido SET estado_preparacion = 'DESCONOCIDO'");
        }
        assertThrows(SQLException.class, () -> dao.buscarBloqueado(pedido.getIdPedido(), conexion));
        assertThrows(SQLException.class, dao::listarActivos);
    }

    private Pedido borradorConLinea(String nombre, Producto producto, int cantidad) throws SQLException {
        Pedido pedido = dao.insertarBorrador(nombre, "cajero");
        pedido.agregarLinea(new lineaPedido(0, producto, List.of(producto.getOpciones().getFirst()), cantidad));
        return dao.guardarBorrador(pedido);
    }

    private Pedido confirmado(String nombre, Producto producto, int cantidad) throws SQLException {
        Pedido pedido = borradorConLinea(nombre, producto, cantidad);
        pedido.registrarIngredientesDescontados(Map.of(9_000_000_001L, 0.125 * cantidad));
        pedido.confirmar();
        dao.actualizar(pedido, conexion);
        return pedido;
    }

    private void cobrar(Pedido pedido, LocalDateTime fecha) throws SQLException {
        Pago pago = new Pago(MetodoPago.TARJETA, pedido.calcularTotal(), fecha, "cajero");
        Recibo nuevo = new Recibo(0, pedido.getIdPedido(), pago, pedido.getLineas());
        long id = new ReciboDAO().insertar(nuevo, conexion);
        pedido.registrarCobro(new Recibo(id, pedido.getIdPedido(), pago, pedido.getLineas()));
        dao.actualizar(pedido, conexion);
    }

    private void entregar(Pedido pedido) throws SQLException {
        pedido.iniciarPreparacion();
        dao.actualizar(pedido, conexion);
        pedido.entregarPedido();
        dao.actualizar(pedido, conexion);
    }

    private Producto producto() {
        Producto producto = new Producto(6_000_000_001L, "Hamburguesa", new BigDecimal("1000"),
                new Categoria(7_000_000_001L, "Comidas", true), true);
        producto.setOpciones(List.of(
                new OpcionProducto(5_000_000_001L, "Queso", TipoOpcion.EXTRA, new BigDecimal("200"), true),
                new OpcionProducto(5_000_000_002L, "Papas", TipoOpcion.ACOMPANAMIENTO, new BigDecimal("500"), true)));
        return producto;
    }

    private Set<Long> consultarOpciones(long id) throws SQLException {
        Set<Long> ids = new HashSet<>();
        try (var sentencia = conexion.prepareStatement("SELECT opcion_producto_id FROM linea_pedido_opcion WHERE linea_pedido_id = ?")) {
            sentencia.setLong(1, id);
            try (var r = sentencia.executeQuery()) {
                while (r.next()) ids.add(r.getLong(1));
            }
        }
        return ids;
    }

    private int contarLineas() throws SQLException {
        try (var sentencia = conexion.createStatement(); var r = sentencia.executeQuery("SELECT COUNT(*) FROM linea_pedido")) {
            r.next();
            return r.getInt(1);
        }
    }

    private String consultarNombre(long id) throws SQLException {
        try (var sentencia = conexion.prepareStatement("SELECT nombre_cliente FROM pedido WHERE id = ?")) {
            sentencia.setLong(1, id);
            try (var resultado = sentencia.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getString(1);
            }
        }
    }

    private int contarPedidos() throws SQLException {
        try (var sentencia = conexion.createStatement();
             var resultado = sentencia.executeQuery("SELECT COUNT(*) FROM pedido")) {
            resultado.next();
            return resultado.getInt(1);
        }
    }
}
