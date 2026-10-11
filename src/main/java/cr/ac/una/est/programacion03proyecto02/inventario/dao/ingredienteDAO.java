package cr.ac.una.est.programacion03proyecto02.inventario.dao;

import cr.ac.una.est.programacion03proyecto02.inventario.model.ingrediente;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ingredienteDAO {

    public List<ingrediente> listar(Connection conexion) throws SQLException {
        //...
        return null;
    }

    public int guardar(ingrediente ingrediente, Connection conexion) throws SQLException {
        //...
        return 0;
    }

    public boolean validar(
            Map<Integer, Double> cantidades,
            Connection conexion
    ) throws SQLException {
        //...
        return false;
    }

    public void aplicarVariacion(
            int ingredienteId,
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
