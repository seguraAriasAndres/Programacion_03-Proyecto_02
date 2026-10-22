Este es el conjunto de archivos en repositorio que conforman el proyecto #02 de la clase de Programación III en la UNA.

## Organización del proyecto

Paquete base: `cr.ac.una.est.programacion03proyecto02`. Se usan nombres de paquetes
en minúscula, como es habitual en Java. La separación por capa es común a todo el
equipo; dentro de cada capa se agrupa el código por módulo.

```text
src/main/
  java/cr/ac/una/est/programacion03proyecto02/
    app/                         # HelloApplication y Launcher: arranque
    logic/
      model/
        catalogo/                # Producto, Categoria, OpcionProducto y enums
        inventario/              # Ingredientes, recetas y movimientos
        pedidos/                 # Pedido, lineaPedido, Pago, Recibo y enums
      service/
        inventario/              # Servicio y contrato de inventario
    persistence/
      dao/
        inventario/              # DAO aún comentados
        pedidos/                 # ReciboDAO
    controller/
      seguridad/                 # Login, menú y reportes futuros
      catalogo/                  # Categorías y productos futuros
      inventario/                # Ingredientes y movimientos futuros
      pedidos/                   # Pedidos y cobro futuros
      demo/                      # HelloController temporal
  resources/cr/ac/una/est/programacion03proyecto02/
    view/
      seguridad/
      catalogo/
      inventario/
      pedidos/
      demo/hello-view.fxml
  resources/db/                  # Scripts SQL y guía de conexión
src/test/java/                   # Misma estructura de paquetes que main/java
```

`controller` está al mismo nivel que `logic` y `persistence`. `view` conserva el
mismo paquete base bajo `resources`: Maven coloca las clases y los recursos en el
classpath del programa. Los FXML permanecen separados del código fuente Java.

Los archivos `.gitkeep` conservan en Git las carpetas de controladores y vistas
que aún no tienen implementación. No son clases ni funcionalidades terminadas.
No se crearon los controladores de negocio: la demo sigue siendo la pantalla que
abre la aplicación.

Los nombres de clases existentes se conservan, incluidos `lineaPedido` y los
nombres en minúscula de inventario. El paquete de catálogo se unificó como
`catalogo` y los enums se agrupan en `enums`. Las interfaces permanecen junto al
servicio que define su contrato.

## Configuracion de MySQL en Aiven

MySQL Connector/J esta declarado en Maven y en `module-info.java`. La configuracion
compartida de ejemplo esta en `config/database.properties.example`; cada integrante
usa `config/database.local.properties` con sus credenciales. Los archivos locales y
el truststore estan excluidos de Git y fuera de `src/main/resources`, para que no
se empaqueten en el JAR. El certificado CA publico esta en `config/aiven-ca.pem`.

Consulta [la guia de conexion](src/main/resources/db/README.md) para preparar el
truststore y conocer las propiedades que utilizara la futura `ConexionDB`.
La clase `ConexionDB` sigue pendiente. `ReciboDAO` recibe una conexion del servicio;
esto permite avanzar
en pedidos sin implementar esa infraestructura. Tener estos archivos
no abre una conexion automaticamente. Se utiliza el schema `restaurante`, segun el servidor
actual; no se crean bases ni tablas al iniciar o compilar el proyecto.

## Convención para los próximos módulos

Las rutas de esta tabla son relativas al paquete base de Java:

| Responsabilidad | Paquete |
|---|---|
| Modelos | `logic.model.<modulo>` |
| Enums | `logic.model.<modulo>.enums`, o el paquete del modelo existente en inventario |
| Servicios e interfaces | `logic.service.<modulo>` |
| Repositorio de pedidos | `logic.repository.pedidos` |
| Sesión y autenticación | `logic.service.seguridad` |
| DAO | `persistence.dao.<modulo>` |
| Conexión MySQL | `persistence.connection` |
| Controladores JavaFX | `controller.<modulo>` |
| Navegación JavaFX | `view.navigation` (clases Java, cuando se implemente) |
| Arranque y composición | `app` |

Los paquetes pendientes se crean al incorporar sus clases. Dev 4 implementará
`PedidoDAO` en `persistence.dao.pedidos`, `PedidoService`, `CobroService` e
`IConsultaVentas` en `logic.service.pedidos`, y `PedidoRepositorio` en
`logic.repository.pedidos`.

Los controladores usan servicios y modelos. Los servicios coordinan reglas de
negocio y transacciones; los DAO ejecutan SQL. Un controlador no abre conexiones,
no ejecuta SQL y no accede directamente a los DAO.

Los FXML se cargan desde `/cr/ac/una/est/programacion03proyecto02/view/<modulo>/`.
Su `fx:controller` usa el paquete completo del controlador. `module-info.java`
abre por ahora `controller.demo` a `javafx.fxml`; se abrirá cada paquete de negocio
cuando contenga sus controladores. Abrir un paquete no abre sus subpaquetes.

El arranque Maven apunta a `app.HelloApplication`. Los archivos DAO solo cambiaron
de paquete e imports; no se modificaron sus consultas ni se implementaron los
DAO comentados. La futura conexión compartida sigue pendiente.

Los scripts SQL pertenecen a `src/main/resources/db`; los datos y scripts
exclusivos de pruebas, a `src/test/resources`.

## Compilación

El proyecto usa Maven Wrapper, Java release 21 y JavaFX 21.0.6. Configurar `JAVA_HOME` hacia un JDK compatible y ejecutar `mvnw.cmd clean verify` en Windows o `./mvnw clean verify` en otros sistemas. La aplicación se inicia con `mvnw.cmd javafx:run`.

El módulo Java y la clase de arranque mantienen los nombres configurados en `pom.xml`.

## Pruebas de modelos

El proyecto configura Surefire 3.5.4 para ejecutar las pruebas JUnit 5. Mientras los
DAO estén comentados y su servicio siga importándolos, utiliza el perfil de modelos:

```powershell
.\mvnw.cmd -Ppruebas-modelos clean test
```

Este perfil compila y prueba el dominio (`model`) y los DAO de pedidos, sin compilar
el servicio de inventario pendiente ni la aplicación JavaFX. Por defecto no requiere
MySQL; las pruebas JDBC se activan con `-Dpruebas.mysql=true`. Usa el `pom.xml` principal;
no necesita archivos de configuración temporales. Si no encuentra pruebas, falla
para evitar reportar éxito sin haber ejecutado ninguna.

En IntelliJ, abre el panel Maven y pulsa **Reload All Maven Projects** para que
cargue las dependencias JUnit de alcance `test`. Si no aparece el panel Maven,
haz clic derecho en `pom.xml` y selecciona **Add as Maven Project**. Verifica que
`src/test/java` esté marcado como **Test Sources Root**. Después activa el perfil
`pruebas-modelos` y ejecuta el objetivo `test` desde el panel Maven.
Esta configuración no verifica el arranque JavaFX ni las transacciones de persistencia.

La compilación completa sigue siendo `mvnw.cmd clean verify`; requiere que las
clases DAO importadas por el servicio existan, aunque solo sean esqueletos. Para
ejecutar la aplicación, desactiva el perfil de modelos y recompila el proyecto.

Si Maven no encuentra Java, configura `JAVA_HOME` con la carpeta de un JDK 21 o
superior. El wrapper Windows ejecuta Java en una sola línea para evitar errores
al interpretar variables opcionales vacías junto a continuaciones de línea.
