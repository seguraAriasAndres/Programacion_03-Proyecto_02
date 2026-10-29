-- Alta inicial de los usuarios administrativos del sistema.
-- Ejecutar DESPUES de 01_seguridad.sql. No elimina datos existentes.

USE restaurante;

-- Claves semilla: admin -> admin1234 y stock -> stock1234.
-- Para otra clave, corra el main de
-- logic.service.seguridad.GeneradorHash con la clave como argumento
-- y pegue la salida aqui.
INSERT IGNORE INTO usuario (nombre_usuario, hash, rol, activo) VALUES
    ('admin', '3e7c90076fa7c84e17eece229a6df59f:7a1c73a3a5ff22a4f86d9ad9e322ee7f238de68ef0e0711c21ac254079a22c3f', 'ADMIN', TRUE),
    ('stock', 'b97403641f766526eaeef3b96315fef1:432dd43549ad9fc7680d669beb108b5a24e3722eef57e77f4f6324491cf90651', 'STOCK_MANAGER', TRUE);
