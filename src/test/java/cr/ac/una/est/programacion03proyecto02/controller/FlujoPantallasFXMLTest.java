package cr.ac.una.est.programacion03proyecto02.controller;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.ResumenUsuario;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.UsuariosService;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.seguridad.UsuarioDAO;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.*;
import cr.ac.una.est.programacion03proyecto02.logic.service.reportes.ReporteService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;
import cr.ac.una.est.programacion03proyecto02.support.JavaFxTestSupport;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import cr.ac.una.est.programacion03proyecto02.view.personalizacion.*;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Navega por los FXML reales con servicios simulados; no abre conexiones SQL. */
@EnabledIfSystemProperty(named="pruebas.javafx", matches="true")
class FlujoPantallasFXMLTest {
    private Stage ventana;
    private NavegadorVistas navegador;
    private SesionActual sesion;
    private final InventarioSimulado inventario = new InventarioSimulado();
    private final AtomicInteger consultasReporte = new AtomicInteger();
    private boolean fallarReporte;
    private boolean fallarAutenticacion;
    private Usuario usuarioAutenticado = usuario(Rol.ADMIN);
    private Recibo recibo;
    private Pedido pedido;
    private final List<ResumenUsuario> cuentas=new CopyOnWriteArrayList<>();
    private final ConexionDB conexiones = new ConexionDB() {
        @Override public Connection getConnection() { throw new AssertionError("Una prueba visual no debe abrir MySQL."); }
    };

    @BeforeAll static void iniciar() throws Exception { JavaFxTestSupport.iniciar(); }

    @BeforeEach void preparar() throws Exception {
        Categoria categoria = new Categoria(1,"Comidas típicas",true);
        Producto producto = new Producto(7,"Casado con pollo",new BigDecimal("3500.00"),categoria,true);
        producto.setDisponible(true);
        producto.getReceta().add(new recetaItem(1,200));
        OpcionProducto extra=new OpcionProducto(9,"Pollo extra",cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion.EXTRA,new BigDecimal("500"),true);
        extra.setReceta(List.of(new recetaItem(1,100)));producto.getOpciones().add(extra);
        catalogoService catalogo = new catalogoService(conexiones,new productoDAO(),new categoriaDAO(),inventario) {
            @Override public List<Categoria> listarCategorias() { segundoPlano(); return List.of(categoria); }
            @Override public List<Categoria> listarCategoriasAdministracion() { segundoPlano(); return List.of(categoria); }
            @Override public List<Producto> listarProductosActivos() { segundoPlano(); return List.of(producto); }
            @Override public Producto obtenerProducto(long id) { segundoPlano(); return producto; }
            @Override public boolean hayStockSuficientePara(long id,List<Long> opciones,int cantidad) { segundoPlano(); return true; }
            @Override public boolean hayStockSuficientePara(long id,Map<Long,Integer> opciones,int cantidad) { segundoPlano(); return true; }
        };
        pedido = new Pedido(42,"Ana");
        pedido.agregarLinea(new lineaPedido(51,producto,List.of(),1));
        pedido.confirmar();
        recibo = new Recibo(88,42,new Pago(MetodoPago.TARJETA,pedido.calcularTotal(),LocalDateTime.now(),"cajero"),pedido.getLineas());
        pedido.registrarCobro(recibo);
        PedidoRepository repositorio = new PedidoRepository(conexiones) {
            @Override public synchronized void recargarActivos() { }
        };
        PedidoService pedidos = new PedidoService(conexiones,catalogo,inventario,repositorio) {
            @Override public Pedido obtener(Long id) { segundoPlano(); return pedido; }
            @Override public List<Pedido> listarActivos() { segundoPlano(); return List.of(pedido); }
            @Override public List<Pedido> pedidosPagadosEntre(LocalDateTime desde,LocalDateTime hasta) throws SQLException {
                segundoPlano(); consultasReporte.incrementAndGet();
                if(fallarReporte) throw new SQLException("Fallo de consulta simulado");
                return List.of(pedido);
            }
            @Override public Map<Long,Long> rankingProductos(LocalDateTime desde,LocalDateTime hasta) { segundoPlano(); return Map.of(7L,1L); }
        };
        JavaFxTestSupport.ejecutar(() -> {
            ventana = new Stage(StageStyle.UTILITY);
            ventana.setOpacity(0); ventana.setX(-10000); ventana.setY(-10000);
            sesion = new SesionActual();
            navegador = new NavegadorVistas();
            navegador.configurar(ventana,catalogo,inventario,sesion);
            navegador.configurarPedidos(pedidos,new CobroService(repositorio,new ReciboDAO(),conexiones));
            navegador.configurarPantallas((nombre,clave) -> {
                segundoPlano();
                if(fallarAutenticacion) throw new SQLException("Fallo de autenticación simulado");
                return usuarioAutenticado;
            },new ReporteService(pedidos));
            navegador.configurarUsuarios(new UsuariosService(new UsuarioDAO(conexiones) {
                @Override public List<ResumenUsuario> listar() {segundoPlano();return List.copyOf(cuentas);}
                @Override public long crear(String nombre,String hash,Rol rol,boolean activo) {
                    segundoPlano(); assertNotEquals("clave-prueba",hash);
                    cuentas.add(new ResumenUsuario(cuentas.size()+1,nombre,rol,activo)); return cuentas.size();
                }
            },sesion));
        });
    }

