-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: gateway01.ap-southeast-1.prod.aws.tidbcloud.com    Database: auction_system
-- ------------------------------------------------------
-- Server version	8.0.11-TiDB-v8.5.3-serverless

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
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_general_ci DEFAULT NULL,
  `start_price` double DEFAULT NULL,
  `current_price` double DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction_item`
--

LOCK TABLES `auction_item` WRITE;
/*!40000 ALTER TABLE `auction_item` DISABLE KEYS */;
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
  `current_price` decimal(15,2) NOT NULL DEFAULT '0.00',
  `end_time` datetime(6) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `status` enum('ACTIVE','ENDED','PAID','CANCELED','COMING','DRAFT') COLLATE utf8mb4_general_ci NOT NULL,
  `product_id` int DEFAULT NULL,
  `item_id` int DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `approved_by_admin_id` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `reject_reason` text COLLATE utf8mb4_general_ci DEFAULT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `rejected_by_admin_id` int DEFAULT NULL,
  `starting_price` decimal(15,2) NOT NULL DEFAULT '0.00',
  `step_price` decimal(15,2) NOT NULL DEFAULT '0.00',
  `seller_id` int DEFAULT NULL,
  `apply_min_rate` bit(1) DEFAULT b'0',
  `min_rate` decimal(15,2) DEFAULT '0.00',
  `winner_id` int DEFAULT NULL,
  `highest_bidder_id` int DEFAULT NULL,
  `reserve_price` decimal(15,2) DEFAULT NULL,
  `delivery_address` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `delivery_note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `delivery_phone` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `delivery_recipient` varchar(150) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `delivery_submitted_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_auction_product_id` (`product_id`),
  KEY `idx_auction_item_id` (`item_id`),
  KEY `idx_auction_seller_id` (`seller_id`),
  KEY `idx_auction_winner_id` (`winner_id`),
  KEY `idx_auction_highest_bidder_id` (`highest_bidder_id`),
  KEY `idx_auction_approved_by_admin_id` (`approved_by_admin_id`),
  KEY `idx_auction_rejected_by_admin_id` (`rejected_by_admin_id`),
  KEY `idx_session_status_start` (`status`,`start_time`),
  KEY `idx_session_status_end` (`status`,`end_time`),
  KEY `idx_session_seller_status` (`seller_id`,`status`),
  CONSTRAINT `fk_auction_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_auction_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_auction_seller` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_auction_winner` FOREIGN KEY (`winner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_auction_highest_bidder` FOREIGN KEY (`highest_bidder_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_auction_approved_admin` FOREIGN KEY (`approved_by_admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_auction_rejected_admin` FOREIGN KEY (`rejected_by_admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci AUTO_INCREMENT=60001;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction_sessions`
--

LOCK TABLES `auction_sessions` WRITE;
/*!40000 ALTER TABLE `auction_sessions` DISABLE KEYS */;
INSERT INTO `auction_sessions` VALUES (1,10004000000.00,'2026-08-29 10:24:00.000000','2026-05-28 10:24:00.000000','ACTIVE',NULL,1,'2026-05-28 10:31:25.500262',NULL,'2026-05-28 10:31:25.501259',NULL,NULL,NULL,1000000.00,10000.00,2,_binary '\0',0.00,NULL,5,50000000.00,NULL,NULL,NULL,NULL,NULL),(2,500000000.00,'2026-09-29 10:32:00.000000','2026-07-28 10:32:00.000000','COMING',NULL,2,'2026-05-28 10:34:21.624604',NULL,'2026-05-28 10:34:21.624604',NULL,NULL,NULL,500000000.00,10000.00,2,_binary '\0',0.00,NULL,NULL,1000000000.00,NULL,NULL,NULL,NULL,NULL),(3,31013000000.00,'2026-08-20 10:35:00.000000','2026-05-28 10:35:00.000000','ACTIVE',NULL,3,'2026-05-28 10:38:17.741397',NULL,'2026-05-28 10:38:17.742497',NULL,NULL,NULL,5000000000.00,10000.00,2,_binary '\0',0.00,NULL,5,30000000000.00,NULL,NULL,NULL,NULL,NULL),(4,40000000.00,'2026-08-30 11:08:00.000000','2026-07-29 11:08:00.000000','COMING',NULL,4,'2026-05-29 04:18:19.424069',NULL,'2026-05-29 04:18:19.424585',NULL,NULL,NULL,40000000.00,10000.00,2,_binary '\0',0.00,NULL,NULL,500000000.00,NULL,NULL,NULL,NULL,NULL),(30001,9004000000.00,'2026-07-01 17:00:00.000000','2026-05-28 17:00:00.000000','ACTIVE',NULL,30001,'2026-05-31 09:33:24.850736',NULL,'2026-05-31 09:33:24.851230',NULL,NULL,NULL,9000000000.00,10000.00,2,_binary '\0',0.00,NULL,5,30000000000.00,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `auction_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auto_bid_configs`
--

DROP TABLE IF EXISTS `auto_bid_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auto_bid_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `bidder_id` int NOT NULL,
  `increment` decimal(15,2) NOT NULL,
  `max_bid` decimal(15,2) NOT NULL,
  `session_id` int NOT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci AUTO_INCREMENT=30001;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auto_bid_configs`
--

LOCK TABLES `auto_bid_configs` WRITE;
/*!40000 ALTER TABLE `auto_bid_configs` DISABLE KEYS */;
INSERT INTO `auto_bid_configs` VALUES (1,_binary '\0',5,10000000.00,500000000.00,1),(2,_binary '\0',7,100000000.00,500000000.00,1),(3,_binary '\0',7,10000000.00,2000000000.00,1),(4,_binary '\0',8,10000000.00,1000000000.00,1),(5,_binary '\0',8,10000000.00,1700000000.00,1),(6,_binary '\0',5,10000000.00,2000000000.00,1),(7,_binary '\0',8,100000000.00,2100000000.00,1),(8,_binary '\0',7,100000000.00,5000000000.00,1),(9,_binary '\0',5,20000000.00,5100000000.00,1);
/*!40000 ALTER TABLE `auto_bid_configs` ENABLE KEYS */;
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
  `username` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bid_amount` double DEFAULT NULL,
  `bid_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `bidder_id` int DEFAULT NULL,
  `amount` decimal(38,2) DEFAULT NULL,
  `time` datetime(6) DEFAULT NULL,
  `session_id` int DEFAULT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  KEY `idx_bids_item_id` (`item_id`),
  KEY `idx_bids_bidder_id` (`bidder_id`),
  KEY `idx_bids_session_id` (`session_id`),
  KEY `idx_bid_bidder_session` (`bidder_id`,`session_id`),
  KEY `idx_bid_session_time` (`session_id`,`time`),
  CONSTRAINT `fk_bids_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_bids_bidder` FOREIGN KEY (`bidder_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_bids_session` FOREIGN KEY (`session_id`) REFERENCES `auction_sessions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci AUTO_INCREMENT=60001;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bids`
--

LOCK TABLES `bids` WRITE;
/*!40000 ALTER TABLE `bids` DISABLE KEYS */;
INSERT INTO `bids` VALUES (1,NULL,NULL,NULL,'2026-05-28 12:47:44',5,1200000.00,'2026-05-28 12:47:44.454135',1),(2,NULL,NULL,NULL,'2026-05-28 14:22:39',7,50000000.00,'2026-05-28 14:22:39.032730',1),(3,NULL,NULL,NULL,'2026-05-28 14:45:16',7,10000000000.00,'2026-05-28 14:45:16.093111',3),(4,NULL,NULL,NULL,'2026-05-28 14:46:10',7,20000000000.00,'2026-05-28 14:46:09.743989',3),(5,NULL,NULL,NULL,'2026-05-28 14:49:06',7,31000000000.00,'2026-05-28 14:49:06.294153',3),(6,NULL,NULL,NULL,'2026-05-28 15:08:37',5,52000000.00,'2026-05-28 15:08:36.763037',1),(7,NULL,NULL,NULL,'2026-05-28 15:08:46',5,54000000.00,'2026-05-28 15:08:46.298635',1),(8,NULL,NULL,NULL,'2026-05-28 15:09:03',5,59000000.00,'2026-05-28 15:09:03.328093',1),(9,NULL,NULL,NULL,'2026-05-28 15:18:15',5,61000000.00,'2026-05-28 15:18:15.651093',1),(10,NULL,NULL,NULL,'2026-05-28 15:18:21',5,62000000.00,'2026-05-28 15:18:21.873455',1),(11,NULL,NULL,NULL,'2026-05-28 15:27:50',5,63000000.00,'2026-05-28 15:27:49.945894',1),(12,NULL,NULL,NULL,'2026-05-28 15:47:07',7,100000000.00,'2026-05-28 15:47:07.360244',1),(13,NULL,NULL,NULL,'2026-05-28 16:29:10',5,101000000.00,'2026-05-28 16:29:09.892307',1),(14,NULL,NULL,NULL,'2026-05-28 16:29:39',7,120000000.00,'2026-05-28 16:29:39.355441',1),(15,NULL,NULL,NULL,'2026-05-28 16:30:06',5,125000000.00,'2026-05-28 16:30:06.569115',1),(16,NULL,NULL,NULL,'2026-05-28 16:30:41',7,130000000.00,'2026-05-28 16:30:41.553977',1),(17,NULL,NULL,NULL,'2026-05-28 16:31:02',5,135000000.00,'2026-05-28 16:31:01.985141',1),(18,NULL,NULL,NULL,'2026-05-28 16:31:14',7,140000000.00,'2026-05-28 16:31:13.699108',1),(19,NULL,NULL,NULL,'2026-05-28 16:32:32',5,150000000.00,'2026-05-28 16:32:31.836346',1),(20,NULL,NULL,NULL,'2026-05-28 16:36:08',7,152000000.00,'2026-05-28 16:36:08.496453',1),(21,NULL,NULL,NULL,'2026-05-28 16:36:13',5,162000000.00,'2026-05-28 16:36:13.516454',1),(22,NULL,NULL,NULL,'2026-05-28 16:39:39',5,164000000.00,'2026-05-28 16:39:39.395937',1),(23,NULL,NULL,NULL,'2026-05-28 16:50:21',5,166000000.00,'2026-05-28 16:50:21.242468',1),(24,NULL,NULL,NULL,'2026-05-28 17:39:16',5,500000000.00,'2026-05-28 17:39:16.052654',1),(25,NULL,NULL,NULL,'2026-05-28 17:41:02',7,510000000.00,'2026-05-28 17:41:01.862748',1),(26,NULL,NULL,NULL,'2026-05-28 17:41:08',7,1000000000.00,'2026-05-28 17:41:07.896260',1),(27,NULL,NULL,NULL,'2026-05-28 17:42:46',7,1710000000.00,'2026-05-28 17:42:46.352251',1),(28,NULL,NULL,NULL,'2026-05-28 17:47:48',7,2000000000.00,'2026-05-28 17:47:48.564148',1),(29,NULL,NULL,NULL,'2026-05-28 17:47:52',8,2100000000.00,'2026-05-28 17:47:52.124486',1),(30,NULL,NULL,NULL,'2026-05-28 17:48:32',7,2200000000.00,'2026-05-28 17:48:32.366476',1),(31,NULL,NULL,NULL,'2026-05-28 17:48:48',5,5020000000.00,'2026-05-28 17:48:48.337239',1),(32,NULL,NULL,NULL,'2026-05-28 17:49:13',8,5025000000.00,'2026-05-28 17:49:13.505544',1),(33,NULL,NULL,NULL,'2026-05-28 17:49:18',5,5045000000.00,'2026-05-28 17:49:18.230333',1),(34,NULL,NULL,NULL,'2026-05-28 18:51:51',5,31002000000.00,'2026-05-28 18:51:51.080717',3),(35,NULL,NULL,NULL,'2026-05-28 19:25:58',5,31004000000.00,'2026-05-28 19:25:58.359395',3),(36,NULL,NULL,NULL,'2026-05-28 19:26:05',5,31009000000.00,'2026-05-28 19:26:05.737380',3),(37,NULL,NULL,NULL,'2026-05-28 19:26:13',5,31011000000.00,'2026-05-28 19:26:13.252117',3),(38,NULL,NULL,NULL,'2026-05-29 03:49:29',5,5047000000.00,'2026-05-29 03:49:29.580441',1),(30001,NULL,NULL,NULL,'2026-05-30 15:27:50',5,5048000000.00,'2026-05-30 15:27:50.346277',1),(30002,NULL,NULL,NULL,'2026-05-30 16:17:37',5,5050000000.00,'2026-05-30 16:17:36.135367',1),(30003,NULL,NULL,NULL,'2026-05-30 16:18:00',5,5053000000.00,'2026-05-30 16:17:59.919260',1),(30004,NULL,NULL,NULL,'2026-05-30 16:18:06',5,5055000000.00,'2026-05-30 16:18:05.394582',1),(30005,NULL,NULL,NULL,'2026-05-30 16:44:56',7,10000000000.00,'2026-05-30 16:44:56.143115',1),(30007,NULL,NULL,NULL,'2026-05-31 10:50:09',5,10002000000.00,'2026-05-31 10:50:09.071471',1),(30008,NULL,NULL,NULL,'2026-05-31 10:50:46',5,10004000000.00,'2026-05-31 10:50:45.701963',1),(30009,NULL,NULL,NULL,'2026-05-31 11:06:41',5,9002000000.00,'2026-05-31 11:06:41.551448',30001),(30010,NULL,NULL,NULL,'2026-05-31 11:09:19',5,9004000000.00,'2026-05-31 11:09:19.359811',30001),(30011,NULL,NULL,NULL,'2026-05-31 11:14:03',5,31013000000.00,'2026-05-31 11:14:02.891508',3);
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
  `image_path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `category` varchar(31) COLLATE utf8mb4_general_ci NOT NULL,
  `description` text COLLATE utf8mb4_general_ci DEFAULT NULL,
  `hidden` bit(1) NOT NULL DEFAULT b'0',
  `uuid` varchar(36) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  UNIQUE KEY `uk_items_uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci AUTO_INCREMENT=60001;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `items`
--

LOCK TABLES `items` WRITE;
/*!40000 ALTER TABLE `items` DISABLE KEYS */;
INSERT INTO `items` VALUES (1,NULL,NULL,NULL,'Patek Philippe Grand Complication Skymoon Tourbillon',NULL,NULL,'Art','art','Đồng hồ Patek Philippe Grand Complication Skymoon Tourbillon là siêu phẩm đồng hồ của hãng gây ấn tượng mạnh mẽ với 2 mặt số. Được thiết kế từ 705 bộ phận, chiếc đồng hồ sở hữu lên đến 12 tính năng riêng biệt.\nNgoài ra vỏ đồng hồ được làm từ vàng trắng cao cấp được chạm khắc thủ công. Cùng với đó là kỹ thuật tráng men cloisonne điêu luyện đem lại sự sang trọng, đỉnh cao có một không hai cho sản phẩm.',_binary '\0','b71c6d80-3290-4a40-abc5-64e8c568e449'),(2,NULL,NULL,NULL,'McLAREN 720S SPIDER',NULL,NULL,'Vehicle','vehicle','Following on from the success of the award-winning McLaren 720S Coupé, we created a Spider variant – designed for a truly thrilling drive. The McLaren 720S Spider has a bespoke carbon fibre shell, designed to accommodate its roof mechanism: the Monocage II-S. The car’s retractable hard top is fully electric and near-silent, ready to invite the intoxicating elements of the outside world along for the ride.',_binary '\0','5c9e1c88-6079-4f8e-993e-b07733cff278'),(3,NULL,NULL,NULL,'Red Bull Racing Formula 1 - RB22',NULL,NULL,'Vehicle','vehicle','The car marks a historical transition for Oracle Red Bull Racing as they move away from Honda power units to their own in-house engine division, built in partnership with Ford (Red Bull Ford Powertrains).',_binary '\0','1b5c60ea-6475-4d88-b32d-ddcad49fd810'),(4,NULL,NULL,NULL,'Mona Lisa (La Gioconda)',NULL,NULL,'Art','art','The Mona Lisa is a half-length portrait painted by Italian Renaissance artist Leonardo da Vinci. Believed to depict Lisa Gherardini, the wife of a Florentine merchant, it is famous for the subject\'s enigmatic smile, sfumato technique, and its mysterious history.',_binary '\0','1988c432-56e7-4c1a-9012-b8182052d407'),(30001,NULL,NULL,NULL,'Đồng hồ Audemars Piguet Royal Oak Jumbo Selfwinding Flying Tourbillon',NULL,NULL,'Art','art','Audemars Piguet Royal Oak Jumbo Selfwinding Flying Tourbillon Extra-Thin RD#3 (mã hiệu phổ biến 26670ST) là một trong những kiệt tác đỉnh cao của nghệ thuật chế tác đồng hồ, được thương hiệu Audemars Piguet ra mắt lần đầu năm 2022 nhân dịp kỷ niệm 50 năm bộ sưu tập huyền thoại Royal Oak. Đây là chiếc đồng hồ dáng \"Jumbo\" đầu tiên trong lịch sử được trang bị cơ chế Flying Tourbillon (Tourbillon bay) tự động mà vẫn giữ nguyên được độ mỏng ấn tượng.',_binary '\0','4fa6efda-6ab1-4074-b322-fa6a6b8b73db');
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
  `image_path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_general_ci DEFAULT NULL,
  `image_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
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
  `username` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `balance` decimal(38,2) DEFAULT NULL,
  `cccd` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dob` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pob` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `fullname` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `place_of_birth` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `role` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_business` bit(1) DEFAULT NULL,
  `shop_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tax_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `employee_code` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `admin_role` enum('SUPER_ADMIN','MODERATOR','SUPPORT') COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SUPPORT',
  `banned` bit(1) NOT NULL DEFAULT b'0',
  `avatar_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `frozen_balance` decimal(38,2) DEFAULT NULL,
  `password_set` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`) /*T![clustered_index] CLUSTERED */,
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_cccd` (`cccd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci AUTO_INCREMENT=60001;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2a$12$P3X0FR.w8RWCRR1HS9SA0Ov28JoGlbFJ34d9sVRax2umCZBKyAiGC','Admin',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'admin',NULL,NULL,NULL,NULL,'SUPER_ADMIN',_binary '\0',NULL,NULL,NULL),(2,'seller','$2a$12$ywCJeOPImWw9pTZWAfWUkenfSSr.8kWkGvkp6WRJ7Av2OGE.Oz5L.',NULL,0.00,NULL,NULL,NULL,'seller@gmail.com','Fan',NULL,'seller',NULL,NULL,NULL,NULL,'SUPPORT',_binary '\0',NULL,0.00,_binary ''),(5,'shin25112007','$2a$12$HcsrxRVSTEKqchcmVlWEMuz.Dmo3kDs8dMJi1Vba6B64LZ02aoO3O',NULL,1000000000000000000000499999999.00,NULL,NULL,NULL,'shin25112007@gmail.com','tùng thanh lê',NULL,'seller',NULL,NULL,NULL,NULL,'SUPPORT',_binary '\0',NULL,81032000000.00,_binary '\0'),(6,'blank1','$2a$12$mbFfVuffiInVo4kOAvC/Ku3qhq/MpSxc8OPFWgsPiCgAAJayciOmm',NULL,4000000.00,NULL,NULL,NULL,'abc@gmail.com','minh',NULL,'seller',NULL,NULL,NULL,NULL,'SUPPORT',_binary '\0',NULL,0.00,_binary ''),(7,'ledinhquockhanh942','$2a$12$liSKVViNighAKzh3e/zuFu1IMhiBVov3LJiFVGNfZLBgEuO8N1lwq',NULL,50999999999.00,NULL,NULL,NULL,'ledinhquockhanh942@gmail.com','khanh le dinh quoc',NULL,'bidder',NULL,NULL,NULL,NULL,'SUPPORT',_binary '\0',NULL,9710000000.00,_binary ''),(8,'25021877','$2a$12$ol.oBvaSeYv/bY/ANAlLj.QhvYFyZgjUzlSG/wDCiCh3mzM72fD3m',NULL,9999999999999999999999999.00,NULL,NULL,NULL,'25021877@vnu.edu.vn','25021877 Nguyễn Lê Quang Minh',NULL,'bidder',NULL,NULL,NULL,NULL,'SUPPORT',_binary '\0',NULL,0.00,_binary '\0'),(30001,'25021824','$2a$12$OscLsW0Ap2FlecuRnK76..cKT8nXduGgRqJ9UFFfyUTEJ.GbNl2cy',NULL,0.00,NULL,NULL,NULL,'25021824@vnu.edu.vn','25021824 Lê Đình Quốc Khánh',NULL,'bidder',NULL,NULL,NULL,NULL,'SUPPORT',_binary '\0',NULL,0.00,_binary '\0'),(30002,'bidder1','$2a$12$Ws4cMIt4sA9hUzVeqwDM1eA6uf3RWK4q.Uyu35bnatp.NuDHM6Xaa',NULL,0.00,NULL,NULL,NULL,'bidder1@gmail.com','bidder1',NULL,'bidder',NULL,NULL,NULL,NULL,'SUPPORT',_binary '\0',NULL,0.00,_binary '');
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

-- Dump completed on 2026-05-31 18:35:19
