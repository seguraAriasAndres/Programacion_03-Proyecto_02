package cr.ac.una.est.programacion03proyecto02.logic.model.catalogo;

public class Categoria {
    private long id;
    private String nombre;
    private boolean activa;

    public Categoria() {
        this.activa = true;
    }

    public Categoria(long id, String nombre, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.activa = activa;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
