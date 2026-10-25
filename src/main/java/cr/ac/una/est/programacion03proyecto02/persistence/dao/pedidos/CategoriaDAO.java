package cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//DAO para consultar y guardar categorías en la base de datos
public class CategoriaDAO {
    private final ConexionDB bd;

    public CategoriaDAO() {
        this.bd = new ConexionDB();
    }

    //devuelve las categorías activas ordenadas por nombre
    public List<Categoria> listarActivas() {
        String sql = """
                SELECT id, nombre, activa
                FROM categoria
                WHERE activa = TRUE
                ORDER BY nombre
                """;

        List<Categoria> categorias = new ArrayList<>();

        try (Connection conexion = bd.getConnection();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                categorias.add(new Categoria(resultado.getLong("id"), resultado.getString("nombre"), resultado.getBoolean("activa")));
            }
            return categorias;
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudieron listar las categorías activas.", error);
        }
    }

    //inserta una categoría nueva o actualiza una existente
    public void guardar(Categoria categoria) {
        String insertar = """
                INSERT INTO categoria (nombre, activa)
                VALUES (?, ?)
                """;
        String actualizar = """
                UPDATE categoria
                SET nombre = ?, activa = ?
                WHERE id = ?
                """;
        try (Connection conexion = bd.getConnection()) {
            if (categoria.getId() > 0) {
                try (PreparedStatement consulta = conexion.prepareStatement(actualizar)) {
                    consulta.setString(1, categoria.getNombre());
                    consulta.setBoolean(2, categoria.isActiva());
                    consulta.setLong(3, categoria.getId());

                    if (consulta.executeUpdate() == 0) {
                        throw new IllegalArgumentException("No existe la categoría con id " + categoria.getId());
                    }
                }
            } else {
                try (PreparedStatement consulta = conexion.prepareStatement(insertar, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    consulta.setString(1, categoria.getNombre());
                    consulta.setBoolean(2, categoria.isActiva());
                    consulta.executeUpdate();
                    try (ResultSet claves = consulta.getGeneratedKeys()) {
                        if (claves.next()) {
                            categoria.setId(claves.getLong(1));
                        }
                    }
                }
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo guardar la categoría.", error);
        }
    }
}