-- Ejecutar después de 02_inventario.sql, porque receta_item
-- referencia ingredientes.
USE restaurante;

CREATE TABLE IF NOT EXISTS categoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS producto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    precio_base DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    categoria_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_producto_categoria_activo (categoria_id, activo),
    CONSTRAINT chk_producto_precio_base CHECK (precio_base >= 0),
    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS opcion_producto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    producto_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    tipo_opcion ENUM('ACOMPANAMIENTO', 'EXTRA') NOT NULL,
    precio_adicional DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_opcion_producto_activa (producto_id, activa),
    CONSTRAINT chk_opcion_precio_adicional
        CHECK (precio_adicional >= 0),
    CONSTRAINT fk_opcion_producto
        FOREIGN KEY (producto_id) REFERENCES producto(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS receta_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    producto_id BIGINT NULL,
    opcion_producto_id BIGINT NULL,
    ingrediente_id BIGINT NOT NULL,
    cantidad_requerida DECIMAL(15,6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_receta_producto_ingrediente
        (producto_id, ingrediente_id),
    UNIQUE KEY uq_receta_opcion_ingrediente
        (opcion_producto_id, ingrediente_id),
    KEY idx_receta_ingrediente (ingrediente_id),
    CONSTRAINT chk_receta_item_un_solo_padre CHECK (
        (producto_id IS NOT NULL AND opcion_producto_id IS NULL)
        OR
        (producto_id IS NULL AND opcion_producto_id IS NOT NULL)
    ),
    CONSTRAINT chk_receta_item_cantidad CHECK (cantidad_requerida > 0),
    CONSTRAINT fk_receta_producto
        FOREIGN KEY (producto_id) REFERENCES producto(id),
    CONSTRAINT fk_receta_opcion
        FOREIGN KEY (opcion_producto_id) REFERENCES opcion_producto(id),
    CONSTRAINT fk_receta_ingrediente
        FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id)
) ENGINE=InnoDB;
