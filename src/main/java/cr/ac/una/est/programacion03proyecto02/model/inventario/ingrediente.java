package cr.ac.una.est.programacion03proyecto02.model.inventario;

import java.util.Objects;

public class ingrediente {

    private final int id;
    private final String nombre;
    private final double cantidadDisponible;
    private final unidadMedida unidadMedida;
    private final double stockMinimo;
    private final boolean activo;

    public ingrediente(
            int id,
            String nombre,
            double cantidadDisponible,
            unidadMedida unidadMedida,
            double stockMinimo,
            boolean activo
    ) {
        if (id < 0) {
            throw new IllegalArgumentException("El id no puede ser negativo.");
        }

        String nombreValidado = Objects.requireNonNull(
                nombre,
                "El nombre no puede ser null."
        ).trim();

        if (nombreValidado.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }

        validarCantidadNoNegativa(cantidadDisponible, "La cantidad disponible");
        validarCantidadNoNegativa(stockMinimo, "El stock mínimo");

        this.id = id;
        this.nombre = nombreValidado;
        this.cantidadDisponible = cantidadDisponible;
        this.unidadMedida = Objects.requireNonNull(
                unidadMedida,
                "La unidad de medida no puede ser null."
        );
        this.stockMinimo = stockMinimo;
        this.activo = activo;
    }

    private static void validarCantidadNoNegativa(double cantidad, String campo) {
        if (!Double.isFinite(cantidad) || cantidad < 0) {
            throw new IllegalArgumentException(
                    campo + " debe ser un número válido y no negativo."
            );
        }
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getCantidadDisponible() {
        return cantidadDisponible;
    }

    public unidadMedida getUnidadMedida() {
        return unidadMedida;
    }

    public double getStockMinimo() {
        return stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean estaBajoMinimo() {
        return cantidadDisponible <= stockMinimo;
    }
}
