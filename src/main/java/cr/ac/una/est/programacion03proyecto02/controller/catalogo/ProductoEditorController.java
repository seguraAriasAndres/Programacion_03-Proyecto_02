package cr.ac.una.est.programacion03proyecto02.controller.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.catalogoService;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.logic.service.seguridad.SesionActual;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//controlador para crear o editar productos, opciones y recetas
public class ProductoEditorController {
    private final catalogoService catalogo;
    private final iinventarioService inventario;
    private final SesionActual sesion;
    private final NavegadorVistas navegador;

    private Producto producto;
    private boolean referenciasCargadas;
    private Long productoPendiente;
    @FXML private javafx.scene.layout.BorderPane raiz;

    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecioBase;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkActivo;

    @FXML private TableView<OpcionProducto> tblOpciones;
    @FXML private TableColumn<OpcionProducto, String> colOpcionNombre;
    @FXML private TableColumn<OpcionProducto, String> colOpcionTipo;
    @FXML private TableColumn<OpcionProducto, String> colOpcionPrecio;
    @FXML private TableColumn<OpcionProducto, Boolean> colOpcionActiva;

    @FXML private TextField txtNombreOpcion;
    @FXML private ComboBox<TipoOpcion> cmbTipoOpcion;
    @FXML private TextField txtPrecioOpcion;
    @FXML private CheckBox chkOpcionActiva;
    @FXML private CheckBox chkEstadoOpcion;

    @FXML private ComboBox<ingrediente> cmbIngrediente;
    @FXML private TextField txtCantidadReceta;
    @FXML private ComboBox<String> cmbOrigenOpcion;
    @FXML private ComboBox<Producto> cmbProductoOpcion;
    @FXML private ComboBox<ingrediente> cmbIngredienteOpcion;
    @FXML private TextField txtCantidadOpcion;
    @FXML private Label lblOrigenOpcion;
    @FXML private Button btnAgregarRecetaOpcion, btnQuitarRecetaOpcion;
    @FXML private javafx.scene.layout.HBox filaProductoOpcion, filaIngredienteOpcion;

    @FXML private TableView<recetaItem> tblRecetaBase;
    @FXML private TableColumn<recetaItem, String> colBaseIngrediente;
    @FXML private TableColumn<recetaItem, Number> colBaseCantidad;

    @FXML private TableView<recetaItem> tblRecetaOpcion;
    @FXML private TableColumn<recetaItem, String> colOpcionIngrediente;
    @FXML private TableColumn<recetaItem, Number> colOpcionCantidad;

    @FXML private Label lblMensaje;

