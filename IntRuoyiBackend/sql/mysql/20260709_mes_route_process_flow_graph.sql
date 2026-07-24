-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_mes_base_schema; type=schema; riskLevel=medium
-- MES 工艺路线流转关系图：新增工序有向边表与画布布局表，用于表达串行、并行分支与汇合关系。
-- Rollback: DROP TABLE `mes_pro_route_process_flow_layout`; DROP TABLE `mes_pro_route_process_flow_edge`;

DROP PROCEDURE IF EXISTS intruoyi_create_mes_route_process_flow_graph;

DELIMITER //

CREATE PROCEDURE intruoyi_create_mes_route_process_flow_graph()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route process flow graph migration missing mes_pro_route_process';
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_route_process_flow_edge` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `graph_version` bigint NOT NULL DEFAULT 1 COMMENT '关系图版本',
    `source_route_process_id` bigint NOT NULL COMMENT '前置路线工序ID',
    `target_route_process_id` bigint NOT NULL COMMENT '后置路线工序ID',
    `relation_type` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '关系类型：NORMAL',
    `sort` int DEFAULT NULL COMMENT '同一来源工序的出边排序',
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_route_process_flow_edge` (`tenant_id`, `route_id`, `source_route_process_id`, `target_route_process_id`, `deleted`),
    KEY `idx_mes_route_process_flow_edge_route` (`tenant_id`, `route_id`, `graph_version`),
    KEY `idx_mes_route_process_flow_edge_source` (`tenant_id`, `source_route_process_id`),
    KEY `idx_mes_route_process_flow_edge_target` (`tenant_id`, `target_route_process_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线工序流转关系边';

  CREATE TABLE IF NOT EXISTS `mes_pro_route_process_flow_layout` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
    `x` int NOT NULL DEFAULT 0 COMMENT '画布X坐标',
    `y` int NOT NULL DEFAULT 0 COMMENT '画布Y坐标',
    `width` int DEFAULT NULL COMMENT '节点宽度',
    `height` int DEFAULT NULL COMMENT '节点高度',
    `graph_version` bigint NOT NULL DEFAULT 1 COMMENT '关系图版本',
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_route_process_flow_layout` (`tenant_id`, `route_id`, `route_process_id`, `deleted`),
    KEY `idx_mes_route_process_flow_layout_route` (`tenant_id`, `route_id`, `graph_version`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线工序流转关系图布局';
END//

DELIMITER ;

CALL intruoyi_create_mes_route_process_flow_graph();

DROP PROCEDURE IF EXISTS intruoyi_create_mes_route_process_flow_graph;
