-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261103_mes_tenant_production_code_rules; type=schema; riskLevel=medium
-- 保留历史租户规则及流水；项目规则在正式项目创建或首次使用时按标准模板初始化。
DROP PROCEDURE IF EXISTS migrate_mes_project_production_codes;
DELIMITER $$
CREATE PROCEDURE migrate_mes_project_production_codes()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_md_auto_code_rule' AND column_name='project_id') THEN
    ALTER TABLE mes_md_auto_code_rule ADD COLUMN project_id BIGINT NULL COMMENT '编号所属产品项目';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_md_auto_code_rule' AND column_name='active_project_rule') THEN
    ALTER TABLE mes_md_auto_code_rule ADD COLUMN active_project_rule VARCHAR(160)
      GENERATED ALWAYS AS (CASE WHEN deleted=b'0' AND project_id IS NOT NULL
        THEN CONCAT(project_id, ':', code) ELSE NULL END) STORED;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_md_auto_code_rule' AND index_name='uk_mes_project_rule') THEN
    ALTER TABLE mes_md_auto_code_rule ADD UNIQUE KEY uk_mes_project_rule (tenant_id,active_project_rule);
  END IF;
END$$
DELIMITER ;
CALL migrate_mes_project_production_codes();
DROP PROCEDURE migrate_mes_project_production_codes;
