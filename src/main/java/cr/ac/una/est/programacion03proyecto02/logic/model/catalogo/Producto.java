package cr.ac.una.est.programacion03proyecto02.logic.model.catalogo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
//representa un producto del catálogo
public class Producto {
    private long id;
    private String nombre;

    //precio base, antes de sumar opciones adicionales
    private BigDecimal precioBase;
    private Categoria categoria;
    private boolean activo;
    private List<recetaItem> receta;
    private List<OpcionProducto> opciones;
    private boolean disponible;

    public Producto() {
        this.precioBase = BigDecimal.ZERO;
        this.activo = true;
        this.receta = new ArrayList<>();
        this.opciones = new ArrayList<>();
    }

    public Producto(long id, String nombre, BigDecimal precioBase, Categoria categoria, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.precioBase = precioBase != null ? precioBase : BigDecimal.ZERO;
        this.categoria = categoria;
        this.activo = activo;
        this.receta = new ArrayList<>();
        this.opciones = new ArrayList<>();
    }
    
    public boolean isDisponible() {
        return disponible;
    }
    
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
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

    public List<recetaItem> getReceta() {
        return receta;
    }

    //reemplaza la receta base, si recibe null, deja una lista vacía
    public void setReceta(List<recetaItem> receta) {
        this.receta = receta != null ? receta : new ArrayList<>();
    }

    public List<OpcionProducto> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<OpcionProducto> opciones) {
        this.opciones = opciones != null ? opciones : new ArrayList<>();
    }

    /** Precio sin opciones elegidas por el cliente. */
    public BigDecimal calcularSubtotal() {
        return calcularSubtotal(List.of());
    }

    /** Precio por unidad con las opciones elegidas, no con todo el catalogo. */
    public BigDecimal calcularSubtotal(List<OpcionProducto> opcionesElegidas) {
        BigDecimal subtotal = Objects.requireNonNull(precioBase, "Falta el precio base.");
        if (subtotal.signum() < 0) {
            throw new IllegalArgumentException("El precio base no puede ser negativo.");
        }
        var idsElegidos = new HashSet<Long>();
        for (OpcionProducto opcion : List.copyOf(opcionesElegidas)) {
            if (!opciones.contains(opcion) || !opcion.isActiva()
                    || !idsElegidos.add(opcion.getId())) {
                throw new IllegalArgumentException("Cada opcion debe pertenecer al producto, estar activa y no repetirse.");
            }
            BigDecimal precio = Objects.requireNonNull(opcion.getPrecioAdicional(), "Falta el precio de la opcion.");
            if (precio.signum() < 0) {
                throw new IllegalArgumentException("El precio de una opcion no puede ser negativo.");
            }
            subtotal = subtotal.add(precio);
        }
        return subtotal;
    }
}
