package cr.ac.una.est.programacion03proyecto02.model.inventario;

import java.time.LocalDateTime;

public class merma extends movimientoStock {

    public merma(
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
        return -getCantidad();
    }
}
