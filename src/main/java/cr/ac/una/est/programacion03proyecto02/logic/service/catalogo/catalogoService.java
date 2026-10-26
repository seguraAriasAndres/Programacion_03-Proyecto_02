package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.categoriaDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.productoDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;
import java.math.BigDecimal;
import java.util.ArrayList;

public class catalogoService implements ICatalogoService {

    private final ConexionDB conexionDB;
    private final productoDAO productos;
    private final categoriaDAO categorias;
    private final iinventarioService inventario;

    public catalogoService(
            ConexionDB conexionDB,
            productoDAO productos,
            categoriaDAO categorias,
            iinventarioService inventario
    ) {
        this.conexionDB = Objects.requireNonNull(conexionDB);
        this.productos = Objects.requireNonNull(productos);
        this.categorias = Objects.requireNonNull(categorias);
        this.inventario = Objects.requireNonNull(inventario);
    }

    @Override
    public List<Categoria> listarCategoriasAdministracion()
            throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            return List.copyOf(categorias.listarTodas(conexion));
        }
    }
    
    @Override
    public void guardarCategoria(Categoria categoria) throws SQLException {
        Objects.requireNonNull(categoria, "La categoría no puede ser null.");
        validarCategoria(categoria);
    
        long id = ejecutarTransaccion(
                conexion -> categorias.guardar(categoria, conexion)
        );
    
        categoria.setId(id);
    }

    @Override
    public Producto obtenerProducto(
            long productoId,
            Connection conexion
    ) throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");

        Producto producto = productos.buscar(productoId, conexion);
        if (producto == null
                || !producto.isActivo()
                || producto.getCategoria() == null
                || !producto.getCategoria().isActiva()) {
            throw new NoSuchElementException(
                    "No existe un producto activo con ID " + productoId + "."
            );
        }

        actualizarDisponibilidad(producto, conexion);
        // El pedido solo puede elegir opciones activas; el editor obtiene todas por la otra consulta.
        producto.setOpciones(new ArrayList<>(producto.getOpciones().stream().filter(OpcionProducto::isActiva).toList()));
        return producto;
    }

    @Override
    public Producto obtenerProducto(long productoId) throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            Producto producto = productos.buscar(productoId, conexion);
            if (producto == null) throw new NoSuchElementException("No existe el producto con ID " + productoId);
            return producto;
        }
    }

    @Override
    public void guardarProducto(Producto producto) throws SQLException {
        // El DAO no modifica IDs del formulario antes de confirmar la transacción.
        Producto guardado = ejecutarTransaccion(conexion -> productos.guardarConRecetas(producto, conexion));
        producto.setId(guardado.getId());
        for (int i = 0; i < producto.getOpciones().size(); i++) {
            producto.getOpciones().get(i).setId(guardado.getOpciones().get(i).getId());
        }
    }

    @Override
    public void desactivarProducto(long productoId) throws SQLException {
        ejecutarTransaccion(conexion -> {
            productos.desactivar(productoId, conexion);
            return null;
        });
    }

    @Override
    public List<Categoria> listarCategorias() throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            return List.copyOf(categorias.listarActivas(conexion));
        }
    }

    @Override
    public List<Producto> listarPorCategoria(long categoriaId)
            throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            List<Producto> resultado =
                    productos.listarPorCategoria(categoriaId, conexion);

            for (Producto producto : resultado) {
                actualizarDisponibilidad(producto, conexion);
            }

            return List.copyOf(resultado);
        }
    }

    @Override
    public List<Producto> listarProductosActivos() throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            List<Producto> resultado = productos.listarActivos(conexion);

            for (Producto producto : resultado) {
                actualizarDisponibilidad(producto, conexion);
            }

            return List.copyOf(resultado);
        }
    }

    @Override
    public List<OpcionProducto> listarOpcionesDeProducto(long productoId)
            throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            return obtenerProducto(productoId, conexion).getOpciones();
        }
    }

    private void actualizarDisponibilidad(
            Producto producto,
            Connection conexion
    ) throws SQLException {
        Map<Long, BigDecimal> totales = new TreeMap<>();

        for (recetaItem item : producto.getReceta()) {
            totales.merge(
                    item.getIngredienteId(),
                    BigDecimal.valueOf(item.getCantidadRequerida()),
                    BigDecimal::add
            );
        }

        Map<Long, Double> cantidades = new TreeMap<>();
        totales.forEach((id, cantidad) -> cantidades.put(id, cantidad.doubleValue()));
        producto.setDisponible(
                inventario.hayStockSuficiente(cantidades, conexion)
        );
    }

    private void validarCategoria(Categoria categoria) {
        if (categoria.getId() < 0) {
            throw new IllegalArgumentException(
                    "El ID de la categoría no puede ser negativo."
            );
        }
    
        String nombre = categoria.getNombre();
    
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría es obligatorio y admite "
                            + "hasta 100 caracteres."
            );
        }
    
        categoria.setNombre(nombre.trim());
    }
    
    private <T> T ejecutarTransaccion(
            operacionConConexion<T> operacion
    ) throws SQLException {
        try (Connection conexion = conexionDB.getConnection()) {
            conexion.setAutoCommit(false);
    
            try {
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    error.addSuppressed(errorRollback);
                }
    
                throw error;
            }
        }
    }
    
    @FunctionalInterface
    private interface operacionConConexion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }
}
