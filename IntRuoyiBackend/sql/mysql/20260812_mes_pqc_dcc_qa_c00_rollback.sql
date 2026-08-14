-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_pqc_dcc_qa_c00_schema; type=rollback-dry-run; riskLevel=high
-- C00 rollback requires active-order and PQC submit writes stopped before any schema reversal.

DROP TEMPORARY TABLE IF EXISTS c00_rollback_plan_report;
CREATE TEMPORARY TABLE c00_rollback_plan_report (
  rollback_step varchar(64) NOT NULL,
  input_manifest_sha256 char(64) NOT NULL,
  affected_row_count bigint NOT NULL,
  blocker_reason varchar(512) DEFAULT NULL
);

INSERT INTO c00_rollback_plan_report
SELECT 'drop_pqc_event_generated_unique', SHA2('drop-pqc-event-generated-v1', 256), COUNT(1),
       'requires old application restored and all writes stopped before execution'
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pro_process_pool_event'
   AND column_name = 'pqc_submission_task_id';

INSERT INTO c00_rollback_plan_report
SELECT 'drop_pqc_task_submission_columns', SHA2('drop-pqc-task-submission-v1', 256), COUNT(1),
       'requires backup restore of submitted content before execution'
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pqc_inspection_task'
   AND column_name IN ('inspection_rule_key', 'submitted_content_hash', 'submitted_event_id');

INSERT INTO c00_rollback_plan_report
SELECT 'drop_active_order_qa_snapshot_columns', SHA2('drop-active-order-snapshot-v1', 256), COUNT(1),
       'requires backup restore of active-order snapshot evidence before execution'
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pro_process_pool_active_order'
   AND column_name IN ('dcc_project_code_id', 'qa_regulation_id', 'qa_regulation_version_id');

INSERT INTO c00_rollback_plan_report
SELECT 'drop_route_dcc_binding_table', SHA2('drop-route-dcc-v1', 256), COUNT(1),
       'requires approved route-DCC manifest retained for forward reapply'
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pro_route_dcc_project_binding';

SELECT * FROM c00_rollback_plan_report ORDER BY rollback_step;
SELECT 'C00 rollback dry-run only; execute destructive statements only from the approved maintenance rollback runbook' AS c00_rollback_status;
