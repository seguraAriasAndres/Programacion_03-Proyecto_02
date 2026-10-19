package cr.ac.una.est.programacion03proyecto02.model.pedidos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class Recibo {
    private final long id;
    private final long idPedido;
    private final BigDecimal total;
    private final Pago pago;
    private final List<lineaPedido> lineas;

    public Recibo(long id, long idPedido, Pago pago, List <lineaPedido> lineas) {
            this.id = id;
            this.idPedido = idPedido;
            this.pago = Objects.requireNonNull(pago, "El pago no puede ser null.");
            this.lineas = List.copyOf(lineas);
            BigDecimal total = BigDecimal.ZERO;
            for (lineaPedido linea : this.lineas) {
                total = total.add(linea.calcularSubTotal());
            }
            this.total = total;
    }

    public long getId() { return id; }
    public long getIdPedido() { return idPedido; }
    public BigDecimal getTotal() { return total; }
    public Pago getPago() { return pago; }
    public List<lineaPedido> getLineas() { return lineas; }
}