    @AfterEach void cerrar() throws Exception { fx(() -> ventana.close()); }

    @Test void pantallasPermitenCambiarTresDisenosSinPerderLaSeleccion() throws Exception {
        fx(()->javafx.css.CssParser.errorsProperty().clear());
        for(String vista : List.of("login","menu","usuarios","inventario","reportes","categorias","productos","editor","pedidos","toma","cobro","recibo")) {
            TemaVisual[] anterior = new TemaVisual[1];
            fx(() -> {
                anterior[0] = navegador.getTemaActual();
                sesion.iniciar(usuario(Set.of("pedidos","toma","cobro","recibo").contains(vista)?Rol.CAJERO:Rol.ADMIN));
                switch(vista) {
                    case "login" -> navegador.mostrarLogin();
                    case "menu" -> navegador.mostrarMenu();
                    case "usuarios" -> navegador.mostrarUsuarios();
                    case "inventario" -> navegador.mostrarInventario();
                    case "reportes" -> navegador.mostrarReportes();
                    case "categorias" -> navegador.abrir("catalogo/categorias.fxml");
                    case "productos" -> navegador.mostrarProductos();
                    case "editor" -> navegador.mostrarProductoEditor(0);
                    case "pedidos" -> navegador.mostrarPedidosActivos();
                    case "toma" -> navegador.abrirPedido(0);
                    case "cobro" -> navegador.abrirCobro(42);
                    case "recibo" -> navegador.mostrarRecibo(recibo);
                }
            });
            esperar();
            fx(() -> {
                Scene escena = ventana.getScene();
                Parent raiz = root();
                assertEquals(anterior[0],navegador.getTemaActual(),"Navegar debe conservar el diseño");
                BotonTema boton = (BotonTema)nodo("btnTema");
                for(int i=0;i<3;i++) {
                    TemaVisual esperado = navegador.getTemaActual().siguiente();
                    boton.fire();
                    assertEquals(esperado,navegador.getTemaActual());
                    assertEquals("Diseño "+esperado.getNombre(),boton.getText());
                    assertEquals(esperado.getTipoIcono(),((cr.ac.una.est.programacion03proyecto02.view.iconos.Icono)boton.getGraphic()).getTipo());
                    assertSame(escena,ventana.getScene());
                    assertSame(raiz,root(),"El selector no debe recrear el formulario");
                    var hojas = escena.getStylesheets();
                    assertEquals(esperado==TemaVisual.FRIO?1:2,hojas.size());
                    assertEquals(hojas.size(),new HashSet<>(hojas).size());
                    assertEquals(esperado==TemaVisual.CALIDO,hojas.stream().anyMatch(s->s.endsWith("app-calido.css")));
                    assertEquals(esperado==TemaVisual.PERSONALIZADO,hojas.stream().anyMatch(s->s.endsWith("app-personalizado.css")));
                    root().applyCss(); root().layout();
                    assertTrue(javafx.css.CssParser.errorsProperty().isEmpty(),
                            ()->"Error de CSS al aplicar "+esperado+" en "+vista+": "+javafx.css.CssParser.errorsProperty());
                    if (vista.equals("toma")) {
                        assertTrue(nodo("lstOpciones").getBoundsInLocal().getHeight() >= 80,
                                "La lista de extras debe permitir seleccionar opciones");
                        assertTrue(nodo("tblLineas").getBoundsInLocal().getHeight() >= 120,
                                "La tabla de líneas debe conservar espacio para sus filas");
                    }
                }
                while(navegador.getTemaActual()!=TemaVisual.PERSONALIZADO) boton.fire();
                root().applyCss(); root().layout();
                var limites = boton.localToScene(boton.getBoundsInLocal());
                assertTrue(limites.getMinX()>=0 && limites.getMaxX()<=escena.getWidth()+1,"Selector recortado en "+vista);
                if (vista.equals("menu")) {
                    root().lookupAll(".module-card").stream().filter(javafx.scene.Node::isManaged)
                            .forEach(nodo -> assertTrue(nodo.getBoundsInParent().getHeight() <= 200,
                                    "Las tarjetas del menú no deben crecer hasta desbordar la ventana"));
                }
                capturar(vista);
            });
        }
    }

