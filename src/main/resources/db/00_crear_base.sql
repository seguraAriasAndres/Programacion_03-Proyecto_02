-- Ejecutar una vez desde una conexion administrativa en MySQL Workbench.
-- Todos los modulos del proyecto comparten este schema.
-- Este script no elimina datos ni crea usuarios o contrasenas.

CREATE DATABASE IF NOT EXISTS restaurante
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE restaurante;

SELECT DATABASE() AS base_actual;
