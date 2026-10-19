package cr.ac.una.est.programacion03proyecto02.model.producto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

//representa un producto del catálogo
public class Producto {
    private int id;
    private String nombre;

    //precio base, antes de sumar opciones adicionales
    private BigDecimal precioBase;
    private Categoria categoria;
    private boolean activo;
    private List<RecetaItem> receta;
    private List<OpcionProducto> opciones;

    public Producto() {
        this.precioBase = BigDecimal.ZERO;
        this.activo = true;
        this.receta = new ArrayList<>();
        this.opciones = new ArrayList<>();
    }

    public Producto(int id, String nombre, BigDecimal precioBase, Categoria categoria, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.precioBase = precioBase != null ? precioBase : BigDecimal.ZERO;
        this.categoria = categoria;
        this.activo = activo;
        this.receta = new ArrayList<>();
        this.opciones = new ArrayList<>();
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

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase != null ? precioBase : BigDecimal.ZERO;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public boolean isActivo() {
        return activo;
    }

    //activa o desactiva el producto
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public List<RecetaItem> getReceta() {
        return receta;
    }

    //reemplaza la receta base, si recibe null, deja una lista vacía
    public void setReceta(List<RecetaItem> receta) {
        this.receta = receta != null ? receta : new ArrayList<>();
    }

    public List<OpcionProducto> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<OpcionProducto> opciones) {
        this.opciones = opciones != null ? opciones : new ArrayList<>();
    }

    //calcula el precio base más el precio adicional de cada opción activa
    //el cálculo se ajusta para sumar solo las opciones que el cliente elija
    public BigDecimal calcularSubtotal() {
        BigDecimal subtotal = precioBase != null ? precioBase : BigDecimal.ZERO;
        if (opciones == null) {
            return subtotal;
        }
        for (OpcionProducto opcion : opciones) {
            if (opcion != null && opcion.isActiva()) {
                BigDecimal precioOpcion = opcion.getPrecioAdicional();

                if (precioOpcion != null) {
                    subtotal = subtotal.add(precioOpcion);
                }
            }
        }
        return subtotal;
    }
}