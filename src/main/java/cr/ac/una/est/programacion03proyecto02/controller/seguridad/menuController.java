package cr.ac.una.est.programacion03proyecto02.controller.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import cr.ac.una.est.programacion03proyecto02.logic.service.reportes.ReporteService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Objects;

/** Home administrativa y marco persistente de los cuatro módulos. */
public class menuController {
    private final SesionActual sesion;
    private final NavegadorVistas navegador;
    private final ReporteService reportes;
    private final iinventarioService inventario;
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CR");
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(ESPANOL);
    private boolean cargandoVentas, cargandoInventario;
    private Parent contenidoActual;
    @FXML private Label lblBienvenida, lblFechaInicio, lblVentasHoy, lblPedidosHoy, lblTicketPromedio;
    @FXML private Label lblEstadoVentas, lblEstadoInventario;
    @FXML private VBox listaAlertasInicio;
    @FXML private Button btnActualizarInicio;
    @FXML private Label lblUsuario, lblSeccion, lblEstadoNavegacion;
    @FXML private Button btnInicio, btnInventario, btnCatalogo, btnReportes, btnUsuarios, btnMenuLateral;
    @FXML private VBox barraLateral;
    @FXML private StackPane contenedorPrincipal;
    @FXML private javafx.scene.control.ScrollPane paginaInicio;
    @FXML private Region marcaInicio;

    public menuController(SesionActual sesion, NavegadorVistas navegador, ReporteService reportes, iinventarioService inventario){
        this.sesion=Objects.requireNonNull(sesion);
        this.navegador=Objects.requireNonNull(navegador);
        this.reportes=Objects.requireNonNull(reportes);
        this.inventario=Objects.requireNonNull(inventario);
    }

    @FXML private void initialize(){
        sesion.exigirRol(Rol.ADMIN);
        lblUsuario.setText(sesion.getUsuario().getNombreUsuario() + " · Administrador");
        lblEstadoNavegacion.visibleProperty().bind(lblEstadoNavegacion.textProperty().isNotEmpty());
        lblEstadoNavegacion.managedProperty().bind(lblEstadoNavegacion.visibleProperty());
        lblBienvenida.setText("Bienvenido, " + sesion.getUsuario().getNombreUsuario());
        contenidoActual=paginaInicio;
        seleccionar(btnInicio,"Inicio");
    }

    public void mostrarInicio(){
        contenedorPrincipal.getChildren().setAll(paginaInicio);
        contenidoActual=paginaInicio;
        seleccionar(btnInicio,"Inicio");
        onActualizarInicio();
    }

