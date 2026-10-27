package cr.ac.una.est.programacion03proyecto02.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.PedidoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.CobroService;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;
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
            Label estado = new Label("Preparando servicios...");
            stage.setScene(new Scene(new StackPane(estado), 640, 240));
            stage.show();
            record Servicios(PedidoService pedidos, CobroService cobros) { }
            Task<Servicios> tarea = new Task<>() {
                @Override protected Servicios call() throws Exception {
                    // Pedidos y cobro comparten una sola cache. El constructor consulta SQL fuera de JavaFX.
                    PedidoRepository repositorio = new PedidoRepository(conexiones);
                    PedidoService pedidos = new PedidoService(conexiones, catalogo, inventario, repositorio);
                    return new Servicios(pedidos, new CobroService(repositorio, new ReciboDAO(), conexiones));
                }
            };
            tarea.setOnSucceeded(e -> {
                navegador.configurarPedidos(tarea.getValue().pedidos(), tarea.getValue().cobros());
                try { navegador.mostrarLogin(); }
                catch (RuntimeException error) { estado.setText("No se pudo abrir el inicio de sesión."); }
            });
            tarea.setOnFailed(e -> estado.setText("No se pudieron preparar los servicios. Revise la conexión y vuelva a iniciar."));
            new Thread(tarea, "iniciar-servicios").start();
        } else {
            // El login sigue pendiente. No se inventa una sesión ADMIN para abrir el catálogo.
            Label mensaje = new Label("El inicio de sesión todavía está pendiente de implementación.");
            mensaje.setWrapText(true);
            stage.setScene(new Scene(new StackPane(mensaje), 640, 240));
            stage.show();
        }
    }
}
