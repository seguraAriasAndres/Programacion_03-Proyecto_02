package cr.ac.una.est.programacion03proyecto02.view.iconos;

import javafx.scene.shape.SVGPath;

import java.util.Map;

/** Iconos vectoriales reutilizables en las vistas FXML. */
public final class Icono extends SVGPath {
    private String tipo;

    private static final Map<String, String> TRAZADOS = Map.ofEntries(
            Map.entry("VOLVER", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"),
            Map.entry("NUEVO", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"),
            Map.entry("EDITAR", "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"),
            Map.entry("ELIMINAR", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"),
            Map.entry("SUBIR", "M5 20h14v-2H5v2zm8-16v10h-2V4h2zm-1-2L5.5 8.5l1.41 1.41L11 5.83V14h2V5.83l4.09 4.08L18.5 8.5 12 2z"),
            Map.entry("COBRAR", "M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 14H4v-7h16v7zm0-10H4V6h16v2z"),
            Map.entry("TEMA", "M12 3a9 9 0 1 0 9 9h-4a5 5 0 0 1-5-5V3zm-4 8a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm4-4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm4 4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"),
            Map.entry("TEMA_FRIO", "M12.9 12L12.9 2L11.1 2L11.1 12zM12.4 5.3L9.1 3.4L8.3 4.8L11.6 6.7zM12.4 6.7L15.7 4.8L14.9 3.4L11.6 5.3zM12.5 12.8L21.1 7.8L20.2 6.2L11.6 11.2zM18 9L18 5.2L16.4 5.2L16.4 9zM16.8 9.7L20.1 11.6L20.9 10.2L17.6 8.3zM11.6 12.8L20.2 17.8L21.1 16.2L12.5 11.2zM17.6 15.7L20.9 13.8L20.1 12.4L16.8 14.3zM16.4 15L16.4 18.8L18 18.8L18 15zM11.1 12L11.1 22L12.9 22L12.9 12zM11.6 18.7L14.9 20.6L15.7 19.2L12.4 17.3zM11.6 17.3L8.3 19.2L9.1 20.6L12.4 18.7zM11.5 11.2L2.9 16.2L3.8 17.8L12.5 12.8zM6 15L6 18.8L7.6 18.8L7.6 15zM7.2 14.3L3.9 12.4L3.1 13.8L6.4 15.7zM12.5 11.2L3.8 6.2L2.9 7.8L11.6 12.8zM6.4 8.3L3.1 10.2L3.9 11.6L7.2 9.7zM7.6 9L7.6 5.2L6 5.2L6 9z"),
            Map.entry("TEMA_CALIDO", "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM11 1h2v4h-2zM11 19h2v4h-2zM1 11h4v2H1zM19 11h4v2h-4zM3.5 4.9l1.4-1.4 2.8 2.8-1.4 1.4zM16.3 17.7l1.4-1.4 2.8 2.8-1.4 1.4zM3.5 19.1l2.8-2.8 1.4 1.4-2.8 2.8zM16.3 6.3l2.8-2.8 1.4 1.4-2.8 2.8z"),
            Map.entry("TEMA_PERSONALIZADO", "M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10h1.3a3 3 0 0 0 2.1-5.1 1.2 1.2 0 0 1 .8-2.1H18a4 4 0 0 0 4-4C22 6 17.5 2 12 2zM7 12a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zM10 7a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zM15 7a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zM18 11a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"),
            Map.entry("INVENTARIO", "M20 6h-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v2H4a2 2 0 0 0-2 2v12h20V8a2 2 0 0 0-2-2zM8 4h8v2H8V4zm12 14H4V8h16v10zM7 10h4v3H7zm6 0h4v3h-4z"),
            Map.entry("CATALOGO", "M3 4h18v2H3zm0 7h18v2H3zm0 7h18v2H3z"),
            Map.entry("REPORTES", "M5 9h3v10H5zm5.5-5h3v15h-3zm5.5 8h3v7h-3z"),
            Map.entry("PEDIDOS", "M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45A2 2 0 0 0 7 17h12v-2H7l1.1-2h7.45a2 2 0 0 0 1.75-1.03L21 5H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z")
    );

    public Icono() {
        getStyleClass().add("icono");
    }

    public void setTipo(String tipo) {
        String trazado = TRAZADOS.get(tipo);
        if (trazado == null) {
            throw new IllegalArgumentException("Tipo de icono desconocido: " + tipo);
        }
        this.tipo = tipo;
        setContent(trazado);
        setFillRule("TEMA_PERSONALIZADO".equals(tipo)
                ? javafx.scene.shape.FillRule.EVEN_ODD : javafx.scene.shape.FillRule.NON_ZERO);
    }

    public String getTipo() {
        return tipo;
    }
}
