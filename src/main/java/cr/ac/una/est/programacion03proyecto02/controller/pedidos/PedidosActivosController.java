package cr.ac.una.est.programacion03proyecto02.controller.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.PedidoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class PedidosActivosController {
    private final PedidoService pedidos;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;
    private TareaPedidos tareas;
    @FXML private BorderPane raiz;
    @FXML private Label lblEstado;
    @FXML private TableView<Pedido> tblPedidos;
    @FXML private TableColumn<Pedido, Long> colId;
    @FXML private TableColumn<Pedido, String> colCliente, colFecha, colPreparacion, colPago, colTotal;
    @FXML private Button btnAbrir, btnCancelar, btnPreparar, btnEntregar, btnCobrar, btnVerRecibo;

    public PedidosActivosController(PedidoService pedidos, SesionActual sesion, NavegadorVistas navegador) {
        this.pedidos = Objects.requireNonNull(pedidos);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    @FXML private void initialize() {
        tareas = new TareaPedidos(raiz);
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getIdPedido()));
        colCliente.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNombreCliente()));
        colFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getFechaPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colPreparacion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isConfirmado()
                ? c.getValue().getEstadoPreparacion().name() : "BORRADOR"));
        colPago.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstadoPago().name()));
        colPreparacion.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String estado, boolean vacia) {
                super.updateItem(estado, vacia);
                mostrarEstado(this, estado, vacia, switch (estado == null ? "" : estado) {
                    case "BORRADOR", "CANCELADO" -> "status-neutral";
                    case "EN_PREPARACION" -> "status-warning";
                    case "ENTREGADO" -> "status-success";
                    default -> "status-neutral";
                });
            }
        });
        colPago.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String estado, boolean vacia) {
                super.updateItem(estado, vacia);
                mostrarEstado(this, estado, vacia, "PAGADO".equals(estado) ? "status-success" : "status-warning");
            }
        });
        colTotal.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().calcularTotal().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        tblPedidos.setPlaceholder(new Label("No hay pedidos activos."));
        var seleccion = tblPedidos.getSelectionModel().selectedItemProperty();
        seleccion.addListener((observable, anterior, actual) -> resaltarSiguienteAccion(actual));
        btnAbrir.disableProperty().bind(seleccion.isNull());
        btnCancelar.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Pedido p = seleccion.get();
            return p == null || p.getEstadoPago() != EstadoPago.PENDIENTE
                    || p.getEstadoPreparacion() == EstadoPreparacion.ENTREGADO
                    || p.getEstadoPreparacion() == EstadoPreparacion.CANCELADO;
        }, seleccion));
        btnPreparar.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Pedido p = seleccion.get();
            return p == null || !p.isConfirmado() || p.getEstadoPreparacion() != EstadoPreparacion.PENDIENTE;
        }, seleccion));
        btnEntregar.disableProperty().bind(Bindings.createBooleanBinding(() -> seleccion.get() == null
                || seleccion.get().getEstadoPreparacion() != EstadoPreparacion.EN_PREPARACION, seleccion));
        btnCobrar.disableProperty().bind(Bindings.createBooleanBinding(() -> seleccion.get() == null
                || !seleccion.get().isConfirmado() || seleccion.get().getEstadoPago() != EstadoPago.PENDIENTE
                || seleccion.get().getEstadoPreparacion() == EstadoPreparacion.CANCELADO, seleccion));
        btnVerRecibo.disableProperty().bind(Bindings.createBooleanBinding(() -> seleccion.get() == null
                || seleccion.get().getRecibo() == null, seleccion));
        onActualizar();
    }

    private void mostrarEstado(TableCell<Pedido, String> celda, String estado, boolean vacia, String clase) {
        if (vacia || estado == null) {
            celda.setText(null);
            celda.setGraphic(null);
            return;
        }
        Label etiqueta = new Label(formatearEstado(estado));
        etiqueta.getStyleClass().addAll("stock-pill", clase);
        celda.setText(null);
        celda.setGraphic(etiqueta);
        celda.setAlignment(Pos.CENTER_LEFT);
    }

    private String formatearEstado(String estado) {
        return switch (estado) {
            case "BORRADOR" -> "Borrador";
            case "PENDIENTE" -> "Pendiente";
            case "EN_PREPARACION" -> "En preparación";
            case "ENTREGADO" -> "Entregado";
            case "CANCELADO" -> "Cancelado";
            case "PAGADO" -> "Pagado";
            default -> estado;
        };
    }

    private void resaltarSiguienteAccion(Pedido pedido) {
        Button[] acciones = {btnAbrir, btnCancelar, btnPreparar, btnEntregar, btnCobrar, btnVerRecibo};
        for (Button boton : acciones) boton.getStyleClass().remove("button-primary");
        if (pedido == null) return;

        Button siguiente = switch (pedido.getEstadoPreparacion()) {
            case CANCELADO -> pedido.getRecibo() == null ? btnAbrir : btnVerRecibo;
            case EN_PREPARACION -> btnEntregar;
            case ENTREGADO -> pedido.getEstadoPago() == EstadoPago.PENDIENTE ? btnCobrar : btnVerRecibo;
            case PENDIENTE -> pedido.isConfirmado() ? btnPreparar : btnAbrir;
        };
        if (!siguiente.isDisabled()) siguiente.getStyleClass().add("button-primary");
    }

    @FXML private void onActualizar() {
        try {
            sesion.exigirRol(Rol.CAJERO);
            lblEstado.setText("Consultando pedidos...");
            tareas.ejecutar(pedidos::listarActivos, lista -> {
                tblPedidos.getItems().setAll(lista);
                lblEstado.setText(lista.size() + " pedidos activos.");
            }, error -> lblEstado.setText(TareaPedidos.mensaje("No se pudieron cargar los pedidos.", error)));
        } catch (RuntimeException error) { lblEstado.setText(error.getMessage()); }
    }

    @FXML private void onNuevo() { navegar(() -> navegador.abrirPedido(0)); }
    @FXML private void onAbrir() { navegar(() -> navegador.abrirPedido(seleccionado().getIdPedido())); }
    @FXML private void onCobrar() { navegar(() -> navegador.abrirCobro(seleccionado().getIdPedido())); }
    @FXML private void onVerRecibo() { navegar(() -> navegador.mostrarRecibo(seleccionado().getRecibo())); }
    @FXML private void onVolver() { navegar(navegador::cerrarSesionYMostrarLogin); }
    @FXML private void onCancelar() { cambiar((id, usuario) -> pedidos.cancelar(id, usuario)); }
    @FXML private void onPreparar() { cambiar((id, usuario) -> pedidos.iniciarPreparacion(id, usuario)); }
    @FXML private void onEntregar() { cambiar((id, usuario) -> pedidos.entregar(id, usuario)); }

    private void cambiar(Cambio cambio) {
        if (tareas.estaOcupado()) return;
        try {
            sesion.exigirRol(Rol.CAJERO);
            long id = seleccionado().getIdPedido();
            String usuario = sesion.getUsuario().getNombreUsuario();
            lblEstado.setText("Procesando pedido...");
            tareas.ejecutar(() -> {
                cambio.aplicar(id, usuario);
                return pedidos.listarActivos();
            }, lista -> {
                tblPedidos.getItems().setAll(lista);
                lblEstado.setText("Pedido actualizado.");
            }, error -> {
                tblPedidos.getSelectionModel().clearSelection();
                lblEstado.setText(TareaPedidos.mensaje("No se pudo completar la operación.", error));
            });
        } catch (RuntimeException error) { lblEstado.setText(error.getMessage()); }
    }

    private Pedido seleccionado() {
        Pedido pedido = tblPedidos.getSelectionModel().getSelectedItem();
        if (pedido == null) throw new IllegalArgumentException("Seleccione un pedido.");
        return pedido;
    }
    private void navegar(Runnable accion) {
        if (tareas.estaOcupado()) return;
        try { sesion.exigirRol(Rol.CAJERO); accion.run(); }
        catch (RuntimeException error) { lblEstado.setText(error.getMessage()); }
    }
    @FunctionalInterface private interface Cambio {
        void aplicar(long id, String usuario) throws java.sql.SQLException;
    }
}
