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
-- Table structure for table `receta_item`
--

DROP TABLE IF EXISTS `receta_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receta_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `producto_id` bigint DEFAULT NULL,
  `opcion_producto_id` bigint DEFAULT NULL,
  `ingrediente_id` bigint NOT NULL,
  `cantidad_requerida` decimal(15,6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_receta_producto_ingrediente` (`producto_id`,`ingrediente_id`),
  UNIQUE KEY `uq_receta_opcion_ingrediente` (`opcion_producto_id`,`ingrediente_id`),
  KEY `idx_receta_ingrediente` (`ingrediente_id`),
  CONSTRAINT `fk_receta_ingrediente` FOREIGN KEY (`ingrediente_id`) REFERENCES `ingrediente` (`id`),
  CONSTRAINT `fk_receta_opcion` FOREIGN KEY (`opcion_producto_id`) REFERENCES `opcion_producto` (`id`),
  CONSTRAINT `fk_receta_producto` FOREIGN KEY (`producto_id`) REFERENCES `producto` (`id`),
  CONSTRAINT `chk_receta_item_cantidad` CHECK ((`cantidad_requerida` > 0)),
  CONSTRAINT `chk_receta_item_un_solo_padre` CHECK ((((`producto_id` is not null) and (`opcion_producto_id` is null)) or ((`producto_id` is null) and (`opcion_producto_id` is not null))))
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receta_item`
--

LOCK TABLES `receta_item` WRITE;
/*!40000 ALTER TABLE `receta_item` DISABLE KEYS */;
INSERT INTO `receta_item` VALUES (16,6,NULL,1,275.000000),(17,6,NULL,7,15.000000),(18,6,NULL,8,100.000000),(19,6,NULL,6,150.000000),(20,6,NULL,9,5.000000),(22,4,NULL,1,350.000000),(23,4,NULL,2,200.000000),(24,NULL,4,3,2.000000),(25,NULL,5,4,150.000000),(26,NULL,6,3,150.000000),(27,5,NULL,1,350.000000),(28,5,NULL,5,200.000000),(29,NULL,7,2,200.000000),(30,7,NULL,8,30.000000),(33,8,NULL,1,200.000000),(34,8,NULL,11,0.500000),(35,NULL,8,14,500.000000),(37,9,NULL,15,365.000000);
/*!40000 ALTER TABLE `receta_item` ENABLE KEYS */;
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

-- Dump completed on 2026-11-02 22:12:51
