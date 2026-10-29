package cr.ac.una.est.programacion03proyecto02.app;

import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.inventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.CobroService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.PedidoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.reportes.ReporteService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.AutenticacionService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.UsuariosService;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.seguridad.UsuarioDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.categoriaDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.productoDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.ingredienteDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.movimientoStockDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import cr.ac.una.est.programacion03proyecto02.view.personalizacion.MarcaVentana;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloApplication extends Application{
    @Override
    public void stop(){
        ConexionDB.cerrarPool();
    }

    private record serviciosAplicacion(inventarioService inventario, catalogoService catalogo, PedidoService pedidos, CobroService cobros){
    }

    // Prepara los servicios en segundo plano y luego abre la pantalla inicial.
    @Override
    public void start(Stage stage){
        stage.setTitle("Restaurante");
        MarcaVentana.aplicar(stage, MarcaVentana.desdeConfiguracion());

        Label estado = new Label("Preparando servicios...");
        estado.setWrapText(true);
        stage.setScene(new Scene(new StackPane(estado), 640, 240));
        stage.show();

        ConexionDB conexiones = new ConexionDB();
        SesionActual sesion = new SesionActual();
        NavegadorVistas navegador = new NavegadorVistas();

        Task<serviciosAplicacion> tarea = new Task<>(){

            @Override
            protected serviciosAplicacion call() throws Exception{
                inventarioService inventario = new inventarioService(conexiones, new ingredienteDAO(), new movimientoStockDAO());
                catalogoService catalogo = new catalogoService(conexiones, new productoDAO(), new categoriaDAO(), inventario);
                PedidoRepository repositorio = new PedidoRepository(conexiones);
                PedidoService pedidos = new PedidoService(conexiones, catalogo, inventario, repositorio);
                CobroService cobros = new CobroService(repositorio, new ReciboDAO(), conexiones);

                return new serviciosAplicacion(inventario, catalogo, pedidos, cobros);
            }
        };

        tarea.setOnSucceeded(evento -> {
            serviciosAplicacion servicios = tarea.getValue();

            navegador.configurar(stage, servicios.catalogo(), servicios.inventario(), sesion);
            navegador.configurarPedidos(servicios.pedidos(), servicios.cobros());
            UsuarioDAO usuarios = new UsuarioDAO(conexiones);
            navegador.configurarPantallas(new AutenticacionService(usuarios), new ReporteService(servicios.pedidos()));
            navegador.configurarUsuarios(new UsuariosService(usuarios,sesion));

            try{
                navegador.mostrarInicio();
            } catch (RuntimeException error){
                estado.setText("Los servicios están listos, pero no se pudo abrir la pantalla inicial. " + "Verifique que exista seguridad/inicio.fxml y esté configurada.");
            }
        });

        tarea.setOnFailed(evento -> estado.setText("No se pudieron preparar los servicios. " + "Revise la conexión y vuelva a iniciar."));

        Thread hilo = new Thread(tarea, "iniciar-servicios");
        hilo.start();
    }
}
