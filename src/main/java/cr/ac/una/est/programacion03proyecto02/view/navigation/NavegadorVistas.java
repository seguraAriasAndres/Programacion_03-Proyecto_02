package cr.ac.una.est.programacion03proyecto02.view.navigation;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;

/**
 * Estructura de navegación entre pantallas según el UML de presentación.
 * Los métodos están pendientes de implementación y todavía no cambian la vista.
 */
public class NavegadorVistas {

    public void mostrarLogin() {
        // TODO: Cargar y mostrar la pantalla de inicio de sesión.
    }

    public void mostrarMenu() {
        // TODO: Mostrar el menú correspondiente al usuario de la sesión actual.
    }

    public void abrir(String vista) {
        // TODO: Cargar el FXML indicado y mostrarlo en el contenedor de la aplicación.
    }

    public void abrirPedido(long pedidoId) {
        // TODO: Abrir la pantalla de toma de pedido y proporcionar el ID a su controlador.
    }

    public void abrirCobro(long pedidoId) {
        // TODO: Abrir la pantalla de cobro y proporcionar el ID a su controlador.
    }

    public void abrirProducto(long productoId) {
        // TODO: Abrir el editor de productos y proporcionar el ID a su controlador.
    }

    public void mostrarRecibo(Recibo recibo) {
        // TODO: Abrir la pantalla del recibo y entregar el objeto a su controlador.
    }
}
