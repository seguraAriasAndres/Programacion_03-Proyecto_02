package cr.ac.una.est.programacion03proyecto02.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.model.producto.Producto;
import cr.ac.una.est.programacion03proyecto02.model.producto.OpcionProducto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class lineaPedido {
    private final long id;
    private final String nombreProducto;
    private final String nombreCategoria;
    private final String descripcion_extras;
    private final BigDecimal precio_Unitario;
    private final int cantidad;

    private final Producto producto;
    private final List<OpcionProducto> extras;


    public lineaPedido(long id, Producto producto, int cantidad) {
        this.id = id;
        this.nombreProducto = producto.getNombre();
        this.nombreCategoria = producto.getCategoria().toString();
        this.descripcion_extras = producto.getOpciones().toString();
        this.precio_Unitario = producto.getPrecioBase();
        this.cantidad = cantidad;

        this.extras = List.copyOf(producto.getOpciones());
        this.producto = producto;

    }

    public static lineaPedido reconstruirHistorica(long id, String nombreProducto,
            String nombreCategoria, String descripcionExtras,
            BigDecimal precioUnitario, int cantidad) {
        return new lineaPedido(id, nombreProducto, nombreCategoria,
                descripcionExtras, precioUnitario, cantidad);
    }

    private lineaPedido(long id, String nombreProducto, String nombreCategoria,
            String descripcionExtras, BigDecimal precioUnitario, int cantidad) {
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
    }

    public long getId() { return id; }
    public String getNombreProducto() { return nombreProducto; }
    public String getNombreCategoria() { return nombreCategoria; }
    public String getDescripcionExtras() { return descripcion_extras; }
    public BigDecimal getPrecioUnitario() { return precio_Unitario; }
    public int getCantidad() { return cantidad; }
    public Producto getProducto() { return producto; }
    public List<OpcionProducto> getExtras() { return extras; }

    public BigDecimal calcularSubTotal (){ return precio_Unitario.multiply(new BigDecimal(cantidad)); }

    public String toString(){
        String linea = this.nombreProducto + "/" + this.nombreCategoria + "/" + this.descripcion_extras + "/" + this.precio_Unitario + this.cantidad;
        return linea;
    }

}
