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
INSERT INTO `auction_item` VALUES (1,'Antique Ceramic Vase','Nguyen Dynasty Ceramics',1000000,1200000,'2026-03-20 20:00:00'),(2,'Oil Painting','Landscape Painting',500000,700000,'2026-03-22 18:00:00');
/*!40000 ALTER TABLE `auction_item` ENABLE KEYS */;
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
  `status` enum('PENDING','ACTIVE','ENDED','CANCELED','REJECTED') DEFAULT NULL,
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
  `apply_min_rate` bit(1) DEFAULT b'0',
  `min_rate` decimal(15,2) DEFAULT '0.00',
  `winner_id` int DEFAULT NULL,
  `highest_bidder_id` int DEFAULT NULL,
  `reserve_price` decimal(15,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK22lotvg0okvk74b3w8cudgh92` (`product_id`),
  KEY `FKk42jfok4qomtvm3uq2cu43geo` (`item_id`),
  KEY `FK86s10x90aml8yaipegrhyu216` (`seller_id`),
  KEY `fk_auction_winner` (`winner_id`),
  CONSTRAINT `FK22lotvg0okvk74b3w8cudgh92` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FK86s10x90aml8yaipegrhyu216` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_auction_winner` FOREIGN KEY (`winner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKk42jfok4qomtvm3uq2cu43geo` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction_sessions`
--

LOCK TABLES `auction_sessions` WRITE;
/*!40000 ALTER TABLE `auction_sessions` DISABLE KEYS */;
INSERT INTO `auction_sessions` VALUES (1,300000.00,'2026-06-15 13:00:00.000000','2026-04-13 01:00:00.000000','ACTIVE',1,1,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL,_binary '\0',0.00,12,12,NULL),(2,10000.00,'2026-06-21 05:00:00.000000','2026-04-21 04:26:00.000000','ACTIVE',2,2,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL,_binary '\0',0.00,NULL,NULL,NULL),(3,10000.00,'2026-06-21 07:00:00.000000','2026-04-21 06:12:00.000000','ACTIVE',1,2,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL,_binary '\0',0.00,12,12,NULL),(4,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:16:00.000000','ENDED',NULL,3,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL,_binary '\0',0.00,NULL,NULL,NULL),(5,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:21:00.000000','ENDED',NULL,4,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL,_binary '\0',0.00,NULL,NULL,NULL),(6,500000.00,'2026-04-25 07:00:00.000000','2026-04-21 07:35:00.000000','ENDED',NULL,5,NULL,NULL,NULL,NULL,NULL,NULL,0.00,0.00,NULL,_binary '\0',0.00,NULL,NULL,NULL),(7,50000.00,'2026-04-30 11:00:00.000000','2026-04-23 10:47:46.000000','ENDED',NULL,7,NULL,NULL,'2026-04-23 10:42:46.985206',NULL,NULL,NULL,50000.00,50000.00,14,_binary '\0',0.00,NULL,NULL,NULL),(8,500000.00,'2026-04-30 11:00:00.000000','2026-04-23 10:52:02.000000','ENDED',NULL,8,NULL,NULL,'2026-04-23 10:47:02.700684',NULL,NULL,NULL,500000.00,5000.00,14,_binary '\0',0.00,NULL,NULL,NULL),(9,1.00,'2026-04-30 11:00:00.000000','2026-04-23 10:57:05.340038','ENDED',NULL,9,'2026-04-23 10:57:05.340038',15,'2026-04-23 10:56:21.949887',NULL,NULL,NULL,1.00,1.00,14,_binary '\0',0.00,NULL,NULL,NULL);
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
  CONSTRAINT `FK_bids_session_new` FOREIGN KEY (`session_id`) REFERENCES `auction_sessions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FKcodn6nqli14j1kcu37a12e92a` FOREIGN KEY (`session_id`) REFERENCES `auction_sessions` (`id`),
  CONSTRAINT `FKmtrc6tnwawlpk1u2km6qnxbha` FOREIGN KEY (`bidder_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bids`
--

LOCK TABLES `bids` WRITE;
/*!40000 ALTER TABLE `bids` DISABLE KEYS */;
INSERT INTO `bids` VALUES (1,NULL,NULL,NULL,'2026-05-19 19:34:37',12,20000.00,'2026-05-19 12:34:37.324658',1),(2,NULL,NULL,NULL,'2026-05-19 19:35:11',10,30000.00,'2026-05-19 12:35:11.256689',1),(3,NULL,NULL,NULL,'2026-05-19 19:35:47',12,50000.00,'2026-05-19 12:35:47.120920',1),(4,NULL,NULL,NULL,'2026-05-19 19:36:02',10,111111.00,'2026-05-19 12:36:02.061350',1),(5,NULL,NULL,NULL,'2026-05-19 19:36:16',12,150000.00,'2026-05-19 12:36:16.549932',1),(6,NULL,NULL,NULL,'2026-05-19 19:37:07',10,180000.00,'2026-05-19 12:37:07.090018',1),(7,NULL,NULL,NULL,'2026-05-19 19:37:15',12,200000.00,'2026-05-19 12:37:15.120568',1),(8,NULL,NULL,NULL,'2026-05-19 19:42:46',10,250000.00,'2026-05-19 12:42:46.565027',1),(9,NULL,NULL,NULL,'2026-05-19 22:56:14',12,300000.00,'2026-05-19 15:56:13.908041',1);
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
  `hidden` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `items`
--

LOCK TABLES `items` WRITE;
/*!40000 ALTER TABLE `items` DISABLE KEYS */;
INSERT INTO `items` VALUES (1,15000000,'2026-04-30 23:59:59.000000','iphone.jpg','iPhone 13 128GB','2026-04-21 08:00:00.000000','ACTIVE','phone','electronics',NULL,_binary '\0'),(2,NULL,NULL,'iphone.jpg','One Piece Luffy Gear 5 Figure',NULL,NULL,NULL,'art',NULL,_binary '\0'),(3,NULL,NULL,'luffy.png','One Piece Luffy Gear 5 Figure',NULL,NULL,NULL,'art',NULL,_binary '\0'),(4,NULL,NULL,'luffy.png','One Piece Luffy Gear 5 Figure',NULL,NULL,NULL,'art',NULL,_binary '\0'),(5,NULL,NULL,'luffy.png','One Piece Luffy Gear 5 Figure',NULL,NULL,NULL,'art',NULL,_binary '\0'),(7,NULL,NULL,'https://suckhoedoisong.qltns.mediacdn.vn/324455921873985536/2022/4/25/trong-muop-huong-11-1650895342271997058777.jpg','Luffa',NULL,NULL,'Art','art','Baby Luffa',_binary '\0'),(8,NULL,NULL,'','cnocac',NULL,NULL,'Electronics','electronics','adawd',_binary '\0'),(9,NULL,NULL,'deoco','Phan get to work',NULL,NULL,'Electronics','electronics','Do not be lazy',_binary '\0');
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
INSERT INTO `products` VALUES (1,'iphone.jpg','Lenovo Legion 5 Laptop','Electronics',NULL,NULL),(2,'upload/images/luffy.png','One Piece Luffy Gear 5 Figure','toy',NULL,NULL),(3,NULL,'Dell XPS 13 Laptop','Electronics','Like new, good battery','https://example.com/laptop.jpg');
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
  `employee_code` varchar(20) DEFAULT NULL,
  `admin_role` enum('SUPER_ADMIN','MODERATOR','SUPPORT') NOT NULL,
  `banned` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `cccd` (`cccd`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (10,'chuvanan','123',NULL,0.00,NULL,NULL,NULL,'bodoiqua189@gmail.com','chu van an',NULL,'seller',NULL,NULL,NULL,'','SUPER_ADMIN',_binary '\0'),(12,'lethanhtung','lethanhtung',NULL,0.00,NULL,NULL,NULL,'giaovien895@gmail.com','le thanh tung',NULL,'seller',NULL,NULL,NULL,'','SUPER_ADMIN',_binary '\0'),(14,'ok','112233',NULL,0.00,NULL,NULL,NULL,'val261263@gmail.com','ok',NULL,'seller',_binary '\0','Shop Test','TAX001','','SUPER_ADMIN',_binary '\0'),(15,'phanbuom','phanbuom1',NULL,0.00,NULL,NULL,NULL,'faverrices@gmail.com','Nguyễn Hà Phan',NULL,'admin',NULL,NULL,NULL,'ADM001','SUPER_ADMIN',_binary '\0'),(16,'tung','tung',NULL,0.00,NULL,NULL,NULL,'tung@gmail.com','tung',NULL,'user',NULL,NULL,NULL,NULL,'SUPER_ADMIN',_binary '\0'),(17,'okoo','ok',NULL,0.00,NULL,NULL,NULL,'ok@gmail.com','okoo',NULL,'user',NULL,NULL,NULL,NULL,'SUPER_ADMIN',_binary '\0');
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

-- Dump completed on 2026-05-20  0:57:56
