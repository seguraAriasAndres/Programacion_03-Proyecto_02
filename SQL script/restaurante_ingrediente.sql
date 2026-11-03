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
-- Table structure for table `ingrediente`
--

DROP TABLE IF EXISTS `ingrediente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingrediente` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `cantidad_disponible` decimal(15,6) NOT NULL DEFAULT '0.000000',
  `unidad_medida` enum('UNIDAD','GRAMO','MILILITRO') NOT NULL,
  `stock_minimo` decimal(15,6) NOT NULL DEFAULT '0.000000',
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ingrediente_nombre` (`nombre`),
  CONSTRAINT `chk_ingrediente_cantidad_disponible` CHECK ((`cantidad_disponible` >= 0)),
  CONSTRAINT `chk_ingrediente_stock_minimo` CHECK ((`stock_minimo` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ingrediente`
--

LOCK TABLES `ingrediente` WRITE;
/*!40000 ALTER TABLE `ingrediente` DISABLE KEYS */;
INSERT INTO `ingrediente` VALUES (1,'Arroz',41700.000000,'GRAMO',5000.000000,1),(2,'Frijoles',56740.000000,'GRAMO',5000.000000,1),(3,'Huevo',59.000000,'UNIDAD',12.000000,1),(4,'Carne de res',12100.000000,'GRAMO',5000.000000,1),(5,'Atún',3000.000000,'GRAMO',2000.000000,1),(6,'Lengua de Vaca',3200.000000,'GRAMO',2000.000000,1),(7,'Sal',4970.000000,'GRAMO',500.000000,1),(8,'Tomate',4770.000000,'GRAMO',1000.000000,1),(9,'Ajo',490.000000,'GRAMO',200.000000,1),(10,'Leche',10000.000000,'MILILITRO',10000.000000,1),(11,'Baguet',2.500000,'UNIDAD',3.000000,1),(12,'Cebolla',0.000000,'UNIDAD',9.000000,1),(13,'Lechuga',7.000000,'UNIDAD',5.000000,1),(14,'Mayonesa',6200.000000,'MILILITRO',5000.000000,1),(15,'Agua',99270.000000,'MILILITRO',10000.000000,1);
/*!40000 ALTER TABLE `ingrediente` ENABLE KEYS */;
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

-- Dump completed on 2026-11-02 22:12:45
