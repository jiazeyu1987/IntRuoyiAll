-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260726_mes_batch_record_cell_link_work_order_source,20260830_mes_batch_record_cell_link_structured_source_widths,20260526_edhr_approval_archive_schema_contract,20260708_mes_batch_record_version_phase_one; type=schema; riskLevel=medium; requiresTargetPreflight=true
-- Forward-only differences from immutable historical originals. No row backfill.
DELIMITER $$
CREATE PROCEDURE release_20261022_schema_forward()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_batch_record_cell_link_rule' AND COLUMN_NAME='route_process_id') THEN
  ALTER TABLE mes_pro_batch_record_cell_link_rule ADD COLUMN route_process_id bigint DEFAULT NULL COMMENT 'Route process ID';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_batch_record_cell_link_rule' AND COLUMN_NAME='route_process_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='HISTORICAL_FORWARD_ROUTE_PROCESS_TYPE_CONFLICT';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_batch_record_cell_link_rule' AND COLUMN_NAME='aggregation_strategy') THEN
  ALTER TABLE mes_pro_batch_record_cell_link_rule ADD COLUMN aggregation_strategy varchar(32) DEFAULT NULL COMMENT 'SUM/LIST/DISTINCT_LIST/FIRST/LAST/MIN/MAX';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_batch_record_cell_link_rule' AND COLUMN_NAME='aggregation_strategy' AND COLUMN_TYPE='varchar(32)' AND IS_NULLABLE='YES') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='HISTORICAL_FORWARD_AGGREGATION_TYPE_CONFLICT';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_batch_record_approval_snapshot' AND COLUMN_NAME='process_instance_id' AND COLUMN_TYPE='varchar(64)' AND COLLATION_NAME='utf8mb4_unicode_ci') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='HISTORICAL_FORWARD_APPROVAL_SNAPSHOT_TYPE_CONFLICT';
 END IF;
 ALTER TABLE mes_pro_batch_record_approval_snapshot MODIFY COLUMN process_instance_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM process instance ID';
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_batch_record_version_approval_event' AND COLUMN_NAME='approval_instance_id' AND COLUMN_TYPE='varchar(128)' AND COLLATION_NAME='utf8mb4_unicode_ci') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='HISTORICAL_FORWARD_APPROVAL_EVENT_TYPE_CONFLICT';
 END IF;
 ALTER TABLE mes_pro_batch_record_version_approval_event MODIFY COLUMN approval_instance_id varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Approval instance ID';
END$$
DELIMITER ;
CALL release_20261022_schema_forward();
DROP PROCEDURE release_20261022_schema_forward;
