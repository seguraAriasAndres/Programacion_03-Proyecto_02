package cr.ac.una.est.programacion03proyecto02.logic.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.CantidadesOpciones;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class lineaPedido {
    private final long id;
    private final String nombreProducto;
    private final String nombreCategoria;
    private final String descripcion_extras;
    private final BigDecimal precio_Unitario;
    private final int cantidad;

    private final Producto producto;
    private final List<OpcionProducto> extras;
    private final long productoId;
    private final List<Long> opcionIds;
    private final Map<Long,Integer> cantidadesOpciones;


    public lineaPedido(long id, Producto producto, int cantidad) {
        this(id, producto, List.of(), cantidad);
    }

    public lineaPedido(long id, Producto producto, List<OpcionProducto> opcionesElegidas, int cantidad) {
        this(id,producto,opcionesElegidas,CantidadesOpciones.unaPorId(opcionesElegidas.stream().map(OpcionProducto::getId).toList()),cantidad);
    }

    public lineaPedido(long id, Producto producto, List<OpcionProducto> opcionesElegidas, Map<Long,Integer> cantidadesExtras, int cantidad) {
        if (id < 0 || cantidad <= 0) {
            throw new IllegalArgumentException("El id no puede ser negativo y la cantidad debe ser positiva.");
        }
        Objects.requireNonNull(producto, "Falta el producto.");
        Objects.requireNonNull(producto.getCategoria(), "Falta la categoria del producto.");
        List<OpcionProducto> seleccion = List.copyOf(opcionesElegidas);
        this.id = id;
        this.nombreProducto = Objects.requireNonNull(producto.getNombre(), "Falta el nombre del producto.");
        this.nombreCategoria = Objects.requireNonNull(producto.getCategoria().getNombre(), "Falta el nombre de la categoria.");
        Map<Long,Integer> cantidadesValidadas=CantidadesOpciones.validar(cantidadesExtras);
        if(!cantidadesValidadas.keySet().equals(new java.util.HashSet<>(seleccion.stream().map(OpcionProducto::getId).toList())))
            throw new IllegalArgumentException("Las cantidades deben corresponder exactamente a los extras elegidos.");
        BigDecimal precio=producto.calcularSubtotal(seleccion);
        for(OpcionProducto opcion:seleccion)precio=precio.add(opcion.getPrecioAdicional().multiply(BigDecimal.valueOf(cantidadesValidadas.get(opcion.getId())-1)));
        this.precio_Unitario = precio;
        this.descripcion_extras = seleccion.stream()
                .map(opcion -> Objects.requireNonNull(opcion.getNombre(), "Falta el nombre de la opcion.")
                        +(cantidadesValidadas.get(opcion.getId())==1?"":" x"+cantidadesValidadas.get(opcion.getId())))
                .collect(Collectors.joining(", "));
        this.cantidad = cantidad;

        this.extras = seleccion;
        this.producto = producto;
        this.productoId = producto.getId();
        this.opcionIds = seleccion.stream().map(OpcionProducto::getId).toList();
        this.cantidadesOpciones=cantidadesValidadas;

    }

    public static lineaPedido reconstruirHistorica(long id, String nombreProducto,
            String nombreCategoria, String descripcionExtras,
            BigDecimal precioUnitario, int cantidad) {
        return new lineaPedido(id, nombreProducto, nombreCategoria,
                descripcionExtras, precioUnitario, cantidad);
    }

    private lineaPedido(long id, String nombreProducto, String nombreCategoria,
            String descripcionExtras, BigDecimal precioUnitario, int cantidad) {
        this(id, 0, Map.of(), nombreProducto, nombreCategoria, descripcionExtras, precioUnitario, cantidad);
    }

    /** Restaura referencias y precios guardados sin cargar ni recalcular el catalogo. */
    public static lineaPedido reconstruirPersistida(long id, long productoId, List<Long> opcionIds,
            String nombreProducto, String nombreCategoria, String descripcionExtras,
            BigDecimal precioUnitario, int cantidad) {
        if (productoId <= 0) {
            throw new IllegalArgumentException("El producto persistido requiere un id positivo.");
        }
        List<Long> ids = List.copyOf(opcionIds);
        if (ids.stream().anyMatch(opcionId -> opcionId <= 0)
                || new java.util.HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Las opciones persistidas requieren ids positivos y unicos.");
        }
        return new lineaPedido(id, productoId, CantidadesOpciones.unaPorId(ids), nombreProducto, nombreCategoria,
                descripcionExtras, precioUnitario, cantidad);
    }

    public static lineaPedido reconstruirPersistida(long id,long productoId,Map<Long,Integer> cantidadesExtras,
            String nombreProducto,String nombreCategoria,String descripcionExtras,BigDecimal precioUnitario,int cantidad) {
        if(productoId<=0)throw new IllegalArgumentException("El producto persistido requiere un ID positivo.");
        return new lineaPedido(id,productoId,CantidadesOpciones.validar(cantidadesExtras),nombreProducto,nombreCategoria,descripcionExtras,precioUnitario,cantidad);
    }

    private lineaPedido(long id, long productoId, Map<Long,Integer> cantidadesExtras, String nombreProducto,
            String nombreCategoria, String descripcionExtras, BigDecimal precioUnitario, int cantidad) {
        if (id <= 0 || cantidad <= 0) {
            throw new IllegalArgumentException("El id y la cantidad deben ser mayores que cero.");
        }
        this.id = id;
        this.nombreProducto = Objects.requireNonNull(nombreProducto, "Falta el nombre historico del producto.");
        this.nombreCategoria = Objects.requireNonNull(nombreCategoria, "Falta la categoria historica.");
        this.descripcion_extras = Objects.requireNonNull(descripcionExtras, "Falta la descripcion historica de opciones.");
        this.precio_Unitario = Objects.requireNonNull(precioUnitario, "Falta el precio historico.");
        if (precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio historico no puede ser negativo.");
        }
        this.cantidad = cantidad;
        this.producto = null;
        this.extras = List.of();
        this.productoId = productoId;
        this.cantidadesOpciones=CantidadesOpciones.validar(cantidadesExtras);
        this.opcionIds = List.copyOf(cantidadesOpciones.keySet());
    }

    /** Copia la linea con su id generado sin recalcular nombres, opciones ni precios. */
    public lineaPedido conIdPersistido(long nuevoId) {
        if (nuevoId <= 0 || (id > 0 && id != nuevoId)) {
            throw new IllegalArgumentException("El id persistido debe ser positivo y no reemplazar otro id existente.");
        }
        return new lineaPedido(nuevoId, this);
    }

    private lineaPedido(long id, lineaPedido origen) {
        this.id = id;
        this.nombreProducto = origen.nombreProducto;
        this.nombreCategoria = origen.nombreCategoria;
        this.descripcion_extras = origen.descripcion_extras;
        this.precio_Unitario = origen.precio_Unitario;
        this.cantidad = origen.cantidad;
        this.producto = origen.producto;
        this.extras = origen.extras;
        this.productoId = origen.productoId;
        this.opcionIds = origen.opcionIds;
        this.cantidadesOpciones=origen.cantidadesOpciones;
    }

    public long getId() { return id; }
    public String getNombreProducto() { return nombreProducto; }
    public String getNombreCategoria() { return nombreCategoria; }
    public String getDescripcionExtras() { return descripcion_extras; }
    public BigDecimal getPrecioUnitario() { return precio_Unitario; }
    public int getCantidad() { return cantidad; }
    /** Puede ser null al recuperar desde SQL; getProductoId() conserva la referencia. */
    public Producto getProducto() { return producto; }
    /** Objetos cargados al crear la linea; al recuperar desde SQL se consultan getOpcionIds(). */
    public List<OpcionProducto> getExtras() { return extras; }
    public long getProductoId() { return productoId; }
    public List<Long> getOpcionIds() { return opcionIds; }
    public Map<Long,Integer> getCantidadesOpciones() { return cantidadesOpciones; }

    public BigDecimal calcularSubtotal() {
        return precio_Unitario.multiply(BigDecimal.valueOf(cantidad));
    }

    /** Alias conservado para los consumidores existentes. */
    public BigDecimal calcularSubTotal() { return calcularSubtotal(); }

    public String toString(){
        String linea = this.nombreProducto + "/" + this.nombreCategoria + "/" + this.descripcion_extras + "/" + this.precio_Unitario + this.cantidad;
        return linea;
    }

}
