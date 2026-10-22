package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

import cr.ac.una.est.programacion03proyecto02.logic.model.seguridad.Usuario;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.seguridad.UsuarioDAO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Login del sistema. La clave se almacena como SHA-256 con sal aleatoria en
 * formato salHex:hashHex; la comparacion es de tiempo constante.
 * Todo intento queda registrado en auditoria_login, tambien los fallidos.
 */
public class AutenticacionService implements IAutenticacionService {

    private static final int TAMANO_SAL = 16;
    private static final int TAMANO_RESUMEN = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioDAO usuarios = new UsuarioDAO();

    @Override
    public Usuario autenticar(String nombre, String clave) throws SQLException {
        validarEntrada(nombre, clave);
        Usuario usuario = usuarios.buscar(nombre);
        boolean exitoso = usuario != null && usuario.isActivo()
                && coincide(clave, usuario.getHash());
        usuarios.registrarIntento(nombre, exitoso);
        return exitoso ? usuario : null;
    }

    /** Genera un hash nuevo con sal aleatoria; dos llamadas jamas dan lo mismo. */
    static String hashear(String clave) {
        Objects.requireNonNull(clave, "Falta la clave.");
        byte[] sal = new byte[TAMANO_SAL];
        ALEATORIO.nextBytes(sal);
        return HexFormat.of().formatHex(sal) + ":" + HexFormat.of().formatHex(resumir(sal, clave));
    }

    /** Compara la clave contra el hash; cualquier hash corrupto se rechaza, no explota. */
    static boolean coincide(String clave, String hash) {
        if (clave == null || hash == null) {
            return false;
        }
        String[] partes = hash.split(":");
        if (partes.length != 2) {
            return false;
        }
        try {
            byte[] sal = HexFormat.of().parseHex(partes[0]);
            byte[] guardado = HexFormat.of().parseHex(partes[1]);
            if (sal.length != TAMANO_SAL || guardado.length != TAMANO_RESUMEN) {
                return false;
            }
            return MessageDigest.isEqual(guardado, resumir(sal, clave));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private void validarEntrada(String nombre, String clave) {
        Objects.requireNonNull(nombre, "Falta el nombre de usuario.");
        Objects.requireNonNull(clave, "Falta la clave.");
        if (nombre.isBlank() || nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 1 y 100 caracteres.");
        }
        if (clave.isBlank()) {
            throw new IllegalArgumentException("La clave no puede estar vacia.");
        }
    }

    private static byte[] resumir(byte[] sal, String clave) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(sal);
            return sha.digest(clave.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("El JDK no ofrece SHA-256.", error);
        }
    }
}
