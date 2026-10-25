package cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//DAO para buscar, listar, guardar y desactivar productos
public class ProductoDAO {

    //proveedor de conexiones a la base de datos
    private final ConexionDB bd;

    //inicializa el proveedor de conexiones utilizado por este DAO
    public ProductoDAO() {
        this.bd = new ConexionDB();
    }

    //busca un producto por id y carga su categoría, receta y opciones.
    //no cierra la conexión recibida, quien la proporciona debe cerrarla
    public Producto buscar(int id, Connection c) {
        String sql = """
                SELECT p.id,
                       p.nombre,
                       p.precio_base,
                       p.activo,
                       ca.id AS categoria_id,
                       ca.nombre AS categoria_nombre,
                       ca.activa AS categoria_activa
                FROM producto p
                JOIN categoria ca ON ca.id = p.categoria_id
                WHERE p.id = ?
                """;
        try (PreparedStatement consulta = c.prepareStatement(sql)) {
            consulta.setInt(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                Categoria categoria = new Categoria(resultado.getLong("categoria_id"), resultado.getString("categoria_nombre"), resultado.getBoolean("categoria_activa"));
                Producto producto = new Producto(resultado.getLong("id"), resultado.getString("nombre"), resultado.getBigDecimal("precio_base"), categoria, resultado.getBoolean("activo"));
                cargarRecetaProducto(producto, c);
                cargarOpciones(producto, c);
                return producto;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo buscar el producto.", error);
        }
    }

    //lista los productos activos de una categoría y carga sus detalles
    public List<Producto> listarActivos(int categoriaId) {
        String sql = """
                SELECT id
                FROM producto
                WHERE activo = TRUE
                  AND categoria_id = ?
                ORDER BY nombre
                """;
        List<Producto> productos = new ArrayList<>();
        try (Connection conexion = bd.getConnection();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, categoriaId);
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    Producto producto = buscar(resultado.getInt("id"), conexion);
                    if (producto != null) {
                        productos.add(producto);
                    }
                }
            }
            return productos;
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudieron listar los productos activos.", error);
        }
    }

    //guarda el producto, su receta base, sus opciones y las recetas de estas
    //ejecuta las operaciones en una transacción para evitar guardar datos incompletos
    public void guardarConRecetas(Producto producto) {
        try (Connection conexion = bd.getConnection()) {
            boolean autoCommitOriginal = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            try {
                guardarProducto(producto, conexion);
                guardarReceta(producto.getId(), producto.getReceta(), "producto_receta", "producto_id", conexion);
                if (producto.getOpciones() != null) {
                    for (OpcionProducto opcion : producto.getOpciones()) {
                        guardarOpcion(producto.getId(), opcion, conexion);
                    }
                }
                conexion.commit();
                conexion.setAutoCommit(autoCommitOriginal);
            } catch (SQLException | RuntimeException error) {
                conexion.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo guardar el producto y sus recetas.", error);
        }
    }

    //marca como inactivo el producto indicado, sin eliminarlo
    public void desactivar(int id) {
        String sql = "UPDATE producto SET activo = FALSE WHERE id = ?";
        try (Connection conexion = bd.getConnection();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);
            consulta.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo desactivar el producto.", error);
        }
    }

    //inserta un producto nuevo o actualiza uno existente
    //si se inserta, guarda en el objeto el id generado por MySQL
    private void guardarProducto(Producto producto, Connection conexion)
            throws SQLException {
        if (producto.getId() > 0) {
            String sql = """
                    UPDATE producto
                    SET nombre = ?,
                        precio_base = ?,
                        categoria_id = ?,
                        activo = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setString(1, producto.getNombre());
                consulta.setBigDecimal(2, producto.getPrecioBase());
                consulta.setLong(3, producto.getCategoria().getId());
                consulta.setBoolean(4, producto.isActivo());
                consulta.setLong(5, producto.getId());
                consulta.executeUpdate();
            }
        } else {
            String sql = """
                    INSERT INTO producto
                        (nombre, precio_base, categoria_id, activo)
                    VALUES (?, ?, ?, ?)
                    """;
            try (PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                consulta.setString(1, producto.getNombre());
                consulta.setBigDecimal(2, producto.getPrecioBase());
                consulta.setLong(3, producto.getCategoria().getId());
                consulta.setBoolean(4, producto.isActivo());
                consulta.executeUpdate();
                try (ResultSet claves = consulta.getGeneratedKeys()) {
                    if (claves.next()) {
                        producto.setId(claves.getLong(1));
                    }
                }
            }
        }
    }

    //carga desde la base de datos la receta base del producto
    private void cargarRecetaProducto(Producto producto, Connection conexion) throws SQLException {
        String sql = """
                SELECT ingrediente_id, cantidad_requerida
                FROM producto_receta
                WHERE producto_id = ?
                """;
        List<recetaItem> receta = new ArrayList<>();
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, producto.getId());
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    receta.add(new recetaItem(resultado.getLong("ingrediente_id"), resultado.getDouble("cantidad_requerida")));
                }
            }
        }
        producto.setReceta(receta);
    }

    //carga las opciones del producto y la receta de cada opción
    private void cargarOpciones(Producto producto, Connection conexion) throws SQLException {
        String sql = """
                SELECT id, nombre, tipo, precio_adicional, activa
                FROM opcion_producto
                WHERE producto_id = ?
                """;
        List<OpcionProducto> opciones = new ArrayList<>();
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, producto.getId());
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    OpcionProducto opcion = new OpcionProducto(
                            resultado.getLong("id"),
                            resultado.getString("nombre"),
                            TipoOpcion.valueOf(resultado.getString("tipo")),
                            resultado.getBigDecimal("precio_adicional"),
                            resultado.getBoolean("activa")
                    );
                    cargarRecetaOpcion(opcion, conexion);
                    opciones.add(opcion);
                }
            }
        }
        producto.setOpciones(opciones);
    }

    //carga los ingredientes de la receta de una opción
    private void cargarRecetaOpcion(OpcionProducto opcion, Connection conexion) throws SQLException {
        String sql = """
                SELECT ingrediente_id, cantidad_requerida
                FROM opcion_receta
                WHERE opcion_id = ?
                """;
        List<recetaItem> receta = new ArrayList<>();
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, opcion.getId());
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    receta.add(new recetaItem(resultado.getLong("ingrediente_id"), resultado.getDouble("cantidad_requerida")));
                }
            }
        }
        opcion.setReceta(receta);
    }

    //reemplaza las filas de receta del producto u opción indicado
    private void guardarReceta(
            long propietarioId,
            List<recetaItem> receta,
            String tabla,
            String columnaPropietario,
            Connection conexion
    ) throws SQLException {
        String borrarSql = "DELETE FROM " + tabla + " WHERE " + columnaPropietario + " = ?";
        String insertarSql = """
                INSERT INTO %s (%s, ingrediente_id, cantidad_requerida)
                VALUES (?, ?, ?)
                """.formatted(tabla, columnaPropietario);
        try (PreparedStatement borrar = conexion.prepareStatement(borrarSql)) {
            borrar.setLong(1, propietarioId);
            borrar.executeUpdate();
        }
        if (receta == null || receta.isEmpty()) {
            return;
        }
        try (PreparedStatement insertar = conexion.prepareStatement(insertarSql)) {
            for (recetaItem item : receta) {
                insertar.setLong(1, propietarioId);
                insertar.setLong(2, item.getIngredienteId());
                insertar.setDouble(3, item.getCantidadRequerida());
                insertar.addBatch();
            }
            insertar.executeBatch();
        }
    }

    //inserta o actualiza una opción y guarda su receta asociada
    private void guardarOpcion(long productoId, OpcionProducto opcion, Connection conexion) throws SQLException {
        if (opcion.getId() > 0) {
            String sql = """
                    UPDATE opcion_producto
                    SET nombre = ?,
                        tipo = ?,
                        precio_adicional = ?,
                        activa = ?
                    WHERE id = ? AND producto_id = ?
                    """;
            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setString(1, opcion.getNombre());
                consulta.setString(2, opcion.getTipo().name());
                consulta.setBigDecimal(3, opcion.getPrecioAdicional());
                consulta.setBoolean(4, opcion.isActiva());
                consulta.setLong(5, opcion.getId());
                consulta.setLong(6, productoId);
                consulta.executeUpdate();
            }
        } else {
            String sql = """
                    INSERT INTO opcion_producto
                        (producto_id, nombre, tipo, precio_adicional, activa)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                consulta.setLong(1, productoId);
                consulta.setString(2, opcion.getNombre());
                consulta.setString(3, opcion.getTipo().name());
                consulta.setBigDecimal(4, opcion.getPrecioAdicional());
                consulta.setBoolean(5, opcion.isActiva());
                consulta.executeUpdate();
                try (ResultSet claves = consulta.getGeneratedKeys()) {
                    if (claves.next()) {
                        opcion.setId(claves.getLong(1));
                    }
                }
            }
        }
        guardarReceta(opcion.getId(), opcion.getReceta(), "opcion_receta", "opcion_id", conexion);
    }
}