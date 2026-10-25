package cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class categoriaDAO {

    public List<Categoria> listarActivas(Connection conexion)
            throws SQLException {
        String sql = """
                SELECT id, nombre, activa
                FROM categoria
                WHERE activa = TRUE
                ORDER BY nombre, id
                """;

        return listar(sql, conexion);
    }

    public List<Categoria> listarTodas(Connection conexion)
            throws SQLException {
        String sql = """
                SELECT id, nombre, activa
                FROM categoria
                ORDER BY nombre, id
                """;

        return listar(sql, conexion);
    }

    public long guardar(Categoria categoria, Connection conexion)
            throws SQLException {
        Objects.requireNonNull(categoria, "La categoría no puede ser null.");
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        String nombre = validarNombre(categoria.getNombre());

        if (categoria.getId() == 0) {
            String sql = """
                    INSERT INTO categoria (nombre, activa)
                    VALUES (?, ?)
                    """;

            try (PreparedStatement sentencia = conexion.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                sentencia.setString(1, nombre);
                sentencia.setBoolean(2, categoria.isActiva());

                if (sentencia.executeUpdate() != 1) {
                    throw new SQLException("No se pudo crear la categoría.");
                }

                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException(
                                "MySQL no devolvió el ID de la categoría."
                        );
                    }

                    long id = claves.getLong(1);
                    if (claves.wasNull() || id <= 0) {
                        throw new SQLException(
                                "MySQL devolvió un ID de categoría inválido."
                        );
                    }

                    return id;
                }
            }
        }

        if (categoria.getId() < 0) {
            throw new IllegalArgumentException(
                    "El ID de la categoría no puede ser negativo."
            );
        }

        String sql = """
                UPDATE categoria
                SET nombre = ?, activa = ?
                WHERE id = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombre);
            sentencia.setBoolean(2, categoria.isActiva());
            sentencia.setLong(3, categoria.getId());

            int filas = sentencia.executeUpdate();

            if (filas > 1) {
                throw new SQLException(
                        "Se actualizó más de una categoría con el mismo ID."
                );
            }

            if (filas == 0 && !existe(categoria.getId(), conexion)) {
                throw new SQLException(
                        "No existe la categoría con ID " + categoria.getId() + "."
                );
            }
        }

        return categoria.getId();
    }

    private List<Categoria> listar(String sql, Connection conexion)
            throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        List<Categoria> categorias = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                categorias.add(new Categoria(
                        resultado.getLong("id"),
                        resultado.getString("nombre"),
                        resultado.getBoolean("activa")
                ));
            }
        }

        return List.copyOf(categorias);
    }

    private boolean existe(long id, Connection conexion)
            throws SQLException {
        String sql = "SELECT 1 FROM categoria WHERE id = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);

            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría es obligatorio y admite "
                            + "hasta 100 caracteres."
            );
        }

        return nombre.trim();
    }
}
