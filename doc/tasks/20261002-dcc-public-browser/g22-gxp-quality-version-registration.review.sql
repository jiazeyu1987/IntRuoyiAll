-- REVIEW ONLY: separate maximum1 tenant1 quality-version registration; not a25-operation config or production CSV approval.
-- Actual quality approver/time/reference variables intentionally unset; never default to admin/account1/old approval.
-- Prepared candidate and source mapping hashes are fixed; no policy file modification.
DELIMITER $$
CREATE PROCEDURE `dcc_gxp1_tenant1_quality`(IN quality_reference text, IN quality_approver bigint, IN quality_time datetime)
BEGIN
  DECLARE row_count int DEFAULT 0;
  DECLARE lock_owned int DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF lock_owned=1 THEN DO RELEASE_LOCK('dcc:gxp25:tenant1'); END IF;
    RESIGNAL;
  END;
  IF DATABASE() IS NULL OR DATABASE()<>'ruoyi-vue-pro' OR @dcc_gxp_quality_version_write_authorized IS NULL OR @dcc_gxp_quality_version_write_authorized<>1
    OR @dcc_gxp_actual_quality_role_confirmed IS NULL OR @dcc_gxp_actual_quality_role_confirmed<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='separate local-test write and actual quality-role identity confirmed required';
  END IF;
  IF quality_reference IS NULL OR TRIM(quality_reference)='' OR CHAR_LENGTH(quality_reference)>256 OR LOWER(quality_reference) LIKE '%pending%'
    OR LOWER(quality_reference) LIKE '%placeholder%' OR BINARY quality_reference=BINARY 'CODEX-IMPLEMENTATION-20260908'
    OR quality_approver IS NULL OR quality_approver<1 OR quality_time IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual new quality approval person/time/reference required';
  END IF;
  SELECT GET_LOCK('dcc:gxp25:tenant1',10) INTO lock_owned;
  IF lock_owned IS NULL OR lock_owned<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='policy registration lock unavailable'; END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='gxp_audit_policy_version' AND ENGINE='InnoDB')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='transactional quality table engine required';
  END IF;
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  START TRANSACTION;
  -- Existence/enablement is required but is not a substituted QA-role approval proof.
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_users'
    AND ((COLUMN_NAME IN ('id','tenant_id') AND DATA_TYPE='bigint') OR (COLUMN_NAME='status' AND DATA_TYPE='tinyint') OR (COLUMN_NAME='deleted' AND COLUMN_TYPE='bit(1)')))<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual system_users identity schema must be readonly verified';
  END IF;
  IF (SELECT COUNT(*) FROM `system_users` WHERE id=quality_approver AND tenant_id=1 AND status=0 AND deleted=b'0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='quality approver must be an actual enabled tenant1 account';
  END IF;
  SELECT id FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version='2026-10-dcc-integration-01' FOR UPDATE;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version='2026-10-dcc-integration-01')>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='duplicate quality version conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version='2026-10-dcc-integration-01'
    AND NOT(BINARY policy_version <=> BINARY '2026-10-dcc-integration-01' AND BINARY policy_hash <=> BINARY '776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df'
      AND approved_by <=> quality_approver AND approved_at <=> quality_time
      AND BINARY approval_reference <=> BINARY quality_reference AND BINARY coverage_report_hash <=> BINARY '319fc04a677dcb8670b420c9791bf5e4584d6708e0e765e58dbb637d9ab6b558')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing approved quality version conflict, no overwrite';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version='2026-10-dcc-integration-01') THEN
    INSERT INTO `gxp_audit_policy_version` (`tenant_id`,`policy_version`,`policy_hash`,`approved_by`,`approved_at`,`approval_reference`,`coverage_report_hash`)
    VALUES(1,'2026-10-dcc-integration-01','776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df',quality_approver,quality_time,quality_reference,'319fc04a677dcb8670b420c9791bf5e4584d6708e0e765e58dbb637d9ab6b558');
    SET row_count=ROW_COUNT();
    IF row_count<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='quality version insert count must be1'; END IF;
  END IF;
  COMMIT;
  DO RELEASE_LOCK('dcc:gxp25:tenant1');
  SELECT row_count AS inserted_quality_version,0 AS updated_quality_version,0 AS deleted_quality_version;
END$$
DELIMITER ;
CALL `dcc_gxp1_tenant1_quality`(@dcc_gxp_quality_approval_reference,@dcc_gxp_quality_approved_by,@dcc_gxp_quality_approved_at);
DROP PROCEDURE `dcc_gxp1_tenant1_quality`;
