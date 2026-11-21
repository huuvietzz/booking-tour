-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: localhost    Database: booking_tour
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bookings`
--

DROP TABLE IF EXISTS `bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `booking_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `booking_code` varchar(50) NOT NULL,
  `booking_status` enum('CANCELLED','COMPLETED','CONFIRMED','PENDING') DEFAULT NULL,
  `contact_address` varchar(255) DEFAULT NULL,
  `contact_email` varchar(255) NOT NULL,
  `contact_name` varchar(255) NOT NULL,
  `contact_phone` varchar(20) NOT NULL,
  `note` text,
  `payment_status` enum('PAID','PENDING','REFUNDED') DEFAULT NULL,
  `temp_amount` decimal(15,0) DEFAULT NULL,
  `total_adults` int NOT NULL,
  `total_amount` decimal(15,0) NOT NULL,
  `total_children` int DEFAULT NULL,
  `total_toddlers` int DEFAULT NULL,
  `promotion_id` int DEFAULT NULL,
  `schedule_id` int NOT NULL,
  `user_id` int DEFAULT NULL,
  `payment_token` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`booking_id`),
  UNIQUE KEY `UKq97166k18hklq6ls46osbrftx` (`booking_code`),
  UNIQUE KEY `UKa9n7iihjmehmny8v8eq5ll3qo` (`payment_token`),
  KEY `FKk4byobgkjv3y3952wwpxyep7o` (`promotion_id`),
  KEY `FKer0lq2qsui5vv3qn0i6sm1rom` (`schedule_id`),
  KEY `FKeyog2oic85xg7hsu2je2lx3s6` (`user_id`),
  CONSTRAINT `FKer0lq2qsui5vv3qn0i6sm1rom` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`schedule_id`),
  CONSTRAINT `FKeyog2oic85xg7hsu2je2lx3s6` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKk4byobgkjv3y3952wwpxyep7o` FOREIGN KEY (`promotion_id`) REFERENCES `promotions` (`promotion_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `category_name` varchar(255) NOT NULL,
  `description` text,
  `display_order` int NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `slug` varchar(255) NOT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `UK5hb0nsagspu6fq9ftvpohnwmf` (`display_order`),
  UNIQUE KEY `UKoul14ho7bctbefv8jywp5v3i2` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `favorite_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `tour_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `unique_user_tour` (`user_id`,`tour_id`),
  KEY `FKmmamfjvnw2qm9kb1ke6tq25lc` (`tour_id`),
  CONSTRAINT `FKk7du8b8ewipawnnpg76d55fus` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKmmamfjvnw2qm9kb1ke6tq25lc` FOREIGN KEY (`tour_id`) REFERENCES `tours` (`tour_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `images`
--

DROP TABLE IF EXISTS `images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `images` (
  `image_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `image_url` varchar(500) NOT NULL,
  `tour_id` int NOT NULL,
  PRIMARY KEY (`image_id`),
  KEY `FKq1oajpn71mmqponbjomn8incv` (`tour_id`),
  CONSTRAINT `FKq1oajpn71mmqponbjomn8incv` FOREIGN KEY (`tour_id`) REFERENCES `tours` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `itineraries`
--

DROP TABLE IF EXISTS `itineraries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `itineraries` (
  `itinerary_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `day_number` int NOT NULL,
  `description` text,
  `title` varchar(255) NOT NULL,
  `tour_id` int NOT NULL,
  PRIMARY KEY (`itinerary_id`),
  UNIQUE KEY `unique_tour_day` (`tour_id`,`day_number`),
  CONSTRAINT `FKma0pnbq68uayxgf8w5qejwb3k` FOREIGN KEY (`tour_id`) REFERENCES `tours` (`tour_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `locations`
--

DROP TABLE IF EXISTS `locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `locations` (
  `location_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `description` text,
  `image_url` varchar(500) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `location_name` varchar(255) NOT NULL,
  PRIMARY KEY (`location_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `participants`
--

DROP TABLE IF EXISTS `participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `participants` (
  `participant_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `date_of_birth` date NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `gender` enum('FEMALE','MALE','OTHER') DEFAULT NULL,
  `participant_type` enum('ADULT','CHILD','TODDLER') NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `single_room` bit(1) DEFAULT NULL,
  `booking_id` int NOT NULL,
  PRIMARY KEY (`participant_id`),
  KEY `FKjisrqy9vyd72b78mg9d5y14vd` (`booking_id`),
  CONSTRAINT `FKjisrqy9vyd72b78mg9d5y14vd` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `payment_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `amount` decimal(15,0) NOT NULL,
  `gateway` varchar(50) DEFAULT NULL,
  `method` enum('BANK_TRANSFER','VNPAY') NOT NULL,
  `payment_date` datetime(6) DEFAULT NULL,
  `reference_code` varchar(100) DEFAULT NULL,
  `transaction_id` varchar(100) DEFAULT NULL,
  `transaction_status` enum('FAILED','PENDING','SUCCESS') NOT NULL,
  `booking_id` int NOT NULL,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payments_method_txn` (`method`,`transaction_id`),
  KEY `FKc52o2b1jkxttngufqp3t7jr3h` (`booking_id`),
  CONSTRAINT `FKc52o2b1jkxttngufqp3t7jr3h` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `promotion_tours`
--

DROP TABLE IF EXISTS `promotion_tours`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_tours` (
  `promotion_id` int NOT NULL,
  `tour_id` int NOT NULL,
  PRIMARY KEY (`promotion_id`,`tour_id`),
  KEY `FK4jg4glva6hct6ww1fhngph6gs` (`tour_id`),
  CONSTRAINT `FK4jg4glva6hct6ww1fhngph6gs` FOREIGN KEY (`tour_id`) REFERENCES `tours` (`tour_id`),
  CONSTRAINT `FKqhrw11iiudd3fgpy2030ppr7y` FOREIGN KEY (`promotion_id`) REFERENCES `promotions` (`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `promotions`
--

DROP TABLE IF EXISTS `promotions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotions` (
  `promotion_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `discount_type` enum('FIXED','PERCENT') NOT NULL,
  `discount_value` decimal(15,0) NOT NULL,
  `end_date` datetime(6) NOT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `max_discount_amount` decimal(15,0) DEFAULT NULL,
  `max_uses_per_user` int DEFAULT NULL,
  `min_order_amount` decimal(15,0) DEFAULT NULL,
  `promotion_code` varchar(50) NOT NULL,
  `promotion_name` varchar(255) NOT NULL,
  `quantity` int DEFAULT NULL,
  `start_date` datetime(6) NOT NULL,
  `used_count` int DEFAULT NULL,
  PRIMARY KEY (`promotion_id`),
  UNIQUE KEY `UK3d3vjx83uxeubedcpiuafgllw` (`promotion_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `admin_response` text,
  `comment` text,
  `is_hidden` bit(1) NOT NULL,
  `hidden_reason` varchar(255) DEFAULT NULL,
  `rating` int NOT NULL,
  `booking_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3p9j9vyr1qofbcxju65es206r` (`booking_id`),
  CONSTRAINT `FK28an517hrxtt2bsg93uefugrm` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schedules`
--

DROP TABLE IF EXISTS `schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedules` (
  `schedule_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `adult_price` decimal(15,0) NOT NULL,
  `available_slots` int NOT NULL,
  `child_price` decimal(15,0) NOT NULL,
  `departure_date` date NOT NULL,
  `return_date` date NOT NULL,
  `schedule_code` varchar(50) NOT NULL,
  `single_room_supplement` decimal(15,0) NOT NULL,
  `status` enum('AVAILABLE','CANCELLED','FULL') DEFAULT NULL,
  `toddler_price` decimal(15,0) NOT NULL,
  `total_slots` int NOT NULL,
  `transport` varchar(100) DEFAULT NULL,
  `tour_id` int NOT NULL,
  PRIMARY KEY (`schedule_id`),
  UNIQUE KEY `unique_tour_departure` (`tour_id`,`departure_date`),
  UNIQUE KEY `UKgc0d0jm9xlu9ehlb4lnal0hyg` (`schedule_code`),
  CONSTRAINT `FKg107aofjd5egbg0gjugh17jlx` FOREIGN KEY (`tour_id`) REFERENCES `tours` (`tour_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tokens`
--

DROP TABLE IF EXISTS `tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tokens` (
  `token_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `token` varchar(255) NOT NULL,
  `token_type` enum('EMAIL_VERIFICATION','PASSWORD_RESET') NOT NULL,
  `used` bit(1) DEFAULT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`token_id`),
  UNIQUE KEY `UKna3v9f8s7ucnj16tylrs822qj` (`token`),
  KEY `FK2dylsfo39lgjyqml2tbe0b0ss` (`user_id`),
  CONSTRAINT `FK2dylsfo39lgjyqml2tbe0b0ss` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tours`
--

DROP TABLE IF EXISTS `tours`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tours` (
  `tour_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `average_rating` decimal(3,2) DEFAULT NULL,
  `base_price` decimal(15,0) NOT NULL,
  `departure_location` varchar(100) NOT NULL,
  `description` text,
  `duration` varchar(20) NOT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_featured` bit(1) DEFAULT NULL,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `total_reviews` int DEFAULT NULL,
  `tour_class` enum('BUDGET','GOOD_PRICE','PREMIUM','STANDARD') DEFAULT NULL,
  `tour_code` varchar(50) NOT NULL,
  `tour_name` varchar(255) NOT NULL,
  `view_count` int DEFAULT NULL,
  `category_id` int NOT NULL,
  `location_id` int NOT NULL,
  PRIMARY KEY (`tour_id`),
  UNIQUE KEY `UKr20eechhviuokimbwk1qqs7vn` (`tour_code`),
  KEY `FK4mwn5rnlqad3gi9lm0yddhoy4` (`category_id`),
  KEY `FKa22qtu9dqju2e36is0yd1dwu5` (`location_id`),
  CONSTRAINT `FK4mwn5rnlqad3gi9lm0yddhoy4` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `FKa22qtu9dqju2e36is0yd1dwu5` FOREIGN KEY (`location_id`) REFERENCES `locations` (`location_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `email_verified` bit(1) DEFAULT NULL,
  `full_name` varchar(255) NOT NULL,
  `gender` enum('FEMALE','MALE','OTHER') DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone_number` varchar(20) NOT NULL,
  `role` enum('ADMIN','CUSTOMER','STAFF') DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK9q63snka3mdh91as4io72espi` (`phone_number`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'booking_tour'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-20  3:17:37
