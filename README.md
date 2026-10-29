# Proyecto 02 - Programación III (UNA)

Aplicación de restaurante: JavaFX 21 + MySQL (Aiven) + Maven. Cobertura de catálogo,
inventario, pedidos, cobro con recibo, reportes y login por roles.

## Árbol del proyecto

```text
├── pom.xml
└── src/
    ├── main/
    │   ├── java/cr/ac/una/est/programacion03proyecto02/
    │   │   ├── app/                    # HelloApplication (composición) y Launcher
    │   │   ├── logic/
    │   │   │   ├── model/              # catalogo, inventario, pedidos, seguridad, reportes
    │   │   │   ├── service/            # reglas de negocio y transacciones
    │   │   │   └── repository/         # caché de pedidos activos
    │   │   ├── persistence/
    │   │   │   ├── dao/                # SQL por módulo
    │   │   │   └── connection/         # ConexionDB (lee config/database.local.properties)
    │   │   ├── controller/             # seguridad, catalogo, inventario, pedidos, reportes
    │   │   └── view/navigation/        # NavegadorVistas: carga FXML, roles y estilos
    │   └── resources/
    │       ├── cr/ac/una/est/programacion03proyecto02/view/
    │       │   ├── app.css             # tema neutro base, adjuntado a cada escena
    │       │   ├── seguridad/          # inicio, login, menu.fxml
    │       │   ├── catalogo/           # categorias, productos, producto-editor.fxml
    │       │   ├── inventario/         # inventario.fxml
    │       │   ├── pedidos/            # pedidos_activos, toma_pedido, Cobro, recibo.fxml
    │       │   └── reportes/           # reportes.fxml
    │       └── db/                     # scripts SQL 00–04 y guía de conexión
    └── test/                           # misma estructura de paquetes que main
```

Paquete base: `cr.ac.una.est.programacion03proyecto02`. `controller` está al mismo
nivel que `logic` y `persistence`; los FXML y `app.css` viven en `resources` bajo el
mismo paquete base.

## Cómo correr

1. JDK 21 o superior y abrir el proyecto en IntelliJ (Reload All Maven Projects).
2. Copiar la configuración: `Copy-Item config/database.properties.example config/database.local.properties`
   y editar credenciales de Aiven (detalles en [db/README.md](src/main/resources/db/README.md)).
3. Ejecutar los scripts SQL de `src/main/resources/db` en orden: `00` → `04`
   (son `CREATE TABLE IF NOT EXISTS`; no borran datos).
4. Arrancar: `mvnw.cmd javafx:run`.

## Accesos

| Rol | Pantalla | Usuario y clave |
|---|---|---|
| ADMIN | catálogo, reportes e inventario | `admin` / `admin1234` |
| STOCK_MANAGER | inventario | `stock` / `stock1234` |
| CAJERO | pedidos, cobro y recibo | solo el nombre, sin clave |

Claves semilla de `04_usuarios.sql`: Para una clave
nueva, correr el main de `logic.service.seguridad.GeneradorHash` y hacer un
`UPDATE` en la tabla `usuario`.

## Pruebas

- `mvnw.cmd test` - modelos y servicios sin base de datos.
- Con MySQL configurado: `mvnw.cmd test "-Dpruebas.mysql=true"` - JDBC con tablas temporales.
- Con pantalla: `mvnw.cmd test "-Dpruebas.javafx=true" "-Djava.awt.headless=false"` -
  carga real de FXML (incluye `ContratoFxmlTest`, que valida fx:id y eventos de cada
  vista contra su controlador).