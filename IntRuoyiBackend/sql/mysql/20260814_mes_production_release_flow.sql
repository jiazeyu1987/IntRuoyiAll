-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260808_mes_active_order_release_application; type=schema; riskLevel=high
-- 生产放行共享结构：申请聚合 CAS、PQC 决策、报告快照和申请唯一 PQC 待办。
-- Rollback before business use: drop the three new unique indexes and added columns, then restore batch_execution_id nullability.
-- After target business data exists, destructive rollback is forbidden; use backup recovery or an approved forward migration.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migrate_mes_production_release_flow;

DELIMITER $$

CREATE PROCEDURE migrate_mes_production_release_flow()
BEGIN
  DECLARE legacy_application_count bigint DEFAULT 0;
  DECLARE legacy_batch_execution_count bigint DEFAULT 0;
  DECLARE invalid_pqc_task_count bigint DEFAULT 0;
  DECLARE duplicate_count bigint DEFAULT 0;
  DECLARE index_columns varchar(512) DEFAULT NULL;
  DECLARE index_non_unique int DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_active_order_release_application';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_work_task';
  END IF;

  IF (
    SELECT COUNT(1)
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name IN ('id', 'tenant_id', 'application_status', 'batch_execution_id', 'deleted')
  ) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Release application migration requires id, tenant_id, application_status, batch_execution_id and deleted';
  END IF;

  IF (
    SELECT COUNT(1)
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
       AND column_name IN ('tenant_id', 'task_type', 'batch_execution_id', 'business_scope_type', 'business_scope_id', 'deleted')
  ) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Work task migration requires tenant, task, batch and business scope columns';
  END IF;

  SELECT COUNT(1)
    INTO legacy_application_count
    FROM `mes_pro_process_pool_active_order_release_application`
   WHERE `deleted` = b'0'
     AND (`application_status` IN ('BLOCKED', 'PENDING_RELEASE_APPROVAL')
       OR `application_status` NOT IN (
         'PQC_RELEASE_PENDING',
         'PQC_RELEASE_REJECTED',
         'REPORT_UPLOAD_PENDING',
         'MANAGER_RELEASE_PENDING',
         'RELEASED'
       ));

  IF legacy_application_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_decision'
  ) THEN
    SELECT COUNT(1)
      INTO legacy_batch_execution_count
      FROM `mes_pro_process_pool_active_order_release_application`
     WHERE `deleted` = b'0'
       AND `batch_execution_id` IS NOT NULL;

    IF legacy_batch_execution_count > 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED';
    END IF;
  END IF;

  SELECT COUNT(1)
    INTO invalid_pqc_task_count
    FROM `mes_pro_edhr_work_task`
   WHERE `deleted` = b'0'
     AND `task_type` = 'PQC_PRODUCTION_RELEASE'
     AND (`business_scope_type` <> 'RELEASE_APPLICATION'
       OR `business_scope_type` IS NULL
       OR `business_scope_id` IS NULL);

  IF invalid_pqc_task_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED';
  END IF;

  SELECT COUNT(1)
    INTO duplicate_count
    FROM (
      SELECT `tenant_id`, `business_scope_id`
        FROM `mes_pro_edhr_work_task`
       WHERE `deleted` = b'0'
         AND `task_type` = 'PQC_PRODUCTION_RELEASE'
         AND `business_scope_type` = 'RELEASE_APPLICATION'
       GROUP BY `tenant_id`, `business_scope_id`
      HAVING COUNT(1) > 1
    ) duplicate_pqc_tasks;

  IF duplicate_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate PQC release work tasks require formal migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_release_work_task_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD COLUMN `pqc_release_work_task_id` bigint NULL COMMENT 'PQC生产放行待办ID'
      AFTER `release_approval_work_task_id`;
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_release_work_task_id'
       AND data_type = 'bigint'
       AND is_nullable = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing pqc_release_work_task_id has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_decision'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD COLUMN `pqc_decision` varchar(32) NULL COMMENT 'PQC决定：APPROVED/REJECTED'
      AFTER `pqc_release_work_task_id`;
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_decision'
       AND data_type = 'varchar'
       AND character_maximum_length = 32
       AND is_nullable = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing pqc_decision has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_decided_by'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD COLUMN `pqc_decided_by` bigint NULL COMMENT 'PQC决定人用户ID'
      AFTER `pqc_decision`;
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_decided_by'
       AND data_type = 'bigint'
       AND is_nullable = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing pqc_decided_by has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_decided_at'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD COLUMN `pqc_decided_at` datetime NULL COMMENT 'PQC决定时间'
      AFTER `pqc_decided_by`;
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_decided_at'
       AND data_type = 'datetime'
       AND is_nullable = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing pqc_decided_at has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_reject_reason'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD COLUMN `pqc_reject_reason` varchar(500) NULL COMMENT 'PQC驳回原因'
      AFTER `pqc_decided_at`;
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'pqc_reject_reason'
       AND data_type = 'varchar'
       AND character_maximum_length = 500
       AND is_nullable = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing pqc_reject_reason has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'report_snapshot_hash'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD COLUMN `report_snapshot_hash` varchar(128) NULL COMMENT '四份报告证据快照哈希'
      AFTER `source_snapshot_hash`;
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'report_snapshot_hash'
       AND data_type = 'varchar'
       AND character_maximum_length = 128
       AND is_nullable = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing report_snapshot_hash has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'version'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD COLUMN `version` int NOT NULL DEFAULT 1 COMMENT 'SP-1至SP-3申请聚合乐观锁版本'
      AFTER `report_snapshot_hash`;
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND column_name = 'version'
       AND data_type = 'int'
       AND is_nullable = 'NO'
       AND column_default = '1'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing release application version has an incompatible definition';
  END IF;

  SET @legacy_batch_execution_count = 0;
  SET @legacy_batch_execution_sql =
    'SELECT COUNT(1) INTO @legacy_batch_execution_count '
    'FROM mes_pro_process_pool_active_order_release_application '
    'WHERE deleted = b''0'' AND batch_execution_id IS NOT NULL '
    'AND (pqc_decision IS NULL OR pqc_decision <> ''APPROVED'' OR pqc_release_work_task_id IS NULL)';
  PREPARE legacy_batch_execution_statement FROM @legacy_batch_execution_sql;
  EXECUTE legacy_batch_execution_statement;
  DEALLOCATE PREPARE legacy_batch_execution_statement;
  SET legacy_batch_execution_count = @legacy_batch_execution_count;

  IF legacy_batch_execution_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED';
  END IF;

  SELECT COUNT(1)
    INTO duplicate_count
    FROM (
      SELECT `tenant_id`, `pqc_release_work_task_id`
        FROM `mes_pro_process_pool_active_order_release_application`
       WHERE `deleted` = b'0'
         AND `pqc_release_work_task_id` IS NOT NULL
       GROUP BY `tenant_id`, `pqc_release_work_task_id`
      HAVING COUNT(1) > 1
    ) duplicate_application_pqc_tasks;

  IF duplicate_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate release application PQC task relationships';
  END IF;

  SELECT COUNT(1)
    INTO duplicate_count
    FROM (
      SELECT `tenant_id`, `batch_execution_id`
        FROM `mes_pro_process_pool_active_order_release_application`
       WHERE `deleted` = b'0'
         AND `batch_execution_id` IS NOT NULL
       GROUP BY `tenant_id`, `batch_execution_id`
      HAVING COUNT(1) > 1
    ) duplicate_application_batch_executions;

  IF duplicate_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate release application batch execution relationships';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND index_name = 'uk_mes_pp_release_pqc_task'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD UNIQUE KEY `uk_mes_pp_release_pqc_task`
      (`tenant_id`, `pqc_release_work_task_id`, `deleted`);
  ELSE
    SELECT GROUP_CONCAT(`column_name` ORDER BY `seq_in_index` SEPARATOR ','), MIN(`non_unique`)
      INTO index_columns, index_non_unique
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND index_name = 'uk_mes_pp_release_pqc_task';
    IF index_columns <> 'tenant_id,pqc_release_work_task_id,deleted' OR index_non_unique <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Existing uk_mes_pp_release_pqc_task has an incompatible definition';
    END IF;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND index_name = 'uk_mes_pp_release_batch_execution'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order_release_application`
      ADD UNIQUE KEY `uk_mes_pp_release_batch_execution`
      (`tenant_id`, `batch_execution_id`, `deleted`);
  ELSE
    SELECT GROUP_CONCAT(`column_name` ORDER BY `seq_in_index` SEPARATOR ','), MIN(`non_unique`)
      INTO index_columns, index_non_unique
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_release_application'
       AND index_name = 'uk_mes_pp_release_batch_execution';
    IF index_columns <> 'tenant_id,batch_execution_id,deleted' OR index_non_unique <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Existing uk_mes_pp_release_batch_execution has an incompatible definition';
    END IF;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
       AND column_name = 'batch_execution_id'
       AND data_type = 'bigint'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing work task batch_execution_id has an incompatible type';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
       AND column_name = 'batch_execution_id'
       AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task`
      MODIFY COLUMN `batch_execution_id` bigint NULL
      COMMENT '批次执行ID；PQC生产放行申请待办为空';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
       AND column_name = 'pqc_release_application_scope_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task`
      ADD COLUMN `pqc_release_application_scope_id` bigint
      GENERATED ALWAYS AS (
        CASE
          WHEN business_scope_type = 'RELEASE_APPLICATION'
           AND task_type = 'PQC_PRODUCTION_RELEASE'
           AND deleted = b'0'
          THEN business_scope_id
          ELSE NULL
        END
      ) STORED COMMENT '有效PQC生产放行待办的申请作用域';
  ELSEIF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
       AND column_name = 'pqc_release_application_scope_id'
       AND data_type = 'bigint'
       AND extra LIKE '%STORED GENERATED%'
       AND generation_expression LIKE '%business_scope_type%'
       AND generation_expression LIKE '%RELEASE_APPLICATION%'
       AND generation_expression LIKE '%task_type%'
       AND generation_expression LIKE '%PQC_PRODUCTION_RELEASE%'
       AND generation_expression LIKE '%business_scope_id%'
       AND generation_expression LIKE '%deleted%'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing pqc_release_application_scope_id has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
       AND index_name = 'uk_mes_edhr_work_task_release_application'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task`
      ADD UNIQUE KEY `uk_mes_edhr_work_task_release_application`
      (`tenant_id`, `pqc_release_application_scope_id`);
  ELSE
    SELECT GROUP_CONCAT(`column_name` ORDER BY `seq_in_index` SEPARATOR ','), MIN(`non_unique`)
      INTO index_columns, index_non_unique
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_edhr_work_task'
       AND index_name = 'uk_mes_edhr_work_task_release_application';
    IF index_columns <> 'tenant_id,pqc_release_application_scope_id' OR index_non_unique <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Existing uk_mes_edhr_work_task_release_application has an incompatible definition';
    END IF;
  END IF;
END$$

DELIMITER ;

CALL migrate_mes_production_release_flow();

DROP PROCEDURE IF EXISTS migrate_mes_production_release_flow;
