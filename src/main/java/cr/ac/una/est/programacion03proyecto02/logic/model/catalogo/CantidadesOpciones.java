package cr.ac.una.est.programacion03proyecto02.logic.model.catalogo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cantidades de cada extra por unidad del producto de una línea. */
public final class CantidadesOpciones {
    private CantidadesOpciones() { }
    public static Map<Long,Integer> unaPorId(List<Long> ids) {
        if(ids==null)throw new IllegalArgumentException("Use una lista vacía si no hay extras.");
        Map<Long,Integer> cantidades=new LinkedHashMap<>();
        for(Long id:ids) {
            if(id==null || id<=0 || cantidades.put(id,1)!=null)
                throw new IllegalArgumentException("Los extras requieren IDs positivos y únicos.");
        }
        return validar(cantidades);
    }
    public static Map<Long,Integer> validar(Map<Long,Integer> cantidades) {
        if(cantidades==null)throw new IllegalArgumentException("Use un mapa vacío si no hay extras.");
        Map<Long,Integer> copia=new LinkedHashMap<>();
        cantidades.forEach((id,cantidad)->{
            if(id==null || id<=0 || cantidad==null || cantidad<=0)
                throw new IllegalArgumentException("Cada extra requiere un ID y una cantidad positivos.");
            copia.put(id,cantidad);
        });
        return Collections.unmodifiableMap(copia);
    }
}
