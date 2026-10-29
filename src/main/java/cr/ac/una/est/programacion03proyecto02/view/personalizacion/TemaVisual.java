package cr.ac.una.est.programacion03proyecto02.view.personalizacion;

/** Diseños independientes; su orden coincide con el recorrido del selector. */
public enum TemaVisual {
    FRIO("frío", "personalizacion/app-frio.css", "TEMA_FRIO"),
    CALIDO("cálido", "personalizacion/app-calido.css", "TEMA_CALIDO"),
    PERSONALIZADO("personalizado", "personalizacion/parada-del-puente/app-personalizado.css", "TEMA_PERSONALIZADO");

    private final String nombre;
    private final String archivo;
    private final String tipoIcono;

    TemaVisual(String nombre, String archivo, String tipoIcono) {
        this.nombre = nombre;
        this.archivo = archivo;
        this.tipoIcono = tipoIcono;
    }

    public String getNombre() { return nombre; }
    public String getArchivo() { return archivo; }
    public String getTipoIcono() { return tipoIcono; }
    public TemaVisual siguiente() { return values()[(ordinal() + 1) % values().length]; }
}
