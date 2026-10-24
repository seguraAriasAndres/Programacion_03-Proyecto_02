package cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ingredienteDAO {

    public List<ingrediente> listar(Connection conexion) throws SQLException {
        //...
        return null;
    }

    public long guardar(
            ingrediente ingrediente,
            Connection conexion
    ) throws SQLException {
        //...
        return 0L;
    }

    public boolean validar(
            Map<Long, Double> cantidades,
            Connection conexion
    ) throws SQLException {
        //...
        return false;
    }

    public void aplicarVariacion(
            long ingredienteId,
            double variacion,
            Connection conexion
    ) throws SQLException {
        //...
    }

    private void validarCantidadRequerida(double cantidad) {
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad requerida debe ser válida y mayor que cero."
            );
        }
    }
}
