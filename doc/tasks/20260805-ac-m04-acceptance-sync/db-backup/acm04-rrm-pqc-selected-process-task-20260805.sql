-- MySQL dump 10.13  Distrib 8.0.39, for Linux (x86_64)
--
-- Host: localhost    Database: ruoyi-vue-pro
-- ------------------------------------------------------
-- Server version	8.0.39

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
-- Table structure for table `mes_pro_task`
--

DROP TABLE IF EXISTS `mes_pro_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_order_id` bigint DEFAULT NULL,
  `workstation_id` bigint DEFAULT NULL,
  `route_id` bigint DEFAULT NULL,
  `process_id` bigint DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `quantity` decimal(24,6) DEFAULT NULL,
  `produced_quantity` decimal(24,6) DEFAULT NULL,
  `qualify_quantity` decimal(24,6) DEFAULT NULL,
  `unqualify_quantity` decimal(24,6) DEFAULT NULL,
  `changed_quantity` decimal(24,6) DEFAULT NULL,
  `client_id` bigint DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `color_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `finish_date` datetime DEFAULT NULL,
  `cancel_date` datetime DEFAULT NULL,
  `status` int DEFAULT NULL,
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_task_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=981941 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesProTaskDO';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mes_pro_task`
--
-- WHERE:  work_order_id=980008

LOCK TABLES `mes_pro_task` WRITE;
/*!40000 ALTER TABLE `mes_pro_task` DISABLE KEYS */;
INSERT INTO `mes_pro_task` VALUES (981939,'PT-52097','球囊扩张压力泵【10】PCS',980008,980008,922119,922986,902149,10.000000,NULL,NULL,NULL,NULL,NULL,'2026-08-05 08:00:00',1,'2026-08-05 09:00:00','#00AEF3',NULL,NULL,0,'Task-owned RRM PQC source event prerequisite','1','2026-08-05 19:29:43','1','2026-08-05 19:40:51',_binary '\0',1),(981940,'RRM-20260805-PRIMARY-922985','RRM primary process source task 922985',980008,980010,922119,922985,902149,10.000000,NULL,NULL,NULL,NULL,NULL,'2026-08-05 08:00:00',1,'2026-08-05 09:00:00','#00AEF3',NULL,NULL,0,'rrm-ac-m04-pqc-source-task','rrm-acm04','2026-08-06 03:13:06','rrm-acm04','2026-08-06 03:13:06',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_task` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-06  6:51:11
