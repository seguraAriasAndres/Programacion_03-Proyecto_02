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
        pedidos/                 # CobroService
      repository/
        pedidos/                 # PedidoRepository: caché de pedidos activos
    persistence/
      dao/
        inventario/              # DAO aún comentados
        pedidos/                 # PedidoDAO, LineaPedidoDAO y ReciboDAO
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
truststore y conocer las propiedades que utiliza `ConexionDB`.
`persistence.connection.ConexionDB` lee `config/database.local.properties` y
devuelve una conexion nueva por llamada; `ReciboDAO` recibe una conexion del
servicio. Tener estos archivos
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

Los paquetes pendientes se crean al incorporar sus clases. `CobroService` vive en
`logic.service.pedidos` y `PedidoRepository` en `logic.repository.pedidos`.
`PedidoService` e `IConsultaVentas` siguen pendientes.

Los controladores usan servicios y modelos. Los servicios coordinan reglas de
negocio y transacciones; los DAO ejecutan SQL. Un controlador no abre conexiones,
no ejecuta SQL y no accede directamente a los DAO.

Los FXML se cargan desde `/cr/ac/una/est/programacion03proyecto02/view/<modulo>/`.
Su `fx:controller` usa el paquete completo del controlador. `module-info.java`
abre por ahora `controller.demo` a `javafx.fxml`; se abrirá cada paquete de negocio
cuando contenga sus controladores. Abrir un paquete no abre sus subpaquetes.

El arranque Maven apunta a `app.HelloApplication`. Los DAO comentados de inventario
siguen pendientes; `ConexionDB` ya está disponible.

Los scripts SQL pertenecen a `src/main/resources/db`; los datos y scripts
exclusivos de pruebas, a `src/test/resources`.

## Guardado de borradores de pedidos

`PedidoDAO.insertarBorrador(nombreCliente, nombreUsuario)` crea la cabecera vacía
y devuelve el pedido con el ID generado por MySQL. El usuario es quien lo creó;
guardar el borrador después no cambia ese dato ni la fecha original.

`PedidoDAO.guardarBorrador(pedido)` guarda la cabecera y delega en `LineaPedidoDAO`
la sincronización de `linea_pedido` y `linea_pedido_opcion`: inserta líneas nuevas
con ID `0`, actualiza las existentes y elimina las retiradas junto con sus opciones.
Las líneas existentes deben pertenecer al pedido. Los nombres y precios guardados
son los que conserva la línea, sin recalcularlos desde el catálogo.

El servicio debe usar una misma conexión con `autoCommit=false` y ejecutar el
`commit` o `rollback`. Los DAO no cierran esa conexión. Si falla el guardado,
`PedidoDAO` revierte sus cambios hasta un punto de guardado (`Savepoint`).
El método devuelve una copia del pedido con los IDs de línea generados, de tipo
`long`; el servicio debe conservar esa copia después de un `commit` exitoso y
descartarla si revierte la transacción. Reutilizar el objeto anterior con IDs `0`
haría que esas líneas se trataran otra vez como nuevas.

`LineaPedidoDAO.sincronizar` es interno al paquete: las pantallas utilizan el
servicio, y este utiliza `PedidoDAO` para guardar el borrador completo.

## Consultas y estados de pedidos

`PedidoDAO` usa la conexión de su constructor en todas las operaciones. También
conserva las sobrecargas del UML `buscarBloqueado(id, c)` y `actualizar(pedido, c)`;
estas usan la conexión explícita recibida. El servicio crea el DAO con la misma
conexión que entrega a inventario y a `ReciboDAO` para esa operación.

| Método | Comportamiento |
|---|---|
| `buscarBloqueado(id, c)` | Recupera cabecera, líneas, IDs de opciones, descuentos y recibo. Bloquea las filas leídas y devuelve `null` si el pedido no existe. |
| `actualizar(pedido, c)` | Guarda los estados y, al confirmar, las cantidades realmente descontadas. Comprueba las transiciones del modelo y conserva las líneas históricas. |
| `listarActivos()` | Incluye borradores y pedidos en atención; excluye cancelados y los que ya están entregados y pagados. |
| `listarPagados(desde, hasta)` | Recupera pedidos pagados completos por fecha del recibo. |
| `ranking(desde, hasta)` | Devuelve `Map<Long, Long>` de producto a unidades vendidas, ordenado de mayor a menor, con desempate por ID. |

Los rangos de ventas incluyen `desde` y excluyen `hasta`; para un día completo
se usa desde su medianoche hasta la medianoche siguiente. Se usa la fecha de cobro,
no la de creación del borrador. Los reportes excluyen cancelados y pedidos sin pagar.
Los listados completos y las operaciones bloqueadas requieren `autoCommit=false`;
`ranking`, que ejecuta una sola consulta, también puede usarse con autocommit.
Ninguno de estos métodos confirma ni cierra la conexión.

