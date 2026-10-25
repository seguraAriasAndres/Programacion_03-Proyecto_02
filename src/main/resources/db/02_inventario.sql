-- Requiere que exista restaurante.pedido, porque movimiento_stock
-- guarda el pedido al que pertenece un consumo o una devolución.
USE restaurante;

CREATE TABLE IF NOT EXISTS ingrediente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    cantidad_disponible DECIMAL(15,6) NOT NULL DEFAULT 0.000000,
    unidad_medida ENUM('UNIDAD', 'GRAMO', 'MILILITRO') NOT NULL,
    stock_minimo DECIMAL(15,6) NOT NULL DEFAULT 0.000000,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ingrediente_nombre (nombre),
    CONSTRAINT chk_ingrediente_cantidad_disponible
        CHECK (cantidad_disponible >= 0),
    CONSTRAINT chk_ingrediente_stock_minimo
        CHECK (stock_minimo >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS movimiento_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ingrediente_id BIGINT NOT NULL,
    pedido_id BIGINT NULL,
    tipo_movimiento ENUM(
        'REABASTECIMIENTO',
        'MERMA',
        'CONSUMO_VENTA',
        'DEVOLUCION_VENTA'
    ) NOT NULL,
    cantidad DECIMAL(15,6) NOT NULL,
    fecha DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    usuario VARCHAR(100) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_mov_stock_ingrediente_fecha (ingrediente_id, fecha),
    UNIQUE KEY uq_mov_stock_pedido_ingrediente_tipo
        (pedido_id, ingrediente_id, tipo_movimiento),
    CONSTRAINT chk_mov_stock_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_mov_stock_ingrediente
        FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id),
    CONSTRAINT fk_mov_stock_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido(id)
) ENGINE=InnoDB;
