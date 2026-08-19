-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_pqc_dcc_qa_c00_schema; type=schema; riskLevel=high
-- C00 postflight verifies zero blockers before NOT NULL tightening and final unique-key reliance.

DROP TEMPORARY TABLE IF EXISTS c00_postflight_blocker_report;
CREATE TEMPORARY TABLE c00_postflight_blocker_report (
  blocker_scope varchar(64) NOT NULL,
  source_id bigint DEFAULT NULL,
  input_manifest_sha256 char(64) NOT NULL,
  affected_row_count bigint NOT NULL,
  blocker_reason varchar(512) NOT NULL
);

INSERT INTO c00_postflight_blocker_report
SELECT 'active_order_snapshot_null', id, SHA2(CONCAT('active-order-null:', id), 256), 1,
       'active-order QA snapshot columns must be non-null before C00 NOT NULL tightening'
  FROM mes_pro_process_pool_active_order
 WHERE dcc_project_code_id IS NULL
    OR qa_regulation_id IS NULL
    OR qa_regulation_version_id IS NULL;

INSERT INTO c00_postflight_blocker_report
SELECT 'task_rule_key_null', id, SHA2(CONCAT('task-rule-null:', id), 256), 1,
       'PQC task inspectionRuleKey must be non-null before rule identity unique key becomes authoritative'
  FROM mes_pqc_inspection_task
 WHERE inspection_rule_key IS NULL;

INSERT INTO c00_postflight_blocker_report
SELECT 'submitted_task_missing_hash_or_event', id, SHA2(CONCAT('CanonicalPqcSubmissionV1:', id), 256), 1,
       'submitted or confirmed PQC task must have submittedContentHash and submittedEventId'
  FROM mes_pqc_inspection_task
 WHERE deleted = b'0'
   AND task_status IN ('SUBMITTED', 'CONFIRMED')
   AND (submitted_content_hash IS NULL OR submitted_event_id IS NULL);

INSERT INTO c00_postflight_blocker_report
SELECT 'pending_task_has_formal_submission', id, SHA2(CONCAT('pending-submission:', id), 256), 1,
       'PENDING PQC task must not carry submittedContentHash or submittedEventId'
  FROM mes_pqc_inspection_task
 WHERE deleted = b'0'
   AND task_status = 'PENDING'
   AND (submitted_content_hash IS NOT NULL OR submitted_event_id IS NOT NULL);

INSERT INTO c00_postflight_blocker_report
SELECT 'formal_pqc_event_duplicate', pqc_submission_task_id, SHA2(CONCAT('pqc-event-task:', pqc_submission_task_id), 256), COUNT(1),
       'one task must map to exactly one formal PQC event'
  FROM mes_pro_process_pool_event
 WHERE pqc_submission_task_id IS NOT NULL
 GROUP BY tenant_id, pqc_submission_task_id
HAVING COUNT(1) > 1;

INSERT INTO c00_postflight_blocker_report
SELECT 'task_rule_identity_duplicate', NULL,
       SHA2(CONCAT('task-rule-identity:', tenant_id, ':', active_order_id, ':', regulation_version_id, ':', COALESCE(qa_process_id, 0), ':', inspection_rule_key, ':', business_date), 256),
       COUNT(1),
       'duplicate PQC task rule identities must be resolved before unique key tightening'
  FROM mes_pqc_inspection_task
 WHERE deleted = b'0'
   AND inspection_rule_key IS NOT NULL
 GROUP BY tenant_id, active_order_id, regulation_version_id, qa_process_id, inspection_rule_key, business_date, deleted
HAVING COUNT(1) > 1;

INSERT INTO c00_postflight_blocker_report
SELECT 'submitted_event_duplicate', submitted_event_id,
       SHA2(CONCAT('submitted-event:', submitted_event_id), 256), COUNT(1),
       'submittedEventId must be unique across non-deleted PQC tasks before unique key tightening'
  FROM mes_pqc_inspection_task
 WHERE deleted = b'0'
   AND submitted_event_id IS NOT NULL
 GROUP BY tenant_id, submitted_event_id, deleted
HAVING COUNT(1) > 1;

DROP TEMPORARY TABLE IF EXISTS c00_postflight_hash_report;
CREATE TEMPORARY TABLE c00_postflight_hash_report (
  report_scope varchar(64) NOT NULL,
  input_manifest_sha256 char(64) NOT NULL,
  affected_row_count bigint NOT NULL,
  blocker_reason varchar(512) DEFAULT NULL
);

INSERT INTO c00_postflight_hash_report
SELECT 'active_order_snapshot',
       COALESCE(SHA2(GROUP_CONCAT(CONCAT(id, ':', dcc_project_code_id, ':', qa_regulation_version_id) ORDER BY id SEPARATOR '|'), 256), SHA2('empty-active-order-snapshot', 256)),
       COUNT(1), NULL
  FROM mes_pro_process_pool_active_order;

