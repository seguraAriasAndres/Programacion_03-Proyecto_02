package cr.ac.una.est.programacion03proyecto02.inventario.model;

import java.time.LocalDateTime;

public class reabastecimiento extends movimientoStock {

    public reabastecimiento(
            long id,
            ingrediente ingrediente,
            double cantidad,
            LocalDateTime fecha,
            String usuario,
            String motivo
    ) {
        super(id, ingrediente, cantidad, fecha, usuario, motivo);
    }

    @Override
    public double variacion() {
        return getCantidad();
    }
}
