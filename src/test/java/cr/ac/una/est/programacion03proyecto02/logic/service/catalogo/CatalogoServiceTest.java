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

        CatalogoService servicio = crearServicio(
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

        CatalogoService servicio = crearServicio(conexionDB, categorias);
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

        CatalogoService servicio = crearServicio(conexionDB, categorias);
        Categoria categoria = new Categoria(0L, "Bebidas", true);

        assertThrows(
                SQLException.class,
                () -> servicio.guardarCategoria(categoria)
        );

        assertEquals(0L, categoria.getId());
        assertFalse(confirmada.get());
        assertTrue(revertida.get());
    }

    private CatalogoService crearServicio(
            ConexionDB conexionDB,
            categoriaDAO categorias
    ) {
        inventarioService inventario = new inventarioService(
                conexionDB,
                new ingredienteDAO(),
                new movimientoStockDAO()
        );

        return new CatalogoService(
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
