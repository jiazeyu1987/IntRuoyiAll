-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_dcc_nas_control_audit_file; type=schema; riskLevel=medium
-- Add snapshot fields for DCC NAS uncontrolled-file import tasks.

DROP PROCEDURE IF EXISTS ensure_dcc_uncontrolled_import_column;

CREATE PROCEDURE ensure_dcc_uncontrolled_import_column(
  IN target_table VARCHAR(64),
  IN target_column VARCHAR(64),
  IN ddl_statement TEXT
)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_uncontrolled_import_column_exists
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND COLUMN_NAME = target_column;

  IF @dcc_uncontrolled_import_column_exists = 0 THEN
    SET @dcc_uncontrolled_import_column_sql = ddl_statement;
    PREPARE dcc_uncontrolled_import_column_stmt FROM @dcc_uncontrolled_import_column_sql;
    EXECUTE dcc_uncontrolled_import_column_stmt;
    DEALLOCATE PREPARE dcc_uncontrolled_import_column_stmt;
  END IF;
END;

DROP PROCEDURE IF EXISTS ensure_dcc_uncontrolled_import_index;

CREATE PROCEDURE ensure_dcc_uncontrolled_import_index(
  IN target_table VARCHAR(64),
  IN target_index VARCHAR(64),
  IN ddl_statement TEXT
)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_uncontrolled_import_index_exists
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND INDEX_NAME = target_index;

  IF @dcc_uncontrolled_import_index_exists = 0 THEN
    SET @dcc_uncontrolled_import_index_sql = ddl_statement;
    PREPARE dcc_uncontrolled_import_index_stmt FROM @dcc_uncontrolled_import_index_sql;
    EXECUTE dcc_uncontrolled_import_index_stmt;
    DEALLOCATE PREPARE dcc_uncontrolled_import_index_stmt;
  END IF;
END;

CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task',
  'audit_task_id',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `audit_task_id` bigint DEFAULT NULL COMMENT ''dcc_nas_control_audit_task.id for NAS_UNCONTROLLED_IMPORT'' AFTER `id`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task',
  'idempotency_key',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `idempotency_key` varchar(128) DEFAULT NULL COMMENT ''Client idempotency key for NAS uncontrolled import'' AFTER `source_type`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task',
  'request_hash',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `request_hash` char(64) DEFAULT NULL COMMENT ''Canonical selected-files request hash'' AFTER `idempotency_key`'
);

CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'audit_file_id',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `audit_file_id` bigint DEFAULT NULL COMMENT ''dcc_nas_control_audit_file.id'' AFTER `task_id`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'source_signature',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `source_signature` char(64) DEFAULT NULL COMMENT ''Audit file source signature snapshot'' AFTER `source_file_id`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'classification_status_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `classification_status_snapshot` varchar(32) DEFAULT NULL COMMENT ''Audit classification status snapshot'' AFTER `source_signature`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'matched_project_code_id_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `matched_project_code_id_snapshot` bigint DEFAULT NULL COMMENT ''Matched dcc_project_code.id snapshot'' AFTER `classification_status_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'matched_file_type_taxonomy_id_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `matched_file_type_taxonomy_id_snapshot` bigint DEFAULT NULL COMMENT ''Matched taxonomy id snapshot'' AFTER `matched_project_code_id_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'matched_file_type_level1_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `matched_file_type_level1_snapshot` varchar(128) DEFAULT NULL AFTER `matched_file_type_taxonomy_id_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'matched_file_type_level2_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `matched_file_type_level2_snapshot` varchar(128) DEFAULT NULL AFTER `matched_file_type_level1_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'matched_file_type_level3_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `matched_file_type_level3_snapshot` varchar(128) DEFAULT NULL AFTER `matched_file_type_level2_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'matched_file_type_level4_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `matched_file_type_level4_snapshot` varchar(128) DEFAULT NULL AFTER `matched_file_type_level3_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'matched_file_type_level5_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `matched_file_type_level5_snapshot` varchar(128) DEFAULT NULL AFTER `matched_file_type_level4_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'classification_reason_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `classification_reason_snapshot` varchar(255) DEFAULT NULL AFTER `matched_file_type_level5_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'classification_candidates_json_snapshot',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `classification_candidates_json_snapshot` text DEFAULT NULL AFTER `classification_reason_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'local_relative_path',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `local_relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT ''Authorized local relative path snapshot'' AFTER `classification_candidates_json_snapshot`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'local_write_status',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `local_write_status` varchar(32) DEFAULT NULL COMMENT ''NOT_STARTED/LOCAL_WRITTEN/LOCAL_WRITE_FAILED'' AFTER `local_relative_path`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'local_write_error_code',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `local_write_error_code` varchar(64) DEFAULT NULL AFTER `local_write_status`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'local_write_error',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `local_write_error` varchar(512) DEFAULT NULL AFTER `local_write_error_code`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'archive_status',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `archive_status` varchar(32) DEFAULT NULL COMMENT ''NOT_STARTED/ARCHIVED/PENDING_MANUAL_REVIEW/FAILED'' AFTER `local_write_error`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'archive_error_code',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `archive_error_code` varchar(64) DEFAULT NULL AFTER `archive_status`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_controlled_file_nas_transfer_task_item',
  'archive_error',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `archive_error` varchar(512) DEFAULT NULL AFTER `archive_error_code`'
);

CALL ensure_dcc_uncontrolled_import_column(
  'dcc_nas_control_audit_file',
  'selected_import_task_id',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `selected_import_task_id` bigint DEFAULT NULL COMMENT ''Current selected import task id'' AFTER `archive_status`'
);
CALL ensure_dcc_uncontrolled_import_column(
  'dcc_nas_control_audit_file',
  'selected_import_task_item_id',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `selected_import_task_item_id` bigint DEFAULT NULL COMMENT ''Current selected import task item id'' AFTER `selected_import_task_id`'
);

CALL ensure_dcc_uncontrolled_import_index(
  'dcc_controlled_file_nas_transfer_task',
  'idx_dcc_nas_transfer_import_idempotency',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD INDEX `idx_dcc_nas_transfer_import_idempotency` (`tenant_id`, `operator_user_id`, `idempotency_key`, `deleted`)'
);
CALL ensure_dcc_uncontrolled_import_index(
  'dcc_controlled_file_nas_transfer_task',
  'idx_dcc_nas_transfer_audit_task',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD INDEX `idx_dcc_nas_transfer_audit_task` (`tenant_id`, `audit_task_id`, `source_type`, `deleted`)'
);
CALL ensure_dcc_uncontrolled_import_index(
  'dcc_controlled_file_nas_transfer_task_item',
  'idx_dcc_nas_transfer_item_audit_file',
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD INDEX `idx_dcc_nas_transfer_item_audit_file` (`tenant_id`, `audit_file_id`, `deleted`)'
);
CALL ensure_dcc_uncontrolled_import_index(
  'dcc_nas_control_audit_file',
  'idx_dcc_nas_audit_file_import_task',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD INDEX `idx_dcc_nas_audit_file_import_task` (`tenant_id`, `selected_import_task_id`, `selected_import_task_item_id`, `deleted`)'
);

DROP PROCEDURE IF EXISTS ensure_dcc_uncontrolled_import_index;
DROP PROCEDURE IF EXISTS ensure_dcc_uncontrolled_import_column;
