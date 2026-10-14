package cr.ac.una.est.programacion03proyecto02.model.pedidos;
import cr.ac.una.est.programacion03proyecto02.model.pedidos.enum_class.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Pago {
    MetodoPago metodo;
    BigDecimal monto;
    LocalDate fecha;
    String usuario;

    public Pago (MetodoPago metodo, BigDecimal monto, LocalDate fecha, String usuario) {
        this.metodo = metodo;
        this.monto = monto;
        this.fecha = fecha;
        this.usuario = usuario;
    }

    public MetodoPago getMetodo() { return metodo;}
    public BigDecimal getMonto() { return monto;}
    public LocalDate getFecha() { return fecha;}
    public String getUsuario() { return usuario;}

    @Override
    public String toString() {
        String respuesta = getUsuario() + "," + getMetodo() + "," + getMonto() + "," + getFecha();
        return respuesta;
    }

}
