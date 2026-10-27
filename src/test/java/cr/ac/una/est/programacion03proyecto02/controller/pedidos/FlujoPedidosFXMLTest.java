package cr.ac.una.est.programacion03proyecto02.controller.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.inventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.*;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.*;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Carga los FXML y usa sus botones reales. Los servicios simulados nunca acceden a MySQL. */
@EnabledIfSystemProperty(named="pruebas.javafx", matches="true")
class FlujoPedidosFXMLTest {
    private Stage ventana;
    private NavegadorVistas navegador;
    private SesionActual sesion;
    private PedidosSimulados pedidos;
    private int cobros;
    private boolean perderRespuestaCobro;
    private CountDownLatch liberarCobro;
    private static final long PRODUCTO_ID=6_000_000_001L, OPCION_ID=7_000_000_001L;
    private final ConexionDB conexiones = new ConexionDB() {
        @Override public Connection getConnection() { throw new AssertionError("Las pruebas FXML no deben abrir conexiones reales."); }
    };

    @BeforeAll static void iniciarJavaFX() throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        try { Platform.startup(() -> { Platform.setImplicitExit(false); listo.countDown(); }); }
        catch (IllegalStateException yaIniciado) { Platform.runLater(listo::countDown); }
        assertTrue(listo.await(10,TimeUnit.SECONDS));
    }
    @AfterAll static void terminarJavaFX() { Platform.exit(); }

    @BeforeEach void preparar() throws Exception {
        inventarioService inventario = new inventarioService(conexiones,new ingredienteDAO(),new movimientoStockDAO());
        catalogoService catalogo = new catalogoService(conexiones,new productoDAO(),new categoriaDAO(),inventario) {
            @Override public List<Categoria> listarCategorias() { segundoPlano(); return List.of(new Categoria(1,"Comidas",true)); }
            @Override public List<Producto> listarProductosActivos() { segundoPlano(); return List.of(producto()); }
        };
        PedidoRepository repositorio = new PedidoRepository(conexiones) {
            @Override public synchronized void recargarActivos() { }
        };
        pedidos = new PedidosSimulados(catalogo,inventario,repositorio);
        CobroService pagos = new CobroService(repositorio,new ReciboDAO(),conexiones) {
            @Override public Recibo cobrar(long id,MetodoPago metodo,String usuario) throws SQLException {
                segundoPlano(); cobros++;
                if (liberarCobro != null) {
                    try { if (!liberarCobro.await(10, TimeUnit.SECONDS)) throw new SQLException("Tiempo de prueba agotado."); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new SQLException(error); }
                }
                Pedido p=pedidos.datos.get(id);
                Recibo recibo=new Recibo(8_000_000_001L,id,new Pago(metodo,p.calcularTotal(),LocalDateTime.now(),usuario),p.getLineas());
                p.registrarCobro(recibo);
                if (perderRespuestaCobro) throw new SQLException("Se perdió la respuesta después del commit.");
                return recibo;
            }
        };
        fx(() -> {
            sesion=new SesionActual();
            sesion.iniciar(new Usuario(1,"cajero","hash-prueba",Rol.CAJERO,true));
            ventana=new Stage();
            // Ventana de prueba invisible: no interfiere con las aplicaciones del usuario.
            ventana.initStyle(StageStyle.UTILITY); ventana.setOpacity(0); ventana.setX(-10000); ventana.setY(-10000);
            navegador=new NavegadorVistas();
            navegador.configurar(ventana,catalogo,inventario,sesion);
            navegador.configurarPedidos(pedidos,pagos);
        });
    }
    @AfterEach void cerrarVentana() throws Exception { fx(() -> ventana.close()); }

    @Test void creaAgregaConfirmaCobraMuestraReciboYEntregaDesdeLosFXML() throws Exception {
        nuevoConProducto();
        fx(() -> { seleccionarProducto(); texto("txtCantidad","2"); boton("btnAgregar").fire(); boton("btnAgregar").fire(); });
        esperar("#tblLineas");
        fx(() -> {
            assertEquals(1,tabla("tblLineas").getItems().size());
            assertEquals("2001.00",label("lblTotal").getText());
            assertEquals(1,pedidos.agregados,"Un doble clic no debe guardar dos lineas");
            capturar("toma_pedido");
            boton("btnConfirmar").fire();
        });
        esperar("#tblLineas");
        fx(() -> { assertTrue(boton("btnAgregar").isDisabled()); assertFalse(boton("btnCobrar").isDisabled()); boton("btnCobrar").fire(); });
        esperar("#chMetodoPago");
        fx(() -> {
            assertEquals("3000000001",label("lblPedido").getText());
            assertTrue(boton("btnCobrar").isDisabled());
            ((ChoiceBox<MetodoPago>)nodo("chMetodoPago")).setValue(MetodoPago.TARJETA);
            capturar("cobro");
            boton("btnCobrar").fire(); boton("btnCobrar").fire();
        });
        esperar("#lblIdRecibo");
        fx(() -> {
            assertEquals(1,cobros);
            assertEquals("2001.00",label("lblTotal").getText());
            assertEquals("8000000001",label("lblIdRecibo").getText());
            capturar("recibo");
            root().lookupAll(".button").stream().map(n->(Button)n).filter(b->b.getText().equals("Cerrar recibo")).findFirst().orElseThrow().fire();
        });
        esperar("#tblPedidos");
        fx(() -> { tabla("tblPedidos").getSelectionModel().selectFirst(); capturar("pedidos_activos"); boton("btnPreparar").fire(); });
        esperar("#tblPedidos");
        fx(() -> { tabla("tblPedidos").getSelectionModel().selectFirst(); assertFalse(boton("btnEntregar").isDisabled()); boton("btnEntregar").fire(); });
        esperar("#tblPedidos");
        fx(() -> assertTrue(tabla("tblPedidos").getItems().isEmpty()));
    }

    @Test void permiteQuitarLineasYCancelarUnBorradorDesdeLaLista() throws Exception {
        nuevoConProducto();
        fx(() -> { seleccionarProducto(); boton("btnAgregar").fire(); });
        esperar("#tblLineas");
        fx(() -> { tabla("tblLineas").getSelectionModel().selectFirst(); boton("btnQuitar").fire(); });
        esperar("#tblLineas");
        fx(() -> { assertTrue(tabla("tblLineas").getItems().isEmpty()); assertTrue(boton("btnConfirmar").isDisabled()); boton("btnVolver").fire(); });
        esperar("#tblPedidos");
        fx(() -> { tabla("tblPedidos").getSelectionModel().selectFirst(); assertTrue(boton("btnCobrar").isDisabled()); boton("btnCancelar").fire(); });
        esperar("#tblPedidos");
        fx(() -> assertTrue(tabla("tblPedidos").getItems().isEmpty()));
    }

    @Test void unFalloDeGuardadoExigeRecargarAntesDeReintentar() throws Exception {
        nuevoConProducto();
        pedidos.fallarAgregar=true;
        fx(() -> { seleccionarProducto(); boton("btnAgregar").fire(); });
        esperar("#tblLineas");
        fx(() -> { assertTrue(boton("btnAgregar").isDisabled()); assertTrue(label("lblEstado").getText().contains("Actualice")); });
        pedidos.fallarAgregar=false;
        fx(() -> boton("btnActualizar").fire());
        esperar("#tblLineas");
        fx(() -> { seleccionarProducto(); assertFalse(boton("btnAgregar").isDisabled()); assertTrue(tabla("tblLineas").getItems().isEmpty()); });
    }

    @Test void abrirPedidoExistenteEntregaSuIdAunqueElCatalogoEsteCargando() throws Exception {
        pedidos.datos.put(3_000_000_099L,new Pedido(3_000_000_099L,"Cliente existente"));
        fx(() -> navegador.abrirPedido(3_000_000_099L));
        esperar("#tblLineas");
        fx(() -> {
            assertEquals("3000000099",label("lblPedido").getText());
            assertEquals("Cliente existente",((TextField)nodo("txtCliente")).getText());
            assertTrue(boton("btnCrear").isDisabled());
        });
    }

    @Test void rechazaAccesoDelRolDeInventario() throws Exception {
        fx(() -> {
            sesion.cerrar();
            sesion.iniciar(new Usuario(2,"stock","hash-prueba",Rol.STOCK_MANAGER,true));
            assertThrows(SecurityException.class,navegador::mostrarPedidosActivos);
            assertThrows(SecurityException.class,()->navegador.abrirPedido(0));
            assertThrows(SecurityException.class,()->navegador.abrirCobro(1));
        });
    }

    @Test void pagoConRespuestaPerdidaNoSeRepiteYActualizarDetectaElPago() throws Exception {
        nuevoConProducto();
        fx(() -> { seleccionarProducto(); boton("btnAgregar").fire(); }); esperar("#tblLineas");
        fx(() -> boton("btnConfirmar").fire()); esperar("#tblLineas");
        fx(() -> boton("btnCobrar").fire()); esperar("#chMetodoPago");
        perderRespuestaCobro=true;
        fx(() -> { ((ChoiceBox<MetodoPago>)nodo("chMetodoPago")).setValue(MetodoPago.EFECTIVO);boton("btnCobrar").fire(); });
        esperar("#chMetodoPago");
        fx(() -> { assertTrue(boton("btnCobrar").isDisabled());boton("btnCobrar").fire();assertEquals(1,cobros);boton("btnActualizar").fire(); });
        esperar("#chMetodoPago");
        fx(() -> { assertTrue(boton("btnCobrar").isDisabled());assertTrue(label("lblMensaje").getText().contains("no está disponible"));assertEquals(1,cobros); });
    }

    @Test void noCierraLaVentanaNiCambiaPantallaDuranteElCobro() throws Exception {
        nuevoConProducto();
        fx(() -> { seleccionarProducto(); boton("btnAgregar").fire(); }); esperar("#tblLineas");
        fx(() -> boton("btnConfirmar").fire()); esperar("#tblLineas");
        fx(() -> boton("btnCobrar").fire()); esperar("#chMetodoPago");
        liberarCobro = new CountDownLatch(1);
        try {
            fx(() -> {
                ((ChoiceBox<MetodoPago>)nodo("chMetodoPago")).setValue(MetodoPago.EFECTIVO);
                boton("btnCobrar").fire();
                assertTrue(root().isDisabled());
                assertThrows(IllegalStateException.class,navegador::mostrarPedidosActivos);
                javafx.stage.WindowEvent cierre = new javafx.stage.WindowEvent(ventana,javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST);
                javafx.event.Event.fireEvent(ventana,cierre);
                assertTrue(cierre.isConsumed());
            });
        } finally { liberarCobro.countDown(); }
        esperar("#lblIdRecibo");
    }

    private void capturar(String nombre) {
        if (!Boolean.getBoolean("pruebas.capturas")) return;
        javafx.scene.image.WritableImage imagen=root().snapshot(null,null);
        int ancho=(int)imagen.getWidth(),alto=(int)imagen.getHeight();
        byte[] pixeles=new byte[ancho*alto*4];
        imagen.getPixelReader().getPixels(0,0,ancho,alto,javafx.scene.image.PixelFormat.getByteBgraInstance(),pixeles,0,ancho*4);
        for(int i=0;i<pixeles.length;i+=4){byte azul=pixeles[i];pixeles[i]=pixeles[i+2];pixeles[i+2]=azul;}
        try {
            java.nio.file.Path dir=java.nio.file.Path.of("target/pedidos-gui");java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.write(dir.resolve(nombre+".rgba"),pixeles);
            java.nio.file.Files.writeString(dir.resolve(nombre+".json"),"{\"width\":"+ancho+",\"height\":"+alto+"}");
        } catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }

    private void nuevoConProducto() throws Exception {
        fx(() -> navegador.abrirPedido(0)); esperar("#tblProductos");
        fx(() -> { texto("txtCliente","Ana"); boton("btnCrear").fire(); }); esperar("#tblLineas");
    }
    private void seleccionarProducto() {
        tabla("tblProductos").getSelectionModel().selectFirst();
        ((ListView<?>)nodo("lstOpciones")).getSelectionModel().selectFirst();
    }
    private void esperar(String selector) throws Exception {
        for(int i=0;i<600;i++) {
            boolean[] listo={false};
            fx(() -> { if(ventana.getScene()!=null){root().applyCss();root().layout();listo[0]=root().lookup(selector)!=null&&!root().isDisabled();} });
            if(listo[0])return;
            Thread.sleep(15);
        }
        fail("No terminó la carga de "+selector);
    }
    private Parent root() { return ventana.getScene().getRoot(); }
    private javafx.scene.Node nodo(String id) { return Objects.requireNonNull(root().lookup("#"+id),id); }
    private Button boton(String id) { return (Button)nodo(id); }
    private Label label(String id) { return (Label)nodo(id); }
    private TableView<?> tabla(String id) { return (TableView<?>)nodo(id); }
    private void texto(String id,String valor) { ((TextField)nodo(id)).setText(valor); }
    private static void fx(Runnable accion) throws Exception {
        FutureTask<Void> tarea=new FutureTask<>(()->{accion.run();return null;});
        Platform.runLater(tarea);
        try { tarea.get(10,TimeUnit.SECONDS); }
        catch(ExecutionException error) { if(error.getCause() instanceof AssertionError a)throw a;throw new RuntimeException(error.getCause()); }
    }
    private static void segundoPlano() { assertFalse(Platform.isFxApplicationThread(),"El servicio no debe ejecutarse en el hilo JavaFX"); }
    private Producto producto() {
        Producto p=new Producto(PRODUCTO_ID,"Hamburguesa",new BigDecimal("1000.00"),new Categoria(1,"Comidas",true),true);
        p.setDisponible(true);
        p.getOpciones().add(new OpcionProducto(OPCION_ID,"Queso",TipoOpcion.EXTRA,new BigDecimal("0.50"),true));
        return p;
    }

    private final class PedidosSimulados extends PedidoService {
        private final Map<Long,Pedido> datos=new LinkedHashMap<>();
        private long siguiente=3_000_000_000L, lineaId=4_000_000_000L;
        private int agregados;
        private boolean fallarAgregar;
        PedidosSimulados(catalogoService c,inventarioService i,PedidoRepository r) throws SQLException { super(conexiones,c,i,r); }
        @Override public Pedido crear(String nombre,String usuario) { segundoPlano();Pedido p=new Pedido(++siguiente,nombre);datos.put(p.getIdPedido(),p);return copiar(p); }
        @Override public Pedido obtener(Long id) { segundoPlano();return datos.get(id)==null?null:copiar(datos.get(id)); }
        @Override public List<Pedido> listarActivos() {
            segundoPlano();return datos.values().stream().filter(p->p.getEstadoPreparacion()!=EstadoPreparacion.CANCELADO
                    && !(p.getEstadoPreparacion()==EstadoPreparacion.ENTREGADO&&p.getEstadoPago()==EstadoPago.PAGADO)).map(this::copiar).toList();
        }
        @Override public void agregarLinea(long id,Long productoId,List<Long> opciones,int cantidad,String usuario) throws SQLException {
            segundoPlano();if(fallarAgregar)throw new SQLException("Fallo simulado");agregados++;
            Producto p=producto();
            datos.get(id).agregarLinea(new lineaPedido(++lineaId,p,p.getOpciones().stream().filter(o->opciones.contains(o.getId())).toList(),cantidad));
        }
        @Override public void quitarLinea(Long id,Long linea,String usuario) { segundoPlano();Pedido p=datos.get(id);p.eliminarLinea(p.getLineas().stream().filter(l->l.getId()==linea).findFirst().orElseThrow()); }
        @Override public void confirmar(Long id,String usuario) { segundoPlano();datos.get(id).confirmar(); }
        @Override public void cancelar(Long id,String usuario) { segundoPlano();datos.get(id).cancelarPedido(); }
        @Override public void iniciarPreparacion(Long id,String usuario) { segundoPlano();datos.get(id).iniciarPreparacion(); }
        @Override public void entregar(Long id,String usuario) { segundoPlano();datos.get(id).entregarPedido(); }
        private Pedido copiar(Pedido p) {return Pedido.reconstruir(p.getIdPedido(),p.getNombreCliente(),p.getFechaPedido(),p.isConfirmado(),p.getEstadoPreparacion(),p.getEstadoPago(),p.getLineas(),p.getIngredientesDescontados(),p.getRecibo());}
    }
}
