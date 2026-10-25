package cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.unidadMedida;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ingredienteDAO {

    private static final String COLUMNAS_INGREDIENTE = """
            id, nombre, cantidad_disponible, unidad_medida,
            stock_minimo, activo
            """;

    public List<ingrediente> listar(Connection conexion) throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        String sql = """
                SELECT id, nombre, cantidad_disponible, unidad_medida,
                       stock_minimo, activo
                FROM ingrediente
                ORDER BY id
                """;

        List<ingrediente> ingredientes = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                ingredientes.add(mapearIngrediente(resultado));
            }
        }

        return List.copyOf(ingredientes);
    }

     //Crea un ingrediente si su ID es cero; actualiza sus datos descriptivos
     //si ya tiene ID. El stock existente solo cambia mediante movimientos.

    public long guardar(
            ingrediente ingrediente,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(ingrediente, "El ingrediente no puede ser null.");
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        BigDecimal stockMinimo = convertirDecimalNoNegativo(
                ingrediente.getStockMinimo(),
                "El stock mínimo",
                true
        );

        if (ingrediente.getId() == 0) {
            BigDecimal stockInicial = convertirDecimalNoNegativo(
                    ingrediente.getCantidadDisponible(),
                    "La cantidad disponible",
                    true
            );

            if (stockInicial.signum() != 0) {
                throw new IllegalArgumentException(
                        "Un ingrediente nuevo debe iniciar con stock cero."
                );
            }

            String sql = """
                    INSERT INTO ingrediente
                        (nombre, cantidad_disponible, unidad_medida,
                         stock_minimo, activo)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement sentencia = conexion.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                sentencia.setString(1, ingrediente.getNombre());
                sentencia.setBigDecimal(2, BigDecimal.ZERO.setScale(6));
                sentencia.setString(3, ingrediente.getUnidadMedida().name());
                sentencia.setBigDecimal(4, stockMinimo);
                sentencia.setBoolean(5, ingrediente.isActivo());

                if (sentencia.executeUpdate() != 1) {
                    throw new SQLException("No se pudo crear el ingrediente.");
                }

                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException(
                                "MySQL no devolvió el ID del ingrediente creado."
                        );
                    }

                    long idGenerado = claves.getLong(1);
                    if (idGenerado <= 0) {
                        throw new SQLException(
                                "MySQL devolvió un ID inválido para el ingrediente."
                        );
                    }

                    return idGenerado;
                }
            }
        }

        long id = ingrediente.getId();

        String sql = """
                UPDATE ingrediente
                SET nombre = ?,
                    unidad_medida = ?,
                    stock_minimo = ?,
                    activo = ?
                WHERE id = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, ingrediente.getNombre());
            sentencia.setString(2, ingrediente.getUnidadMedida().name());
            sentencia.setBigDecimal(3, stockMinimo);
            sentencia.setBoolean(4, ingrediente.isActivo());
            sentencia.setLong(5, id);

            int filasActualizadas = sentencia.executeUpdate();

            if (filasActualizadas > 1) {
                throw new SQLException(
                        "Se actualizó más de un ingrediente con el mismo ID."
                );
            }

            if (filasActualizadas == 0 && !existeIngrediente(id, conexion)) {
                throw new SQLException(
                        "No existe el ingrediente con ID " + id + "."
                );
            }
        }

        return id;
    }

    public boolean validar(
            Map<Long, Double> cantidades,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(cantidades, "El mapa no puede ser null.");
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        Map<Long, BigDecimal> cantidadesValidadas = new TreeMap<>();

        for (Map.Entry<Long, Double> entrada : cantidades.entrySet()) {
            Long ingredienteId = entrada.getKey();
            Double cantidad = entrada.getValue();

            if (ingredienteId == null || ingredienteId <= 0) {
                throw new IllegalArgumentException(
                        "Cada ingrediente debe tener un ID positivo."
                );
            }

            if (cantidad == null) {
                throw new IllegalArgumentException(
                        "La cantidad requerida no puede ser null."
                );
            }

            cantidadesValidadas.put(
                    ingredienteId,
                    convertirDecimalNoNegativo(
                            cantidad,
                            "La cantidad requerida",
                            false
                    )
            );
        }

        String sql = """
                SELECT cantidad_disponible, activo
                FROM ingrediente
                WHERE id = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (Map.Entry<Long, BigDecimal> entrada
                    : cantidadesValidadas.entrySet()) {

                sentencia.setLong(1, entrada.getKey());

                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) {
                        return false;
                    }

                    BigDecimal disponible =
                            resultado.getBigDecimal("cantidad_disponible");
                    boolean activo = resultado.getBoolean("activo");

                    if (!activo
                            || disponible == null
                            || disponible.compareTo(entrada.getValue()) < 0) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

     //Aplica una variación de forma atómica. Una reducción solo se aplica si
     //el ingrediente está activo y todavía tiene suficiente cantidad.

    public void aplicarVariacion(
            long ingredienteId,
            double variacion,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        if (ingredienteId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del ingrediente debe ser positivo."
            );
        }

        BigDecimal delta = convertirDecimalFirmado(
                variacion,
                "La variación"
        );

        if (delta.signum() == 0) {
            throw new IllegalArgumentException(
                    "La variación debe ser distinta de cero."
            );
        }

        String sql = """
                UPDATE ingrediente
                SET cantidad_disponible = cantidad_disponible + ?
                WHERE id = ?
                  AND cantidad_disponible + ? >= 0
                  AND (? >= 0 OR activo = TRUE)
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBigDecimal(1, delta);
            sentencia.setLong(2, ingredienteId);
            sentencia.setBigDecimal(3, delta);
            sentencia.setBigDecimal(4, delta);

            if (sentencia.executeUpdate() != 1) {
                throw new IllegalStateException(
                        "No se pudo aplicar la variación: el ingrediente no "
                                + "existe, está inactivo o no tiene stock suficiente."
                );
            }
        }
    }

    private ingrediente mapearIngrediente(ResultSet resultado)
            throws SQLException {
        try {
            long id = resultado.getLong("id");
            String nombre = resultado.getString("nombre");
            BigDecimal cantidad = resultado.getBigDecimal(
                    "cantidad_disponible"
            );
            String unidad = resultado.getString("unidad_medida");
            BigDecimal stockMinimo = resultado.getBigDecimal("stock_minimo");
            boolean activo = resultado.getBoolean("activo");

            if (resultado.wasNull()
                    || cantidad == null
                    || stockMinimo == null) {
                throw new SQLException(
                        "La fila del ingrediente contiene valores null inválidos."
                );
            }

            return new ingrediente(
                    id,
                    nombre,
                    cantidad.doubleValue(),
                    unidadMedida.valueOf(unidad),
                    stockMinimo.doubleValue(),
                    activo
            );
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new SQLException(
                    "No se pudo convertir una fila de ingrediente.",
                    error
            );
        }
    }

    private boolean existeIngrediente(long id, Connection conexion)
            throws SQLException {
        String sql = "SELECT 1 FROM ingrediente WHERE id = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);

            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private BigDecimal convertirDecimalNoNegativo(
            double valor,
            String campo,
            boolean permitirCero
    ) {
        if (!Double.isFinite(valor)
                || valor < 0
                || (!permitirCero && valor == 0)) {
            throw new IllegalArgumentException(
                    campo + " debe ser válido y "
                            + (permitirCero ? "no negativo." : "mayor que cero.")
            );
        }

        return convertirDecimalFirmado(valor, campo);
    }

    private BigDecimal convertirDecimalFirmado(double valor, String campo) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException(
                    campo + " debe ser un número finito."
            );
        }

        try {
            BigDecimal decimal = BigDecimal.valueOf(valor)
                    .setScale(6, RoundingMode.UNNECESSARY);

            if (decimal.precision() > 15) {
                throw new IllegalArgumentException(
                        campo + " excede el rango DECIMAL(15,6)."
                );
            }

            return decimal;
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException(
                    campo + " admite como máximo seis decimales.",
                    error
            );
        }
    }
}
