# Proyecto 02 - Programación III (UNA)

Aplicación de escritorio para operar un restaurante. Está construida con Java 21, JavaFX, Maven y MySQL; cubre catálogo, inventario, pedidos, cobros, recibos, reportes y acceso por roles.

## Funciones principales

- **Seguridad:** acceso de administración y de inventario con cuenta; el cajero inicia con su nombre.
- **Catálogo:** categorías, productos, opciones, recetas y consulta de disponibilidad según existencias.
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
│   │   │   └── view/            # navegación e iconos
│   │   └── resources/
│   │       ├── cr/ac/una/est/programacion03proyecto02/view/
│   │       │   ├── catalogo/
│   │       │   ├── inventario/
│   │       │   ├── pedidos/
│   │       │   ├── reportes/
│   │       │   ├── seguridad/   # FXML
│   │       │   ├── app.css
│   │       │   └── app-calido.css
│   │       └── db/               # scripts SQL y guía de conexión
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
3. Crea el esquema y ejecuta los scripts SQL que correspondan en src/main/resources/db.
4. Inicia la aplicación:

   ~~~powershell
   .\mvnw.cmd javafx:run
   ~~~

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