    @Test void cambiarDisenoNoBorraFormulariosNiLaPestanaSeleccionada() throws Exception {
        abrirInventario();
        fx(() -> {
            TabPane tabs = (TabPane)nodo("tabAcciones");
            tabs.getSelectionModel().select(1);
            texto("txtNombreIngrediente","Ingrediente pendiente");
            texto("txtStockMinimo","12.5");
            for(int i=0;i<3;i++) boton("btnTema").fire();
            assertEquals(1,tabs.getSelectionModel().getSelectedIndex());
            assertEquals("Ingrediente pendiente",((TextField)nodo("txtNombreIngrediente")).getText());
            assertEquals("12.5",((TextField)nodo("txtStockMinimo")).getText());
            assertEquals(0,inventario.guardados.get());
        });
    }

    @Test void loginValidaCamposYRechazaCredencialesIncorrectas() throws Exception {
        fx(() -> {
            navegador.mostrarLogin();
            assertFalse(boton("btnIniciar").isDisabled());
            assertTrue(boton("btnIniciar").isDefaultButton());
            assertNull(sesion.getUsuario());
            assertThrows(IllegalStateException.class,navegador::mostrarMenu);
            boton("btnIniciar").fire();
            assertTrue(((Label)nodo("lblMensaje")).getText().contains("nombre de usuario y la clave"));
            usuarioAutenticado = null;
            texto("txtNombreUsuario","stock"); texto("txtClave","incorrecta"); boton("btnIniciar").fire();
        });
        esperar();
        fx(() -> {
            assertEquals("Nombre o clave incorrectos.",((Label)nodo("lblMensaje")).getText());
            assertEquals("",((PasswordField)nodo("txtClave")).getText());
            assertNull(sesion.getUsuario());
            assertFalse(boton("btnIniciar").isDisabled());
        });
    }

