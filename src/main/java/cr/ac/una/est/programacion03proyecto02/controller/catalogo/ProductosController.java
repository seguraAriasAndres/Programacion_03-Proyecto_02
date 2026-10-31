package cr.ac.una.est.programacion03proyecto02.controller.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.CatalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Locale;
import java.util.Objects;

//controlador para listar, filtrar, editar y desactivar productos
public class ProductosController {
    private final CatalogoService catalogo;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Number> colProductoId;
    @FXML private TableColumn<Producto, String> colProductoNombre;
    @FXML private TableColumn<Producto, String> colProductoCategoria;
    @FXML private TableColumn<Producto, String> colProductoPrecio;
    @FXML private TableColumn<Producto, Boolean> colProductoDisponible;
    @FXML private Label lblMensaje;
    @FXML private javafx.scene.layout.BorderPane raiz;
    private final ObservableList<Producto> productosDisponibles = FXCollections.observableArrayList();
    private FilteredList<Producto> productosFiltrados;

    public ProductosController(CatalogoService catalogo, SesionActual sesion, NavegadorVistas navegador) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    //configura la tabla y el filtro de categorías, y carga los productos
    @FXML
    private void initialize() {
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());
        productosFiltrados = new FilteredList<>(productosDisponibles, producto -> true);
        tblProductos.setItems(productosFiltrados);
        txtBuscar.textProperty().addListener((observable, anterior, actual) -> filtrarProductos(actual));

        colProductoId.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getId()));

        colProductoNombre.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getNombre()));

        colProductoCategoria.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        celda.getValue().getCategoria() == null ? "" : celda.getValue().getCategoria().getNombre()));

        colProductoPrecio.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        celda.getValue().getPrecioBase().toPlainString()));

        colProductoDisponible.setCellValueFactory(celda ->
                new ReadOnlyObjectWrapper<>(celda.getValue().isDisponible()));
        colProductoDisponible.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean disponible, boolean vacia) {
                super.updateItem(disponible, vacia);
                if (vacia || disponible == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label indicador = new Label(disponible ? "Disponible" : "No disponible");
                indicador.getStyleClass().addAll("stock-pill", disponible
                        ? "availability-available" : "availability-unavailable");
                setText(null);
                setGraphic(indicador);
            }
        });

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
    }

    //actualiza la tabla, usando la categoría seleccionada como filtro
    @FXML
    private void onActualizar() {
        Categoria categoria = cmbCategoria.getValue();
        TareaCatalogo.ejecutar(raiz, () -> {
            sesion.exigirRol(Rol.ADMIN);
            return categoria == null ? catalogo.listarProductosActivos()
                    : catalogo.listarPorCategoria(categoria.getId());
        }, productos -> {
            productosDisponibles.setAll(productos);
            filtrarProductos(txtBuscar.getText());
            mostrarMensaje("Productos actualizados.");
        }, error -> mostrarError("No se pudieron cargar los productos.", error));
    }

    // Filtra por nombre sin volver a consultar el catálogo.
    private void filtrarProductos(String consulta) {
        String texto = consulta == null ? "" : consulta.trim().toLowerCase(Locale.ROOT);
        productosFiltrados.setPredicate(producto -> producto.getNombre().toLowerCase(Locale.ROOT).contains(texto));
        tblProductos.getSelectionModel().clearSelection();
    }

    //abre la administración de categorías
    @FXML
    private void onCategorias() {
        try {
            sesion.exigirRol(Rol.ADMIN);
            navegador.abrir("catalogo/categorias.fxml");
        } catch (RuntimeException error) {
            mostrarError("No se pudieron abrir las categorías.", error);
        }
    }

    //regresa al menú principal sin pasar por categorías
    @FXML
    private void onVolver() {
        navegador.mostrarMenu();
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
            TareaCatalogo.ejecutar(raiz, () -> {
                catalogo.desactivarProducto(producto.getId());
                return null;
            }, resultado -> onActualizar(), error -> mostrarError("No se pudo desactivar el producto.", error));
        } catch (RuntimeException error) {
            mostrarError("No se pudo desactivar el producto.", error);
        }
    }

    //carga las categorías activas para el filtro
    private void cargarCategorias() {
        TareaCatalogo.ejecutar(raiz, () -> {
            sesion.exigirRol(Rol.ADMIN);
            return catalogo.listarCategorias();
        }, categorias -> {
            cmbCategoria.getItems().setAll(categorias);
            // null representa todas y permite quitar el filtro sin reiniciar la vista.
            cmbCategoria.getItems().add(0, null);
            onActualizar();
        }, error -> mostrarError("No se pudieron cargar las categorías.", error));
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
