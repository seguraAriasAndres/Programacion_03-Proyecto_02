package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;

import java.sql.Connection;
import java.util.List;

//define las operaciones disponibles para consultar el catálogo
public interface ICatalogoService {

    //obtiene un producto usando la conexión recibida
    Producto obtenerProducto(int id, Connection c);

    //obtiene las categorías activas del catálogo
    List<Categoria> listarCategorias();

    //obtiene los productos activos de una categoría
    List<Producto> listarPorCategoria(int id);

    //obtiene los productos activos de todas las categorías activas
    List<Producto> listarProductosActivos();

    //obtiene las opciones asociadas a un producto
    List<OpcionProducto> listarOpcionesDeProducto(int id);
}