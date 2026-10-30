package cr.ac.una.est.programacion03proyecto02.controller.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Presenta un recibo ya cobrado; no cobra ni consulta directamente la base de datos. */
public class ReciboController {
    private final NavegadorVistas navegadorVistas;
    private Recibo recibo;

    // FXMLLoader inyecta los controles cuyos fx:id coinciden con estos nombres.
    @FXML private Label lblIdRecibo;
    @FXML private Label lblPedido;
    @FXML private Label lblTotal;
    @FXML private Label lblFecha;
    @FXML private Label lblMetodoPago;
    @FXML private Label lblUsuario;
    @FXML private TableView<lineaPedido> tblDesglose;
    @FXML private TableColumn<lineaPedido, String> colProducto;
    @FXML private TableColumn<lineaPedido, String> colOpciones;
    @FXML private TableColumn<lineaPedido, Integer> colCantidad;
    @FXML private TableColumn<lineaPedido, String> colPrecio;
    @FXML private TableColumn<lineaPedido, String> colSubtotal;

    // La fabrica de controladores entrega el navegador al crear este controlador.
    // El recibo se entrega despues de load(), cuando los controles ya existen.
    public ReciboController(NavegadorVistas navegadorVistas) {
        this.navegadorVistas = Objects.requireNonNull(navegadorVistas, "Falta el navegador.");
    }

    @FXML
    private void initialize() {
        // FXMLLoader lo llama despues de inyectar los controles del FXML.
        // Cada columna indica que dato debe tomar de una linea del recibo.
        colProducto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNombreProducto()));
        colOpciones.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDescripcionExtras()));
        colCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCantidad()));
        colPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(formatearMonto(c.getValue().getPrecioUnitario())));
        colSubtotal.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(formatearMonto(c.getValue().calcularSubtotal())));
        tblDesglose.setPlaceholder(new Label("El recibo no tiene lineas."));
    }

    public void mostrarRecibo(Recibo recibo) {
        // Muestra los datos historicos, incluido el usuario que realizo el cobro.
        Objects.requireNonNull(recibo, "Falta el recibo que se va a mostrar.");
        if (lblIdRecibo == null) {
            throw new IllegalStateException("Primero debe cargarse recibo.fxml con FXMLLoader.load().");
        }
        this.recibo = recibo;
        lblIdRecibo.setText(Long.toString(recibo.getId()));
        lblPedido.setText(Long.toString(recibo.getIdPedido()));
        lblTotal.setText(formatearMonto(recibo.getTotal()));
        lblFecha.setText(recibo.getPago().getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lblMetodoPago.setText(recibo.getPago().getMetodo().name());
        lblUsuario.setText(recibo.getPago().getUsuario());
        // Una copia para la tabla respeta la lista protegida del modelo.
        tblDesglose.setItems(FXCollections.observableArrayList(recibo.getLineas()));
    }

    @FXML
    private void onCerrar() {
        navegadorVistas.mostrarPedidosActivos();
    }

    private String formatearMonto(BigDecimal monto) {
        // Solo cambia la presentacion; el importe del modelo se conserva.
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
