-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_backfill; type=schema; riskLevel=high
-- C015 converges current-main schema only after preflight and explicit zero-repair backfill.

DROP PROCEDURE IF EXISTS migrate_mes_c015_route_dcc_qa_reconciliation_schema;
DELIMITER $$
CREATE PROCEDURE migrate_mes_c015_route_dcc_qa_reconciliation_schema()
BEGIN
  DECLARE v_signature varchar(512) DEFAULT NULL;
  DECLARE v_non_unique int DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
       AND column_name = 'version'
  ) THEN
    IF EXISTS (SELECT 1 FROM mes_pro_route_dcc_project_binding) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'C015 schema refused: existing route-DCC rows have no authoritative version';
    END IF;
    ALTER TABLE `mes_pro_route_dcc_project_binding`
      ADD COLUMN `version` bigint NOT NULL COMMENT '同租户同路线单调递增版本' AFTER `dcc_project_code_id`;
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
       AND column_name = 'version' AND (data_type <> 'bigint' OR is_nullable <> 'NO')
  ) THEN
    ALTER TABLE `mes_pro_route_dcc_project_binding`
      MODIFY COLUMN `version` bigint NOT NULL COMMENT '同租户同路线单调递增版本';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
       AND column_name = 'active_route_id'
  ) THEN
    ALTER TABLE `mes_pro_route_dcc_project_binding`
      ADD COLUMN `active_route_id` BIGINT GENERATED ALWAYS AS
        (CASE WHEN `deleted` = b'0' THEN `route_id` ELSE NULL END) STORED COMMENT '未删除当前路线唯一键';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
       AND index_name = 'uk_mes_pro_route_dcc_current'
  ) THEN
    ALTER TABLE `mes_pro_route_dcc_project_binding`
      ADD UNIQUE KEY `uk_mes_pro_route_dcc_current` (`tenant_id`, `active_route_id`);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
       AND index_name = 'uk_mes_pro_route_dcc_history_version'
  ) THEN
    ALTER TABLE `mes_pro_route_dcc_project_binding`
      ADD UNIQUE KEY `uk_mes_pro_route_dcc_history_version` (`tenant_id`, `route_id`, `version`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_qa_inspection_regulation'
       AND column_name = 'dcc_project_code_id' AND (data_type <> 'bigint' OR is_nullable <> 'NO')
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      MODIFY COLUMN `dcc_project_code_id` bigint NOT NULL COMMENT 'DCC项目代码ID';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_qa_inspection_regulation'
       AND column_name = 'active_dcc_project_code_id'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      ADD COLUMN `active_dcc_project_code_id` BIGINT GENERATED ALWAYS AS
        (CASE WHEN `deleted` = b'0' THEN `dcc_project_code_id` ELSE NULL END) STORED COMMENT '未删除QA规程DCC唯一键';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'mes_qa_inspection_regulation'
       AND index_name = 'uk_mes_qa_regulation_active_dcc'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      ADD UNIQUE KEY `uk_mes_qa_regulation_active_dcc` (`tenant_id`, `active_dcc_project_code_id`);
  END IF;

  SET v_signature = NULL;
  SET v_non_unique = NULL;
  SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index), MIN(non_unique)
    INTO v_signature, v_non_unique
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_qa_inspection_regulation'
     AND index_name = 'uk_mes_qa_regulation_dcc_project';
  IF v_signature IS NOT NULL THEN
    IF v_signature <> 'tenant_id,dcc_project_code_id,deleted' OR v_non_unique <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'C015 schema refused: legacy QA DCC index signature is not canonical';
    END IF;
    ALTER TABLE `mes_qa_inspection_regulation`
      DROP INDEX `uk_mes_qa_regulation_dcc_project`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_task'
       AND column_name = 'inspection_rule_key'
       AND (column_type <> 'varchar(32)' OR is_nullable <> 'NO')
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      MODIFY COLUMN `inspection_rule_key` varchar(32) NOT NULL COMMENT '正式检验规则身份';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
     WHERE constraint_schema = DATABASE() AND table_name = 'mes_pqc_inspection_task'
       AND constraint_name = 'chk_mes_pqc_inspection_rule_key'
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD CONSTRAINT `chk_mes_pqc_inspection_rule_key`
      CHECK (`inspection_rule_key` IN ('FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'));
  END IF;
END$$
DELIMITER ;

CALL migrate_mes_c015_route_dcc_qa_reconciliation_schema();
DROP PROCEDURE IF EXISTS migrate_mes_c015_route_dcc_qa_reconciliation_schema;