    @Test void loginUsaElRolDeLaCuentaYCerrarSesionPermiteCambiarDeUsuario() throws Exception {
        fx(() -> {
            navegador.mostrarLogin();
            boton("btnTema").fire();
            ventana.setWidth(1100); ventana.setHeight(780);
        });
        Scene[] escena = new Scene[1];
        double[] dimensiones = new double[2];
        fx(() -> { escena[0]=ventana.getScene(); dimensiones[0]=ventana.getWidth(); dimensiones[1]=ventana.getHeight(); });
        for(Rol rol : Rol.values()) {
            fx(() -> {
                usuarioAutenticado = usuario(rol);
                texto("txtNombreUsuario",rol.name()); texto("txtClave","clave-prueba"); boton("btnIniciar").fire();
            });
            esperar();
            fx(() -> {
                assertEquals(rol,sesion.getUsuario().getRol());
                assertEquals(rol==Rol.CAJERO,nodo("btnPedidos").isVisible());
                assertEquals(rol!=Rol.CAJERO,nodo("btnInventario").isVisible());
                for(String id : List.of("btnCatalogo","btnReportes","btnUsuarios")) {
                    assertEquals(rol==Rol.ADMIN,nodo(id).isVisible());
                    assertEquals(nodo(id).isVisible(),nodo(id).isManaged());
                }
                if(rol!=Rol.ADMIN) {
                    assertThrows(SecurityException.class,navegador::mostrarProductos);
                    assertThrows(SecurityException.class,navegador::mostrarReportes);
                    assertThrows(SecurityException.class,navegador::mostrarUsuarios);
                }
                if(rol==Rol.CAJERO) assertThrows(SecurityException.class,navegador::mostrarInventario);
                else assertThrows(SecurityException.class,navegador::mostrarPedidosActivos);
                ventana.getProperties().put("pedidoId",42L);
                boton("btnCerrarSesion").fire();
                assertNull(sesion.getUsuario());
                assertFalse(ventana.getProperties().containsKey("pedidoId"));
                assertEquals("",((TextField)nodo("txtNombreUsuario")).getText());
                assertEquals("",((PasswordField)nodo("txtClave")).getText());
                assertEquals("",((Label)nodo("lblMensaje")).getText());
                assertEquals(TemaVisual.CALIDO,navegador.getTemaActual());
                assertSame(escena[0],ventana.getScene());
                assertEquals(dimensiones[0],ventana.getWidth(),1);
                assertEquals(dimensiones[1],ventana.getHeight(),1);
                assertThrows(IllegalStateException.class,navegador::mostrarMenu);
            });
        }
    }

    @Test void errorDeAutenticacionNoIniciaSesionYPermiteReintentar() throws Exception {
        fallarAutenticacion = true;
        fx(() -> {
            navegador.mostrarLogin();
            texto("txtNombreUsuario","admin"); texto("txtClave","clave-prueba"); boton("btnIniciar").fire();
        });
        esperar();
        fx(() -> {
            assertNull(sesion.getUsuario());
            assertEquals("No se pudo completar el inicio de sesión.",((Label)nodo("lblMensaje")).getText());
            assertEquals("",((PasswordField)nodo("txtClave")).getText());
            assertFalse(boton("btnIniciar").isDisabled());
            fallarAutenticacion = false;
            texto("txtClave","clave-prueba"); boton("btnIniciar").fire();
        });
        esperar();
        fx(() -> {
            assertEquals(Rol.ADMIN,sesion.getUsuario().getRol());
            assertTrue(nodo("btnUsuarios").isVisible());
        });
    }

    @Test void filtroBajoMinimoSePuedeQuitarSinConsultarDeNuevo()throws Exception {
        abrirInventario();int lecturas=inventario.consultas.get();
        fx(()->{
            boton("btnBajoMinimo").fire();
            assertEquals(0,((TableView<?>)nodo("tblIngredientes")).getItems().size());
            assertEquals("Ver todos",boton("btnBajoMinimo").getText());
            boton("btnBajoMinimo").fire();
            assertEquals(1,((TableView<?>)nodo("tblIngredientes")).getItems().size());
            assertEquals(lecturas,inventario.consultas.get());
        });
    }

    @Test void ingredienteNuevoPermiteGuardarUnidadSinSeleccionarlaDosVeces()throws Exception {
        abrirInventario();
        fx(()->{
            boton("btnNueva").fire();texto("txtNombreIngrediente","Huevos");
            assertEquals(unidadMedida.UNIDAD,((ComboBox<?>)nodo("cmbUnidad")).getValue());
            boton("btnGuardar").fire();
        });
        esperar();fx(()->assertEquals(1,inventario.guardados.get()));
    }

    @Test void navegarNoCambiaEscenaNiElTamanoElegidoDeLaVentana()throws Exception {
        fx(()->{sesion.iniciar(usuario(Rol.ADMIN));navegador.mostrarMenu();ventana.setWidth(1250);ventana.setHeight(850);});
        fx(()->{
            Scene escena=ventana.getScene();double ancho=ventana.getWidth(),alto=ventana.getHeight();
            navegador.mostrarReportes();
            assertSame(escena,ventana.getScene());
            assertEquals(ancho,ventana.getWidth(),1);assertEquals(alto,ventana.getHeight(),1);
        });
    }

