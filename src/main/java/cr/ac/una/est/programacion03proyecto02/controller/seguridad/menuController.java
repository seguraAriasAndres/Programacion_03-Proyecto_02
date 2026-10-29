package cr.ac.una.est.programacion03proyecto02.controller.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Objects;

public class menuController{

    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private Label lblUsuario;
    @FXML private Button btnInventario;
    @FXML private Button btnCatalogo;
    @FXML private Button btnReportes;
    @FXML private Button btnPedidos;

    public menuController(SesionActual sesion, NavegadorVistas navegador){
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    // Presenta las opciones habilitadas para el rol de la sesión.
    @FXML
    private void initialize(){
        sesion.exigirAlgunoDe(Rol.ADMIN, Rol.STOCK_MANAGER, Rol.CAJERO);

        Usuario usuario = sesion.getUsuario();
        Rol rol = usuario.getRol();
        lblUsuario.setText(usuario.getNombreUsuario() + " - " + rol);

        boolean esAdmin = rol == Rol.ADMIN;
        boolean esCajero = rol == Rol.CAJERO;

        mostrarBoton(btnInventario, rol == Rol.ADMIN || rol == Rol.STOCK_MANAGER);
        mostrarBoton(btnCatalogo, esAdmin);
        mostrarBoton(btnReportes, esAdmin);
        mostrarBoton(btnPedidos, esCajero);
    }

    // Abre la pantalla de inventario para el administrador o el encargado.
    @FXML
    private void onInventario(){
        navegador.abrir("inventario/inventario.fxml");
    }

    // Abre la administración del catálogo.
    @FXML
    private void onCatalogo(){
        navegador.mostrarProductos();
    }

    // Abre los reportes administrativos.
    @FXML
    private void onReportes(){
        navegador.abrir("reportes/reportes.fxml");
    }

    // Abre los pedidos activos del cajero.
    @FXML
    private void onPedidos(){
        navegador.mostrarPedidosActivos();
    }

    // Cierra la sesión y vuelve al inicio.
    @FXML
    private void onCerrarSesion(){
        navegador.cerrarSesionYMostrarInicio();
    }

    // Controla la visibilidad y el espacio ocupado por cada botón.
    private void mostrarBoton(Button boton, boolean mostrar){
        boton.setVisible(mostrar);
        boton.setManaged(mostrar);
    }
}
