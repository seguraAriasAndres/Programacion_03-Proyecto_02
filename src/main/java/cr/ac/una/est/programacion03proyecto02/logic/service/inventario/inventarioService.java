package cr.ac.una.est.programacion03proyecto02.logic.service.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.movimientoStock;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.ingredienteDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.movimientoStockDAO;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.merma;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.reabastecimiento;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.time.LocalDateTime;

public class inventarioService implements iinventarioService {

    private final ConexionDB conexionDB;
    private final ingredienteDAO ingredientes;
    private final movimientoStockDAO movimientos;

    public inventarioService(
            ConexionDB conexionDB,
            ingredienteDAO ingredientes,
            movimientoStockDAO movimientos
    ) {
        this.conexionDB = Objects.requireNonNull(
                conexionDB,
                "La conexión a la base de datos no puede ser null."
        );
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
    public List<movimientoStock> listarMovimientos(
            LocalDateTime desde,
            LocalDateTime hasta
    ) throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            return List.copyOf(movimientos.listar(desde, hasta, conexion));
        }
    }

    @Override
    public boolean hayStockSuficiente(
            Map<Long, Double> cantidades
    ) throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            return hayStockSuficiente(cantidades, conexion);
        }
    }

    @Override
    public boolean hayStockSuficiente(
            Map<Long, Double> cantidades,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(
                conexion,
                "La conexión no puede ser null."
        );

        Map<Long, Double> cantidadesValidadas =
                validarCantidades(cantidades, true);

        return ingredientes.validar(cantidadesValidadas, conexion);
    }

    @Override
    public void descontar(
            Map<Long, Double> cantidades,
            long pedidoId,
            String usuario,
            Connection conexion
    ) throws SQLException {
        validarDatosPedido(pedidoId, usuario, conexion);

        Map<Long, Double> cantidadesValidadas =
                validarCantidades(cantidades, true);

        if (!ingredientes.validar(cantidadesValidadas, conexion)) {
            throw new IllegalStateException(
                    "No hay suficiente stock para completar el pedido."
            );
        }

        for (Map.Entry<Long, Double> entrada : cantidadesValidadas.entrySet()) {
            long ingredienteId = entrada.getKey();
            double cantidad = entrada.getValue();

            ingredientes.aplicarVariacion(
                    ingredienteId,
                    -cantidad,
                    conexion
            );

            movimientos.insertarMovimientoPedido(
                    ingredienteId,
                    cantidad,
                    "CONSUMO_VENTA",
                    pedidoId,
                    usuario.trim(),
                    conexion
            );
        }
    }

    @Override
    public void restituir(
            Map<Long, Double> cantidades,
            long pedidoId,
            String usuario,
            Connection conexion
    ) throws SQLException {
        validarDatosPedido(pedidoId, usuario, conexion);

        Map<Long, Double> cantidadesValidadas =
                validarCantidades(cantidades, true);

        for (Map.Entry<Long, Double> entrada : cantidadesValidadas.entrySet()) {
            long ingredienteId = entrada.getKey();
            double cantidad = entrada.getValue();

            ingredientes.aplicarVariacion(
                    ingredienteId,
                    cantidad,
                    conexion
            );

            movimientos.insertarMovimientoPedido(
                    ingredienteId,
                    cantidad,
                    "DEVOLUCION_VENTA",
                    pedidoId,
                    usuario.trim(),
                    conexion
            );
        }
    }

    @Override
    public List<ingrediente> listarIngredientes() throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            return List.copyOf(ingredientes.listar(conexion));
        }
    }

    @Override
    public List<ingrediente> listarBajoMinimo() throws SQLException {
        return listarIngredientes().stream()
                .filter(ingrediente::isActivo)
                .filter(ingrediente::estaBajoMinimo)
                .toList();
    }

    @Override
    public void guardarIngrediente(
            ingrediente ingrediente
    ) throws SQLException {
        Objects.requireNonNull(
                ingrediente,
                "El ingrediente no puede ser null."
        );

        if (ingrediente.getId() == 0
                && ingrediente.getCantidadDisponible() != 0) {
            throw new IllegalArgumentException(
                    "Crea el ingrediente con stock cero y registra la cantidad "
                            + "inicial como un movimiento de reabastecimiento."
            );
        }

        ejecutarTransaccion(conexion -> {
            ingredientes.guardar(ingrediente, conexion);
            return null;
        });
    }

    @Override
    public void registrarMovimiento(
            movimientoStock movimiento
    ) throws SQLException {
        Objects.requireNonNull(
                movimiento,
                "El movimiento no puede ser null."
        );
        
        if (!(movimiento instanceof reabastecimiento)
                && !(movimiento instanceof merma)) {
            throw new IllegalArgumentException(
                    "Los consumos y devoluciones deben registrarse "
                            + "mediante las operaciones del pedido."
            );
        }
        
        long ingredienteId = movimiento.getIngrediente().getId();

        if (ingredienteId <= 0) {
            throw new IllegalArgumentException(
                    "El ingrediente debe estar guardado antes de registrar "
                            + "un movimiento."
            );
        }

        ejecutarTransaccion(conexion -> {
            ingredientes.aplicarVariacion(
                    ingredienteId,
                    movimiento.variacion(),
                    conexion
            );
            movimientos.insertar(movimiento, conexion);
            return null;
        });
    }

    private Map<Long, Double> validarCantidades(
            Map<Long, Double> cantidades,
            boolean permitirVacio
    ) {
        Objects.requireNonNull(
                cantidades,
                "El mapa de cantidades no puede ser null."
        );

        if (!permitirVacio && cantidades.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe haber al menos un ingrediente."
            );
        }

        Map<Long, Double> resultado = new TreeMap<>();

        for (Map.Entry<Long, Double> entrada : cantidades.entrySet()) {
            Long ingredienteId = entrada.getKey();
            Double cantidad = entrada.getValue();

            if (ingredienteId == null || ingredienteId <= 0) {
                throw new IllegalArgumentException(
                        "Cada ingrediente debe tener un ID guardado válido."
                );
            }

            if (cantidad == null
                    || !Double.isFinite(cantidad)
                    || cantidad <= 0) {
                throw new IllegalArgumentException(
                        "Cada cantidad debe ser válida y mayor que cero."
                );
            }

            resultado.put(ingredienteId, cantidad);
        }

        return resultado;
    }

    private void validarDatosPedido(
        long pedidoId,
        String usuario,
        Connection conexion
    ) throws SQLException {
        if (pedidoId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del pedido debe ser positivo."
            );
        }
    
        validarUsuario(usuario);
    
        Objects.requireNonNull(
                conexion,
                "La conexión no puede ser null."
        );
    
        if (conexion.getAutoCommit()) {
            throw new SQLException(
                    "La operación del pedido requiere una transacción abierta."
            );
        }
    }

    private void validarUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "El usuario no puede estar vacío."
            );
        }
    }

    private <T> T ejecutarTransaccion(
            operacionConConexion<T> operacion
    ) throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            conexion.setAutoCommit(false);

            try {
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    error.addSuppressed(errorRollback);
                }

                throw error;
            }
        }
    }

    @FunctionalInterface
    private interface operacionConConexion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }
}
