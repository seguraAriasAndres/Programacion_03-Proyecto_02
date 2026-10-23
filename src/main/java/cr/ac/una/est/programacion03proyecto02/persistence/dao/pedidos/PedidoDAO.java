package cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Pedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.Recibo;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.lineaPedido;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Savepoint;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PedidoDAO {
    private final Connection conexion;
    private final LineaPedidoDAO lineas = new LineaPedidoDAO();
    private final ReciboDAO recibos = new ReciboDAO();

    private static final String CABECERA = """
            SELECT id, nombre_cliente, fecha_pedido, confirmado, estado_preparacion, estado_pago
            FROM pedido
            """;
    private static final String ACTUALIZAR_ESTADOS = """
            UPDATE pedido SET confirmado = ?, estado_preparacion = ?, estado_pago = ? WHERE id = ?
            """;
    private static final String CARGAR_DESCUENTOS = """
            SELECT ingrediente_id, cantidad FROM ingredientes_descontados
            WHERE pedido_id = ? ORDER BY ingrediente_id
            """;
    private static final String INSERTAR_DESCUENTO = """
            INSERT INTO ingredientes_descontados (pedido_id, ingrediente_id, cantidad) VALUES (?, ?, ?)
            """;
    private static final String RANKING = """
            SELECT l.producto_id, SUM(l.cantidad) AS unidades
            FROM linea_pedido l JOIN pedido p ON p.id = l.pedido_id
            JOIN Recibo r ON r.id_pedido = p.id
            WHERE p.confirmado = 1 AND p.estado_pago = 'PAGADO'
                AND p.estado_preparacion <> 'CANCELADO'
                AND r.fecha_recibo >= ? AND r.fecha_recibo < ?
            GROUP BY l.producto_id ORDER BY unidades DESC, l.producto_id
            """;

    // Se omite id para que MySQL lo genere mediante AUTO_INCREMENT.
    private static final String INSERTAR = """
            INSERT INTO pedido
                (nombre_cliente, fecha_pedido, confirmado,
                 estado_preparacion, estado_pago, usuario_creacion)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String ACTUALIZAR_BORRADOR = """
            UPDATE pedido
            SET nombre_cliente = ?
            WHERE id = ? AND confirmado = 0
                AND estado_preparacion = 'PENDIENTE' AND estado_pago = 'PENDIENTE'
            """;

    private static final String BLOQUEAR_BORRADOR = """
            SELECT fecha_pedido FROM pedido
            WHERE id = ? AND confirmado = 0
                AND estado_preparacion = 'PENDIENTE' AND estado_pago = 'PENDIENTE'
            FOR UPDATE
            """;

    public PedidoDAO(Connection conexion) {
        // Conserva la conexión recibida para ejecutar las operaciones del DAO.
        this.conexion = Objects.requireNonNull(conexion, "La conexion no puede ser null.");
    }

    /** La conexion pertenece al llamador: este DAO no la cierra ni hace commit/rollback. */
    public Pedido insertarBorrador(String nombreCliente, String nombreUsuario) throws SQLException {
        // Crea un pedido vacío en MySQL y lo devuelve con su ID generado y su fecha de creación.
        if (nombreCliente == null || nombreCliente.isBlank() || nombreCliente.length() > 150) {
            throw new IllegalArgumentException("El nombre del cliente debe tener entre 1 y 150 caracteres y no estar en blanco.");
        }
        if (nombreUsuario == null || nombreUsuario.isBlank() || nombreUsuario.length() > 100) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 1 y 100 caracteres y no estar en blanco.");
        }
        // DATETIME(6) conserva microsegundos. El objeto devuelto usa esta misma fecha.
        LocalDateTime fechaPedido = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, nombreCliente);
            sentencia.setObject(2, fechaPedido);
            sentencia.setBoolean(3, false);
            sentencia.setString(4, EstadoPreparacion.PENDIENTE.name());
            sentencia.setString(5, EstadoPago.PENDIENTE.name());
            sentencia.setString(6, nombreUsuario);

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No se inserto exactamente un pedido.");
            }

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("MySQL no devolvio el id del pedido.");
                }
                long id = claves.getLong(1);
                if (claves.wasNull() || id <= 0) {
                    throw new SQLException("MySQL devolvio un id de pedido invalido.");
                }
                return new Pedido(id, nombreCliente, fechaPedido);
            }
        }
    }

    /**
     Guarda cabecera, lineas y opciones en la transaccion del llamador.
      Devuelve una copia con los ids de lineas generados; usarla tras commit.
      Si falla, revierte solo este guardado mediante un savepoint.
     **/
    public Pedido guardarBorrador(Pedido pedido) throws SQLException {
        // Guarda la cabecera, líneas y opciones del borrador y devuelve una copia con los IDs generados.
        Objects.requireNonNull(pedido, "El pedido no puede ser null.");
        if (pedido.getIdPedido() <= 0) {
            throw new IllegalArgumentException("El pedido debe tener un id generado por la base de datos.");
        }
        if (pedido.isConfirmado() || pedido.getEstadoPreparacion() != EstadoPreparacion.PENDIENTE
                || pedido.getEstadoPago() != EstadoPago.PENDIENTE
                || !pedido.getIngredientesDescontados().isEmpty() || pedido.getRecibo() != null) {
            throw new IllegalStateException("Solo se puede guardar un borrador pendiente, sin descuentos ni cobros.");
        }
        if (pedido.getNombreCliente() == null || pedido.getNombreCliente().isBlank()
                || pedido.getNombreCliente().length() > 150) {
            throw new IllegalArgumentException("El nombre del cliente debe tener entre 1 y 150 caracteres y no estar en blanco.");
        }
        if (conexion.getAutoCommit()) {
            throw new SQLException("Guardar el borrador completo requiere autoCommit=false; el llamador hace commit o rollback.");
        }

        Savepoint inicio = conexion.setSavepoint();
        try {
            LocalDateTime fechaOriginal = bloquearBorrador(pedido.getIdPedido());
            try (PreparedStatement sentencia = conexion.prepareStatement(ACTUALIZAR_BORRADOR)) {
                sentencia.setString(1, pedido.getNombreCliente());
                sentencia.setLong(2, pedido.getIdPedido());
                int filas = sentencia.executeUpdate();
                // La fila ya esta comprobada y bloqueada: 0 tambien es valido si el nombre no cambio.
                if (filas > 1) {
                    throw new SQLException("Se actualizo mas de un pedido.");
                }
            }
            var lineasGuardadas = lineas.sincronizar(pedido.getIdPedido(), pedido.getLineas(), conexion);
            Pedido guardado = Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), fechaOriginal,
                    false, EstadoPreparacion.PENDIENTE, EstadoPago.PENDIENTE, lineasGuardadas, Map.of(), null);
            conexion.releaseSavepoint(inicio);
            return guardado;
        } catch (SQLException | RuntimeException error) {
            revertirHasta(conexion, inicio, error);
            throw error;
        }
    }

    private LocalDateTime bloquearBorrador(long id) throws SQLException {
        // Comprueba que el pedido siga editable, bloquea su fila y recupera su fecha original.
        try (PreparedStatement sentencia = conexion.prepareStatement(BLOQUEAR_BORRADOR)) {
            sentencia.setLong(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("El pedido no existe o ya no es un borrador editable.");
                }
                return resultado.getObject("fecha_pedido", LocalDateTime.class);
            }
        }
    }

    /** Recupera el agregado completo y bloquea su cabecera hasta commit/rollback. Devuelve null si no existe. */
    public Pedido buscarBloqueado(long id) throws SQLException {
        // Recupera el pedido completo y bloquea sus datos en la transacción; devuelve null si no existe.
        exigirTransaccion(conexion);
        validarId(id);
        Cabecera cabecera = buscarCabeceraBloqueada(id, conexion);
        return cabecera == null ? null : cargarPedido(cabecera, conexion, true);
    }

    public Pedido buscarBloqueado(long id, Connection c) throws SQLException {
        // Mantiene la firma del UML utilizando la conexión explícita de la transacción del servicio.
        return new PedidoDAO(c).buscarBloqueado(id);
    }

    /**
     * Persiste las transiciones del modelo y, al confirmar, el descuento historico.
     * Las lineas deben estar guardadas previamente con guardarBorrador.
     * El inventario y la insercion del recibo corresponden a los servicios, usando esta misma conexion.
     */
    public void actualizar(Pedido pedido) throws SQLException {
        // Valida y guarda los cambios de estado y, al confirmar, los ingredientes realmente descontados.
        exigirTransaccion(conexion);
        Objects.requireNonNull(pedido, "El pedido no puede ser null.");
        validarId(pedido.getIdPedido());
        Savepoint inicio = conexion.setSavepoint();
        try {
            Cabecera cabecera = buscarCabeceraBloqueada(pedido.getIdPedido(), conexion);
            if (cabecera == null) {
                throw new SQLException("El pedido no existe.");
            }
            Recibo reciboSQL = recibos.buscarPorPedido(cabecera.id(), conexion, true);
            // Durante cobrar(), el recibo se inserta antes de actualizar estado_pago.
            Pedido anterior = reconstruir(cabecera, lineas.cargar(cabecera.id(), conexion, true),
                    cargarDescuentos(cabecera.id(), conexion, true),
                    cabecera.pago() == EstadoPago.PAGADO ? reciboSQL : null);
            validarDatosConservados(anterior, pedido);
            Pedido.reconstruir(pedido.getIdPedido(), pedido.getNombreCliente(), pedido.getFechaPedido(),
                    pedido.isConfirmado(), pedido.getEstadoPreparacion(), pedido.getEstadoPago(),
                    pedido.getLineas(), pedido.getIngredientesDescontados(), pedido.getRecibo());

            boolean confirmando = !anterior.isConfirmado() && pedido.isConfirmado();
            if (confirmando) {
                anterior.registrarIngredientesDescontados(pedido.getIngredientesDescontados());
                anterior.confirmar();
            } else if (!anterior.getIngredientesDescontados().equals(pedido.getIngredientesDescontados())) {
                throw new IllegalStateException("El descuento historico solo se registra al confirmar y luego se conserva.");
            }
            if (anterior.getEstadoPreparacion() != pedido.getEstadoPreparacion()) {
                switch (pedido.getEstadoPreparacion()) {
                    case EN_PREPARACION -> anterior.iniciarPreparacion();
                    case ENTREGADO -> anterior.entregarPedido();
                    case CANCELADO -> anterior.cancelarPedido();
                    case PENDIENTE -> throw new IllegalStateException("No se puede regresar a preparacion pendiente.");
                }
            }
            if (pedido.getEstadoPago() == EstadoPago.PAGADO) {
                validarReciboPersistido(pedido.getRecibo(), reciboSQL);
                if (anterior.getEstadoPago() != EstadoPago.PAGADO) {
                    anterior.registrarCobro(reciboSQL);
                }
            } else if (reciboSQL != null) {
                throw new IllegalStateException("Existe un recibo: el pedido debe quedar PAGADO en esta transaccion.");
            }
            if (anterior.isConfirmado() != pedido.isConfirmado()
                    || anterior.getEstadoPago() != pedido.getEstadoPago()) {
                throw new IllegalStateException("No se puede deshacer una confirmacion ni un pago.");
            }

            if (confirmando) {
                guardarDescuentos(pedido, conexion);
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(ACTUALIZAR_ESTADOS)) {
                sentencia.setBoolean(1, pedido.isConfirmado());
                sentencia.setString(2, pedido.getEstadoPreparacion().name());
                sentencia.setString(3, pedido.getEstadoPago().name());
                sentencia.setLong(4, pedido.getIdPedido());
                if (sentencia.executeUpdate() > 1) {
                    throw new SQLException("Se actualizo mas de un pedido.");
                }
            }
            conexion.releaseSavepoint(inicio);
        } catch (SQLException | RuntimeException error) {
            revertirHasta(conexion, inicio, error);
            throw error;
        }
    }

    public void actualizar(Pedido pedido, Connection c) throws SQLException {
        // Mantiene la firma del UML y delega en la actualización que usa la conexión recibida.
        new PedidoDAO(c).actualizar(pedido);
    }

    /** Incluye borradores; excluye cancelados y los que estan ENTREGADO y PAGADO. */
    public List<Pedido> listarActivos() throws SQLException {
        // Recupera los pedidos que siguen activos, incluidos borradores y entregados pendientes de pago.
        exigirTransaccion(conexion);
        return listar(CABECERA + """
                WHERE estado_preparacion <> 'CANCELADO'
                    AND NOT (estado_preparacion = 'ENTREGADO' AND estado_pago = 'PAGADO')
                ORDER BY fecha_pedido, id
                """, null, null);
    }

    /** Ventas por fecha del recibo: desde incluido y hasta excluido. */
    public List<Pedido> listarPagados(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        // Recupera ventas por fecha de cobro, incluyendo desde y excluyendo hasta.
        exigirTransaccion(conexion);
        validarRango(desde, hasta);
        return listar("""
                SELECT p.id, p.nombre_cliente, p.fecha_pedido, p.confirmado,
                       p.estado_preparacion, p.estado_pago
                FROM pedido p JOIN Recibo r ON r.id_pedido = p.id
                WHERE p.confirmado = 1 AND p.estado_pago = 'PAGADO'
                    AND p.estado_preparacion <> 'CANCELADO'
                    AND r.fecha_recibo >= ? AND r.fecha_recibo < ?
                ORDER BY r.fecha_recibo, p.id
                """, desde, hasta);
    }

    /** Producto -> unidades vendidas; IDs y acumulados long, orden descendente por unidades. */
    public Map<Long, Long> ranking(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        // Suma las unidades vendidas por producto en el periodo y las ordena de mayor a menor.
        validarRango(desde, hasta);
        Map<Long, Long> ranking = new LinkedHashMap<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(RANKING)) {
            sentencia.setObject(1, desde);
            sentencia.setObject(2, hasta);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    try {
                        long productoId = resultado.getLong("producto_id");
                        long unidades = resultado.getBigDecimal("unidades").longValueExact();
                        if (productoId <= 0 || unidades <= 0) {
                            throw new SQLException("El ranking contiene ids o cantidades invalidos.");
                        }
                        ranking.put(productoId, unidades);
                    } catch (ArithmeticException error) {
                        throw new SQLException("Las unidades vendidas exceden el rango de long.", error);
                    }
                }
            }
        }
        return Collections.unmodifiableMap(ranking);
    }

    private Cabecera buscarCabeceraBloqueada(long id, Connection c) throws SQLException {
        // Busca y bloquea la cabecera del pedido; devuelve null cuando no encuentra el ID.
        try (PreparedStatement sentencia = c.prepareStatement(CABECERA + "WHERE id = ? FOR UPDATE")) {
            sentencia.setLong(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? leerCabecera(resultado) : null;
            }
        }
    }

    private Cabecera leerCabecera(ResultSet resultado) throws SQLException {
        // Convierte la fila SQL actual en una cabecera y comprueba que sus estados sean válidos.
        try {
            return new Cabecera(resultado.getLong("id"), resultado.getString("nombre_cliente"),
                    resultado.getObject("fecha_pedido", LocalDateTime.class), resultado.getBoolean("confirmado"),
                    EstadoPreparacion.valueOf(resultado.getString("estado_preparacion")),
                    EstadoPago.valueOf(resultado.getString("estado_pago")));
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new SQLException("El pedido contiene estados persistidos invalidos.", error);
        }
    }

    private Pedido cargarPedido(Cabecera cabecera, Connection c, boolean bloquear) throws SQLException {
        // Completa la cabecera con sus líneas, opciones, descuentos y recibo, aplicando el bloqueo solicitado.
        return reconstruir(cabecera, lineas.cargar(cabecera.id(), c, bloquear),
                cargarDescuentos(cabecera.id(), c, bloquear), recibos.buscarPorPedido(cabecera.id(), c, bloquear));
    }

    private Pedido reconstruir(Cabecera cabecera, List<lineaPedido> detalle,
            Map<Long, Double> descuentos, Recibo recibo) throws SQLException {
        // Reconstruye el modelo con los datos guardados y reporta como error SQL cualquier inconsistencia.
        try {
            return Pedido.reconstruir(cabecera.id(), cabecera.nombre(), cabecera.fecha(), cabecera.confirmado(),
                    cabecera.preparacion(), cabecera.pago(), detalle, descuentos, recibo);
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new SQLException("El pedido " + cabecera.id() + " contiene datos persistidos inconsistentes.", error);
        }
    }

    private List <Pedido> listar(String sql, LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        // Ejecuta una consulta de cabeceras, completa cada pedido y devuelve una lista protegida.
        List<Cabecera> cabeceras = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (desde != null) {
                sentencia.setObject(1, desde);
                sentencia.setObject(2, hasta);
            }
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    cabeceras.add(leerCabecera(resultado));
                }
            }
        }
        List<Pedido> pedidos = new ArrayList<>();
        for (Cabecera cabecera : cabeceras) {
            pedidos.add(cargarPedido(cabecera, conexion, false));
        }
        return List.copyOf(pedidos);
    }

    private Map<Long, Double> cargarDescuentos(long id, Connection c, boolean bloquear) throws SQLException {
        // Recupera las cantidades históricas descontadas por ingrediente y valida sus IDs y cantidades.
        Map<Long, Double> descuentos = new LinkedHashMap<>();
        try (PreparedStatement sentencia = c.prepareStatement(CARGAR_DESCUENTOS + (bloquear ? " FOR UPDATE" : ""))) {
            sentencia.setLong(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    long ingredienteId = resultado.getLong("ingrediente_id");
                    BigDecimal cantidad = resultado.getBigDecimal("cantidad");
                    if (ingredienteId <= 0 || cantidad == null || cantidad.signum() <= 0) {
                        throw new SQLException("El pedido contiene un descuento de ingrediente invalido.");
                    }
                    descuentos.put(ingredienteId, cantidad.doubleValue());
                }
            }
        }
        return descuentos;
    }

    private void guardarDescuentos(Pedido pedido, Connection c) throws SQLException {
        // Inserta los descuentos por ingrediente en orden de ID, comprobando la precisión de DECIMAL(15,6).
        for (var entrada : new java.util.TreeMap<>(pedido.getIngredientesDescontados()).entrySet()) {
            BigDecimal cantidad;
            try {
                cantidad = BigDecimal.valueOf(entrada.getValue()).setScale(6, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException error) {
                throw new IllegalArgumentException("El descuento requiere como maximo seis decimales.", error);
            }
            if (cantidad.signum() <= 0 || cantidad.precision() > 15) {
                throw new IllegalArgumentException("El descuento debe ser positivo y caber en DECIMAL(15,6).");
            }
            try (PreparedStatement sentencia = c.prepareStatement(INSERTAR_DESCUENTO)) {
                sentencia.setLong(1, pedido.getIdPedido());
                sentencia.setLong(2, entrada.getKey());
                sentencia.setBigDecimal(3, cantidad);
                if (sentencia.executeUpdate() != 1) {
                    throw new SQLException("No se pudo guardar el descuento de un ingrediente.");
                }
            }
        }
    }

    private void validarDatosConservados(Pedido anterior, Pedido nuevo) {
        // Comprueba que actualizar no cambie el nombre, fecha, líneas ni opciones ya guardados.
        if (!anterior.getNombreCliente().equals(nuevo.getNombreCliente())
                || !anterior.getFechaPedido().equals(nuevo.getFechaPedido())
                || anterior.getLineas().size() != nuevo.getLineas().size()) {
            throw new IllegalArgumentException("Guardar primero el borrador: actualizar solo persiste estados y descuentos.");
        }
        for (int i = 0; i < anterior.getLineas().size(); i++) {
            lineaPedido a = anterior.getLineas().get(i);
            lineaPedido b = nuevo.getLineas().get(i);
            if (a.getId() != b.getId() || a.getProductoId() != b.getProductoId()
                    || a.getCantidad() != b.getCantidad()
                    || a.getPrecioUnitario().compareTo(b.getPrecioUnitario()) != 0
                    || !a.getNombreProducto().equals(b.getNombreProducto())
                    || !a.getNombreCategoria().equals(b.getNombreCategoria())
                    || !a.getDescripcionExtras().equals(b.getDescripcionExtras())
                    || !new java.util.HashSet<>(a.getOpcionIds()).equals(new java.util.HashSet<>(b.getOpcionIds()))) {
                throw new IllegalArgumentException("Las lineas no coinciden con las guardadas; guardar primero el borrador.");
            }
        }
    }

    private void validarReciboPersistido(Recibo pedido, Recibo sql) {
        // Verifica que el recibo del modelo coincida con el guardado antes de permitir el estado PAGADO.
        if (pedido == null || sql == null || pedido.getId() != sql.getId()
                || pedido.getTotal().compareTo(sql.getTotal()) != 0
                || pedido.getPago().getMetodo() != sql.getPago().getMetodo()
                || !Objects.equals(pedido.getPago().getUsuario(), sql.getPago().getUsuario())
                || pedido.getPago().getFecha() == null
                || !pedido.getPago().getFecha().truncatedTo(ChronoUnit.MICROS).equals(sql.getPago().getFecha())) {
            throw new IllegalStateException("Para marcar PAGADO debe existir el mismo recibo en esta transaccion.");
        }
    }

    private void exigirTransaccion(Connection c) throws SQLException {
        // Exige una conexión válida con autocommit desactivado para coordinar la transacción del servicio.
        Objects.requireNonNull(c, "La conexion no puede ser null.");
        if (c.getAutoCommit()) {
            throw new SQLException("La operacion requiere autoCommit=false; el servicio hace commit o rollback.");
        }
    }

    private void validarId(long id) {
        // Rechaza IDs de pedido que no sean positivos.
        if (id <= 0) {
            throw new IllegalArgumentException("El id del pedido debe ser positivo.");
        }
    }

    private void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        // Comprueba que ambas fechas existan y que desde sea anterior a hasta.
        if (desde == null || hasta == null || !desde.isBefore(hasta)) {
            throw new IllegalArgumentException("El rango requiere desde < hasta; desde incluido y hasta excluido.");
        }
    }

    private void revertirHasta(Connection c, Savepoint inicio, Exception error) {
        // Revierte hasta el punto de guardado y lo libera, conservando los posibles errores de limpieza.
        try {
            c.rollback(inicio);
        } catch (SQLException fallo) {
            error.addSuppressed(fallo);
        }
        try {
            c.releaseSavepoint(inicio);
        } catch (SQLException fallo) {
            error.addSuppressed(fallo);
        }
    }

    private record Cabecera(long id, String nombre, LocalDateTime fecha, boolean confirmado,
            EstadoPreparacion preparacion, EstadoPago pago) { }
}
