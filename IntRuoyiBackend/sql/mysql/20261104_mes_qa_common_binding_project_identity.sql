-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260909_mes_qa_common_regulation_product_binding; type=schema; riskLevel=medium
-- Production QA is scoped by DCC project. Preserve legacy MDM/common-project columns for audit.
-- Before changing any existing binding, require an unambiguous tenant-local project mapping.
DROP PROCEDURE IF EXISTS migrate_mes_qa_common_binding_project_identity;
DELIMITER $$
CREATE PROCEDURE migrate_mes_qa_common_binding_project_identity()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_qa_common_regulation_product_binding' AND column_name='product_dcc_project_code_id') THEN
    IF EXISTS (
      SELECT b.id FROM mes_qa_common_regulation_product_binding b
      LEFT JOIN dcc_project_code p ON p.tenant_id=b.tenant_id
        AND p.product_master_id=b.product_id AND p.deleted=b'0'
      WHERE b.deleted=b'0'
      GROUP BY b.id HAVING COUNT(p.id) <> 1
    ) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Common QA binding migration requires exactly one tenant-local DCC project per legacy binding';
    END IF;
    ALTER TABLE mes_qa_common_regulation_product_binding
      ADD COLUMN product_dcc_project_code_id BIGINT NULL COMMENT '使用通用规程的DCC项目代码ID',
      MODIFY COLUMN product_id BIGINT NULL COMMENT '历史MDM产品ID，仅保留审计',
      MODIFY COLUMN dcc_project_code_id BIGINT NULL COMMENT '历史通用规程项目ID，仅保留审计';
  END IF;

  IF EXISTS (
    SELECT b.id FROM mes_qa_common_regulation_product_binding b
    LEFT JOIN dcc_project_code p ON p.tenant_id=b.tenant_id
      AND p.product_master_id=b.product_id AND p.deleted=b'0'
    WHERE b.deleted=b'0' AND b.product_dcc_project_code_id IS NULL
    GROUP BY b.id HAVING COUNT(p.id) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unmapped common QA binding requires exactly one tenant-local DCC project';
  END IF;
  UPDATE mes_qa_common_regulation_product_binding b
    JOIN dcc_project_code p ON p.tenant_id=b.tenant_id
      AND p.product_master_id=b.product_id AND p.deleted=b'0'
    SET b.product_dcc_project_code_id=p.id, b.update_time=b.update_time
    WHERE b.deleted=b'0' AND b.product_dcc_project_code_id IS NULL;

  IF EXISTS (SELECT 1 FROM mes_qa_common_regulation_product_binding
      WHERE deleted=b'0' AND product_dcc_project_code_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Common QA binding has no explicit DCC project after migration';
  END IF;
  IF EXISTS (SELECT tenant_id,product_dcc_project_code_id
      FROM mes_qa_common_regulation_product_binding
      WHERE deleted=b'0' AND binding_status='ENABLED'
      GROUP BY tenant_id,product_dcc_project_code_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Multiple enabled common QA bindings exist for the same DCC project';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_qa_common_regulation_product_binding' AND index_name='uk_mes_qa_common_binding_active_project') THEN
    ALTER TABLE mes_qa_common_regulation_product_binding
      DROP INDEX uk_mes_qa_common_binding_active_product,
      MODIFY COLUMN active_binding_key VARCHAR(64) GENERATED ALWAYS AS (
        CASE WHEN deleted=b'0' AND binding_status='ENABLED'
          THEN CAST(product_dcc_project_code_id AS CHAR) ELSE NULL END
      ) STORED COMMENT '项目启用绑定唯一键',
      ADD UNIQUE KEY uk_mes_qa_common_binding_active_project (tenant_id,active_binding_key),
      ADD KEY idx_mes_qa_common_binding_project_status (tenant_id,product_dcc_project_code_id,binding_status);
  END IF;
END$$
DELIMITER ;
CALL migrate_mes_qa_common_binding_project_identity();
DROP PROCEDURE migrate_mes_qa_common_binding_project_identity;
