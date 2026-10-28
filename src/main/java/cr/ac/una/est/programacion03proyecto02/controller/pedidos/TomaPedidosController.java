package cr.ac.una.est.programacion03proyecto02.controller.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.ICatalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.pedidos.PedidoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;

import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.animation.PauseTransition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Callable;

public class TomaPedidosController {
    private final PedidoService pedidos;
    private final ICatalogoService catalogo;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;
    private TareaPedidos tareas;
    private Pedido pedido;
    private long pedidoId;
    private Long pendienteId;
    private List<Producto> productos = List.of();
    private final Map<Long, Boolean> disponibilidadBase = new HashMap<>();
    private final BooleanProperty editable = new SimpleBooleanProperty(false);
    private final BooleanProperty cobroPermitido = new SimpleBooleanProperty(false);
    private final BooleanProperty nuevo = new SimpleBooleanProperty(true);
    private final BooleanProperty incierto = new SimpleBooleanProperty(false);
    private PauseTransition temporizadorDisponibilidad;
    private boolean actualizandoControles;
    @FXML private BorderPane raiz;
    @FXML private TextField txtCliente, txtCantidad;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colProducto, colPrecioProducto, colDisponible;
    @FXML private ListView<OpcionProducto> lstOpciones;
    @FXML private TableView<lineaPedido> tblLineas;
    @FXML private TableColumn<lineaPedido, String> colLineaProducto, colExtras, colPrecio, colSubtotal;
    @FXML private TableColumn<lineaPedido, Integer> colCantidad;
    @FXML private Label lblPedido, lblTotal, lblEstado;
    @FXML private Button btnCrear, btnAgregar, btnQuitar, btnConfirmar, btnCobrar;

