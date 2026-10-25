package cr.ac.una.est.programacion03proyecto02.view.navigation;

import cr.ac.una.est.programacion03proyecto02.controller.catalogo.CategoriasController;
import cr.ac.una.est.programacion03proyecto02.controller.catalogo.ProductoEditorController;
import cr.ac.una.est.programacion03proyecto02.controller.catalogo.ProductosController;
import cr.ac.una.est.programacion03proyecto02.controller.pedidos.ReciboController;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.CatalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

//carga las vistas FXML y cambia la pantalla de la aplicación
public class NavegadorVistas {
    private static final String RUTA_BASE = "/cr/ac/una/est/programacion03proyecto02/view/";

    private static final String CLAVE_PEDIDO_ID = "pedidoId";

    private Stage ventana;
    private CatalogoService catalogo;
    private iinventarioService inventario;
    private SesionActual sesion;
    private ConexionDB conexionDB;

    //configura la ventana y las dependencias necesarias para crear controladores
    public void configurar(Stage ventana, CatalogoService catalogo, iinventarioService inventario, SesionActual sesion, ConexionDB conexionDB) {
        this.ventana = Objects.requireNonNull(ventana);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.inventario = Objects.requireNonNull(inventario);
        this.sesion = Objects.requireNonNull(sesion);
        this.conexionDB = Objects.requireNonNull(conexionDB);
    }

    //abre la vista de inicio de sesión
    public void mostrarLogin() {
        abrir("seguridad/login.fxml");
    }

    //abre el menú principal
    public void mostrarMenu() {
        abrir("menu.fxml");
    }

    //abre una vista relativa a la carpeta view
    public void abrir(String vista) {
        abrirVista(resolverRuta(vista), null);
    }

    //abre la lista de productos
    public void mostrarProductos() {
        abrir("catalogo/productos.fxml");
    }

    //abre el editor de producto y le entrega el id que debe cargar
    //un id cero indica que se creará un producto nuevo
    public void mostrarProductoEditor(long productoId) {
        abrirVista(
                resolverRuta("catalogo/producto-editor.fxml"),
                controlador -> {
                    if (!(controlador instanceof ProductoEditorController editor)) {
                        throw new IllegalStateException("El FXML de producto debe usar ProductoEditorController.");
                    }
                    editor.mostrarProducto(productoId);
                }
        );
    }

    public void abrirProducto(long productoId) {
        mostrarProductoEditor(productoId);
    }

    //abre la vista de pedido y guarda el id para el controlador
    public void abrirPedido(long pedidoId) {
        guardarPedidoId(pedidoId);
        abrir("pedidos/pedido.fxml");
    }

    //abre la vista de cobro y guarda el id para el controlador
    public void abrirCobro(long pedidoId) {
        guardarPedidoId(pedidoId);
        abrir("pedidos/Cobro.fxml");
    }

    //abre el recibo y entrega el objeto a su controlador
    public void mostrarRecibo(Recibo recibo) {
        Objects.requireNonNull(recibo, "El recibo no puede ser null.");
        abrirVista(
                resolverRuta("pedidos/recibo.fxml"),
                controlador -> {
                    if (!(controlador instanceof ReciboController reciboController)) {
                        throw new IllegalStateException("El FXML de recibo debe usar ReciboController.");
                    }
                    reciboController.mostrarRecibo(recibo);
                }
        );
    }

    //devuelve el id que se guardó antes de abrir una vista de pedido o cobro
    public long getPedidoIdActual() {
        if (ventana == null) {
            throw new IllegalStateException("Primero debe configurarse el navegador.");
        }
        Object valor = ventana.getProperties().get(CLAVE_PEDIDO_ID);
        if (!(valor instanceof Long pedidoId)) {
            throw new IllegalStateException("No hay un id de pedido guardado en la navegación.");
        }
        return pedidoId;
    }

    //guarda el id del pedido en la ventana compartida
    private void guardarPedidoId(long pedidoId) {
        if (pedidoId <= 0) {
            throw new IllegalArgumentException("El id del pedido debe ser positivo.");
        }
        if (ventana == null) {
            throw new IllegalStateException("Primero debe llamarse a configurar(...).");
        }
        ventana.getProperties().put(CLAVE_PEDIDO_ID, pedidoId);
    }

    //resuelve la ruta del recurso FXML dentro de view
    private String resolverRuta(String vista) {
        Objects.requireNonNull(vista, "La ruta de la vista no puede ser null.");
        String ruta = vista.startsWith("/") ? vista.substring(1) : vista;
        if (!ruta.endsWith(".fxml")) {
            ruta += ".fxml";
        }
        return RUTA_BASE + ruta;
    }

    //carga el FXML, crea el controlador con sus dependencias y muestra la vista
    private void abrirVista(String ruta, Consumer<Object> configurarControlador) {
        if (ventana == null) {
            throw new IllegalStateException("Primero debe llamarse a configurar(...) con la ventana principal.");
        }
        URL recurso = getClass().getResource(ruta);
        if (recurso == null) {
            throw new IllegalArgumentException("No se encontró el archivo FXML: " + ruta);
        }
        try {
            FXMLLoader loader = new FXMLLoader(recurso);
            loader.setControllerFactory(this::crearControlador);
            Parent raiz = loader.load();
            Object controlador = loader.getController();
            if (configurarControlador != null) {
                configurarControlador.accept(controlador);
            }
            ventana.setScene(new Scene(raiz));
            ventana.show();
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo cargar la vista " + ruta, error);
        }
    }

    //crea los controladores que necesitan dependencias en su constructor
    private Object crearControlador(Class<?> tipo) {
        if (tipo == CategoriasController.class) {
            return new CategoriasController(catalogo, sesion, this);
        }
        if (tipo == ProductosController.class) {
            return new ProductosController(catalogo, sesion, this);
        }
        if (tipo == ProductoEditorController.class) {
            return new ProductoEditorController(catalogo, inventario, sesion, this, conexionDB);
        }
        if (tipo == ReciboController.class) {
            return new ReciboController(this);
        }

        try {
            return tipo.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("No se pudo crear el controlador " + tipo.getName() + ". Agréguelo a crearControlador().", error);
        }
    }
}