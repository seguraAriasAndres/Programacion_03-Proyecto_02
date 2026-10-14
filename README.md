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

Las carpetas de pruebas quedan reservadas mediante `.gitkeep`; todavía no hay pruebas automatizadas ni scripts SQL en este repositorio.

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

Si el wrapper Windows falla al interpretar sus continuaciones de línea, se puede invocar el mismo Maven Wrapper directamente desde PowerShell:

```powershell
$env:JAVA_HOME = 'C:\ruta\al\jdk'
& "$env:JAVA_HOME\bin\java.exe" '-Dmaven.multiModuleProjectDirectory=.' -classpath '.mvn\wrapper\maven-wrapper.jar' org.apache.maven.wrapper.MavenWrapperMain -B -ntp clean verify
```
