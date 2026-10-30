package cr.ac.una.est.programacion03proyecto02.logic.service.seguridad;

/**
 * Genera el hash (sal:SHA-256) que la tabla usuario necesita.
 * Uso: correr su main con cada clave como argumento, una por linea de salida.
 */
public class GeneradorHash {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: main con una o varias claves como argumentos (ej: admin1234 stock1234)");
            return;
        }
        for (String clave : args) {
            System.out.println(AutenticacionService.hashear(clave));
        }
    }
}
