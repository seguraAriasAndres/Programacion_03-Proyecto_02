package cr.ac.una.est.programacion03proyecto02.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.model.producto.Categoria;
import cr.ac.una.est.programacion03proyecto02.model.producto.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {
    @Test
    void conservaLaFechaOriginalAlRecuperarUnBorradorConIdLong() {
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 9, 15, 14, 35, 22);
        long id = (long) Integer.MAX_VALUE + 100;

        Pedido pedido = new Pedido(id, "Ana", fechaOriginal);
        pedido.agregarLinea(linea(1, "1000", 2));
        pedido.confirmar();

        assertEquals(id, pedido.getIdPedido());
        assertEquals(fechaOriginal, pedido.getFechaPedido());
        assertThrows(NullPointerException.class, () -> new Pedido(id, "Ana", null));
    }

    private lineaPedido linea(long id, String precio, int cantidad) {
        Producto producto = new Producto((int) id, "Producto " + id,
                new BigDecimal(precio), new Categoria(1, "Comidas", true), true);
        return new lineaPedido(id, producto, cantidad);
    }

    private Pedido pedidoConfirmado() {
        Pedido pedido = new Pedido(7, "Ana");
        pedido.agregarLinea(linea(1, "1000", 2));
        pedido.confirmar();
        return pedido;
    }

    private Recibo recibo(Pedido pedido, long idPedido, String monto) {
        Pago pago = new Pago(MetodoPago.EFECTIVO, new BigDecimal(monto),
                LocalDateTime.of(2026, 10, 19, 14, 30), "cajero");
        return new Recibo(1, idPedido, pago, pedido.getLineas());
    }

    @Test
    void creaBorradorSinReciboYExponeColeccionesProtegidas() {
        Pedido pedido = new Pedido(7, "Ana");
        var lineasAntes = pedido.getLineas();
        pedido.agregarLinea(linea(1, "1000", 2));

        assertAll(
                () -> assertEquals(7, pedido.getIdPedido()),
                () -> assertEquals("Ana", pedido.getNombreCliente()),
                () -> assertNotNull(pedido.getFechaPedido()),
                () -> assertFalse(pedido.isConfirmado()),
                () -> assertEquals(EstadoPreparacion.PENDIENTE, pedido.getEstadoPreparacion()),
                () -> assertEquals(EstadoPago.PENDIENTE, pedido.getEstadoPago()),
                () -> assertNull(pedido.getRecibo()),
                () -> assertTrue(lineasAntes.isEmpty()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> pedido.getLineas().clear()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> pedido.getIngredientesDescontados().put(1, 3.0))
        );
    }

    @Test
    void entregaSinCobrarYPermiteElPagoDiferido() {
        Pedido pedido = pedidoConfirmado();
        assertEquals(EstadoPreparacion.PENDIENTE, pedido.getEstadoPreparacion());
        assertEquals(0, pedido.calcularTotal().compareTo(new BigDecimal("2000")));

        pedido.iniciarPreparacion();
        pedido.entregarPedido();
        assertEquals(EstadoPago.PENDIENTE, pedido.getEstadoPago());
        assertNull(pedido.getRecibo());

        Recibo recibo = recibo(pedido, 7, "2000.00");
        pedido.registrarCobro(recibo);
        assertEquals(EstadoPago.PAGADO, pedido.getEstadoPago());
        assertEquals(EstadoPreparacion.ENTREGADO, pedido.getEstadoPreparacion());
        assertSame(recibo, pedido.getRecibo());
    }

    @Test
    void permiteCobrarAntesDePrepararSinAdelantarLaEntrega() {
        Pedido pedido = pedidoConfirmado();
        pedido.registrarCobro(recibo(pedido, 7, "2000"));
        assertEquals(EstadoPreparacion.PENDIENTE, pedido.getEstadoPreparacion());
        pedido.iniciarPreparacion();
        pedido.entregarPedido();
        assertEquals(EstadoPago.PAGADO, pedido.getEstadoPago());
    }

    @Test
    void rechazaSaltosDeEstadoYEdicionesTrasConfirmar() {
        Pedido vacio = new Pedido(1, "Cliente");
        assertThrows(IllegalStateException.class, vacio::confirmar);
        assertThrows(IllegalStateException.class, vacio::iniciarPreparacion);
        assertThrows(IllegalStateException.class, vacio::entregarPedido);

        Pedido pedido = pedidoConfirmado();
        lineaPedido linea = pedido.getLineas().getFirst();
        assertThrows(IllegalStateException.class, pedido::confirmar);
        assertThrows(IllegalStateException.class, pedido::entregarPedido);
        assertThrows(IllegalStateException.class, () -> pedido.agregarLinea(linea));
        assertThrows(IllegalStateException.class, () -> pedido.eliminarLinea(linea));
        pedido.iniciarPreparacion();
        assertThrows(IllegalStateException.class, pedido::iniciarPreparacion);
        assertThrows(IllegalStateException.class, pedido::cancelarPedido);
    }

    @Test
    void cancelaUnaSolaVezConservandoLosEstadosYLaConfirmacion() {
        Pedido pedido = pedidoConfirmado();
        pedido.cancelarPedido();
        assertTrue(pedido.isConfirmado());
        assertEquals(EstadoPreparacion.CANCELADO, pedido.getEstadoPreparacion());
        assertEquals(EstadoPago.PENDIENTE, pedido.getEstadoPago());
        assertThrows(IllegalStateException.class, pedido::cancelarPedido);
        assertThrows(IllegalStateException.class, pedido::iniciarPreparacion);
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarCobro(recibo(pedido, 7, "2000")));

        Pedido borrador = new Pedido(2, "Cliente");
        borrador.cancelarPedido();
        assertFalse(borrador.isConfirmado());
        assertThrows(IllegalStateException.class,
                () -> borrador.agregarLinea(linea(1, "1000", 1)));
    }

    @Test
    void rechazaCobrosInvalidosSinModificarElPedidoYEvitaDuplicados() {
        Pedido pedido = pedidoConfirmado();
        assertThrows(NullPointerException.class, () -> pedido.registrarCobro(null));
        assertThrows(IllegalArgumentException.class,
                () -> pedido.registrarCobro(recibo(pedido, 99, "2000")));
        assertThrows(IllegalArgumentException.class,
                () -> pedido.registrarCobro(recibo(pedido, 7, "1999")));
        Pago pagoSinMetodo = new Pago(null, new BigDecimal("2000"), LocalDateTime.now(), "cajero");
        assertThrows(IllegalArgumentException.class,
                () -> pedido.registrarCobro(new Recibo(1, 7, pagoSinMetodo, pedido.getLineas())));
        assertNull(pedido.getRecibo());
        assertEquals(EstadoPago.PENDIENTE, pedido.getEstadoPago());

        Recibo valido = recibo(pedido, 7, "2000");
        pedido.registrarCobro(valido);
        assertThrows(IllegalStateException.class, () -> pedido.registrarCobro(valido));
        assertThrows(IllegalStateException.class, pedido::cancelarPedido);
    }

    @Test
    void rechazaUnReciboConOtrasLineasAunqueElMontoCoincida() {
        Pedido pedido = pedidoConfirmado();
        Pago pago = new Pago(MetodoPago.EFECTIVO, new BigDecimal("2000"),
                LocalDateTime.of(2026, 10, 19, 14, 30), "cajero");
        Recibo recibo = new Recibo(1, 7, pago, List.of(linea(99, "1000", 2)));

        assertThrows(IllegalArgumentException.class, () -> pedido.registrarCobro(recibo));
        assertEquals(EstadoPago.PENDIENTE, pedido.getEstadoPago());
        assertNull(pedido.getRecibo());
    }
}