    /** Consulta cada sección por separado para conservar el resto si una falla. */
    @FXML private void onActualizarInicio(){
        sesion.exigirRol(Rol.ADMIN);
        if(cargandoVentas || cargandoInventario) return;
        LocalDate hoy=LocalDate.now();
        lblFechaInicio.setText(hoy.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy",ESPANOL)));
        cargandoVentas=true;
        cargandoInventario=true;
        btnActualizarInicio.setDisable(true);
        lblVentasHoy.setText("—"); lblPedidosHoy.setText("—"); lblTicketPromedio.setText("—");
        estado(lblEstadoVentas,"Consultando las ventas de hoy…",false);
        estado(lblEstadoInventario,"Consultando las existencias…",false);
        listaAlertasInicio.getChildren().clear();

        Task<VentasDia> ventas=new Task<>(){
            @Override protected VentasDia call() throws Exception {
                var pedidos=reportes.historial(hoy.atStartOfDay(),hoy.plusDays(1).atStartOfDay());
                BigDecimal total=BigDecimal.ZERO;
                for(var pedido:pedidos) total=total.add(pedido.calcularTotal());
                return new VentasDia(total,pedidos.size());
            }
        };
        ventas.setOnSucceeded(evento->{
            VentasDia datos=ventas.getValue();
            lblVentasHoy.setText(moneda.format(datos.total()));
            lblPedidosHoy.setText(Integer.toString(datos.pedidos()));
            BigDecimal promedio=datos.pedidos()==0 ? BigDecimal.ZERO
                    : datos.total().divide(BigDecimal.valueOf(datos.pedidos()),2,RoundingMode.HALF_UP);
            lblTicketPromedio.setText(moneda.format(promedio));
            estado(lblEstadoVentas,datos.pedidos()==0 ? "Todavía no hay ventas cobradas hoy."
                    : "Actualizado a las " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " · Solo ventas cobradas.",false);
            cargandoVentas=false; terminarCarga();
        });
        ventas.setOnFailed(evento->{
            estado(lblEstadoVentas,"No se pudieron consultar las ventas. Actualiza para reintentar.",true);
            cargandoVentas=false; terminarCarga();
        });
        Task<List<ingrediente>> stock=new Task<>(){
            @Override protected List<ingrediente> call() throws Exception {
                return inventario.listarBajoMinimo().stream().filter(ingrediente::isActivo)
                        .sorted(java.util.Comparator.comparing(ingrediente::getNombre)).toList();
            }
        };
        stock.setOnSucceeded(evento->{
            var alertas=stock.getValue();
            estado(lblEstadoInventario,alertas.isEmpty() ? "El inventario está al día."
                    : (alertas.size()==1 ? "1 ingrediente necesita reposición." : alertas.size() + " ingredientes necesitan reposición.")
                    + (alertas.size()>5 ? " Se muestran los primeros 5." : ""),false);
            for(ingrediente item:alertas.stream().limit(5).toList()){
                Label nombre=new Label(item.getNombre()); nombre.setWrapText(true); nombre.setMaxWidth(Double.MAX_VALUE);
                Label cantidad=new Label("Actual: " + cantidad(item.getCantidadDisponible()) + " · mínimo: "
                        + cantidad(item.getStockMinimo()) + " " + unidad(item));
                cantidad.setWrapText(true); cantidad.getStyleClass().add("muted-text");
                VBox fila=new VBox(5,nombre,cantidad); fila.getStyleClass().add("home-stock-row");
                listaAlertasInicio.getChildren().add(fila);
            }
            cargandoInventario=false; terminarCarga();
        });
        stock.setOnFailed(evento->{
            estado(lblEstadoInventario,"No se pudo consultar el inventario. Actualiza para reintentar.",true);
            cargandoInventario=false; terminarCarga();
        });
        iniciar(ventas,"inicio-ventas"); iniciar(stock,"inicio-inventario");
    }

    private void terminarCarga(){ btnActualizarInicio.setDisable(cargandoVentas || cargandoInventario); }
    private static void iniciar(Task<?> tarea,String nombre){
        Thread hilo=new Thread(tarea,nombre); hilo.setDaemon(true); hilo.start();
    }
    private static void estado(Label etiqueta,String texto,boolean error){
        etiqueta.setText(texto); etiqueta.getStyleClass().remove("status-error");
        if(error) etiqueta.getStyleClass().add("status-error");
    }
    private static String cantidad(double valor){ return BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString(); }
    private static String unidad(ingrediente item){
        return switch(item.getUnidadMedida()){
            case GRAMO -> "g";
            case MILILITRO -> "ml";
            case UNIDAD -> "unidades";
        };
    }
    private record VentasDia(BigDecimal total,int pedidos) { }

    public void mostrarModulo(Parent modulo, String ruta){
        contenedorPrincipal.getChildren().setAll(modulo);
        contenidoActual=modulo;
        if(ruta.startsWith("inventario/")) seleccionar(btnInventario,"Inventario");
        else if(ruta.startsWith("catalogo/")) seleccionar(btnCatalogo,"Catálogo de productos");
        else if(ruta.startsWith("reportes/")) seleccionar(btnReportes,"Reportes");
        else seleccionar(btnUsuarios,"Usuarios y roles");
    }

    public boolean hayOperacionEnCurso(){
        return contenidoActual!=null && contenidoActual.isDisabled();
    }

    private void seleccionar(Button actual, String titulo){
        for(Button boton:List.of(btnInicio,btnInventario,btnCatalogo,btnReportes,btnUsuarios)){
            boton.getStyleClass().remove("admin-nav-active");
            if(boton==actual) boton.getStyleClass().add("admin-nav-active");
        }
        lblSeccion.setText(titulo);
        lblEstadoNavegacion.setText("");
    }

    @FXML private void onAlternarBarra(){
        boolean mostrar=!barraLateral.isVisible();
        barraLateral.setVisible(mostrar);
        barraLateral.setManaged(mostrar);
        btnMenuLateral.setText(mostrar?"Ocultar menú":"Mostrar menú");
        btnMenuLateral.setAccessibleText(mostrar?"Contraer barra de módulos":"Desplegar barra de módulos");
    }

    private void navegar(Runnable accion){
        try { accion.run(); }
        catch(RuntimeException error){ lblEstadoNavegacion.setText(error.getMessage()); }
    }
    @FXML private void onInicio(){ navegar(navegador::mostrarMenu); }
    @FXML private void onInventario(){ navegar(navegador::mostrarInventario); }
    @FXML private void onCatalogo(){ navegar(navegador::mostrarProductos); }
    @FXML private void onReportes(){ navegar(navegador::mostrarReportes); }
    @FXML private void onUsuarios(){ navegar(navegador::mostrarUsuarios); }
    @FXML private void onCerrarSesion(){ navegar(navegador::cerrarSesionYMostrarLogin); }
}
