-- Ejecutar una vez en el schema restaurante desde MySQL Workbench,
-- con la misma conexion administrativa usada para 00_crear_base.sql.
-- No elimina datos existentes.

USE restaurante;

-- hash guarda SHA-256 con sal en texto hexadecimal (sal:hash).
CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_usuario VARCHAR(100) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuario_nombre (nombre_usuario)
) ENGINE=InnoDB;

-- registra cada intento de login, tambien los fallidos y de usuarios inexistentes,
-- por eso no tiene foreign key: el nombre se guarda tal cual llego.
CREATE TABLE IF NOT EXISTS auditoria_login (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_usuario VARCHAR(100) NOT NULL,
    exitoso BOOLEAN NOT NULL,
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'restaurante' AND TABLE_NAME IN ('usuario', 'auditoria_login');
