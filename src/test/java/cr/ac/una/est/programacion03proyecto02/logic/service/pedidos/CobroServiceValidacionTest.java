package cr.ac.una.est.programacion03proyecto02.logic.service.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.repository.pedidos.PedidoRepository;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.pedidos.ReciboDAO;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class CobroServiceValidacionTest {
    private final ConexionDB bd = new ConexionDB() {
        @Override
        public Connection getConnection() {
            throw new AssertionError("No debe abrir SQL para argumentos inválidos.");
        }
    };

    @Test
    void rechazaArgumentosInvalidosAntesDeAbrirLaConexion() {
        CobroService servicio = new CobroService(new PedidoRepository(bd), new ReciboDAO(), bd);
        assertThrows(IllegalArgumentException.class, () -> servicio.cobrar(0, MetodoPago.TARJETA, "cajero"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cobrar(1, null, "cajero"));
        for (String usuario : new String[]{null, "", "  ", "a".repeat(101)}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.cobrar(1, MetodoPago.TARJETA, usuario));
        }
    }

    @Test
    void rechazaDependenciasNulas() {
        assertThrows(NullPointerException.class, () -> new CobroService(null, new ReciboDAO(), bd));
        assertThrows(NullPointerException.class, () -> new CobroService(new PedidoRepository(bd), null, bd));
        assertThrows(NullPointerException.class, () -> new CobroService(new PedidoRepository(bd), new ReciboDAO(), null));
    }
}
