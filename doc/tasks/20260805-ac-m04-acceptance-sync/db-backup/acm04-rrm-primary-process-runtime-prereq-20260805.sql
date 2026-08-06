
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
DROP TABLE IF EXISTS `mes_pro_process_pool_team_leader_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_process_pool_team_leader_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `leader_user_id` bigint NOT NULL COMMENT '班组长用户ID',
  `leader_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班组长类型：PRODUCTION/PQC',
  `scope_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '负责范围类型：EMPLOYEE/PROCESS/WORKSTATION',
  `employee_user_id` bigint DEFAULT NULL COMMENT '负责员工用户ID',
  `process_id` bigint DEFAULT NULL COMMENT '负责工序ID',
  `workstation_id` bigint DEFAULT NULL COMMENT '负责工作站ID',
  `production_line_id` bigint DEFAULT NULL COMMENT 'è´Ÿè´£ç”Ÿäº§çº¿ID',
  `equipment_id` bigint DEFAULT NULL COMMENT 'è´Ÿè´£è®¾å¤‡ID',
  `work_order_id` bigint DEFAULT NULL COMMENT 'è´Ÿè´£ç”Ÿäº§è®¢å•ID',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pp_tl_scope_employee` (`tenant_id`,`leader_user_id`,`employee_user_id`),
  KEY `idx_mes_pp_tl_scope_process` (`tenant_id`,`leader_user_id`,`process_id`),
  KEY `idx_mes_pp_tl_scope_workstation` (`tenant_id`,`leader_user_id`,`workstation_id`),
  KEY `idx_mes_pp_tl_scope_line` (`tenant_id`,`leader_user_id`,`production_line_id`),
  KEY `idx_mes_pp_tl_scope_equipment` (`tenant_id`,`leader_user_id`,`equipment_id`),
  KEY `idx_mes_pp_tl_scope_order` (`tenant_id`,`leader_user_id`,`work_order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=980039 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组长负责范围';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `mes_pro_process_pool_team_leader_scope` WRITE;
/*!40000 ALTER TABLE `mes_pro_process_pool_team_leader_scope` DISABLE KEYS */;
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980011,914520,'PRODUCTION','PROCESS',NULL,980002,NULL,NULL,NULL,NULL,_binary '','TLW P6 process scope','codex','2026-08-01 06:43:50','codex','2026-08-01 06:43:50',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980012,914520,'PRODUCTION','EMPLOYEE',914520,NULL,NULL,NULL,NULL,NULL,_binary '','TLW P6 employee scope','codex','2026-08-01 06:43:50','codex','2026-08-01 06:43:50',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980013,512,'PQC','EMPLOYEE',659,NULL,NULL,NULL,NULL,NULL,_binary '','RRM M6 local E2E fixture: PQC leader huzonggang to employee shangmengying for pressure pump V21 actual employee switch','codex-rrm-m6','2026-08-02 21:11:35','codex-rrm-m6','2026-08-02 21:11:35',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980024,914524,'PQC','EMPLOYEE',914524,NULL,NULL,NULL,NULL,NULL,_binary '','20260804-pqc-fill-fullscreen-toggle','20260804-pqc-fill-fullscreen-toggle','2026-08-04 23:05:38','20260804-pqc-fill-fullscreen-toggle','2026-08-04 23:05:38',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980032,1520,'PRODUCTION','EMPLOYEE',964,NULL,NULL,NULL,NULL,NULL,_binary '','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980033,1520,'PRODUCTION','PROCESS',NULL,922986,NULL,NULL,NULL,NULL,_binary '','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980034,1520,'PRODUCTION','PROCESS',NULL,922987,NULL,NULL,NULL,NULL,_binary '','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980035,1520,'PRODUCTION','WORKSTATION',NULL,NULL,980008,NULL,NULL,NULL,_binary '','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980036,1520,'PRODUCTION','WORKSTATION',NULL,NULL,980009,NULL,NULL,NULL,_binary '','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980037,1520,'PRODUCTION','EQUIPMENT',NULL,NULL,NULL,NULL,41,NULL,_binary '','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_leader_scope` (`id`, `leader_user_id`, `leader_type`, `scope_type`, `employee_user_id`, `process_id`, `workstation_id`, `production_line_id`, `equipment_id`, `work_order_id`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980038,1520,'PRODUCTION','ORDER',NULL,NULL,NULL,NULL,NULL,980008,_binary '','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_process_pool_team_leader_scope` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `mes_pro_process_pool_team_process_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_process_pool_team_process_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `leader_user_id` bigint NOT NULL COMMENT '生产组长用户ID',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `device_id` bigint NOT NULL COMMENT '班组设备ID',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `disabled_at` datetime DEFAULT NULL COMMENT '禁用时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pp_team_process_device` (`tenant_id`,`leader_user_id`,`process_id`,`device_id`,`deleted`),
  KEY `idx_mes_pp_team_process_device` (`tenant_id`,`process_id`,`device_id`,`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组工序设备关系';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `mes_pro_process_pool_team_process_device` WRITE;
/*!40000 ALTER TABLE `mes_pro_process_pool_team_process_device` DISABLE KEYS */;
INSERT INTO `mes_pro_process_pool_team_process_device` (`id`, `leader_user_id`, `process_id`, `device_id`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (9,914520,980002,980005,_binary '',NULL,NULL,'914520','2026-08-05 15:18:32','914520','2026-08-05 15:18:32',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_process_device` (`id`, `leader_user_id`, `process_id`, `device_id`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (13,1520,922986,41,_binary '',NULL,'rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_process_device` (`id`, `leader_user_id`, `process_id`, `device_id`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (14,1520,922987,41,_binary '',NULL,'rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_process_pool_team_process_device` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `mes_pro_process_pool_team_employee_binding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_process_pool_team_employee_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `leader_user_id` bigint NOT NULL COMMENT '班组长用户ID',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `employee_profile_id` bigint DEFAULT NULL COMMENT '班组员工档案ID',
  `employee_user_id` bigint DEFAULT NULL COMMENT '员工系统用户ID；临时工为空',
  `display_name_snapshot` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绑定时员工显示名快照，用于历史报工追溯',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `disabled_at` datetime DEFAULT NULL COMMENT '禁用时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pp_team_employee_binding` (`tenant_id`,`leader_user_id`,`process_id`,`employee_user_id`,`deleted`),
  KEY `idx_mes_pp_team_employee_candidate` (`tenant_id`,`process_id`,`employee_user_id`,`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组员工候选绑定';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `mes_pro_process_pool_team_employee_binding` WRITE;
/*!40000 ALTER TABLE `mes_pro_process_pool_team_employee_binding` DISABLE KEYS */;
INSERT INTO `mes_pro_process_pool_team_employee_binding` (`id`, `leader_user_id`, `process_id`, `employee_profile_id`, `employee_user_id`, `display_name_snapshot`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (12,914525,980008,980015,914527,NULL,_binary '',NULL,'ACD04-20260805 fixture employee binding','codex-acd04','2026-08-05 14:43:03','codex-acd04','2026-08-05 15:03:42',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_binding` (`id`, `leader_user_id`, `process_id`, `employee_profile_id`, `employee_user_id`, `display_name_snapshot`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (13,914525,980009,980015,914527,NULL,_binary '',NULL,'ACD04-20260805 fixture employee binding','codex-acd04','2026-08-05 14:43:03','codex-acd04','2026-08-05 15:03:42',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_binding` (`id`, `leader_user_id`, `process_id`, `employee_profile_id`, `employee_user_id`, `display_name_snapshot`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (14,914520,980002,980017,NULL,'PPM-20260805065131-临时工',_binary '',NULL,NULL,'914520','2026-08-05 14:51:46','914520','2026-08-05 14:51:46',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_binding` (`id`, `leader_user_id`, `process_id`, `employee_profile_id`, `employee_user_id`, `display_name_snapshot`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (15,914520,980002,980020,NULL,'PPM-151308-临时工',_binary '',NULL,NULL,'914520','2026-08-05 15:18:32','914520','2026-08-05 15:18:32',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_binding` (`id`, `leader_user_id`, `process_id`, `employee_profile_id`, `employee_user_id`, `display_name_snapshot`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (19,1520,922986,980022,964,'刘悦悦',_binary '',NULL,'rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_employee_binding` (`id`, `leader_user_id`, `process_id`, `employee_profile_id`, `employee_user_id`, `display_name_snapshot`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (20,1520,922987,980022,964,'刘悦悦',_binary '',NULL,'rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_process_pool_team_employee_binding` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `mes_pro_process_pool_team_employee_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_process_pool_team_employee_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `leader_user_id` bigint NOT NULL COMMENT '生产组长用户ID',
  `system_user_id` bigint DEFAULT NULL COMMENT '系统用户ID；临时工为空',
  `employee_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '员工编号',
  `employee_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '员工姓名',
  `display_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产人员显示名；同一组长有效员工不可重复',
  `employee_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '员工来源：FORMAL/TEMPORARY',
  `signature_password_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '临时工电子签名密码哈希；正式工为空并使用系统用户原电子签名密码',
  `signature_password_updated_at` datetime DEFAULT NULL COMMENT '临时工电子签名密码更新时间',
  `active_display_name` varchar(128) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (if((`enabled` = 0x01),coalesce(`display_name`,`employee_name`),NULL)) STORED COMMENT '有效员工显示名唯一键辅助列',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `disabled_at` datetime DEFAULT NULL COMMENT '禁用时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pp_team_employee_profile` (`tenant_id`,`leader_user_id`,`employee_code`,`deleted`),
  UNIQUE KEY `uk_mes_pp_team_employee_active_display_name` (`tenant_id`,`leader_user_id`,`active_display_name`,`deleted`),
  KEY `idx_mes_pp_team_employee_profile_user` (`tenant_id`,`leader_user_id`,`system_user_id`,`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=980024 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组员工档案';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `mes_pro_process_pool_team_employee_profile` WRITE;
/*!40000 ALTER TABLE `mes_pro_process_pool_team_employee_profile` DISABLE KEYS */;
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980014,914520,914520,'TLW-20260731-EMP-001','TLW-20260731-Employee','TLW-20260731-Employee','SYSTEM',NULL,NULL,_binary '',NULL,'TLW P6 fixture','codex','2026-08-01 06:43:50','codex','2026-08-05 14:32:04',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980015,914525,914527,'ACD04-20260805-EMP','ACD04 Fixture Worker',NULL,'FORMAL',NULL,NULL,_binary '',NULL,'ACD04-20260805 fixture employee profile','codex-acd04','2026-08-05 14:43:03','codex-acd04','2026-08-05 15:03:42',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980016,914520,914528,'USER-914528','PPM-20260805065131-正式工','PPM-20260805065131-正式工','FORMAL',NULL,NULL,_binary '',NULL,NULL,'914520','2026-08-05 14:51:45','914520','2026-08-05 14:51:45',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980017,914520,NULL,'TMP-1785912705644','PPM-20260805065131-临时工','PPM-20260805065131-临时工','TEMPORARY','$2a$04$OGwYjGCoCsGhEx7bvtyjWuOREQZX/bP7RPN81Xgr/DHJ.UtBbGGWO','2026-08-05 14:51:46',_binary '',NULL,NULL,'914520','2026-08-05 14:51:46','914520','2026-08-05 14:51:46',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980019,914520,914529,'USER-914529','PPM-151308-正式工','PPM-151308-正式工','FORMAL',NULL,NULL,_binary '\0','2026-08-05 15:18:35',NULL,'914520','2026-08-05 15:18:31','914520','2026-08-05 15:18:35',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980020,914520,NULL,'TMP-1785914311082','PPM-151308-临时工','PPM-151308-临时工','TEMPORARY','$2a$04$STEd6fog8C32JJnS6ZE0PeY0nsYazY0h3bKXxfVxkijK23CNOM/C2','2026-08-05 15:18:33',_binary '\0','2026-08-05 15:18:34',NULL,'914520','2026-08-05 15:18:31','914520','2026-08-05 15:18:34',_binary '\0',122);
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980022,1520,964,'USER-964','刘悦悦','刘悦悦','FORMAL',NULL,NULL,_binary '',NULL,'rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_employee_profile` (`id`, `leader_user_id`, `system_user_id`, `employee_code`, `employee_name`, `display_name`, `employee_type`, `signature_password_hash`, `signature_password_updated_at`, `enabled`, `disabled_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980023,1,NULL,'TMP-1785945170345','112','112','TEMPORARY','$2a$04$X9qBJn5kPOVd96DKqQFf/.lw9JS75jF84se9nsQWwaJ.PFvjV3lou','2026-08-05 23:52:50',_binary '','2026-08-05 23:54:47',NULL,'1','2026-08-05 23:52:50','1','2026-08-05 23:55:20',_binary '\0',1);
/*!40000 ALTER TABLE `mes_pro_process_pool_team_employee_profile` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `mes_pro_process_pool_team_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mes_pro_process_pool_team_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `leader_user_id` bigint NOT NULL COMMENT '生产组长用户ID',
  `device_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备编号',
  `device_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备名称',
  `device_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备状态：ENABLED/REPAIRING/DISABLED',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `status_changed_at` datetime NOT NULL COMMENT '状态变更时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pp_team_device` (`tenant_id`,`leader_user_id`,`device_code`,`deleted`),
  KEY `idx_mes_pp_team_device_status` (`tenant_id`,`leader_user_id`,`device_status`,`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=980006 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组设备';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `mes_pro_process_pool_team_device` WRITE;
/*!40000 ALTER TABLE `mes_pro_process_pool_team_device` DISABLE KEYS */;
INSERT INTO `mes_pro_process_pool_team_device` (`id`, `leader_user_id`, `device_code`, `device_name`, `device_status`, `enabled`, `status_changed_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (41,1520,'A03190','球囊成型机','ENABLED',_binary '','2026-08-05 19:59:58','rrm-ac-m04-runtime-prereq','rrm-acm04','2026-08-05 19:59:58','rrm-acm04','2026-08-05 19:59:58',_binary '\0',1);
INSERT INTO `mes_pro_process_pool_team_device` (`id`, `leader_user_id`, `device_code`, `device_name`, `device_status`, `enabled`, `status_changed_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (980005,914520,'TLW-20260731-DEV-001','TLW-20260731-Device','ENABLED',_binary '','2026-08-05 15:18:32','TLW P6 fixture; id aligned with mes_dv_machinery.id','codex','2026-08-01 06:43:50','914520','2026-08-05 15:18:32',_binary '\0',122);
/*!40000 ALTER TABLE `mes_pro_process_pool_team_device` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
