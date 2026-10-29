package cr.ac.una.est.programacion03proyecto02.controller.reportes;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.reportes.ProductoRanking;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.reportes.ReporteService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public class reportesController{

    private final ReporteService reportes;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    @FXML private Pane raiz;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblIngresos;
    @FXML private Label lblCategorias;
    @FXML private Label lblMensaje;
    @FXML private TableView<ProductoRanking> tblRanking;
    @FXML private TableColumn<ProductoRanking, String> colRankingProducto;
    @FXML private TableColumn<ProductoRanking, Number> colRankingUnidades;
    @FXML private TableView<Pedido> tblHistorial;
    @FXML private TableColumn<Pedido, Number> colHistPedido;
    @FXML private TableColumn<Pedido, String> colHistCliente;
    @FXML private TableColumn<Pedido, String> colHistFecha;
    @FXML private TableColumn<Pedido, String> colHistPreparacion;
    @FXML private TableColumn<Pedido, String> colHistPago;
    @FXML private TableColumn<Pedido, String> colHistTotal;

    public reportesController(ReporteService reportes, SesionActual sesion, NavegadorVistas navegador){
        this.reportes = Objects.requireNonNull(reportes);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    // Comprueba el rol administrativo y establece el rango inicial.
    @FXML
    private void initialize(){
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());

        sesion.exigirRol(Rol.ADMIN);

        colRankingProducto.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getNombreProducto()));
        colRankingUnidades.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getUnidades()));

        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colHistPedido.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getIdPedido()));
        colHistCliente.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getNombreCliente()));
        colHistFecha.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getFechaPedido().format(formatoFecha)));
        colHistPreparacion.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getEstadoPreparacion().name()));
        colHistPago.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getEstadoPago().name()));
        colHistTotal.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                celda.getValue().calcularTotal().setScale(2, RoundingMode.HALF_UP).toPlainString()));

        tblRanking.setPlaceholder(new Label("Consulte el rango para ver el ranking."));
        tblHistorial.setPlaceholder(new Label("Consulte el rango para ver el historial."));

        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
    }

    // Consulta los reportes financieros para el rango elegido.
    @FXML
    private void onConsultar(){
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null || desde.isAfter(hasta)){
            lblMensaje.setText("Indique un rango de fechas válido.");
            return;
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();

        Task<datosReporte> tarea = new Task<>(){
            @Override
            protected datosReporte call() throws Exception{
                return new datosReporte(reportes.ingresosBrutos(inicio, finExclusivo), reportes.porCategoria(inicio, finExclusivo),
                                        reportes.ranking(inicio, finExclusivo), reportes.historial(inicio, finExclusivo));
            }
        };

        raiz.setDisable(true);
        tarea.setOnSucceeded(evento -> {
            raiz.setDisable(false);
            datosReporte datos = tarea.getValue();

            lblIngresos.setText(datos.ingresos().toPlainString());
            lblCategorias.setText(formatearCategorias(datos.categorias()));
            tblRanking.setItems(FXCollections.observableArrayList(datos.ranking()));
            tblHistorial.setItems(FXCollections.observableArrayList(datos.historial()));
            lblMensaje.setText("Reporte actualizado.");
        });
        tarea.setOnFailed(evento -> {
            raiz.setDisable(false);
            Throwable error = tarea.getException();
            lblMensaje.setText(error == null ? "No se pudo consultar el reporte." : "No se pudo consultar el reporte: " + error.getMessage());
        });

        new Thread(tarea, "consultar-reportes").start();
    }

    // Regresa al menú.
    @FXML
    private void onVolver(){
        navegador.mostrarMenu();
    }

    // Convierte los totales por categoría en texto para mostrarlos en la vista.
    private String formatearCategorias(Map<String, java.math.BigDecimal> categorias){
        if (categorias.isEmpty()) {
            return "Sin ventas en este período.";
        }

        StringBuilder texto = new StringBuilder();
        categorias.forEach((categoria, total) -> {
            if (!texto.isEmpty()){
                texto.append("\n");
            }
            texto.append(categoria).append(": ").append(total.toPlainString());
        });
        return texto.toString();
    }

    private record datosReporte(java.math.BigDecimal ingresos, Map<String, java.math.BigDecimal> categorias, java.util.List<ProductoRanking> ranking, java.util.List<Pedido> historial){
    }
}
