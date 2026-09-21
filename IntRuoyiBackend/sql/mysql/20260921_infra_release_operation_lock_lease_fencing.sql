-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_infra_release_migration_state; type=schema; riskLevel=high
-- Purpose: Add executor identity, renewable lease and write-fence fields to the release environment lock.

DROP PROCEDURE IF EXISTS upgrade_infra_release_operation_lock_lease;

DELIMITER $$
CREATE PROCEDURE upgrade_infra_release_operation_lock_lease()
BEGIN
  DECLARE lock_table_count INT DEFAULT 0;
  DECLARE lease_column_count INT DEFAULT 0;

  SELECT COUNT(*)
    INTO lock_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'infra_release_operation_lock';

  IF lock_table_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'infra release operation lock table missing';
  END IF;

  SELECT COUNT(*)
    INTO lease_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'infra_release_operation_lock'
     AND COLUMN_NAME IN (
       'executor_host',
       'executor_pid',
       'executor_identity',
       'lease_token',
       'heartbeat_at',
       'write_fence'
     );

  IF lease_column_count = 0 THEN
    ALTER TABLE `infra_release_operation_lock`
      ADD COLUMN `executor_host` varchar(255) DEFAULT NULL COMMENT '执行宿主',
      ADD COLUMN `executor_pid` varchar(64) DEFAULT NULL COMMENT '执行进程身份',
      ADD COLUMN `executor_identity` varchar(255) DEFAULT NULL COMMENT '执行身份',
      ADD COLUMN `lease_token` varchar(128) DEFAULT NULL COMMENT '锁租约令牌',
      ADD COLUMN `heartbeat_at` datetime DEFAULT NULL COMMENT '最近心跳',
      ADD COLUMN `write_fence` bigint NOT NULL DEFAULT 0 COMMENT '写入栅栏版本';
  ELSEIF lease_column_count <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'infra release operation lock partial lease schema detected';
  END IF;

  SELECT COUNT(*)
    INTO lease_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'infra_release_operation_lock'
     AND COLUMN_NAME IN (
       'executor_host',
       'executor_pid',
       'executor_identity',
       'lease_token',
       'heartbeat_at',
       'write_fence'
     );

  IF lease_column_count <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'infra release operation lock lease schema upgrade incomplete';
  END IF;
END$$
DELIMITER ;

CALL upgrade_infra_release_operation_lock_lease();
DROP PROCEDURE IF EXISTS upgrade_infra_release_operation_lock_lease;
