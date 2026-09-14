-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_mes_base_schema,20260513_dcc_base_schema; type=schema; riskLevel=medium
-- MES route to DCC project-code formal binding schema.
-- Safe to run repeatedly: creates the missing relation table only and does not modify business data.

DROP PROCEDURE IF EXISTS migrate_mes_route_dcc_project_binding_schema;
DELIMITER $$
CREATE PROCEDURE migrate_mes_route_dcc_project_binding_schema()
BEGIN
  DECLARE v_missing_table_count int DEFAULT 0;

  SELECT COUNT(1)
    INTO v_missing_table_count
    FROM (
      SELECT 'mes_pro_route' AS table_name
      UNION ALL SELECT 'dcc_project_code'
    ) required_table
   WHERE NOT EXISTS (
     SELECT 1
       FROM information_schema.tables existing_table
      WHERE existing_table.table_schema = DATABASE()
        AND existing_table.table_name = required_table.table_name
   );

  IF v_missing_table_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'route-DCC binding requires MES route and DCC project-code base tables';
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_route_dcc_project_binding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `dcc_project_code_id` bigint NOT NULL COMMENT 'DCC项目代码ID',
    `version` bigint NOT NULL COMMENT '同租户同路线单调递增版本',
    `active_route_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `deleted` = b'0' THEN `route_id` ELSE NULL END) STORED COMMENT '未删除当前路线唯一键',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_route_dcc_current` (`tenant_id`, `active_route_id`),
    UNIQUE KEY `uk_mes_pro_route_dcc_history_version` (`tenant_id`, `route_id`, `version`),
    KEY `idx_mes_pro_route_dcc_project` (`tenant_id`, `dcc_project_code_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES工艺路线与DCC项目代码正式关系';
END$$
DELIMITER ;

CALL migrate_mes_route_dcc_project_binding_schema();

DROP PROCEDURE IF EXISTS migrate_mes_route_dcc_project_binding_schema;
