package cr.ac.una.est.programacion03proyecto02.controller.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.CatalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ProductoDAO;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.SQLException;
import java.util.Objects;

//controlador para listar, filtrar, editar y desactivar productos
public class ProductosController {
    private final CatalogoService catalogo;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;
    private final ProductoDAO productoDAO;

    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Number> colProductoId;
    @FXML private TableColumn<Producto, String> colProductoNombre;
    @FXML private TableColumn<Producto, String> colProductoCategoria;
    @FXML private TableColumn<Producto, String> colProductoPrecio;
    @FXML private TableColumn<Producto, Boolean> colProductoDisponible;
    @FXML private Label lblMensaje;

    public ProductosController(CatalogoService catalogo, SesionActual sesion, NavegadorVistas navegador) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
        this.productoDAO = new ProductoDAO();
    }

    //configura la tabla y el filtro de categorías, y carga los productos
    @FXML
    private void initialize() {
        colProductoId.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getId()));

        colProductoNombre.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getNombre()));

        colProductoCategoria.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        celda.getValue().getCategoria() == null ? "" : celda.getValue().getCategoria().getNombre()));

        colProductoPrecio.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        celda.getValue().getPrecioBase().toPlainString()));

        colProductoDisponible.setCellValueFactory(celda ->
                new ReadOnlyObjectWrapper<>(celda.getValue().isDisponible()));

        cmbCategoria.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Categoria categoria, boolean vacia) {
                super.updateItem(categoria, vacia);
                setText(vacia || categoria == null ? null : categoria.getNombre());
            }
        });

        cmbCategoria.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Categoria categoria, boolean vacia) {
                super.updateItem(categoria, vacia);
                setText(vacia || categoria == null ? "Todas las categorías" : categoria.getNombre());
            }
        });

        cmbCategoria.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> onActualizar());
        cargarCategorias();
        onActualizar();
    }

    //actualiza la tabla, usando la categoría seleccionada como filtro
    @FXML
    private void onActualizar() {
        try {
            Categoria categoria = cmbCategoria.getValue();
            if (categoria == null) {
                tblProductos.getItems().setAll(catalogo.listarProductosActivos());
            } else {
                tblProductos.getItems().setAll(catalogo.listarPorCategoria(categoria.getId()));
            }
            mostrarMensaje("Productos actualizados.");
        } catch (SQLException | RuntimeException error) {
            mostrarError("No se pudieron cargar los productos.", error);
        }
    }

    //abre el editor para crear un producto nuevo
    @FXML
    private void onNuevo() {
        try {
            sesion.exigirRol(Rol.ADMIN);
            navegador.mostrarProductoEditor(0L);
        } catch (RuntimeException error) {
            mostrarError("No se pudo abrir el editor.", error);
        }
    }

    //abre el editor para el producto seleccionado
    @FXML
    private void onEditar() {
        Producto producto = tblProductos.getSelectionModel().getSelectedItem();
        if (producto == null) {
            mostrarError("Seleccione un producto para editar.", null);
            return;
        }
        try {
            sesion.exigirRol(Rol.ADMIN);
            navegador.mostrarProductoEditor(producto.getId());
        } catch (RuntimeException error) {
            mostrarError("No se pudo abrir el producto.", error);
        }
    }

    //desactiva el producto seleccionado sin eliminarlo
    @FXML
    private void onDesactivar() {
        Producto producto = tblProductos.getSelectionModel().getSelectedItem();
        if (producto == null) {
            mostrarError("Seleccione un producto para desactivar.", null);
            return;
        }
        try {
            sesion.exigirRol(Rol.ADMIN);
            productoDAO.desactivar(Math.toIntExact(producto.getId()));
            mostrarMensaje("Producto desactivado.");
            onActualizar();
        } catch (RuntimeException error) {
            mostrarError("No se pudo desactivar el producto.", error);
        }
    }

    //carga las categorías activas para el filtro
    private void cargarCategorias() {
        try {
            cmbCategoria.getItems().setAll(catalogo.listarCategorias());
        } catch (SQLException | RuntimeException error) {
            mostrarError("No se pudieron cargar las categorías.", error);
        }
    }

    //actualiza el mensaje de estado
    private void mostrarMensaje(String mensaje) {
        if (lblMensaje != null) {
            lblMensaje.setText(mensaje);
        }
    }

    //presenta un error en la vista y en un diálogo
    private void mostrarError(String mensaje, Throwable error) {
        mostrarMensaje(mensaje);
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error de productos");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(error == null ? "" : error.getMessage());
        alerta.showAndWait();
    }
}