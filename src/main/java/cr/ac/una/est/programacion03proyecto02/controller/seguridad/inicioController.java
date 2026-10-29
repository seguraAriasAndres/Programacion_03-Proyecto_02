package cr.ac.una.est.programacion03proyecto02.controller.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Objects;

public class inicioController{

    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private Label lblMensaje;

    public inicioController(SesionActual sesion, NavegadorVistas navegador){
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    @FXML
    private void initialize(){
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());
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

    // El cajero también debe autenticarse con una cuenta registrada.
    @FXML
    private void onIniciarCajero(){
        abrirLogin(Rol.CAJERO);
    }

    // Cierra la aplicación.
    @FXML
    private void onSalir(){
        Platform.exit();
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
