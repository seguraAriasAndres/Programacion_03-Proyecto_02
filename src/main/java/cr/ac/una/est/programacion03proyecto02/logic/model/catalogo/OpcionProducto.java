package cr.ac.una.est.programacion03proyecto02.logic.model.catalogo;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

//representa una opción que se puede agregar a un producto, un acompañamiento o un extra
public class OpcionProducto {
    private long id;
    private String nombre;
    private TipoOpcion tipo;

    //permite representar montos decimales con precisión
    private BigDecimal precioAdicional;

    //opción está disponible para seleccionarse
    private boolean activa;

    //elementos asociados a la receta de esta opción
    private List<recetaItem> receta;
    private long productoOrigenId;
    private boolean origenDisponible = true;

    public OpcionProducto() {
        this.receta = new ArrayList<>();
        this.activa = true;
    }

    public OpcionProducto(long id, String nombre, TipoOpcion tipo, BigDecimal precioAdicional, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.precioAdicional = precioAdicional;
        this.activa = activa;
        this.receta = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoOpcion getTipo() {
        return tipo;
    }

    public void setTipo(TipoOpcion tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getPrecioAdicional() {
        return precioAdicional;
    }

    public void setPrecioAdicional(BigDecimal precioAdicional) {
        this.precioAdicional = precioAdicional;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public List<recetaItem> getReceta() {
        return receta;
    }

    //si se recibe null, asigna una lista vacía para evitar errores al usarla
    public void setReceta(List<recetaItem> receta) {
        this.receta = receta != null ? new ArrayList<>(receta) : new ArrayList<>();
    }
    public long getProductoOrigenId() { return productoOrigenId; }
    public void setProductoOrigenId(long id) {
        if(id<0) throw new IllegalArgumentException("El ID del producto de origen no puede ser negativo.");
        productoOrigenId=id;
    }
    public boolean isDisponible() { return activa && origenDisponible; }
    public void setOrigenDisponible(boolean disponible) { origenDisponible=disponible; }
}