INSERT INTO c00_postflight_hash_report
SELECT 'task_rule_and_submission',
       COALESCE(SHA2(GROUP_CONCAT(CONCAT(id, ':', inspection_rule_key, ':', COALESCE(submitted_content_hash, ''), ':', COALESCE(submitted_event_id, 0)) ORDER BY id SEPARATOR '|'), 256), SHA2('empty-task-rule-and-submission', 256)),
       COUNT(1), NULL
  FROM mes_pqc_inspection_task;

SELECT * FROM c00_postflight_blocker_report ORDER BY blocker_scope, source_id;
SELECT * FROM c00_postflight_hash_report ORDER BY report_scope;

SET @c00_postflight_blocker_count := (SELECT COUNT(1) FROM c00_postflight_blocker_report);
SET @sql := IF(@c00_postflight_blocker_count = 0,
  'ALTER TABLE `mes_pro_process_pool_active_order` MODIFY COLUMN `dcc_project_code_id` bigint NOT NULL COMMENT ''订单锁定DCC项目代码ID'', MODIFY COLUMN `qa_regulation_id` bigint NOT NULL COMMENT ''订单锁定QA规程ID'', MODIFY COLUMN `qa_regulation_version_id` bigint NOT NULL COMMENT ''订单锁定QA规程发布版本ID''',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''C00 postflight blockers remain for active-order QA snapshots''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@c00_postflight_blocker_count = 0,
  'ALTER TABLE `mes_pqc_inspection_task` MODIFY COLUMN `inspection_rule_key` varchar(32) NOT NULL COMMENT ''正式检验规则身份''',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''C00 postflight blockers remain for PQC task rule identity''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c00_old_task_identity_index_exists := (
  SELECT COUNT(DISTINCT index_name)
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND index_name = 'uk_mes_pqc_task_qa_identity'
);
SET @sql := IF(@c00_postflight_blocker_count = 0 AND @c00_old_task_identity_index_exists > 0,
  'ALTER TABLE `mes_pqc_inspection_task` DROP INDEX `uk_mes_pqc_task_qa_identity`',
  IF(@c00_postflight_blocker_count = 0,
     'SELECT ''C00 old task QA identity index already absent'' AS c00_postflight_status',
     'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''C00 postflight blockers remain before old task identity index replacement'''));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c00_task_rule_identity_index_exists := (
  SELECT COUNT(DISTINCT index_name)
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND index_name = 'uk_mes_pqc_task_rule_identity'
);
SET @sql := IF(@c00_postflight_blocker_count = 0 AND @c00_task_rule_identity_index_exists = 0,
  'ALTER TABLE `mes_pqc_inspection_task` ADD UNIQUE KEY `uk_mes_pqc_task_rule_identity` (`tenant_id`, `active_order_id`, `regulation_version_id`, `qa_process_id`, `inspection_rule_key`, `business_date`, `deleted`)',
  IF(@c00_postflight_blocker_count = 0,
     'SELECT ''C00 task rule identity unique key already exists'' AS c00_postflight_status',
     'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''C00 postflight blockers remain before task rule identity unique key'''));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c00_task_submitted_event_index_exists := (
  SELECT COUNT(DISTINCT index_name)
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND index_name = 'uk_mes_pqc_task_submitted_event'
);
SET @sql := IF(@c00_postflight_blocker_count = 0 AND @c00_task_submitted_event_index_exists = 0,
  'ALTER TABLE `mes_pqc_inspection_task` ADD UNIQUE KEY `uk_mes_pqc_task_submitted_event` (`tenant_id`, `submitted_event_id`, `deleted`)',
  IF(@c00_postflight_blocker_count = 0,
     'SELECT ''C00 task submitted event unique key already exists'' AS c00_postflight_status',
     'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''C00 postflight blockers remain before task submitted event unique key'''));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c00_pqc_event_task_index_exists := (
  SELECT COUNT(DISTINCT index_name)
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pro_process_pool_event'
     AND index_name = 'uk_mes_pro_process_pool_event_pqc_task'
);
SET @sql := IF(@c00_postflight_blocker_count = 0 AND @c00_pqc_event_task_index_exists = 0,
  'ALTER TABLE `mes_pro_process_pool_event` ADD UNIQUE KEY `uk_mes_pro_process_pool_event_pqc_task` (`tenant_id`, `pqc_submission_task_id`)',
  IF(@c00_postflight_blocker_count = 0,
     'SELECT ''C00 PQC event task unique key already exists'' AS c00_postflight_status',
     'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''C00 postflight blockers remain before PQC event task unique key'''));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
