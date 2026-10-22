package cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/** Persistencia de las lineas y sus opciones; PedidoDAO coordina el borrador y la transaccion. */
public class LineaPedidoDAO {
    private static final String LISTAR_IDS = """
            SELECT id FROM linea_pedido WHERE pedido_id = ? ORDER BY id FOR UPDATE
            """;
    private static final String CARGAR = """
            SELECT id, producto_id, nombre_producto, nombre_categoria, descripcion_opciones,
                   precio_unitario, cantidad FROM linea_pedido WHERE pedido_id = ? ORDER BY id
            """;
    private static final String CARGAR_OPCIONES = """
            SELECT o.linea_pedido_id, o.opcion_producto_id
            FROM linea_pedido_opcion o JOIN linea_pedido l ON l.id = o.linea_pedido_id
            WHERE l.pedido_id = ? ORDER BY o.linea_pedido_id, o.opcion_producto_id
            """;
    private static final String INSERTAR = """
            INSERT INTO linea_pedido
                (pedido_id, producto_id, nombre_producto, nombre_categoria,
                 descripcion_opciones, precio_unitario, cantidad)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String ACTUALIZAR = """
            UPDATE linea_pedido
            SET producto_id = ?, nombre_producto = ?, nombre_categoria = ?,
                descripcion_opciones = ?, precio_unitario = ?, cantidad = ?
            WHERE id = ? AND pedido_id = ?
            """;
    private static final String ELIMINAR = "DELETE FROM linea_pedido WHERE id = ? AND pedido_id = ?";
    private static final String ELIMINAR_OPCIONES = "DELETE FROM linea_pedido_opcion WHERE linea_pedido_id = ?";
    private static final String INSERTAR_OPCION = """
            INSERT INTO linea_pedido_opcion (linea_pedido_id, opcion_producto_id) VALUES (?, ?)
            """;

    List<lineaPedido> sincronizar(long pedidoId, List<lineaPedido> lineas, Connection conexion) throws SQLException {
        // Sincroniza todas las líneas y opciones del borrador: agrega, actualiza o elimina según la lista recibida.
        Objects.requireNonNull(conexion, "La conexion no puede ser null.");
        if (pedidoId <= 0 || conexion.getAutoCommit()) {
            throw new SQLException("Las lineas requieren un pedido persistido y una transaccion abierta.");
        }
        List<DatosLinea> datos = validarLineas(lineas);
        Set<Long> existentes = listarIds(pedidoId, conexion);
        Set<Long> conservadas = new HashSet<>();
        for (DatosLinea dato : datos) {
            long id = dato.linea().getId();
            if (id > 0) {
                if (!existentes.contains(id)) {
                    throw new IllegalArgumentException("La linea " + id + " no pertenece al pedido " + pedidoId + ".");
                }
                conservadas.add(id);
            }
        }
        // Primero borrar las relaciones hijas para respetar las FK de las lineas eliminadas.
        for (long id : existentes) {
            if (!conservadas.contains(id)) {
                borrarOpciones(id, conexion);
                try (PreparedStatement sentencia = conexion.prepareStatement(ELIMINAR)) {
                    sentencia.setLong(1, id);
                    sentencia.setLong(2, pedidoId);
                    if (sentencia.executeUpdate() != 1) {
                        throw new SQLException("No se pudo eliminar la linea del borrador.");
                    }
                }
            }
        }
        List<lineaPedido> guardadas = new ArrayList<>();
        for (DatosLinea dato : datos) {
            long id = dato.linea().getId();
            if (id == 0) {
                id = insertar(pedidoId, dato, conexion);
            } else {
                actualizar(pedidoId, dato, conexion);
                borrarOpciones(id, conexion);
            }
            for (long opcionId : dato.opciones()) {
                try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR_OPCION)) {
                    sentencia.setLong(1, id);
                    sentencia.setLong(2, opcionId);
                    if (sentencia.executeUpdate() != 1) {
                        throw new SQLException("No se pudo guardar una opcion de la linea.");
                    }
                }
            }
            guardadas.add(dato.linea().conIdPersistido(id));
        }
        // Coincide con ORDER BY id usado al recuperar el desglose en ReciboDAO.
        guardadas.sort(Comparator.comparingLong(lineaPedido::getId));
        return List.copyOf(guardadas);
    }

    private Set<Long> listarIds(long pedidoId, Connection conexion) throws SQLException {
        // Recupera y bloquea los IDs de las líneas que actualmente pertenecen al pedido.
        Set<Long> ids = new LinkedHashSet<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(LISTAR_IDS)) {
            sentencia.setLong(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    ids.add(resultado.getLong("id"));
                }
            }
        }
        return ids;
    }

    private long insertar(long pedidoId, DatosLinea dato, Connection conexion) throws SQLException {
        // Inserta una línea nueva vinculada al pedido y devuelve el ID long generado por MySQL.
        try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setLong(1, pedidoId);
            asignarDatos(sentencia, 2, dato);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No se inserto exactamente una linea de pedido.");
            }
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("MySQL no devolvio el id de la linea.");
                }
                long id = claves.getLong(1);
                if (claves.wasNull() || id <= 0) {
                    throw new SQLException("MySQL devolvio un id de linea invalido.");
                }
                return id;
            }
        }
    }

    private void actualizar(long pedidoId, DatosLinea dato, Connection conexion) throws SQLException {
        // Actualiza los datos de una línea existente usando su ID y el del pedido al que pertenece.
        try (PreparedStatement sentencia = conexion.prepareStatement(ACTUALIZAR)) {
            asignarDatos(sentencia, 1, dato);
            sentencia.setLong(7, dato.linea().getId());
            sentencia.setLong(8, pedidoId);
            // La linea ya esta bloqueada y pertenece al pedido; 0 es valido si no cambio nada.
            if (sentencia.executeUpdate() > 1) {
                throw new SQLException("Se actualizo mas de una linea.");
            }
        }
    }

    /** Seis datos comunes al INSERT y al UPDATE, comenzando en el indice indicado. */
    private void asignarDatos(PreparedStatement sentencia, int inicio, DatosLinea dato) throws SQLException {
        // Asigna los seis datos comunes de una línea a los parámetros SQL desde la posición indicada.
        sentencia.setLong(inicio, dato.productoId());
        sentencia.setString(inicio + 1, dato.linea().getNombreProducto());
        sentencia.setString(inicio + 2, dato.linea().getNombreCategoria());
        sentencia.setString(inicio + 3, dato.linea().getDescripcionExtras());
        sentencia.setBigDecimal(inicio + 4, dato.precio());
        sentencia.setInt(inicio + 5, dato.linea().getCantidad());
    }

    private void borrarOpciones(long id, Connection conexion) throws SQLException {
        // Elimina las relaciones de opciones seleccionadas de una línea sin borrar las opciones del catálogo.
        try (PreparedStatement sentencia = conexion.prepareStatement(ELIMINAR_OPCIONES)) {
            sentencia.setLong(1, id);
            sentencia.executeUpdate();
        }
    }

    private List<DatosLinea> validarLineas(List<lineaPedido> lineas) {
        // Valida referencias, textos, cantidades y precios de las líneas y prepara sus datos para SQL.
        List<DatosLinea> datos = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        for (lineaPedido linea : List.copyOf(lineas)) {
            if (linea.getId() < 0 || (linea.getId() > 0 && !ids.add(linea.getId()))) {
                throw new IllegalArgumentException("Las lineas persistidas no pueden repetir id.");
            }
            if (linea.getProductoId() <= 0) {
                throw new IllegalArgumentException("Una linea editable necesita el id del producto; no basta el desglose historico.");
            }
            validarTexto(linea.getNombreProducto(), 150, "nombre del producto");
            validarTexto(linea.getNombreCategoria(), 100, "nombre de categoria");
            if (linea.getDescripcionExtras() == null || linea.getDescripcionExtras().length() > 1000
                    || linea.getCantidad() <= 0) {
                throw new IllegalArgumentException("La descripcion no puede superar 1000 caracteres y la cantidad debe ser positiva.");
            }
            BigDecimal precio;
            try {
                precio = linea.getPrecioUnitario().setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException error) {
                throw new IllegalArgumentException("El precio de la linea tiene mas de dos decimales; no se redondea al guardar.", error);
            }
            if (precio.signum() < 0 || precio.precision() > 15) {
                throw new IllegalArgumentException("El precio de la linea debe ser no negativo y caber en DECIMAL(15,2).");
            }
            Set<Long> opciones = new LinkedHashSet<>();
            for (long opcionId : linea.getOpcionIds()) {
                if (opcionId <= 0 || !opciones.add(opcionId)) {
                    throw new IllegalArgumentException("Las opciones necesitan ids positivos y no repetidos.");
                }
            }
            datos.add(new DatosLinea(linea, linea.getProductoId(), precio, List.copyOf(opciones)));
        }
        return datos;
    }

    private void validarTexto(String texto, int limite, String campo) {
        // Comprueba que el texto tenga contenido y no exceda la longitud permitida para el campo.
        if (texto == null || texto.isBlank() || texto.length() > limite) {
            throw new IllegalArgumentException("El " + campo + " es obligatorio y no puede superar " + limite + " caracteres.");
        }
    }

    private record DatosLinea(lineaPedido linea, long productoId, BigDecimal precio, List<Long> opciones) { }

    /** Uso interno: recupera lineas completas por IDs, sin depender de tablas de catalogo. */
    List<lineaPedido> cargar(long pedidoId, Connection conexion, boolean bloquear) throws SQLException {
        // Reconstruye las líneas y sus IDs de opciones sin consultar catálogo, bloqueándolas si se solicita.
        String bloqueo = bloquear ? " FOR UPDATE" : "";
        Map<Long, List<Long>> opciones = new HashMap<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(CARGAR_OPCIONES + bloqueo)) {
            sentencia.setLong(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    opciones.computeIfAbsent(resultado.getLong("linea_pedido_id"), id -> new ArrayList<>())
                            .add(resultado.getLong("opcion_producto_id"));
                }
            }
        }
        List<lineaPedido> cargadas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(CARGAR + bloqueo)) {
            sentencia.setLong(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    long id = resultado.getLong("id");
                    try {
                        cargadas.add(lineaPedido.reconstruirPersistida(id, resultado.getLong("producto_id"),
                                opciones.getOrDefault(id, List.of()), resultado.getString("nombre_producto"),
                                resultado.getString("nombre_categoria"), resultado.getString("descripcion_opciones"),
                                resultado.getBigDecimal("precio_unitario"), resultado.getInt("cantidad")));
                    } catch (IllegalArgumentException | NullPointerException error) {
                        throw new SQLException("La linea " + id + " contiene datos persistidos invalidos.", error);
                    }
                }
            }
        }
        return List.copyOf(cargadas);
    }
}
