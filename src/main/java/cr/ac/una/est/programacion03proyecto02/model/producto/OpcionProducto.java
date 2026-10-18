package cr.ac.una.est.programacion03proyecto02.model.producto;
import cr.ac.una.est.programacion03proyecto02.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.model.producto.enum_class.TipoOpcion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

//representa una opción que se puede agregar a un producto, un acompañamiento o un extra
public class OpcionProducto {
    private int id;
    private String nombre;
    private TipoOpcion tipo;

    //permite representar montos decimales con precisión
    private BigDecimal precioAdicional;

    //opción está disponible para seleccionarse
    private boolean activa;

    //elementos asociados a la receta de esta opción
    private List<recetaItem> receta;

    public OpcionProducto() {
        this.receta = new ArrayList<>();
        this.activa = true;
    }

    public OpcionProducto(int id, String nombre, TipoOpcion tipo, BigDecimal precioAdicional, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.precioAdicional = precioAdicional;
        this.activa = activa;
        this.receta = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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
        this.receta = receta != null ? receta : new ArrayList<>();
    }
}
