package cr.ac.una.est.programacion03proyecto02.controller.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.IAutenticacionService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;

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
    @FXML private Label lblRol;
    @FXML private Label lblMensaje;
    @FXML private Button btnIniciar;

    public loginController(IAutenticacionService autenticacion, SesionActual sesion, NavegadorVistas navegador){
        this.autenticacion = Objects.requireNonNull(autenticacion);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    // Muestra el rol solicitado al llegar desde la pantalla inicial.
    @FXML
    private void initialize(){
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());

        Rol rol = navegador.getRolLoginSeleccionado();
        if (rol == null) {
            lblRol.setText("Seleccione el tipo de acceso desde el inicio.");
            btnIniciar.setDisable(true);
        } else{
            lblRol.setText("Acceso: " + rol);
        }
    }

    // Autentica al usuario y crea la sesión si coincide el rol solicitado.
    @FXML
    private void onIniciar(){
        String nombre = txtNombreUsuario.getText();
        String clave = txtClave.getText();
        Rol rolEsperado = navegador.getRolLoginSeleccionado();

        if (rolEsperado == null){
            lblMensaje.setText("Vuelva al inicio y seleccione un tipo de acceso.");
            return;
        }
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

        raiz.setDisable(true);
        tarea.setOnSucceeded(evento -> {
            raiz.setDisable(false);
            Usuario usuario = tarea.getValue();

            if (usuario == null){
                txtClave.clear();
                lblMensaje.setText("Nombre o clave incorrectos.");
                return;
            }
            if (usuario.getRol() != rolEsperado){
                txtClave.clear();
                lblMensaje.setText("La cuenta no tiene el rol seleccionado.");
                return;
            }

            try{
                sesion.iniciar(usuario);
                navegador.mostrarMenu();
            } catch (RuntimeException error){
                lblMensaje.setText(error.getMessage());
            }
        });

        tarea.setOnFailed(evento -> {
            raiz.setDisable(false);
            txtClave.clear();
            lblMensaje.setText("No se pudo completar el inicio de sesión.");
        });

        new Thread(tarea, "autenticar-usuario").start();
    }

    // Regresa a la pantalla inicial.
    @FXML
    private void onVolver(){
        navegador.mostrarInicio();
    }
}
