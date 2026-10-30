package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.categoriaDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.productoDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class catalogoService implements ICatalogoService {

    private final ConexionDB conexionDB;
    private final productoDAO productos;
    private final categoriaDAO categorias;
    private final iinventarioService inventario;

    public catalogoService(ConexionDB conexionDB, productoDAO productos, categoriaDAO categorias, iinventarioService inventario){
        this.conexionDB = Objects.requireNonNull(conexionDB, "La conexión a la base de datos no puede ser null.");
        this.productos = Objects.requireNonNull(productos, "El DAO de productos no puede ser null.");
        this.categorias = Objects.requireNonNull(categorias, "El DAO de categorías no puede ser null.");
        this.inventario = Objects.requireNonNull(inventario, "El servicio de inventario no puede ser null.");
    }

    // Lista todas las categorías para administración.
    @Override
    public List<Categoria> listarCategoriasAdministracion() throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            return List.copyOf(categorias.listarTodas(conexion));
        }
    }

    // Guarda una categoría y asigna su ID después de confirmar la transacción.
    @Override
    public void guardarCategoria(Categoria categoria) throws SQLException{
        Objects.requireNonNull(categoria, "La categoría no puede ser null.");
        validarCategoria(categoria);
        long id = ejecutarTransaccion(conexion -> categorias.guardar(categoria, conexion));
        categoria.setId(id);
    }

    // Busca un producto activo y calcula la disponibilidad de su receta base.
    @Override
    public Producto obtenerProducto(long productoId, Connection conexion) throws SQLException{
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");
        Producto producto = obtenerProductoActivo(productoId, conexion);
        producto.setDisponible(inventario.hayStockSuficiente(calculadorReceta.calcular(producto, List.of(), 1), conexion));
        return producto;
    }

    // Comprueba la disponibilidad de una combinación abriendo su propia conexión.
    @Override
    public boolean hayStockSuficientePara(long productoId, List<Long> opcionesId, int cantidad) throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            return hayStockSuficientePara(productoId, opcionesId, cantidad, conexion);
        }
    }

    // Comprueba stock para el producto y las opciones elegidas en la misma conexión.
    @Override
    public boolean hayStockSuficientePara(long productoId, List<Long> opcionesId, int cantidad, Connection conexion) throws SQLException{
        return hayStockSuficientePara(productoId,cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.CantidadesOpciones.unaPorId(opcionesId),cantidad,conexion);
    }

    @Override public boolean hayStockSuficientePara(long productoId,Map<Long,Integer> cantidadesExtras,int cantidad)throws SQLException {
        try(Connection conexion=conexionDB.getConnection()){return hayStockSuficientePara(productoId,cantidadesExtras,cantidad,conexion);}
    }

    @Override public boolean hayStockSuficientePara(long productoId,Map<Long,Integer> cantidadesExtras,int cantidad,Connection conexion)throws SQLException {
        Objects.requireNonNull(conexion, "La conexión no puede ser null.");
        if (productoId <= 0){
            throw new IllegalArgumentException("El ID del producto debe ser positivo.");
        }
        if (cantidad <= 0){
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }

        Map<Long,Integer> extras=cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.CantidadesOpciones.validar(cantidadesExtras);
        List<Long> idsValidados = List.copyOf(extras.keySet());
        Producto producto = obtenerProductoActivo(productoId, conexion);
        List<OpcionProducto> opciones = seleccionarOpciones(producto, idsValidados);
        Map<Long, Double> cantidades = calculadorReceta.calcular(producto, opciones, extras, cantidad);
        return inventario.hayStockSuficiente(cantidades, conexion);
    }

    // Busca un producto para administración, incluyendo opciones inactivas.
    @Override
    public Producto obtenerProducto(long productoId) throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            Producto producto = productos.buscar(productoId, conexion);
            if (producto == null){
                throw new NoSuchElementException("No existe el producto con ID " + productoId + ".");
            }
            return producto;
        }
    }

    // Guarda el producto y asigna sus IDs solo después de confirmar.
    @Override
    public void guardarProducto(Producto producto) throws SQLException{
        Objects.requireNonNull(producto, "El producto no puede ser null.");
        Producto guardado = ejecutarTransaccion(conexion -> productos.guardarConRecetas(producto, conexion));
        producto.setId(guardado.getId());
        for (int i = 0; i < producto.getOpciones().size(); i++){
            producto.getOpciones().get(i).setId(guardado.getOpciones().get(i).getId());
        }
    }

    // Desactiva el producto dentro de una transacción.
    @Override
    public void desactivarProducto(long productoId) throws SQLException{
        ejecutarTransaccion(conexion -> {
            productos.desactivar(productoId, conexion);
            return null;
        });
    }

    // Lista las categorías activas.
    @Override
    public List<Categoria> listarCategorias() throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            return List.copyOf(categorias.listarActivas(conexion));
        }
    }

    // Lista productos por categoría y calcula la disponibilidad de sus recetas base.
    @Override
    public List<Producto> listarPorCategoria(long categoriaId) throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            List<Producto> resultado = productos.listarPorCategoria(categoriaId, conexion);
            actualizarDisponibilidades(resultado,conexion);
            return List.copyOf(resultado);
        }
    }

    // Lista productos activos y calcula la disponibilidad de sus recetas base.
    @Override
    public List<Producto> listarProductosActivos() throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            List<Producto> resultado = productos.listarActivos(conexion);
            actualizarDisponibilidades(resultado,conexion);
            return List.copyOf(resultado);
        }
    }

    // Devuelve solo opciones activas para la toma de pedidos.
    @Override
    public List<OpcionProducto> listarOpcionesDeProducto(long productoId) throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            return List.copyOf(obtenerProducto(productoId, conexion).getOpciones());
        }
    }

    // Carga un producto activo y oculta sus opciones inactivas.
    private Producto obtenerProductoActivo(long productoId, Connection conexion) throws SQLException{
        if (productoId <= 0){
            throw new IllegalArgumentException("El ID del producto debe ser positivo.");
        }
        Producto producto = productos.buscar(productoId, conexion);
        if (producto == null || !producto.isActivo() || producto.getCategoria() == null || !producto.getCategoria().isActiva()){
            throw new NoSuchElementException("No existe un producto activo con ID " + productoId + ".");
        }
        producto.setOpciones(producto.getOpciones().stream().filter(OpcionProducto::isDisponible).toList());
        return producto;
    }

    // Comprueba el stock de la receta base de un producto.
    private void actualizarDisponibilidad(Producto producto, Connection conexion) throws SQLException{
        Map<Long, Double> cantidades = calculadorReceta.calcular(producto, List.of(), 1);
        producto.setDisponible(inventario.hayStockSuficiente(cantidades, conexion));
    }
    private void actualizarDisponibilidades(List<Producto> productos,Connection conexion)throws SQLException {
        if(productos.isEmpty())return;
        Map<Long,Map<Long,Double>> recetas=new java.util.HashMap<>();
        for(Producto producto:productos)recetas.put(producto.getId(),calculadorReceta.calcular(producto,List.of(),1));
        Map<Long,Boolean> disponibles=inventario.disponibilidades(recetas,conexion);
        for(Producto producto:productos)producto.setDisponible(Boolean.TRUE.equals(disponibles.get(producto.getId())));
    }

    // Selecciona opciones activas que pertenezcan al producto.
    private List<OpcionProducto> seleccionarOpciones(Producto producto, List<Long> opcionesId){
        List<OpcionProducto> seleccionadas = new ArrayList<>();
        for (Long opcionId : opcionesId){
            OpcionProducto encontrada = null;
            for (OpcionProducto opcion : producto.getOpciones()){
                if (opcion.getId() == opcionId){
                    encontrada = opcion;
                    break;
                }
            }
            if (encontrada == null){
                throw new IllegalArgumentException("Una opción no pertenece al producto o está inactiva.");
            }
            seleccionadas.add(encontrada);
        }
        return List.copyOf(seleccionadas);
    }

    // Valida que los IDs de opciones existan y no se repitan.
    private List<Long> validarIdsOpciones(List<Long> opcionesId){
        if (opcionesId == null){
            throw new IllegalArgumentException("Use una lista vacía si no eligió opciones.");
        }
        List<Long> resultado = new ArrayList<>();
        Set<Long> revisados = new HashSet<>();
        for (Long opcionId : opcionesId){
            if (opcionId == null || opcionId <= 0 || !revisados.add(opcionId)){
                throw new IllegalArgumentException("Los IDs de opción deben ser positivos y únicos.");
            }
            resultado.add(opcionId);
        }
        return List.copyOf(resultado);
    }

    // Valida y normaliza el nombre de una categoría.
    private void validarCategoria(Categoria categoria){
        if (categoria.getId() < 0){
            throw new IllegalArgumentException("El ID de la categoría no puede ser negativo.");
        }
        String nombre = categoria.getNombre();
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100){
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio y admite hasta 100 caracteres.");
        }
        categoria.setNombre(nombre.trim());
    }

    // Ejecuta una operación de escritura y revierte la conexión si falla.
    private <T> T ejecutarTransaccion(operacionConConexion<T> operacion) throws SQLException{
        try (Connection conexion = conexionDB.getConnection()){
            conexion.setAutoCommit(false);
            try{
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException error){
                try{
                    conexion.rollback();
                } catch (SQLException errorRollback){
                    error.addSuppressed(errorRollback);
                }
                throw error;
            }
        }
    }

    @FunctionalInterface
    private interface operacionConConexion<T>{
        T ejecutar(Connection conexion) throws SQLException;
    }
}
