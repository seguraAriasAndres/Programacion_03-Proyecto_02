package cr.ac.una.est.programacion03proyecto02.logic.service.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface iinventarioService {

    boolean hayStockSuficiente(
            Map<Long, Double> cantidades,
            Connection conexion
    ) throws SQLException;

    void descontar(
            Map<Long, Double> cantidades,
            Connection conexion
    ) throws SQLException;

    void restituir(
            Map<Long, Double> cantidades,
            Connection conexion
    ) throws SQLException;

    List<ingrediente> listarIngredientes() throws SQLException;

    List<ingrediente> listarBajoMinimo() throws SQLException;
}
