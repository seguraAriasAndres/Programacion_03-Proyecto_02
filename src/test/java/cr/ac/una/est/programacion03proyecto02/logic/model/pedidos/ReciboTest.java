package cr.ac.una.est.programacion03proyecto02.logic.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReciboTest {
    @Test
    void conservaTotalYDesgloseAunqueCambieLaListaOriginalOElCatalogo() {
        Producto producto = new Producto(1, "Producto", new BigDecimal("1000"),
                new Categoria(1, "Comidas", true), true);
        lineaPedido linea = new lineaPedido(1, producto, 2);
        var lineas = new ArrayList<>(List.of(linea));
        Pago pago = new Pago(MetodoPago.EFECTIVO, new BigDecimal("2000"),
                LocalDateTime.of(2026, 10, 19, 14, 30), "cajero");
        Recibo recibo = new Recibo(10, 7, pago, lineas);
        lineas.clear();
        producto.setPrecioBase(new BigDecimal("5000"));

        assertAll(
                () -> assertEquals(10, recibo.getId()),
                () -> assertEquals(7, recibo.getIdPedido()),
                () -> assertSame(pago, recibo.getPago()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 19, 14, 30), recibo.getPago().getFecha()),
                () -> assertEquals(0, recibo.getTotal().compareTo(new BigDecimal("2000"))),
                () -> assertEquals(1, recibo.getLineas().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> recibo.getLineas().clear())
        );
    }
}
