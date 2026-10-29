package cr.ac.una.est.programacion03proyecto02.controller.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Objects;

public class inicioController{

    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private TextField txtNombreCajero;
    @FXML private Label lblMensaje;
    @FXML private Button btnTema;

    public inicioController(SesionActual sesion, NavegadorVistas navegador){
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    @FXML
    private void initialize(){
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());
        actualizarTextoTema();
    }

    // Abre el login para la opción de administrador.
    @FXML
    private void onLoginAdmin(){
        abrirLogin(Rol.ADMIN);
    }

    // Abre el login para la opción de encargado de inventario.
    @FXML
    private void onLoginStockManager(){
        abrirLogin(Rol.STOCK_MANAGER);
    }

    // Inicia la sesión de cajero con el nombre escrito.
    @FXML
    private void onIniciarCajero(){
        try{
            sesion.iniciarComoCajero(txtNombreCajero.getText());
            navegador.mostrarMenu();
        } catch (RuntimeException error){
            mostrarMensaje(error.getMessage());
        }
    }

    // Cierra la aplicación.
    @FXML
    private void onSalir(){
        Platform.exit();
    }

    @FXML
    private void onCambiarTema(){
        navegador.alternarTema();
        actualizarTextoTema();
    }

    private void actualizarTextoTema(){
        btnTema.setText(navegador.esTemaCalido() ? "Usar tema frío" : "Usar tema cálido");
    }

    // Abre el login con el rol que la persona eligió.
    private void abrirLogin(Rol rol){
        try{
            navegador.mostrarLogin(rol);
        } catch (RuntimeException error){
            mostrarMensaje(error.getMessage());
        }
    }

    // Muestra un mensaje de validación en la pantalla inicial.
    private void mostrarMensaje(String mensaje){
        lblMensaje.setText(mensaje == null ? "No se pudo completar la acción." : mensaje);
    }
}
