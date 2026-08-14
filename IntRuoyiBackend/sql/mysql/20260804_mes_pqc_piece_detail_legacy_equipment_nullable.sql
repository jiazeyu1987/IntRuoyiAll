-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium
-- M6 PQC piece-detail schema reconcile:
-- historical local/test databases may still contain mandatory selected_equipment_* columns,
-- while the current formal PQC task model does not require equipment for QA-regulation sourced inspections.

DROP PROCEDURE IF EXISTS relax_mes_pqc_piece_detail_column_if_required;
DELIMITER $$
CREATE PROCEDURE relax_mes_pqc_piece_detail_column_if_required(
  IN p_column_name varchar(128),
  IN p_modify_sql text
)
BEGIN
  IF EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_piece_detail'
       AND column_name = p_column_name
       AND is_nullable = 'NO'
  ) THEN
    SET @modify_column_sql = p_modify_sql;
    PREPARE modify_column_stmt FROM @modify_column_sql;
    EXECUTE modify_column_stmt;
    DEALLOCATE PREPARE modify_column_stmt;
  END IF;
END$$
DELIMITER ;

CALL relax_mes_pqc_piece_detail_column_if_required(
  'selected_equipment_id',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` MODIFY COLUMN `selected_equipment_id` bigint NULL COMMENT ''历史设备ID；QA规程来源PQC任务可为空'''
);
CALL relax_mes_pqc_piece_detail_column_if_required(
  'selected_equipment_code',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` MODIFY COLUMN `selected_equipment_code` varchar(64) NULL COMMENT ''历史设备编码；QA规程来源PQC任务可为空'''
);
CALL relax_mes_pqc_piece_detail_column_if_required(
  'selected_equipment_name',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` MODIFY COLUMN `selected_equipment_name` varchar(128) NULL COMMENT ''历史设备名称；QA规程来源PQC任务可为空'''
);
CALL relax_mes_pqc_piece_detail_column_if_required(
  'selected_equipment_number',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` MODIFY COLUMN `selected_equipment_number` varchar(64) NULL COMMENT ''历史设备编号；QA规程来源PQC任务可为空'''
);

DROP PROCEDURE IF EXISTS relax_mes_pqc_piece_detail_column_if_required;
