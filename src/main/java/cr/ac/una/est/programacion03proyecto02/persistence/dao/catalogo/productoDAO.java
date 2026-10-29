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

    private static final String SELECCION = """
            SELECT p.id AS producto_id, p.nombre AS producto_nombre, p.precio_base,
                   p.activo AS producto_activo, c.id AS categoria_id,
                   c.nombre AS categoria_nombre, c.activa AS categoria_activa
            FROM producto p JOIN categoria c ON c.id=p.categoria_id
            """;

    public Producto buscar(long productoId, Connection conexion) throws SQLException {
        validarId(productoId);
        List<Producto> resultado=cargarProductos(SELECCION+" WHERE p.id=?",productoId,conexion);
        return resultado.isEmpty()?null:resultado.getFirst();
    }
    public List<Producto> listarActivos(Connection conexion) throws SQLException {
        return cargarProductos(SELECCION+" WHERE p.activo=TRUE AND c.activa=TRUE ORDER BY p.id",null,conexion);
    }
    public List<Producto> listarPorCategoria(long categoriaId, Connection conexion) throws SQLException {
        validarId(categoriaId);
        return cargarProductos(SELECCION+" WHERE p.activo=TRUE AND c.activa=TRUE AND p.categoria_id=? ORDER BY p.id",categoriaId,conexion);
    }

    /** Tres consultas por listado, independientemente de cuántos productos u opciones existan. */
    private List<Producto> cargarProductos(String sql, Long parametro, Connection conexion) throws SQLException {
        Objects.requireNonNull(conexion,"Falta la conexión.");
        java.util.Map<Long,Producto> productos=new java.util.LinkedHashMap<>();
        try(PreparedStatement s=conexion.prepareStatement(sql)) {
            if(parametro!=null)s.setLong(1,parametro);
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    BigDecimal precio=r.getBigDecimal("precio_base");
                    if(precio==null)throw new SQLException("Precio base null.");
                    Categoria categoria=new Categoria(r.getLong("categoria_id"),r.getString("categoria_nombre"),r.getBoolean("categoria_activa"));
                    Producto producto=new Producto(r.getLong("producto_id"),r.getString("producto_nombre"),precio,categoria,r.getBoolean("producto_activo"));
                    productos.put(producto.getId(),producto);
                }
            }
        }
        if(productos.isEmpty())return List.of();
        java.util.Map<Long,OpcionProducto> opciones=new java.util.LinkedHashMap<>();
        Set<Long> padresReceta=new java.util.LinkedHashSet<>(productos.keySet());
        String opcionesSql="SELECT o.*, po.activo AS origen_activo, co.activa AS origen_categoria_activa FROM opcion_producto o "
                +"LEFT JOIN producto po ON po.id=o.producto_origen_id LEFT JOIN categoria co ON co.id=po.categoria_id "
                +"WHERE o.eliminada=FALSE AND o.producto_id IN ("+marcas(productos.size())+") ORDER BY o.id";
        try(PreparedStatement s=conexion.prepareStatement(opcionesSql)) {
            enlazar(s,1,productos.keySet());
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    try {
                        if(r.getBigDecimal("precio_adicional")==null)throw new SQLException("Una opción tiene precio adicional null.");
                        OpcionProducto opcion=new OpcionProducto(r.getLong("id"),r.getString("nombre"),
                                TipoOpcion.valueOf(r.getString("tipo_opcion")),r.getBigDecimal("precio_adicional"),r.getBoolean("activa"));
                        long origen=r.getLong("producto_origen_id");
                        opcion.setProductoOrigenId(origen);
                        if(origen>0) {
                            padresReceta.add(origen);
                            opcion.setOrigenDisponible(r.getBoolean("origen_activo") && r.getBoolean("origen_categoria_activa"));
                        }
                        opciones.put(opcion.getId(),opcion);
                        productos.get(r.getLong("producto_id")).getOpciones().add(opcion);
                    } catch(IllegalArgumentException | NullPointerException e) {throw new SQLException("Una opción contiene datos inválidos.",e);}
                }
            }
        }
        java.util.Map<Long,List<recetaItem>> recetasBase=new java.util.HashMap<>();
        java.util.Map<Long,List<recetaItem>> recetasOpciones=new java.util.HashMap<>();
        String recetaSql="SELECT producto_id,opcion_producto_id,ingrediente_id,cantidad_requerida FROM receta_item WHERE producto_id IN ("
                +marcas(padresReceta.size())+")"+(opciones.isEmpty()?"":" OR opcion_producto_id IN ("+marcas(opciones.size())+")")+" ORDER BY ingrediente_id";
        try(PreparedStatement s=conexion.prepareStatement(recetaSql)) {
            int siguiente=enlazar(s,1,padresReceta); enlazar(s,siguiente,opciones.keySet());
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    BigDecimal cantidad=r.getBigDecimal("cantidad_requerida");
                    if(cantidad==null)throw new SQLException("Una receta tiene cantidad null.");
                    recetaItem item=new recetaItem(r.getLong("ingrediente_id"),cantidad.doubleValue());
                    long padre=r.getLong("producto_id");
                    if(!r.wasNull())recetasBase.computeIfAbsent(padre,k->new ArrayList<>()).add(item);
                    else recetasOpciones.computeIfAbsent(r.getLong("opcion_producto_id"),k->new ArrayList<>()).add(item);
                }
            }
        }
        for(Producto producto:productos.values())producto.setReceta(recetasBase.getOrDefault(producto.getId(),List.of()));
        for(OpcionProducto opcion:opciones.values()) {
            opcion.setReceta(opcion.getProductoOrigenId()>0?recetasBase.getOrDefault(opcion.getProductoOrigenId(),List.of())
                    :recetasOpciones.getOrDefault(opcion.getId(),List.of()));
            if(opcion.getReceta().isEmpty())opcion.setOrigenDisponible(false);
        }
        return List.copyOf(productos.values());
    }
    private static String marcas(int n) {return String.join(",",java.util.Collections.nCopies(n,"?"));}
    private static int enlazar(PreparedStatement s,int indice,java.util.Collection<Long> ids)throws SQLException {
        for(long id:ids)s.setLong(indice++,id);
        return indice;
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
            if(opcion.isActiva() && opcion.getProductoOrigenId()==0 && opcion.getReceta().isEmpty())
                throw new IllegalArgumentException("El extra '"+opcion.getNombre()+"' requiere ingredientes o un producto de origen.");
            if(opcion.getProductoOrigenId()>0 && opcion.isActiva()) {
                try(PreparedStatement s=conexion.prepareStatement("SELECT COUNT(r.id) FROM producto p JOIN categoria c ON c.id=p.categoria_id LEFT JOIN receta_item r ON r.producto_id=p.id WHERE p.id=? AND p.activo=TRUE AND c.activa=TRUE GROUP BY p.id")) {
                    s.setLong(1,opcion.getProductoOrigenId());
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next() || r.getLong(1)==0) throw new IllegalArgumentException("El producto del extra debe estar activo y tener una receta de ingredientes.");
                    }
                }
            }
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
        // Las opciones omitidas desaparecen del catálogo, conservando sus IDs en pedidos históricos.
        try (PreparedStatement s = conexion.prepareStatement("UPDATE opcion_producto SET activa=FALSE, eliminada=TRUE WHERE producto_id=?")) {
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
                        "UPDATE opcion_producto SET nombre=?,tipo_opcion=?,precio_adicional=?,activa=?,producto_origen_id=?,eliminada=FALSE WHERE id=? AND producto_id=?")) {
                    s.setString(1, validarNombre(opcion.getNombre())); s.setString(2, opcion.getTipo().name());
                    s.setBigDecimal(3, validarPrecio(opcion.getPrecioAdicional())); s.setBoolean(4, opcion.isActiva());
                    if(opcion.getProductoOrigenId()>0)s.setLong(5,opcion.getProductoOrigenId());else s.setNull(5,java.sql.Types.BIGINT);
                    s.setLong(6, opcionId); s.setLong(7, id); s.executeUpdate();
                }
            } else {
                try (PreparedStatement s = conexion.prepareStatement(
                        "INSERT INTO opcion_producto(producto_id,nombre,tipo_opcion,precio_adicional,activa,producto_origen_id) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    s.setLong(1, id); s.setString(2, validarNombre(opcion.getNombre())); s.setString(3, opcion.getTipo().name());
                    s.setBigDecimal(4, validarPrecio(opcion.getPrecioAdicional())); s.setBoolean(5, opcion.isActiva());
                    if(opcion.getProductoOrigenId()>0)s.setLong(6,opcion.getProductoOrigenId());else s.setNull(6,java.sql.Types.BIGINT);
                    if (s.executeUpdate() != 1) throw new SQLException("No se pudo insertar la opción.");
                    opcionId = leerId(s);
                }
            }
            guardarReceta(opcionId, true, opcion.getProductoOrigenId()>0?List.of():opcion.getReceta(), conexion);
            OpcionProducto guardada = new OpcionProducto(opcionId, opcion.getNombre().trim(), opcion.getTipo(),
                    opcion.getPrecioAdicional(), opcion.isActiva());
            guardada.setReceta(new ArrayList<>(opcion.getReceta()));
            guardada.setProductoOrigenId(opcion.getProductoOrigenId());
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
