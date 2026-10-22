package cr.ac.una.est.programacion03proyecto02.logic.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.EstadoPreparacion;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Categoria;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

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
        Producto producto = new Producto(id, "Producto " + id,
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
                        () -> pedido.getIngredientesDescontados().put(1L, 3.0))
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

    @Test
    void reconstruyeUnPedidoEntregadoYPagadoSinRepetirLasTransiciones() {
        long id = 3_000_000_001L;
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 1, 12, 30);
        var lineas = new ArrayList<>(List.of(linea(3_000_000_002L, "1200", 2)));
        var descuentos = new HashMap<>(Map.of(3_000_000_003L, 2.5));
        Pago pago = new Pago(MetodoPago.TARJETA, new BigDecimal("2400"), fecha.plusHours(1), "cajero");
        Recibo recibo = new Recibo(3_000_000_004L, id, pago, lineas);

        Pedido pedido = Pedido.reconstruir(id, "Ana", fecha, true, EstadoPreparacion.ENTREGADO,
                EstadoPago.PAGADO, lineas, descuentos, recibo);
        lineas.clear();
        descuentos.clear();
        assertAll(
                () -> assertEquals(id, pedido.getIdPedido()),
                () -> assertEquals(fecha, pedido.getFechaPedido()),
                () -> assertTrue(pedido.isConfirmado()),
                () -> assertEquals(EstadoPreparacion.ENTREGADO, pedido.getEstadoPreparacion()),
                () -> assertEquals(EstadoPago.PAGADO, pedido.getEstadoPago()),
                () -> assertSame(recibo, pedido.getRecibo()),
                () -> assertEquals(1, pedido.getLineas().size()),
                () -> assertEquals(Map.of(3_000_000_003L, 2.5), pedido.getIngredientesDescontados()),
                () -> assertThrows(IllegalStateException.class, pedido::confirmar),
                () -> assertThrows(IllegalStateException.class, () -> pedido.registrarCobro(recibo))
        );
    }

    @Test
    void reconstruyeBorradoresYCanceladosPeroRechazaEstadosPersistidosIncoherentes() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 1, 12, 30);
        var lineas = List.of(linea(1, "1000", 2));
        Pedido borrador = Pedido.reconstruir(7, "Ana", fecha, false, EstadoPreparacion.PENDIENTE,
                EstadoPago.PENDIENTE, List.of(), Map.of(), null);
        borrador.agregarLinea(lineas.getFirst());
        assertEquals(fecha, borrador.getFechaPedido());
        Pedido cancelado = Pedido.reconstruir(7, "Ana", fecha, true, EstadoPreparacion.CANCELADO,
                EstadoPago.PENDIENTE, lineas, Map.of(1L, 2.0), null);
        assertThrows(IllegalStateException.class, cancelado::iniciarPreparacion);

        Recibo recibo = recibo(borrador, 7, "2000");
        assertThrows(IllegalArgumentException.class, () -> Pedido.reconstruir(7, "Ana", fecha,
                false, EstadoPreparacion.EN_PREPARACION, EstadoPago.PENDIENTE, lineas, Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> Pedido.reconstruir(7, "Ana", fecha,
                true, EstadoPreparacion.CANCELADO, EstadoPago.PAGADO, lineas, Map.of(), recibo));
        assertThrows(IllegalArgumentException.class, () -> Pedido.reconstruir(7, "Ana", fecha,
                true, EstadoPreparacion.ENTREGADO, EstadoPago.PAGADO, lineas, Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> Pedido.reconstruir(8, "Ana", fecha,
                true, EstadoPreparacion.ENTREGADO, EstadoPago.PAGADO, lineas, Map.of(), recibo));
        assertThrows(IllegalArgumentException.class, () -> Pedido.reconstruir(7, "Ana", fecha,
                true, EstadoPreparacion.PENDIENTE, EstadoPago.PENDIENTE,
                List.of(lineas.getFirst(), lineas.getFirst()), Map.of(), null));
    }

    @Test
    void registraDescuentosConIdsLongSinAceptarCantidadesInvalidasNiCambiosTrasConfirmar() {
        Pedido pedido = new Pedido(7, "Ana");
        pedido.agregarLinea(linea(1, "1000", 2));
        var descuentos = new HashMap<>(Map.of(3_000_000_000L, 2.0));
        pedido.registrarIngredientesDescontados(descuentos);
        descuentos.clear();
        assertThrows(IllegalArgumentException.class,
                () -> pedido.registrarIngredientesDescontados(Map.of(1L, Double.NaN)));
        assertEquals(Map.of(3_000_000_000L, 2.0), pedido.getIngredientesDescontados());
        pedido.confirmar();
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarIngredientesDescontados(Map.of(1L, 1.0)));
    }
}
