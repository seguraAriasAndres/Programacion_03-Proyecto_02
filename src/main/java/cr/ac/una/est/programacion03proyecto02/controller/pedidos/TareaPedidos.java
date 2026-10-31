package cr.ac.una.est.programacion03proyecto02.controller.pedidos;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Mantiene las llamadas a servicios fuera de JavaFX y evita operaciones simultaneas. */
final class TareaPedidos {
    private final SimpleBooleanProperty ocupado = new SimpleBooleanProperty();

    TareaPedidos(Node vista) { vista.disableProperty().bind(ocupado); }
    boolean estaOcupado() { return ocupado.get(); }
    ReadOnlyBooleanProperty ocupadoProperty() { return ocupado; }

    <T> void ejecutar(Callable<T> operacion, Consumer<T> exito, Consumer<Throwable> fallo) {
        if (estaOcupado()) return;
        Task<T> tarea = new Task<>() {
            @Override protected T call() throws Exception { return operacion.call(); }
        };
        tarea.setOnSucceeded(e -> {
            ocupado.set(false);
            try { exito.accept(tarea.getValue()); } catch (RuntimeException error) { fallo.accept(error); }
        });
        tarea.setOnFailed(e -> { ocupado.set(false); fallo.accept(tarea.getException()); });
        tarea.setOnCancelled(e -> ocupado.set(false));
        ocupado.set(true);
        new Thread(tarea, "operacion-pedidos").start();
    }

    static String mensaje(String contexto, Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException
                || error instanceof java.util.NoSuchElementException || error instanceof SecurityException) {
            return contexto + " " + error.getMessage();
        }
        return contexto + " No se pudo confirmar el resultado. Actualice el pedido antes de reintentar.";
    }
}
