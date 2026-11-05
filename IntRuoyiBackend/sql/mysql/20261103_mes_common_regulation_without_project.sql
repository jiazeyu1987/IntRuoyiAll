-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260909_mes_qa_common_regulation_product_binding; type=schema; riskLevel=medium
-- 通用检验规程独立于产品项目；产品 QA 的项目必填仍由服务校验。
-- 不改写历史项目关联，不合并同编号规程。回退非空约束前必须显式处理无项目通用规程。
DROP PROCEDURE IF EXISTS migrate_mes_common_regulation_without_project;
DELIMITER //
CREATE PROCEDURE migrate_mes_common_regulation_without_project()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_qa_inspection_regulation'
          AND column_name = 'dcc_project_code_id'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'required regulation project column is missing';
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_qa_inspection_regulation'
          AND column_name = 'dcc_project_code_id' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE mes_qa_inspection_regulation
            MODIFY COLUMN dcc_project_code_id bigint NULL COMMENT 'DCC项目代码ID；通用规程可为空';
    END IF;
END//
DELIMITER ;
CALL migrate_mes_common_regulation_without_project();
DROP PROCEDURE IF EXISTS migrate_mes_common_regulation_without_project;
