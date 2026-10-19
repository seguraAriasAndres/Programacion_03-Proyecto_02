package cr.ac.una.est.programacion03proyecto02.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.model.producto.Producto;
import cr.ac.una.est.programacion03proyecto02.model.producto.OpcionProducto;

import java.math.BigDecimal;
import java.util.List;

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
