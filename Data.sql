-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: auction_db
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
INSERT INTO `auction_sessions` VALUES (1,15000000.00,'2026-04-15 13:00:00.000000','2026-04-13 01:00:00.000000','ENDED',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(2,500000.00,'2026-04-21 05:00:00.000000','2026-04-21 04:26:00.000000','ENDED',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(3,500000.00,'2026-04-21 07:00:00.000000','2026-04-21 06:12:00.000000','ENDED',NULL,2,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(4,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:16:00.000000','ACTIVE',NULL,3,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(5,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:21:00.000000','ACTIVE',NULL,4,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(6,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:35:00.000000','ACTIVE',NULL,5,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL),(7,50000.00,'2026-04-30 11:00:00.000000','2026-04-23 10:47:46.000000','ACTIVE',NULL,7,NULL,NULL,'2026-04-23 10:42:46.985206',NULL,NULL,NULL,50000.00,50000.00,14),(8,500000.00,'2026-04-30 11:00:00.000000','2026-04-23 10:52:02.000000','ACTIVE',NULL,8,NULL,NULL,'2026-04-23 10:47:02.700684',NULL,NULL,NULL,500000.00,5000.00,14),(9,1.00,'2026-04-30 11:00:00.000000','2026-04-23 10:57:05.340038','ACTIVE',NULL,9,'2026-04-23 10:57:05.340038',15,'2026-04-23 10:56:21.949887',NULL,NULL,NULL,1.00,1.00,14);
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bids`
--

LOCK TABLES `bids` WRITE;
/*!40000 ALTER TABLE `bids` DISABLE KEYS */;
INSERT INTO `bids` VALUES (1,3,'admin',7600000,'2026-03-18 12:06:16',NULL,NULL,NULL,NULL),(2,3,'minh',7700000,'2026-03-18 12:12:11',NULL,NULL,NULL,NULL),(3,3,'minh',7800000,'2026-03-18 12:13:18',NULL,NULL,NULL,NULL),(4,3,'minh',7800001,'2026-03-18 12:25:20',NULL,NULL,NULL,NULL),(5,1,'admin',27500001,'2026-03-18 12:39:03',NULL,NULL,NULL,NULL),(6,3,'admin',7800002,'2026-03-18 13:12:08',NULL,NULL,NULL,NULL),(7,2,'admin',31000000,'2026-03-18 13:50:23',NULL,NULL,NULL,NULL);
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
INSERT INTO `items` VALUES (1,15000000,'2026-04-30 23:59:59.000000','iphone.jpg','iPhone 13 128GB','2026-04-21 08:00:00.000000','ACTIVE','phone','electronics',NULL),(2,NULL,NULL,'upload/images/luffy.png','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(3,NULL,NULL,'luffy.png','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(4,NULL,NULL,'luffy.png','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(5,NULL,NULL,'luffy.png','Mô hình One Piece Luffy Gear 5',NULL,NULL,NULL,'art',NULL),(7,NULL,NULL,'https://suckhoedoisong.qltns.mediacdn.vn/324455921873985536/2022/4/25/trong-muop-huong-11-1650895342271997058777.jpg','muop',NULL,NULL,'Art','art','muop con'),(8,NULL,NULL,'','cnocac',NULL,NULL,'Electronics','electronics','adawd'),(9,NULL,NULL,'deoco','phanlamviecdi',NULL,NULL,'Electronics','electronics','dungluoi');
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
  `dob` date DEFAULT NULL,
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

-- Dump completed on 2026-04-25 12:14:18
