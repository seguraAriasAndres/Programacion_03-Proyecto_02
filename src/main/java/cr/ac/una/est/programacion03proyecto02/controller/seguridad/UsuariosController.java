package cr.ac.una.est.programacion03proyecto02.controller.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.ResumenUsuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.UsuariosService;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import java.util.List;
import java.util.Objects;

public final class UsuariosController {
    private final UsuariosService usuarios;
    private final NavegadorVistas navegador;
    @FXML private BorderPane raiz;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave, txtConfirmacion;
    @FXML private ComboBox<Rol> cmbRol;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblMensaje;
    @FXML private TableView<ResumenUsuario> tblUsuarios;
    @FXML private TableColumn<ResumenUsuario,Long> colId;
    @FXML private TableColumn<ResumenUsuario,String> colNombre,colRol,colActivo;
    @FXML private Button btnCambiarEstado;
    @FXML private Button btnGuardarCambios;

    public UsuariosController(UsuariosService usuarios,NavegadorVistas navegador) {
        this.usuarios=Objects.requireNonNull(usuarios); this.navegador=Objects.requireNonNull(navegador);
    }
    @FXML private void initialize() {
        cmbRol.getItems().setAll(Rol.values()); cmbRol.setValue(Rol.CAJERO);
        colId.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().id()));
        colNombre.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().nombreUsuario()));
        colRol.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().rol().name()));
        colActivo.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().activo()?"Sí":"No"));
        tblUsuarios.setPlaceholder(new Label("No hay cuentas registradas."));
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            boolean haySeleccion = actual != null;
            btnCambiarEstado.setDisable(!haySeleccion);
            btnCambiarEstado.setText(haySeleccion && actual.activo() ? "Desactivar seleccionado" : "Reactivar seleccionado");
            btnGuardarCambios.setDisable(!haySeleccion);
            if (haySeleccion) {
                txtUsuario.setText(actual.nombreUsuario());
                cmbRol.setValue(actual.rol());
                chkActivo.setSelected(actual.activo());
                txtClave.clear();
                txtConfirmacion.clear();
            }
        });
        onActualizar();
    }
    @FXML private void onActualizar() { ejecutar(usuarios::listar,"Cuentas actualizadas."); }
    @FXML private void onCambiarEstado() {
        ResumenUsuario seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        boolean nuevoEstado = !seleccionado.activo();
        ejecutar(() -> {
            usuarios.cambiarEstado(seleccionado.id(), nuevoEstado);
            return usuarios.listar();
        }, "Estado de cuenta actualizado.");
    }
    @FXML private void onGuardarCambios() {
        ResumenUsuario seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        String nombre = txtUsuario.getText();
        String clave = txtClave.getText();
        String confirmacion = txtConfirmacion.getText();
        if (clave != null && !clave.isBlank() && !Objects.equals(clave, confirmacion)) {
            lblMensaje.setText("La confirmación de clave no coincide.");
            return;
        }
        Rol rol = cmbRol.getValue();
        boolean activo = chkActivo.isSelected();
        ejecutar(() -> {
            usuarios.editarUsuario(seleccionado.id(), nombre, rol, activo, clave);
            return usuarios.listar();
        }, "Usuario actualizado.");
    }
    @FXML private void onCrear() {
        if (!Objects.equals(txtClave.getText(),txtConfirmacion.getText())) {
            lblMensaje.setText("La confirmación de clave no coincide."); return;
        }
        String nombre=txtUsuario.getText(),clave=txtClave.getText(); Rol rol=cmbRol.getValue(); boolean activo=chkActivo.isSelected();
        ejecutar(()->{
            usuarios.crear(nombre,clave,rol,activo);
            return usuarios.listar();
        },"Cuenta creada. Ya puede iniciar sesión con su usuario y clave.");
    }
    @FXML private void onVolver() { navegador.mostrarMenu(); }
    private void ejecutar(Consulta consulta,String mensaje) {
        if (raiz.isDisabled()) return;
        raiz.setDisable(true);
        Task<List<ResumenUsuario>> tarea=new Task<>() {
            @Override protected List<ResumenUsuario> call() throws Exception { return consulta.ejecutar(); }
        };
        tarea.setOnSucceeded(e->{
            raiz.setDisable(false); tblUsuarios.getItems().setAll(tarea.getValue()); lblMensaje.setText(mensaje);
            txtClave.clear(); txtConfirmacion.clear();
            tblUsuarios.getSelectionModel().clearSelection();
            btnGuardarCambios.setDisable(true);
            if(mensaje.startsWith("Cuenta creada"))txtUsuario.clear();
        });
        tarea.setOnFailed(e->{
            raiz.setDisable(false); txtClave.clear(); txtConfirmacion.clear();
            tblUsuarios.getSelectionModel().clearSelection();
            btnGuardarCambios.setDisable(true);
            lblMensaje.setText(tarea.getException().getMessage());
        });
        new Thread(tarea,"administrar-usuarios").start();
    }
    @FunctionalInterface private interface Consulta { List<ResumenUsuario> ejecutar() throws Exception; }
}
