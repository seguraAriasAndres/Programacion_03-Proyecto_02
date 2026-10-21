Este es el conjunto de archivos en repositorio que conforman el proyecto #02 de la clase de Programación III en la UNA.

## Organización del proyecto

Paquete base: `cr.ac.una.est.programacion03proyecto02`. Las capas son comunes a todo el equipo; los módulos se agrupan dentro de cada capa cuando tienen código.

```text
src/
  main/
    java/
      module-info.java
      cr/ac/una/est/programacion03proyecto02/
        HelloApplication.java
        Launcher.java
        model/inventario/       # Entidades y enum existentes
        dao/inventario/         # Persistencia existente
        service/inventario/     # Servicio y su contrato existente
        controller/            # HelloController
    resources/
      cr/ac/una/est/programacion03proyecto02/view/
        hello-view.fxml
      db/                      # Ubicación compartida de scripts SQL
  test/
    java/                      # Pruebas con los mismos paquetes que main/java
    resources/                 # Fixtures y configuración de pruebas
```

Las pruebas de modelos de inventario y pedidos viven en `src/test/java`.

## Configuracion de MySQL en Aiven

MySQL Connector/J esta declarado en Maven y en `module-info.java`. La configuracion
compartida de ejemplo esta en `config/database.properties.example`; cada integrante
usa `config/database.local.properties` con sus credenciales. Los archivos locales y
el truststore estan excluidos de Git y fuera de `src/main/resources`, para que no
se empaqueten en el JAR. El certificado CA publico esta en `config/aiven-ca.pem`.

Consulta [la guia de conexion](src/main/resources/db/README.md) para preparar el
truststore y conocer las propiedades que utilizara la futura `ConexionDB`.
Por ahora no se implementa esa clase ni se modifican los DAO. Tener estos archivos
no abre una conexion automaticamente. Se utiliza `defaultdb`, segun el servidor
actual; no se crean bases ni tablas al iniciar o compilar el proyecto.

## Convención para los próximos módulos

| Capa | Ubicación bajo el paquete base |
|---|---|
| Entidades y enums | `model/catalogo`, `model/inventario`, `model/pedidos`, `model/seguridad`, `model/reportes` |
| DAO | `dao/catalogo`, `dao/inventario`, `dao/pedidos`, `dao/seguridad` |
| Servicios y contratos | `service/catalogo`, `service/inventario`, `service/pedidos`, `service/seguridad`, `service/reportes` |
| Caché de pedidos | `repository/pedidos` |
| Controladores JavaFX | `controller/<modulo>` |
| Infraestructura compartida | `infrastructure` |

Los subpaquetes se crean al incorporar clases reales. Las interfaces Java permanecen junto a los servicios que las exponen, sin una carpeta por integrante. Dev 4 colocará Pedido, LineaPedido, Pago, Recibo y sus enums en `model/pedidos`; PedidoDAO y ReciboDAO en `dao/pedidos`; PedidoService, CobroService e IConsultaVentas en `service/pedidos`; y PedidoRepositorio en `repository/pedidos`.

Catálogo y pedidos consumirán el contrato de inventario desde `service/inventario`; pedidos consumirá catálogo desde `service/catalogo`; reportes consumirá IConsultaVentas desde `service/pedidos`. Esta reorganización conserva los nombres y firmas existentes.

Los FXML de cada módulo se ubicarán en `src/main/resources/cr/ac/una/est/programacion03proyecto02/view/<modulo>`, con CSS e imágenes como recursos. Se cargan con rutas absolutas de classpath, y su `fx:controller` debe usar el paquete completo del controlador. Cada paquete de controladores que use FXML debe declararse con `opens ... to javafx.fxml` en `module-info.java`.

Los scripts SQL pertenecen a `src/main/resources/db`; los datos y scripts exclusivos de pruebas, a `src/test/resources`.

## Compilación

El proyecto usa Maven Wrapper, Java release 21 y JavaFX 21.0.6. Configurar `JAVA_HOME` hacia un JDK compatible y ejecutar `mvnw.cmd clean verify` en Windows o `./mvnw clean verify` en otros sistemas. La aplicación se inicia con `mvnw.cmd javafx:run`.

El módulo Java y la clase de arranque mantienen los nombres configurados en `pom.xml`.

## Pruebas de modelos

El proyecto configura Surefire 3.5.4 para ejecutar las pruebas JUnit 5. Mientras los
DAO estén comentados y su servicio siga importándolos, utiliza el perfil de modelos:

```powershell
.\mvnw.cmd -Ppruebas-modelos clean test
```

Este perfil compila y prueba únicamente el dominio (`model`), sin compilar los DAO,
servicios ni la aplicación JavaFX. No requiere MySQL. Usa el `pom.xml` principal;
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
