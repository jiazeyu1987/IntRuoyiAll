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
-- Dumping data for table `mes_pro_process_pool_team_leader_scope`
--
-- WHERE:  tenant_id=1 AND deleted=0 AND leader_type=0x505143 AND (leader_user_id IN (512,914524) OR employee_user_id=914524)

LOCK TABLES `mes_pro_process_pool_team_leader_scope` WRITE;
/*!40000 ALTER TABLE `mes_pro_process_pool_team_leader_scope` DISABLE KEYS */;
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980013,512,'PQC','EMPLOYEE',659,NULL,NULL,NULL,NULL,NULL,_binary '','RRM M6 local E2E fixture: PQC leader huzonggang to employee shangmengying for pressure pump V21 actual employee switch','codex-rrm-m6','2026-08-02 21:11:35','codex-rrm-m6','2026-08-02 21:11:35',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980024,914524,'PQC','EMPLOYEE',914524,NULL,NULL,NULL,NULL,NULL,_binary '','20260804-pqc-fill-fullscreen-toggle','20260804-pqc-fill-fullscreen-toggle','2026-08-04 23:05:38','20260804-pqc-fill-fullscreen-toggle','2026-08-04 23:05:38',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_process_pool_team_leader_scope` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-06  5:14:20
