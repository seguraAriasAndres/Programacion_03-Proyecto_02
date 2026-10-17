package cr.ac.una.est.programacion03proyecto02.model.inventario;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class movimientoStock {

    private final long id;
    private final ingrediente ingrediente;
    private final double cantidad;
    private final LocalDateTime fecha;
    private final String usuario;
    private final String motivo;

    protected movimientoStock(
            long id,
            ingrediente ingrediente,
            double cantidad,
            LocalDateTime fecha,
            String usuario,
            String motivo
    ) {
        if (id < 0) {
            throw new IllegalArgumentException("El id no puede ser negativo.");
        }
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser válida y mayor que cero."
            );
        }

        this.id = id;
        this.ingrediente = Objects.requireNonNull(
                ingrediente,
                "El ingrediente no puede ser null."
        );
        this.cantidad = cantidad;
        this.fecha = Objects.requireNonNull(
                fecha,
                "La fecha no puede ser null."
        );
        this.usuario = validarTexto(usuario, "El usuario");
        this.motivo = validarTexto(motivo, "El motivo");
    }

    private static String validarTexto(String texto, String campo) {
        String textoValidado = Objects.requireNonNull(
                texto,
                campo + " no puede ser null."
        ).trim();

        if (textoValidado.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }

        return textoValidado;
    }

    public long getId() {
        return id;
    }

    public ingrediente getIngrediente() {
        return ingrediente;
    }

    public double getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getMotivo() {
        return motivo;
    }

    public abstract double variacion();
}
