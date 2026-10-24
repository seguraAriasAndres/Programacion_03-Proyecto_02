package cr.ac.una.est.programacion03proyecto02.logic.model.reportes;

/** Fila del ranking de productos mas vendidos en un rango de fechas. */
public class ProductoRanking {
    private final long productoId;
    private final String nombreProducto;
    private final int unidades;

    public ProductoRanking(long productoId, String nombreProducto, int unidades) {
        if (productoId <= 0) {
            throw new IllegalArgumentException("El id del producto debe ser positivo.");
        }
        if (unidades <= 0) {
            throw new IllegalArgumentException("Las unidades del ranking deben ser positivas.");
        }
        this.productoId = productoId;
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("Falta el nombre del producto.");
        }
        this.nombreProducto = nombreProducto;
        this.unidades = unidades;
    }

    public long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getUnidades() {
        return unidades;
    }
}