    @Test void administradorPuedeCrearUnCajeroDesdeSuPantalla()throws Exception {
        fx(()->{sesion.iniciar(usuario(Rol.ADMIN));navegador.mostrarUsuarios();});esperar();
        fx(()->{
            texto("txtUsuario","nuevo-cajero");texto("txtClave","clave-prueba");texto("txtConfirmacion","clave-prueba");
            boton("btnCrearUsuario").fire();
        });esperar();
        fx(()->{assertEquals(Rol.CAJERO,cuentas.getFirst().rol());assertEquals(1,((TableView<?>)nodo("tblUsuarios")).getItems().size());});
    }

    @Test void extraSeCreaVinculadoAUnProductoConReceta()throws Exception {
        fx(()->{sesion.iniciar(usuario(Rol.ADMIN));navegador.mostrarProductoEditor(0);});esperar();
        fx(()->{
            ((TabPane)nodo("tabEditor")).getSelectionModel().select(1);root().applyCss();root().layout();
            ((ComboBox<String>)nodo("cmbOrigenOpcion")).setValue("Producto del catálogo");
            ((ComboBox<?>)nodo("cmbProductoOpcion")).getSelectionModel().selectFirst();boton("btnAgregarOpcion").fire();
            OpcionProducto opcion=(OpcionProducto)((TableView<?>)nodo("tblOpciones")).getItems().getFirst();
            assertEquals(7,opcion.getProductoOrigenId());assertEquals(1,opcion.getReceta().size());
            assertTrue(boton("btnAgregarRecetaOpcion").isDisabled());
            root().applyCss();root().layout();capturar("editor_extras");
        });
    }

    @Test void categoriaDeProductoEditadoMuestraNombreYPermiteEliminarUnExtra()throws Exception {
        fx(()->{sesion.iniciar(usuario(Rol.ADMIN));navegador.mostrarProductoEditor(7);});esperar();
        fx(()->{
            ComboBox<Categoria> categoria=(ComboBox<Categoria>)nodo("cmbCategoria");
            assertEquals("Comidas típicas",categoria.getConverter().toString(categoria.getValue()));
            assertEquals("Comidas típicas",categoria.getButtonCell().getText());
            ((TabPane)nodo("tabEditor")).getSelectionModel().select(1);root().applyCss();root().layout();
            TableView<?> opciones=(TableView<?>)nodo("tblOpciones");opciones.getSelectionModel().selectFirst();
            ((CheckBox)nodo("chkEstadoOpcion")).fire();
            assertFalse(((OpcionProducto)opciones.getItems().getFirst()).isActiva());
            assertEquals(1,opciones.getItems().size());boton("btnQuitarOpcion").fire();
            assertTrue(opciones.getItems().isEmpty());
            assertTrue(((Label)nodo("lblMensaje")).getText().contains("Guarde"));
        });
    }

    @Test void inventarioSeparaHistorialYDejaEspacioParaIngredientesConUnidad()throws Exception {
        abrirInventario();
        fx(()->{
            TabPane listado=(TabPane)nodo("tabListado");assertEquals(2,listado.getTabs().size());
            TableView<?> tabla=(TableView<?>)nodo("tblIngredientes");
            assertTrue(tabla.getHeight()>=160,"La lista debe permitir ver varios ingredientes.");
            assertEquals("Gramos",tabla.getColumns().get(3).getCellData(0));
            listado.getSelectionModel().select(1);root().applyCss();root().layout();
            assertNotNull(nodo("tblMovimientos"));capturar("inventario_movimientos");
            listado.getSelectionModel().select(0);root().applyCss();root().layout();
            assertSame(tabla,nodo("tblIngredientes"));
        });
    }

