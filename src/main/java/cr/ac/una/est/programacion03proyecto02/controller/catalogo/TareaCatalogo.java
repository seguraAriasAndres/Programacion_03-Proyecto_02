package cr.ac.una.est.programacion03proyecto02.controller.catalogo;

import javafx.concurrent.Task;
import javafx.scene.Node;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Ejecuta las consultas fuera de JavaFX y aplica sus resultados en el hilo de la vista. */
final class TareaCatalogo {
    private TareaCatalogo() { }

    static <T> void ejecutar(Node vista, Callable<T> operacion, Consumer<T> alTerminar,
                            Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override protected T call() throws Exception { return operacion.call(); }
        };
        tarea.setOnSucceeded(evento -> {
            vista.setDisable(false);
            alTerminar.accept(tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            vista.setDisable(false);
            alFallar.accept(tarea.getException());
        });
        vista.setDisable(true);
        new Thread(tarea, "consulta-catalogo").start();
    }
}
