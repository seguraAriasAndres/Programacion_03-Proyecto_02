package cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.consumoVenta;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.devolucionVenta;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.merma;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.movimientoStock;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.reabastecimiento;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.unidadMedida;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class movimientoStockDAO {

    public void insertar(
            movimientoStock movimiento,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(movimiento, "El movimiento no puede ser null.");
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        String tipoMovimiento;
        Long pedidoId = null;

        if (movimiento instanceof reabastecimiento) {
            tipoMovimiento = "REABASTECIMIENTO";
        } else if (movimiento instanceof merma) {
            tipoMovimiento = "MERMA";
        } else if (movimiento instanceof consumoVenta consumo) {
            tipoMovimiento = "CONSUMO_VENTA";
            pedidoId = consumo.getPedidoId();
        } else if (movimiento instanceof devolucionVenta devolucion) {
            tipoMovimiento = "DEVOLUCION_VENTA";
            pedidoId = devolucion.getPedidoId();
        } else {
            throw new IllegalArgumentException(
                    "Tipo de movimiento no soportado: "
                            + movimiento.getClass().getName()
            );
        }

        insertarRegistro(
                movimiento.getIngrediente().getId(),
                pedidoId,
                tipoMovimiento,
                movimiento.getCantidad(),
                movimiento.getFecha(),
                movimiento.getUsuario(),
                movimiento.getMotivo(),
                conexion
        );
    }

    /**
     * Registra el movimiento de inventario asociado a un pedido.
     * El stock se modifica por ingredienteDAO; este método solo registra
     * la auditoría y no confirma la transacción.
     */
    public void insertarMovimientoPedido(
            long ingredienteId,
            double cantidad,
            String tipoMovimiento,
            long pedidoId,
            String usuario,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        if (pedidoId <= 0 || ingredienteId <= 0) {
            throw new IllegalArgumentException(
                    "Los IDs del pedido y del ingrediente deben ser positivos."
            );
        }

        String tipoValidado = validarTipoPedido(tipoMovimiento);
        String usuarioValidado = validarTexto(usuario, "El usuario");
        BigDecimal cantidadValidada = convertirCantidad(cantidad);

        String motivo = switch (tipoValidado) {
            case "CONSUMO_VENTA" ->
                    "Consumo de ingredientes asociado al pedido.";
            case "DEVOLUCION_VENTA" ->
                    "Devolución de ingredientes asociada al pedido.";
            default -> throw new IllegalStateException(
                    "El tipo de movimiento no está soportado."
            );
        };

        String sql = """
                INSERT INTO movimiento_stock
                    (ingrediente_id, pedido_id, tipo_movimiento,
                     cantidad, usuario, motivo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, ingredienteId);
            sentencia.setLong(2, pedidoId);
            sentencia.setString(3, tipoValidado);
            sentencia.setBigDecimal(4, cantidadValidada);
            sentencia.setString(5, usuarioValidado);
            sentencia.setString(6, motivo);

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo guardar el movimiento del pedido."
                );
            }
        }
    }

    /**
     * Lista movimientos desde la fecha inicial incluida hasta la fecha final
     * excluida.
     */
    public List<movimientoStock> listar(
            LocalDateTime desde,
            LocalDateTime hasta,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        if (desde == null || hasta == null || !desde.isBefore(hasta)) {
            throw new IllegalArgumentException(
                    "El rango requiere desde < hasta."
            );
        }

        String sql = """
                SELECT
                    m.id AS movimiento_id,
                    m.pedido_id,
                    m.tipo_movimiento,
                    m.cantidad AS movimiento_cantidad,
                    m.fecha AS movimiento_fecha,
                    m.usuario AS movimiento_usuario,
                    m.motivo AS movimiento_motivo,
                    i.id AS ingrediente_id,
                    i.nombre AS ingrediente_nombre,
                    i.cantidad_disponible,
                    i.unidad_medida,
                    i.stock_minimo,
                    i.activo AS ingrediente_activo
                FROM movimiento_stock m
                JOIN ingrediente i ON i.id = m.ingrediente_id
                WHERE m.fecha >= ?
                  AND m.fecha < ?
                ORDER BY m.fecha, m.id
                """;

        List<movimientoStock> movimientos = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setObject(1, desde);
            sentencia.setObject(2, hasta);

            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    movimientos.add(mapearMovimiento(resultado));
                }
            }
        }

        return List.copyOf(movimientos);
    }

    private void insertarRegistro(
            long ingredienteId,
            Long pedidoId,
            String tipoMovimiento,
            double cantidad,
            LocalDateTime fecha,
            String usuario,
            String motivo,
            Connection conexion
    ) throws SQLException {
        if (ingredienteId <= 0) {
            throw new IllegalArgumentException(
                    "El ingrediente debe tener un ID positivo."
            );
        }

        Objects.requireNonNull(fecha, "La fecha no puede ser null.");

        BigDecimal cantidadValidada = convertirCantidad(cantidad);
        String usuarioValidado = validarTexto(usuario, "El usuario");
        String motivoValidado = validarTexto(motivo, "El motivo");

        String sql = """
                INSERT INTO movimiento_stock
                    (ingrediente_id, pedido_id, tipo_movimiento, cantidad,
                     fecha, usuario, motivo)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, ingredienteId);

            if (pedidoId == null) {
                sentencia.setNull(2, Types.BIGINT);
            } else {
                sentencia.setLong(2, pedidoId);
            }

            sentencia.setString(3, tipoMovimiento);
            sentencia.setBigDecimal(4, cantidadValidada);
            sentencia.setObject(5, fecha);
            sentencia.setString(6, usuarioValidado);
            sentencia.setString(7, motivoValidado);

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No se pudo guardar el movimiento.");
            }
        }
    }

    private movimientoStock mapearMovimiento(ResultSet resultado)
            throws SQLException {
        try {
            long movimientoId = resultado.getLong("movimiento_id");
            long ingredienteId = resultado.getLong("ingrediente_id");
            Long pedidoId = resultado.getObject("pedido_id", Long.class);

            ingrediente ingrediente = new ingrediente(
                    ingredienteId,
                    resultado.getString("ingrediente_nombre"),
                    resultado.getBigDecimal("cantidad_disponible").doubleValue(),
                    unidadMedida.valueOf(resultado.getString("unidad_medida")),
                    resultado.getBigDecimal("stock_minimo").doubleValue(),
                    resultado.getBoolean("ingrediente_activo")
            );

            String tipo = resultado.getString("tipo_movimiento");
            double cantidad = resultado
                    .getBigDecimal("movimiento_cantidad")
                    .doubleValue();
            LocalDateTime fecha = resultado.getObject(
                    "movimiento_fecha",
                    LocalDateTime.class
            );
            String usuario = resultado.getString("movimiento_usuario");
            String motivo = resultado.getString("movimiento_motivo");

            return switch (tipo) {
                case "REABASTECIMIENTO" -> new reabastecimiento(
                        movimientoId, ingrediente, cantidad, fecha, usuario, motivo
                );
                case "MERMA" -> new merma(
                        movimientoId, ingrediente, cantidad, fecha, usuario, motivo
                );
                case "CONSUMO_VENTA" -> new consumoVenta(
                        movimientoId,
                        exigirPedidoId(pedidoId),
                        ingrediente,
                        cantidad,
                        fecha,
                        usuario,
                        motivo
                );
                case "DEVOLUCION_VENTA" -> new devolucionVenta(
                        movimientoId,
                        exigirPedidoId(pedidoId),
                        ingrediente,
                        cantidad,
                        fecha,
                        usuario,
                        motivo
                );
                default -> throw new SQLException(
                        "Tipo de movimiento desconocido en la base de datos: "
                                + tipo
                );
            };
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new SQLException(
                    "No se pudo convertir un movimiento de inventario.",
                    error
            );
        }
    }

    private long exigirPedidoId(Long pedidoId) throws SQLException {
        if (pedidoId == null || pedidoId <= 0) {
            throw new SQLException(
                    "Un movimiento de venta debe estar asociado a un pedido."
            );
        }

        return pedidoId;
    }

    private String validarTipoPedido(String tipoMovimiento) {
        if ("CONSUMO_VENTA".equals(tipoMovimiento)
                || "DEVOLUCION_VENTA".equals(tipoMovimiento)) {
            return tipoMovimiento;
        }

        throw new IllegalArgumentException(
                "El tipo debe ser CONSUMO_VENTA o DEVOLUCION_VENTA."
        );
    }

    private String validarTexto(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    campo + " no puede estar vacío."
            );
        }

        return texto.trim();
    }

    private BigDecimal convertirCantidad(double cantidad) {
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser finita y mayor que cero."
            );
        }

        try {
            BigDecimal decimal = BigDecimal.valueOf(cantidad)
                    .setScale(6, RoundingMode.UNNECESSARY);

            if (decimal.precision() > 15) {
                throw new IllegalArgumentException(
                        "La cantidad excede el rango DECIMAL(15,6)."
                );
            }

            return decimal;
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException(
                    "La cantidad admite como máximo seis decimales.",
                    error
            );
        }
    }
}
