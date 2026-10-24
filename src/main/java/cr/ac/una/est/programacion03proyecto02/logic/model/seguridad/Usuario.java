package cr.ac.una.est.programacion03proyecto02.logic.model.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;

import java.util.Objects;

/**
 * Usuario del sistema. Inmutable: el hash nunca cambia despues de creado;
 * la autenticacion solo lo compara, no lo recalcula.
 */
public class Usuario {
    private final long id;
    private final String nombreUsuario;
    private final String hash;
    private final Rol rol;
    private final boolean activo;

    public Usuario(long id, String nombreUsuario, String hash, Rol rol, boolean activo) {
        if (id < 0) {
            throw new IllegalArgumentException("El id del usuario no puede ser negativo.");
        }
        this.id = id;
        this.nombreUsuario = Objects.requireNonNull(nombreUsuario, "Falta el nombre de usuario.");
        if (nombreUsuario.isBlank() || nombreUsuario.length() > 100) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 1 y 100 caracteres.");
        }
        this.hash = Objects.requireNonNull(hash, "Falta el hash de la clave.");
        if (hash.isBlank() || hash.length() > 255) {
            throw new IllegalArgumentException("El hash debe tener entre 1 y 255 caracteres.");
        }
        this.rol = Objects.requireNonNull(rol, "Falta el rol del usuario.");
        this.activo = activo;
    }

    public long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getHash() {
        return hash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }
}
