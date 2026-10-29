package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.ResumenUsuario;
import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.enums.Rol;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.seguridad.UsuarioDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Administración de cuentas permitida exclusivamente a una sesión ADMIN. */
public class UsuariosService {
    private final UsuarioDAO usuarios;
    private final SesionActual sesion;

    public UsuariosService(UsuarioDAO usuarios, SesionActual sesion) {
        this.usuarios=Objects.requireNonNull(usuarios); this.sesion=Objects.requireNonNull(sesion);
    }
    public List<ResumenUsuario> listar() throws SQLException {
        sesion.exigirRol(Rol.ADMIN);
        return usuarios.listar();
    }
    public ResumenUsuario crear(String nombre, String clave, Rol rol, boolean activo) throws SQLException {
        sesion.exigirRol(Rol.ADMIN);
        if (nombre==null || nombre.isBlank() || nombre.trim().length()>100)
            throw new IllegalArgumentException("El usuario requiere de 1 a 100 caracteres.");
        if (clave==null || clave.isBlank() || clave.length()<8 || clave.length()>128)
            throw new IllegalArgumentException("La clave requiere de 8 a 128 caracteres.");
        Objects.requireNonNull(rol,"Seleccione un rol.");
        try {
            long id=usuarios.crear(nombre.trim(),AutenticacionService.hashear(clave),rol,activo);
            return new ResumenUsuario(id,nombre.trim(),rol,activo);
        } catch (SQLException error) {
            if (error.getErrorCode()==1062) throw new IllegalArgumentException("Ya existe una cuenta con ese nombre.",error);
            throw error;
        }
    }
}
