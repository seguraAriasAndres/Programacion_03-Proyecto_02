package cr.ac.una.est.programacion03proyecto02.view.navigation;

import cr.ac.una.est.programacion03proyecto02.controller.catalogo.CategoriasController;
import cr.ac.una.est.programacion03proyecto02.controller.catalogo.ProductoEditorController;
import cr.ac.una.est.programacion03proyecto02.controller.catalogo.ProductosController;
import cr.ac.una.est.programacion03proyecto02.controller.inventario.inventarioController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.CobroController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.PedidosActivosController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.ReciboController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.TomaPedidosController;
import cr.ac.una.est.programacion03proyecto02.controller.reportes.reportesController;
import cr.ac.una.est.programacion03proyecto02.controller.seguridad.inicioController;
import cr.ac.una.est.programacion03proyecto02.controller.seguridad.loginController;
import cr.ac.una.est.programacion03proyecto02.controller.seguridad.menuController;
import cr.ac.una.est.programacion03proyecto02.controller.seguridad.UsuariosController;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.UsuariosService;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.CobroService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.PedidoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.reportes.ReporteService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.IAutenticacionService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.personalizacion.BotonTema;
import cr.ac.una.est.programacion03proyecto02.view.personalizacion.TemaVisual;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

public class NavegadorVistas{

    private static final String rutaBase = "/cr/ac/una/est/programacion03proyecto02/view/";
    private static final String clavePedidoId = "pedidoId";
    private static final String claveRolLogin = "rolLogin";

    private Stage ventana;
    private catalogoService catalogo;
    private iinventarioService inventario;
    private SesionActual sesion;
    private PedidoService pedidos;
    private CobroService cobros;
    private IAutenticacionService autenticacion;
    private ReporteService reportes;
    private UsuariosService usuarios;
    private boolean vistaMostrada;
    // Hoja de estilos por defecto; un tema de cliente la sustituye sin tocar los FXML.
    private String hojaEstilos = rutaBase + TemaVisual.FRIO.getArchivo();
    private final ReadOnlyObjectWrapper<TemaVisual> tema = new ReadOnlyObjectWrapper<>(TemaVisual.FRIO);

    // Guarda los servicios de pedidos y cobros que compartirán sus controladores.
    public void configurarPedidos(PedidoService pedidos, CobroService cobros){
        this.pedidos = Objects.requireNonNull(pedidos);
        this.cobros = Objects.requireNonNull(cobros);
    }

    // Guarda los servicios de login y reportes que usarán sus controladores.
    public void configurarPantallas(IAutenticacionService autenticacion, ReporteService reportes){
        this.autenticacion = Objects.requireNonNull(autenticacion);
        this.reportes = Objects.requireNonNull(reportes);
    }

    public void configurarUsuarios(UsuariosService usuarios){ this.usuarios=Objects.requireNonNull(usuarios); }
    public void mostrarUsuarios(){ abrir("seguridad/usuarios.fxml"); }

