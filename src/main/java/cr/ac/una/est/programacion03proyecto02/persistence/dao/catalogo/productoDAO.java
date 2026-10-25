package cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class productoDAO {

    private static final String BUSCAR_PRODUCTO = """
            SELECT p.id AS producto_id,
                   p.nombre AS producto_nombre,
                   p.precio_base,
                   p.activo AS producto_activo,
                   c.id AS categoria_id,
                   c.nombre AS categoria_nombre,
                   c.activa AS categoria_activa
            FROM producto p
            JOIN categoria c ON c.id = p.categoria_id
            WHERE p.id = ?
            """;

    public Producto buscar(long productoId, Connection conexion)
            throws SQLException {
        validarId(productoId);
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        Producto producto;

        try (PreparedStatement sentencia =
                     conexion.prepareStatement(BUSCAR_PRODUCTO)) {
            sentencia.setLong(1, productoId);

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }

                BigDecimal precioBase = resultado.getBigDecimal("precio_base");
                if (precioBase == null) {
                    throw new SQLException(
                            "El producto tiene un precio base null."
                    );
                }

                Categoria categoria = new Categoria(
                        resultado.getLong("categoria_id"),
                        resultado.getString("categoria_nombre"),
                        resultado.getBoolean("categoria_activa")
                );

                producto = new Producto(
                        resultado.getLong("producto_id"),
                        resultado.getString("producto_nombre"),
                        precioBase,
                        categoria,
                        resultado.getBoolean("producto_activo")
                );
            }
        }

        producto.setReceta(cargarRecetaProducto(productoId, conexion));
        producto.setOpciones(cargarOpcionesActivas(productoId, conexion));

        return producto;
    }

    public List<Producto> listarActivos(Connection conexion)
            throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        String sql = """
                SELECT p.id
                FROM producto p
                JOIN categoria c ON c.id = p.categoria_id
                WHERE p.activo = TRUE
                  AND c.activa = TRUE
                ORDER BY p.id
                """;

        return cargarProductos(sql, null, conexion);
    }

    public List<Producto> listarPorCategoria(
            long categoriaId,
            Connection conexion
    ) throws SQLException {
        validarId(categoriaId);
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        String sql = """
                SELECT p.id
                FROM producto p
                JOIN categoria c ON c.id = p.categoria_id
                WHERE p.activo = TRUE
                  AND c.activa = TRUE
                  AND p.categoria_id = ?
                ORDER BY p.id
                """;

        return cargarProductos(sql, categoriaId, conexion);
    }

    private List<Producto> cargarProductos(
            String sql,
            Long categoriaId,
            Connection conexion
    ) throws SQLException {
        List<Long> ids = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (categoriaId != null) {
                sentencia.setLong(1, categoriaId);
            }

            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    ids.add(resultado.getLong("id"));
                }
            }
        }

        List<Producto> productos = new ArrayList<>();

        for (long id : ids) {
            Producto producto = buscar(id, conexion);
            if (producto != null) {
                productos.add(producto);
            }
        }

        return List.copyOf(productos);
    }

    private List<OpcionProducto> cargarOpcionesActivas(
            long productoId,
            Connection conexion
    ) throws SQLException {
        String sql = """
                SELECT id, nombre, tipo_opcion, precio_adicional, activa
                FROM opcion_producto
                WHERE producto_id = ?
                  AND activa = TRUE
                ORDER BY id
                """;

        List<OpcionProducto> opciones = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, productoId);

            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    BigDecimal precio =
                            resultado.getBigDecimal("precio_adicional");

                    if (precio == null) {
                        throw new SQLException(
                                "Una opción tiene precio adicional null."
                        );
                    }

                    try {
                        opciones.add(new OpcionProducto(
                                resultado.getLong("id"),
                                resultado.getString("nombre"),
                                TipoOpcion.valueOf(
                                        resultado.getString("tipo_opcion")
                                ),
                                precio,
                                resultado.getBoolean("activa")
                        ));
                    } catch (IllegalArgumentException error) {
                        throw new SQLException(
                                "Una opción tiene un tipo desconocido.",
                                error
                        );
                    }
                }
            }
        }

        for (OpcionProducto opcion : opciones) {
            opcion.setReceta(
                    cargarRecetaOpcion(opcion.getId(), conexion)
            );
        }

        return List.copyOf(opciones);
    }

    private List<recetaItem> cargarRecetaProducto(
            long productoId,
            Connection conexion
    ) throws SQLException {
        String sql = """
                SELECT ingrediente_id, cantidad_requerida
                FROM receta_item
                WHERE producto_id = ?
                  AND opcion_producto_id IS NULL
                ORDER BY ingrediente_id
                """;

        return cargarReceta(sql, productoId, conexion);
    }

    private List<recetaItem> cargarRecetaOpcion(
            long opcionProductoId,
            Connection conexion
    ) throws SQLException {
        String sql = """
                SELECT ingrediente_id, cantidad_requerida
                FROM receta_item
                WHERE opcion_producto_id = ?
                  AND producto_id IS NULL
                ORDER BY ingrediente_id
                """;

        return cargarReceta(sql, opcionProductoId, conexion);
    }

    private List<recetaItem> cargarReceta(
            String sql,
            long padreId,
            Connection conexion
    ) throws SQLException {
        List<recetaItem> receta = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, padreId);

            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    BigDecimal cantidad =
                            resultado.getBigDecimal("cantidad_requerida");

                    if (cantidad == null) {
                        throw new SQLException(
                                "Una receta tiene cantidad requerida null."
                        );
                    }

                    try {
                        receta.add(new recetaItem(
                                resultado.getLong("ingrediente_id"),
                                cantidad.doubleValue()
                        ));
                    } catch (IllegalArgumentException error) {
                        throw new SQLException(
                                "Una receta contiene datos inválidos.",
                                error
                        );
                    }
                }
            }
        }

        return List.copyOf(receta);
    }

    private void validarId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser positivo."
            );
        }
    }

}
