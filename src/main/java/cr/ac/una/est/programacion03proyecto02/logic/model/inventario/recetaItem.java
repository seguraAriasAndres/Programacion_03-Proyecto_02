package cr.ac.una.est.programacion03proyecto02.logic.model.inventario;

public final class recetaItem {

    private final long ingredienteId;
    private final double cantidadRequerida;

    public recetaItem(long ingredienteId, double cantidadRequerida) {
        if (ingredienteId < 0) {
            throw new IllegalArgumentException(
                    "El id del ingrediente no puede ser negativo."
            );
        }

        if (!Double.isFinite(cantidadRequerida) || cantidadRequerida <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad requerida debe ser válida y mayor que cero."
            );
        }

        this.ingredienteId = ingredienteId;
        this.cantidadRequerida = cantidadRequerida;
    }

    public long getIngredienteId() {
        return ingredienteId;
    }

    public double getCantidadRequerida() {
        return cantidadRequerida;
    }
}
