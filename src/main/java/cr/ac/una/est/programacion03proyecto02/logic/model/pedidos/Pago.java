package cr.ac.una.est.programacion03proyecto02.logic.model.pedidos;
import cr.ac.una.est.programacion03proyecto02.logic.model.pedidos.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago {
    private final MetodoPago metodo;
    private final BigDecimal monto;
    private final LocalDateTime fecha;
    private final String usuario;

    public Pago (MetodoPago metodo, BigDecimal monto, LocalDateTime fecha, String usuario) {
        this.metodo = metodo;
        this.monto = monto;
        this.fecha = fecha;
        this.usuario = usuario;
    }

    public MetodoPago getMetodo() { return metodo;}
    public BigDecimal getMonto() { return monto;}
    public LocalDateTime getFecha() { return fecha;}
    public String getUsuario() { return usuario;}

    @Override
    public String toString() {
        String respuesta = getUsuario() + "," + getMetodo() + "," + getMonto() + "," + getFecha();
        return respuesta;
    }

}
