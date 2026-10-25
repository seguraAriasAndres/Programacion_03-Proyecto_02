package cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class categoriaDAO {

    public List<Categoria> listarActivas(Connection conexion)
            throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        String sql = """
                SELECT id, nombre, activa
                FROM categoria
                WHERE activa = TRUE
                ORDER BY nombre, id
                """;

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
}
