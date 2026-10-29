package cr.ac.una.est.programacion03proyecto02.logic.service.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.OpcionProducto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.Producto;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.CantidadesOpciones;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class calculadorReceta {

    private static final int escalaCantidad = 6;
    private static final int precisionMaxima = 15;

    private calculadorReceta() {
    }

    // Calcula los ingredientes de una unidad o varias unidades con las opciones elegidas.
    public static Map<Long, Double> calcular(Producto producto, List<OpcionProducto> opcionesElegidas, int cantidad){
        Map<Long, BigDecimal> totales = new TreeMap<>();
        acumular(totales, producto, opcionesElegidas, cantidad);
        return convertir(totales);
    }

    public static Map<Long,Double> calcular(Producto producto,List<OpcionProducto> opciones,Map<Long,Integer> cantidades,int cantidad) {
        Map<Long,BigDecimal> totales=new TreeMap<>();acumular(totales,producto,opciones,cantidades,cantidad);return convertir(totales);
    }

    // Suma recetas de productos y opciones a un acumulado compartido.
    public static void acumular(Map<Long, BigDecimal> totales, Producto producto, List<OpcionProducto> opcionesElegidas, int cantidad){
        if(opcionesElegidas==null || opcionesElegidas.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Use una lista vacía si no hay extras y no incluya opciones null.");
        acumular(totales,producto,opcionesElegidas,CantidadesOpciones.unaPorId(opcionesElegidas.stream().map(OpcionProducto::getId).toList()),cantidad);
    }

    public static void acumular(Map<Long,BigDecimal> totales,Producto producto,List<OpcionProducto> opcionesElegidas,Map<Long,Integer> cantidadesExtras,int cantidad) {
        if(opcionesElegidas==null || opcionesElegidas.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("La selección de extras no puede contener null.");
        Map<Long,Integer> cantidades=CantidadesOpciones.validar(cantidadesExtras);
        if(!cantidades.keySet().equals(new HashSet<>(opcionesElegidas.stream().map(OpcionProducto::getId).toList())))
            throw new IllegalArgumentException("Las cantidades no coinciden con los extras elegidos.");
        Objects.requireNonNull(totales, "El acumulado no puede ser null.");
        Objects.requireNonNull(producto, "El producto no puede ser null.");

        if (cantidad <= 0){
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        if (opcionesElegidas == null){
            throw new IllegalArgumentException(
                    "Use una lista vacía si no se eligieron opciones."
            );
        }

        acumularReceta(totales, producto.getReceta(), cantidad);

        Set<Long> opcionesRevisadas = new HashSet<>();

        for (OpcionProducto opcion : opcionesElegidas){
            if (opcion == null
                    || !producto.getOpciones().contains(opcion)
                    || !opcion.isDisponible()
                    || opcion.getId() <= 0
                    || !opcionesRevisadas.add(opcion.getId())){
                throw new IllegalArgumentException(
                        "Cada opción debe pertenecer al producto, estar activa "
                                + "y aparecer una sola vez."
                );
            }

            int unidadesExtra;
            try {unidadesExtra=Math.multiplyExact(cantidad,cantidades.get(opcion.getId()));}
            catch(ArithmeticException error){throw new IllegalArgumentException("La cantidad total de extras es demasiado grande.",error);}
            acumularReceta(totales, opcion.getReceta(), unidadesExtra);
        }
    }

    // Convierte cantidades acumuladas al formato que acepta inventario y MySQL.
    public static Map<Long, Double> convertir( Map<Long, BigDecimal> totales){
        Objects.requireNonNull(totales, "El acumulado no puede ser null.");

        Map<Long, Double> resultado = new TreeMap<>();

        for (Map.Entry<Long, BigDecimal> entrada : totales.entrySet()){
            Long ingredienteId = entrada.getKey();
            BigDecimal cantidad = entrada.getValue();

            if (ingredienteId == null || ingredienteId <= 0 || cantidad == null || cantidad.signum() <= 0){
                throw new IllegalArgumentException("El acumulado contiene un ingrediente o cantidad inválidos.");
            }

            BigDecimal normalizada;
            try{
                normalizada = cantidad.setScale(
                        escalaCantidad,
                        RoundingMode.UNNECESSARY
                );
            } catch (ArithmeticException error){
                throw new IllegalArgumentException("Las cantidades admiten como máximo seis decimales.", error);
            }

            double valor = normalizada.doubleValue();

            if (normalizada.precision() > precisionMaxima || !Double.isFinite(valor) || BigDecimal.valueOf(valor).compareTo(normalizada) != 0){
                throw new IllegalArgumentException("La cantidad requerida excede el rango DECIMAL(15,6).");
            }

            resultado.put(ingredienteId, valor);
        }

        return Collections.unmodifiableMap(resultado);
    }

    // Multiplica cada componente de una receta por la cantidad solicitada.
    private static void acumularReceta(Map<Long, BigDecimal> totales, List<recetaItem> receta, int cantidad){
        Objects.requireNonNull(receta, "La receta no puede ser null.");

        for (recetaItem item : receta){
            if (item == null || item.getIngredienteId() <= 0){
                throw new IllegalArgumentException("La receta debe referenciar ingredientes guardados.");
            }

            double cantidadPorUnidad = item.getCantidadRequerida();

            if (!Double.isFinite(cantidadPorUnidad)
                    || cantidadPorUnidad <= 0){
                throw new IllegalArgumentException("La receta debe contener cantidades positivas y finitas.");
            }

            BigDecimal requerida = BigDecimal.valueOf(cantidadPorUnidad)
                    .multiply(BigDecimal.valueOf(cantidad));

            totales.merge(item.getIngredienteId(), requerida, BigDecimal::add);
        }
    }
}
