-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_qa_dcc_project_scope; type=preflight; riskLevel=high
-- C00 preflight is read-only and must run before C00 schema, active-order/PQC submit write stop, and data backfill.

DROP TEMPORARY TABLE IF EXISTS c00_preflight_release_metadata;
CREATE TEMPORARY TABLE c00_preflight_release_metadata (
  release_step varchar(64) NOT NULL,
  input_manifest_sha256 char(64) NOT NULL,
  affected_row_count bigint NOT NULL,
  blocker_reason varchar(512) DEFAULT NULL
);

INSERT INTO c00_preflight_release_metadata
SELECT 'required_tables', SHA2('C00-required-tables-v1', 256), COUNT(1), NULL
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name IN ('mes_qa_inspection_regulation', 'mes_qa_inspection_regulation_version',
                      'mes_pro_process_pool_active_order', 'mes_pqc_inspection_task',
                      'mes_pro_process_pool_event');

INSERT INTO c00_preflight_release_metadata
SELECT 'route_dcc_existing_structure', SHA2('route-dcc-existing-structure-v1', 256), COUNT(1),
       'missing route-DCC table or columns is expected before C00 schema; duplicate scan is skipped until structure exists'
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pro_route_dcc_project_binding'
   AND column_name IN ('tenant_id', 'route_id', 'deleted');

DROP TEMPORARY TABLE IF EXISTS c00_preflight_blocker_report;
CREATE TEMPORARY TABLE c00_preflight_blocker_report (
  blocker_scope varchar(64) NOT NULL,
  source_id bigint DEFAULT NULL,
  input_manifest_sha256 char(64) NOT NULL,
  affected_row_count bigint NOT NULL,
  blocker_reason varchar(512) NOT NULL
);

SET @c00_route_dcc_binding_ready := (
  SELECT CASE
           WHEN EXISTS (
             SELECT 1
               FROM information_schema.tables
              WHERE table_schema = DATABASE()
                AND table_name = 'mes_pro_route_dcc_project_binding'
           )
            AND 3 = (
              SELECT COUNT(1)
                FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'mes_pro_route_dcc_project_binding'
                 AND column_name IN ('tenant_id', 'route_id', 'deleted')
            )
           THEN 1 ELSE 0
         END
);

SET @sql := IF(@c00_route_dcc_binding_ready = 1,
  'INSERT INTO c00_preflight_blocker_report
   SELECT ''route_dcc_duplicate_current'', NULL, SHA2(''route-dcc-current-v1'', 256), COUNT(1),
          ''duplicate current route-DCC bindings must be resolved before C00 schema migration''
     FROM (
       SELECT tenant_id, route_id
         FROM mes_pro_route_dcc_project_binding
        WHERE deleted = b''0''
        GROUP BY tenant_id, route_id
       HAVING COUNT(1) > 1
     ) duplicate_current',
  'INSERT INTO c00_preflight_release_metadata
   SELECT ''route_dcc_duplicate_current_skipped'', SHA2(''route-dcc-current-skip-v1'', 256), 0,
          ''route-DCC binding structure is absent before C00 schema; no pre-schema duplicate scan required'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO c00_preflight_blocker_report
SELECT 'active_order_task_version', active_order.id, SHA2(CONCAT('active-order-task-version:', active_order.id), 256), COUNT(task.id),
       'active-order has zero, null, or multiple task regulation versions; approved list is required'
  FROM mes_pro_process_pool_active_order active_order
  LEFT JOIN mes_pqc_inspection_task task
    ON task.tenant_id = active_order.tenant_id
   AND task.active_order_id = active_order.id
 GROUP BY active_order.id
HAVING COUNT(DISTINCT task.regulation_version_id) <> 1
    OR SUM(CASE WHEN task.regulation_version_id IS NULL THEN 1 ELSE 0 END) > 0;

INSERT INTO c00_preflight_blocker_report
SELECT 'task_rule_key_ambiguous', task.id, SHA2(CONCAT('task-rule:', task.id), 256), 1,
       'PQC PATROL task cannot be mapped to PATROL_AM or PATROL_PM from current evidence'
  FROM mes_pqc_inspection_task task
 WHERE task.deleted = b'0'
   AND task.inspection_type = 'PATROL'
   AND COALESCE(task.shift_code, '') NOT IN ('AM', 'PM');

INSERT INTO c00_preflight_blocker_report
SELECT 'formal_event_cardinality', task.id, SHA2(CONCAT('formal-event:', task.id), 256), COUNT(event.id),
       'non-PENDING PQC task must have exactly one formal PQC event for CanonicalPqcSubmissionV1 reconstruction'
  FROM mes_pqc_inspection_task task
  LEFT JOIN mes_pro_process_pool_event event
    ON event.tenant_id = task.tenant_id
   AND event.feedback_source_id = task.id
   AND event.event_type = 'PQC_INSPECTION'
   AND event.feedback_source_type = 'MES_PQC_INSPECTION_TASK'
   AND event.deleted = b'0'
 WHERE task.deleted = b'0'
   AND task.task_status <> 'PENDING'
 GROUP BY task.id
HAVING COUNT(event.id) <> 1;

SELECT * FROM c00_preflight_release_metadata;
SELECT * FROM c00_preflight_blocker_report ORDER BY blocker_scope, source_id;
