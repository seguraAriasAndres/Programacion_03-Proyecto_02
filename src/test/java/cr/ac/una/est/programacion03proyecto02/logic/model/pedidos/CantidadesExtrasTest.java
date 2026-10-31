package cr.ac.una.est.programacion03proyecto02.logic.model.pedidos;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.service.catalogo.calculadorReceta;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CantidadesExtrasTest {
    @Test void cobraYDescuentaCadaCantidadPorUnidadDelProducto() {
        Producto p=new Producto(1,"Gallo pinto",new BigDecimal("2000"),new Categoria(1,"Desayunos",true),true);
        p.setReceta(List.of(new recetaItem(1,200)));
        OpcionProducto huevo=new OpcionProducto(3,"Huevo",TipoOpcion.EXTRA,new BigDecimal("500"),true);
        huevo.setReceta(List.of(new recetaItem(2,1)));
        OpcionProducto queso=new OpcionProducto(4,"Queso",TipoOpcion.EXTRA,new BigDecimal("300"),true);
        queso.setReceta(List.of(new recetaItem(3,25)));p.setOpciones(List.of(huevo,queso));
        Map<Long,Integer> extras=new LinkedHashMap<>();extras.put(3L,2);extras.put(4L,3);
        lineaPedido linea=new lineaPedido(0,p,p.getOpciones(),extras,2);
        assertEquals(new BigDecimal("3900"),linea.getPrecioUnitario());
        assertEquals(new BigDecimal("7800"),linea.calcularSubtotal());
        assertEquals("Huevo x2, Queso x3",linea.getDescripcionExtras());
        assertEquals(Map.of(1L,400.0,2L,4.0,3L,150.0),calculadorReceta.calcular(p,p.getOpciones(),extras,2));
        extras.clear();assertEquals(Map.of(3L,2,4L,3),linea.getCantidadesOpciones());
        assertThrows(UnsupportedOperationException.class,()->linea.getCantidadesOpciones().clear());
        lineaPedido restaurada=lineaPedido.reconstruirPersistida(10,1,linea.getCantidadesOpciones(),linea.getNombreProducto(),linea.getNombreCategoria(),linea.getDescripcionExtras(),linea.getPrecioUnitario(),linea.getCantidad());
        assertEquals(linea.getCantidadesOpciones(),restaurada.getCantidadesOpciones());
        assertEquals(linea.calcularSubtotal(),restaurada.calcularSubtotal());
    }
    @Test void rechazaCantidadesInvalidasYSigueInterpretandoLaListaComoUnaUnidad() {
        assertEquals(Map.of(3L,1),CantidadesOpciones.unaPorId(List.of(3L)));
        assertThrows(IllegalArgumentException.class,()->CantidadesOpciones.unaPorId(List.of(3L,3L)));
        assertThrows(IllegalArgumentException.class,()->CantidadesOpciones.validar(Map.of(3L,0)));
        assertThrows(IllegalArgumentException.class,()->CantidadesOpciones.validar(Map.of(3L,-2)));
    }
}
