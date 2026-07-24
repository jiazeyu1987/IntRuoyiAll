-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 排产 P4：日期维度资源调整

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_resource_adjustment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
  `calendar_date` date NOT NULL COMMENT '生效日期',
  `resource_type` varchar(16) NOT NULL COMMENT '资源类型：MACHINE/WORKER',
  `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
  `workstation_machine_id` bigint DEFAULT NULL COMMENT '工位设备绑定ID',
  `machinery_id` bigint DEFAULT NULL COMMENT '设备ID',
  `available_quantity_override` int DEFAULT NULL COMMENT '设备今日可用数量覆盖',
  `worker_quantity_override` int DEFAULT NULL COMMENT '人工人数覆盖',
  `single_hourly_capacity_override` decimal(24,6) DEFAULT NULL COMMENT '单人小时产能覆盖',
  `shift_hours_override` decimal(24,6) DEFAULT NULL COMMENT '班次小时覆盖',
  `reason` varchar(500) DEFAULT NULL COMMENT '调整原因',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_schedule_resource_adjustment` (`tenant_id`,`calendar_date`,`resource_type`,`route_process_id`,`workstation_machine_id`,`machinery_id`,`deleted`),
  KEY `idx_mes_pro_schedule_resource_adjustment_route` (`tenant_id`,`route_id`,`calendar_date`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 排产日资源调整';
