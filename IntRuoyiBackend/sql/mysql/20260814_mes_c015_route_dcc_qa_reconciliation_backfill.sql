-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_preflight; type=backfill; riskLevel=high
-- C015 has no inference backfill. Non-empty repair work must be supplied as an independently approved ID manifest.

DROP PROCEDURE IF EXISTS backfill_mes_c015_route_dcc_qa_reconciliation;
DELIMITER $$
CREATE PROCEDURE backfill_mes_c015_route_dcc_qa_reconciliation()
BEGIN
  DECLARE v_required_repair_count bigint DEFAULT 0;

  DROP TEMPORARY TABLE IF EXISTS c015_reconciliation_approved_manifest;
  CREATE TEMPORARY TABLE c015_reconciliation_approved_manifest (
    input_manifest_sha256 char(64) NOT NULL,
    approved bit(1) NOT NULL,
    affected_row_count bigint NOT NULL,
    entity_type varchar(64) NOT NULL,
    source_id bigint NOT NULL,
    target_id bigint NOT NULL,
    PRIMARY KEY (entity_type, source_id)
  );

  SELECT COUNT(1)
    INTO v_required_repair_count
    FROM mes_qa_inspection_regulation
   WHERE dcc_project_code_id IS NULL;

  IF v_required_repair_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 backfill refused: approved ID manifest was not loaded; no name/code/first-row inference is allowed';
  END IF;

  SELECT SHA2('', 256) AS input_manifest_sha256,
         b'0' AS approved,
         0 AS affected_row_count,
         'no backfill required after zero-blocker preflight' AS result;
END$$
DELIMITER ;

CALL backfill_mes_c015_route_dcc_qa_reconciliation();
DROP PROCEDURE IF EXISTS backfill_mes_c015_route_dcc_qa_reconciliation;
