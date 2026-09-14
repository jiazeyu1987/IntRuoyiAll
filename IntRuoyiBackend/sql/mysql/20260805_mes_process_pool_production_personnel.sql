-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_process_pool_active_order_process_snapshot; type=schema; riskLevel=medium
-- MES 生产组长生产人员档案：正式工/临时工、签名密码哈希、显示名唯一与管理审计追溯
-- physical-contract: `display_name` varchar(128)
-- physical-contract: `signature_password_hash` varchar(255)
-- physical-contract: `signature_password_updated_at` datetime
-- physical-contract: `employee_type` varchar(32) NOT NULL COMMENT '员工来源：FORMAL/TEMPORARY'
-- physical-contract: `active_display_name` varchar(128) GENERATED ALWAYS AS
-- physical-contract: UNIQUE KEY `uk_mes_pp_team_employee_active_display_name`
-- physical-contract: `display_name_snapshot` varchar(128)
-- physical-contract: `operator_user_id` bigint
-- physical-contract: `result_status` varchar(32)
-- physical-contract: `change_summary` varchar(1000)

DROP PROCEDURE IF EXISTS ensure_mes_pp_personnel_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_personnel_column(
    IN p_table_name varchar(128),
    IN p_column_name varchar(128),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_employee_profile',
    'display_name',
    'varchar(128) DEFAULT NULL COMMENT ''生产人员显示名；同一组长有效员工不可重复'' AFTER `employee_name`'
);

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_employee_profile',
    'signature_password_hash',
    'varchar(255) DEFAULT NULL COMMENT ''临时工电子签名密码哈希；正式工为空并使用系统用户原电子签名密码'' AFTER `employee_type`'
);

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_employee_profile',
    'signature_password_updated_at',
    'datetime DEFAULT NULL COMMENT ''临时工电子签名密码更新时间'' AFTER `signature_password_hash`'
);

ALTER TABLE `mes_pro_process_pool_team_employee_profile`
    MODIFY COLUMN `employee_type` varchar(32) NOT NULL COMMENT '员工来源：FORMAL/TEMPORARY';

UPDATE `mes_pro_process_pool_team_employee_profile`
SET `display_name` = COALESCE(NULLIF(`display_name`, ''), `employee_name`)
WHERE `display_name` IS NULL OR `display_name` = '';

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_employee_profile',
    'active_display_name',
    'varchar(128) GENERATED ALWAYS AS (IF(`enabled` = b''1'', COALESCE(`display_name`, `employee_name`), NULL)) STORED COMMENT ''有效员工显示名唯一键辅助列'' AFTER `signature_password_updated_at`'
);

DROP PROCEDURE IF EXISTS ensure_mes_pp_personnel_index;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_personnel_index(
    IN p_table_name varchar(128),
    IN p_index_name varchar(128),
    IN p_index_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_personnel_index(
    'mes_pro_process_pool_team_employee_profile',
    'uk_mes_pp_team_employee_active_display_name',
    'UNIQUE KEY `uk_mes_pp_team_employee_active_display_name` (`tenant_id`, `leader_user_id`, `active_display_name`, `deleted`)'
);

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_employee_binding',
    'display_name_snapshot',
    'varchar(128) DEFAULT NULL COMMENT ''绑定时员工显示名快照，用于历史报工追溯'' AFTER `employee_user_id`'
);

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_maintenance_audit',
    'operator_user_id',
    'bigint DEFAULT NULL COMMENT ''操作人用户ID'' AFTER `leader_user_id`'
);

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_maintenance_audit',
    'result_status',
    'varchar(32) DEFAULT NULL COMMENT ''操作结果：SUCCESS/FAILED'' AFTER `target_id`'
);

CALL ensure_mes_pp_personnel_column(
    'mes_pro_process_pool_team_maintenance_audit',
    'change_summary',
    'varchar(1000) DEFAULT NULL COMMENT ''变更摘要'' AFTER `result_status`'
);

DROP PROCEDURE IF EXISTS ensure_mes_pp_personnel_index;
DROP PROCEDURE IF EXISTS ensure_mes_pp_personnel_column;
