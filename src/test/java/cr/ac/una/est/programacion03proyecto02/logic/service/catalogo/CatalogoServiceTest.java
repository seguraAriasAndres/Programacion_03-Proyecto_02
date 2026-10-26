package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.inventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.categoriaDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo.productoDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.ingredienteDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.movimientoStockDAO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoServiceTest {

    @Test
    void consultaComparteConexionYSumaCantidadesSinErrorBinario() throws SQLException {
        Connection conexion = conexionDeTransaccion(new AtomicBoolean(), new AtomicBoolean());
        var producto = new cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto(
                3_000_000_001L, "Producto", java.math.BigDecimal.ONE, new Categoria(1,"Categoria",true),true);
        producto.setReceta(java.util.List.of(
                new cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem(1,.1),
                new cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem(1,.2)));
        var inactiva = new cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto(
                5,"Opcion",cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion.EXTRA,
                java.math.BigDecimal.ZERO,false);
        producto.setOpciones(java.util.List.of(inactiva));
        ConexionDB bd = new ConexionDB() { public Connection getConnection() { throw new AssertionError("Conexion compartida ignorada"); } };
        productoDAO productos = new productoDAO() {
            public cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto buscar(long id, Connection c) {
                assertEquals(3_000_000_001L,id); assertSame(conexion,c); return producto;
            }
        };
        inventarioService inventario = new inventarioService(bd,new ingredienteDAO(),new movimientoStockDAO()) {
            public boolean hayStockSuficiente(java.util.Map<Long,Double> cantidades, Connection c) {
                assertSame(conexion,c); assertEquals(.3,cantidades.get(1L)); return true;
            }
        };
        var servicio = new catalogoService(bd,productos,new categoriaDAO(),inventario);
        assertTrue(servicio.obtenerProducto(3_000_000_001L,conexion).isDisponible());
        assertTrue(producto.getOpciones().isEmpty(),"La consulta para pedidos debe excluir opciones inactivas");
        // El editor debe poder trabajar incluso si el DAO entrega listas protegidas.
        producto.getReceta().add(new cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem(2,1));
        producto.getOpciones().add(inactiva);
    }

    @Test
    void guardarProductoNoAsignaIdsSiFallaCommit() {
        Connection conexion = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (p,m,a) -> {
                    if(m.getName().equals("commit")) throw new SQLException("Fallo commit");
                    if(java.util.Set.of("setAutoCommit","rollback","close").contains(m.getName())) return null;
                    throw new AssertionError(m.getName());
                });
        ConexionDB bd = new ConexionDB() { public Connection getConnection() { return conexion; } };
        productoDAO productos = new productoDAO() {
            public cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto guardarConRecetas(
                    cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto p, Connection c) {
                assertSame(conexion,c);
                return new cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto(3_000_000_001L,
                        p.getNombre(), p.getPrecioBase(),p.getCategoria(),true);
            }
        };
        var servicio = new catalogoService(bd,productos,new categoriaDAO(),
                new inventarioService(bd,new ingredienteDAO(),new movimientoStockDAO()));
        var producto = new cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto(0,"Producto",
                java.math.BigDecimal.ONE,new Categoria(1,"Categoria",true),true);
        assertThrows(SQLException.class,()->servicio.guardarProducto(producto));
        assertEquals(0,producto.getId());
    }

    @Test
    void rechazaNombreVacioAntesDeAbrirConexion() {
        AtomicBoolean intentoAbrirConexion = new AtomicBoolean(false);

        ConexionDB conexionDB = new ConexionDB() {
            @Override
            public Connection getConnection() throws SQLException {
                intentoAbrirConexion.set(true);
                throw new SQLException(
                        "No se esperaba abrir una conexión en esta prueba."
                );
            }
        };

        catalogoService servicio = crearServicio(
                conexionDB,
                new categoriaDAO()
        );

        Categoria categoria = new Categoria(0L, "  ", true);

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.guardarCategoria(categoria)
        );

        assertFalse(intentoAbrirConexion.get());
    }

    @Test
    void guardarCategoriaConfirmaYAsignaIdGenerado() throws SQLException {
        AtomicBoolean confirmada = new AtomicBoolean(false);
        AtomicBoolean revertida = new AtomicBoolean(false);
        Connection conexion = conexionDeTransaccion(confirmada, revertida);

        ConexionDB conexionDB = new ConexionDB() {
            @Override
            public Connection getConnection() {
                return conexion;
            }
        };

        categoriaDAO categorias = new categoriaDAO() {
            @Override
            public long guardar(
                    Categoria categoria,
                    Connection conexionRecibida
            ) {
                assertSame(conexion, conexionRecibida);
                assertEquals("Bebidas", categoria.getNombre());
                return 27L;
            }
        };

        catalogoService servicio = crearServicio(conexionDB, categorias);
        Categoria categoria = new Categoria(0L, " Bebidas ", true);

        servicio.guardarCategoria(categoria);

        assertEquals(27L, categoria.getId());
        assertTrue(confirmada.get());
        assertFalse(revertida.get());
    }

    @Test
    void guardarCategoriaRevierteSiFallaElDao() {
        AtomicBoolean confirmada = new AtomicBoolean(false);
        AtomicBoolean revertida = new AtomicBoolean(false);
        Connection conexion = conexionDeTransaccion(confirmada, revertida);

        ConexionDB conexionDB = new ConexionDB() {
            @Override
            public Connection getConnection() {
                return conexion;
            }
        };

        categoriaDAO categorias = new categoriaDAO() {
            @Override
            public long guardar(
                    Categoria categoria,
                    Connection conexionRecibida
            ) throws SQLException {
                throw new SQLException("Falla simulada.");
            }
        };

        catalogoService servicio = crearServicio(conexionDB, categorias);
        Categoria categoria = new Categoria(0L, "Bebidas", true);

        assertThrows(
                SQLException.class,
                () -> servicio.guardarCategoria(categoria)
        );

        assertEquals(0L, categoria.getId());
        assertFalse(confirmada.get());
        assertTrue(revertida.get());
    }

    private catalogoService crearServicio(
            ConexionDB conexionDB,
            categoriaDAO categorias
    ) {
        inventarioService inventario = new inventarioService(
                conexionDB,
                new ingredienteDAO(),
                new movimientoStockDAO()
        );

        return new catalogoService(
                conexionDB,
                new productoDAO(),
                categorias,
                inventario
        );
    }

    private Connection conexionDeTransaccion(
            AtomicBoolean confirmada,
            AtomicBoolean revertida
    ) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    return switch (metodo.getName()) {
                        case "setAutoCommit", "close" -> null;
                        case "commit" -> {
                            confirmada.set(true);
                            yield null;
                        }
                        case "rollback" -> {
                            revertida.set(true);
                            yield null;
                        }
                        default -> throw new AssertionError(
                                "Llamada JDBC no esperada: "
                                        + metodo.getName()
                        );
                    };
                }
        );
    }
  
}
