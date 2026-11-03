-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: restaurante-mysql-restaurante1234.a.aivencloud.com    Database: restaurante
-- ------------------------------------------------------
-- Server version	8.4.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '8c0ded80-cd11-11f1-8b3d-6244f4941306:1-430';

--
-- Table structure for table `linea_pedido`
--

DROP TABLE IF EXISTS `linea_pedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `linea_pedido` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pedido_id` bigint NOT NULL,
  `producto_id` bigint NOT NULL,
  `nombre_producto` varchar(150) NOT NULL,
  `nombre_categoria` varchar(100) NOT NULL,
  `descripcion_opciones` varchar(1000) NOT NULL DEFAULT ' ',
  `precio_unitario` decimal(15,2) unsigned NOT NULL,
  `cantidad` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_linea_pedido_pedido_idx` (`pedido_id`),
  CONSTRAINT `fk_linea_pedido_pedido` FOREIGN KEY (`pedido_id`) REFERENCES `pedido` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `linea_pedido`
--

LOCK TABLES `linea_pedido` WRITE;
/*!40000 ALTER TABLE `linea_pedido` DISABLE KEYS */;
INSERT INTO `linea_pedido` VALUES (8,4,4,'gallo pinto','Almuerzo','huevos, carne de res',5500.00,2),(9,5,6,'Lengua en Salsa','Almuerzo','',4500.00,2),(10,5,5,'arroz con atun','Almuerzo','',4000.00,1),(11,6,4,'gallo pinto','Almuerzo','',4500.00,3),(12,7,5,'arroz con atun','Almuerzo','',4000.00,2),(13,7,4,'gallo pinto','Almuerzo','',4500.00,3),(14,7,6,'Lengua en Salsa','Almuerzo','',4500.00,1),(15,8,7,'choripan','Almuerzo','',2500.00,1),(16,7,4,'gallo pinto','Almuerzo','',4500.00,1),(17,9,4,'Gallo pinto','Almuerzo','huevos, carne de res',5500.00,4),(18,9,6,'Lengua en Salsa','Almuerzo','',4500.00,1),(19,10,4,'Gallo pinto','Almuerzo','huevos',5000.00,1),(20,11,8,'Cantonés Taper Verde','Almuerzo','Bañado en mayonesa',2200.00,1),(21,12,8,'Cantonés Taper Verde','Almuerzo','Bañado en mayonesa',2200.00,1),(22,13,8,'Cantonés Taper Verde','Almuerzo','Bañado en mayonesa',2200.00,1),(23,14,8,'Cantonés Taper Verde','Almuerzo','Bañado en mayonesa',2200.00,1),(24,15,5,'Arroz con atun','Almuerzo','frijoles',4500.00,1),(25,16,9,'Agua','Bebidas','',600.00,2),(26,16,5,'Arroz con atun','Comida','',4000.00,1),(27,16,4,'Gallo pinto','Comida','',4500.00,1),(28,16,4,'Gallo pinto','Comida','',4500.00,1);
/*!40000 ALTER TABLE `linea_pedido` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-02 22:12:42
