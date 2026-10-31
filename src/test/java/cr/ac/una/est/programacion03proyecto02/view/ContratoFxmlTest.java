package cr.ac.una.est.programacion03proyecto02.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import cr.ac.una.est.programacion03proyecto02.support.JavaFxTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica el contrato entre cada FXML y su controlador sin arrancar JavaFX:
 * cada campo @FXML del controlador tiene su fx:id en la vista y cada
 * onAction="#metodo" apunta a un método que existe. Un typo de FXML rompe
 * esta prueba en lugar de fallar en tiempo de ejecución.
 */
class ContratoFxmlTest {

    private static final String RUTA_BASE = "/cr/ac/una/est/programacion03proyecto02/view/";

    // Las rutas exactas que usa NavegadorVistas para abrir cada pantalla.
    private static final String[] VISTAS = {
            "seguridad/login.fxml",
            "seguridad/menu.fxml",
            "seguridad/usuarios.fxml",
            "inventario/inventario.fxml",
            "reportes/reportes.fxml",
            "catalogo/categorias.fxml",
            "catalogo/productos.fxml",
            "catalogo/producto-editor.fxml",
            "pedidos/pedidos_activos.fxml",
            "pedidos/toma_pedido.fxml",
            "pedidos/Cobro.fxml",
            "pedidos/recibo.fxml"
    };

    @Test
    void cadaFxmlExisteYSuControladorCoincideConLosControlesYEventos() throws Exception {
        for (String vista : VISTAS) {
            try (InputStream entrada = getClass().getResourceAsStream(RUTA_BASE + vista)) {
                assertNotNull(entrada, "Falta el recurso FXML: " + vista);

                Document documento = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entrada);
                String controlador = documento.getDocumentElement().getAttribute("fx:controller");
                assertFalse(controlador.isBlank(), vista + " no declara fx:controller.");

                Set<String> identificadores = new HashSet<>();
                Set<String> eventos = new HashSet<>();
                NodeList nodos = documento.getElementsByTagName("*");
                for (int i = 0; i < nodos.getLength(); i++) {
                    Element elemento = (Element) nodos.item(i);
                    if (elemento.hasAttribute("fx:id")) {
                        assertTrue(identificadores.add(elemento.getAttribute("fx:id")),
                                vista + " repite un fx:id.");
                    }
                    for (int a = 0; a < elemento.getAttributes().getLength(); a++) {
                        String atributo = elemento.getAttributes().item(a).getNodeName();
                        String valor = elemento.getAttributes().item(a).getNodeValue();
                        if (atributo.startsWith("on") && valor.startsWith("#")) {
                            eventos.add(valor.substring(1));
                        }
                    }
                }

                Class<?> tipo = Class.forName(controlador);
                assertTrue(documento.getElementsByTagName("BotonTema").getLength() == 1,
                        vista + " debe tener un selector de tema accesible fuera de las pestañas.");
                for (var campo : tipo.getDeclaredFields()) {
                    if (campo.isAnnotationPresent(FXML.class)) {
                        assertTrue(identificadores.contains(campo.getName()),
                                controlador + "." + campo.getName() + " es @FXML pero " + vista + " no declara ese fx:id.");
                    }
                }
                for (String evento : eventos) {
                    assertTrue(tieneMetodo(tipo, evento),
                            controlador + " no tiene el método " + evento + " referenciado por " + vista + ".");
                }
            }
        }
    }

    @Test
    void existeLaHojaDeEstilosNeutraBase() {
        assertNotNull(getClass().getResource(RUTA_BASE + "personalizacion/app-frio.css"),
                "Falta view/personalizacion/app-frio.css: es el tema base que adjunta NavegadorVistas.");
    }

    /**
     * Carga cada FXML con FXMLLoader sin controlador, para validar la sintaxis
     * de elementos y propiedades (un atributo mal escrito no se ve en el análisis
     * DOM). Requiere display: igual que FlujoPedidosFXMLTest, se activa con
     * -Dpruebas.javafx=true.
     */
    @EnabledIfSystemProperty(named = "pruebas.javafx", matches = "true")
    @Test
    void cadaFxmlSeCargaConFXMLLoaderSinController() throws Exception {
        JavaFxTestSupport.iniciar();

        for (String vista : VISTAS) {
            try (InputStream entrada = getClass().getResourceAsStream(RUTA_BASE + vista)) {
                assertNotNull(entrada, "Falta el recurso FXML: " + vista);
                byte[] sinControlador = quitarControllerYEventos(entrada.readAllBytes());
                FXMLLoader loader = new FXMLLoader();
                JavaFxTestSupport.ejecutar(() -> assertDoesNotThrow(() -> loader.load(new ByteArrayInputStream(sinControlador)),
                        vista + " no carga con FXMLLoader: revise elementos, atributos y propiedades."));
            }
        }
    }

    // FXMLLoader exige controlador para los eventos; se quita también fx:controller.
    private byte[] quitarControllerYEventos(byte[] xml) throws Exception {
        Document documento = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml));
        documento.getDocumentElement().removeAttribute("fx:controller");
        NodeList nodos = documento.getElementsByTagName("*");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element elemento = (Element) nodos.item(i);
            for (int a = elemento.getAttributes().getLength() - 1; a >= 0; a--) {
                String atributo = elemento.getAttributes().item(a).getNodeName();
                if (atributo.startsWith("on")) {
                    elemento.removeAttribute(atributo);
                }
            }
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(documento), new StreamResult(salida));
        return salida.toByteArray();
    }

    // FXMLLoader acepta el evento sin parámetros o con el evento de acción.
    private boolean tieneMetodo(Class<?> tipo, String nombre) {
        for (Method metodo : tipo.getDeclaredMethods()) {
            if (metodo.getName().equals(nombre)
                    && (metodo.getParameterCount() == 0
                    || (metodo.getParameterCount() == 1 && metodo.getParameterTypes()[0] == ActionEvent.class))) {
                return true;
            }
        }
        return false;
    }
}
