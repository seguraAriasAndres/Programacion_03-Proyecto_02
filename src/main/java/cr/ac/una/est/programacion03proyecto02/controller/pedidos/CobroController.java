package cr.ac.una.est.programacion03proyecto02.controller.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.CobroService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.PedidoService;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;


import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;

import java.math.RoundingMode;
import java.util.Objects;

public class CobroController {
    private final PedidoService pedidoService;
    private final CobroService cobroService;
    private final SesionActual sesionActual;
    private final NavegadorVistas navegadorVistas;
    private Long pedidoId;

    // MostrarPedido habilitara el pedido; onCobrar marcara el proceso en curso.
    private final BooleanProperty pedidoListoParaCobrar = new SimpleBooleanProperty(false);
    private final BooleanProperty cobrando = new SimpleBooleanProperty(false);

    @FXML private Label lblPedido;
    @FXML private Label lblCliente;
    @FXML private Label lblTotal;
    @FXML private Label lblMensaje;
    @FXML private TableView<lineaPedido> tblLineas;
    @FXML private AnchorPane raiz;

    @FXML private TableColumn<lineaPedido, String> colProducto;
    @FXML private TableColumn<lineaPedido, String> colOpciones;
    @FXML private TableColumn<lineaPedido, Integer> colCantidad;
    @FXML private TableColumn<lineaPedido, String> colPrecio;
    @FXML private TableColumn<lineaPedido, String> colSubtotal;
    @FXML private ChoiceBox<MetodoPago> chMetodoPago;

    @FXML private Button btnVolver;
    @FXML private Button btnCobrar;


    public CobroController(PedidoService pedidoService, CobroService cobroService,
                           SesionActual sesionActual, NavegadorVistas navegadorVistas) {
        this.pedidoService = Objects.requireNonNull(pedidoService, "Falta el servicio de pedidos.");
        this.cobroService = Objects.requireNonNull(cobroService, "Falta el servicio de cobro.");
        this.sesionActual = Objects.requireNonNull(sesionActual, "Falta la sesión actual.");
        this.navegadorVistas = Objects.requireNonNull(navegadorVistas, "Falta el navegador.");
    }