    @Test void actualizarIngredientesNoBorraLasFechasDeLaPestanaTodaviaOculta()throws Exception {
        abrirInventario();int consultas=inventario.consultas.get();
        fx(()->boton("btnActualizar").fire());esperar();
        fx(()->{
            assertEquals(consultas+1,inventario.consultas.get());
            assertEquals("Inventario actualizado.",((Label)nodo("lblMensaje")).getText());
            ((TabPane)nodo("tabListado")).getSelectionModel().select(1);root().applyCss();root().layout();
            assertNotNull(((DatePicker)nodo("dpDesde")).getValue());assertNotNull(((DatePicker)nodo("dpHasta")).getValue());
        });
    }

    @Test void inventarioDistingueVentasDeMermasYDevolucionesDeReabastecimientos() throws Exception {
        abrirInventario();
        fx(() -> {
            ((TabPane)nodo("tabListado")).getSelectionModel().select(1);root().applyCss();root().layout();
            TableView<?> tabla = (TableView<?>)nodo("tblMovimientos");
            TableColumn<?,?> tipo = tabla.getColumns().get(2);
            assertEquals(List.of("Reabastecimiento","Merma","Consumo por venta","Devolución de venta"),
                    java.util.stream.IntStream.range(0,tabla.getItems().size()).mapToObj(tipo::getCellData).toList());
        });
    }

    @Test void fechaEscritaInvalidaNoConsultaInventarioConElValorAnterior() throws Exception {
        abrirInventario();
        int consultas = inventario.consultas.get();
        fx(() -> {
            ((TabPane)nodo("tabListado")).getSelectionModel().select(1);root().applyCss();root().layout();
            ((DatePicker)nodo("dpHasta")).getEditor().setText("fecha inválida");
            boton("btnActualizar").fire();
            assertEquals(consultas,inventario.consultas.get());
            assertTrue(((Label)nodo("lblMensaje")).getText().contains("rango de fechas válido"));
            assertFalse(root().isDisabled());
        });
    }

    @Test void inventarioValidaCantidadYGuardaMovimientoEnSegundoPlano() throws Exception {
        abrirInventario();
        fx(() -> {
            ((ComboBox<?>)nodo("cmbIngrediente")).getSelectionModel().selectFirst();
            texto("txtCantidad","NaN"); texto("txtMotivo","Producto dañado");
            boton("btnMerma").fire();
            assertEquals(0,inventario.registrados.get());
            texto("txtCantidad","2.5"); boton("btnMerma").fire();
        });
        esperar();
        fx(() -> { assertEquals(1,inventario.registrados.get()); assertEquals("",((TextField)nodo("txtCantidad")).getText()); });
    }

    @Test void reporteValidaFechaEscritaYMuestraDatosSinBloquearJavaFX() throws Exception {
        fx(() -> { sesion.iniciar(usuario(Rol.ADMIN)); navegador.mostrarReportes(); });
        fx(() -> {
            ((DatePicker)nodo("dpDesde")).getEditor().setText("no es fecha");
            boton("btnConsultar").fire();
            assertEquals(0,consultasReporte.get());
            assertTrue(((Label)nodo("lblMensaje")).getText().contains("rango de fechas válido"));
            ((DatePicker)nodo("dpDesde")).setValue(LocalDate.now().minusDays(1));
            boton("btnConsultar").fire();
        });
        esperar();
        fx(() -> {
            assertEquals("3500.00",((Label)nodo("lblIngresos")).getText());
            assertEquals(1,consultasReporte.get(),"El reporte debe compartir una sola lectura de pedidos.");
            assertEquals(1,((TableView<?>)nodo("tblHistorial")).getItems().size());
            assertEquals(1,((TableView<?>)nodo("tblRanking")).getItems().size());
        });
    }

    @Test void falloDeReporteLiberaLaPantallaYPermiteCambiarDiseno() throws Exception {
        fallarReporte = true;
        fx(() -> { sesion.iniciar(usuario(Rol.ADMIN)); navegador.mostrarReportes(); boton("btnConsultar").fire(); });
        esperar();
        fx(() -> {
            assertTrue(((Label)nodo("lblMensaje")).getText().contains("Fallo de consulta simulado"));
            boton("btnTema").fire();
            assertEquals(TemaVisual.CALIDO,navegador.getTemaActual());
        });
    }

