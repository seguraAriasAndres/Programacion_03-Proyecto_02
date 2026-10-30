package cr.ac.una.est.programacion03proyecto02.view.personalizacion;

import cr.ac.una.est.programacion03proyecto02.view.iconos.Icono;
import cr.ac.una.est.programacion03proyecto02.view.navigation.NavegadorVistas;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Selector común a todas las pantallas; cambia colores sin recargar el formulario. */
public final class BotonTema extends Button {
    public BotonTema() {
        super("Diseño frío");
        getStyleClass().addAll("button-secondary", "theme-toggle");
        Icono icono = new Icono();
        icono.setTipo(TemaVisual.FRIO.getTipoIcono());
        setGraphic(icono);
        setGraphicTextGap(8);
        setMinWidth(USE_PREF_SIZE);
    }

    /** El navegador configura cada instancia al cargar el FXML. */
    public void configurar(NavegadorVistas navegador) {
        Objects.requireNonNull(navegador, "El navegador es obligatorio.");
        textProperty().bind(Bindings.createStringBinding(
                () -> "Diseño " + navegador.getTemaActual().getNombre(), navegador.temaProperty()));
        Tooltip ayuda = new Tooltip();
        ayuda.textProperty().bind(Bindings.createStringBinding(
                () -> "Cambiar al diseño " + navegador.getTemaActual().siguiente().getNombre(), navegador.temaProperty()));
        setTooltip(ayuda);
        accessibleTextProperty().bind(textProperty().concat(". ").concat(ayuda.textProperty()));
        graphicProperty().bind(Bindings.createObjectBinding(() -> {
            Icono icono = new Icono();
            icono.setTipo(navegador.getTemaActual().getTipoIcono());
            return icono;
        }, navegador.temaProperty()));
        setOnAction(evento -> navegador.alternarTema());
    }
}
