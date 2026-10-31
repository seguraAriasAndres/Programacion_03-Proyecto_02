package cr.ac.una.est.programacion03proyecto02.support;

import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Un solo toolkit por JVM: JavaFX no permite reiniciarlo después de Platform.exit(). */
public final class JavaFxTestSupport {
    private JavaFxTestSupport() { }

    public static void iniciar() throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        Runnable preparar = () -> { Platform.setImplicitExit(false); listo.countDown(); };
        try { Platform.startup(preparar); }
        catch (IllegalStateException iniciado) { Platform.runLater(preparar); }
        if (!listo.await(10, TimeUnit.SECONDS)) throw new AssertionError("No se inició JavaFX.");
    }

    public static void ejecutar(Runnable accion) throws Exception {
        FutureTask<Void> tarea = new FutureTask<>(() -> { accion.run(); return null; });
        Platform.runLater(tarea);
        try { tarea.get(15, TimeUnit.SECONDS); }
        catch (ExecutionException error) {
            if (error.getCause() instanceof AssertionError fallo) throw fallo;
            throw new RuntimeException(error.getCause());
        }
    }
}
