package cr.ac.una.est.programacion03proyecto02.controller.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.IAutenticacionService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.util.Objects;

public class loginController{

    private final IAutenticacionService autenticacion;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private Pane raiz;
    @FXML private TextField txtNombreUsuario;
    @FXML private PasswordField txtClave;
    @FXML private Button btnSalir;
    @FXML private Label lblMensaje;

    public loginController(IAutenticacionService autenticacion, SesionActual sesion, NavegadorVistas navegador){
        this.autenticacion = Objects.requireNonNull(autenticacion);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    // Prepara los mensajes del acceso único para todas las cuentas.
    @FXML
    private void initialize(){
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());
        Platform.runLater(() -> txtNombreUsuario.requestFocus());
    }

    // Autentica la cuenta y abre directamente el puesto correspondiente al rol guardado.
    @FXML
    private void onIniciar(){
        String nombre = txtNombreUsuario.getText();
        String clave = txtClave.getText();
        if (nombre == null || nombre.isBlank() || clave == null || clave.isBlank()){
            lblMensaje.setText("Escriba el nombre de usuario y la clave.");
            return;
        }

        Task<Usuario> tarea = new Task<>(){
            @Override
            protected Usuario call() throws Exception{
                return autenticacion.autenticar(nombre.trim(), clave);
            }
        };

        lblMensaje.setText("");
        raiz.setDisable(true);
        tarea.setOnSucceeded(evento -> {
            raiz.setDisable(false);
            Usuario usuario = tarea.getValue();
            txtClave.clear();

            if (usuario == null){
                lblMensaje.setText("Nombre o clave incorrectos.");
                return;
            }

            try{
                sesion.iniciar(usuario);
                navegador.mostrarInicioSegunRol();
            } catch (RuntimeException error){
                sesion.cerrar();
                lblMensaje.setText(error.getMessage());
            }
        });

        tarea.setOnFailed(evento -> {
            raiz.setDisable(false);
            txtClave.clear();
            Throwable error = tarea.getException();
            if (error instanceof java.sql.SQLException) {
                lblMensaje.setText("Error de base de datos: " + error.getMessage());
            } else {
                lblMensaje.setText("No se pudo completar el inicio de sesión.");
            }
        });

        new Thread(tarea, "autenticar-usuario").start();
    }

    // Cierra la aplicación desde la pantalla de acceso.
    @FXML
    private void onSalir(){
        Platform.exit();
    }
}
