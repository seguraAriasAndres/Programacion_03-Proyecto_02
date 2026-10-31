package cr.ac.una.est.programacion03proyecto02.logic.service.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.movimientoStock;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public interface iinventarioService {
    default Map<Long,Boolean> disponibilidades(Map<Long,Map<Long,Double>> recetas,Connection conexion) throws SQLException {
        Map<Long,Boolean> resultado=new java.util.HashMap<>();
        for(var entrada:recetas.entrySet())resultado.put(entrada.getKey(),hayStockSuficiente(entrada.getValue(),conexion));
        return Map.copyOf(resultado);
    }

    List<movimientoStock> listarMovimientos(
        LocalDateTime desde,
        LocalDateTime hasta
    ) throws SQLException;

    boolean hayStockSuficiente(
            Map<Long, Double> cantidades
    ) throws SQLException;

    boolean hayStockSuficiente(
            Map<Long, Double> cantidades,
            Connection conexion
    ) throws SQLException;

    void descontar(
            Map<Long, Double> cantidades,
            long pedidoId,
            String usuario,
            Connection conexion
    ) throws SQLException;

    void restituir(
            Map<Long, Double> cantidades,
            long pedidoId,
            String usuario,
            Connection conexion
    ) throws SQLException;

    List<ingrediente> listarIngredientes() throws SQLException;

    List<ingrediente> listarBajoMinimo() throws SQLException;

    void guardarIngrediente(ingrediente ingrediente) throws SQLException;

    void registrarMovimiento(movimientoStock movimiento) throws SQLException;
}
