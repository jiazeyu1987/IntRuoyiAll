-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- goal: Persist the workstation selected by each route process. Resource capacity remains owned by workstation master data.
-- rollback: ALTER TABLE mes_pro_route_process DROP COLUMN workstation_id; run only after confirming downstream route-process bindings are no longer required.

START TRANSACTION;

SET @mes_route_process_workstation_column_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_process'
      AND COLUMN_NAME = 'workstation_id'
);

SET @mes_route_process_workstation_add_column_sql := IF(
    @mes_route_process_workstation_column_exists = 0,
    'ALTER TABLE `mes_pro_route_process` ADD COLUMN `workstation_id` bigint NULL DEFAULT NULL COMMENT ''绑定工作站ID'' AFTER `process_id`',
    'SELECT ''mes_pro_route_process.workstation_id already exists'' AS migration_message'
);

PREPARE mes_route_process_workstation_add_column_stmt FROM @mes_route_process_workstation_add_column_sql;
EXECUTE mes_route_process_workstation_add_column_stmt;
DEALLOCATE PREPARE mes_route_process_workstation_add_column_stmt;

SET @mes_route_process_workstation_backfill_candidates := (
    SELECT COUNT(1)
    FROM `mes_pro_route_process` route_process
    JOIN (
        SELECT tenant_id, process_id, MIN(id) AS workstation_id
        FROM `mes_md_workstation`
        WHERE deleted = b'0'
          AND status = 0
          AND process_id IS NOT NULL
        GROUP BY tenant_id, process_id
        HAVING COUNT(1) = 1
    ) unique_workstation_process
      ON unique_workstation_process.tenant_id = route_process.tenant_id
     AND unique_workstation_process.process_id = route_process.process_id
    WHERE route_process.deleted = b'0'
      AND route_process.workstation_id IS NULL
);

UPDATE `mes_pro_route_process` route_process
JOIN (
    SELECT tenant_id, process_id, MIN(id) AS workstation_id
    FROM `mes_md_workstation`
    WHERE deleted = b'0'
      AND status = 0
      AND process_id IS NOT NULL
    GROUP BY tenant_id, process_id
    HAVING COUNT(1) = 1
) unique_workstation_process
  ON unique_workstation_process.tenant_id = route_process.tenant_id
 AND unique_workstation_process.process_id = route_process.process_id
SET route_process.workstation_id = unique_workstation_process.workstation_id,
    route_process.update_time = NOW()
WHERE route_process.deleted = b'0'
  AND route_process.workstation_id IS NULL;

SET @mes_route_process_workstation_backfilled_rows := ROW_COUNT();

SET @mes_route_process_workstation_manual_review_route_process_count := (
    SELECT COUNT(1)
    FROM `mes_pro_route_process` route_process
    WHERE route_process.deleted = b'0'
      AND route_process.workstation_id IS NULL
);

SELECT @mes_route_process_workstation_backfill_candidates AS backfill_candidate_route_process_count,
       @mes_route_process_workstation_backfilled_rows AS backfilled_route_process_count,
       @mes_route_process_workstation_manual_review_route_process_count AS manual_review_route_process_count;

COMMIT;
