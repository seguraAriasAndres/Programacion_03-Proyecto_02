package cr.ac.una.est.programacion03proyecto02.dao.pedidos;

import cr.ac.una.est.programacion03proyecto02.model.pedidos.Pago;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.lineaPedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persistencia de recibos y del pago incluido en cada recibo.
 * El servicio obtiene la conexion de ConexionDB y conserva su propiedad:
 * este DAO cierra sus sentencias y resultados; el servicio gestiona la conexion.
 */
public class ReciboDAO {
    // Plantillas de texto SQL: se ejecutan cuando se usan en un PreparedStatement.
    private static final String INSERTAR = """
            INSERT INTO Recibo
                (id_Pedido, total_pago, metodo_pago, fecha_recibo, usuario_recibo)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String BUSCAR = """
            SELECT id_recibo, total_pago, metodo_pago, fecha_recibo, usuario_recibo
            FROM Recibo WHERE id_Pedido = ?
            """;
    private static final String BUSCAR_LINEAS = """
            SELECT id, nombre_producto, nombre_categoria, descripcion_opciones,
                   precio_unitario, cantidad
            FROM linea_pedido WHERE pedido_id = ? ORDER BY id
            """;

    /**
     * Devuelve el BIGINT generado por MySQL; no modifica el Recibo inmutable.
     * El servicio debe haber guardado las lineas historicas y abierto la
     * transaccion en esta misma conexion antes de cobrar.
     */
    public long insertar(Recibo recibo, Connection conexion) throws SQLException {
        Objects.requireNonNull(conexion, "La conexion no puede ser null.");
        Objects.requireNonNull(recibo, "El recibo no puede ser null.");
        Pago pago = recibo.getPago();

        if (recibo.getIdPedido() <= 0 || recibo.getLineas().isEmpty()
                || recibo.getTotal().signum() <= 0 || pago.getMonto() == null
                || pago.getMonto().compareTo(recibo.getTotal()) != 0
                || pago.getMetodo() == null || pago.getFecha() == null
                || pago.getUsuario() == null || pago.getUsuario().isBlank()
                || pago.getUsuario().length() > 100) {
            throw new IllegalArgumentException("El recibo requiere pedido, desglose y pago completo por el total exacto.");
        }
        BigDecimal total = importeSQL(recibo.getTotal());
        if (conexion.getAutoCommit()) {
            throw new SQLException("El cobro requiere una transaccion con autoCommit=false.");
        }

        try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setLong(1, recibo.getIdPedido());
            sentencia.setBigDecimal(2, total);
            sentencia.setString(3, pago.getMetodo().name());
            sentencia.setObject(4, pago.getFecha());
            sentencia.setString(5, pago.getUsuario());
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No se inserto exactamente un recibo.");
            }
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("MySQL no devolvio el id del recibo.");
                }
                long id = claves.getLong(1);
                if (claves.wasNull() || id <= 0) {
                    throw new SQLException("MySQL devolvio un id de recibo invalido.");
                }
                return id;
            }
        }
    }

    /** Devuelve null si el pedido no tiene recibo; requiere linea_pedido para el desglose. */
    public Recibo buscarPorPedido(long pedidoId, Connection conexion) throws SQLException {
        Objects.requireNonNull(conexion, "La conexion no puede ser null.");
        if (pedidoId <= 0) {
            throw new IllegalArgumentException("El id del pedido debe ser mayor que cero.");
        }
        long id;
        BigDecimal total;
        Pago pago;
        try (PreparedStatement sentencia = conexion.prepareStatement(BUSCAR)) {
            sentencia.setLong(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                id = resultado.getLong("id_recibo");
                total = resultado.getBigDecimal("total_pago");
                try {
                    pago = new Pago(MetodoPago.valueOf(resultado.getString("metodo_pago")),
                            total,
                            resultado.getObject("fecha_recibo", LocalDateTime.class),
                            resultado.getString("usuario_recibo"));
                } catch (IllegalArgumentException | NullPointerException error) {
                    throw new SQLException("El recibo contiene un metodo de pago invalido.", error);
                }
                if (resultado.next()) {
                    throw new SQLException("Hay mas de un recibo para el pedido; falta UNIQUE(id_Pedido).");
                }
            }
        }

        List<lineaPedido> lineas = cargarDesglose(pedidoId, conexion);
        Recibo recibo = new Recibo(id, pedidoId, pago, lineas);
        if (id <= 0 || total == null || total.signum() <= 0 || lineas.isEmpty()
                || recibo.getTotal().compareTo(total) != 0 || pago.getMonto() == null
                || pago.getMonto().compareTo(total) != 0 || pago.getFecha() == null
                || pago.getUsuario() == null || pago.getUsuario().isBlank()) {
            throw new SQLException("El recibo guardado no coincide con su pago y desglose historico.");
        }
        return recibo;
    }

    private List<lineaPedido> cargarDesglose(long pedidoId, Connection conexion) throws SQLException {
        List<lineaPedido> lineas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(BUSCAR_LINEAS)) {
            sentencia.setLong(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    try {
                        lineas.add(lineaPedido.reconstruirHistorica(
                                resultado.getLong("id"), resultado.getString("nombre_producto"),
                                resultado.getString("nombre_categoria"), resultado.getString("descripcion_opciones"),
                                resultado.getBigDecimal("precio_unitario"), resultado.getInt("cantidad")));
                    } catch (IllegalArgumentException | NullPointerException error) {
                        throw new SQLException("El desglose guardado contiene una linea invalida.", error);
                    }
                }
            }
        }
        return lineas;
    }

    private BigDecimal importeSQL(BigDecimal importe) {
        try {
            BigDecimal exacto = importe.setScale(2, RoundingMode.UNNECESSARY);
            if (exacto.precision() > 15) {
                throw new IllegalArgumentException("El importe excede DECIMAL(15,2).");
            }
            return exacto;
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("El importe tiene mas de dos decimales; no se redondea al guardar.", error);
        }
    }
}
