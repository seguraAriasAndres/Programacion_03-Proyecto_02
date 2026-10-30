# Proyecto 02 - Programación III (UNA)

Aplicación de escritorio para operar un restaurante. Está construida con Java 21, JavaFX, Maven y MySQL; cubre catálogo, inventario, pedidos, cobros, recibos, reportes y acceso por roles.

## Funciones principales

- **Seguridad:** al abrir el sistema aparece el inicio de sesión, sin elegir un rol. El menú muestra los módulos permitidos según el rol guardado de la cuenta; cerrar sesión vuelve al login. Administración, inventario y caja requieren cuenta y contraseña. El administrador crea usuarios y asigna sus roles desde el menú Usuarios.
- **Catálogo:** categorías, productos, opciones, recetas y consulta de disponibilidad según existencias. En Extras y acompañamientos se crean opciones con ingredientes o vinculadas a la receta vigente de otro producto.
- **Inventario:** ingredientes, movimientos de entrada y merma, consumo por venta y alertas de stock mínimo.
- **Pedidos y cobros:** borradores, confirmación con descuento de existencias, preparación, entrega, cancelación con restitución de existencias y recibo de pago.
- **Reportes:** ventas pagadas por periodo, ingresos por categoría y productos más vendidos.

## Estructura

Nuestro árbol muestra las carpetas que concentran el código, las vistas, la configuración y las pruebas. Omite metadatos del IDE, artefactos compilados y archivos internos de Git.

~~~text
.
├── config/
│   ├── aiven-ca.pem
│   └── database.properties.example
├── src/
│   ├── main/
│   │   ├── java/cr/ac/una/est/programacion03proyecto02/
│   │   │   ├── app/             # inicio y composición de servicios
│   │   │   ├── controller/      # pantallas por módulo
│   │   │   ├── logic/
│   │   │   │   ├── model/       # entidades y reglas de dominio
│   │   │   │   ├── repository/  # pedidos activos en memoria
│   │   │   │   └── service/     # reglas y transacciones
│   │   │   ├── persistence/
│   │   │   │   ├── connection/  # conexión MySQL
│   │   │   │   └── dao/         # consultas y persistencia
│   │   │   └── view/            # navegación, iconos y clases de personalización
│   │   └── resources/
│   │       └── cr/ac/una/est/programacion03proyecto02/view/
│   │       │   ├── catalogo/
│   │       │   ├── inventario/
│   │       │   ├── pedidos/
│   │       │   ├── reportes/
│   │       │   ├── seguridad/   # FXML
│   │       │   └── personalizacion/
│   │       │       ├── app-frio.css
│   │       │       ├── app-calido.css
│   │       │       └── parada-del-puente/
│   │       │           ├── app-personalizado.css
│   │       │           └── iconos/ # PNG de 16, 32, 64 y 256 px
│   └── test/java/cr/ac/una/est/programacion03proyecto02/
│       ├── controller/          # flujos JavaFX
│       ├── logic/               # modelos, servicios y repositorio
│       ├── persistence/         # conexión y DAO MySQL
│       └── view/                # contrato entre FXML y controladores
├── .mvn/wrapper/
├── mvnw
├── mvnw.cmd
└── pom.xml
~~~

Paquete base: cr.ac.una.est.programacion03proyecto02. Las capas controller, logic, persistence y view están al mismo nivel.

## Preparar y ejecutar

1. JDK 21 o superior.
2. Desde la raíz del proyecto, copia la plantilla de conexión y completa las credenciales de MySQL:

   ~~~powershell
   Copy-Item config/database.properties.example config/database.local.properties
   ~~~

   El archivo local está excluido de Git; no compartir credenciales.
3. Las tablas del esquema compartido `restaurante` se crean y actualizan directamente desde MySQL Workbench en Aiven. La aplicación utiliza esas tablas existentes; no ejecuta archivos de creación o actualización del esquema al arrancar.
4. Inicia la aplicación:

   ~~~powershell
   .\mvnw.cmd javafx:run
   ~~~

Maven descarga HikariCP para reutilizar hasta tres conexiones MySQL. Cada operación sigue usando su propia conexión y transacción; al cerrar la aplicación se cierra el pool. Las pruebas lo desactivan para aislar las tablas temporales de cada caso.

### Conexión en otro equipo

La conexión verifica el certificado TLS de Aiven. Además de completar el archivo local de propiedades, prepara su almacén de certificados si aún no existe:

