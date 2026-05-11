-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: auction_system
-- ------------------------------------------------------
-- Server version	8.0.45

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

--
-- Table structure for table `auction_item`
--

DROP TABLE IF EXISTS `auction_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auction_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  `start_price` double DEFAULT NULL,
  `current_price` double DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction_item`
--

LOCK TABLES `auction_item` WRITE;
/*!40000 ALTER TABLE `auction_item` DISABLE KEYS */;
INSERT INTO `auction_item` VALUES (1,'Bình gốm cổ','Đồ gốm thời Nguyễn',1000000,1200000,'2026-03-20 20:00:00'),(2,'Tranh sơn dầu','Tranh phong cảnh',500000,700000,'2026-03-22 18:00:00');
/*!40000 ALTER TABLE `auction_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auction_session`
--

DROP TABLE IF EXISTS `auction_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auction_session` (
  `id` int NOT NULL AUTO_INCREMENT,
  `approved_at` datetime(6) DEFAULT NULL,
  `approved_by_admin_id` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `current_price` decimal(15,2) NOT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `reject_reason` text,
  `rejected_at` datetime(6) DEFAULT NULL,
  `rejected_by_admin_id` int DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `starting_price` decimal(15,2) NOT NULL,
  `status` varchar(30) DEFAULT NULL,
  `step_price` decimal(15,2) NOT NULL,
  `product_id` int DEFAULT NULL,
  `seller_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_g72mltw5kv4k9l2a1xtovaq1o` (`product_id`),
  KEY `FK7brcqcaw8i3rf0kpr6kjtp4uw` (`seller_id`),
  CONSTRAINT `FK7brcqcaw8i3rf0kpr6kjtp4uw` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKiydy2ah7tvoux9733o63f7spm` FOREIGN KEY (`product_id`) REFERENCES `items` (`id`),
  CONSTRAINT `FKlgxdvl5yh8l5kjvrerdcmfglt` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction_session`
--

LOCK TABLES `auction_session` WRITE;
/*!40000 ALTER TABLE `auction_session` DISABLE KEYS */;
INSERT INTO `auction_session` VALUES (1,'2026-04-21 06:09:45.746502',15,'2026-04-21 06:02:52.511406',15000000.00,'2026-12-31 13:00:00.000000',NULL,NULL,NULL,'2026-04-21 06:09:45.746502',15000000.00,'ACTIVE',500000.00,3,14);
/*!40000 ALTER TABLE `auction_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auction_sessions`
--

DROP TABLE IF EXISTS `auction_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auction_sessions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `current_price` decimal(15,2) NOT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','ACTIVE','ENDED','CANCELED') DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `item_id` int DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `approved_by_admin_id` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `reject_reason` text,
  `rejected_at` datetime(6) DEFAULT NULL,
  `rejected_by_admin_id` int DEFAULT NULL,
  `starting_price` decimal(15,2) NOT NULL,
  `step_price` decimal(15,2) NOT NULL,
  `seller_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK22lotvg0okvk74b3w8cudgh92` (`product_id`),
  KEY `FKk42jfok4qomtvm3uq2cu43geo` (`item_id`),
  KEY `FK86s10x90aml8yaipegrhyu216` (`seller_id`),
  CONSTRAINT `FK22lotvg0okvk74b3w8cudgh92` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FK86s10x90aml8yaipegrhyu216` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKk42jfok4qomtvm3uq2cu43geo` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction_sessions`
--

LOCK TABLES `auction_sessions` WRITE;
/*!40000 ALTER TABLE `auction_sessions` DISABLE KEYS */;
INSERT INTO `auction_sessions` VALUES (1,9999999.00,'2026-06-15 13:00:00.000000','2026-04-13 01:00:00.000000','ACTIVE',1,1,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(2,500000.00,'2026-06-21 05:00:00.000000','2026-04-21 04:26:00.000000','ACTIVE',2,2,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(3,100.00,'2026-06-21 07:00:00.000000','2026-04-21 06:12:00.000000','ACTIVE',1,2,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(4,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:16:00.000000','ENDED',NULL,3,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(5,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:21:00.000000','ENDED',NULL,4,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(6,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:35:00.000000','ENDED',NULL,5,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(7,50000.00,'2026-04-30 11:00:00.000000','2026-04-23 10:47:46.000000','ENDED',NULL,7,NULL,NULL,'2026-04-23 10:42:46.985206',NULL,NULL,NULL,50000.00,50000.00,14),(8,500000.00,'2026-04-30 11:00:00.000000','2026-04-23 10:52:02.000000','ENDED',NULL,8,NULL,NULL,'2026-04-23 10:47:02.700684',NULL,NULL,NULL,500000.00,5000.00,14),(9,1.00,'2026-04-30 11:00:00.000000','2026-04-23 10:57:05.340038','ENDED',NULL,9,'2026-04-23 10:57:05.340038',15,'2026-04-23 10:56:21.949887',NULL,NULL,NULL,1.00,1.00,14);
/*!40000 ALTER TABLE `auction_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bids`
--

DROP TABLE IF EXISTS `bids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bids` (
  `id` int NOT NULL AUTO_INCREMENT,
  `item_id` int DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `bid_amount` double DEFAULT NULL,
  `bid_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `bidder_id` int DEFAULT NULL,
  `amount` decimal(38,2) DEFAULT NULL,
  `time` datetime(6) DEFAULT NULL,
  `session_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmtrc6tnwawlpk1u2km6qnxbha` (`bidder_id`),
  KEY `FKcodn6nqli14j1kcu37a12e92a` (`session_id`),
  CONSTRAINT `FK87p1gqx16r8g0t7dv5xooqsyn` FOREIGN KEY (`session_id`) REFERENCES `auction_session` (`id`),
  CONSTRAINT `FKcodn6nqli14j1kcu37a12e92a` FOREIGN KEY (`session_id`) REFERENCES `auction_sessions` (`id`),
  CONSTRAINT `FKmtrc6tnwawlpk1u2km6qnxbha` FOREIGN KEY (`bidder_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bids`
--

LOCK TABLES `bids` WRITE;
/*!40000 ALTER TABLE `bids` DISABLE KEYS */;
INSERT INTO `bids` VALUES (1,3,'admin',7600000,'2026-03-18 12:06:16',NULL,NULL,NULL,NULL),(2,3,'minh',7700000,'2026-03-18 12:12:11',NULL,NULL,NULL,NULL),(3,3,'minh',7800000,'2026-03-18 12:13:18',NULL,NULL,NULL,NULL),(4,3,'minh',7800001,'2026-03-18 12:25:20',NULL,NULL,NULL,NULL),(5,1,'admin',27500001,'2026-03-18 12:39:03',NULL,NULL,NULL,NULL),(6,3,'admin',7800002,'2026-03-18 13:12:08',NULL,NULL,NULL,NULL),(7,2,'admin',31000000,'2026-03-18 13:50:23',NULL,NULL,NULL,NULL),(8,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1003.00,'2026-05-09 09:14:41.217542',1),(9,NULL,NULL,NULL,'2026-05-09 16:14:41',15,1004.00,'2026-05-09 09:14:41.263541',1),(10,NULL,NULL,NULL,'2026-05-09 16:14:41',10,1011.00,'2026-05-09 09:14:41.281543',1),(11,NULL,NULL,NULL,'2026-05-09 16:14:41',10,1016.00,'2026-05-09 09:14:41.334541',1),(12,NULL,NULL,NULL,'2026-05-09 16:14:41',10,1021.00,'2026-05-09 09:14:41.394543',1),(13,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1023.00,'2026-05-09 09:14:41.413542',1),(14,NULL,NULL,NULL,'2026-05-09 16:14:41',10,1026.00,'2026-05-09 09:14:41.450541',1),(15,NULL,NULL,NULL,'2026-05-09 16:14:41',12,1027.00,'2026-05-09 09:14:41.469542',1),(16,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1028.00,'2026-05-09 09:14:41.485543',1),(17,NULL,NULL,NULL,'2026-05-09 16:14:41',10,1031.00,'2026-05-09 09:14:41.521541',1),(18,NULL,NULL,NULL,'2026-05-09 16:14:41',12,1032.00,'2026-05-09 09:14:41.539558',1),(19,NULL,NULL,NULL,'2026-05-09 16:14:41',15,1034.00,'2026-05-09 09:14:41.555542',1),(20,NULL,NULL,NULL,'2026-05-09 16:14:41',10,1036.00,'2026-05-09 09:14:41.572542',1),(21,NULL,NULL,NULL,'2026-05-09 16:14:41',12,1037.00,'2026-05-09 09:14:41.589543',1),(22,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1038.00,'2026-05-09 09:14:41.603543',1),(23,NULL,NULL,NULL,'2026-05-09 16:14:41',15,1039.00,'2026-05-09 09:14:41.618543',1),(24,NULL,NULL,NULL,'2026-05-09 16:14:41',12,1042.00,'2026-05-09 09:14:41.647541',1),(25,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1043.00,'2026-05-09 09:14:41.667544',1),(26,NULL,NULL,NULL,'2026-05-09 16:14:41',12,1047.00,'2026-05-09 09:14:41.712586',1),(27,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1048.00,'2026-05-09 09:14:41.743543',1),(28,NULL,NULL,NULL,'2026-05-09 16:14:41',10,1051.00,'2026-05-09 09:14:41.789630',1),(29,NULL,NULL,NULL,'2026-05-09 16:14:41',15,1054.00,'2026-05-09 09:14:41.815587',1),(30,NULL,NULL,NULL,'2026-05-09 16:14:41',12,1057.00,'2026-05-09 09:14:41.867368',1),(31,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1058.00,'2026-05-09 09:14:41.884602',1),(32,NULL,NULL,NULL,'2026-05-09 16:14:41',15,1059.00,'2026-05-09 09:14:41.906040',1),(33,NULL,NULL,NULL,'2026-05-09 16:14:41',12,1062.00,'2026-05-09 09:14:41.939688',1),(34,NULL,NULL,NULL,'2026-05-09 16:14:41',14,1063.00,'2026-05-09 09:14:41.958695',1),(35,NULL,NULL,NULL,'2026-05-09 16:14:41',15,1064.00,'2026-05-09 09:14:41.971695',1),(36,NULL,NULL,NULL,'2026-05-09 16:14:42',10,1066.00,'2026-05-09 09:14:42.003943',1),(37,NULL,NULL,NULL,'2026-05-09 16:14:42',12,1067.00,'2026-05-09 09:14:42.018033',1),(38,NULL,NULL,NULL,'2026-05-09 16:14:42',15,1069.00,'2026-05-09 09:14:42.033154',1),(39,NULL,NULL,NULL,'2026-05-09 16:14:42',10,1071.00,'2026-05-09 09:14:42.066913',1),(40,NULL,NULL,NULL,'2026-05-09 16:14:42',12,1072.00,'2026-05-09 09:14:42.083924',1),(41,NULL,NULL,NULL,'2026-05-09 16:14:42',14,1073.00,'2026-05-09 09:14:42.098669',1),(42,NULL,NULL,NULL,'2026-05-09 16:14:42',15,1074.00,'2026-05-09 09:14:42.117538',1),(43,NULL,NULL,NULL,'2026-05-09 16:14:42',10,1076.00,'2026-05-09 09:14:42.136729',1),(44,NULL,NULL,NULL,'2026-05-09 16:14:42',12,1077.00,'2026-05-09 09:14:42.156872',1),(45,NULL,NULL,NULL,'2026-05-09 16:14:42',14,1078.00,'2026-05-09 09:14:42.173061',1),(46,NULL,NULL,NULL,'2026-05-09 16:14:42',12,1082.00,'2026-05-09 09:14:42.187749',1),(47,NULL,NULL,NULL,'2026-05-09 16:14:42',14,1083.00,'2026-05-09 09:14:42.210210',1),(48,NULL,NULL,NULL,'2026-05-09 16:14:42',10,1086.00,'2026-05-09 09:14:42.222628',1),(49,NULL,NULL,NULL,'2026-05-09 16:14:42',12,1087.00,'2026-05-09 09:14:42.241203',1),(50,NULL,NULL,NULL,'2026-05-09 16:14:42',14,1088.00,'2026-05-09 09:14:42.255227',1),(51,NULL,NULL,NULL,'2026-05-09 16:14:42',15,1089.00,'2026-05-09 09:14:42.271224',1),(52,NULL,NULL,NULL,'2026-05-09 16:14:42',10,1091.00,'2026-05-09 09:14:42.301226',1),(53,NULL,NULL,NULL,'2026-05-09 16:14:42',12,1092.00,'2026-05-09 09:14:42.318225',1),(54,NULL,NULL,NULL,'2026-05-09 16:14:42',14,1093.00,'2026-05-09 09:14:42.335226',1),(55,NULL,NULL,NULL,'2026-05-09 16:14:42',15,1094.00,'2026-05-09 09:14:42.355139',1),(56,NULL,NULL,NULL,'2026-05-09 16:14:42',10,1096.00,'2026-05-09 09:14:42.388136',1),(57,NULL,NULL,NULL,'2026-05-09 16:14:42',14,1098.00,'2026-05-09 09:14:42.404224',1),(58,NULL,NULL,NULL,'2026-05-09 16:14:42',15,1099.00,'2026-05-09 09:14:42.422138',1),(59,NULL,NULL,NULL,'2026-05-09 16:14:43',12,9999999.00,'2026-05-09 09:14:43.813028',1);
/*!40000 ALTER TABLE `bids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `items`
--

DROP TABLE IF EXISTS `items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `items` (
  `id` int NOT NULL AUTO_INCREMENT,
  `current_price` double DEFAULT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `image_path` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `category` varchar(31) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `items`
--

LOCK TABLES `items` WRITE;
/*!40000 ALTER TABLE `items` DISABLE KEYS */;
INSERT INTO `items` VALUES (1,15000000,'2026-04-30 23:59:59.000000','iphone.jpg','iPhone 13 128GB','2026-04-21 08:00:00.000000','ACTIVE','phone','electronics',NULL),(2,NULL,NULL,'iphone.jpg','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(3,NULL,NULL,'luffy.png','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(4,NULL,NULL,'luffy.png','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(5,NULL,NULL,'luffy.png','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(7,NULL,NULL,'https://suckhoedoisong.qltns.mediacdn.vn/324455921873985536/2022/4/25/trong-muop-huong-11-1650895342271997058777.jpg','muop',NULL,NULL,'Art','art','muop con'),(8,NULL,NULL,'','cnocac',NULL,NULL,'Electronics','electronics','adawd'),(9,NULL,NULL,'deoco','phanlamviecdi',NULL,NULL,'Electronics','electronics','dungluoi');
/*!40000 ALTER TABLE `items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` int NOT NULL AUTO_INCREMENT,
  `image_path` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `description` text,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'iphone.jpg','Laptop Lenovo Legion 5','Electronics',NULL,NULL),(2,'upload/images/luffy.png','Mô hình One Piece Luffy Gear 5','toy',NULL,NULL),(3,NULL,'Laptop Dell XPS 13','Electronics','Máy còn mới, pin tốt','https://example.com/laptop.jpg');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `balance` decimal(38,2) DEFAULT NULL,
  `cccd` varchar(20) DEFAULT NULL,
  `dob` varchar(255) DEFAULT NULL,
  `pob` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `fullname` varchar(255) DEFAULT NULL,
  `place_of_birth` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `is_business` bit(1) DEFAULT NULL,
  `shop_name` varchar(255) DEFAULT NULL,
  `tax_id` varchar(255) DEFAULT NULL,
  `employee_code` varchar(20) NOT NULL,
  `admin_role` enum('SUPER_ADMIN','MODERATOR','SUPPORT') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `cccd` (`cccd`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (10,'chuvanan','123',NULL,0.00,NULL,NULL,NULL,'bodoiqua189@gmail.com','chu van an',NULL,'bidder',NULL,NULL,NULL,'','SUPER_ADMIN'),(12,'lethanhtung','lethanhtung',NULL,0.00,NULL,NULL,NULL,'giaovien895@gmail.com','le thanh tung',NULL,'bidder',NULL,NULL,NULL,'','SUPER_ADMIN'),(14,'ok','112233',NULL,0.00,NULL,NULL,NULL,'val261263@gmail.com','ok',NULL,'SELLER',_binary '\0','Shop Test','TAX001','','SUPER_ADMIN'),(15,'phanbuom','phanbuom1',NULL,0.00,NULL,NULL,NULL,'faverrices@gmail.com','Nguyễn Hà Phan',NULL,'admin',NULL,NULL,NULL,'ADM001','SUPER_ADMIN');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-09 17:16:41
