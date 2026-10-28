package cr.ac.una.est.programacion03proyecto02.controller.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.merma;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.movimientoStock;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.reabastecimiento;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class inventarioController{

    private final iinventarioService inventario;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private Pane raiz;
    @FXML private ComboBox<ingrediente> cmbIngrediente;
    @FXML private TableView<ingrediente> tblIngredientes;
    @FXML private TableView<movimientoStock> tblMovimientos;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtMotivo;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblMensaje;

    public inventarioController(iinventarioService inventario, SesionActual sesion, NavegadorVistas navegador){
        this.inventario = Objects.requireNonNull(inventario);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    // Comprueba el rol y carga los datos iniciales.
    @FXML
    private void initialize(){
        sesion.exigirAlgunoDe(Rol.ADMIN, Rol.STOCK_MANAGER);
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
