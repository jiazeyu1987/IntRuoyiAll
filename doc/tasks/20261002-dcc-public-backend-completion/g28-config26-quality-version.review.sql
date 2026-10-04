-- REVIEW ONLY: separate maximum1 tenant1 quality-version registration; not a26-operation config or production CSV approval.
-- Actual quality approver/time/reference variables intentionally unset; never default to admin/account1/old approval.
-- Prepared candidate and source mapping hashes are fixed; no policy file modification.
DELIMITER $$
CREATE PROCEDURE `dcc_gxp26_tenant1_quality`(IN quality_reference text, IN quality_approver bigint, IN quality_time datetime)
BEGIN
  DECLARE row_count int DEFAULT 0;
  DECLARE lock_owned int DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF lock_owned=1 THEN DO RELEASE_LOCK('dcc:gxp26:tenant1'); END IF;
    RESIGNAL;
  END;
  IF DATABASE() IS NULL OR DATABASE()<>'ruoyi-vue-pro' OR @dcc_gxp26_quality_version_write_authorized IS NULL OR @dcc_gxp26_quality_version_write_authorized<>1
    OR @dcc_gxp26_actual_quality_role_confirmed IS NULL OR @dcc_gxp26_actual_quality_role_confirmed<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='separate local-test write and actual quality-role identity confirmed required';
  END IF;
  IF quality_reference IS NULL OR TRIM(quality_reference)='' OR CHAR_LENGTH(quality_reference)>256 OR LOWER(quality_reference) LIKE '%pending%'
    OR LOWER(quality_reference) LIKE '%placeholder%' OR BINARY quality_reference=BINARY 'CODEX-IMPLEMENTATION-20260908'
    OR quality_approver IS NULL OR quality_approver<1 OR quality_time IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual new quality approval person/time/reference required';
  END IF;
  IF @@server_uuid<>'92ca05d0-aec8-11f1-a944-02b4e226a5ef' OR FIND_IN_SET('STRICT_TRANS_TABLES',@@session.sql_mode)=0 AND FIND_IN_SET('STRICT_ALL_TABLES',@@session.sql_mode)=0
    OR @@character_set_connection<>'utf8mb4' OR @@collation_connection<>'utf8mb4_unicode_ci' OR @@session.time_zone<>'+08:00' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fresh strict UTF8 Shanghai exact-source session required';
  END IF;
  IF @dcc_gxp26_actual_quality_role_confirmed IS NULL OR @dcc_gxp26_actual_quality_role_confirmed<>1
    OR @dcc_gxp26_quality_signature_evidence IS NULL OR TRIM(@dcc_gxp26_quality_signature_evidence)=''
    OR CHAR_LENGTH(@dcc_gxp26_quality_signature_evidence)>2000
    OR LOWER(@dcc_gxp26_quality_signature_evidence) LIKE '%pending%' OR LOWER(@dcc_gxp26_quality_signature_evidence) LIKE '%placeholder%'
    OR @dcc_gxp26_quality_signature_evidence LIKE '%尚未%' OR @dcc_gxp26_quality_signature_evidence LIKE '%待确认%'
    OR @dcc_gxp26_quality_role_basis IS NULL OR TRIM(@dcc_gxp26_quality_role_basis)='' OR CHAR_LENGTH(@dcc_gxp26_quality_role_basis)>2000
    OR LOWER(@dcc_gxp26_quality_role_basis) LIKE '%pending%' OR LOWER(@dcc_gxp26_quality_role_basis) LIKE '%placeholder%'
    OR @dcc_gxp26_quality_role_basis LIKE '%尚未%' OR @dcc_gxp26_quality_role_basis LIKE '%待确认%'
    OR @dcc_gxp26_quality_role_id IS NULL OR @dcc_gxp26_quality_role_id<1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual signed quality-role facts required, not default account';
  END IF;
  SELECT GET_LOCK('dcc:gxp26:tenant1',10) INTO lock_owned;
  IF lock_owned IS NULL OR lock_owned<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='policy registration lock unavailable'; END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='gxp_audit_policy_version' AND ENGINE='InnoDB')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='transactional quality table engine required';
  END IF;
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  START TRANSACTION;
  SELECT u.id FROM system_users u JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=b'0'
    JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id AND r.deleted=b'0' AND r.status=0
    WHERE u.id=quality_approver AND u.tenant_id=1 AND u.status=0 AND u.deleted=b'0' AND r.id=@dcc_gxp26_quality_role_id FOR UPDATE;
  IF (SELECT COUNT(*) FROM system_users u JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=b'0'
      JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id AND r.deleted=b'0' AND r.status=0
      WHERE u.id=quality_approver AND u.tenant_id=1 AND u.status=0 AND u.deleted=b'0' AND r.id=@dcc_gxp26_quality_role_id)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual enabled tenant1 quality account and registered role required';
  END IF;

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
    AND NOT(BINARY policy_version <=> BINARY '2026-10-dcc-integration-01' AND BINARY policy_hash <=> BINARY '661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894'
      AND approved_by <=> quality_approver AND approved_at <=> quality_time
      AND BINARY approval_reference <=> BINARY quality_reference AND BINARY coverage_report_hash <=> BINARY '3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing approved quality version conflict, no overwrite';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND policy_version='2026-10-dcc-integration-01') THEN
    INSERT INTO `gxp_audit_policy_version` (`tenant_id`,`policy_version`,`policy_hash`,`approved_by`,`approved_at`,`approval_reference`,`coverage_report_hash`)
    VALUES(1,'2026-10-dcc-integration-01','661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894',quality_approver,quality_time,quality_reference,'3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d');
    SET row_count=ROW_COUNT();
    IF row_count<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='quality version insert count must be1'; END IF;
  END IF;
  COMMIT;
  DO RELEASE_LOCK('dcc:gxp26:tenant1');
  SELECT row_count AS inserted_quality_version,0 AS updated_quality_version,0 AS deleted_quality_version;
END$$
DELIMITER ;
CALL `dcc_gxp26_tenant1_quality`(@dcc_gxp26_quality_approval_reference,@dcc_gxp26_quality_approved_by,@dcc_gxp26_quality_approved_at);
DROP PROCEDURE `dcc_gxp26_tenant1_quality`;
