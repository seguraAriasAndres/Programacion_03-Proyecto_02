package cr.ac.una.est.programacion03proyecto02.persistence.dao.catalogo;

import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.*;
import cr.ac.una.est.programacion03proyecto02.logic.model.catalogo.enums.TipoOpcion;
import cr.ac.una.est.programacion03proyecto02.logic.model.inventario.recetaItem;
import cr.ac.una.est.programacion03proyecto02.logic.service.inventario.inventarioService;
import cr.ac.una.est.programacion03proyecto02.persistence.connection.ConexionDB;
import cr.ac.una.est.programacion03proyecto02.persistence.dao.inventario.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Todas las escrituras usan exclusivamente tablas TEMPORARY de esta sesión. */
@EnabledIfSystemProperty(named="pruebas.mysql", matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogoInventarioMySQLTest {
    private Connection conexion;
    private final productoDAO productos = new productoDAO();

    @BeforeAll void preparar() throws Exception {
        Properties config = new Properties();
        try (var archivo = Files.newInputStream(Path.of("config/database.local.properties"))) { config.load(archivo); }
        String url = (String)config.remove("url");
        conexion = DriverManager.getConnection(url,config);
        // Crear TODAS las tablas antes de modificar cualquiera evita tocar tablas reales.
        try(var s=conexion.createStatement()) {
            s.executeUpdate("CREATE TEMPORARY TABLE categoria(id BIGINT PRIMARY KEY AUTO_INCREMENT,nombre VARCHAR(100),activa BOOLEAN) ENGINE=InnoDB");
            s.executeUpdate("CREATE TEMPORARY TABLE producto(id BIGINT PRIMARY KEY AUTO_INCREMENT,nombre VARCHAR(150),precio_base DECIMAL(15,2),categoria_id BIGINT,activo BOOLEAN) ENGINE=InnoDB AUTO_INCREMENT=3000000001");
            s.executeUpdate("CREATE TEMPORARY TABLE opcion_producto(id BIGINT PRIMARY KEY AUTO_INCREMENT,producto_id BIGINT,nombre VARCHAR(150),tipo_opcion VARCHAR(30),precio_adicional DECIMAL(15,2),activa BOOLEAN) ENGINE=InnoDB AUTO_INCREMENT=4000000001");
            s.executeUpdate("CREATE TEMPORARY TABLE receta_item(id BIGINT PRIMARY KEY AUTO_INCREMENT,producto_id BIGINT NULL,opcion_producto_id BIGINT NULL,ingrediente_id BIGINT,cantidad_requerida DECIMAL(15,6)) ENGINE=InnoDB");
            s.executeUpdate("CREATE TEMPORARY TABLE ingrediente(id BIGINT PRIMARY KEY,nombre VARCHAR(100),cantidad_disponible DECIMAL(15,6),unidad_medida VARCHAR(20),stock_minimo DECIMAL(15,6),activo BOOLEAN) ENGINE=InnoDB");
            s.executeUpdate("CREATE TEMPORARY TABLE movimiento_stock(id BIGINT PRIMARY KEY AUTO_INCREMENT,ingrediente_id BIGINT,pedido_id BIGINT NULL,tipo_movimiento VARCHAR(30),cantidad DECIMAL(15,6),fecha DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),usuario VARCHAR(100),motivo VARCHAR(255)) ENGINE=InnoDB");
            s.executeUpdate("INSERT INTO categoria VALUES(1,'Categoria',TRUE)");
            s.executeUpdate("INSERT INTO ingrediente VALUES(1,'Harina',10,'GRAMO',1,TRUE),(2,'Queso',10,'GRAMO',1,TRUE)");
        }
        conexion.setAutoCommit(false);
    }
    @AfterEach void revertir() throws Exception { if(conexion!=null) conexion.rollback(); }
    @AfterAll void cerrar() throws Exception { if(conexion!=null) conexion.close(); }

    private Producto nuevo() {
        Producto p = new Producto(0,"Hamburguesa",new BigDecimal("2500.50"),new Categoria(1,"Categoria",true),true);
        p.setReceta(List.of(new recetaItem(1,.3)));
        OpcionProducto o = new OpcionProducto(0,"Queso",TipoOpcion.EXTRA,new BigDecimal("100.25"),false);
        o.setReceta(List.of(new recetaItem(2,.5)));
        p.setOpciones(List.of(o));
        return p;
    }

    @Test void guardaYRecuperaRecetasOpcionesInactivasYLongSinMutarAntesDelCommit() throws Exception {
        Producto original = nuevo();
        Producto guardado = productos.guardarConRecetas(original,conexion);
        assertTrue(guardado.getId()>Integer.MAX_VALUE);
        assertTrue(guardado.getOpciones().getFirst().getId()>Integer.MAX_VALUE);
        assertEquals(0,original.getId());
        assertEquals(0,original.getOpciones().getFirst().getId());
        Producto leido=productos.buscar(guardado.getId(),conexion);
        assertEquals(new BigDecimal("2500.50"),leido.getPrecioBase());
        assertEquals(.3,leido.getReceta().getFirst().getCantidadRequerida());
        assertFalse(leido.getOpciones().getFirst().isActiva());
        leido.getOpciones().getFirst().getReceta().add(new recetaItem(1,.1));
        productos.guardarConRecetas(leido,conexion);
        assertEquals(2,productos.buscar(leido.getId(),conexion).getOpciones().getFirst().getReceta().size());
        conexion.rollback();
        assertNull(productos.buscar(guardado.getId(),conexion));
    }

    @Test void rechazaOpcionDeOtroProductoYPermiteDesactivarIdLong() throws Exception {
        Producto a=productos.guardarConRecetas(nuevo(),conexion);
        Producto b=productos.guardarConRecetas(nuevo(),conexion);
        a.setOpciones(b.getOpciones());
        assertThrows(SQLException.class,()->productos.guardarConRecetas(a,conexion));
        conexion.rollback();
        Producto p=productos.guardarConRecetas(nuevo(),conexion);
        productos.desactivar(p.getId(),conexion);
        assertFalse(productos.buscar(p.getId(),conexion).isActivo());
    }

    @Test void inventarioComparteTransaccionYRollbackRevierteStockYAuditoria() throws Exception {
        ConexionDB proveedor = new ConexionDB() { public Connection getConnection() { throw new AssertionError("Abrio otra conexion"); } };
        inventarioService servicio = new inventarioService(proveedor,new ingredienteDAO(),new movimientoStockDAO());
        servicio.descontar(Map.of(1L,.3),3000000001L,"cajero",conexion);
        assertEquals(new BigDecimal("9.700000"),stock());
        servicio.restituir(Map.of(1L,.3),3000000001L,"cajero",conexion);
        assertEquals(new BigDecimal("10.000000"),stock());
        try(var s=conexion.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM movimiento_stock")) {r.next();assertEquals(2,r.getInt(1));}
        conexion.rollback();
        assertEquals(new BigDecimal("10.000000"),stock());
        try(var s=conexion.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM movimiento_stock")) {r.next();assertEquals(0,r.getInt(1));}
        assertThrows(IllegalStateException.class,()->new ingredienteDAO().aplicarVariacion(1,-11,conexion));
        assertEquals(new BigDecimal("10.000000"),stock());
        servicio.descontar(Map.of(),3000000001L,"cajero",conexion);
    }
    private BigDecimal stock() throws Exception {
        try(var s=conexion.createStatement();var r=s.executeQuery("SELECT cantidad_disponible FROM ingrediente WHERE id=1")) {r.next();return r.getBigDecimal(1);}
    }
}