    // Configura la ventana, los servicios compartidos y la sesión actual.
    public void configurar(Stage ventana, catalogoService catalogo, iinventarioService inventario, SesionActual sesion){
        this.ventana = Objects.requireNonNull(ventana);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.inventario = Objects.requireNonNull(inventario);
        this.sesion = Objects.requireNonNull(sesion);

        this.ventana.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, evento -> {
            if (operacionEnCurso()){
                evento.consume();
            }
        });
    }

    // Cambia la hoja de estilos de todas las pantallas. Una ruta inexistente
    // vuelve al app-frio.css neutro; un tema de cliente se copia desde ese archivo.
    public void usarHojaEstilos(String ruta){
        if (ruta == null || ruta.isBlank()){
            throw new IllegalArgumentException("La ruta de la hoja de estilos no puede estar vacía.");
        }
        this.hojaEstilos = ruta.replace('\\', '/').startsWith("/")
                ? ruta.replace('\\', '/')
                : rutaBase + ruta.replace('\\', '/');
        if (ventana != null && ventana.getScene() != null){
            aplicarEstilos(ventana.getScene());
        }
    }

    // Recorre frío -> cálido -> personalizado -> frío sin recargar el formulario.
    public void alternarTema(){
        exigirVentana();
        TemaVisual siguiente = tema.get().siguiente();
        if (getClass().getResource(rutaBase + siguiente.getArchivo()) == null){
            throw new IllegalStateException("No se encontró la hoja del diseño " + siguiente.getNombre() + ".");
        }
        tema.set(siguiente);
        if (ventana.getScene() != null){
            aplicarEstilos(ventana.getScene());
        }
    }

    public boolean esTemaCalido(){
        return tema.get() == TemaVisual.CALIDO;
    }

    public TemaVisual getTemaActual(){
        return tema.get();
    }

    // Permite mantener sincronizados los botones sin que modifiquen el estado directamente.
    public ReadOnlyObjectProperty<TemaVisual> temaProperty(){
        return tema.getReadOnlyProperty();
    }

    // Abre la pantalla inicial pública.
    public void mostrarInicio(){
        abrir("seguridad/inicio.fxml");
    }

    // Abre el login administrativo sin rol preseleccionado.
    public void mostrarLogin(){
        exigirVentana();
        ventana.getProperties().remove(claveRolLogin);
        abrir("seguridad/login.fxml");
    }

    // Abre el login con el rol administrativo elegido.
    public void mostrarLogin(Rol rolEsperado){
        Objects.requireNonNull(rolEsperado);

        exigirVentana();
        ventana.getProperties().put(claveRolLogin, rolEsperado);
        abrir("seguridad/login.fxml");
    }

    // Devuelve el rol elegido antes de abrir el login.
    public Rol getRolLoginSeleccionado(){
        exigirVentana();
        Object valor = ventana.getProperties().get(claveRolLogin);
        return valor instanceof Rol rol ? rol : null;
    }

    // Abre el menú de la sesión actual.
    public void mostrarMenu(){
        abrir("seguridad/menu.fxml");
    }

    // Abre la vista de inventario.
    public void mostrarInventario(){
        abrir("inventario/inventario.fxml");
    }

    // Abre los reportes financieros.
    public void mostrarReportes(){
        abrir("reportes/reportes.fxml");
    }

    // Cierra la sesión y vuelve al inicio.
    public void cerrarSesionYMostrarInicio(){
        exigirVentana();
        if (operacionEnCurso()){
            throw new IllegalStateException("Espere a que termine la operación actual.");
        }

        sesion.cerrar();
        ventana.getProperties().remove(clavePedidoId);
        ventana.getProperties().remove(claveRolLogin);
        mostrarInicio();
    }

    // Resuelve y abre una vista aplicando sus permisos.
    public void abrir(String vista){
        abrirVista(resolverRuta(vista), null);
    }

    // Abre la lista de productos.
    public void mostrarProductos(){
        abrir("catalogo/productos.fxml");
    }

    // Abre el editor de producto y entrega el id que debe cargar.
    public void mostrarProductoEditor(long productoId){
        abrirVista(resolverRuta("catalogo/producto-editor.fxml"), controlador -> ((ProductoEditorController) controlador).mostrarProducto(productoId));
    }

    // Abre un pedido y entrega su id al controlador.
    public void abrirPedido(long pedidoId){
        validarPedidos();
        if (pedidoId < 0){
            throw new IllegalArgumentException("El id no puede ser negativo.");
        }
        if (pedidoId > 0){
            ventana.getProperties().put(clavePedidoId, pedidoId);
        } else{
            ventana.getProperties().remove(clavePedidoId);
        }

        abrirVista(resolverRuta("pedidos/toma_pedido.fxml"), controlador -> ((TomaPedidosController) controlador).mostrarPedido(pedidoId));
    }

    // Abre la pantalla de cobro para el pedido indicado.
    public void abrirCobro(long pedidoId){
        validarPedidos();
        if (pedidoId <= 0) {
            throw new IllegalArgumentException("El id del pedido debe ser positivo.");
        }
        ventana.getProperties().put(clavePedidoId, pedidoId);

        abrirVista(resolverRuta("pedidos/Cobro.fxml"), controlador -> ((CobroController) controlador).mostrarPedido(pedidoId));
    }

    // Abre la lista de pedidos activos.
    public void mostrarPedidosActivos(){
        abrir("pedidos/pedidos_activos.fxml");
    }

    // Abre el recibo de una venta.
    public void mostrarRecibo(Recibo recibo){
        validarPedidos();
        Objects.requireNonNull(recibo);

        abrirVista(resolverRuta("pedidos/recibo.fxml"), controlador -> ((ReciboController) controlador).mostrarRecibo(recibo));
    }

    // Resuelve una ruta corta al recurso FXML correspondiente.
    private String resolverRuta(String vista){
        Objects.requireNonNull(vista);
        String ruta = vista.replace('\\', '/');
        if (ruta.startsWith("/")){
            ruta = ruta.substring(1);
        }
        if (ruta.contains("../") || ruta.equals("..")){
            throw new IllegalArgumentException("La ruta de la vista no es válida.");
        }
        if (!ruta.endsWith(".fxml")){
            ruta += ".fxml";
        }
        return rutaBase + ruta;
    }

    // Valida el rol permitido para cada grupo de vistas.
    private void validarAccesoVista(String ruta){
        if (!ruta.startsWith(rutaBase)){
            throw new IllegalArgumentException("La vista está fuera de la carpeta permitida.");
        }

        String vista = ruta.substring(rutaBase.length());

        if (vista.equals("seguridad/inicio.fxml")
                || vista.equals("seguridad/login.fxml")){
            return;
        }
        if (vista.equals("seguridad/menu.fxml")){
            sesion.exigirAlgunoDe(Rol.ADMIN, Rol.STOCK_MANAGER, Rol.CAJERO);
            return;
        }
        if (vista.equals("seguridad/usuarios.fxml")){
            sesion.exigirRol(Rol.ADMIN);
            return;
        }
        if (vista.startsWith("pedidos/")){
            validarPedidos();
            return;
        }
        if (vista.startsWith("catalogo/")){
            sesion.exigirRol(Rol.ADMIN);
            return;
        }
        if (vista.startsWith("inventario/")){
            sesion.exigirAlgunoDe(Rol.ADMIN, Rol.STOCK_MANAGER);
            return;
        }
        if (vista.startsWith("reportes/") || vista.startsWith("ventas/") || vista.startsWith("compras/")) {
            sesion.exigirRol(Rol.ADMIN);
            return;
        }

        throw new IllegalArgumentException("No hay regla de acceso para: " + vista);
    }

    // Carga el FXML y muestra la vista después de validar acceso.
    private void abrirVista(String ruta, Consumer<Object> configurarControlador){
        exigirVentana();
        validarAccesoVista(ruta);

        if (operacionEnCurso()){
            throw new IllegalStateException("Espere a que termine la operación actual.");
        }

        URL recurso = getClass().getResource(ruta);
        if (recurso == null){
            throw new IllegalArgumentException("No se encontró el archivo FXML: " + ruta);
        }

        try{
            FXMLLoader loader = new FXMLLoader(recurso);
            loader.setControllerFactory(this::crearControlador);
            Parent raiz = loader.load();
            Object controlador = loader.getController();
            raiz.lookupAll(".theme-toggle").forEach(nodo -> ((BotonTema) nodo).configurar(this));

            if (configurarControlador != null) {
                configurarControlador.accept(controlador);
            }

            // La escena y la ventana permanecen: cambiar de vista no restaura sus dimensiones.
            Scene escena = ventana.getScene();
            if (!vistaMostrada || escena == null){
                var limites = javafx.stage.Screen.getPrimary().getVisualBounds();
                escena = new Scene(raiz, Math.min(1150, limites.getWidth() - 40),
                        Math.min(800, limites.getHeight() - 80));
                ventana.setScene(escena);
                vistaMostrada = true;
            } else {
                escena.setRoot(raiz);
            }
            aplicarEstilos(escena);
            ventana.show();
        } catch (IOException error){
            throw new IllegalStateException("No se pudo cargar la vista " + ruta, error);
        }
    }

    // Adjunta la hoja de estilos a la escena; si falta, usa el tema neutro base.
    private void aplicarEstilos(Scene escena){
        java.util.Set<String> hojas = new java.util.LinkedHashSet<>();
        URL hoja = getClass().getResource(hojaEstilos);
        if (hoja == null){
            hoja = getClass().getResource(rutaBase + TemaVisual.FRIO.getArchivo());
        }
        if (hoja != null){
            hojas.add(hoja.toExternalForm());
        }
        if (tema.get() != TemaVisual.FRIO){
            URL seleccionada = getClass().getResource(rutaBase + tema.get().getArchivo());
            if (seleccionada != null){
                hojas.add(seleccionada.toExternalForm());
            }
        }
        escena.getStylesheets().setAll(hojas);
    }

    // Construye los controladores con sus dependencias compartidas.
    private Object crearControlador(Class<?> tipo){
        if(tipo==UsuariosController.class) return new UsuariosController(usuarios,this);
        if (tipo == inicioController.class){
            return new inicioController(sesion, this);
        }
        if (tipo == loginController.class){
            return new loginController(autenticacion, sesion, this);
        }
        if (tipo == menuController.class){
            return new menuController(sesion, this);
        }
        if (tipo == inventarioController.class){
            return new inventarioController(inventario, sesion, this);
        }
        if (tipo == reportesController.class){
            return new reportesController(reportes, sesion, this);
        }
        if (tipo == PedidosActivosController.class){
            return new PedidosActivosController(pedidos, sesion, this);
        }
        if (tipo == TomaPedidosController.class){
            return new TomaPedidosController(pedidos, catalogo, sesion, this);
        }
        if (tipo == CobroController.class){
            return new CobroController(pedidos, cobros, sesion, this);
        }
        if (tipo == CategoriasController.class){
            return new CategoriasController(catalogo, sesion, this);
        }
        if (tipo == ProductosController.class){
            return new ProductosController(catalogo, sesion, this);
        }
        if (tipo == ProductoEditorController.class){
            return new ProductoEditorController(catalogo, inventario, sesion, this);
        }
        if (tipo == ReciboController.class){
            return new ReciboController(this);
        }

        try{
            return tipo.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException error){
            throw new IllegalStateException("No se pudo crear el controlador " + tipo.getName() + ". Agréguelo a crearControlador().", error);
        }
    }

    // Comprueba que el usuario sea cajero y que los servicios de pedidos estén listos.
    private void validarPedidos(){
        exigirVentana();
        if (pedidos == null || cobros == null){
            throw new IllegalStateException("Los servicios de pedidos no están configurados.");
        }
        sesion.exigirRol(Rol.CAJERO);
        if (operacionEnCurso()){
            throw new IllegalStateException("Espere a que termine la operación actual.");
        }
    }

    // Comprueba que el navegador ya tenga una ventana configurada.
    private void exigirVentana(){
        if (ventana == null) {
            throw new IllegalStateException("Primero debe configurarse el navegador.");
        }
    }

    // Detecta si la vista actual está bloqueada por una operación.
    private boolean operacionEnCurso(){
        return ventana != null && ventana.getScene() != null && ventana.getScene().getRoot().isDisabled();
    }
}
