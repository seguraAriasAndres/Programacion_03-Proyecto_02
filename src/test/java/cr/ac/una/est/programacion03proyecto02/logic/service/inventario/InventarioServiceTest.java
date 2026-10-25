package cr.ac.una.est.programacion03proyecto02.logic.service.inventario;

import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.consumoVenta;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.ingrediente;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.unidadMedida;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.ingredienteDAO;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.movimientoStockDAO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class InventarioServiceTest {

    @Test
    void descontarRequiereUnaTransaccionAbierta() {
        inventarioService servicio = new inventarioService(
                new ConexionDB(),
                new ingredienteDAO(),
                new movimientoStockDAO()
        );

        SQLException error = assertThrows(
                SQLException.class,
                () -> servicio.descontar(
                        Map.of(1L, 1.0),
                        5L,
                        "cajero",
                        conexionConAutocommitActivo()
                )
        );

        assertTrue(error.getMessage().contains("transacción abierta"));
    }

    @Test
    void registrarMovimientoNoPermiteConsumoDeVentaManual() {
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

        inventarioService servicio = new inventarioService(
                conexionDB,
                new ingredienteDAO(),
                new movimientoStockDAO()
        );

        ingrediente harina = new ingrediente(
                1L,
                "Harina",
                10.0,
                unidadMedida.GRAMO,
                1.0,
                true
        );

        consumoVenta consumo = new consumoVenta(
                0L,
                12L,
                harina,
                2.0,
                LocalDateTime.now(),
                "cajero",
                "Consumo de prueba"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarMovimiento(consumo)
        );

        assertFalse(intentoAbrirConexion.get());
    }

    private Connection conexionConAutocommitActivo() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    if ("getAutoCommit".equals(metodo.getName())) {
                        return true;
                    }

                    throw new AssertionError(
                            "No se esperaba llamar a " + metodo.getName()
                    );
                }
        );
    }
}
