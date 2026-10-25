package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ICatalogoService {

     //Busca un producto e incluye su receta base y sus opciones con sus recetas.
     //La conexión pertenece al llamador y este método no debe cerrarla.

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
}
