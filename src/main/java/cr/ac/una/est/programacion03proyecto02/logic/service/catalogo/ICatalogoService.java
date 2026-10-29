package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ICatalogoService {

    // Consulta el producto y calcula la disponibilidad de su receta base.
    Producto obtenerProducto(long productoId, Connection conexion) throws SQLException;

    // Comprueba la disponibilidad de una combinación usando una conexión propia.
    boolean hayStockSuficientePara(long productoId, List<Long> opcionesId, int cantidad) throws SQLException;

    // Comprueba la disponibilidad dentro de una conexión compartida.
    boolean hayStockSuficientePara(long productoId, List<Long> opcionesId, int cantidad, Connection conexion) throws SQLException;

    default boolean hayStockSuficientePara(long productoId,Map<Long,Integer> extras,int cantidad)throws SQLException {
        Map<Long,Integer> validadas=cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.CantidadesOpciones.validar(extras);
        if(validadas.values().stream().anyMatch(n->n!=1))throw new UnsupportedOperationException("El catálogo debe comprobar las cantidades de extras.");
        return hayStockSuficientePara(productoId,List.copyOf(validadas.keySet()),cantidad);
    }

    default boolean hayStockSuficientePara(long productoId,Map<Long,Integer> extras,int cantidad,Connection conexion)throws SQLException {
        Map<Long,Integer> validadas=cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.CantidadesOpciones.validar(extras);
        if(validadas.values().stream().anyMatch(n->n!=1))throw new UnsupportedOperationException("El catálogo debe comprobar las cantidades de extras.");
        return hayStockSuficientePara(productoId,List.copyOf(validadas.keySet()),cantidad,conexion);
    }

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