~~~powershell
keytool -importcert -alias aiven-ca -file config/aiven-ca.pem -keystore config/aiven-truststore.p12 -storetype PKCS12 -storepass changeit
~~~

Comprueba el certificado antes de confirmar su importación. El almacén contiene la CA pública; `changeit` no es la contraseña de MySQL. Ejecuta desde la raíz del proyecto para que se resuelvan las rutas de `config/`. Si Aiven restringe el acceso por IP, autoriza la IP del equipo correspondiente.

Workbench y Java son clientes del mismo servidor: Workbench no necesita estar abierto para usar la aplicación. Los cambios confirmados en Workbench se guardan en Aiven; Git comparte el código del proyecto. Los archivos de credenciales, almacenes de certificados y respaldos locales de `config/` están excluidos de Git.

### Estructura utilizada por los extras

Al administrar las tablas en Workbench, conserva los campos que utiliza el código actual:

| Tabla | Campo | Función |
|---|---|---|
| `opcion_producto` | `producto_origen_id`, BIGINT nullable | Referencia al producto cuya receta utiliza el extra. |
| `opcion_producto` | `eliminada`, BOOLEAN con valor inicial FALSE | Retira el extra del catálogo y conserva sus referencias históricas. |
| `linea_pedido_opcion` | `cantidad`, INT positivo con valor inicial 1 | Cantidad del extra por unidad del producto de la línea. |

Estos campos ya existen en el servidor compartido. El servicio coordina la conexión y el commit o rollback de cada operación; los DAO reciben esa misma conexión cuando participan en una transacción compartida.

En Tomar pedido, agregar o quitar una línea guarda el cambio inmediatamente.
**Guardar borrador y salir** permite continuar más tarde sin confirmar ni descontar inventario.
Los extras activos sin receta aparecen con un aviso y se habilitan al configurar sus ingredientes y cantidades en el catálogo.
Los extras se eligen con casillas independientes, sin Ctrl. Cada uno tiene su cantidad por unidad del producto: dos productos con dos huevos cada uno cobran y descuentan cuatro huevos. La consulta de stock se realiza aparte y permite volver o guardar el borrador mientras espera respuesta.

## Personalización

El botón de diseño recorre **Frío → Cálido → Personalizado → Frío** en todas las pantallas.
Los CSS y las imágenes viven en `src/main/resources/cr/ac/una/est/programacion03proyecto02/view/personalizacion/`.
`app-frio.css` contiene los estilos comunes; el cálido o el personalizado de `parada-del-puente/` se añaden por separado.
Las clases `BotonTema`, `TemaVisual` y `MarcaVentana` se encuentran en el paquete Java `view.personalizacion`.
El ámbito `JavaFX CSS` de `.idea/scopes/` limita a esta carpeta un ajuste del perfil de inspecciones: evita que la inspección de pseudoestados web rechace estados válidos de JavaFX y permite sus propiedades con prefijo `-fx-`. El resto del proyecto conserva las inspecciones habituales. Esto permite trabajar con IntelliJ sin el módulo Ultimate de JavaFX CSS; las pruebas validan los temas con el parser y los controles reales de JavaFX.
Si el proyecto ya estaba abierto al recibir esta configuración, vuelve a abrirlo para cargar el perfil `Project Default` y el ámbito de JavaFX.
Cambiar el diseño conserva los formularios y la pestaña seleccionada. La selección dura hasta cerrar la aplicación.

El icono de ventana se configura aparte: por defecto es neutral. La propiedad de JVM
`-Drestaurante.marca=parada-del-puente` activa los cuatro tamaños PNG incluidos en `view/personalizacion/parada-del-puente/iconos/`;
`-Drestaurante.marca=neutral` mantiene el icono predeterminado. Cambiar el tema no cambia esa marca.

## Pruebas

- Pruebas de modelos y servicios sin MySQL:

  ~~~powershell
  .\mvnw.cmd -Ppruebas-modelos test
  ~~~

- Pruebas de persistencia contra MySQL. Requieren config/database.local.properties; preparan tablas temporales para sus casos:

  ~~~powershell
  .\mvnw.cmd -Ppruebas-modelos test -Dpruebas.mysql=true
  ~~~

- Pruebas de carga FXML y flujos JavaFX:

  ~~~powershell
  .\mvnw.cmd test -Dpruebas.javafx=true -Djava.awt.headless=false
  ~~~
