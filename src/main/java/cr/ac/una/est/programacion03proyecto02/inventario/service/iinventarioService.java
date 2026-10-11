package cr.ac.una.est.programacion03proyecto02.inventario.service;

import cr.ac.una.est.programacion03proyecto02.inventario.model.ingrediente;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface iinventarioService {

    boolean hayStockSuficiente(
            Map<Integer, Double> cantidades,
            Connection conexion
    ) throws SQLException;

    void descontar(
            Map<Integer, Double> cantidades,
            Connection conexion
    ) throws SQLException;

    void restituir(
            Map<Integer, Double> cantidades,
            Connection conexion
    ) throws SQLException;

    List<ingrediente> listarIngredientes() throws SQLException;

    List<ingrediente> listarBajoMinimo() throws SQLException;
}