    private void abrirInventario() throws Exception {
        fx(() -> { sesion.iniciar(usuario(Rol.STOCK_MANAGER)); navegador.mostrarInventario(); }); esperar();
    }
    private static Usuario usuario(Rol rol) { return new Usuario(1,"prueba","hash-prueba",rol,true); }
    private static void segundoPlano() { assertFalse(Platform.isFxApplicationThread(),"El servicio debe ejecutarse fuera del hilo gráfico"); }
    private static void fx(Runnable accion) throws Exception { JavaFxTestSupport.ejecutar(accion); }
    private Parent root() { return ventana.getScene().getRoot(); }
    private javafx.scene.Node nodo(String id) { return Objects.requireNonNull(root().lookup("#"+id),id); }
    private Button boton(String id) { return (Button)nodo(id); }
    private void texto(String id,String texto) { ((TextField)nodo(id)).setText(texto); }
    private void esperar() throws Exception {
        for(int i=0;i<600;i++) {
            boolean[] listo={false};
            fx(() -> { root().applyCss(); root().layout(); listo[0]=!root().isDisabled(); });
            if(listo[0])return;
            Thread.sleep(15);
        }
        fail("La pantalla no terminó la operación");
    }
    private void capturar(String nombre) {
        if(!Boolean.getBoolean("pruebas.capturas"))return;
        var imagen=root().snapshot(null,null);
        int w=(int)imagen.getWidth(),h=(int)imagen.getHeight(); byte[] p=new byte[w*h*4];
        imagen.getPixelReader().getPixels(0,0,w,h,javafx.scene.image.PixelFormat.getByteBgraInstance(),p,0,w*4);
        for(int i=0;i<p.length;i+=4){byte b=p[i];p[i]=p[i+2];p[i+2]=b;}
        try {
            var dir=java.nio.file.Path.of("target/review-20261029/capturas"); java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.write(dir.resolve(nombre+".rgba"),p);
            java.nio.file.Files.writeString(dir.resolve(nombre+".json"),"{\"width\":"+w+",\"height\":"+h+"}");
        } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
    }

    private static final class InventarioSimulado implements iinventarioService {
        final ingrediente arroz = new ingrediente(1,"Arroz",20000,unidadMedida.GRAMO,5000,true);
        final AtomicInteger consultas=new AtomicInteger(),registrados=new AtomicInteger(),guardados=new AtomicInteger();
        final List<movimientoStock> movimientos=new CopyOnWriteArrayList<>(List.of(
                new reabastecimiento(1,arroz,4,LocalDateTime.now(),"stock","Compra"),
                new merma(2,arroz,1,LocalDateTime.now(),"stock","Daño"),
                new consumoVenta(3,42,arroz,2,LocalDateTime.now(),"cajero","Venta"),
                new devolucionVenta(4,42,arroz,2,LocalDateTime.now(),"cajero","Cancelación")));
        @Override public List<ingrediente> listarIngredientes() { segundoPlano(); consultas.incrementAndGet(); return List.of(arroz); }
        @Override public List<movimientoStock> listarMovimientos(LocalDateTime desde,LocalDateTime hasta) { segundoPlano(); return List.copyOf(movimientos); }
        @Override public List<ingrediente> listarBajoMinimo() { segundoPlano(); return List.of(); }
        @Override public void guardarIngrediente(ingrediente valor) { segundoPlano(); guardados.incrementAndGet(); }
        @Override public void registrarMovimiento(movimientoStock valor) { segundoPlano(); registrados.incrementAndGet(); movimientos.add(valor); }
        @Override public boolean hayStockSuficiente(Map<Long,Double> cantidades) { segundoPlano(); return true; }
        @Override public boolean hayStockSuficiente(Map<Long,Double> cantidades,Connection c) { segundoPlano(); return true; }
        @Override public void descontar(Map<Long,Double> cantidades,long id,String usuario,Connection c) { segundoPlano(); }
        @Override public void restituir(Map<Long,Double> cantidades,long id,String usuario,Connection c) { segundoPlano(); }
    }
}
