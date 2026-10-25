package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ICatalogoService {

    // Consulta para el catálogo que se muestra al cliente.
    Producto obtenerProducto(
            long productoId,
            Connection conexion
    ) throws SQLException;

    List<Categoria> listarCategorias() throws SQLException;

    List<Producto> listarPorCategoria(long categoriaId) throws SQLException;

    List<Producto> listarProductosActivos() throws SQLException;

    List<OpcionProducto> listarOpcionesDeProducto(
            long productoId
    ) throws SQLException;

    // Operaciones administrativas de categorías.
    List<Categoria> listarCategoriasAdministracion() throws SQLException;

    void guardarCategoria(Categoria categoria) throws SQLException;

