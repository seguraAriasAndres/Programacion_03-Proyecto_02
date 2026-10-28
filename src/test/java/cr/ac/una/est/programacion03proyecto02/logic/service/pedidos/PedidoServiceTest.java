package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.ICatalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.inventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.ingredienteDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.movimientoStockDAO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "pruebas.mysql", matches = "true")
class PedidoServiceTest {

    private static final long productoId = 6_000_000_001L;
    private static final long opcionId = 7_000_000_001L;

    private Connection real;
    private Pedido pedido;
    private PedidoService servicio;
    private int aperturas;
    private int cierres;
    private int commits;
    private int rollbacks;
    private int consultasCatalogo;
    private boolean fallarCommit;
    private Runnable antesCommit;
    private ConexionDB proveedor;
    private ICatalogoService catalogo;
    private Connection ultimaConexion;
    private boolean catalogoNoDisponible;

    @BeforeEach
    void preparar() throws Exception {
        real = new ConexionDB().getConnection();

        try (var sentencia = real.createStatement()) {
            sentencia.executeUpdate(
                    "CREATE TEMPORARY TABLE pedido("
                            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "nombre_cliente VARCHAR(150),"
                            + "fecha_pedido DATETIME(6),"
                            + "confirmado BOOLEAN,"
                            + "estado_preparacion VARCHAR(20),"
                            + "estado_pago VARCHAR(20),"
                            + "usuario_creacion VARCHAR(100)"
                            + ") ENGINE=InnoDB AUTO_INCREMENT=3000000001"
            );
            sentencia.executeUpdate(
                    "CREATE TEMPORARY TABLE linea_pedido("
                            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "pedido_id BIGINT,"
                            + "producto_id BIGINT,"
                            + "nombre_producto VARCHAR(150),"
                            + "nombre_categoria VARCHAR(100),"
                            + "descripcion_opciones VARCHAR(1000),"
                            + "precio_unitario DECIMAL(15,2),"
                            + "cantidad INT"
                            + ") ENGINE=InnoDB AUTO_INCREMENT=4000000001"
            );
            sentencia.executeUpdate(
                    "CREATE TEMPORARY TABLE linea_pedido_opcion("
                            + "linea_pedido_id BIGINT,"
                            + "opcion_producto_id BIGINT,"
                            + "PRIMARY KEY(linea_pedido_id,opcion_producto_id)"
                            + ") ENGINE=InnoDB"
            );
            sentencia.executeUpdate(
                    "CREATE TEMPORARY TABLE ingredientes_descontados("
                            + "pedido_id BIGINT,"
                            + "ingrediente_id BIGINT,"
                            + "cantidad DECIMAL(15,6),"
                            + "PRIMARY KEY(pedido_id,ingrediente_id)"
                            + ") ENGINE=InnoDB"
            );
            sentencia.executeUpdate(
                    "CREATE TEMPORARY TABLE Recibo("
                            + "id_recibo BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "id_pedido BIGINT UNIQUE,"
                            + "total_pago DECIMAL(15,2),"
                            + "metodo_pago VARCHAR(20),"
                            + "fecha_recibo DATETIME(6),"
                            + "usuario_recibo VARCHAR(100)"
                            + ") ENGINE=InnoDB"
            );
            sentencia.executeUpdate(
                    "CREATE TEMPORARY TABLE ingrediente("
                            + "id BIGINT PRIMARY KEY,"
                            + "nombre VARCHAR(100),"
                            + "cantidad_disponible DECIMAL(15,6),"
                            + "unidad_medida VARCHAR(20),"
                            + "stock_minimo DECIMAL(15,6),"
                            + "activo BOOLEAN"
                            + ") ENGINE=InnoDB"
            );
            sentencia.executeUpdate(
                    "CREATE TEMPORARY TABLE movimiento_stock("
                            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "ingrediente_id BIGINT,"
                            + "pedido_id BIGINT,"
                            + "tipo_movimiento VARCHAR(30),"
                            + "cantidad DECIMAL(15,6),"
                            + "fecha DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),"
                            + "usuario VARCHAR(100),"
                            + "motivo VARCHAR(255),"
                            + "UNIQUE(pedido_id,ingrediente_id,tipo_movimiento)"
                            + ") ENGINE=InnoDB"
            );
            sentencia.executeUpdate(
                    "INSERT INTO ingrediente VALUES"
                            + "(1,'Harina',10,'GRAMO',1,TRUE),"
                            + "(2,'Queso',10,'GRAMO',1,TRUE)"
            );
        }

        real.setAutoCommit(false);
        pedido = new PedidoDAO(real).insertarBorrador("Ana", "creador");
        real.commit();

        proveedor = new ConexionDB() {
            @Override
            public Connection getConnection() {
                aperturas++;
                AtomicBoolean cerrada = new AtomicBoolean();

                ultimaConexion = (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy, metodo, argumentos) -> {
                            if (metodo.getName().equals("close")) {
                                cierres++;
                                cerrada.set(true);
                                return null;
                            }

                            if (cerrada.get()) {
                                throw new SQLException(
                                        "Se reutilizó una conexión cerrada."
                                );
                            }

                            if (metodo.getName().equals("commit")) {
                                if (antesCommit != null) {
                                    antesCommit.run();
                                }
                                if (fallarCommit) {
                                    throw new SQLException(
                                            "Fallo simulado de commit."
                                    );
                                }
                                commits++;
                            }

                            if (metodo.getName().equals("rollback")
                                    && (argumentos == null
                                    || argumentos.length == 0)) {
                                rollbacks++;
                            }

                            try {
                                return metodo.invoke(real, argumentos);
                            } catch (InvocationTargetException error) {
                                throw error.getCause();
                            }
                        }
                );

                return ultimaConexion;
            }
        };

