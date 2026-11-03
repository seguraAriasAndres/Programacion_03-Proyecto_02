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
-- Table structure for table `movimiento_stock`
--

DROP TABLE IF EXISTS `movimiento_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movimiento_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ingrediente_id` bigint NOT NULL,
  `pedido_id` bigint DEFAULT NULL,
  `tipo_movimiento` enum('REABASTECIMIENTO','MERMA','CONSUMO_VENTA','DEVOLUCION_VENTA') NOT NULL,
  `cantidad` decimal(15,6) NOT NULL,
  `fecha` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `usuario` varchar(100) NOT NULL,
  `motivo` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_mov_stock_pedido_ingrediente_tipo` (`pedido_id`,`ingrediente_id`,`tipo_movimiento`),
  KEY `idx_mov_stock_ingrediente_fecha` (`ingrediente_id`,`fecha`),
  CONSTRAINT `fk_mov_stock_ingrediente` FOREIGN KEY (`ingrediente_id`) REFERENCES `ingrediente` (`id`),
  CONSTRAINT `fk_mov_stock_pedido` FOREIGN KEY (`pedido_id`) REFERENCES `pedido` (`id`),
  CONSTRAINT `chk_mov_stock_cantidad` CHECK ((`cantidad` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `movimiento_stock`
--

LOCK TABLES `movimiento_stock` WRITE;
/*!40000 ALTER TABLE `movimiento_stock` DISABLE KEYS */;
INSERT INTO `movimiento_stock` VALUES (1,1,NULL,'REABASTECIMIENTO',50000.000000,'2026-10-29 02:42:26.335404','admin','compra'),(2,2,NULL,'REABASTECIMIENTO',50000.000000,'2026-10-29 02:45:47.295420','admin','Reabastecer'),(3,3,NULL,'REABASTECIMIENTO',60.000000,'2026-10-29 02:46:06.613310','admin','reabastecer'),(4,2,NULL,'REABASTECIMIENTO',4940.000000,'2026-10-29 10:32:54.858195','admin','compra'),(5,2,NULL,'REABASTECIMIENTO',5000.000000,'2026-10-29 12:02:30.867071','admin','compra'),(6,4,NULL,'REABASTECIMIENTO',5000.000000,'2026-10-29 12:05:09.350905','admin','compra'),(7,4,NULL,'REABASTECIMIENTO',5000.000000,'2026-10-29 12:05:43.837738','admin','compra'),(8,1,4,'CONSUMO_VENTA',700.000000,'2026-10-29 21:24:11.027135','cajero','Consumo de ingredientes asociado al pedido.'),(9,2,4,'CONSUMO_VENTA',400.000000,'2026-10-29 21:24:11.207532','cajero','Consumo de ingredientes asociado al pedido.'),(10,3,4,'CONSUMO_VENTA',4.000000,'2026-10-29 21:24:11.390799','cajero','Consumo de ingredientes asociado al pedido.'),(11,4,4,'CONSUMO_VENTA',300.000000,'2026-10-29 21:24:11.577121','cajero','Consumo de ingredientes asociado al pedido.'),(12,5,NULL,'REABASTECIMIENTO',4000.000000,'2026-10-29 21:35:08.341290','admin','compra'),(13,6,NULL,'REABASTECIMIENTO',3500.000000,'2026-10-30 13:39:24.687079','admin','Ingreso A Stock de Lengua'),(14,7,NULL,'REABASTECIMIENTO',5000.000000,'2026-10-30 13:39:48.967260','admin','Surplus de sal'),(15,8,NULL,'REABASTECIMIENTO',5000.000000,'2026-10-30 13:40:12.832246','admin','Amamos los tomates'),(16,1,5,'CONSUMO_VENTA',900.000000,'2026-10-30 19:44:30.943355','cajero','Consumo de ingredientes asociado al pedido.'),(17,5,5,'CONSUMO_VENTA',200.000000,'2026-10-30 19:44:31.138697','cajero','Consumo de ingredientes asociado al pedido.'),(18,1,6,'CONSUMO_VENTA',1050.000000,'2026-10-30 19:45:09.895952','cajero','Consumo de ingredientes asociado al pedido.'),(19,2,6,'CONSUMO_VENTA',600.000000,'2026-10-30 19:45:10.092704','cajero','Consumo de ingredientes asociado al pedido.'),(20,9,NULL,'REABASTECIMIENTO',500.000000,'2026-10-30 13:47:02.846461','stock','ajo surplus'),(21,3,NULL,'REABASTECIMIENTO',11.000000,'2026-10-30 14:26:24.312027','admin','six seven'),(22,4,NULL,'REABASTECIMIENTO',3000.000000,'2026-10-30 17:32:29.987385','admin','surplus'),(23,8,8,'CONSUMO_VENTA',30.000000,'2026-10-30 23:36:37.973339','cajero','Consumo de ingredientes asociado al pedido.'),(24,1,7,'CONSUMO_VENTA',2375.000000,'2026-10-30 23:38:07.265174','cajero','Consumo de ingredientes asociado al pedido.'),(25,2,7,'CONSUMO_VENTA',800.000000,'2026-10-30 23:38:07.406194','cajero','Consumo de ingredientes asociado al pedido.'),(26,5,7,'CONSUMO_VENTA',400.000000,'2026-10-30 23:38:07.546017','cajero','Consumo de ingredientes asociado al pedido.'),(27,6,7,'CONSUMO_VENTA',150.000000,'2026-10-30 23:38:07.684304','cajero','Consumo de ingredientes asociado al pedido.'),(28,7,7,'CONSUMO_VENTA',15.000000,'2026-10-30 23:38:07.823256','cajero','Consumo de ingredientes asociado al pedido.'),(29,8,7,'CONSUMO_VENTA',100.000000,'2026-10-30 23:38:07.966289','cajero','Consumo de ingredientes asociado al pedido.'),(30,9,7,'CONSUMO_VENTA',5.000000,'2026-10-30 23:38:08.105167','cajero','Consumo de ingredientes asociado al pedido.'),(31,10,NULL,'REABASTECIMIENTO',10000.000000,'2026-10-30 18:11:47.495263','admin','Llegaron 3 galones en la mañana'),(32,11,NULL,'REABASTECIMIENTO',3.000000,'2026-10-30 18:14:52.984894','stock','Tres baguet para almuerzos'),(33,1,9,'CONSUMO_VENTA',1675.000000,'2026-10-31 01:18:39.542221','cajero','Consumo de ingredientes asociado al pedido.'),(34,2,9,'CONSUMO_VENTA',800.000000,'2026-10-31 01:18:39.723500','cajero','Consumo de ingredientes asociado al pedido.'),(35,3,9,'CONSUMO_VENTA',8.000000,'2026-10-31 01:18:39.903998','cajero','Consumo de ingredientes asociado al pedido.'),(36,4,9,'CONSUMO_VENTA',600.000000,'2026-10-31 01:18:40.088137','cajero','Consumo de ingredientes asociado al pedido.'),(37,6,9,'CONSUMO_VENTA',150.000000,'2026-10-31 01:18:40.267391','cajero','Consumo de ingredientes asociado al pedido.'),(38,7,9,'CONSUMO_VENTA',15.000000,'2026-10-31 01:18:40.453796','cajero','Consumo de ingredientes asociado al pedido.'),(39,8,9,'CONSUMO_VENTA',100.000000,'2026-10-31 01:18:40.637171','cajero','Consumo de ingredientes asociado al pedido.'),(40,9,9,'CONSUMO_VENTA',5.000000,'2026-10-31 01:18:40.815575','cajero','Consumo de ingredientes asociado al pedido.'),(41,12,NULL,'REABASTECIMIENTO',9.000000,'2026-10-31 03:37:42.009312','admin','Compré dos mallas de cebolla'),(42,12,NULL,'MERMA',9.000000,'2026-10-31 03:39:57.229333','admin','Compre dos mallas de cebolla'),(43,13,NULL,'REABASTECIMIENTO',10.000000,'2026-10-31 10:34:42.003860','Dueño','Compré en el mercado de San José'),(44,13,NULL,'MERMA',3.000000,'2026-10-31 10:35:45.992322','Dueño','rallamos 3 en almuerzos'),(45,14,NULL,'REABASTECIMIENTO',7000.000000,'2026-10-31 10:52:25.957862','Dueño','Compré 7 botellas'),(46,1,10,'CONSUMO_VENTA',350.000000,'2026-10-31 16:59:19.105514','CajaAzul','Consumo de ingredientes asociado al pedido.'),(47,2,10,'CONSUMO_VENTA',200.000000,'2026-10-31 16:59:19.289018','CajaAzul','Consumo de ingredientes asociado al pedido.'),(48,3,10,'CONSUMO_VENTA',2.000000,'2026-10-31 16:59:19.464499','CajaAzul','Consumo de ingredientes asociado al pedido.'),(49,14,NULL,'MERMA',300.000000,'2026-10-31 11:55:12.404366','Dueño','Gastado en dos almuerzos'),(50,1,10,'DEVOLUCION_VENTA',350.000000,'2026-10-31 18:11:23.629615','CajaRoja','Devolución de ingredientes asociada al pedido.'),(51,2,10,'DEVOLUCION_VENTA',200.000000,'2026-10-31 18:11:23.788686','CajaRoja','Devolución de ingredientes asociada al pedido.'),(52,3,10,'DEVOLUCION_VENTA',2.000000,'2026-10-31 18:11:23.945823','CajaRoja','Devolución de ingredientes asociada al pedido.'),(53,1,13,'CONSUMO_VENTA',200.000000,'2026-10-31 18:20:52.462135','CajaRoja','Consumo de ingredientes asociado al pedido.'),(54,11,13,'CONSUMO_VENTA',0.500000,'2026-10-31 18:20:52.630394','CajaRoja','Consumo de ingredientes asociado al pedido.'),(55,14,13,'CONSUMO_VENTA',500.000000,'2026-10-31 18:20:52.798715','CajaRoja','Consumo de ingredientes asociado al pedido.'),(56,1,13,'DEVOLUCION_VENTA',200.000000,'2026-10-31 18:39:25.680166','CajaRoja','Devolución de ingredientes asociada al pedido.'),(57,11,13,'DEVOLUCION_VENTA',0.500000,'2026-10-31 18:39:25.847020','CajaRoja','Devolución de ingredientes asociada al pedido.'),(58,14,13,'DEVOLUCION_VENTA',500.000000,'2026-10-31 18:39:26.012456','CajaRoja','Devolución de ingredientes asociada al pedido.'),(59,1,14,'CONSUMO_VENTA',200.000000,'2026-10-31 18:39:43.253709','CajaRoja','Consumo de ingredientes asociado al pedido.'),(60,11,14,'CONSUMO_VENTA',0.500000,'2026-10-31 18:39:43.419041','CajaRoja','Consumo de ingredientes asociado al pedido.'),(61,14,14,'CONSUMO_VENTA',500.000000,'2026-10-31 18:39:43.584432','CajaRoja','Consumo de ingredientes asociado al pedido.'),(62,1,15,'CONSUMO_VENTA',350.000000,'2026-10-31 18:42:03.524267','CajaRoja','Consumo de ingredientes asociado al pedido.'),(63,2,15,'CONSUMO_VENTA',200.000000,'2026-10-31 18:42:03.714552','CajaRoja','Consumo de ingredientes asociado al pedido.'),(64,5,15,'CONSUMO_VENTA',200.000000,'2026-10-31 18:42:03.880019','CajaRoja','Consumo de ingredientes asociado al pedido.'),(65,15,NULL,'REABASTECIMIENTO',100000.000000,'2026-11-01 20:03:45.543007','Dueño','AguaPlus'),(66,1,16,'CONSUMO_VENTA',1050.000000,'2026-11-02 02:55:00.326919','CajaRoja','Consumo de ingredientes asociado al pedido.'),(67,2,16,'CONSUMO_VENTA',400.000000,'2026-11-02 02:55:00.529793','CajaRoja','Consumo de ingredientes asociado al pedido.'),(68,5,16,'CONSUMO_VENTA',200.000000,'2026-11-02 02:55:00.718015','CajaRoja','Consumo de ingredientes asociado al pedido.'),(69,15,16,'CONSUMO_VENTA',730.000000,'2026-11-02 02:55:00.905913','CajaRoja','Consumo de ingredientes asociado al pedido.');
/*!40000 ALTER TABLE `movimiento_stock` ENABLE KEYS */;
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

-- Dump completed on 2026-11-02 22:12:36
