-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_pqc_dcc_qa_c00_schema; type=data; riskLevel=high
-- C00 backfill consumes only approved ID manifests. It never infers history from labels, current QA, product names, or rawPayload.
-- Default mode is read-only. The maintenance runner must set all JSON/SHA variables below in the same MySQL session.
--
-- Required session inputs:
--   @c00_apply_backfill = 0 (dry-run) or 1 (apply)
--   @c00_route_dcc_manifest_json / @c00_route_dcc_manifest_sha256
--   @c00_active_order_snapshot_manifest_json / @c00_active_order_snapshot_manifest_sha256
--   @c00_task_submission_manifest_json / @c00_task_submission_manifest_sha256
--
-- Route manifest item:
--   tenantId, routeId, dccProjectCodeId, expectedPreviousVersion, approvedBy, approvedAt, approvalNote
-- Active-order manifest item (required exactly once for every active order that needs snapshot backfill):
--   tenantId, activeOrderId, dccProjectCodeId, qaRegulationId, qaRegulationVersionId,
--   approvedBy, approvedAt, approvalNote
-- Historical task regulation versions are cross-check evidence only and never create an order snapshot candidate.
-- Submission manifest item:
--   tenantId, taskId, submittedEventId, pqcRecordId, signatureId, pieceDetailCount,
--   pieceDetailSha256, submittedContentHash, canonicalPayloadJson,
--   approvedBy, approvedAt, approvalNote
-- canonicalPayloadJson must be the exact compact UTF-8 JSON emitted by the shared
-- CanonicalPqcSubmissionV1 implementation. The script hashes that exact text and
-- separately reconciles its identity/items with formal task/event/record/detail rows.

SET @c00_apply_backfill := COALESCE(@c00_apply_backfill, 0);

