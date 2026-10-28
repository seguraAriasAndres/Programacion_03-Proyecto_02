package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ICatalogoService {

    // Consulta el producto y calcula la disponibilidad de su receta base.
    Producto obtenerProducto(long productoId, Connection conexion) throws SQLException;

    // Comprueba la disponibilidad de una combinación usando una conexión propia.
    boolean hayStockSuficientePara(long productoId, List<Long> opcionesId, int cantidad) throws SQLException;

    // Comprueba la disponibilidad dentro de una conexión compartida.
    boolean hayStockSuficientePara(long productoId, List<Long> opcionesId, int cantidad, Connection conexion) throws SQLException;

    List<Categoria> listarCategorias() throws SQLException;

    List<Producto> listarPorCategoria(long categoriaId) throws SQLException;

    List<Producto> listarProductosActivos() throws SQLException;

    List<OpcionProducto> listarOpcionesDeProducto(long productoId) throws SQLException;

    List<Categoria> listarCategoriasAdministracion() throws SQLException;

    void guardarCategoria(Categoria categoria) throws SQLException;

    Producto obtenerProducto(long productoId) throws SQLException;

    void guardarProducto(Producto producto) throws SQLException;

    void desactivarProducto(long productoId) throws SQLException;
}
