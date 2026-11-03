-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema; type=schema; riskLevel=medium
-- Rollback: ALTER TABLE mes_pro_route_dcc_project_binding DROP INDEX uk_mes_route_project_current, DROP COLUMN active_project_code_id;
-- 不修改业务数据。重复或失效关联必须先由业务确认处理，再重跑此迁移。
DROP PROCEDURE IF EXISTS migrate_mes_route_project_unique;
DELIMITER $$
CREATE PROCEDURE migrate_mes_route_project_unique()
BEGIN
  IF EXISTS (SELECT 1 FROM mes_pro_route_dcc_project_binding WHERE deleted = b'0'
             GROUP BY tenant_id, dcc_project_code_id HAVING COUNT(*) > 1) THEN
    SELECT tenant_id, dcc_project_code_id, GROUP_CONCAT(route_id ORDER BY route_id) AS conflicting_route_ids
      FROM mes_pro_route_dcc_project_binding WHERE deleted = b'0'
      GROUP BY tenant_id, dcc_project_code_id HAVING COUNT(*) > 1;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate project-route bindings: resolve listed conflicts before migration';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_pro_route_dcc_project_binding b
             LEFT JOIN mes_pro_route r ON r.id=b.route_id AND r.tenant_id=b.tenant_id AND r.deleted=b'0'
             WHERE b.deleted=b'0' AND r.id IS NULL) THEN
    SELECT b.tenant_id, b.dcc_project_code_id, b.route_id AS missing_or_deleted_route_id
      FROM mes_pro_route_dcc_project_binding b
      LEFT JOIN mes_pro_route r ON r.id=b.route_id AND r.tenant_id=b.tenant_id AND r.deleted=b'0'
      WHERE b.deleted=b'0' AND r.id IS NULL;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Orphan project-route bindings: resolve listed conflicts before migration';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
                 AND table_name='mes_pro_route_dcc_project_binding' AND column_name='active_project_code_id') THEN
    ALTER TABLE mes_pro_route_dcc_project_binding
      ADD COLUMN active_project_code_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN deleted=b'0' THEN dcc_project_code_id ELSE NULL END) STORED;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
                 AND table_name='mes_pro_route_dcc_project_binding' AND index_name='uk_mes_route_project_current') THEN
    ALTER TABLE mes_pro_route_dcc_project_binding
      ADD UNIQUE KEY uk_mes_route_project_current (tenant_id, active_project_code_id);
  END IF;
END$$
DELIMITER ;
CALL migrate_mes_route_project_unique();
DROP PROCEDURE IF EXISTS migrate_mes_route_project_unique;