DROP PROCEDURE IF EXISTS migrate_mes_pqc_dcc_qa_c00_backfill;
DELIMITER $$
CREATE PROCEDURE migrate_mes_pqc_dcc_qa_c00_backfill()
BEGIN
  DECLARE v_blocker_count bigint DEFAULT 0;
  DECLARE v_expected_count bigint DEFAULT 0;
  DECLARE v_actual_count bigint DEFAULT 0;
  DECLARE v_route_update_count bigint DEFAULT 0;
  DECLARE v_route_insert_count bigint DEFAULT 0;
  DECLARE v_active_order_update_count bigint DEFAULT 0;
  DECLARE v_task_rule_update_count bigint DEFAULT 0;
  DECLARE v_task_submission_update_count bigint DEFAULT 0;
  DECLARE v_previous_group_concat_max_len bigint DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    SET SESSION group_concat_max_len = v_previous_group_concat_max_len;
    RESIGNAL;
  END;

  IF @c00_apply_backfill NOT IN (0, 1) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C00 apply flag must be 0 or 1';
  END IF;

  SET v_previous_group_concat_max_len = @@SESSION.group_concat_max_len;
  SET SESSION group_concat_max_len = 67108864;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_blocker_report;
  CREATE TEMPORARY TABLE c00_backfill_blocker_report (
    blocker_scope varchar(64) NOT NULL,
    source_id bigint DEFAULT NULL,
    input_manifest_sha256 char(64) NOT NULL,
    affected_row_count bigint NOT NULL,
    blocker_reason varchar(512) NOT NULL
  ) ENGINE=InnoDB;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_approved_route_dcc_binding;
  CREATE TEMPORARY TABLE c00_backfill_approved_route_dcc_binding (
    tenant_id bigint DEFAULT NULL,
    route_id bigint DEFAULT NULL,
    dcc_project_code_id bigint DEFAULT NULL,
    expected_previous_version bigint DEFAULT NULL,
    approved_by varchar(64) DEFAULT NULL,
    approved_at datetime DEFAULT NULL,
    approval_note varchar(512) DEFAULT NULL,
    input_manifest_sha256 char(64) DEFAULT NULL
  ) ENGINE=InnoDB;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_approved_active_order_snapshot;
  CREATE TEMPORARY TABLE c00_backfill_approved_active_order_snapshot (
    tenant_id bigint DEFAULT NULL,
    active_order_id bigint DEFAULT NULL,
    dcc_project_code_id bigint DEFAULT NULL,
    qa_regulation_id bigint DEFAULT NULL,
    qa_regulation_version_id bigint DEFAULT NULL,
    approved_by varchar(64) DEFAULT NULL,
    approved_at datetime DEFAULT NULL,
    approval_note varchar(512) DEFAULT NULL,
    input_manifest_sha256 char(64) DEFAULT NULL
  ) ENGINE=InnoDB;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_approved_task_submission;
  CREATE TEMPORARY TABLE c00_backfill_approved_task_submission (
    tenant_id bigint DEFAULT NULL,
    task_id bigint DEFAULT NULL,
    submitted_event_id bigint DEFAULT NULL,
    pqc_record_id bigint DEFAULT NULL,
    signature_id bigint DEFAULT NULL,
    piece_detail_count bigint DEFAULT NULL,
    piece_detail_sha256 char(64) DEFAULT NULL,
    submitted_content_hash char(64) DEFAULT NULL,
    canonical_payload_json longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
    approved_by varchar(64) DEFAULT NULL,
    approved_at datetime DEFAULT NULL,
    approval_note varchar(512) DEFAULT NULL,
    input_manifest_sha256 char(64) DEFAULT NULL
  ) ENGINE=InnoDB;

  IF @c00_route_dcc_manifest_json IS NULL
     OR JSON_VALID(@c00_route_dcc_manifest_json) = 0
     OR JSON_TYPE(CAST(@c00_route_dcc_manifest_json AS JSON)) <> 'ARRAY' THEN
    INSERT INTO c00_backfill_blocker_report
    VALUES ('route_dcc_manifest_invalid', NULL, SHA2('missing-route-dcc-manifest', 256), 0,
            'route-DCC manifest must be a valid JSON array');
  ELSEIF @c00_route_dcc_manifest_sha256 IS NULL
      OR LOWER(@c00_route_dcc_manifest_sha256) NOT REGEXP '^[0-9a-f]{64}$'
      OR LOWER(@c00_route_dcc_manifest_sha256) <> SHA2(@c00_route_dcc_manifest_json, 256) THEN
    INSERT INTO c00_backfill_blocker_report
    VALUES ('route_dcc_manifest_hash_mismatch', NULL, SHA2(@c00_route_dcc_manifest_json, 256), 0,
            'route-DCC manifest SHA-256 does not match the approved hash');
  ELSE
    INSERT INTO c00_backfill_approved_route_dcc_binding
      (tenant_id, route_id, dcc_project_code_id, expected_previous_version,
       approved_by, approved_at, approval_note, input_manifest_sha256)
    SELECT manifest.tenant_id,
           manifest.route_id,
           manifest.dcc_project_code_id,
           manifest.expected_previous_version,
           manifest.approved_by,
           manifest.approved_at,
           manifest.approval_note,
           LOWER(@c00_route_dcc_manifest_sha256)
      FROM JSON_TABLE(
        @c00_route_dcc_manifest_json,
        '$[*]' COLUMNS (
          tenant_id bigint PATH '$.tenantId' NULL ON EMPTY NULL ON ERROR,
          route_id bigint PATH '$.routeId' NULL ON EMPTY NULL ON ERROR,
          dcc_project_code_id bigint PATH '$.dccProjectCodeId' NULL ON EMPTY NULL ON ERROR,
          expected_previous_version bigint PATH '$.expectedPreviousVersion' NULL ON EMPTY NULL ON ERROR,
          approved_by varchar(64) PATH '$.approvedBy' NULL ON EMPTY NULL ON ERROR,
          approved_at datetime PATH '$.approvedAt' NULL ON EMPTY NULL ON ERROR,
          approval_note varchar(512) PATH '$.approvalNote' NULL ON EMPTY NULL ON ERROR
        )
      ) manifest;
  END IF;

  IF @c00_active_order_snapshot_manifest_json IS NULL
     OR JSON_VALID(@c00_active_order_snapshot_manifest_json) = 0
     OR JSON_TYPE(CAST(@c00_active_order_snapshot_manifest_json AS JSON)) <> 'ARRAY' THEN
    INSERT INTO c00_backfill_blocker_report
    VALUES ('active_order_manifest_invalid', NULL, SHA2('missing-active-order-manifest', 256), 0,
            'active-order snapshot manifest must be a valid JSON array');
  ELSEIF @c00_active_order_snapshot_manifest_sha256 IS NULL
      OR LOWER(@c00_active_order_snapshot_manifest_sha256) NOT REGEXP '^[0-9a-f]{64}$'
      OR LOWER(@c00_active_order_snapshot_manifest_sha256)
          <> SHA2(@c00_active_order_snapshot_manifest_json, 256) THEN
    INSERT INTO c00_backfill_blocker_report
    VALUES ('active_order_manifest_hash_mismatch', NULL,
            SHA2(@c00_active_order_snapshot_manifest_json, 256), 0,
            'active-order snapshot manifest SHA-256 does not match the approved hash');
  ELSE
    INSERT INTO c00_backfill_approved_active_order_snapshot
      (tenant_id, active_order_id, dcc_project_code_id, qa_regulation_id,
       qa_regulation_version_id, approved_by, approved_at, approval_note, input_manifest_sha256)
    SELECT manifest.tenant_id,
           manifest.active_order_id,
           manifest.dcc_project_code_id,
           manifest.qa_regulation_id,
           manifest.qa_regulation_version_id,
           manifest.approved_by,
           manifest.approved_at,
           manifest.approval_note,
           LOWER(@c00_active_order_snapshot_manifest_sha256)
      FROM JSON_TABLE(
        @c00_active_order_snapshot_manifest_json,
        '$[*]' COLUMNS (
          tenant_id bigint PATH '$.tenantId' NULL ON EMPTY NULL ON ERROR,
          active_order_id bigint PATH '$.activeOrderId' NULL ON EMPTY NULL ON ERROR,
          dcc_project_code_id bigint PATH '$.dccProjectCodeId' NULL ON EMPTY NULL ON ERROR,
          qa_regulation_id bigint PATH '$.qaRegulationId' NULL ON EMPTY NULL ON ERROR,
          qa_regulation_version_id bigint PATH '$.qaRegulationVersionId' NULL ON EMPTY NULL ON ERROR,
          approved_by varchar(64) PATH '$.approvedBy' NULL ON EMPTY NULL ON ERROR,
          approved_at datetime PATH '$.approvedAt' NULL ON EMPTY NULL ON ERROR,
          approval_note varchar(512) PATH '$.approvalNote' NULL ON EMPTY NULL ON ERROR
        )
      ) manifest;
  END IF;

  IF @c00_task_submission_manifest_json IS NULL
     OR JSON_VALID(@c00_task_submission_manifest_json) = 0
     OR JSON_TYPE(CAST(@c00_task_submission_manifest_json AS JSON)) <> 'ARRAY' THEN
    INSERT INTO c00_backfill_blocker_report
    VALUES ('task_submission_manifest_invalid', NULL, SHA2('missing-task-submission-manifest', 256), 0,
            'task submission manifest must be a valid JSON array');
  ELSEIF @c00_task_submission_manifest_sha256 IS NULL
      OR LOWER(@c00_task_submission_manifest_sha256) NOT REGEXP '^[0-9a-f]{64}$'
      OR LOWER(@c00_task_submission_manifest_sha256)
          <> SHA2(@c00_task_submission_manifest_json, 256) THEN
    INSERT INTO c00_backfill_blocker_report
    VALUES ('task_submission_manifest_hash_mismatch', NULL,
            SHA2(@c00_task_submission_manifest_json, 256), 0,
            'task submission manifest SHA-256 does not match the approved hash');
  ELSE
    INSERT INTO c00_backfill_approved_task_submission
      (tenant_id, task_id, submitted_event_id, pqc_record_id, signature_id,
       piece_detail_count, piece_detail_sha256, submitted_content_hash, canonical_payload_json,
       approved_by, approved_at, approval_note, input_manifest_sha256)
    SELECT manifest.tenant_id,
           manifest.task_id,
           manifest.submitted_event_id,
           manifest.pqc_record_id,
           manifest.signature_id,
           manifest.piece_detail_count,
           LOWER(manifest.piece_detail_sha256),
           LOWER(manifest.submitted_content_hash),
           manifest.canonical_payload_json,
           manifest.approved_by,
           manifest.approved_at,
           manifest.approval_note,
           LOWER(@c00_task_submission_manifest_sha256)
      FROM JSON_TABLE(
        @c00_task_submission_manifest_json,
        '$[*]' COLUMNS (
          tenant_id bigint PATH '$.tenantId' NULL ON EMPTY NULL ON ERROR,
          task_id bigint PATH '$.taskId' NULL ON EMPTY NULL ON ERROR,
          submitted_event_id bigint PATH '$.submittedEventId' NULL ON EMPTY NULL ON ERROR,
          pqc_record_id bigint PATH '$.pqcRecordId' NULL ON EMPTY NULL ON ERROR,
          signature_id bigint PATH '$.signatureId' NULL ON EMPTY NULL ON ERROR,
          piece_detail_count bigint PATH '$.pieceDetailCount' NULL ON EMPTY NULL ON ERROR,
          piece_detail_sha256 varchar(64) PATH '$.pieceDetailSha256' NULL ON EMPTY NULL ON ERROR,
          submitted_content_hash varchar(64) PATH '$.submittedContentHash' NULL ON EMPTY NULL ON ERROR,
          canonical_payload_json longtext PATH '$.canonicalPayloadJson' NULL ON EMPTY NULL ON ERROR,
          approved_by varchar(64) PATH '$.approvedBy' NULL ON EMPTY NULL ON ERROR,
          approved_at datetime PATH '$.approvedAt' NULL ON EMPTY NULL ON ERROR,
          approval_note varchar(512) PATH '$.approvalNote' NULL ON EMPTY NULL ON ERROR
        )
      ) manifest;
  END IF;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'route_dcc_manifest_row_invalid', route_id,
         COALESCE(input_manifest_sha256, SHA2('invalid-route-dcc-row', 256)), 1,
         'route-DCC manifest row has missing identity, approval, or expected version'
    FROM c00_backfill_approved_route_dcc_binding
   WHERE tenant_id IS NULL OR tenant_id <= 0
      OR route_id IS NULL OR route_id <= 0
      OR dcc_project_code_id IS NULL OR dcc_project_code_id <= 0
      OR expected_previous_version IS NULL OR expected_previous_version < 0
      OR approved_by IS NULL OR TRIM(approved_by) = ''
      OR approved_at IS NULL
      OR approval_note IS NULL OR TRIM(approval_note) = '';

  INSERT INTO c00_backfill_blocker_report
  SELECT 'route_dcc_manifest_duplicate', route_id, MIN(input_manifest_sha256), COUNT(1),
         'route-DCC manifest must contain one row per tenant and route'
    FROM c00_backfill_approved_route_dcc_binding
   GROUP BY tenant_id, route_id
  HAVING COUNT(1) <> 1;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'active_order_manifest_row_invalid', active_order_id,
         COALESCE(input_manifest_sha256, SHA2('invalid-active-order-row', 256)), 1,
         'active-order manifest row has missing identity or approval'
    FROM c00_backfill_approved_active_order_snapshot
   WHERE tenant_id IS NULL OR tenant_id <= 0
      OR active_order_id IS NULL OR active_order_id <= 0
      OR dcc_project_code_id IS NULL OR dcc_project_code_id <= 0
      OR qa_regulation_id IS NULL OR qa_regulation_id <= 0
      OR qa_regulation_version_id IS NULL OR qa_regulation_version_id <= 0
      OR approved_by IS NULL OR TRIM(approved_by) = ''
      OR approved_at IS NULL
      OR approval_note IS NULL OR TRIM(approval_note) = '';

  INSERT INTO c00_backfill_blocker_report
  SELECT 'active_order_manifest_duplicate', active_order_id, MIN(input_manifest_sha256), COUNT(1),
         'active-order manifest must contain one row per tenant and active order'
    FROM c00_backfill_approved_active_order_snapshot
   GROUP BY tenant_id, active_order_id
  HAVING COUNT(1) <> 1;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'task_submission_manifest_row_invalid', task_id,
         COALESCE(input_manifest_sha256, SHA2('invalid-task-submission-row', 256)), 1,
         'task submission manifest row has missing identity, evidence hash, or approval'
    FROM c00_backfill_approved_task_submission
   WHERE tenant_id IS NULL OR tenant_id <= 0
      OR task_id IS NULL OR task_id <= 0
      OR submitted_event_id IS NULL OR submitted_event_id <= 0
      OR pqc_record_id IS NULL OR pqc_record_id <= 0
      OR signature_id IS NULL OR signature_id <= 0
      OR piece_detail_count IS NULL OR piece_detail_count <= 0
      OR piece_detail_sha256 IS NULL OR piece_detail_sha256 NOT REGEXP '^[0-9a-f]{64}$'
      OR submitted_content_hash IS NULL OR submitted_content_hash NOT REGEXP '^[0-9a-f]{64}$'
      OR canonical_payload_json IS NULL OR TRIM(canonical_payload_json) = ''
      OR approved_by IS NULL OR TRIM(approved_by) = ''
      OR approved_at IS NULL
      OR approval_note IS NULL OR TRIM(approval_note) = '';

  INSERT INTO c00_backfill_blocker_report
  SELECT 'task_submission_manifest_duplicate_task', task_id, MIN(input_manifest_sha256), COUNT(1),
         'task submission manifest must contain one row per tenant and task'
    FROM c00_backfill_approved_task_submission
   GROUP BY tenant_id, task_id
  HAVING COUNT(1) <> 1;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'canonical_payload_invalid', manifest.task_id, manifest.input_manifest_sha256, 1,
         'canonicalPayloadJson must be one exact 11-field CanonicalPqcSubmissionV1 JSON object'
    FROM c00_backfill_approved_task_submission manifest
   WHERE CASE
           WHEN JSON_VALID(manifest.canonical_payload_json) = 1 THEN
             JSON_TYPE(CAST(manifest.canonical_payload_json AS JSON)) <> 'OBJECT'
             OR JSON_LENGTH(CAST(manifest.canonical_payload_json AS JSON)) <> 11
             OR JSON_CONTAINS_PATH(
                  CAST(manifest.canonical_payload_json AS JSON),
                  'all',
                  '$.activeOrderId',
                  '$.regulationVersionId',
                  '$.qaProcessId',
                  '$.pqcTaskId',
                  '$.inspectionRuleKey',
                  '$.actualEmployeeId',
                  '$.productionSubmitEventId',
                  '$.actualInspectionQuantity',
                  '$.scrapQuantity',
                  '$.nonconformanceDescription',
                  '$.itemResults') <> 1
           ELSE TRUE
         END;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'canonical_payload_hash_mismatch', manifest.task_id, manifest.input_manifest_sha256, 1,
         'submittedContentHash must equal the SHA-256 of the exact canonicalPayloadJson text'
    FROM c00_backfill_approved_task_submission manifest
   WHERE manifest.canonical_payload_json IS NOT NULL
     AND SHA2(manifest.canonical_payload_json, 256) <> manifest.submitted_content_hash;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'task_submission_manifest_duplicate_event', submitted_event_id,
         MIN(input_manifest_sha256), COUNT(1),
         'task submission manifest must not reuse one event for multiple tasks'
    FROM c00_backfill_approved_task_submission
   GROUP BY tenant_id, submitted_event_id
  HAVING COUNT(1) <> 1;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'route_dcc_manifest_target_missing', manifest.route_id, manifest.input_manifest_sha256, 1,
         'approved route or DCC project does not exist in the same tenant'
    FROM c00_backfill_approved_route_dcc_binding manifest
    LEFT JOIN mes_pro_route route_master
      ON route_master.tenant_id = manifest.tenant_id
     AND route_master.id = manifest.route_id
    LEFT JOIN dcc_project_code dcc
      ON dcc.tenant_id = manifest.tenant_id
     AND dcc.id = manifest.dcc_project_code_id
     AND dcc.deleted = 0
   WHERE route_master.id IS NULL OR dcc.id IS NULL;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_route_state;
  CREATE TEMPORARY TABLE c00_backfill_route_state AS
  SELECT manifest.tenant_id,
         manifest.route_id,
         manifest.dcc_project_code_id,
         manifest.expected_previous_version,
         manifest.input_manifest_sha256,
         current_binding.id AS current_binding_id,
         current_binding.dcc_project_code_id AS current_dcc_project_code_id,
         current_binding.version AS current_version,
         COALESCE(history.max_history_version, 0) AS max_history_version
    FROM c00_backfill_approved_route_dcc_binding manifest
    LEFT JOIN mes_pro_route_dcc_project_binding current_binding
      ON current_binding.tenant_id = manifest.tenant_id
     AND current_binding.route_id = manifest.route_id
     AND current_binding.deleted = b'0'
    LEFT JOIN (
      SELECT tenant_id, route_id, MAX(version) AS max_history_version
        FROM mes_pro_route_dcc_project_binding
       GROUP BY tenant_id, route_id
    ) history
      ON history.tenant_id = manifest.tenant_id
     AND history.route_id = manifest.route_id;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'route_dcc_version_mismatch', route_id, input_manifest_sha256, 1,
         'route-DCC current/history version does not match expectedPreviousVersion or applied target'
    FROM c00_backfill_route_state
   WHERE (current_binding_id IS NULL AND max_history_version <> expected_previous_version)
      OR (current_binding_id IS NOT NULL
          AND current_dcc_project_code_id = dcc_project_code_id
          AND (current_version NOT IN (expected_previous_version, expected_previous_version + 1)
               OR max_history_version <> current_version))
      OR (current_binding_id IS NOT NULL
          AND current_dcc_project_code_id <> dcc_project_code_id
          AND (current_version <> expected_previous_version
               OR max_history_version <> expected_previous_version));

  INSERT INTO c00_backfill_blocker_report
  SELECT 'route_dcc_manifest_missing_for_active_order', active_order.route_id,
         SHA2(CONCAT('active-order-route:', active_order.tenant_id, ':', active_order.route_id), 256),
         COUNT(1),
         'every historical active-order route requires an approved route-DCC manifest row'
    FROM mes_pro_process_pool_active_order active_order
    LEFT JOIN c00_backfill_approved_route_dcc_binding manifest
      ON manifest.tenant_id = active_order.tenant_id
     AND manifest.route_id = active_order.route_id
   WHERE manifest.route_id IS NULL
   GROUP BY active_order.tenant_id, active_order.route_id;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_active_order_task_summary;
  CREATE TEMPORARY TABLE c00_backfill_active_order_task_summary AS
  SELECT active_order.tenant_id,
         active_order.id AS active_order_id,
         active_order.route_id,
         COUNT(task.id) AS task_count,
         COUNT(DISTINCT task.regulation_version_id) AS distinct_version_count,
         SUM(CASE WHEN task.id IS NOT NULL AND task.regulation_version_id IS NULL THEN 1 ELSE 0 END)
           AS null_version_count,
         CASE
           WHEN COUNT(task.id) > 0
            AND COUNT(DISTINCT task.regulation_version_id) = 1
            AND SUM(CASE WHEN task.id IS NOT NULL AND task.regulation_version_id IS NULL
                         THEN 1 ELSE 0 END) = 0
           THEN MAX(task.regulation_version_id)
           ELSE NULL
         END AS unique_regulation_version_id
    FROM mes_pro_process_pool_active_order active_order
    LEFT JOIN mes_pqc_inspection_task task
      ON task.tenant_id = active_order.tenant_id
     AND task.active_order_id = active_order.id
   GROUP BY active_order.tenant_id, active_order.id, active_order.route_id;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'active_order_manifest_task_version_ambiguous', manifest.active_order_id,
         manifest.input_manifest_sha256, summary.task_count,
         'non-zero task history must already prove exactly one regulation version matching the approved snapshot'
    FROM c00_backfill_approved_active_order_snapshot manifest
    JOIN c00_backfill_active_order_task_summary summary
      ON summary.tenant_id = manifest.tenant_id
     AND summary.active_order_id = manifest.active_order_id
   WHERE summary.task_count > 0
     AND (summary.distinct_version_count <> 1
          OR summary.null_version_count <> 0
          OR summary.unique_regulation_version_id <> manifest.qa_regulation_version_id);

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_active_order_candidate;
  CREATE TEMPORARY TABLE c00_backfill_active_order_candidate (
    tenant_id bigint NOT NULL,
    active_order_id bigint NOT NULL,
    route_id bigint NOT NULL,
    dcc_project_code_id bigint NOT NULL,
    qa_regulation_id bigint NOT NULL,
    qa_regulation_version_id bigint NOT NULL,
    evidence_source varchar(32) NOT NULL,
    input_manifest_sha256 char(64) NOT NULL,
    PRIMARY KEY (tenant_id, active_order_id)
  ) ENGINE=InnoDB;

  INSERT INTO c00_backfill_active_order_candidate
    (tenant_id, active_order_id, route_id, dcc_project_code_id, qa_regulation_id,
     qa_regulation_version_id, evidence_source, input_manifest_sha256)
  SELECT manifest.tenant_id,
         manifest.active_order_id,
         active_order.route_id,
         manifest.dcc_project_code_id,
         manifest.qa_regulation_id,
         manifest.qa_regulation_version_id,
         'APPROVED_MANIFEST',
         manifest.input_manifest_sha256
    FROM c00_backfill_approved_active_order_snapshot manifest
    JOIN mes_pro_process_pool_active_order active_order
      ON active_order.tenant_id = manifest.tenant_id
     AND active_order.id = manifest.active_order_id
    JOIN c00_backfill_active_order_task_summary summary
      ON summary.tenant_id = manifest.tenant_id
     AND summary.active_order_id = manifest.active_order_id
   WHERE summary.task_count = 0
      OR (summary.distinct_version_count = 1
          AND summary.null_version_count = 0
          AND summary.unique_regulation_version_id = manifest.qa_regulation_version_id);

  INSERT INTO c00_backfill_blocker_report
  SELECT 'active_order_manifest_target_missing', candidate.active_order_id,
         candidate.input_manifest_sha256, 1,
         'active-order snapshot does not resolve to one same-tenant PUBLISHED/RETIRED QA version and DCC regulation'
    FROM c00_backfill_active_order_candidate candidate
    LEFT JOIN mes_qa_inspection_regulation_version version
      ON version.tenant_id = candidate.tenant_id
     AND version.id = candidate.qa_regulation_version_id
     AND version.regulation_id = candidate.qa_regulation_id
     AND version.deleted = b'0'
     AND version.lifecycle_status IN ('PUBLISHED', 'RETIRED')
    LEFT JOIN mes_qa_inspection_regulation regulation
      ON regulation.tenant_id = candidate.tenant_id
     AND regulation.id = candidate.qa_regulation_id
     AND regulation.dcc_project_code_id = candidate.dcc_project_code_id
     AND regulation.deleted = b'0'
    LEFT JOIN dcc_project_code dcc
      ON dcc.tenant_id = candidate.tenant_id
     AND dcc.id = candidate.dcc_project_code_id
     AND dcc.deleted = 0
   WHERE version.id IS NULL OR regulation.id IS NULL OR dcc.id IS NULL;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'active_order_route_dcc_mismatch', candidate.active_order_id,
         candidate.input_manifest_sha256, 1,
         'active-order QA evidence does not match the approved route-DCC relation'
    FROM c00_backfill_active_order_candidate candidate
    LEFT JOIN c00_backfill_approved_route_dcc_binding route_manifest
      ON route_manifest.tenant_id = candidate.tenant_id
     AND route_manifest.route_id = candidate.route_id
   WHERE route_manifest.route_id IS NULL
      OR route_manifest.dcc_project_code_id <> candidate.dcc_project_code_id;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'active_order_manifest_missing', summary.active_order_id,
         SHA2(CONCAT('active-order-manifest:', summary.tenant_id, ':', summary.active_order_id), 256),
         summary.task_count,
         'every active order requires exactly one approved snapshot manifest row; task versions are cross-check evidence only'
    FROM c00_backfill_active_order_task_summary summary
    LEFT JOIN c00_backfill_active_order_candidate candidate
      ON candidate.tenant_id = summary.tenant_id
     AND candidate.active_order_id = summary.active_order_id
   WHERE candidate.active_order_id IS NULL;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_task_rule_candidate;
  CREATE TEMPORARY TABLE c00_backfill_task_rule_candidate AS
  SELECT task.tenant_id,
         task.id AS task_id,
         CASE
           WHEN task.inspection_type = 'FIRST'
            AND task.shift_code = 'FIRST' AND task.round_no = 1 THEN 'FIRST'
           WHEN task.inspection_type = 'PATROL'
            AND task.shift_code = 'AM' AND task.round_no = 1 THEN 'PATROL_AM'
           WHEN task.inspection_type = 'PATROL'
            AND task.shift_code = 'PM' AND task.round_no = 1 THEN 'PATROL_PM'
           WHEN task.inspection_type = 'FINAL'
            AND task.shift_code = 'FINAL' AND task.round_no = 1 THEN 'FINAL'
           ELSE NULL
         END AS expected_rule_key
    FROM mes_pqc_inspection_task task;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'task_rule_key_ambiguous', task.id,
         SHA2(CONCAT('task-rule:', task.tenant_id, ':', task.id), 256), 1,
         'PQC task inspection type, shift, and round do not prove one canonical rule key'
    FROM mes_pqc_inspection_task task
    JOIN c00_backfill_task_rule_candidate candidate
      ON candidate.tenant_id = task.tenant_id
     AND candidate.task_id = task.id
   WHERE candidate.expected_rule_key IS NULL
      OR (task.inspection_rule_key IS NOT NULL
          AND task.inspection_rule_key <> candidate.expected_rule_key);

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_task_event_evidence;
  CREATE TEMPORARY TABLE c00_backfill_task_event_evidence AS
  SELECT task.tenant_id,
         task.id AS task_id,
         task.task_status,
         task.submitted_content_hash,
         task.submitted_event_id,
         COUNT(event.id) AS formal_event_count,
         MIN(event.id) AS unique_event_id
    FROM mes_pqc_inspection_task task
    LEFT JOIN mes_pro_process_pool_event event
      ON event.tenant_id = task.tenant_id
     AND event.feedback_source_id = task.id
     AND event.event_type = 'PQC_INSPECTION'
     AND event.feedback_source_type = 'MES_PQC_INSPECTION_TASK'
     AND event.deleted = b'0'
   WHERE task.task_status IN ('SUBMITTED', 'CONFIRMED')
   GROUP BY task.tenant_id, task.id, task.task_status,
            task.submitted_content_hash, task.submitted_event_id;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'formal_event_cardinality', task_id,
         SHA2(CONCAT('formal-event:', tenant_id, ':', task_id), 256), formal_event_count,
         'SUBMITTED/CONFIRMED PQC task must have exactly one formal PQC event'
    FROM c00_backfill_task_event_evidence
   WHERE formal_event_count <> 1;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'pending_task_has_formal_submission', task.id,
         SHA2(CONCAT('pending-formal-submission:', task.tenant_id, ':', task.id), 256), 1,
         'PENDING task must not carry a formal event, submitted hash, or submitted event pointer'
    FROM mes_pqc_inspection_task task
   WHERE task.task_status = 'PENDING'
     AND (task.submitted_content_hash IS NOT NULL
          OR task.submitted_event_id IS NOT NULL
          OR EXISTS (
            SELECT 1
              FROM mes_pro_process_pool_event event
             WHERE event.tenant_id = task.tenant_id
               AND event.feedback_source_id = task.id
               AND event.event_type = 'PQC_INSPECTION'
               AND event.feedback_source_type = 'MES_PQC_INSPECTION_TASK'
               AND event.deleted = b'0'
          ));

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_piece_evidence;
  CREATE TEMPORARY TABLE c00_backfill_piece_evidence AS
  SELECT detail.tenant_id,
         detail.task_id,
         COUNT(1) AS piece_detail_count,
         SHA2(
           GROUP_CONCAT(
             CONCAT_WS(':',
               detail.id,
               detail.sample_no,
               COALESCE(HEX(detail.item_code), '~'),
               COALESCE(HEX(detail.item_name), '~'),
               COALESCE(HEX(detail.result_type), '~'),
               COALESCE(HEX(detail.measured_value), '~'),
               COALESCE(HEX(detail.judgement), '~'),
               COALESCE(CAST(detail.selected_equipment_id AS CHAR), '~'),
               COALESCE(HEX(detail.selected_equipment_number), '~'))
             ORDER BY detail.item_code, detail.sample_no, detail.id
             SEPARATOR '|'
           ),
           256
         ) AS piece_detail_sha256
    FROM mes_pqc_inspection_piece_detail detail
   WHERE detail.deleted = b'0'
   GROUP BY detail.tenant_id, detail.task_id;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_valid_canonical_payload;
  CREATE TEMPORARY TABLE c00_backfill_valid_canonical_payload AS
  SELECT manifest.*
    FROM c00_backfill_approved_task_submission manifest
   WHERE JSON_VALID(manifest.canonical_payload_json) = 1
     AND JSON_TYPE(CAST(manifest.canonical_payload_json AS JSON)) = 'OBJECT'
     AND JSON_LENGTH(CAST(manifest.canonical_payload_json AS JSON)) = 11
     AND JSON_CONTAINS_PATH(
           CAST(manifest.canonical_payload_json AS JSON),
           'all',
           '$.activeOrderId',
           '$.regulationVersionId',
           '$.qaProcessId',
           '$.pqcTaskId',
           '$.inspectionRuleKey',
           '$.actualEmployeeId',
           '$.productionSubmitEventId',
           '$.actualInspectionQuantity',
           '$.scrapQuantity',
           '$.nonconformanceDescription',
           '$.itemResults') = 1
     AND SHA2(manifest.canonical_payload_json, 256) = manifest.submitted_content_hash;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_canonical_item;
  CREATE TEMPORARY TABLE c00_backfill_canonical_item (
    tenant_id bigint NOT NULL,
    task_id bigint NOT NULL,
    item_ordinal int NOT NULL,
    item_code varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    selected_equipment_id bigint DEFAULT NULL,
    selected_equipment_number varchar(128)
      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    sample_values_json json DEFAULT NULL,
    input_manifest_sha256 char(64) NOT NULL
  ) ENGINE=InnoDB;

  INSERT INTO c00_backfill_canonical_item
    (tenant_id, task_id, item_ordinal, item_code, selected_equipment_id,
     selected_equipment_number, sample_values_json, input_manifest_sha256)
  SELECT manifest.tenant_id,
         manifest.task_id,
         canonical_item.item_ordinal,
         canonical_item.item_code,
         canonical_item.selected_equipment_id,
         canonical_item.selected_equipment_number,
         canonical_item.sample_values_json,
         manifest.input_manifest_sha256
    FROM c00_backfill_valid_canonical_payload manifest
    JOIN JSON_TABLE(manifest.canonical_payload_json, '$.itemResults[*]' COLUMNS (
      item_ordinal FOR ORDINALITY,
      item_code varchar(128) PATH '$.itemCode' NULL ON EMPTY NULL ON ERROR,
      selected_equipment_id bigint PATH '$.selectedEquipmentId' NULL ON EMPTY NULL ON ERROR,
      selected_equipment_number varchar(128)
        PATH '$.selectedEquipmentNumber' NULL ON EMPTY NULL ON ERROR,
      sample_values_json json PATH '$.sampleValues' NULL ON EMPTY NULL ON ERROR
    )) canonical_item;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_canonical_sample;
  CREATE TEMPORARY TABLE c00_backfill_canonical_sample (
    tenant_id bigint NOT NULL,
    task_id bigint NOT NULL,
    item_code varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sample_no int NOT NULL,
    measured_value varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    input_manifest_sha256 char(64) NOT NULL
  ) ENGINE=InnoDB;

  INSERT INTO c00_backfill_canonical_sample
    (tenant_id, task_id, item_code, sample_no, measured_value, input_manifest_sha256)
  SELECT canonical_item.tenant_id,
         canonical_item.task_id,
         canonical_item.item_code,
         canonical_sample.sample_no,
         canonical_sample.measured_value,
         canonical_item.input_manifest_sha256
    FROM c00_backfill_canonical_item canonical_item
    JOIN JSON_TABLE(canonical_item.sample_values_json, '$[*]' COLUMNS (
      sample_no FOR ORDINALITY,
      measured_value varchar(2000) PATH '$' NULL ON EMPTY NULL ON ERROR
    )) canonical_sample
   WHERE JSON_TYPE(canonical_item.sample_values_json) = 'ARRAY';

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_piece_item_summary;
  CREATE TEMPORARY TABLE c00_backfill_piece_item_summary AS
  SELECT detail.tenant_id,
         detail.task_id,
         detail.item_code,
         COUNT(1) AS sample_count,
         COUNT(DISTINCT COALESCE(CAST(detail.selected_equipment_id AS CHAR), '~NULL~'))
           AS equipment_id_variant_count,
         COUNT(DISTINCT COALESCE(detail.selected_equipment_number, '~NULL~'))
           AS equipment_number_variant_count,
         MIN(detail.selected_equipment_id) AS selected_equipment_id,
         MIN(detail.selected_equipment_number) AS selected_equipment_number
    FROM mes_pqc_inspection_piece_detail detail
   WHERE detail.deleted = b'0'
   GROUP BY detail.tenant_id, detail.task_id, detail.item_code;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'canonical_payload_field_mismatch', manifest.task_id,
         manifest.input_manifest_sha256, 1,
         'canonicalPayloadJson identity/quantity/result fields do not match formal task, event, record, or signature evidence'
    FROM c00_backfill_valid_canonical_payload manifest
    LEFT JOIN mes_pqc_inspection_task task
      ON task.tenant_id = manifest.tenant_id
     AND task.id = manifest.task_id
     AND task.task_status IN ('SUBMITTED', 'CONFIRMED')
    LEFT JOIN c00_backfill_task_rule_candidate rule_candidate
      ON rule_candidate.tenant_id = manifest.tenant_id
     AND rule_candidate.task_id = manifest.task_id
    LEFT JOIN mes_pro_process_pool_event event
      ON event.tenant_id = manifest.tenant_id
     AND event.id = manifest.submitted_event_id
     AND event.event_type = 'PQC_INSPECTION'
     AND event.feedback_source_type = 'MES_PQC_INSPECTION_TASK'
     AND event.feedback_source_id = manifest.task_id
     AND event.deleted = b'0'
    LEFT JOIN mes_pro_process_pool_pqc_record pqc_record
      ON pqc_record.tenant_id = manifest.tenant_id
     AND pqc_record.id = manifest.pqc_record_id
     AND pqc_record.event_id = manifest.submitted_event_id
     AND pqc_record.deleted = b'0'
    LEFT JOIN mes_pro_batch_record_execution_signature signature_record
      ON signature_record.id = manifest.signature_id
     AND signature_record.deleted = b'0'
   WHERE task.id IS NULL
      OR event.id IS NULL
      OR pqc_record.id IS NULL
      OR signature_record.id IS NULL
      OR signature_record.execution_id <> 0
      OR signature_record.action_type <> 'PQC_SUBMIT'
      OR signature_record.password_verified <> b'1'
      OR signature_record.actor_id <> event.actual_employee_id
      OR event.signature_id <> signature_record.id
      OR pqc_record.signature_id <> signature_record.id
      OR NOT (event.work_order_id <=> task.work_order_id)
      OR NOT (event.route_id <=> task.route_id)
      OR NOT (event.qa_process_id <=> task.qa_process_id)
      OR NOT (pqc_record.work_order_id <=> task.work_order_id)
      OR NOT (pqc_record.route_id <=> task.route_id)
      OR NOT (pqc_record.qa_process_id <=> task.qa_process_id)
      OR NOT (event.actual_employee_id <=> pqc_record.actual_employee_id)
      OR CAST(JSON_UNQUOTE(JSON_EXTRACT(
           manifest.canonical_payload_json, '$.activeOrderId')) AS UNSIGNED) <> task.active_order_id
      OR CAST(JSON_UNQUOTE(JSON_EXTRACT(
           manifest.canonical_payload_json, '$.regulationVersionId')) AS UNSIGNED)
           <> task.regulation_version_id
      OR CAST(JSON_UNQUOTE(JSON_EXTRACT(
           manifest.canonical_payload_json, '$.qaProcessId')) AS UNSIGNED) <> task.qa_process_id
      OR CAST(JSON_UNQUOTE(JSON_EXTRACT(
           manifest.canonical_payload_json, '$.pqcTaskId')) AS UNSIGNED) <> task.id
      OR JSON_UNQUOTE(JSON_EXTRACT(
           manifest.canonical_payload_json, '$.inspectionRuleKey'))
           <> rule_candidate.expected_rule_key
      OR CAST(JSON_UNQUOTE(JSON_EXTRACT(
           manifest.canonical_payload_json, '$.actualEmployeeId')) AS UNSIGNED)
           <> event.actual_employee_id
      OR NOT (
           CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
             manifest.canonical_payload_json, '$.productionSubmitEventId')), 'null') AS UNSIGNED)
           <=> pqc_record.production_submit_event_id)
      OR CAST(JSON_UNQUOTE(JSON_EXTRACT(
           manifest.canonical_payload_json, '$.actualInspectionQuantity')) AS UNSIGNED)
           <> task.actual_inspection_quantity
      OR CASE
           WHEN event.raw_payload IS NOT NULL AND JSON_VALID(event.raw_payload) = 1 THEN
             NOT (
               NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
                 manifest.canonical_payload_json, '$.scrapQuantity')), 'null')
               <=> NULLIF(JSON_UNQUOTE(JSON_EXTRACT(event.raw_payload, '$.scrapQuantity')), 'null'))
             OR NOT (
               NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
                 manifest.canonical_payload_json, '$.nonconformanceDescription')), 'null')
               <=> NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
                 event.raw_payload, '$.nonconformanceDescription')), 'null'))
           ELSE TRUE
         END;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_canonical_item_order_violation;
  CREATE TEMPORARY TABLE c00_backfill_canonical_item_order_violation AS
  SELECT ordered_item.tenant_id,
         ordered_item.task_id,
         ordered_item.input_manifest_sha256
    FROM (
      SELECT canonical_item.tenant_id,
             canonical_item.task_id,
             canonical_item.item_code,
             canonical_item.input_manifest_sha256,
             LAG(canonical_item.item_code) OVER (
               PARTITION BY canonical_item.tenant_id, canonical_item.task_id
               ORDER BY canonical_item.item_ordinal) AS previous_item_code
        FROM c00_backfill_canonical_item canonical_item
    ) ordered_item
   WHERE ordered_item.previous_item_code >= ordered_item.item_code;

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_canonical_item_mismatch_task;
  CREATE TEMPORARY TABLE c00_backfill_canonical_item_mismatch_task (
    tenant_id bigint NOT NULL,
    task_id bigint NOT NULL,
    input_manifest_sha256 char(64) NOT NULL,
    mismatch_reason varchar(512) NOT NULL,
    PRIMARY KEY (tenant_id, task_id)
  ) ENGINE=InnoDB;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT manifest.tenant_id, manifest.task_id, manifest.input_manifest_sha256,
         'itemResults must be a non-empty JSON array'
    FROM c00_backfill_valid_canonical_payload manifest
   WHERE JSON_TYPE(JSON_EXTRACT(manifest.canonical_payload_json, '$.itemResults')) <> 'ARRAY'
      OR JSON_LENGTH(JSON_EXTRACT(manifest.canonical_payload_json, '$.itemResults')) = 0;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT canonical_item.tenant_id, canonical_item.task_id,
         MIN(canonical_item.input_manifest_sha256),
         'itemCode and sampleValues must be present for every canonical item'
    FROM c00_backfill_canonical_item canonical_item
   WHERE canonical_item.item_code IS NULL OR TRIM(canonical_item.item_code) = ''
      OR JSON_TYPE(canonical_item.sample_values_json) <> 'ARRAY'
      OR JSON_LENGTH(canonical_item.sample_values_json) = 0
   GROUP BY canonical_item.tenant_id, canonical_item.task_id;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT canonical_item.tenant_id, canonical_item.task_id,
         MIN(canonical_item.input_manifest_sha256),
         'canonical itemResults must contain each itemCode exactly once'
    FROM c00_backfill_canonical_item canonical_item
   GROUP BY canonical_item.tenant_id, canonical_item.task_id, canonical_item.item_code
  HAVING COUNT(1) <> 1;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT order_violation.tenant_id, order_violation.task_id,
         MIN(order_violation.input_manifest_sha256),
         'canonical itemResults must be strictly ordered by itemCode'
    FROM c00_backfill_canonical_item_order_violation order_violation
   GROUP BY order_violation.tenant_id, order_violation.task_id;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT canonical_item.tenant_id, canonical_item.task_id,
         MIN(canonical_item.input_manifest_sha256),
         'canonical item equipment/sample count does not match formal piece details'
    FROM c00_backfill_canonical_item canonical_item
    LEFT JOIN c00_backfill_piece_item_summary piece_item
      ON piece_item.tenant_id = canonical_item.tenant_id
     AND piece_item.task_id = canonical_item.task_id
     AND piece_item.item_code = canonical_item.item_code
    LEFT JOIN mes_pqc_inspection_task task
      ON task.tenant_id = canonical_item.tenant_id
     AND task.id = canonical_item.task_id
   WHERE piece_item.item_code IS NULL
      OR task.id IS NULL
      OR piece_item.equipment_id_variant_count <> 1
      OR piece_item.equipment_number_variant_count <> 1
      OR piece_item.sample_count <> task.actual_inspection_quantity
      OR NOT (piece_item.selected_equipment_id <=> canonical_item.selected_equipment_id)
      OR NOT (piece_item.selected_equipment_number
              <=> canonical_item.selected_equipment_number)
   GROUP BY canonical_item.tenant_id, canonical_item.task_id;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT piece_item.tenant_id, piece_item.task_id,
         MIN(manifest.input_manifest_sha256),
         'formal piece details contain an itemCode absent from canonical itemResults'
    FROM c00_backfill_piece_item_summary piece_item
    JOIN c00_backfill_valid_canonical_payload manifest
      ON manifest.tenant_id = piece_item.tenant_id
     AND manifest.task_id = piece_item.task_id
    LEFT JOIN c00_backfill_canonical_item canonical_item
      ON canonical_item.tenant_id = piece_item.tenant_id
     AND canonical_item.task_id = piece_item.task_id
     AND canonical_item.item_code = piece_item.item_code
   WHERE canonical_item.item_code IS NULL
   GROUP BY piece_item.tenant_id, piece_item.task_id;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT canonical_sample.tenant_id, canonical_sample.task_id,
         MIN(canonical_sample.input_manifest_sha256),
         'canonical sample value or sample number does not match formal piece details'
    FROM c00_backfill_canonical_sample canonical_sample
    LEFT JOIN mes_pqc_inspection_piece_detail detail
      ON detail.tenant_id = canonical_sample.tenant_id
     AND detail.task_id = canonical_sample.task_id
     AND detail.item_code = canonical_sample.item_code
     AND detail.sample_no = canonical_sample.sample_no
     AND detail.deleted = b'0'
   WHERE detail.id IS NULL
      OR NOT (detail.measured_value <=> canonical_sample.measured_value)
   GROUP BY canonical_sample.tenant_id, canonical_sample.task_id;

  INSERT IGNORE INTO c00_backfill_canonical_item_mismatch_task
  SELECT detail.tenant_id, detail.task_id,
         MIN(manifest.input_manifest_sha256),
         'formal piece details contain a sample absent from canonical itemResults'
    FROM mes_pqc_inspection_piece_detail detail
    JOIN c00_backfill_valid_canonical_payload manifest
      ON manifest.tenant_id = detail.tenant_id
     AND manifest.task_id = detail.task_id
    LEFT JOIN c00_backfill_canonical_sample canonical_sample
      ON canonical_sample.tenant_id = detail.tenant_id
     AND canonical_sample.task_id = detail.task_id
     AND canonical_sample.item_code = detail.item_code
     AND canonical_sample.sample_no = detail.sample_no
   WHERE detail.deleted = b'0'
     AND canonical_sample.task_id IS NULL
   GROUP BY detail.tenant_id, detail.task_id;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'canonical_payload_item_mismatch', mismatch.task_id,
         mismatch.input_manifest_sha256, 1, mismatch.mismatch_reason
    FROM c00_backfill_canonical_item_mismatch_task mismatch;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'task_submission_manifest_missing', evidence.task_id,
         SHA2(CONCAT('task-submission-manifest:', evidence.tenant_id, ':', evidence.task_id), 256), 1,
         'every SUBMITTED/CONFIRMED task requires approved CanonicalPqcSubmissionV1 evidence'
    FROM c00_backfill_task_event_evidence evidence
    LEFT JOIN c00_backfill_approved_task_submission manifest
      ON manifest.tenant_id = evidence.tenant_id
     AND manifest.task_id = evidence.task_id
   WHERE evidence.formal_event_count = 1
     AND manifest.task_id IS NULL;

  INSERT INTO c00_backfill_blocker_report
  SELECT 'task_submission_manifest_evidence_mismatch', manifest.task_id,
         manifest.input_manifest_sha256, 1,
         'approved submission row does not match the unique event, PQC record, signature, or piece-detail evidence'
    FROM c00_backfill_approved_task_submission manifest
    LEFT JOIN mes_pqc_inspection_task task
      ON task.tenant_id = manifest.tenant_id
     AND task.id = manifest.task_id
     AND task.task_status IN ('SUBMITTED', 'CONFIRMED')
    LEFT JOIN c00_backfill_task_event_evidence evidence
      ON evidence.tenant_id = manifest.tenant_id
     AND evidence.task_id = manifest.task_id
    LEFT JOIN mes_pro_process_pool_event event
      ON event.tenant_id = manifest.tenant_id
     AND event.id = manifest.submitted_event_id
     AND event.event_type = 'PQC_INSPECTION'
     AND event.feedback_source_type = 'MES_PQC_INSPECTION_TASK'
     AND event.feedback_source_id = manifest.task_id
     AND event.deleted = b'0'
    LEFT JOIN mes_pro_process_pool_pqc_record pqc_record
      ON pqc_record.tenant_id = manifest.tenant_id
     AND pqc_record.id = manifest.pqc_record_id
     AND pqc_record.event_id = manifest.submitted_event_id
     AND pqc_record.deleted = b'0'
    LEFT JOIN c00_backfill_piece_evidence piece
      ON piece.tenant_id = manifest.tenant_id
     AND piece.task_id = manifest.task_id
   WHERE task.id IS NULL
      OR evidence.formal_event_count <> 1
      OR evidence.unique_event_id <> manifest.submitted_event_id
      OR event.id IS NULL
      OR pqc_record.id IS NULL
      OR event.signature_id <> manifest.signature_id
      OR pqc_record.signature_id <> manifest.signature_id
      OR event.actual_employee_id <> pqc_record.actual_employee_id
      OR event.server_submit_time <> pqc_record.server_submit_time
      OR piece.piece_detail_count <> manifest.piece_detail_count
      OR piece.piece_detail_sha256 <> manifest.piece_detail_sha256
      OR (task.submitted_event_id IS NOT NULL
          AND task.submitted_event_id <> manifest.submitted_event_id)
      OR (task.submitted_content_hash IS NOT NULL
          AND task.submitted_content_hash <> manifest.submitted_content_hash);

  INSERT INTO c00_backfill_blocker_report
  SELECT 'existing_task_submission_identity_invalid', evidence.task_id,
         SHA2(CONCAT('existing-task-submission:', evidence.tenant_id, ':', evidence.task_id), 256), 1,
         'existing submitted hash/event pointer does not match the unique formal event'
    FROM c00_backfill_task_event_evidence evidence
   WHERE evidence.formal_event_count = 1
     AND evidence.submitted_content_hash IS NOT NULL
     AND evidence.submitted_event_id IS NOT NULL
     AND (evidence.submitted_content_hash NOT REGEXP '^[0-9a-f]{64}$'
          OR evidence.submitted_event_id <> evidence.unique_event_id);

  DROP TEMPORARY TABLE IF EXISTS c00_backfill_plan_report;
  CREATE TEMPORARY TABLE c00_backfill_plan_report (
    plan_step varchar(64) NOT NULL,
    input_manifest_sha256 char(64) NOT NULL,
    affected_row_count bigint NOT NULL,
    blocker_reason varchar(512) DEFAULT NULL
  ) ENGINE=InnoDB;

  INSERT INTO c00_backfill_plan_report
  SELECT 'route_dcc_binding_upsert',
         COALESCE(MIN(input_manifest_sha256), SHA2('empty-route-dcc', 256)),
         SUM(CASE WHEN current_binding_id IS NULL
                        OR current_dcc_project_code_id <> dcc_project_code_id
                  THEN 1 ELSE 0 END),
         NULL
    FROM c00_backfill_route_state;

  INSERT INTO c00_backfill_plan_report
  SELECT 'active_order_snapshot_update',
         COALESCE(MIN(candidate.input_manifest_sha256), SHA2('empty-active-order', 256)),
         COUNT(1),
         NULL
    FROM c00_backfill_active_order_candidate candidate
    JOIN mes_pro_process_pool_active_order active_order
      ON active_order.tenant_id = candidate.tenant_id
     AND active_order.id = candidate.active_order_id
   WHERE NOT (active_order.dcc_project_code_id <=> candidate.dcc_project_code_id)
      OR NOT (active_order.qa_regulation_id <=> candidate.qa_regulation_id)
      OR NOT (active_order.qa_regulation_version_id <=> candidate.qa_regulation_version_id);

  INSERT INTO c00_backfill_plan_report
  SELECT 'task_rule_key_update', SHA2('FIRST-PATROL_AM-PATROL_PM-FINAL-v1', 256),
         COUNT(1), NULL
    FROM mes_pqc_inspection_task task
    JOIN c00_backfill_task_rule_candidate candidate
      ON candidate.tenant_id = task.tenant_id
     AND candidate.task_id = task.id
   WHERE task.inspection_rule_key IS NULL
     AND candidate.expected_rule_key IS NOT NULL;

  INSERT INTO c00_backfill_plan_report
  SELECT 'CanonicalPqcSubmissionV1_hash_update',
         COALESCE(MIN(manifest.input_manifest_sha256), SHA2('empty-task-submission', 256)),
         COUNT(1), NULL
    FROM mes_pqc_inspection_task task
    JOIN c00_backfill_approved_task_submission manifest
      ON manifest.tenant_id = task.tenant_id
     AND manifest.task_id = task.id
   WHERE task.task_status IN ('SUBMITTED', 'CONFIRMED')
     AND (NOT (task.submitted_content_hash <=> manifest.submitted_content_hash)
          OR NOT (task.submitted_event_id <=> manifest.submitted_event_id));

  SELECT * FROM c00_backfill_blocker_report ORDER BY blocker_scope, source_id;
  SELECT * FROM c00_backfill_plan_report ORDER BY plan_step;

  SELECT COUNT(1) INTO v_blocker_count FROM c00_backfill_blocker_report;

  IF @c00_apply_backfill = 0 THEN
    SELECT 'C00 backfill dry-run complete; apply requires blocker_count=0 and the same approved manifest hashes'
      AS c00_backfill_status,
      v_blocker_count AS blocker_count;
  ELSE
    IF v_blocker_count <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'C00 backfill blockers remain; no business DML was executed';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS c00_backfill_route_update_candidate;
    CREATE TEMPORARY TABLE c00_backfill_route_update_candidate AS
    SELECT *
      FROM c00_backfill_route_state
     WHERE current_binding_id IS NOT NULL
       AND current_dcc_project_code_id <> dcc_project_code_id;

    DROP TEMPORARY TABLE IF EXISTS c00_backfill_route_insert_candidate;
    CREATE TEMPORARY TABLE c00_backfill_route_insert_candidate AS
    SELECT *
      FROM c00_backfill_route_state
     WHERE current_binding_id IS NULL;

    DROP TEMPORARY TABLE IF EXISTS c00_backfill_route_before_image;
    CREATE TEMPORARY TABLE c00_backfill_route_before_image (
      restore_action varchar(32) NOT NULL,
      binding_id bigint DEFAULT NULL,
      tenant_id bigint NOT NULL,
      route_id bigint NOT NULL,
      old_dcc_project_code_id bigint DEFAULT NULL,
      old_version bigint DEFAULT NULL,
      old_deleted bit(1) DEFAULT NULL,
      applied_dcc_project_code_id bigint NOT NULL,
      applied_version bigint NOT NULL,
      applied_deleted bit(1) NOT NULL,
      input_manifest_sha256 char(64) NOT NULL
    ) ENGINE=InnoDB;

    INSERT INTO c00_backfill_route_before_image
      (restore_action, binding_id, tenant_id, route_id, old_dcc_project_code_id,
       old_version, old_deleted, applied_dcc_project_code_id, applied_version,
       applied_deleted, input_manifest_sha256)
    SELECT 'RESTORE_UPDATE', binding.id, binding.tenant_id, binding.route_id,
           binding.dcc_project_code_id, binding.version, binding.deleted,
           candidate.dcc_project_code_id, candidate.expected_previous_version + 1,
           b'0', candidate.input_manifest_sha256
      FROM c00_backfill_route_update_candidate candidate
      JOIN mes_pro_route_dcc_project_binding binding
        ON binding.id = candidate.current_binding_id;

    DROP TEMPORARY TABLE IF EXISTS c00_backfill_active_order_before_image;
    CREATE TEMPORARY TABLE c00_backfill_active_order_before_image AS
    SELECT active_order.id AS active_order_id,
           active_order.tenant_id,
           active_order.dcc_project_code_id AS old_dcc_project_code_id,
           active_order.qa_regulation_id AS old_qa_regulation_id,
           active_order.qa_regulation_version_id AS old_qa_regulation_version_id,
           candidate.dcc_project_code_id AS applied_dcc_project_code_id,
           candidate.qa_regulation_id AS applied_qa_regulation_id,
           candidate.qa_regulation_version_id AS applied_qa_regulation_version_id,
           candidate.input_manifest_sha256
      FROM c00_backfill_active_order_candidate candidate
      JOIN mes_pro_process_pool_active_order active_order
        ON active_order.tenant_id = candidate.tenant_id
       AND active_order.id = candidate.active_order_id
     WHERE NOT (active_order.dcc_project_code_id <=> candidate.dcc_project_code_id)
        OR NOT (active_order.qa_regulation_id <=> candidate.qa_regulation_id)
        OR NOT (active_order.qa_regulation_version_id <=> candidate.qa_regulation_version_id);

    DROP TEMPORARY TABLE IF EXISTS c00_backfill_task_before_image;
    CREATE TEMPORARY TABLE c00_backfill_task_before_image AS
    SELECT task.id AS task_id,
           task.tenant_id,
           task.inspection_rule_key AS old_inspection_rule_key,
           task.submitted_content_hash AS old_submitted_content_hash,
           task.submitted_event_id AS old_submitted_event_id,
           COALESCE(rule_candidate.expected_rule_key, task.inspection_rule_key)
             AS applied_inspection_rule_key,
           COALESCE(submission.submitted_content_hash, task.submitted_content_hash)
             AS applied_submitted_content_hash,
           COALESCE(submission.submitted_event_id, task.submitted_event_id)
             AS applied_submitted_event_id,
           COALESCE(submission.input_manifest_sha256,
                    SHA2('FIRST-PATROL_AM-PATROL_PM-FINAL-v1', 256))
             AS input_manifest_sha256
      FROM mes_pqc_inspection_task task
      LEFT JOIN c00_backfill_task_rule_candidate rule_candidate
        ON rule_candidate.tenant_id = task.tenant_id
       AND rule_candidate.task_id = task.id
      LEFT JOIN c00_backfill_approved_task_submission submission
        ON submission.tenant_id = task.tenant_id
       AND submission.task_id = task.id
     WHERE (task.inspection_rule_key IS NULL
            AND rule_candidate.expected_rule_key IS NOT NULL)
        OR (task.task_status IN ('SUBMITTED', 'CONFIRMED')
            AND submission.task_id IS NOT NULL
            AND (NOT (task.submitted_content_hash <=> submission.submitted_content_hash)
                 OR NOT (task.submitted_event_id <=> submission.submitted_event_id)));

    DROP TEMPORARY TABLE IF EXISTS c00_backfill_apply_report;
    CREATE TEMPORARY TABLE c00_backfill_apply_report (
      apply_step varchar(64) NOT NULL,
      input_manifest_sha256 char(64) NOT NULL,
      expected_row_count bigint NOT NULL,
      affected_row_count bigint NOT NULL,
      blocker_reason varchar(512) DEFAULT NULL
    ) ENGINE=InnoDB;

    START TRANSACTION;

    SELECT affected_row_count
      INTO v_expected_count
      FROM c00_backfill_plan_report
     WHERE plan_step = 'route_dcc_binding_upsert';

    UPDATE `mes_pro_route_dcc_project_binding` binding
    JOIN c00_backfill_route_update_candidate candidate
      ON candidate.current_binding_id = binding.id
     SET binding.dcc_project_code_id = candidate.dcc_project_code_id,
         binding.version = candidate.expected_previous_version + 1,
         binding.updater = 'C00_BACKFILL'
   WHERE binding.deleted = b'0'
     AND binding.version = candidate.expected_previous_version;
    SET v_route_update_count = ROW_COUNT();

    INSERT INTO `mes_pro_route_dcc_project_binding`
      (`route_id`, `dcc_project_code_id`, `version`, `creator`, `updater`, `deleted`, `tenant_id`)
    SELECT candidate.route_id,
           candidate.dcc_project_code_id,
           candidate.expected_previous_version + 1,
           'C00_BACKFILL',
           'C00_BACKFILL',
           b'0',
           candidate.tenant_id
      FROM c00_backfill_route_insert_candidate candidate;
    SET v_route_insert_count = ROW_COUNT();

    INSERT INTO c00_backfill_route_before_image
      (restore_action, binding_id, tenant_id, route_id, old_dcc_project_code_id,
       old_version, old_deleted, applied_dcc_project_code_id, applied_version,
       applied_deleted, input_manifest_sha256)
    SELECT 'DELETE_INSERTED', binding.id, binding.tenant_id, binding.route_id,
           NULL, NULL, NULL, binding.dcc_project_code_id, binding.version,
           binding.deleted, candidate.input_manifest_sha256
      FROM c00_backfill_route_insert_candidate candidate
      JOIN mes_pro_route_dcc_project_binding binding
        ON binding.tenant_id = candidate.tenant_id
       AND binding.route_id = candidate.route_id
       AND binding.dcc_project_code_id = candidate.dcc_project_code_id
       AND binding.version = candidate.expected_previous_version + 1
       AND binding.deleted = b'0';

    SET v_actual_count = v_route_update_count + v_route_insert_count;
    INSERT INTO c00_backfill_apply_report
    SELECT 'route_dcc_binding_upsert',
           COALESCE(MIN(input_manifest_sha256), SHA2('empty-route-dcc', 256)),
           v_expected_count, v_actual_count, NULL
      FROM c00_backfill_route_state;
    IF v_actual_count <> v_expected_count THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'C00 route-DCC affected row count mismatch';
    END IF;

    SELECT affected_row_count
      INTO v_expected_count
      FROM c00_backfill_plan_report
     WHERE plan_step = 'active_order_snapshot_update';

    UPDATE `mes_pro_process_pool_active_order` active_order
    JOIN c00_backfill_active_order_candidate candidate
      ON candidate.tenant_id = active_order.tenant_id
     AND candidate.active_order_id = active_order.id
     SET active_order.dcc_project_code_id = candidate.dcc_project_code_id,
         active_order.qa_regulation_id = candidate.qa_regulation_id,
         active_order.qa_regulation_version_id = candidate.qa_regulation_version_id,
         active_order.updater = 'C00_BACKFILL'
   WHERE NOT (active_order.dcc_project_code_id <=> candidate.dcc_project_code_id)
      OR NOT (active_order.qa_regulation_id <=> candidate.qa_regulation_id)
      OR NOT (active_order.qa_regulation_version_id <=> candidate.qa_regulation_version_id);
    SET v_active_order_update_count = ROW_COUNT();

    INSERT INTO c00_backfill_apply_report
    SELECT 'active_order_snapshot_update',
           COALESCE(MIN(input_manifest_sha256), SHA2('empty-active-order', 256)),
           v_expected_count, v_active_order_update_count, NULL
      FROM c00_backfill_active_order_candidate;
    IF v_active_order_update_count <> v_expected_count THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'C00 active-order affected row count mismatch';
    END IF;

    SELECT affected_row_count
      INTO v_expected_count
      FROM c00_backfill_plan_report
     WHERE plan_step = 'task_rule_key_update';

    UPDATE `mes_pqc_inspection_task` task
    JOIN c00_backfill_task_rule_candidate candidate
      ON candidate.tenant_id = task.tenant_id
     AND candidate.task_id = task.id
     SET task.inspection_rule_key = candidate.expected_rule_key,
         task.updater = 'C00_BACKFILL'
   WHERE task.inspection_rule_key IS NULL
     AND candidate.expected_rule_key IS NOT NULL;
    SET v_task_rule_update_count = ROW_COUNT();

    INSERT INTO c00_backfill_apply_report
    VALUES ('task_rule_key_update', SHA2('FIRST-PATROL_AM-PATROL_PM-FINAL-v1', 256),
            v_expected_count, v_task_rule_update_count, NULL);
    IF v_task_rule_update_count <> v_expected_count THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'C00 task rule affected row count mismatch';
    END IF;

    SELECT affected_row_count
      INTO v_expected_count
      FROM c00_backfill_plan_report
     WHERE plan_step = 'CanonicalPqcSubmissionV1_hash_update';

    UPDATE `mes_pqc_inspection_task` task
    JOIN c00_backfill_approved_task_submission manifest
      ON manifest.tenant_id = task.tenant_id
     AND manifest.task_id = task.id
     SET task.submitted_content_hash = manifest.submitted_content_hash,
         task.submitted_event_id = manifest.submitted_event_id,
         task.updater = 'C00_BACKFILL'
   WHERE task.task_status IN ('SUBMITTED', 'CONFIRMED')
     AND (NOT (task.submitted_content_hash <=> manifest.submitted_content_hash)
          OR NOT (task.submitted_event_id <=> manifest.submitted_event_id));
    SET v_task_submission_update_count = ROW_COUNT();

    INSERT INTO c00_backfill_apply_report
    SELECT 'CanonicalPqcSubmissionV1_hash_update',
           COALESCE(MIN(input_manifest_sha256), SHA2('empty-task-submission', 256)),
           v_expected_count, v_task_submission_update_count, NULL
      FROM c00_backfill_approved_task_submission;
    IF v_task_submission_update_count <> v_expected_count THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'C00 task submission affected row count mismatch';
    END IF;

    COMMIT;

    SELECT * FROM c00_backfill_apply_report ORDER BY apply_step;
    SELECT * FROM c00_backfill_route_before_image ORDER BY tenant_id, route_id, binding_id;
    SELECT * FROM c00_backfill_active_order_before_image ORDER BY tenant_id, active_order_id;
    SELECT * FROM c00_backfill_task_before_image ORDER BY tenant_id, task_id;

    SELECT JSON_ARRAYAGG(JSON_OBJECT(
             'restoreAction', restore_action,
             'bindingId', binding_id,
             'tenantId', tenant_id,
             'routeId', route_id,
             'oldDccProjectCodeId', old_dcc_project_code_id,
             'oldVersion', old_version,
             'oldDeleted', old_deleted,
             'appliedDccProjectCodeId', applied_dcc_project_code_id,
             'appliedVersion', applied_version,
             'appliedDeleted', applied_deleted,
             'inputManifestSha256', input_manifest_sha256
           )) AS c00_route_before_image_json
      FROM c00_backfill_route_before_image;

    SELECT JSON_ARRAYAGG(JSON_OBJECT(
             'activeOrderId', active_order_id,
             'tenantId', tenant_id,
             'oldDccProjectCodeId', old_dcc_project_code_id,
             'oldQaRegulationId', old_qa_regulation_id,
             'oldQaRegulationVersionId', old_qa_regulation_version_id,
             'appliedDccProjectCodeId', applied_dcc_project_code_id,
             'appliedQaRegulationId', applied_qa_regulation_id,
             'appliedQaRegulationVersionId', applied_qa_regulation_version_id,
             'inputManifestSha256', input_manifest_sha256
           )) AS c00_active_order_before_image_json
      FROM c00_backfill_active_order_before_image;

    SELECT JSON_ARRAYAGG(JSON_OBJECT(
             'taskId', task_id,
             'tenantId', tenant_id,
             'oldInspectionRuleKey', old_inspection_rule_key,
             'oldSubmittedContentHash', old_submitted_content_hash,
             'oldSubmittedEventId', old_submitted_event_id,
             'appliedInspectionRuleKey', applied_inspection_rule_key,
             'appliedSubmittedContentHash', applied_submitted_content_hash,
             'appliedSubmittedEventId', applied_submitted_event_id,
             'inputManifestSha256', input_manifest_sha256
           )) AS c00_task_before_image_json
      FROM c00_backfill_task_before_image;

    SELECT 'C00 backfill apply complete; retain all before-image JSON before postflight'
      AS c00_backfill_status,
      0 AS blocker_count;
  END IF;

  SET SESSION group_concat_max_len = v_previous_group_concat_max_len;
END$$
DELIMITER ;

CALL migrate_mes_pqc_dcc_qa_c00_backfill();

DROP PROCEDURE IF EXISTS migrate_mes_pqc_dcc_qa_c00_backfill;
