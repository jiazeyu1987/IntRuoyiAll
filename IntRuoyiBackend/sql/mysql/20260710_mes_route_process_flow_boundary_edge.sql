-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260709_mes_route_process_flow_graph; type=schema; riskLevel=medium
-- MES 工艺路线流转关系图：新增 START / END 边界节点关系表，移除阻止多前置汇合的旧目标唯一索引，仅回填已有真实关系且首尾唯一的路线。
-- Rollback: DROP TABLE `mes_pro_route_process_flow_boundary_edge`;
-- 回滚不恢复 uk_mes_route_process_flow_target：该旧索引与多前置汇合正式模型冲突，且恢复时可能被已保存的合法汇合关系阻断。

DROP PROCEDURE IF EXISTS intruoyi_create_mes_route_process_flow_boundary_edge;

DELIMITER //

CREATE PROCEDURE intruoyi_create_mes_route_process_flow_boundary_edge()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_edge'
  ) OR NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_layout'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'route process boundary edge migration missing flow graph tables';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_edge'
      AND index_name = 'uk_mes_route_process_flow_target'
  ) THEN
    ALTER TABLE `mes_pro_route_process_flow_edge`
      DROP INDEX `uk_mes_route_process_flow_target`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_edge'
      AND index_name = 'idx_mes_route_process_flow_edge_target'
  ) THEN
    ALTER TABLE `mes_pro_route_process_flow_edge`
      ADD INDEX `idx_mes_route_process_flow_edge_target` (`tenant_id`, `target_route_process_id`);
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_route_process_flow_boundary_edge` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `graph_version` bigint NOT NULL DEFAULT 1 COMMENT '关系图版本',
    `boundary_type` varchar(16) NOT NULL COMMENT '边界类型：START、END',
    `route_process_id` bigint NOT NULL COMMENT '关联路线工序ID',
    `sort` int DEFAULT NULL COMMENT '同一边界的关系排序',
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_route_process_flow_boundary_edge` (`tenant_id`, `route_id`, `boundary_type`, `route_process_id`, `deleted`),
    KEY `idx_mes_route_process_flow_boundary_edge_route` (`tenant_id`, `route_id`, `graph_version`),
    KEY `idx_mes_route_process_flow_boundary_edge_process` (`tenant_id`, `route_process_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线边界节点关系';

  INSERT INTO `mes_pro_route_process_flow_boundary_edge`
    (`route_id`, `graph_version`, `boundary_type`, `route_process_id`, `sort`,
     `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT starts.route_id,
         versions.graph_version,
         'START' AS `boundary_type`,
         starts.route_process_id,
         1,
         '', NOW(), '', NOW(), b'0', starts.tenant_id
  FROM (
    SELECT route_process.tenant_id,
           route_process.route_id,
           MIN(route_process.id) AS route_process_id
    FROM mes_pro_route_process route_process
    WHERE route_process.deleted = b'0'
      AND EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` real_edge
        WHERE real_edge.tenant_id = route_process.tenant_id
          AND real_edge.route_id = route_process.route_id
          AND real_edge.deleted = b'0'
      )
      AND NOT EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` incoming_edge
        WHERE incoming_edge.tenant_id = route_process.tenant_id
          AND incoming_edge.route_id = route_process.route_id
          AND incoming_edge.target_route_process_id = route_process.id
          AND incoming_edge.deleted = b'0'
      )
    GROUP BY route_process.tenant_id, route_process.route_id
    HAVING COUNT(*) = 1
  ) starts
  INNER JOIN (
    SELECT route_process.tenant_id,
           route_process.route_id,
           MIN(route_process.id) AS route_process_id
    FROM mes_pro_route_process route_process
    WHERE route_process.deleted = b'0'
      AND EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` real_edge
        WHERE real_edge.tenant_id = route_process.tenant_id
          AND real_edge.route_id = route_process.route_id
          AND real_edge.deleted = b'0'
      )
      AND NOT EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` outgoing_edge
        WHERE outgoing_edge.tenant_id = route_process.tenant_id
          AND outgoing_edge.route_id = route_process.route_id
          AND outgoing_edge.source_route_process_id = route_process.id
          AND outgoing_edge.deleted = b'0'
      )
    GROUP BY route_process.tenant_id, route_process.route_id
    HAVING COUNT(*) = 1
  ) ends ON ends.tenant_id = starts.tenant_id AND ends.route_id = starts.route_id
  INNER JOIN (
    SELECT edge.tenant_id,
           edge.route_id,
           GREATEST(MAX(edge.graph_version), COALESCE(MAX(layout.graph_version), 0)) AS graph_version
    FROM `mes_pro_route_process_flow_edge` edge
    LEFT JOIN `mes_pro_route_process_flow_layout` layout
      ON layout.tenant_id = edge.tenant_id
     AND layout.route_id = edge.route_id
     AND layout.deleted = b'0'
    WHERE edge.deleted = b'0'
    GROUP BY edge.tenant_id, edge.route_id
  ) versions ON versions.tenant_id = starts.tenant_id AND versions.route_id = starts.route_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `mes_pro_route_process_flow_boundary_edge` existing_edge
    WHERE existing_edge.tenant_id = starts.tenant_id
      AND existing_edge.route_id = starts.route_id
      AND existing_edge.boundary_type = 'START'
      AND existing_edge.deleted = b'0'
  );

  INSERT INTO `mes_pro_route_process_flow_boundary_edge`
    (`route_id`, `graph_version`, `boundary_type`, `route_process_id`, `sort`,
     `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT ends.route_id,
         versions.graph_version,
         'END' AS `boundary_type`,
         ends.route_process_id,
         1,
         '', NOW(), '', NOW(), b'0', ends.tenant_id
  FROM (
    SELECT route_process.tenant_id,
           route_process.route_id,
           MIN(route_process.id) AS route_process_id
    FROM mes_pro_route_process route_process
    WHERE route_process.deleted = b'0'
      AND EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` real_edge
        WHERE real_edge.tenant_id = route_process.tenant_id
          AND real_edge.route_id = route_process.route_id
          AND real_edge.deleted = b'0'
      )
      AND NOT EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` incoming_edge
        WHERE incoming_edge.tenant_id = route_process.tenant_id
          AND incoming_edge.route_id = route_process.route_id
          AND incoming_edge.target_route_process_id = route_process.id
          AND incoming_edge.deleted = b'0'
      )
    GROUP BY route_process.tenant_id, route_process.route_id
    HAVING COUNT(*) = 1
  ) starts
  INNER JOIN (
    SELECT route_process.tenant_id,
           route_process.route_id,
           MIN(route_process.id) AS route_process_id
    FROM mes_pro_route_process route_process
    WHERE route_process.deleted = b'0'
      AND EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` real_edge
        WHERE real_edge.tenant_id = route_process.tenant_id
          AND real_edge.route_id = route_process.route_id
          AND real_edge.deleted = b'0'
      )
      AND NOT EXISTS (
        SELECT 1
        FROM `mes_pro_route_process_flow_edge` outgoing_edge
        WHERE outgoing_edge.tenant_id = route_process.tenant_id
          AND outgoing_edge.route_id = route_process.route_id
          AND outgoing_edge.source_route_process_id = route_process.id
          AND outgoing_edge.deleted = b'0'
      )
    GROUP BY route_process.tenant_id, route_process.route_id
    HAVING COUNT(*) = 1
  ) ends ON ends.tenant_id = starts.tenant_id AND ends.route_id = starts.route_id
  INNER JOIN (
    SELECT edge.tenant_id,
           edge.route_id,
           GREATEST(MAX(edge.graph_version), COALESCE(MAX(layout.graph_version), 0)) AS graph_version
    FROM `mes_pro_route_process_flow_edge` edge
    LEFT JOIN `mes_pro_route_process_flow_layout` layout
      ON layout.tenant_id = edge.tenant_id
     AND layout.route_id = edge.route_id
     AND layout.deleted = b'0'
    WHERE edge.deleted = b'0'
    GROUP BY edge.tenant_id, edge.route_id
  ) versions ON versions.tenant_id = ends.tenant_id AND versions.route_id = ends.route_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `mes_pro_route_process_flow_boundary_edge` existing_edge
    WHERE existing_edge.tenant_id = ends.tenant_id
      AND existing_edge.route_id = ends.route_id
      AND existing_edge.boundary_type = 'END'
      AND existing_edge.deleted = b'0'
  );
END//

DELIMITER ;

CALL intruoyi_create_mes_route_process_flow_boundary_edge();

DROP PROCEDURE IF EXISTS intruoyi_create_mes_route_process_flow_boundary_edge;
