package cr.ac.una.est.programacion03proyecto02.inventario.service;

import cr.ac.una.est.programacion03proyecto02.inventario.dao.ingredienteDAO;
import cr.ac.una.est.programacion03proyecto02.inventario.dao.movimientoStockDAO;
import cr.ac.una.est.programacion03proyecto02.inventario.model.ingrediente;
import cr.ac.una.est.programacion03proyecto02.inventario.model.movimientoStock;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class inventarioService implements iinventarioService {

    private final ingredienteDAO ingredientes;
    private final movimientoStockDAO movimientos;

    public inventarioService(
            ingredienteDAO ingredientes,
            movimientoStockDAO movimientos
    ) {
        this.ingredientes = Objects.requireNonNull(
                ingredientes,
                "El DAO de ingredientes no puede ser null."
        );
        this.movimientos = Objects.requireNonNull(
                movimientos,
                "El DAO de movimientos no puede ser null."
        );
    }

    @Override
    public boolean hayStockSuficiente(
            Map<Integer, Double> cantidades,
            Connection conexion
    ) throws SQLException {
        //...
        return false;
    }

    @Override
    public void descontar(
            Map<Integer, Double> cantidades,
            Connection conexion
    ) throws SQLException {
        //...
    }

    @Override
    public void restituir(
            Map<Integer, Double> cantidades,
            Connection conexion
    ) throws SQLException {
        //...
    }

    @Override
    public List<ingrediente> listarIngredientes() throws SQLException {
        //...
        return null;
    }

    @Override
    public List<ingrediente> listarBajoMinimo() throws SQLException {
        //...
        return null;
    }

    public void guardarIngrediente(
            ingrediente ingrediente,
            Connection conexion
    ) throws SQLException {
        //...
    }

    public void registrarMovimiento(
            movimientoStock movimiento,
            String usuario,
            Connection conexion
    ) throws SQLException {
        //...
    }
}
