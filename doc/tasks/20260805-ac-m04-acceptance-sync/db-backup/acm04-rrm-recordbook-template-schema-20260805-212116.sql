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
-- Table structure for table `mes_pro_edhr_recordbook_template`
--

DROP TABLE IF EXISTS `mes_pro_edhr_recordbook_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_edhr_recordbook_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '记录本模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '记录本模板名称',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本',
  `recordbook_type` varchar(64) NOT NULL COMMENT '记录本类型',
  `entry_schema_json` longtext NOT NULL COMMENT '条目字段定义JSON',
  `tag_policy_json` longtext COMMENT '标签策略JSON',
  `status` varchar(32) NOT NULL COMMENT '模板状态：DRAFT/ACTIVE/DISABLED',
  `active_by` bigint DEFAULT NULL COMMENT '启用人',
  `active_at` datetime DEFAULT NULL COMMENT '启用时间',
  `disabled_by` bigint DEFAULT NULL COMMENT '停用人',
  `disabled_at` datetime DEFAULT NULL COMMENT '停用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_recordbook_template_code` (`tenant_id`,`template_code`,`deleted`),
  KEY `idx_mes_pro_edhr_recordbook_template_status` (`tenant_id`,`status`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=980011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MES eDHR记录本模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mes_pro_edhr_recordbook_template`
--
-- WHERE:  id IN (980010,980011)

LOCK TABLES `mes_pro_edhr_recordbook_template` WRITE;
/*!40000 ALTER TABLE `mes_pro_edhr_recordbook_template` DISABLE KEYS */;
INSERT INTO `mes_pro_edhr_recordbook_template` VALUES (980010,'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1','PRODUCTION','[{\"key\":\"fieldValues\",\"label\":\"Field Values\",\"type\":\"text\",\"required\":null,\"min\":null,\"max\":null,\"options\":null},{\"key\":\"defects\",\"label\":\"Defects\",\"type\":\"text\",\"required\":null,\"min\":null,\"max\":null,\"options\":null},{\"key\":\"productionOrder\",\"label\":\"Production Order\",\"type\":\"text\",\"required\":null,\"min\":null,\"max\":null,\"options\":null},{\"key\":\"process\",\"label\":\"Process\",\"type\":\"text\",\"required\":null,\"min\":null,\"max\":null,\"options\":null},{\"key\":\"employee\",\"label\":\"Employee\",\"type\":\"text\",\"required\":null,\"min\":null,\"max\":null,\"options\":null}]',NULL,'ACTIVE',1,'2026-08-05 19:27:10',NULL,NULL,'Activate RRM production recordbook template','1','2026-08-05 19:27:10','1','2026-08-05 19:27:10',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_edhr_recordbook_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mes_pro_edhr_recordbook`
--

DROP TABLE IF EXISTS `mes_pro_edhr_recordbook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_edhr_recordbook` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recordbook_code` varchar(96) NOT NULL COMMENT '记录本编码',
  `recordbook_name` varchar(128) NOT NULL COMMENT '记录本名称',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码快照',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称快照',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本快照',
  `recordbook_type` varchar(64) NOT NULL COMMENT '记录本类型快照',
  `status` varchar(32) NOT NULL COMMENT '记录本状态：OPEN/DISABLED/CLOSED',
  `owner_user_id` bigint DEFAULT NULL COMMENT '责任人',
  `owner_dept_id` bigint DEFAULT NULL COMMENT '责任部门',
  `business_scope` varchar(64) DEFAULT NULL COMMENT '业务范围',
  `business_object_type` varchar(64) DEFAULT NULL COMMENT '业务对象类型',
  `business_object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(96) DEFAULT NULL COMMENT '业务对象编码快照',
  `opened_at` datetime NOT NULL COMMENT '开本时间',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
  `entry_count` int NOT NULL COMMENT '条目数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_recordbook_code` (`tenant_id`,`recordbook_code`,`deleted`),
  KEY `idx_mes_pro_edhr_recordbook_owner` (`tenant_id`,`owner_user_id`,`status`,`deleted`),
  KEY `idx_mes_pro_edhr_recordbook_template` (`tenant_id`,`template_id`,`status`,`deleted`),
  KEY `idx_mes_pro_edhr_recordbook_business` (`tenant_id`,`business_scope`,`business_object_type`,`business_object_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=980012 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MES eDHR记录本';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mes_pro_edhr_recordbook`
--
-- WHERE:  id IN (980010,980011)

LOCK TABLES `mes_pro_edhr_recordbook` WRITE;
/*!40000 ALTER TABLE `mes_pro_edhr_recordbook` DISABLE KEYS */;
INSERT INTO `mes_pro_edhr_recordbook` VALUES (980010,'TLW-20260731-RB-001','TLW-20260731-Recordbook',980009,'TLW-20260731-RB-TPL-001','TLW-20260731-Recordbook Template','V1','PRODUCTION','OPEN',914520,NULL,'MES_PROCESS_POOL','WORK_ORDER',980007,'TLW-20260731-WO-001','2026-08-01 06:43:50',NULL,9,'TLW P6 fixture','codex','2026-08-01 06:43:50','codex-acm04-p0-backfill','2026-08-05 20:42:27',_binary '\0',122),(980011,'RRM-20260801-PP-MO-001-PRODUCTION-RB','RRM production source recordbook',980010,'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1','PRODUCTION','OPEN',964,103,'RRM_E2E','WORK_ORDER',980008,'RRM-20260801-PP-MO-001','2026-08-05 19:27:10',NULL,16,'Task-owned RRM PQC source event prerequisite','1','2026-08-05 19:27:11','codex-acm04-p0-backfill','2026-08-05 20:42:27',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_edhr_recordbook` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-05 21:21:21
