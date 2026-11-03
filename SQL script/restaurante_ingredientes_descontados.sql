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
-- Table structure for table `ingredientes_descontados`
--

DROP TABLE IF EXISTS `ingredientes_descontados`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingredientes_descontados` (
  `pedido_id` bigint NOT NULL,
  `ingrediente_id` bigint NOT NULL,
  `cantidad` decimal(15,6) unsigned NOT NULL,
  PRIMARY KEY (`pedido_id`,`ingrediente_id`),
  CONSTRAINT `fr_ingredientes_descontados_pedido` FOREIGN KEY (`pedido_id`) REFERENCES `pedido` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ingredientes_descontados`
--

LOCK TABLES `ingredientes_descontados` WRITE;
/*!40000 ALTER TABLE `ingredientes_descontados` DISABLE KEYS */;
INSERT INTO `ingredientes_descontados` VALUES (4,1,700.000000),(4,2,400.000000),(4,3,4.000000),(4,4,300.000000),(5,1,900.000000),(5,5,200.000000),(6,1,1050.000000),(6,2,600.000000),(7,1,2375.000000),(7,2,800.000000),(7,5,400.000000),(7,6,150.000000),(7,7,15.000000),(7,8,100.000000),(7,9,5.000000),(8,8,30.000000),(9,1,1675.000000),(9,2,800.000000),(9,3,8.000000),(9,4,600.000000),(9,6,150.000000),(9,7,15.000000),(9,8,100.000000),(9,9,5.000000),(10,1,350.000000),(10,2,200.000000),(10,3,2.000000),(13,1,200.000000),(13,11,0.500000),(13,14,500.000000),(14,1,200.000000),(14,11,0.500000),(14,14,500.000000),(15,1,350.000000),(15,2,200.000000),(15,5,200.000000),(16,1,1050.000000),(16,2,400.000000),(16,5,200.000000),(16,15,730.000000);
/*!40000 ALTER TABLE `ingredientes_descontados` ENABLE KEYS */;
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

-- Dump completed on 2026-11-02 22:12:48
