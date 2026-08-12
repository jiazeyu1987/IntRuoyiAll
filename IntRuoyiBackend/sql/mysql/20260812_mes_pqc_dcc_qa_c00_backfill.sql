-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_pqc_dcc_qa_c00_schema; type=backfill; riskLevel=high
-- C00 backfill consumes approved route-DCC and active-order QA snapshot manifests. Default run is dry-run evidence.

SET @c00_apply_backfill := COALESCE(@c00_apply_backfill, 0);

DROP TEMPORARY TABLE IF EXISTS c00_backfill_approved_route_dcc_binding;
CREATE TEMPORARY TABLE c00_backfill_approved_route_dcc_binding (
  tenant_id bigint NOT NULL,
  route_id bigint NOT NULL,
  dcc_project_code_id bigint NOT NULL,
  expected_previous_version bigint DEFAULT NULL,
  approved_by varchar(64) NOT NULL,
  approved_at datetime NOT NULL,
  approval_note varchar(512) NOT NULL,
  input_manifest_sha256 char(64) NOT NULL
);

DROP TEMPORARY TABLE IF EXISTS c00_backfill_approved_active_order_snapshot;
CREATE TEMPORARY TABLE c00_backfill_approved_active_order_snapshot (
  tenant_id bigint NOT NULL,
  active_order_id bigint NOT NULL,
  dcc_project_code_id bigint NOT NULL,
  qa_regulation_id bigint NOT NULL,
  qa_regulation_version_id bigint NOT NULL,
  approval_note varchar(512) NOT NULL,
  input_manifest_sha256 char(64) NOT NULL
);

DROP TEMPORARY TABLE IF EXISTS c00_backfill_blocker_report;
CREATE TEMPORARY TABLE c00_backfill_blocker_report (
  blocker_scope varchar(64) NOT NULL,
  source_id bigint DEFAULT NULL,
  input_manifest_sha256 char(64) NOT NULL,
  affected_row_count bigint NOT NULL,
  blocker_reason varchar(512) NOT NULL
);

INSERT INTO c00_backfill_blocker_report
SELECT 'missing_approved_route_dcc', NULL, SHA2('route-dcc-approved-list-v1', 256), 0,
       'approved route-DCC manifest is empty; C00 must not infer route bindings from labels or current QA'
 WHERE NOT EXISTS (SELECT 1 FROM c00_backfill_approved_route_dcc_binding);

INSERT INTO c00_backfill_blocker_report
SELECT 'missing_approved_active_order_snapshot', NULL, SHA2('active-order-approved-list-v1', 256), 0,
       'approved active-order QA snapshot manifest is empty for zero-task or ambiguous historical orders'
 WHERE EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order)
   AND NOT EXISTS (SELECT 1 FROM c00_backfill_approved_active_order_snapshot);

DROP TEMPORARY TABLE IF EXISTS c00_backfill_plan_report;
CREATE TEMPORARY TABLE c00_backfill_plan_report (
  plan_step varchar(64) NOT NULL,
  input_manifest_sha256 char(64) NOT NULL,
  affected_row_count bigint NOT NULL,
  blocker_reason varchar(512) DEFAULT NULL
);

INSERT INTO c00_backfill_plan_report
SELECT 'route_dcc_binding_upsert', COALESCE(MIN(input_manifest_sha256), SHA2('empty-route-dcc', 256)), COUNT(1), NULL
  FROM c00_backfill_approved_route_dcc_binding;

INSERT INTO c00_backfill_plan_report
SELECT 'active_order_snapshot_update', COALESCE(MIN(input_manifest_sha256), SHA2('empty-active-order', 256)), COUNT(1), NULL
  FROM c00_backfill_approved_active_order_snapshot;

INSERT INTO c00_backfill_plan_report
SELECT 'task_rule_key_update', SHA2('FIRST-FINAL-PATROL-AM-PM-v1', 256), COUNT(1), NULL
  FROM mes_pqc_inspection_task
 WHERE deleted = b'0'
   AND inspection_rule_key IS NULL
   AND (inspection_type IN ('FIRST', 'FINAL')
        OR (inspection_type = 'PATROL' AND shift_code IN ('AM', 'PM')));

INSERT INTO c00_backfill_plan_report
SELECT 'CanonicalPqcSubmissionV1_hash_update', SHA2('CanonicalPqcSubmissionV1', 256), COUNT(1), NULL
  FROM mes_pqc_inspection_task
 WHERE deleted = b'0'
   AND task_status <> 'PENDING'
   AND submitted_content_hash IS NULL;

SELECT * FROM c00_backfill_blocker_report ORDER BY blocker_scope, source_id;
SELECT * FROM c00_backfill_plan_report ORDER BY plan_step;

SET @c00_blocker_count := (SELECT COUNT(1) FROM c00_backfill_blocker_report);
SET @sql := IF(@c00_blocker_count = 0 AND @c00_apply_backfill = 1,
  'SELECT ''C00 backfill apply mode must be executed from the controlled maintenance runbook with approved manifests already loaded'' AS c00_backfill_status',
  'SELECT ''C00 backfill dry-run only; set @c00_apply_backfill=1 inside the approved maintenance runbook after blocker report is empty'' AS c00_backfill_status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
