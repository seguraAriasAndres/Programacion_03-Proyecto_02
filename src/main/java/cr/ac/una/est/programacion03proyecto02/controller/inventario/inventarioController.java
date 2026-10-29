package cr.ac.una.est.programacion03proyecto02.controller.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.merma;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.movimientoStock;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.reabastecimiento;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.unidadMedida;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class inventarioController{

    private final iinventarioService inventario;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private Pane raiz;
    @FXML private ComboBox<ingrediente> cmbIngrediente;
    @FXML private TableView<ingrediente> tblIngredientes;
    @FXML private TableColumn<ingrediente, Number> colIngredienteId;
    @FXML private TableColumn<ingrediente, String> colIngredienteNombre;
    @FXML private TableColumn<ingrediente, Number> colIngredienteDisponible;
    @FXML private TableColumn<ingrediente, Number> colIngredienteMinimo;
    @FXML private TableColumn<ingrediente, Boolean> colIngredienteActiva;
    @FXML private TableView<movimientoStock> tblMovimientos;
    @FXML private TableColumn<movimientoStock, String> colMovFecha;
    @FXML private TableColumn<movimientoStock, String> colMovIngrediente;
    @FXML private TableColumn<movimientoStock, String> colMovTipo;
    @FXML private TableColumn<movimientoStock, Number> colMovCantidad;
    @FXML private TableColumn<movimientoStock, String> colMovUsuario;
    @FXML private TableColumn<movimientoStock, String> colMovMotivo;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtMotivo;
    @FXML private TextField txtNombreIngrediente;
    @FXML private ComboBox<unidadMedida> cmbUnidad;
    @FXML private TextField txtStockMinimo;
    @FXML private CheckBox chkActivo;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblMensaje;

    // Ingrediente seleccionado en la tabla; null cuando se crea uno nuevo.
    private ingrediente seleccionado;

    public inventarioController(iinventarioService inventario, SesionActual sesion, NavegadorVistas navegador){
        this.inventario = Objects.requireNonNull(inventario);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    // Comprueba el rol y carga los datos iniciales.
    @FXML
    private void initialize(){
        sesion.exigirAlgunoDe(Rol.ADMIN, Rol.STOCK_MANAGER);

        colIngredienteId.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getId()));
        colIngredienteNombre.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getNombre()));
        colIngredienteDisponible.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getCantidadDisponible()));
        colIngredienteMinimo.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getStockMinimo()));
        colIngredienteActiva.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().isActivo()));

        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colMovFecha.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getFecha().format(formatoFecha)));
        colMovIngrediente.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getIngrediente().getNombre()));
        colMovTipo.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                celda.getValue().variacion() > 0 ? "Reabastecimiento" : "Merma"));
        colMovCantidad.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getCantidad()));
        colMovUsuario.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getUsuario()));
        colMovMotivo.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getMotivo()));

        tblIngredientes.setPlaceholder(new Label("No hay ingredientes registrados."));
        tblMovimientos.setPlaceholder(new Label("No hay movimientos en el rango indicado."));

        cmbIngrediente.setCellFactory(lista -> new ListCell<>(){
            @Override
            protected void updateItem(ingrediente item, boolean vacio){
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : item.getNombre());
            }
        });
        cmbIngrediente.setButtonCell(new ListCell<>(){
            @Override
            protected void updateItem(ingrediente item, boolean vacio){
                super.updateItem(item, vacio);
                setText(vacio || item == null ? "Ingrediente" : item.getNombre());
            }
        });

        cmbUnidad.setItems(FXCollections.observableArrayList(unidadMedida.values()));
        cmbUnidad.setButtonCell(new ListCell<>(){
            @Override
            protected void updateItem(unidadMedida item, boolean vacio){
                super.updateItem(item, vacio);
                setText(vacio || item == null ? "Unidad" : item.name());
            }
        });

        tblIngredientes.getSelectionModel().selectedItemProperty().addListener((obs, antes, actual) -> mostrarEnFormulario(actual));

        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
        cargarDatos();
    }

    // Consulta ingredientes y movimientos dentro del rango indicado.
    @FXML
    private void onActualizar(){
        cargarDatos();
    }

    // Registra una entrada de inventario con el ingrediente seleccionado.
    @FXML
    private void onReabastecer(){
        registrarMovimiento(true);
    }

    // Registra una merma con el ingrediente seleccionado.
    @FXML
    private void onRegistrarMerma(){
        registrarMovimiento(false);
    }

    // Vuelve al menú de acuerdo con la sesión activa.
    @FXML
    private void onVolver(){
        navegador.mostrarMenu();
    }

    // Deja el formulario listo para crear un ingrediente nuevo (id 0).
    @FXML
    private void onNueva(){
        tblIngredientes.getSelectionModel().clearSelection();
        mostrarEnFormulario(null);
        txtNombreIngrediente.requestFocus();
    }

    // Guarda el ingrediente del formulario; con selección actualiza, sin ella crea.
    @FXML
    private void onGuardar(){
        String nombre = txtNombreIngrediente.getText() == null ? "" : txtNombreIngrediente.getText().trim();
        if (nombre.isEmpty()){
            lblMensaje.setText("Escriba el nombre del ingrediente.");
            return;
        }

        unidadMedida unidad = cmbUnidad.getValue();
        if (unidad == null){
            lblMensaje.setText("Seleccione la unidad de medida.");
            return;
        }

        double stockMinimo;
        try{
            stockMinimo = Double.parseDouble(txtStockMinimo.getText().trim());
            if (!Double.isFinite(stockMinimo) || stockMinimo < 0){
                throw new NumberFormatException();
            }
        } catch (RuntimeException error){
            lblMensaje.setText("El stock mínimo debe ser un número mayor o igual a cero.");
            return;
        }

        long id = seleccionado == null ? 0 : seleccionado.getId();
        double cantidad = seleccionado == null ? 0 : seleccionado.getCantidadDisponible();
        ingrediente nuevo = new ingrediente(id, nombre, cantidad, unidad, stockMinimo, chkActivo.isSelected());

        Task<Void> tarea = new Task<>(){
            @Override
            protected Void call() throws Exception{
                inventario.guardarIngrediente(nuevo);
                return null;
            }
        };

        ejecutar(tarea, resultado -> {
            mostrarEnFormulario(null);
            lblMensaje.setText("Ingrediente guardado.");
            cargarDatos();
        });
    }

    // Filtra la tabla a los ingredientes activos por debajo de su mínimo.
    @FXML
    private void onBajoMinimo(){
        Task<List<ingrediente>> tarea = new Task<>(){
            @Override
            protected List<ingrediente> call() throws Exception{
                return inventario.listarBajoMinimo();
            }
        };

        ejecutar(tarea, bajoMinimo -> {
            tblIngredientes.setItems(FXCollections.observableArrayList(bajoMinimo));
            lblMensaje.setText(bajoMinimo.isEmpty()
                    ? "No hay ingredientes bajo el mínimo."
                    : "Ingredientes bajo el mínimo: " + bajoMinimo.size() + ".");
        });
    }

    // Copia el ingrediente seleccionado al formulario; null deja el formulario limpio.
    private void mostrarEnFormulario(ingrediente ingrediente){
        seleccionado = ingrediente;
        txtNombreIngrediente.setText(ingrediente == null ? "" : ingrediente.getNombre());
        cmbUnidad.setValue(ingrediente == null ? null : ingrediente.getUnidadMedida());
        txtStockMinimo.setText(ingrediente == null ? "" : String.valueOf(ingrediente.getStockMinimo()));
        chkActivo.setSelected(ingrediente == null || ingrediente.isActivo());
    }

    // Actualiza las tablas sin bloquear el hilo gráfico durante las consultas.
    private void cargarDatos(){
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null || desde.isAfter(hasta)){
            lblMensaje.setText("Indique un rango de fechas válido.");
            return;
        }

        Task<datosInventario> tarea = new Task<>(){
            @Override
            protected datosInventario call() throws Exception{
                List<ingrediente> ingredientes = inventario.listarIngredientes();
                List<movimientoStock> movimientos = inventario.listarMovimientos(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
                return new datosInventario(ingredientes, movimientos);
            }
        };

        ejecutar(tarea, datos -> {
            tblIngredientes.setItems(FXCollections.observableArrayList(datos.ingredientes()));
            tblMovimientos.setItems(FXCollections.observableArrayList(datos.movimientos()));
            cmbIngrediente.setItems(FXCollections.observableArrayList(datos.ingredientes().stream().filter(ingrediente::isActivo).toList()));
            lblMensaje.setText("Inventario actualizado.");
        });
    }

    // Valida los campos y registra el movimiento elegido en segundo plano.
    private void registrarMovimiento(boolean esReabastecimiento){
        ingrediente seleccionado = cmbIngrediente.getValue();
        if (seleccionado == null){
            lblMensaje.setText("Seleccione un ingrediente.");
            return;
        }

        double cantidad;
        try{
            cantidad = Double.parseDouble(txtCantidad.getText().trim());
            if (!Double.isFinite(cantidad) || cantidad <= 0){
                throw new NumberFormatException();
            }
        } catch (RuntimeException error){
            lblMensaje.setText("La cantidad debe ser un número mayor que cero.");
            return;
        }

        String motivo = txtMotivo.getText();
        if (motivo == null || motivo.isBlank()){
            lblMensaje.setText("Escriba el motivo del movimiento.");
            return;
        }

        String nombreUsuario = sesion.getUsuario().getNombreUsuario();
        movimientoStock movimiento = esReabastecimiento
                ? new reabastecimiento(0, seleccionado, cantidad, LocalDateTime.now(), nombreUsuario, motivo)
                : new merma(0, seleccionado, cantidad, LocalDateTime.now(), nombreUsuario, motivo);

        Task<Void> tarea = new Task<>(){
            @Override
            protected Void call() throws Exception{
                inventario.registrarMovimiento(movimiento);
                return null;
            }
        };

        ejecutar(tarea, resultado -> {
            txtCantidad.clear();
            txtMotivo.clear();
            lblMensaje.setText(esReabastecimiento ? "Reabastecimiento registrado." : "Merma registrada.");
            cargarDatos();
        });
    }

    // Ejecuta una tarea y muestra el resultado en el hilo de JavaFX.
    private <T> void ejecutar(Task<T> tarea, java.util.function.Consumer<T> alCompletar){
        raiz.setDisable(true);
        tarea.setOnSucceeded(evento -> {
            raiz.setDisable(false);
            alCompletar.accept(tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            raiz.setDisable(false);
            Throwable error = tarea.getException();
            lblMensaje.setText(error == null ? "No se pudo completar la operación." : "No se pudo completar la operación: " + error.getMessage());
        });
        new Thread(tarea, "operacion-inventario").start();
    }

    private record datosInventario(List<ingrediente> ingredientes, List<movimientoStock> movimientos){
    }
}