Antes de confirmar se guarda el borrador y se conserva el objeto devuelto con los
IDs de líneas. El servicio registra en el modelo los descuentos efectivamente
realizados por inventario, llama a `confirmar()` y a `actualizar(pedido, c)`, todo
con la misma conexión. Para cobrar se inserta primero el recibo con `ReciboDAO`, se
registra en el modelo el recibo con su ID generado y luego se llama a `actualizar`.
La fecha del recibo se guarda con microsegundos, conforme a `DATETIME(6)`.
Después se hace un único `commit`; cualquier fallo exige `rollback` y descartar los
objetos modificados. Cancelar conserva los descuentos históricos; el servicio
coordina su restitución al inventario una sola vez antes de actualizar el estado.

`lineaPedido.reconstruirPersistida(...)` restaura nombres y precios históricos junto
con `getProductoId()` y `getOpcionIds()`. Las referencias se conservan aunque cambie
el catálogo. En líneas recuperadas, `getProducto()` devuelve `null` y `getExtras()`
está vacío porque no se cargan objetos de catálogo; las opciones seleccionadas se
consultan por sus IDs. El futuro servicio debe resolver esos IDs con catálogo para
obtener recetas actuales al confirmar, sin recalcular los precios históricos.

## Cobro y caché de pedidos

El arranque puede componer estas dependencias sin mantener conexiones abiertas:

```java
ConexionDB bd = new ConexionDB();
PedidoRepository repositorio = new PedidoRepository(bd);
CobroService cobros = new CobroService(repositorio, new ReciboDAO(), bd);
```

`CobroService` pertenece a `logic.service.pedidos`. Su método es
`cobrar(long id, MetodoPago metodo, String usuario)` y propaga `SQLException` para
que la futura interfaz presente los errores. Ya no recibe un `PedidoDAO` en el
constructor: lo crea dentro de cada cobro con la conexión nueva de `ConexionDB`.

El cobro carga el pedido desde SQL con bloqueo, comprueba que esté confirmado,
sin cancelar y pendiente de pago, calcula el total histórico, inserta el recibo,
actualiza el estado a `PAGADO` y hace un único `commit`. Si algo falla antes de
confirmar, hace `rollback`. La fecha del pago corresponde al cobro actual.
Después del commit actualiza la caché y cierra la conexión. No depende de que el
pedido ya esté en memoria ni utiliza el total posiblemente atrasado de la caché.

`PedidoRepository.recargarActivos()` abre y cierra su propia conexión, confirma
la lectura y reemplaza la caché completa solo si tiene éxito. Propaga
`SQLException`; si falla, conserva los pedidos que ya tenía.
`obtenerPedidoPorId()` y `listarPedidosActivos()` consultan únicamente memoria y
devuelven copias para que una edición externa no cambie el estado almacenado.
`actualizarTrasCommit()` tampoco ejecuta SQL: actualiza o retira el pedido del mapa
según sus estados. La caché es local a cada ejecución; los cambios de otros equipos
se obtienen al recargar y las operaciones críticas vuelven a consultar MySQL.

## Compilación

El proyecto usa Maven Wrapper, Java release 21 y JavaFX 21.0.6. Configurar `JAVA_HOME` hacia un JDK compatible y ejecutar `mvnw.cmd clean verify` en Windows o `./mvnw clean verify` en otros sistemas. La aplicación se inicia con `mvnw.cmd javafx:run`.

El módulo Java y la clase de arranque mantienen los nombres configurados en `pom.xml`.

## Pruebas de modelos

El proyecto configura Surefire 3.5.4 para ejecutar las pruebas JUnit 5. Mientras los
DAO estén comentados y su servicio siga importándolos, utiliza el perfil de modelos:

```powershell
.\mvnw.cmd -Ppruebas-modelos clean test
```

Este perfil compila y prueba el dominio (`model`), los DAO, la conexión, el
repositorio y el servicio de cobro de pedidos, sin compilar
el servicio de inventario pendiente ni la aplicación JavaFX. Por defecto no requiere
MySQL; las pruebas JDBC se activan con `-Dpruebas.mysql=true`. Usa el `pom.xml` principal;
no necesita archivos de configuración temporales. Si no encuentra pruebas, falla
para evitar reportar éxito sin haber ejecutado ninguna.

En IntelliJ, abre el panel Maven y pulsa **Reload All Maven Projects** para que
cargue las dependencias JUnit de alcance `test`. Si no aparece el panel Maven,
haz clic derecho en `pom.xml` y selecciona **Add as Maven Project**. Verifica que
`src/test/java` esté marcado como **Test Sources Root**. Después activa el perfil
`pruebas-modelos` y ejecuta el objetivo `test` desde el panel Maven.
Esta configuración no verifica el arranque JavaFX. Al activar las pruebas JDBC,
también comprueba el guardado y la reversión de pedidos usando tablas temporales
de la sesión, sin escribir datos en las tablas permanentes del servidor.

La compilación completa sigue siendo `mvnw.cmd clean verify`; requiere que las
clases DAO importadas por el servicio existan, aunque solo sean esqueletos. Para
ejecutar la aplicación, desactiva el perfil de modelos y recompila el proyecto.

Si Maven no encuentra Java, configura `JAVA_HOME` con la carpeta de un JDK 21 o
superior. El wrapper Windows ejecuta Java en una sola línea para evitar errores
al interpretar variables opcionales vacías junto a continuaciones de línea.