        catalogo = (ICatalogoService) Proxy.newProxyInstance(
                ICatalogoService.class.getClassLoader(),
                new Class<?>[]{ICatalogoService.class},
                (proxy, metodo, argumentos) -> {
                    if (!metodo.getName().equals("obtenerProducto")
                            || argumentos.length != 2) {
                        throw new AssertionError(
                                "Consulta inesperada: " + metodo.getName()
                        );
                    }

                    consultasCatalogo++;

                    if (catalogoNoDisponible) {
                        throw new SQLException("Catálogo fuera de servicio.");
                    }

                    assertEquals(productoId, argumentos[0]);
                    assertFalse(((Connection) argumentos[1]).getAutoCommit());
                    assertSame(ultimaConexion, argumentos[1]);

                    Producto resultado = new Producto(
                            productoId,
                            "Hamburguesa",
                            new BigDecimal("1000.00"),
                            new Categoria(1, "Comidas", true),
                            true
                    );
                    resultado.setReceta(List.of(
                            new recetaItem(1, 0.1),
                            new recetaItem(2, 0.05)
                    ));

                    OpcionProducto opcion = new OpcionProducto(
                            opcionId,
                            "Queso",
                            TipoOpcion.EXTRA,
                            new BigDecimal("2.50"),
                            true
                    );
                    opcion.setReceta(List.of(new recetaItem(1, 0.2)));
                    resultado.getOpciones().add(opcion);

                    return resultado;
                }
        );

        servicio = new PedidoService(
                proveedor,
                catalogo,
                new inventarioService(
                        proveedor,
                        new ingredienteDAO(),
                        new movimientoStockDAO()
                )
        );

