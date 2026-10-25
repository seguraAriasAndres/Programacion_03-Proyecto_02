package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.iinventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.CategoriaDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ProductoDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//implementa las operaciones del catálogo y delega el acceso a datos a los DAO correspondientes
public class CatalogoService implements ICatalogoService {
    private final ProductoDAO productos;
    private final CategoriaDAO categorias;
    private final iinventarioService inventario;

    //recibe las dependencias que utiliza el servicio
    public CatalogoService(ProductoDAO productos, CategoriaDAO categorias, iinventarioService inventario) {
        this.productos = productos;
        this.categorias = categorias;
        this.inventario = inventario;
    }

    //busca un producto usando la conexión recibida
    @Override
    public Producto obtenerProducto(int id, Connection c) {
        return productos.buscar(id, c);
    }

    //devuelve las categorías activas
    @Override
    public List<Categoria> listarCategorias() {
        return categorias.listarActivas();
    }

    //devuelve los productos activos de una categoría
    @Override
    public List<Producto> listarPorCategoria(int id) {
        return productos.listarActivos(id);
    }

    //reúne los productos activos de todas las categorías activas
    @Override
    public List<Producto> listarProductosActivos() {
        List<Producto> lista = new ArrayList<>();
        for (Categoria categoria : listarCategorias()) {
            int categoriaId = Math.toIntExact(categoria.getId());
            lista.addAll(productos.listarActivos(categoriaId));
        }
        return lista;
    }

    //busca el producto y devuelve sus opciones, este método abre y cierra su propia conexión
    @Override
    public List<OpcionProducto> listarOpcionesDeProducto(int id) {
        ConexionDB bd = new ConexionDB();
        try (Connection conexion = bd.getConnection()) {
            Producto producto = obtenerProducto(id, conexion);
            if (producto == null || producto.getOpciones() == null) {
                return new ArrayList<>();
            }
            return producto.getOpciones();
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudieron listar las opciones del producto.", error);
        }
    }

    //guarda un producto con su receta y opciones
    public void guardarProducto(Producto producto) {
        productos.guardarConRecetas(producto);
    }

    //guarda una categoría nueva o actualiza una existente
    public void guardarCategoria(Categoria categoria) {
        categorias.guardar(categoria);
    }

    //desactiva un producto sin borrarlo de la base de datos
    public void desactivarProducto(int id) {
        productos.desactivar(id);
    }
}