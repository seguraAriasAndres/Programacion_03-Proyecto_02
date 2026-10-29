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
    }

    public String getTipo() {
        return tipo;
    }
}