        aperturas = 0;
        cierres = 0;
        commits = 0;
        rollbacks = 0;
    }

    @AfterEach
    void cerrar() throws Exception {
        if (real != null) {
            try {
                real.rollback();
            } finally {
                real.close();
            }
        }
    }

    private Pedido leer() throws Exception {
        return new PedidoDAO(real).buscarBloqueado(pedido.getIdPedido());
    }

    @Test
    void guardaLineaYOpcionesYPublicaCacheDespuesDelCommit() throws Exception {
        antesCommit = () -> assertTrue(
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .getLineas()
                        .isEmpty()
        );

        servicio.agregarLinea(
                pedido.getIdPedido(),
                productoId,
                List.of(opcionId),
                2,
                "cajero"
        );

        assertEquals(1, aperturas);
        assertEquals(1, cierres);
        assertEquals(1, commits);
        assertEquals(0, rollbacks);

        Pedido guardado = leer();
        assertEquals(1, guardado.getLineas().size());
        assertTrue(
                guardado.getLineas().getFirst().getId() > Integer.MAX_VALUE
        );
        assertEquals(productoId, guardado.getLineas().getFirst().getProductoId());
        assertEquals(
                List.of(opcionId),
                guardado.getLineas().getFirst().getOpcionIds()
        );
        assertEquals(new BigDecimal("2005.00"), guardado.calcularTotal());
        assertEquals(pedido.getFechaPedido(), guardado.getFechaPedido());
        assertEquals(
                guardado.getLineas().getFirst().getId(),
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .getLineas()
                        .getFirst()
                        .getId()
        );
    }

    @Test
    void conservaLasLineasExistentesConSusIdsOriginales() throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(), productoId, List.of(), 1, "cajero"
        );
        long primera = leer().getLineas().getFirst().getId();
        real.commit();

        aperturas = 0;
        servicio.agregarLinea(
                pedido.getIdPedido(), productoId, List.of(opcionId), 2, "cajero"
        );

        Pedido guardado = leer();
        assertEquals(2, guardado.getLineas().size());
        assertEquals(primera, guardado.getLineas().getFirst().getId());
        assertEquals(new BigDecimal("3005.00"), guardado.calcularTotal());
    }

    @Test
    void opcionAjenaRevierteSinCambiarSQLNiCache() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.agregarLinea(
                        pedido.getIdPedido(),
                        productoId,
                        List.of(opcionId + 1),
                        1,
                        "cajero"
                )
        );

        assertEquals(1, rollbacks);
        assertEquals(0, commits);
        assertEquals(1, cierres);
        assertTrue(leer().getLineas().isEmpty());
        assertTrue(
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .getLineas()
                        .isEmpty()
        );
    }

    @Test
    void falloCommitRevierteLaLineaYNoPublicaIdsEnCache() {
        fallarCommit = true;

        assertThrows(
                SQLException.class,
                () -> servicio.agregarLinea(
                        pedido.getIdPedido(),
                        productoId,
                        List.of(opcionId),
                        1,
                        "cajero"
                )
        );

        assertEquals(1, rollbacks);
        assertEquals(1, cierres);
        assertTrue(leer().getLineas().isEmpty());
        assertTrue(
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .getLineas()
                        .isEmpty()
        );
    }

    @Test
    void rechazaPedidoInexistenteYDatosInvalidosSinAbrirConexion()
            throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.agregarLinea(
                        0, productoId, List.of(), 1, "cajero"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.agregarLinea(
                        pedido.getIdPedido(), productoId, List.of(), 0, "cajero"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.agregarLinea(
                        pedido.getIdPedido(),
                        productoId,
                        List.of(opcionId, opcionId),
                        1,
                        "cajero"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.agregarLinea(
                        pedido.getIdPedido(), productoId, null, 1, "cajero"
                )
        );
        assertEquals(0, aperturas);

        assertThrows(
                NoSuchElementException.class,
                () -> servicio.agregarLinea(
                        Long.MAX_VALUE, productoId, List.of(), 1, "cajero"
                )
        );
        assertEquals(0, consultasCatalogo);
    }

    @Test
    void quitarLineaGuardaElBorradorYEliminaSusOpciones() throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(),
                productoId,
                List.of(opcionId),
                1,
                "cajero"
        );
        long lineaId = leer().getLineas().getFirst().getId();
        real.commit();

        servicio.quitarLinea(pedido.getIdPedido(), lineaId, "cajero");

        assertTrue(leer().getLineas().isEmpty());
        assertEquals(0, contar("linea_pedido_opcion"));
        assertTrue(
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .getLineas()
                        .isEmpty()
        );
    }

    @Test
    void quitarLineaAjenaPropagaErrorSinAlterarCache() throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(), productoId, List.of(), 1, "cajero"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.quitarLinea(
                        pedido.getIdPedido(), Long.MAX_VALUE, "cajero"
                )
        );

        assertEquals(1, leer().getLineas().size());
        assertEquals(
                1,
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .getLineas()
                        .size()
        );
        assertEquals(1, rollbacks);
    }

    @Test
    void confirmarDescuentaRecetaBaseYExtrasPorCantidadYConservaPrecios()
            throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(),
                productoId,
                List.of(opcionId),
                2,
                "cajero"
        );

        antesCommit = () -> assertFalse(
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .isConfirmado()
        );

        servicio.confirmar(pedido.getIdPedido(), "cajero");

        assertTrue(leer().isConfirmado());
        assertEquals(Map.of(1L, 0.6, 2L, 0.1), leer().getIngredientesDescontados());
        assertEquals(new BigDecimal("9.400000"), stock(1));
        assertEquals(new BigDecimal("9.900000"), stock(2));
        assertEquals(new BigDecimal("2005.00"), leer().calcularTotal());
        assertEquals(2, contar("movimiento_stock"));
        assertTrue(
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .isConfirmado()
        );

        antesCommit = null;

        assertThrows(
                IllegalStateException.class,
                () -> servicio.confirmar(pedido.getIdPedido(), "cajero")
        );
        assertEquals(2, contar("movimiento_stock"));
    }

    @Test
    void cancelarRestituyeElHistoricoSinConsultarCatalogoNiDuplicarDevoluciones()
            throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(),
                productoId,
                List.of(opcionId),
                2,
                "cajero"
        );
        servicio.confirmar(pedido.getIdPedido(), "cajero");
        catalogoNoDisponible = true;

        servicio.cancelar(pedido.getIdPedido(), "cajero");

        assertEquals(
                EstadoPreparacion.CANCELADO,
                leer().getEstadoPreparacion()
        );
        assertEquals(new BigDecimal("10.000000"), stock(1));
        assertEquals(new BigDecimal("10.000000"), stock(2));
        assertEquals(
                Map.of(1L, 0.6, 2L, 0.1),
                leer().getIngredientesDescontados()
        );
        assertEquals(4, contar("movimiento_stock"));
        assertNull(
                servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido())
        );

        assertThrows(
                IllegalStateException.class,
                () -> servicio.cancelar(pedido.getIdPedido(), "cajero")
        );
        assertEquals(4, contar("movimiento_stock"));
    }

    @Test
    void falloDespuesDelDescuentoReviertePedidoStockYAuditoria()
            throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(),
                productoId,
                List.of(opcionId),
                2,
                "cajero"
        );

        inventarioService inventarioConFallo = new inventarioService(
                proveedor,
                new ingredienteDAO(),
                new movimientoStockDAO()
        ) {
            @Override
            public void descontar(
                    Map<Long, Double> cantidades,
                    long id,
                    String usuario,
                    Connection c
            ) throws SQLException {
                assertSame(ultimaConexion, c);
                super.descontar(cantidades, id, usuario, c);
                throw new SQLException("Fallo posterior al descuento.");
            }
        };

        PedidoService servicioConFallo = new PedidoService(
                proveedor,
                catalogo,
                inventarioConFallo,
                servicio.repositorio
        );

        assertThrows(
                SQLException.class,
                () -> servicioConFallo.confirmar(
                        pedido.getIdPedido(),
                        "cajero"
                )
        );

        assertFalse(leer().isConfirmado());
        assertTrue(leer().getIngredientesDescontados().isEmpty());
        assertEquals(new BigDecimal("10.000000"), stock(1));
        assertEquals(0, contar("movimiento_stock"));
        assertFalse(
                servicio.repositorio
                        .obtenerPedidoPorId(pedido.getIdPedido())
                        .isConfirmado()
        );
    }

    @Test
    void stockInsuficienteRechazaAgregarLineaYUnBorradorSeCancelaSinMovimientos()
            throws Exception {
        assertThrows(
                IllegalStateException.class,
                () -> servicio.agregarLinea(
                        pedido.getIdPedido(),
                        productoId,
                        List.of(opcionId),
                        100,
                        "cajero"
                )
        );

        assertFalse(leer().isConfirmado());
        assertTrue(leer().getLineas().isEmpty());
        assertEquals(0, contar("movimiento_stock"));

        servicio.cancelar(pedido.getIdPedido(), "cajero");

        assertEquals(0, contar("movimiento_stock"));
        assertEquals(
                EstadoPreparacion.CANCELADO,
                leer().getEstadoPreparacion()
        );
    }

    @Test
    void preparacionEntregaYCobroCompartenRepositorio() throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(), productoId, List.of(), 1, "cajero"
        );

        assertThrows(
                IllegalStateException.class,
                () -> servicio.entregar(pedido.getIdPedido(), "cajero")
        );

        servicio.confirmar(pedido.getIdPedido(), "cajero");
        servicio.iniciarPreparacion(pedido.getIdPedido(), "cajero");

        assertEquals(
                EstadoPreparacion.EN_PREPARACION,
                leer().getEstadoPreparacion()
        );

        assertThrows(
                IllegalStateException.class,
                () -> servicio.cancelar(pedido.getIdPedido(), "cajero")
        );

        servicio.entregar(pedido.getIdPedido(), "cajero");

        assertNotNull(
                servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido())
        );

        CobroService cobros = new CobroService(
                servicio.repositorio,
                new ReciboDAO(),
                proveedor
        );

        cobros.cobrar(
                pedido.getIdPedido(),
                MetodoPago.EFECTIVO,
                "cajero"
        );

        assertNull(
                servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido())
        );
        assertEquals(EstadoPago.PAGADO, leer().getEstadoPago());
    }

    @Test
    void ventasYRankingUsanFechaDeCobroYRangoSemiabierto() throws Exception {
        servicio.agregarLinea(
                pedido.getIdPedido(), productoId, List.of(), 3, "cajero"
        );
        servicio.confirmar(pedido.getIdPedido(), "cajero");

        new CobroService(
                servicio.repositorio,
                new ReciboDAO(),
                proveedor
        ).cobrar(
                pedido.getIdPedido(),
                MetodoPago.TARJETA,
                "cajero"
        );

        LocalDateTime fecha = LocalDateTime.of(2026, 10, 25, 12, 30);

        try (var sentencia = real.prepareStatement(
                "UPDATE Recibo SET fecha_recibo=? WHERE id_pedido=?"
        )) {
            sentencia.setObject(1, fecha);
            sentencia.setLong(2, pedido.getIdPedido());
            sentencia.executeUpdate();
        }

        real.commit();

        IConsultaVentas ventas = servicio;
        Map<Long, Long> ranking =
                ventas.rankingProductos(fecha, fecha.plusHours(1));

        assertEquals(Map.of(productoId, 3L), ranking);
        assertThrows(
                UnsupportedOperationException.class,
                () -> ranking.put(productoId, 0L)
        );
        assertEquals(
                1,
                ventas.pedidosPagadosEntre(fecha, fecha.plusHours(1)).size()
        );
        assertTrue(
                ventas.rankingProductos(fecha.minusHours(1), fecha).isEmpty()
        );
        assertTrue(
                ventas.pedidosPagadosEntre(fecha.minusHours(1), fecha).isEmpty()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ventas.rankingProductos(fecha, fecha)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ventas.pedidosPagadosEntre(fecha, fecha.minusDays(1))
        );
    }

    @Test
    void crearDevuelveIdYLasConsultasRecuperanCambiosDeOtrasSesiones()
            throws Exception {
        Pedido creado = servicio.crear("Luis", "cajero");
        assertTrue(creado.getIdPedido() > Integer.MAX_VALUE);
        assertNotNull(
                servicio.repositorio.obtenerPedidoPorId(creado.getIdPedido())
        );

        try (var sentencia = real.prepareStatement(
                "UPDATE pedido SET nombre_cliente='Nombre actualizado' WHERE id=?"
        )) {
            sentencia.setLong(1, creado.getIdPedido());
            sentencia.executeUpdate();
        }

        real.commit();

        assertEquals(
                "Nombre actualizado",
                servicio.obtener(creado.getIdPedido()).getNombreCliente()
        );
        assertTrue(
                servicio.listarActivos()
                        .stream()
                        .anyMatch(p -> p.getNombreCliente()
                                .equals("Nombre actualizado"))
        );
        assertNull(servicio.obtener(Long.MAX_VALUE));
    }

    private BigDecimal stock(long ingredienteId) throws Exception {
        try (var sentencia = real.prepareStatement(
                "SELECT cantidad_disponible FROM ingrediente WHERE id=?"
        )) {
            sentencia.setLong(1, ingredienteId);

            try (var resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getBigDecimal(1);
            }
        }
    }

    private long contar(String tabla) throws Exception {
        if (!Set.of("linea_pedido_opcion", "movimiento_stock")
                .contains(tabla)) {
            throw new IllegalArgumentException();
        }

        try (var sentencia = real.createStatement();
             var resultado = sentencia.executeQuery(
                     "SELECT COUNT(*) FROM " + tabla
             )) {
            resultado.next();
            return resultado.getLong(1);
        }
    }
}
