# MySQL compartido en Aiven

El equipo utiliza una instancia MySQL en Aiven. Workbench y la aplicacion Java
son clientes independientes; Workbench no necesita estar abierto para usar JDBC.
Las operaciones ejecutadas sobre la conexion de Aiven se guardan en ese servidor
al confirmarse la transaccion. Git comparte codigo y scripts, no datos de MySQL.

## Configuracion preparada

- Driver Maven: `com.mysql:mysql-connector-j:26.7.0`.
- Modulos Java: `java.sql` y `mysql.connector.j`. Este ultimo es el nombre real
  comprobado con `jar --describe-module`; `com.mysql.cj` es un paquete del driver.
- Host: `restaurante-mysql-restaurante1234.a.aivencloud.com`.
- Puerto: `10952`.
- Schema actual: `restaurante`.
- TLS: `sslMode=VERIFY_IDENTITY`, con la CA de Aiven en un truststore PKCS12.
- Configuracion externa: `config/database.local.properties`.

`config/` vive en la raiz del proyecto, fuera de `src/main/resources`. Las
credenciales no se incluyen en el JAR. `persistence.connection.ConexionDB` lee
ese archivo con `Properties.load` y devuelve una conexion nueva por llamada.
`ReciboDAO` recibe una conexion del servicio.

## Preparar otro equipo

1. Instala un JDK compatible con Java 21 y abre el proyecto como proyecto Maven.
2. En IntelliJ pulsa **Reload All Maven Projects**.
3. Desde la raiz del proyecto, copia la plantilla:

   ```powershell
   Copy-Item config/database.properties.example config/database.local.properties
   ```

4. Edita `user` y `password` en la copia local, usando tu usuario de MySQL en Aiven.
   No sobrescribas un archivo local que ya contenga tu configuracion.
5. Genera el truststore usando la CA publica incluida:

   ```powershell
   keytool -importcert -alias aiven-ca -file config/aiven-ca.pem -keystore config/aiven-truststore.p12 -storetype PKCS12 -storepass changeit
   ```

   Si `keytool` no esta en PATH, ejecutalo desde `bin` de tu JDK. Comprueba el
   certificado y confirma su importacion. No repitas la importacion si ya existe
   el alias, salvo que estes reemplazando una CA que haya cambiado en Aiven.
   `changeit` protege un almacen que contiene solo un certificado publico,
   no es la contrasena de MySQL ni contiene una clave privada.
6. Si se ha restringido el acceso por IP en Aiven, agrega tu IP publica autorizada.

La plantilla y `config/aiven-ca.pem` se comparten en Git. El archivo
`*.local.properties` y los almacenes `*.p12` / `*.jks` de `config/` estan ignorados.
No compartas capturas, archivos o configuraciones de ejecucion con contrasenas.

## Contrato de ConexionDB

`ConexionDB` lee el archivo local con `Properties.load` y extrae la propiedad
`url`. Las demas propiedades del archivo son propiedades JDBC que se pasan a
`DriverManager.getConnection(url, properties)`. La propiedad `url` se retira de
ese objeto antes de pasarlo al driver.

Ejecuta desde la raiz del proyecto para que `file:config/aiven-truststore.p12`
resuelva correctamente. `ConexionDB` permite configurar una ruta absoluta en
otros entornos cambiando esa propiedad en el archivo local. No hace falta
definir VM options globales de TLS:
el truststore ya se indica mediante propiedades de Connector/J.

No hace falta `Class.forName`: el driver se registra mediante el proveedor JDBC
declarado en su JAR. Cada conexion debe cerrarse cuando termine su propietario;
no se compartira una unica conexion estatica entre toda la aplicacion.

Las operaciones coordinadas de pedidos, inventario y cobro deben compartir una
conexion y transaccion. El servicio coordina `commit` / `rollback`; los DAO que
reciban esa conexion no deben cerrarla ni confirmar la transaccion por su cuenta.

## Schema y tablas del proyecto

El equipo creo el schema `restaurante` y la tabla `Recibo` en Aiven. La URL JDBC
local y la plantilla apuntan a `/restaurante`. `defaultdb` sigue siendo la base
inicial del proveedor y no se utiliza para las tablas del proyecto.
Todos los modulos utilizaran el mismo schema `restaurante`.

| Modulo | Tablas previstas en el alcance |
|---|---|
| Seguridad | usuario, auditoria_login (`01_seguridad.sql`) |
| Catalogo | categoria, producto, opcion_producto, receta_item |
| Inventario | ingrediente, movimiento_stock |
| Pedidos y cobro | pedido, linea_pedido, ingredientes_descontados, recibo |

El alcance guarda los datos de Pago en recibo; no se agrega otra tabla sin acuerdo.
Ya existen en Aiven las tablas de seguridad (`01_seguridad.sql`) y las de pedidos
y cobro; catalogo e inventario siguen pendientes de que su modulo ejecute su script.

`00_crear_base.sql` es una alternativa manual para crear `restaurante`. No se
ejecuta al compilar o iniciar la aplicacion; puedes crear el schema en Workbench.
Esta configuracion tampoco crea, modifica ni elimina tablas o registros.

## Comprobaciones

Las pruebas de modelos siguen ejecutandose sin base de datos:

```powershell
.\mvnw.cmd -Ppruebas-modelos test
```

Con `config/database.local.properties` configurado, las pruebas contra Aiven se
activan con la propiedad de sistema `pruebas.mysql`:

```powershell
.\mvnw.cmd -Ppruebas-modelos test "-Dpruebas.mysql=true"
```

La compilacion completa sigue pendiente de los DAO actualmente comentados que el
servicio de inventario importa. Configurar MySQL no implementa esas clases.

## Referencias

- [Aiven y Java](https://aiven.io/docs/products/mysql/howto/connect-with-java)
- [Verificar el certificado con Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-server-authentication.html)
- [Propiedades TLS de Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html)
