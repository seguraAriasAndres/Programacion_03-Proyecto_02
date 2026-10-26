package cr.ac.una.est.programacion03proyecto02.controller.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Objects;

//controlador para consultar, crear y guardar categorías
public class CategoriasController {
    private final catalogoService catalogo;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    private Categoria seleccionada;

    @FXML private TableView<Categoria> tblCategorias;
    @FXML private TableColumn<Categoria, Number> colCategoriaId;
    @FXML private TableColumn<Categoria, String> colCategoriaNombre;
    @FXML private TableColumn<Categoria, Boolean> colCategoriaActiva;
    @FXML private TextField txtNombre;
    @FXML private CheckBox chkActiva;
    @FXML private Label lblMensaje;
    @FXML private javafx.scene.layout.BorderPane raiz;

    public CategoriasController(catalogoService catalogo, SesionActual sesion, NavegadorVistas navegador) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    //configura las columnas, la selección y carga las categorías
    @FXML
    private void initialize() {
        colCategoriaId.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getId()));

        colCategoriaNombre.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getNombre()));

        colCategoriaActiva.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().isActiva()));

        tblCategorias.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) ->
                        mostrarEnFormulario(actual));
        onActualizar();
    }

    //actualiza la tabla con todas las categorías de administración
    @FXML
    private void onActualizar() {
        TareaCatalogo.ejecutar(raiz, () -> {
            sesion.exigirRol(Rol.ADMIN);
            return catalogo.listarCategoriasAdministracion();
        }, categorias -> {
            tblCategorias.getItems().setAll(categorias);
            mostrarMensaje("Categorías actualizadas.");
        }, error -> mostrarError("No se pudieron cargar las categorías.", error));
    }

    //limpia el formulario y prepara una categoría nueva
    @FXML
    private void onNueva() {
        tblCategorias.getSelectionModel().clearSelection();
        seleccionada = new Categoria();
        txtNombre.clear();
        chkActiva.setSelected(true);
        mostrarMensaje("Ingrese el nombre de la categoría.");
    }

    //guarda la categoría nueva o actualiza la seleccionada
    @FXML
    private void onGuardar() {
        try {
            sesion.exigirRol(Rol.ADMIN);
            Categoria cambios = new Categoria(seleccionada == null ? 0 : seleccionada.getId(),
                    txtNombre.getText(), chkActiva.isSelected());
            TareaCatalogo.ejecutar(raiz, () -> {
                catalogo.guardarCategoria(cambios);
                return catalogo.listarCategoriasAdministracion();
            }, categorias -> {
                tblCategorias.getItems().setAll(categorias);
                seleccionarPorId(cambios.getId());
                mostrarMensaje("Categoría guardada.");
            }, error -> mostrarError("No se pudo guardar o recargar la categoría. Consulte la lista antes de reintentar.", error));
        } catch (RuntimeException error) {
            mostrarError("No se pudo guardar la categoría.", error);
        }
    }

    //copia los valores de una categoría seleccionada al formulario
    private void mostrarEnFormulario(Categoria categoria) {
        seleccionada = categoria;
        if (categoria == null) {
            txtNombre.clear();
            chkActiva.setSelected(true);
            return;
        }
        txtNombre.setText(categoria.getNombre());
        chkActiva.setSelected(categoria.isActiva());
    }

    //selecciona en la tabla la categoría con el id indicado
    private void seleccionarPorId(long id) {
        for (Categoria categoria : tblCategorias.getItems()) {
            if (categoria.getId() == id) {
                tblCategorias.getSelectionModel().select(categoria);
                return;
            }
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
        String detalle = error == null ? mensaje : error.getMessage();
        mostrarMensaje(mensaje);
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error de categorías");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(detalle);
        alerta.showAndWait();
    }
}
