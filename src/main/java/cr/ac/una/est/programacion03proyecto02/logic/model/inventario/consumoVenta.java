package cr.ac.una.est.programacion03proyecto02.logic.model.inventario;

import java.time.LocalDateTime;

public final class consumoVenta extends movimientoStock {

    private final long pedidoId;

    public consumoVenta(
            long id,
            long pedidoId,
            ingrediente ingrediente,
            double cantidad,
            LocalDateTime fecha,
            String usuario,
            String motivo
    ) {
        super(id, ingrediente, cantidad, fecha, usuario, motivo);

        if (pedidoId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del pedido debe ser positivo."
            );
        }

        this.pedidoId = pedidoId;
    }

    public long getPedidoId() {
        return pedidoId;
    }

    @Override
    public double variacion() {
        return -getCantidad();
    }
}
