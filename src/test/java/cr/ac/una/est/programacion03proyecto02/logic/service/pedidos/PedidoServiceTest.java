package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.ICatalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.inventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.*;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.PedidoDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** MySQL real, exclusivamente con tablas TEMPORARY de una sesión aislada. */
@EnabledIfSystemProperty(named="pruebas.mysql", matches="true")
class PedidoServiceTest {
    private static final long PRODUCTO_ID = 6_000_000_001L;
    private static final long OPCION_ID = 7_000_000_001L;
    private Connection real;
    private Pedido pedido;
    private PedidoService servicio;
    private int aperturas, cierres, commits, rollbacks, consultasCatalogo;
    private boolean fallarCommit;
    private Runnable antesCommit;
    private ConexionDB proveedor;
    private ICatalogoService catalogo;
    private Connection ultimaConexion;
    private boolean catalogoNoDisponible;

    @BeforeEach void preparar() throws Exception {
        real = new ConexionDB().getConnection();
        try (var s=real.createStatement()) {
            // Todas las tablas se crean antes de que el servicio pueda leerlas o escribirlas.
            s.executeUpdate("CREATE TEMPORARY TABLE pedido(id BIGINT PRIMARY KEY AUTO_INCREMENT,nombre_cliente VARCHAR(150),fecha_pedido DATETIME(6),confirmado BOOLEAN,estado_preparacion VARCHAR(20),estado_pago VARCHAR(20),usuario_creacion VARCHAR(100)) ENGINE=InnoDB AUTO_INCREMENT=3000000001");
            s.executeUpdate("CREATE TEMPORARY TABLE linea_pedido(id BIGINT PRIMARY KEY AUTO_INCREMENT,pedido_id BIGINT,producto_id BIGINT,nombre_producto VARCHAR(150),nombre_categoria VARCHAR(100),descripcion_opciones VARCHAR(1000),precio_unitario DECIMAL(15,2),cantidad INT) ENGINE=InnoDB AUTO_INCREMENT=4000000001");
            s.executeUpdate("CREATE TEMPORARY TABLE linea_pedido_opcion(linea_pedido_id BIGINT,opcion_producto_id BIGINT,PRIMARY KEY(linea_pedido_id,opcion_producto_id)) ENGINE=InnoDB");
            s.executeUpdate("CREATE TEMPORARY TABLE ingredientes_descontados(pedido_id BIGINT,ingrediente_id BIGINT,cantidad DECIMAL(15,6),PRIMARY KEY(pedido_id,ingrediente_id)) ENGINE=InnoDB");
            s.executeUpdate("CREATE TEMPORARY TABLE Recibo(id_recibo BIGINT PRIMARY KEY AUTO_INCREMENT,id_pedido BIGINT UNIQUE,total_pago DECIMAL(15,2),metodo_pago VARCHAR(20),fecha_recibo DATETIME(6),usuario_recibo VARCHAR(100)) ENGINE=InnoDB");
            s.executeUpdate("CREATE TEMPORARY TABLE ingrediente(id BIGINT PRIMARY KEY,nombre VARCHAR(100),cantidad_disponible DECIMAL(15,6),unidad_medida VARCHAR(20),stock_minimo DECIMAL(15,6),activo BOOLEAN) ENGINE=InnoDB");
            s.executeUpdate("CREATE TEMPORARY TABLE movimiento_stock(id BIGINT PRIMARY KEY AUTO_INCREMENT,ingrediente_id BIGINT,pedido_id BIGINT,tipo_movimiento VARCHAR(30),cantidad DECIMAL(15,6),fecha DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),usuario VARCHAR(100),motivo VARCHAR(255),UNIQUE(pedido_id,ingrediente_id,tipo_movimiento)) ENGINE=InnoDB");
            s.executeUpdate("INSERT INTO ingrediente VALUES(1,'Harina',10,'GRAMO',1,TRUE),(2,'Queso',10,'GRAMO',1,TRUE)");
        }
        real.setAutoCommit(false);
        pedido = new PedidoDAO(real).insertarBorrador("Ana","creador");
        real.commit();
        proveedor = new ConexionDB() {
            @Override public Connection getConnection() {
                aperturas++;
                AtomicBoolean cerrada = new AtomicBoolean();
                ultimaConexion = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)-> {
                    if(m.getName().equals("close")) {cierres++;cerrada.set(true);return null;}
                    if(cerrada.get()) throw new SQLException("Se reutilizó una conexión cerrada.");
                    if(m.getName().equals("commit")) {
                        if(antesCommit!=null) antesCommit.run();
                        if(fallarCommit) throw new SQLException("Fallo simulado de commit.");
                        commits++;
                    }
                    if(m.getName().equals("rollback") && (a==null || a.length==0)) rollbacks++;
                    try {return m.invoke(real,a);} catch(InvocationTargetException e) {throw e.getCause();}
                });
                return ultimaConexion;
            }
        };
        catalogo = (ICatalogoService) Proxy.newProxyInstance(
                ICatalogoService.class.getClassLoader(),new Class<?>[]{ICatalogoService.class},(p,m,a)-> {
                    if(!m.getName().equals("obtenerProducto") || a.length!=2) throw new AssertionError("Consulta inesperada: "+m.getName());
                    consultasCatalogo++;
                    if (catalogoNoDisponible) throw new SQLException("Catalogo fuera de servicio.");
                    assertEquals(PRODUCTO_ID,a[0]);
                    assertFalse(((Connection)a[1]).getAutoCommit());
                    assertSame(ultimaConexion,a[1],"Catálogo debe usar la conexión de la operación");
                    Producto producto = new Producto(PRODUCTO_ID,"Hamburguesa",new BigDecimal("1000.00"),new Categoria(1,"Comidas",true),true);
                    producto.setReceta(List.of(new recetaItem(1,.1),new recetaItem(2,.05)));
                    OpcionProducto opcion = new OpcionProducto(OPCION_ID,"Queso",TipoOpcion.EXTRA,new BigDecimal("2.50"),true);
                    opcion.setReceta(List.of(new recetaItem(1,.2)));
                    producto.getOpciones().add(opcion);
                    return producto;
                });
        servicio = new PedidoService(proveedor,catalogo,new inventarioService(proveedor,new ingredienteDAO(),new movimientoStockDAO()));
        aperturas=cierres=commits=rollbacks=0;
    }
    @AfterEach void cerrar() throws Exception {if(real!=null){try{real.rollback();}finally{real.close();}}}
    private Pedido leer() throws Exception {return new PedidoDAO(real).buscarBloqueado(pedido.getIdPedido());}

    @Test void guardaLineaYOpcionesYPublicaCacheDespuesDelCommit() throws Exception {
        antesCommit=()->assertTrue(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getLineas().isEmpty());
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),2,"cajero");
        assertEquals(1,aperturas);assertEquals(1,cierres);assertEquals(1,commits);assertEquals(0,rollbacks);
        Pedido guardado=leer();
        assertEquals(1,guardado.getLineas().size());
        assertTrue(guardado.getLineas().getFirst().getId()>Integer.MAX_VALUE);
        assertEquals(PRODUCTO_ID,guardado.getLineas().getFirst().getProductoId());
        assertEquals(List.of(OPCION_ID),guardado.getLineas().getFirst().getOpcionIds());
        assertEquals(new BigDecimal("2005.00"),guardado.calcularTotal());
        assertEquals(pedido.getFechaPedido(),guardado.getFechaPedido());
        assertEquals(guardado.getLineas().getFirst().getId(),servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getLineas().getFirst().getId());
    }

    @Test void conservaLasLineasExistentesConSusIdsOriginales() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(),1,"cajero");
        long primera=leer().getLineas().getFirst().getId();
        real.commit();
        aperturas=0;
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),2,"cajero");
        Pedido guardado=leer();
        assertEquals(2,guardado.getLineas().size());
        assertEquals(primera,guardado.getLineas().getFirst().getId());
        assertEquals(new BigDecimal("3005.00"),guardado.calcularTotal());
    }

    @Test void opcionAjenaRevierteSinCambiarSQLNiCache() throws Exception {
        assertThrows(IllegalArgumentException.class,()->servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID+1),1,"cajero"));
        assertEquals(1,rollbacks);assertEquals(0,commits);assertEquals(1,cierres);
        assertTrue(leer().getLineas().isEmpty());
        assertTrue(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getLineas().isEmpty());
    }

    @Test void falloCommitRevierteLaLineaYNoPublicaIdsEnCache() throws Exception {
        fallarCommit=true;
        assertThrows(SQLException.class,()->servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),1,"cajero"));
        assertEquals(1,rollbacks);assertEquals(1,cierres);
        assertTrue(leer().getLineas().isEmpty());
        assertTrue(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getLineas().isEmpty());
    }

    @Test void rechazaPedidoInexistenteYDatosInvalidosSinAbrirConexion() throws Exception {
        assertThrows(IllegalArgumentException.class,()->servicio.agregarLinea(0,PRODUCTO_ID,List.of(),1,"cajero"));
        assertThrows(IllegalArgumentException.class,()->servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(),0,"cajero"));
        assertThrows(IllegalArgumentException.class,()->servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID,OPCION_ID),1,"cajero"));
        assertThrows(IllegalArgumentException.class,()->servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,null,1,"cajero"));
        assertEquals(0,aperturas);
        assertThrows(NoSuchElementException.class,()->servicio.agregarLinea(Long.MAX_VALUE,PRODUCTO_ID,List.of(),1,"cajero"));
        assertEquals(0,consultasCatalogo);
    }

    @Test void quitarLineaGuardaElBorradorYEliminaSusOpciones() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),1,"cajero");
        long lineaId=leer().getLineas().getFirst().getId();
        real.commit();
        servicio.quitarLinea(pedido.getIdPedido(),lineaId,"cajero");
        assertTrue(leer().getLineas().isEmpty());
        assertEquals(0,contar("linea_pedido_opcion"));
        assertTrue(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getLineas().isEmpty());
    }

    @Test void quitarLineaAjenaPropagaErrorSinAlterarCache() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(),1,"cajero");
        assertThrows(IllegalArgumentException.class,()->servicio.quitarLinea(pedido.getIdPedido(),Long.MAX_VALUE,"cajero"));
        assertEquals(1,leer().getLineas().size());
        assertEquals(1,servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).getLineas().size());
        assertEquals(1,rollbacks);
    }

    @Test void confirmarDescuentaRecetaBaseYExtrasPorCantidadYConservaPrecios() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),2,"cajero");
        antesCommit=()->assertFalse(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).isConfirmado());
        servicio.confirmar(pedido.getIdPedido(),"cajero");
        assertTrue(leer().isConfirmado());
        assertEquals(Map.of(1L,.6,2L,.1),leer().getIngredientesDescontados());
        assertEquals(new BigDecimal("9.400000"),stock(1));
        assertEquals(new BigDecimal("9.900000"),stock(2));
        assertEquals(new BigDecimal("2005.00"),leer().calcularTotal());
        assertEquals(2,contar("movimiento_stock"));
        assertTrue(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).isConfirmado());
        antesCommit=null;
        assertThrows(IllegalStateException.class,()->servicio.confirmar(pedido.getIdPedido(),"cajero"));
        assertEquals(2,contar("movimiento_stock"));
    }

    @Test void cancelarRestituyeElHistoricoSinConsultarCatalogoNiDuplicarDevoluciones() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),2,"cajero");
        servicio.confirmar(pedido.getIdPedido(),"cajero");
        catalogoNoDisponible=true;
        servicio.cancelar(pedido.getIdPedido(),"cajero");
        assertEquals(EstadoPreparacion.CANCELADO,leer().getEstadoPreparacion());
        assertEquals(new BigDecimal("10.000000"),stock(1));
        assertEquals(new BigDecimal("10.000000"),stock(2));
        assertEquals(Map.of(1L,.6,2L,.1),leer().getIngredientesDescontados());
        assertEquals(4,contar("movimiento_stock"));
        assertNull(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()));
        assertThrows(IllegalStateException.class,()->servicio.cancelar(pedido.getIdPedido(),"cajero"));
        assertEquals(4,contar("movimiento_stock"));
    }

    @Test void falloDespuesDelDescuentoReviertePedidoStockYAuditoria() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),2,"cajero");
        inventarioService inventario = new inventarioService(proveedor,new ingredienteDAO(),new movimientoStockDAO()) {
            @Override public void descontar(Map<Long,Double> cantidades,long id,String usuario,Connection c) throws SQLException {
                assertSame(ultimaConexion,c);
                super.descontar(cantidades,id,usuario,c);
                throw new SQLException("Fallo posterior al descuento.");
            }
        };
        PedidoService conFallo = new PedidoService(proveedor,catalogo,inventario,servicio.repositorio);
        assertThrows(SQLException.class,()->conFallo.confirmar(pedido.getIdPedido(),"cajero"));
        assertFalse(leer().isConfirmado());
        assertTrue(leer().getIngredientesDescontados().isEmpty());
        assertEquals(new BigDecimal("10.000000"),stock(1));
        assertEquals(0,contar("movimiento_stock"));
        assertFalse(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()).isConfirmado());
    }

    @Test void stockInsuficienteNoConfirmaYUnBorradorSeCancelaSinMovimientos() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(OPCION_ID),100,"cajero");
        assertThrows(IllegalStateException.class,()->servicio.confirmar(pedido.getIdPedido(),"cajero"));
        assertFalse(leer().isConfirmado());
        assertEquals(0,contar("movimiento_stock"));
        servicio.cancelar(pedido.getIdPedido(),"cajero");
        assertEquals(0,contar("movimiento_stock"));
        assertEquals(EstadoPreparacion.CANCELADO,leer().getEstadoPreparacion());
    }

    @Test void preparacionEntregaYCobroCompartenRepositorio() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(),1,"cajero");
        assertThrows(IllegalStateException.class,()->servicio.entregar(pedido.getIdPedido(),"cajero"));
        servicio.confirmar(pedido.getIdPedido(),"cajero");
        servicio.iniciarPreparacion(pedido.getIdPedido(),"cajero");
        assertEquals(EstadoPreparacion.EN_PREPARACION,leer().getEstadoPreparacion());
        assertThrows(IllegalStateException.class,()->servicio.cancelar(pedido.getIdPedido(),"cajero"));
        servicio.entregar(pedido.getIdPedido(),"cajero");
        assertNotNull(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()));
        CobroService cobros = new CobroService(servicio.repositorio,new ReciboDAO(),proveedor);
        cobros.cobrar(pedido.getIdPedido(),MetodoPago.EFECTIVO,"cajero");
        assertNull(servicio.repositorio.obtenerPedidoPorId(pedido.getIdPedido()));
        assertEquals(EstadoPago.PAGADO,leer().getEstadoPago());
    }

    @Test void ventasYRankingUsanFechaDeCobroYRangoSemiabierto() throws Exception {
        servicio.agregarLinea(pedido.getIdPedido(),PRODUCTO_ID,List.of(),3,"cajero");
        servicio.confirmar(pedido.getIdPedido(),"cajero");
        new CobroService(servicio.repositorio,new ReciboDAO(),proveedor)
                .cobrar(pedido.getIdPedido(),MetodoPago.TARJETA,"cajero");
        LocalDateTime fecha=LocalDateTime.of(2026,10,25,12,30);
        try(var s=real.prepareStatement("UPDATE Recibo SET fecha_recibo=? WHERE id_pedido=?")) {
            s.setObject(1,fecha);s.setLong(2,pedido.getIdPedido());s.executeUpdate();
        }
        real.commit();
        IConsultaVentas ventas=servicio;
        Map<Long,Long> ranking=ventas.rankingProductos(fecha,fecha.plusHours(1));
        assertEquals(Map.of(PRODUCTO_ID,3L),ranking);
        assertThrows(UnsupportedOperationException.class,()->ranking.put(PRODUCTO_ID,0L));
        assertEquals(1,ventas.pedidosPagadosEntre(fecha,fecha.plusHours(1)).size());
        assertTrue(ventas.rankingProductos(fecha.minusHours(1),fecha).isEmpty());
        assertTrue(ventas.pedidosPagadosEntre(fecha.minusHours(1),fecha).isEmpty());
        assertThrows(IllegalArgumentException.class,()->ventas.rankingProductos(fecha,fecha));
        assertThrows(IllegalArgumentException.class,()->ventas.pedidosPagadosEntre(fecha,fecha.minusDays(1)));
    }

    @Test void crearDevuelveIdYLasConsultasRecuperanCambiosDeOtrasSesiones() throws Exception {
        Pedido creado=servicio.crear("Luis","cajero");
        assertTrue(creado.getIdPedido()>Integer.MAX_VALUE);
        assertNotNull(servicio.repositorio.obtenerPedidoPorId(creado.getIdPedido()));
        try(var s=real.prepareStatement("UPDATE pedido SET nombre_cliente='Nombre actualizado' WHERE id=?")) {
            s.setLong(1,creado.getIdPedido());s.executeUpdate();
        }
        real.commit();
        assertEquals("Nombre actualizado",servicio.obtener(creado.getIdPedido()).getNombreCliente());
        assertTrue(servicio.listarActivos().stream().anyMatch(p->p.getNombreCliente().equals("Nombre actualizado")));
        assertNull(servicio.obtener(Long.MAX_VALUE));
    }

    private BigDecimal stock(long id) throws Exception {
        try(var s=real.prepareStatement("SELECT cantidad_disponible FROM ingrediente WHERE id=?")) {
            s.setLong(1,id);try(var r=s.executeQuery()){r.next();return r.getBigDecimal(1);}
        }
    }
    private long contar(String tabla) throws Exception {
        if(!Set.of("linea_pedido_opcion","movimiento_stock").contains(tabla)) throw new IllegalArgumentException();
        try(var s=real.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+tabla)){r.next();return r.getLong(1);}
    }
}
