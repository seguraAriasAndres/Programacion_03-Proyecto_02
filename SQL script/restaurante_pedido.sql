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
-- Table structure for table `pedido`
--

DROP TABLE IF EXISTS `pedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pedido` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre_cliente` varchar(150) NOT NULL,
  `fecha_pedido` datetime(6) NOT NULL,
  `confirmado` tinyint(1) NOT NULL DEFAULT '0',
  `estado_preparacion` varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  `estado_pago` varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  `usuario_creacion` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pedido`
--

LOCK TABLES `pedido` WRITE;
/*!40000 ALTER TABLE `pedido` DISABLE KEYS */;
INSERT INTO `pedido` VALUES (4,'daniel','2026-10-29 12:09:39.643970',1,'ENTREGADO','PAGADO','cajero'),(5,'Juan Perez','2026-10-30 13:44:15.294060',1,'ENTREGADO','PAGADO','cajero'),(6,'pepe el mago','2026-10-30 13:45:06.540504',1,'ENTREGADO','PAGADO','cajero'),(7,'Alejandro','2026-10-30 17:36:04.947819',1,'ENTREGADO','PAGADO','cajero'),(8,'juan Perez','2026-10-30 17:36:45.757450',1,'ENTREGADO','PAGADO','cajero'),(9,'daniel','2026-10-30 19:18:21.330687',1,'ENTREGADO','PAGADO','cajero'),(10,'Junior','2026-10-31 10:58:58.552348',1,'CANCELADO','PENDIENTE','CajaAzul'),(11,'Junior','2026-10-31 12:15:14.930533',0,'CANCELADO','PENDIENTE','CajaRoja'),(12,'Junior','2026-10-31 12:16:13.197651',0,'CANCELADO','PENDIENTE','CajaRoja'),(13,'Junior','2026-10-31 12:20:20.262301',1,'CANCELADO','PENDIENTE','CajaRoja'),(14,'Junior','2026-10-31 12:39:36.898791',1,'ENTREGADO','PAGADO','CajaRoja'),(15,'Marta','2026-10-31 12:42:01.527671',1,'ENTREGADO','PAGADO','CajaRoja'),(16,'Juanito','2026-11-01 20:53:59.018815',1,'ENTREGADO','PAGADO','CajaRoja');
/*!40000 ALTER TABLE `pedido` ENABLE KEYS */;
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

-- Dump completed on 2026-11-02 22:12:54
