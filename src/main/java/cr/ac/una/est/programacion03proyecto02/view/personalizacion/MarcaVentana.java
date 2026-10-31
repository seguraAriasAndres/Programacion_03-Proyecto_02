package cr.ac.una.est.programacion03proyecto02.view.personalizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Identidad opcional de la ventana; no aplica hojas CSS ni cambia las pantallas. */
public final class MarcaVentana {
    public static final String PROPIEDAD = "restaurante.marca";
    private static final String ICONOS = "/cr/ac/una/est/programacion03proyecto02/view/personalizacion/parada-del-puente/iconos/";

    public enum Marca {
        NEUTRAL, PARADA_DEL_PUENTE
    }

    private MarcaVentana() { }

    /** Lee la opción de arranque; sin propiedad se conserva el icono predeterminado de JavaFX. */
    public static Marca desdeConfiguracion() {
        String valor = System.getProperty(PROPIEDAD, "neutral").trim().toLowerCase(Locale.ROOT);
        return switch (valor) {
            case "neutral" -> Marca.NEUTRAL;
            case "parada-del-puente" -> Marca.PARADA_DEL_PUENTE;
            default -> throw new IllegalArgumentException(
                    "Valor de " + PROPIEDAD + " no válido. Use neutral o parada-del-puente.");
        };
    }

    /** Aplica la marca en el hilo JavaFX; neutral retira los iconos personalizados. */
    public static void aplicar(Stage ventana, Marca marca) {
        Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        Objects.requireNonNull(marca, "La marca es obligatoria.");
        List<Image> imagenes = new ArrayList<>();
        if (marca == Marca.PARADA_DEL_PUENTE) {
            for (int tamano : new int[] {16, 32, 64, 256}) {
                String ruta = ICONOS + "icono-" + tamano + ".png";
                var recurso = MarcaVentana.class.getResource(ruta);
                if (recurso == null) {
                    throw new IllegalStateException("Falta el recurso de marca: " + ruta);
                }
                Image imagen = new Image(recurso.toExternalForm(), false);
                if (imagen.isError()) {
                    throw new IllegalStateException("No se pudo cargar el icono: " + ruta, imagen.getException());
                }
                imagenes.add(imagen);
            }
        }
        // Se reemplaza la lista completa para que alternar no acumule iconos.
        ventana.getIcons().setAll(imagenes);
    }
}