    public TomaPedidosController(PedidoService pedidos, ICatalogoService catalogo, SesionActual sesion, NavegadorVistas navegador){
        this.pedidos = Objects.requireNonNull(pedidos);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    // Configura las tablas, los listeners y la carga inicial del catálogo.
    @FXML private void initialize(){
        tareas = new TareaPedidos(raiz);
        temporizadorDisponibilidad = new PauseTransition(Duration.millis(350));
        temporizadorDisponibilidad.setOnFinished(evento -> comprobarDisponibilidadSeleccionada());
        colProducto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNombre()));
        colPrecioProducto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(monto(c.getValue().getPrecioBase())));
        colDisponible.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isDisponible() ? "Disponible" : "Sin stock"));
        colLineaProducto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNombreProducto()));
        colExtras.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDescripcionExtras()));
        colCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCantidad()));
        colPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(monto(c.getValue().getPrecioUnitario())));
        colSubtotal.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(monto(c.getValue().calcularSubtotal())));
        tblProductos.setPlaceholder(new Label("No hay productos para esta categoría."));
        tblLineas.setPlaceholder(new Label("El pedido no tiene productos."));
        cmbCategoria.setConverter(new StringConverter<>(){
            
            @Override public String toString(Categoria categoria){
                return categoria == null ? "Todas las categorías" : categoria.getNombre();
            }
            
            @Override public Categoria fromString(String texto){
                throw new UnsupportedOperationException();
            }
        });
        cmbCategoria.valueProperty().addListener((observable, anterior, actual) -> filtrarProductos());
        lstOpciones.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lstOpciones.setCellFactory(lista -> new ListCell<>(){
            @Override protected void updateItem(OpcionProducto opcion, boolean vacia){
                super.updateItem(opcion, vacia);
                setText(vacia || opcion == null ? null : opcion.getNombre() + " (+" + monto(opcion.getPrecioAdicional()) + ")");
            }
        });
        tblProductos.getSelectionModel().selectedItemProperty().addListener((observable, anterior, actual) -> cambiarProductoSeleccionado(actual));
        lstOpciones.getSelectionModel().getSelectedItems().addListener((ListChangeListener<OpcionProducto>) cambio -> programarComprobacionDisponibilidad());
        txtCantidad.focusedProperty().addListener((observable, teniaFoco, tieneFoco) -> {
            if (!tieneFoco){
                programarComprobacionDisponibilidad();
            }
        });
        btnCrear.disableProperty().bind(nuevo.not().or(incierto));
        btnAgregar.disableProperty().bind(editable.not().or(incierto).or(tblProductos.getSelectionModel().selectedItemProperty().isNull()));
        btnQuitar.disableProperty().bind(editable.not().or(incierto).or(tblLineas.getSelectionModel().selectedItemProperty().isNull()));
        btnConfirmar.disableProperty().bind(editable.not().or(incierto).or(javafx.beans.binding.Bindings.isEmpty(tblLineas.getItems())));
        btnCobrar.disableProperty().bind(cobroPermitido.not().or(incierto));
        txtCliente.editableProperty().bind(nuevo.and(incierto.not()));
        prepararNuevo();
        onActualizarCatalogo();
    }

    // ID cero prepara un pedido nuevo; un ID positivo carga un pedido existente.
    public void mostrarPedido(long id){
        if (id < 0){
            throw new IllegalArgumentException("El ID no puede ser negativo.");
        }
        if (tareas.estaOcupado()){
            pendienteId = id; return;
        }
        try{
            usuarioActual();
            if (id == 0){
                prepararNuevo(); return;
            }
            pedidoId = id;
            editable.set(false);
            cobroPermitido.set(false);
            lblEstado.setText("Cargando pedido...");
            tareas.ejecutar(() -> pedidos.obtener(id), this::aplicarPedido, error -> fallo("No se pudo cargar el pedido.", error));
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Recarga categorías y productos, conservando la consulta en segundo plano.
    @FXML private void onActualizarCatalogo(){
        try{
            usuarioActual();
            record Datos(List<Categoria> categorias, List<Producto> productos) {}
            tareas.ejecutar(() -> new Datos(catalogo.listarCategorias(), catalogo.listarProductosActivos()), datos -> {
                productos = List.copyOf(datos.productos());
                disponibilidadBase.clear();
                for (Producto producto : productos){
                    disponibilidadBase.put(producto.getId(), producto.isDisponible());
                }
                cmbCategoria.getItems().setAll(datos.categorias());
                cmbCategoria.getItems().add(0, null);
                filtrarProductos();
                if (pendienteId != null){
                    long id = pendienteId;
                    pendienteId = null;
                    mostrarPedido(id);
                }
            }, error -> {
                lblEstado.setText(TareaPedidos.mensaje("No se pudo cargar el catálogo.", error));
                if (pendienteId != null){
                    long id = pendienteId;
                    pendienteId = null;
                    mostrarPedido(id);
                }
            });
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Crea un borrador de pedido para permitir agregar productos.
    @FXML private void onCrear(){
        if (tareas.estaOcupado() || !nuevo.get() || incierto.get()){
            return;
        }
        try{
            String usuario = usuarioActual();
            String cliente = txtCliente.getText() == null ? "" : txtCliente.getText().trim();
            if (cliente.isBlank()){throw new IllegalArgumentException("Ingrese el nombre del cliente.");}
            tareas.ejecutar(() -> pedidos.crear(cliente, usuario), this::aplicarPedido, error -> fallo("No se pudo confirmar la creación. Revise la lista de pedidos activos.", error));
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Comprueba el pedido y agrega la línea si su stock acumulado es suficiente.
    @FXML private void onAgregarLinea(){
        if (tareas.estaOcupado() || !editable.get() || incierto.get()){
            return;
        }
        try{
            String usuario = usuarioActual();
            Producto producto = tblProductos.getSelectionModel().getSelectedItem();
            if (producto == null){
                throw new IllegalArgumentException("Seleccione un producto.");
            }
            int cantidad;
            try{
                cantidad = Integer.parseInt(txtCantidad.getText().trim());
            } catch (RuntimeException error){
                throw new IllegalArgumentException("La cantidad debe ser un entero positivo.");
            }
            if (cantidad <= 0){
                throw new IllegalArgumentException("La cantidad debe ser positiva.");
            }
            long id = pedidoId;
            List<Long> opciones = lstOpciones.getSelectionModel().getSelectedItems().stream().map(OpcionProducto::getId).toList();
            cambiar(() -> {
                pedidos.agregarLinea(id, producto.getId(), opciones, cantidad, usuario);
                return pedidos.obtener(id);
            });
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Quita la línea seleccionada del borrador.
    @FXML private void onQuitarLinea(){
        if (tareas.estaOcupado() || !editable.get() || incierto.get()){
            return;
        }
        try{
            String usuario = usuarioActual();
            lineaPedido linea = tblLineas.getSelectionModel().getSelectedItem();
            if (linea == null){
                throw new IllegalArgumentException("Seleccione una línea del pedido.");
            }
            long id = pedidoId;
            cambiar(() -> {
                pedidos.quitarLinea(id, linea.getId(), usuario);
                return pedidos.obtener(id);
            });
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Confirma el borrador; PedidoService vuelve a comprobar el stock en la transacción.
    @FXML private void onConfirmar(){
        if (tareas.estaOcupado() || !editable.get() || incierto.get()){
            return;
        }
        try{
            String usuario = usuarioActual();
            long id = pedidoId;
            cambiar(() -> {
                pedidos.confirmar(id, usuario);
                return pedidos.obtener(id);
            });
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Actualiza el pedido abierto después de una operación incierta.
    @FXML private void onActualizar(){
        if (tareas.estaOcupado()){
            return;
        }
        if (pedidoId > 0){
            mostrarPedido(pedidoId);
        } else{
            lblEstado.setText("Si falló la creación, revise los pedidos activos antes de crear otro.");
        }
    }

    // Abre la pantalla de cobro para el pedido actual.
    @FXML private void onCobrar(){
        if (tareas.estaOcupado() || !cobroPermitido.get() || incierto.get()){
            return;
        }
        try{
            usuarioActual();
            navegador.abrirCobro(pedidoId);
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Vuelve a la lista de pedidos activos.
    @FXML private void onVolver(){
        if (tareas.estaOcupado()){
            return;
        }
        try{
            usuarioActual();
            navegador.mostrarPedidosActivos();
        } catch (RuntimeException error){
            lblEstado.setText(error.getMessage());
        }
    }

    // Debounce de la consulta para evaluar la combinación después de la selección.
    private void programarComprobacionDisponibilidad(){
        if (!actualizandoControles && !tareas.estaOcupado() && temporizadorDisponibilidad != null){
            temporizadorDisponibilidad.playFromStart();
        }
    }

    // Actualiza el indicador del producto según las opciones y cantidad elegidas.
    private void comprobarDisponibilidadSeleccionada(){
        if (tareas.estaOcupado() || actualizandoControles){
            return;
        }
        Producto producto = tblProductos.getSelectionModel().getSelectedItem();
        if (producto == null){
            return;
        }

        int cantidad;
        try{
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0){
                throw new NumberFormatException();
            }
        } catch (RuntimeException error){
            restaurarDisponibilidadBase(producto);
            lblEstado.setText("La cantidad debe ser un entero positivo.");
            return;
        }

        List<Long> opciones = lstOpciones.getSelectionModel().getSelectedItems().stream().map(OpcionProducto::getId).toList();
        if (cantidad == 1 && opciones.isEmpty()){
            restaurarDisponibilidadBase(producto);
            return;
        }

        long productoId = producto.getId();
        lblEstado.setText("Comprobando existencias para esta selección...");
        tareas.ejecutar(
                () -> catalogo.hayStockSuficientePara(productoId, opciones, cantidad),
                disponible -> {
                    Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();
                    if (seleccionado != null && seleccionado.getId() == productoId){
                        seleccionado.setDisponible(disponible);
                        tblProductos.refresh();
                        lblEstado.setText(disponible ? "Hay stock para esta selección." : "No hay stock suficiente para esta selección.");
                    }
                },
                error -> {
                    restaurarDisponibilidadBase(producto);
                    lblEstado.setText(TareaPedidos.mensaje("No se pudo comprobar la disponibilidad.", error));
                }
        );
    }

    // Restaura el estado de stock de la receta base guardado al cargar el catálogo.
    private void restaurarDisponibilidadBase(Producto producto){
        producto.setDisponible(disponibilidadBase.getOrDefault(producto.getId(), producto.isDisponible()));
        tblProductos.refresh();
    }

    // Restaura el estado base y agenda la consulta al cambiar de producto.
    private void cambiarProductoSeleccionado(Producto producto){
        actualizandoControles = true;
        lstOpciones.getSelectionModel().clearSelection();
        lstOpciones.getItems().setAll(producto == null ? List.of() : producto.getOpciones().stream().filter(OpcionProducto::isActiva).toList());
        actualizandoControles = false;
        if (producto != null){
            restaurarDisponibilidadBase(producto);
            programarComprobacionDisponibilidad();
        }
    }

    // Actualiza el modelo y la vista después de cambiar un pedido.
    private void cambiar(Callable<Pedido> operacion){
        lblEstado.setText("Guardando pedido...");
        tareas.ejecutar(operacion, actualizado -> {
            aplicarPedido(actualizado);
            actualizandoControles = true;
            lstOpciones.getSelectionModel().clearSelection();
            txtCantidad.setText("1");
            actualizandoControles = false;
            programarComprobacionDisponibilidad();
        }, error -> fallo("No se pudo completar el cambio.", error));
    }

    private void aplicarPedido(Pedido actualizado){
        if (actualizado == null){
            throw new NoSuchElementException("El pedido ya no existe.");
        }
        
        pedido = actualizado;
        pedidoId = pedido.getIdPedido();
        nuevo.set(false);
        incierto.set(false);
        lblPedido.setText(Long.toString(pedidoId));
        txtCliente.setText(pedido.getNombreCliente());
        tblLineas.getItems().setAll(pedido.getLineas());
        lblTotal.setText(monto(pedido.calcularTotal()));
        editable.set(!pedido.isConfirmado() && pedido.getEstadoPreparacion() == EstadoPreparacion.PENDIENTE && pedido.getEstadoPago() == EstadoPago.PENDIENTE);
        cobroPermitido.set(pedido.isConfirmado() && pedido.getEstadoPreparacion() != EstadoPreparacion.CANCELADO && pedido.getEstadoPago() == EstadoPago.PENDIENTE);
        lblEstado.setText(editable.get() ? "Borrador guardado. Agregue productos y confirme cuando esté listo."
                          : "Pedido " + pedido.getEstadoPreparacion() + " / " + pedido.getEstadoPago() + ". La edición está cerrada.");
    }

    private void prepararNuevo(){
        pedido = null;
        pedidoId = 0;
        nuevo.set(true);
        editable.set(false);
        cobroPermitido.set(false);
        incierto.set(false);
        txtCliente.clear();
        txtCantidad.setText("1");
        tblLineas.getItems().clear();
        lblPedido.setText("Nuevo");
        lblTotal.setText("0.00");
        lblEstado.setText("Ingrese el cliente y cree el borrador para agregar productos.");
    }

    private void filtrarProductos(){
        Categoria categoria = cmbCategoria.getValue();
        tblProductos.getItems().setAll(productos.stream().filter(producto -> categoria == null || producto.getCategoria().getId() == categoria.getId()).toList());
    }

    private void fallo(String contexto, Throwable error){
        incierto.set(true);
        lblEstado.setText(TareaPedidos.mensaje(contexto, error));
    }

    private String usuarioActual(){
        sesion.exigirRol(Rol.CAJERO);
        return sesion.getUsuario().getNombreUsuario();
    }

    private String monto(BigDecimal valor){
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
