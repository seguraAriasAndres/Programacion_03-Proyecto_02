package cr.ac.una.est.programacion03proyecto02.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.inventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.categoriaDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.productoDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.ingredienteDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.movimientoStockDAO;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;


public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        ConexionDB conexiones = new ConexionDB();
        inventarioService inventario = new inventarioService(conexiones, new ingredienteDAO(), new movimientoStockDAO());
        catalogoService catalogo = new catalogoService(conexiones, new productoDAO(), new categoriaDAO(), inventario);
        NavegadorVistas navegador = new NavegadorVistas();
        navegador.configurar(stage, catalogo, inventario, new SesionActual());
        stage.setTitle("Restaurante");
        if (getClass().getResource("/cr/ac/una/est/programacion03proyecto02/view/seguridad/login.fxml") != null) {
            navegador.mostrarLogin();
        } else {
            // El login sigue pendiente. No se inventa una sesión ADMIN para abrir el catálogo.
            Label mensaje = new Label("El inicio de sesión todavía está pendiente de implementación.");
            mensaje.setWrapText(true);
            stage.setScene(new Scene(new StackPane(mensaje), 640, 240));
            stage.show();
        }
    }
}