    @FXML
    private void initialize() {
        raiz.disableProperty().bind(cobrando);
        // Configura que dato de cada lineaPedido corresponde a cada columna.
        colProducto.setCellValueFactory(celda ->
                new ReadOnlyObjectWrapper<>(celda.getValue().getNombreProducto()));
        colOpciones.setCellValueFactory(celda ->
                new ReadOnlyObjectWrapper<>(celda.getValue().getDescripcionExtras()));
        colCantidad.setCellValueFactory(celda ->
                new ReadOnlyObjectWrapper<>(celda.getValue().getCantidad()));
        colPrecio.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                celda.getValue().getPrecioUnitario()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString()));
        colSubtotal.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                celda.getValue().calcularSubtotal()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString()));
        tblLineas.setPlaceholder(new Label("No hay un pedido cargado."));

        // El selector utiliza directamente el enum que espera CobroService.
        chMetodoPago.getItems().setAll(MetodoPago.values());
        chMetodoPago.getSelectionModel().clearSelection();
        lblMensaje.setText("Seleccione un método de pago para continuar.");
        chMetodoPago.valueProperty().addListener((observable, anterior, seleccionado) ->
                lblMensaje.setText(seleccionado == null
                        ? "Seleccione un método de pago para continuar." : ""));

        // El mensaje vacio se oculta
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());

            // Debe de existir un pedido valido y no estar cobrando.
        btnCobrar.disableProperty().bind(chMetodoPago.valueProperty().isNull()
                .or(pedidoListoParaCobrar.not()).or(cobrando));
        chMetodoPago.disableProperty().bind(cobrando);
        btnVolver.disableProperty().bind(cobrando);
    }

    /** Recupera el pedido actual sin hacer SQL en el hilo de la interfaz. */
    public void mostrarPedido(long id) {
        if (cobrando.get()) return;
        try {
            sesionActual.exigirRol(Rol.CAJERO);
            if (id <= 0) throw new IllegalArgumentException("El ID debe ser positivo.");
            pedidoId = id;
            pedidoListoParaCobrar.set(false);
            Task<Pedido> tarea = new Task<>() {
                @Override protected Pedido call() throws Exception { return pedidoService.obtener(id); }
            };
            tarea.setOnSucceeded(e -> {
                cobrando.set(false);
                if (tarea.getValue() == null) lblMensaje.setText("El pedido no existe.");
                else mostrarPedido(tarea.getValue());
            });
            tarea.setOnFailed(e -> {
                cobrando.set(false);
                lblMensaje.setText(TareaPedidos.mensaje("No se pudo cargar el pedido.", tarea.getException()));
            });
            cobrando.set(true);
            lblMensaje.setText("Cargando pedido...");
            new Thread(tarea, "cargar-cobro").start();
        } catch (RuntimeException error) { lblMensaje.setText(error.getMessage()); }
    }

    @FXML private void onActualizar() {
        if (pedidoId != null) mostrarPedido(pedidoId.longValue());
    }

    public void mostrarPedido (Pedido pedido) {
        if (cobrando.get()) {
            throw new IllegalStateException("No se puede cambiar de pedido durante un cobro.");
        }
        Objects.requireNonNull(pedido, "El pedido no puede ser nulo.");
        pedidoListoParaCobrar.set(false);

        this.pedidoId = pedido.getIdPedido();

        lblPedido.setText(Long.toString(pedido.getIdPedido()));
        lblCliente.setText(pedido.getNombreCliente());
        lblTotal.setText(pedido.calcularTotal().setScale(2, RoundingMode.HALF_UP).toPlainString());

        tblLineas.setItems (FXCollections.observableArrayList(pedido.getLineas()));
        tblLineas.setPlaceholder(new Label("El pedido no tiene productos."));
        chMetodoPago.getSelectionModel().clearSelection();

        if (pedidoId > 0 && pedido.isConfirmado() && pedido.getEstadoPreparacion() != EstadoPreparacion.CANCELADO
            && pedido.getEstadoPago() == EstadoPago.PENDIENTE) {
            pedidoListoParaCobrar.set (true);
        }
        lblMensaje.setText(pedidoListoParaCobrar.get()
                ? "Seleccione un método de pago para continuar."
                : "El pedido no está disponible para cobrar.");
    }

    @FXML private void onCobrar  (){
        if (cobrando.get()) {
            return;
        }
        try { sesionActual.exigirRol(Rol.CAJERO); }
        catch (RuntimeException error) { lblMensaje.setText(error.getMessage()); return; }
        if (sesionActual.getUsuario() == null || !sesionActual.getUsuario().isActivo()) {
            lblMensaje.setText("Debe iniciar una sesión válida para cobrar.");
            return;
        }
        if (chMetodoPago.getValue() == null) {
            lblMensaje.setText("Seleccione un método de pago para continuar.");
            return;
        }
        if (pedidoId == null || pedidoId <= 0) {
            lblMensaje.setText("El pedido no tiene un ID válido.");
            return;
        }
        if (!pedidoListoParaCobrar.get()){
            lblMensaje.setText("Pedido no se puede cobrar");
            return;
        }

        long id = pedidoId;
        MetodoPago metodoPago = chMetodoPago.getValue();
        String usuario = sesionActual.getUsuario().getNombreUsuario();

        Task<Recibo> task = new Task<>() {
            @Override
            protected Recibo call() throws Exception {
                return cobroService.cobrar (id, metodoPago, usuario);
            }
        };

        task.setOnSucceeded(event -> {
            Recibo recibo = task.getValue();

            pedidoListoParaCobrar.set(false);
            cobrando.set(false);
            lblMensaje.setText("Cobro registrado correctamente.");
            // Una falla al abrir la vista no significa que el pago haya fallado.
            try {
                navegadorVistas.mostrarRecibo(recibo);
            } catch (RuntimeException error) {
                lblMensaje.setText("El cobro fue registrado, pero no se pudo abrir el recibo.");
                error.printStackTrace();
            }
        });

        task.setOnFailed(event -> {
           pedidoListoParaCobrar.set(false);
           cobrando.set(false);

           Throwable error = task.getException();
            // Una falla de red puede dejar incierto el resultado: recargar antes de reintentar.
            lblMensaje.setText("No se pudo confirmar el resultado del cobro. Vuelva a consultar el pedido antes de reintentar.");

            error.printStackTrace();
        });
        Thread hilo = new Thread(task, "cobrar-pedido");
        cobrando.set(true);
        lblMensaje.setText("Procesando cobro...");
        hilo.start();
    }

    @FXML private void onCerrar () {
        if (!cobrando.get()) {
            navegadorVistas.mostrarPedidosActivos();
        }
    }
}