    public ProductoEditorController(catalogoService catalogo, iinventarioService inventario, SesionActual sesion, NavegadorVistas navegador) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.inventario = Objects.requireNonNull(inventario);
        this.sesion = Objects.requireNonNull(sesion);
        this.navegador = Objects.requireNonNull(navegador);
    }

    //configura las tablas y carga las categorías, ingredientes y tipos
    @FXML
    private void initialize() {
        lblMensaje.visibleProperty().bind(lblMensaje.textProperty().isNotEmpty());
        lblMensaje.managedProperty().bind(lblMensaje.visibleProperty());

        colOpcionNombre.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getNombre()));
        colOpcionTipo.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().getTipo().name()));
        colOpcionPrecio.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        celda.getValue().getPrecioAdicional().toPlainString()));
        colOpcionActiva.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue().isActiva()));

        colBaseIngrediente.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        nombreIngrediente(celda.getValue().getIngredienteId())));
        colBaseCantidad.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        celda.getValue().getCantidadRequerida()));
        colOpcionIngrediente.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        nombreIngrediente(celda.getValue().getIngredienteId())));
        colOpcionCantidad.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(
                        celda.getValue().getCantidadRequerida()));

        configurarComboCategoria();
        configurarComboIngrediente();

        cmbTipoOpcion.getItems().setAll(TipoOpcion.values());
        cmbTipoOpcion.setValue(TipoOpcion.EXTRA);
        cmbOrigenOpcion.getItems().setAll("Ingrediente","Producto del catálogo");
        cmbOrigenOpcion.setValue("Ingrediente");
        filaProductoOpcion.visibleProperty().bind(cmbOrigenOpcion.valueProperty().isEqualTo("Producto del catálogo"));
        filaProductoOpcion.managedProperty().bind(filaProductoOpcion.visibleProperty());
        filaIngredienteOpcion.visibleProperty().bind(filaProductoOpcion.visibleProperty().not());
        filaIngredienteOpcion.managedProperty().bind(filaIngredienteOpcion.visibleProperty());
        cmbProductoOpcion.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Producto p) {return p==null?"":p.getNombre();}
            @Override public Producto fromString(String texto) {throw new UnsupportedOperationException();}
        });
        cmbProductoOpcion.valueProperty().addListener((o,a,p)->{
            if(p!=null){txtNombreOpcion.setText(p.getNombre());txtPrecioOpcion.setText(p.getPrecioBase().toPlainString());}
        });
        cmbIngredienteOpcion.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ingrediente i) {return i==null?"":i.getNombre()+" ("+i.getUnidadMedida()+")";}
            @Override public ingrediente fromString(String texto) {throw new UnsupportedOperationException();}
        });
        cmbIngredienteOpcion.valueProperty().addListener((o,a,i)->{
            if(i!=null && txtNombreOpcion.getText().isBlank())txtNombreOpcion.setText(i.getNombre());
        });

        tblOpciones.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) ->
                        mostrarRecetaOpcion(actual));

        cargarCategoriasEIngredientes();
        prepararProductoNuevo();
    }

    //carga un producto existente. Si el id es cero, prepara uno nuevo
    public void mostrarProducto(long productoId) {
        if (productoId < 0) throw new IllegalArgumentException("El ID no puede ser negativo.");
        if (!referenciasCargadas) {
            productoPendiente = productoId;
            return;
        }
        if (productoId <= 0) {
            prepararProductoNuevo();
            return;
        }
        try {
            sesion.exigirRol(Rol.ADMIN);
            TareaCatalogo.ejecutar(raiz, () -> catalogo.obtenerProducto(productoId), cargado -> {
                producto = cargado;
                mostrarProductoEnFormulario();
                mostrarMensaje("Producto cargado.");
            }, error -> mostrarError("No se pudo cargar el producto.", error));
        } catch (RuntimeException error) {
            mostrarError("No se pudo cargar el producto.", error);
        }
    }

    //agrega una opción nueva a la lista del producto
    @FXML
    private void onAgregarOpcion() {
        try {
            validarProductoInicializado();

            String nombre = txtNombreOpcion.getText();
            TipoOpcion tipo = cmbTipoOpcion.getValue();

            if (nombre == null || nombre.isBlank()) {
                throw new IllegalArgumentException("Ingrese el nombre de la opción.");
            }
            if (tipo == null) {
                throw new IllegalArgumentException("Seleccione el tipo de opción.");
            }

            BigDecimal precio = leerMonto(txtPrecioOpcion.getText(), "precio adicional");

            OpcionProducto nueva=new OpcionProducto(0, nombre.trim(), tipo, precio, chkOpcionActiva.isSelected());
            if("Producto del catálogo".equals(cmbOrigenOpcion.getValue())) {
                Producto origen=cmbProductoOpcion.getValue();
                if(origen==null || origen.getReceta().isEmpty())throw new IllegalArgumentException("Seleccione un producto con receta de ingredientes.");
                nueva.setProductoOrigenId(origen.getId());
                nueva.setReceta(origen.getReceta());
            } else {
                agregarIngrediente(nueva.getReceta(),cmbIngredienteOpcion.getValue(),leerCantidad(txtCantidadOpcion.getText()));
            }
            producto.getOpciones().add(nueva);

            actualizarTablaOpciones();
            txtNombreOpcion.clear();
            txtPrecioOpcion.clear();
            cmbTipoOpcion.setValue(TipoOpcion.EXTRA);
            chkOpcionActiva.setSelected(true);
            tblOpciones.getSelectionModel().select(nueva);
            mostrarMensaje("Opción agregada.");
        } catch (RuntimeException error) {
            mostrarError("No se pudo agregar la opción.", error);
        }
    }

    //Quita la opción del catálogo al guardar; la persistencia conserva sus referencias históricas.
    @FXML
    private void onQuitarOpcion() {
        OpcionProducto opcion = tblOpciones.getSelectionModel().getSelectedItem();

        if (opcion == null) {
            mostrarError("Seleccione una opción para quitar.", null);
            return;
        }

        producto.getOpciones().remove(opcion);
        actualizarTablaOpciones();
        mostrarRecetaOpcion(null);
        mostrarMensaje("Extra eliminado de la lista. Guarde el producto para aplicar el cambio.");
    }

    // Cambia el estado del extra seleccionado; crear uno nuevo usa su casilla independiente.
    @FXML private void onCambiarEstadoOpcion() {
        OpcionProducto opcion=tblOpciones.getSelectionModel().getSelectedItem();
        if(opcion==null)return;
        opcion.setActiva(chkEstadoOpcion.isSelected());tblOpciones.refresh();
        mostrarMensaje("Estado del extra actualizado. Guarde el producto para aplicar el cambio.");
    }

    //agrega el ingrediente seleccionado a la receta base
    @FXML
    private void onAgregarIngredienteBase() {
        try {
            validarProductoInicializado();
            agregarIngrediente(producto.getReceta(), cmbIngrediente.getValue(), leerCantidad(txtCantidadReceta.getText()));

            tblRecetaBase.getItems().setAll(producto.getReceta());
            txtCantidadReceta.clear();
            mostrarMensaje("Ingrediente agregado a la receta base.");
        } catch (RuntimeException error) {
            mostrarError("No se pudo agregar el ingrediente base.", error);
        }
    }

    //quita el ingrediente seleccionado de la receta base
    @FXML
    private void onQuitarIngredienteBase() {
        recetaItem item = tblRecetaBase.getSelectionModel().getSelectedItem();

        if (item == null) {
            mostrarError("Seleccione un ingrediente de la receta base.", null);
            return;
        }

        producto.getReceta().remove(item);
        tblRecetaBase.getItems().setAll(producto.getReceta());
    }

    //agrega el ingrediente seleccionado a la receta de la opción seleccionada
    @FXML
    private void onAgregarIngredienteOpcion() {
        try {
            OpcionProducto opcion = tblOpciones.getSelectionModel().getSelectedItem();

            if (opcion == null) {
                throw new IllegalArgumentException("Seleccione primero una opción.");
            }
            if(opcion.getProductoOrigenId()>0)throw new IllegalArgumentException("Esta opción usa la receta del producto original. Modifique ese producto para cambiarla.");
            agregarIngrediente(opcion.getReceta(), cmbIngredienteOpcion.getValue(), leerCantidad(txtCantidadOpcion.getText()));

            tblRecetaOpcion.getItems().setAll(opcion.getReceta());
            txtCantidadOpcion.clear();
            mostrarMensaje("Ingrediente agregado a la receta de la opción.");
        } catch (RuntimeException error) {
            mostrarError("No se pudo agregar el ingrediente a la opción.", error);
        }
    }

    //quita el ingrediente seleccionado de la receta de la opción
    @FXML
    private void onQuitarIngredienteOpcion() {
        OpcionProducto opcion = tblOpciones.getSelectionModel().getSelectedItem();
        recetaItem item = tblRecetaOpcion.getSelectionModel().getSelectedItem();

        if (opcion == null || item == null) {
            mostrarError("Seleccione una opción y un ingrediente de su receta.", null);
            return;
        }
        if(opcion.getProductoOrigenId()>0){mostrarMensaje("La receta vinculada se modifica en su producto de origen.");return;}
        opcion.getReceta().remove(item);
        tblRecetaOpcion.getItems().setAll(opcion.getReceta());
    }

    //valida y guarda el producto junto con sus opciones y recetas
    @FXML
    private void onGuardar() {
        try {
            sesion.exigirRol(Rol.ADMIN);
            validarProductoInicializado();

            String nombre = txtNombre.getText();
            if (nombre == null || nombre.isBlank()) {
                throw new IllegalArgumentException("El nombre del producto es obligatorio.");
            }

            Categoria categoria = cmbCategoria.getValue();
            if (categoria == null) {
                throw new IllegalArgumentException("Seleccione una categoría.");
            }

            producto.setNombre(nombre.trim());
            producto.setPrecioBase(leerMonto(txtPrecioBase.getText(), "precio base"));
            producto.setCategoria(categoria);
            producto.setActivo(chkActivo.isSelected());

            Producto cambios = producto;
            TareaCatalogo.ejecutar(raiz, () -> {
                catalogo.guardarProducto(cambios);
                return null;
            }, resultado -> {
                mostrarMensaje("Producto guardado correctamente.");
                try {
                    navegador.mostrarProductos();
                } catch (RuntimeException error) {
                    mostrarError("El producto fue guardado, pero no se pudo abrir la lista.", error);
                }
            }, error -> mostrarError("No se pudo guardar el producto. Consulte su estado antes de reintentar.", error));
        } catch (RuntimeException error) {
            mostrarError("No se pudo guardar el producto.", error);
        }
    }

    //regresa a la pantalla de productos
    @FXML
    private void onVolver() {
        navegador.mostrarProductos();
    }

    //carga categorías e ingredientes desde sus servicios
    private void cargarCategoriasEIngredientes() {
        record Referencias(List<Categoria> categorias, List<ingrediente> ingredientes, List<Producto> productos) { }
        TareaCatalogo.ejecutar(raiz, () -> {
            sesion.exigirRol(Rol.ADMIN);
            return new Referencias(catalogo.listarCategoriasAdministracion(), inventario.listarIngredientes(),catalogo.listarProductosActivos());
        }, referencias -> {
            cmbCategoria.getItems().setAll(referencias.categorias());
            cmbIngrediente.getItems().setAll(referencias.ingredientes());
            cmbIngredienteOpcion.getItems().setAll(referencias.ingredientes().stream().filter(ingrediente::isActivo).toList());
            cmbProductoOpcion.getItems().setAll(referencias.productos());
            referenciasCargadas = true;
            if (productoPendiente != null) {
                long id = productoPendiente;
                productoPendiente = null;
                mostrarProducto(id);
            }
        }, error -> mostrarError("No se pudieron cargar categorías o ingredientes.", error));
    }

    //prepara el formulario para crear un producto nuevo
    private void prepararProductoNuevo() {
        producto = new Producto();

        txtNombre.clear();
        txtPrecioBase.clear();
        chkActivo.setSelected(true);
        cmbCategoria.getSelectionModel().clearSelection();

        tblOpciones.getItems().clear();
        tblRecetaBase.getItems().clear();
        tblRecetaOpcion.getItems().clear();
        mostrarRecetaOpcion(null);
    }

    //muestra los datos del producto cargado en el formulario y las tablas
    private void mostrarProductoEnFormulario() {
        txtNombre.setText(producto.getNombre());
        txtPrecioBase.setText(producto.getPrecioBase().toPlainString());
        chkActivo.setSelected(producto.isActivo());
        cmbCategoria.setValue(producto.getCategoria());

        actualizarTablaOpciones();
        tblRecetaBase.getItems().setAll(producto.getReceta());

        if (!tblOpciones.getItems().isEmpty()) {
            tblOpciones.getSelectionModel().selectFirst();
        }
    }

    //muestra la receta de la opción seleccionada
    private void mostrarRecetaOpcion(OpcionProducto opcion) {
        boolean vinculada=opcion!=null && opcion.getProductoOrigenId()>0;
        chkEstadoOpcion.setDisable(opcion==null);
        chkEstadoOpcion.setSelected(opcion!=null && opcion.isActiva());
        btnAgregarRecetaOpcion.setDisable(opcion==null || vinculada);
        btnQuitarRecetaOpcion.setDisable(opcion==null || vinculada);
        lblOrigenOpcion.setText(opcion==null?"Seleccione una opción para consultar sus ingredientes."
                :vinculada?"Receta vinculada al producto #"+opcion.getProductoOrigenId()+"; sus cambios se aplican automáticamente."
                :opcion.getReceta().isEmpty()?"Este extra no tiene ingredientes: agregue su receta antes de ofrecerlo."
                :"Receta propia del extra: se descontará del inventario al confirmar el pedido.");
        if (opcion == null) {
            tblRecetaOpcion.getItems().clear();
            return;
        }
        tblRecetaOpcion.getItems().setAll(opcion.getReceta());
    }

    //agrega o suma la cantidad de un ingrediente en una receta
    private void agregarIngrediente(List<recetaItem> receta, ingrediente ingredienteElegido, double cantidad) {
        validarProductoInicializado();

        if (ingredienteElegido == null) {
            throw new IllegalArgumentException("Seleccione un ingrediente.");
        }

        for (int i = 0; i < receta.size(); i++) {
            recetaItem existente = receta.get(i);
            if (existente.getIngredienteId() == ingredienteElegido.getId()) {
                double total = BigDecimal.valueOf(existente.getCantidadRequerida())
                        .add(BigDecimal.valueOf(cantidad)).doubleValue();
                receta.set(i, new recetaItem(existente.getIngredienteId(), total));
                return;
            }
        }

        receta.add(new recetaItem(ingredienteElegido.getId(), cantidad));
    }

    //actualiza las filas de la tabla de opciones desde el modelo
    private void actualizarTablaOpciones() {
        tblOpciones.getItems().setAll(producto.getOpciones());
    }

    //configura la apariencia de los selectores de categoría e ingrediente
    private void configurarComboCategoria() {
        cmbCategoria.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Categoria categoria) { return categoria==null?"":categoria.getNombre(); }
            @Override public Categoria fromString(String texto) { throw new UnsupportedOperationException(); }
        });
        cmbCategoria.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Categoria categoria, boolean vacia) {
                super.updateItem(categoria, vacia);
                setText(vacia || categoria == null ? null : categoria.getNombre());
            }
        });
        cmbCategoria.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Categoria categoria, boolean vacia) {
                super.updateItem(categoria, vacia);
                setText(vacia || categoria == null ? "Seleccione categoría" : categoria.getNombre());
            }
        });
    }

    private void configurarComboIngrediente() {
        cmbIngrediente.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(ingrediente item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : item.getNombre());
            }
        });
        cmbIngrediente.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ingrediente item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? "Seleccione ingrediente" : item.getNombre());
            }
        });
    }

    //devuelve el nombre del ingrediente asociado a un id para mostrarlo en tabla
    private String nombreIngrediente(long id) {
        for (ingrediente item : cmbIngrediente.getItems()) {
            if (item.getId() == id) {
                return item.getNombre();
            }
        }
        return "Ingrediente #" + id;
    }

    private void validarProductoInicializado() {
        if (producto == null) {
            throw new IllegalStateException(
                    "Primero debe cargarse o crearse un producto."
            );
        }
    }

    private BigDecimal leerMonto(String texto, String campo) {
        try {
            BigDecimal monto = new BigDecimal(texto.trim());
            if (monto.signum() < 0) {
                throw new IllegalArgumentException(campo + " no puede ser negativo.");
            }
            return monto;
        } catch (NumberFormatException | NullPointerException error) {
            throw new IllegalArgumentException("Ingrese un " + campo + " válido.", error);
        }
    }

    private double leerCantidad(String texto) {
        try {
            double cantidad = Double.parseDouble(texto.trim());
            if (!Double.isFinite(cantidad) || cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser válida y mayor que cero.");
            }
            return cantidad;
        } catch (NumberFormatException | NullPointerException error) {
            throw new IllegalArgumentException("Ingrese una cantidad válida.", error);
        }
    }

    private void mostrarMensaje(String mensaje) {
        if (lblMensaje != null) {
            lblMensaje.setText(mensaje);
        }
    }

    private void mostrarError(String mensaje, Throwable error) {
        String detalle = error == null ? mensaje : error.getMessage();
        mostrarMensaje(mensaje + (detalle == null ? "" : " " + detalle));
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error del editor de producto");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(detalle == null ? "" : detalle);
        alerta.showAndWait();
    }
}
