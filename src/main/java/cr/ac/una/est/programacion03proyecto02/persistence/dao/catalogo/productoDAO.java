package cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

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
        producto.setOpciones(new ArrayList<>(cargarOpciones(productoId, conexion)));

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

    private List<OpcionProducto> cargarOpciones(
            long productoId,
            Connection conexion
    ) throws SQLException {
        String sql = """
                SELECT id, nombre, tipo_opcion, precio_adicional, activa
                FROM opcion_producto
                WHERE producto_id = ?
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

        return receta;
    }

    /** Guarda el agregado con la transacción del servicio; devuelve los IDs sin mutar el original. */
    public Producto guardarConRecetas(Producto producto, Connection conexion) throws SQLException {
        Objects.requireNonNull(producto, "Falta el producto.");
        Objects.requireNonNull(conexion, "Falta la conexión.");
        if (conexion.getAutoCommit()) {
            throw new SQLException("Guardar recetas requiere una transacción abierta.");
        }
        if (producto.getId() < 0 || producto.getCategoria() == null || producto.getCategoria().getId() <= 0) {
            throw new IllegalArgumentException("El producto requiere ID no negativo y categoría guardada.");
        }
        String nombre = validarNombre(producto.getNombre());
        BigDecimal precio = validarPrecio(producto.getPrecioBase());
        validarReceta(producto.getReceta());
        Set<Long> ids = new HashSet<>();
        for (OpcionProducto opcion : producto.getOpciones()) {
            if (opcion.getId() < 0 || opcion.getId() > 0 && !ids.add(opcion.getId())) {
                throw new IllegalArgumentException("Las opciones guardadas no pueden repetirse.");
            }
            validarNombre(opcion.getNombre());
            validarPrecio(opcion.getPrecioAdicional());
            Objects.requireNonNull(opcion.getTipo(), "Falta el tipo de opción.");
            validarReceta(opcion.getReceta());
        }
        long id = producto.getId();
        if (id > 0) {
            exigirProducto(id, conexion);
            try (PreparedStatement s = conexion.prepareStatement(
                    "UPDATE producto SET nombre=?, precio_base=?, categoria_id=?, activo=? WHERE id=?")) {
                s.setString(1, nombre); s.setBigDecimal(2, precio);
                s.setLong(3, producto.getCategoria().getId()); s.setBoolean(4, producto.isActivo());
                s.setLong(5, id); s.executeUpdate();
            }
        } else {
            try (PreparedStatement s = conexion.prepareStatement(
                    "INSERT INTO producto(nombre,precio_base,categoria_id,activo) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, nombre); s.setBigDecimal(2, precio);
                s.setLong(3, producto.getCategoria().getId()); s.setBoolean(4, producto.isActivo());
                if (s.executeUpdate() != 1) throw new SQLException("No se pudo insertar el producto.");
                id = leerId(s);
            }
        }
        guardarReceta(id, false, producto.getReceta(), conexion);
        // Las opciones omitidas se desactivan: no se borran sus IDs históricos.
        try (PreparedStatement s = conexion.prepareStatement("UPDATE opcion_producto SET activa=FALSE WHERE producto_id=?")) {
            s.setLong(1, id); s.executeUpdate();
        }
        List<OpcionProducto> opcionesGuardadas = new ArrayList<>();
        for (OpcionProducto opcion : producto.getOpciones()) {
            long opcionId = opcion.getId();
            if (opcionId > 0) {
                try (PreparedStatement s = conexion.prepareStatement("SELECT producto_id FROM opcion_producto WHERE id=? FOR UPDATE")) {
                    s.setLong(1, opcionId);
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next() || r.getLong(1) != id) throw new SQLException("La opción no pertenece al producto.");
                    }
                }
                try (PreparedStatement s = conexion.prepareStatement(
                        "UPDATE opcion_producto SET nombre=?,tipo_opcion=?,precio_adicional=?,activa=? WHERE id=? AND producto_id=?")) {
                    s.setString(1, validarNombre(opcion.getNombre())); s.setString(2, opcion.getTipo().name());
                    s.setBigDecimal(3, validarPrecio(opcion.getPrecioAdicional())); s.setBoolean(4, opcion.isActiva());
                    s.setLong(5, opcionId); s.setLong(6, id); s.executeUpdate();
                }
            } else {
                try (PreparedStatement s = conexion.prepareStatement(
                        "INSERT INTO opcion_producto(producto_id,nombre,tipo_opcion,precio_adicional,activa) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    s.setLong(1, id); s.setString(2, validarNombre(opcion.getNombre())); s.setString(3, opcion.getTipo().name());
                    s.setBigDecimal(4, validarPrecio(opcion.getPrecioAdicional())); s.setBoolean(5, opcion.isActiva());
                    if (s.executeUpdate() != 1) throw new SQLException("No se pudo insertar la opción.");
                    opcionId = leerId(s);
                }
            }
            guardarReceta(opcionId, true, opcion.getReceta(), conexion);
            OpcionProducto guardada = new OpcionProducto(opcionId, opcion.getNombre().trim(), opcion.getTipo(),
                    opcion.getPrecioAdicional(), opcion.isActiva());
            guardada.setReceta(new ArrayList<>(opcion.getReceta()));
            opcionesGuardadas.add(guardada);
        }
        Producto guardado = new Producto(id, nombre, precio, producto.getCategoria(), producto.isActivo());
        guardado.setReceta(new ArrayList<>(producto.getReceta()));
        guardado.setOpciones(opcionesGuardadas);
        return guardado;
    }

    public void desactivar(long id, Connection conexion) throws SQLException {
        validarId(id);
        if (conexion.getAutoCommit()) throw new SQLException("Desactivar requiere una transacción abierta.");
        exigirProducto(id, conexion);
        try (PreparedStatement s = conexion.prepareStatement("UPDATE producto SET activo=FALSE WHERE id=?")) {
            s.setLong(1, id); s.executeUpdate();
        }
    }

    private void exigirProducto(long id, Connection conexion) throws SQLException {
        try (PreparedStatement s = conexion.prepareStatement("SELECT id FROM producto WHERE id=? FOR UPDATE")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("No existe el producto con ID " + id);
            }
        }
    }

    private long leerId(PreparedStatement sentencia) throws SQLException {
        try (ResultSet claves = sentencia.getGeneratedKeys()) {
            if (!claves.next()) throw new SQLException("MySQL no devolvió el ID generado.");
            long id = claves.getLong(1);
            if (claves.wasNull() || id <= 0) throw new SQLException("ID generado inválido.");
            return id;
        }
    }

    private void guardarReceta(long id, boolean opcion, List<recetaItem> receta, Connection conexion) throws SQLException {
        // Los nombres de columna son constantes internas, nunca entradas del usuario.
        String columna = opcion ? "opcion_producto_id" : "producto_id";
        try (PreparedStatement s = conexion.prepareStatement("DELETE FROM receta_item WHERE " + columna + "=?")) {
            s.setLong(1, id); s.executeUpdate();
        }
        try (PreparedStatement s = conexion.prepareStatement(
                "INSERT INTO receta_item(" + columna + ",ingrediente_id,cantidad_requerida) VALUES (?,?,?)")) {
            for (recetaItem item : receta) {
                s.setLong(1, id); s.setLong(2, item.getIngredienteId());
                s.setBigDecimal(3, BigDecimal.valueOf(item.getCantidadRequerida()).setScale(6, RoundingMode.UNNECESSARY));
                s.addBatch();
            }
            if (!receta.isEmpty()) s.executeBatch();
        }
    }

    private void validarReceta(List<recetaItem> receta) {
        Set<Long> ids = new HashSet<>();
        for (recetaItem item : List.copyOf(receta)) {
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidadRequerida()).setScale(6, RoundingMode.UNNECESSARY);
            if (item.getIngredienteId() <= 0 || cantidad.signum() <= 0 || cantidad.precision() > 15 || !ids.add(item.getIngredienteId())) {
                throw new IllegalArgumentException("La receta requiere ingredientes únicos y cantidades DECIMAL(15,6) positivas.");
            }
        }
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 150) {
            throw new IllegalArgumentException("El nombre es obligatorio y admite hasta 150 caracteres.");
        }
        return nombre.trim();
    }

    private BigDecimal validarPrecio(BigDecimal precio) {
        Objects.requireNonNull(precio, "Falta el precio.");
        BigDecimal valor = precio.setScale(2, RoundingMode.UNNECESSARY);
        if (valor.signum() < 0 || valor.precision() > 15) throw new IllegalArgumentException("Precio fuera de DECIMAL(15,2).");
        return valor;
    }

    private void validarId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser positivo."
            );
        }
    }

}
