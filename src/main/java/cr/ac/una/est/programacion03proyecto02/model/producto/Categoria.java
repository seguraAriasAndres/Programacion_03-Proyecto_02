package cr.ac.una.est.programacion03proyecto02.model.producto;

public class Categoria {
    private int id;
    private String nombre;
    private boolean activa;

    public Categoria() {
        this.activa = true;
    }

    public Categoria(int id, String nombre, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.activa = activa;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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
