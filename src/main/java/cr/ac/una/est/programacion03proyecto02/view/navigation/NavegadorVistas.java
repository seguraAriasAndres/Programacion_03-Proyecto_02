package cr.ac.una.est.programacion03proyecto02.view.navigation;

import cr.ac.una.est.programacion03proyecto02.controller.catalogo.CategoriasController;
import cr.ac.una.est.programacion03proyecto02.controller.catalogo.ProductoEditorController;
import cr.ac.una.est.programacion03proyecto02.controller.catalogo.ProductosController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.CobroController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.PedidosActivosController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.ReciboController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.TomaPedidosController;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.CobroService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.PedidoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;

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

    // Guarda los servicios de pedidos y cobros que compartirán sus controladores.
    public void configurarPedidos(PedidoService pedidos, CobroService cobros){
        this.pedidos = Objects.requireNonNull(pedidos, "Falta el servicio de pedidos.");
        this.cobros = Objects.requireNonNull(cobros, "Falta el servicio de cobros.");
    }

    // Configura la ventana, los servicios compartidos y la sesión actual.
    public void configurar(Stage ventana, catalogoService catalogo, iinventarioService inventario, SesionActual sesion){
        this.ventana = Objects.requireNonNull(ventana, "Falta la ventana principal.");
        this.catalogo = Objects.requireNonNull(catalogo, "Falta el servicio de catálogo.");
        this.inventario = Objects.requireNonNull(inventario, "Falta el servicio de inventario.");
        this.sesion = Objects.requireNonNull(sesion, "Falta la sesión actual.");

        this.ventana.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, evento -> {
            if (operacionEnCurso()){
                evento.consume();
            }
        });
    }

    // Abre la pantalla inicial, que será pública.
    public void mostrarInicio(){
        abrir("seguridad/inicio.fxml");
    }

    // Abre el login sin rol preseleccionado para mantener compatibilidad.
    public void mostrarLogin(){
        if (ventana != null){
            ventana.getProperties().remove(claveRolLogin);
        }
        abrir("seguridad/login.fxml");
    }

    // Abre el login para el rol administrativo que el usuario eligió.
    public void mostrarLogin(Rol rolEsperado){
        Objects.requireNonNull(rolEsperado, "Falta el rol esperado.");

        if (rolEsperado != Rol.ADMIN && rolEsperado != Rol.STOCK_MANAGER){
            throw new IllegalArgumentException("El login con contraseña solo admite ADMIN o STOCK_MANAGER.");
        }
        if (ventana == null){
            throw new IllegalStateException("Primero debe configurarse el navegador.");
        }

        ventana.getProperties().put(claveRolLogin, rolEsperado);
        abrir("seguridad/login.fxml");
    }

    // Devuelve el rol elegido antes de abrir el login o null si no se eligió.
    public Rol getRolLoginSeleccionado(){
        if (ventana == null){
            throw new IllegalStateException("Primero debe configurarse el navegador.");
        }

        Object valor = ventana.getProperties().get(claveRolLogin);
        return valor instanceof Rol rol ? rol : null;
    }

    // Abre el menú para cualquier sesión activa.
    public void mostrarMenu(){
        abrir("seguridad/menu.fxml");
    }

    // Cierra la sesión y vuelve a la pantalla inicial.
    public void cerrarSesionYMostrarInicio(){
        if (ventana == null || sesion == null){
            throw new IllegalStateException("Primero debe configurarse el navegador.");
        }
        if (operacionEnCurso()){
            throw new IllegalStateException("Espere a que termine la operación actual.");
        }

        sesion.cerrar();
        ventana.getProperties().remove(clavePedidoId);
        ventana.getProperties().remove(claveRolLogin);
        mostrarInicio();
    }

    // Resuelve y abre una vista aplicando sus permisos de acceso.
    public void abrir(String vista){
        abrirVista(resolverRuta(vista), null);
    }

    // Abre la lista de productos, restringida al administrador.
    public void mostrarProductos(){
        abrir("catalogo/productos.fxml");
    }

    // Abre el editor de producto y le entrega el id que debe cargar.
    public void mostrarProductoEditor(long productoId){
        abrirVista(resolverRuta("catalogo/producto-editor.fxml"),
                controlador -> {
                    if (!(controlador instanceof ProductoEditorController editor)){
                        throw new IllegalStateException("El FXML de producto debe usar ProductoEditorController.");
                    }
                    editor.mostrarProducto(productoId);
                }
        );
    }

    // Mantiene el acceso existente para abrir el editor de un producto.
    public void abrirProducto(long productoId){
        mostrarProductoEditor(productoId);
    }

    // Abre un pedido y entrega su id al controlador.
    public void abrirPedido(long pedidoId){
        validarPedidos();
        if (pedidoId < 0){
            throw new IllegalArgumentException("El ID no puede ser negativo.");
        }

        if (pedidoId > 0){
            guardarPedidoId(pedidoId);
        } else {
            ventana.getProperties().remove(clavePedidoId);
        }

        abrirVista(
                resolverRuta("pedidos/toma_pedido.fxml"),
                controlador -> ((TomaPedidosController) controlador).mostrarPedido(pedidoId)
        );
    }

    // Abre la pantalla de cobro del pedido indicado.
    public void abrirCobro(long pedidoId){
        validarPedidos();
        guardarPedidoId(pedidoId);

        abrirVista(resolverRuta("pedidos/Cobro.fxml"), controlador -> ((CobroController) controlador).mostrarPedido(pedidoId));
    }

    // Abre la lista de pedidos activos para el cajero.
    public void mostrarPedidosActivos(){
        abrir("pedidos/pedidos_activos.fxml");
    }

    // Abre el recibo generado para un pedido.
    public void mostrarRecibo(Recibo recibo){
        validarPedidos();
        Objects.requireNonNull(recibo, "El recibo no puede ser null.");

        abrirVista(resolverRuta("pedidos/recibo.fxml"),
                controlador -> {
                    if (!(controlador instanceof ReciboController reciboController)){
                        throw new IllegalStateException("El FXML de recibo debe usar ReciboController.");
                    }
                    reciboController.mostrarRecibo(recibo);
                }
        );
    }

    // Devuelve el id guardado para la vista actual de pedido o cobro.
    public long getPedidoIdActual(){
        if (ventana == null){
            throw new IllegalStateException("Primero debe configurarse el navegador.");
        }

        Object valor = ventana.getProperties().get(clavePedidoId);
        if (!(valor instanceof Long pedidoId)){
            throw new IllegalStateException("No hay un id de pedido guardado en la navegación.");
        }
        return pedidoId;
    }

    // Guarda el id del pedido para que la siguiente vista pueda recuperarlo.
    private void guardarPedidoId(long pedidoId){
        if (pedidoId <= 0){
            throw new IllegalArgumentException("El id del pedido debe ser positivo.");
        }
        if (ventana == null){
            throw new IllegalStateException("Primero debe llamarse a configurar(...).");
        }

        ventana.getProperties().put(clavePedidoId, pedidoId);
    }

    // Convierte la ruta corta de una vista en la ruta del recurso FXML.
    private String resolverRuta(String vista){
        Objects.requireNonNull(vista, "La ruta de la vista no puede ser null.");

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

    // Aplica la matriz de permisos antes de cargar cualquier vista.
    private void validarAccesoVista(String ruta){
        if (!ruta.startsWith(rutaBase)){
            throw new IllegalArgumentException("La vista está fuera de la carpeta permitida.");
        }

        String vista = ruta.substring(rutaBase.length());

        if (vista.equals("seguridad/inicio.fxml") || vista.equals("seguridad/login.fxml")){
            return;
        }

        if (vista.equals("seguridad/menu.fxml")){
            exigirSesionActiva();
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

        if (vista.startsWith("reportes/") || vista.startsWith("ventas/") || vista.startsWith("compras/")){
            sesion.exigirRol(Rol.ADMIN);
            return;
        }

        throw new IllegalArgumentException("La vista no tiene una regla de acceso: " + vista);
    }

    // Impide mostrar el menú cuando no existe una sesión activa.
    private void exigirSesionActiva(){
        if (sesion == null){
            throw new IllegalStateException("Primero debe configurarse la sesión.");
        }

        sesion.exigirAlgunoDe(Rol.ADMIN, Rol.STOCK_MANAGER, Rol.CAJERO);
    }

    // Carga el FXML, construye su controlador y muestra la vista autorizada.
    private void abrirVista(String ruta, Consumer<Object> configurarControlador){
        if (ventana == null){
            throw new IllegalStateException("Primero debe llamarse a configurar(...) con la ventana principal.");
        }

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

            if (configurarControlador != null){
                configurarControlador.accept(controlador);
            }

            ventana.setScene(new Scene(raiz));
            ventana.show();
        } catch (IOException error){
            throw new IllegalStateException("No se pudo cargar la vista " + ruta, error);
        }
    }

    // Crea los controladores existentes que requieren servicios o sesión.
    private Object crearControlador(Class<?> tipo){
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
        if (ventana == null || sesion == null || pedidos == null || cobros == null) {
            throw new IllegalStateException("Primero deben configurarse la navegación y los servicios de pedidos.");
        }

        sesion.exigirRol(Rol.CAJERO);

        if (operacionEnCurso()){
            throw new IllegalStateException("Espere a que termine la operación actual.");
        }
    }

    // Detecta si la vista actual está bloqueada mientras ocurre una operación.
    private boolean operacionEnCurso(){
        return ventana != null && ventana.getScene() != null && ventana.getScene().getRoot().isDisabled();
    }
}
