-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_route_version_snapshot_identity,20260718_controlled_content_lifecycle; supersedes=20260812_mes_route_version_snapshot_identity_enforce; type=schema; riskLevel=high; requiresTargetPreflight=true; applyOrder=10
-- Run the bound route-snapshot-identity-v2 PRECHECK/BACKFILL/READINESS command.
-- The command owns the single archival transaction and canonical validation.
-- This final gate accepts only full canonical identities or the five approved,
-- non-executable historical summaries. It never rewrites JSON or repairs hashes.
DROP PROCEDURE IF EXISTS enforce_mes_route_snapshot_legacy_summary;
DELIMITER $$
CREATE PROCEDURE enforce_mes_route_snapshot_legacy_summary()
BEGIN
  DECLARE legacy_count BIGINT DEFAULT 0;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME IN ('mes_pro_route_version','controlled_content_version_ref','controlled_content_transition_audit')
      AND ENGINE='InnoDB') <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Route identity requires transactional native/ref/audit tables';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_route_version' AND COLUMN_NAME IN ('route_snapshot_sha256','route_snapshot_format_version')) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Nullable route snapshot identity schema is required';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_pro_route_version WHERE route_snapshot_sha256 IS NULL
      OR route_snapshot_sha256 NOT REGEXP '^[0-9a-f]{64}$' OR route_snapshot_format_version IS NULL
      OR route_snapshot_format_version NOT IN ('MES_ROUTE_SNAPSHOT_CANONICAL_V1','MES_ROUTE_LEGACY_SUMMARY_V1')
      OR (lifecycle_status='LEGACY_INCOMPLETE') <> (route_snapshot_format_version='MES_ROUTE_LEGACY_SUMMARY_V1')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Route snapshot identity readiness is required';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_release_route_legacy_approved;
  CREATE TEMPORARY TABLE tmp_release_route_legacy_approved(id bigint PRIMARY KEY,tenant_id bigint,route_id bigint,raw_sha char(64))
    DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  INSERT INTO tmp_release_route_legacy_approved VALUES
    (1,1,900021,'c9f10288575db4957db1d5b4d95ae0299be3c93c105fba1eaea8c42e26ca0ef6'),
    (2,1,900022,'05dbc0887aacf0d815286c679693a68591fa1d05de009ad356dfaee01269574c'),
    (3,1,900025,'9c6436a3f8e89695f2d1bb6e0f4420c4dc31143fc2448d116c31e8dbac00af1c'),
    (4,1,900026,'fc1360f8b3221d8789165f43b22296b367b518242133d811a41f728f3a41f5e4'),
    (5,122,922022,'19a6df382079f1510b65cbcc3c9a24dc4df38fecf1abe729b0b65f2a664776d9');
  SELECT COUNT(*) INTO legacy_count FROM mes_pro_route_version WHERE route_snapshot_format_version='MES_ROUTE_LEGACY_SUMMARY_V1';
  IF legacy_count NOT IN (0,5) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Approved historical summary set must be complete';
  END IF;
  IF EXISTS (
    SELECT 1 FROM mes_pro_route_version v LEFT JOIN tmp_release_route_legacy_approved a
      ON a.id=v.id AND a.tenant_id=v.tenant_id AND a.route_id=v.route_id
    WHERE v.route_snapshot_format_version='MES_ROUTE_LEGACY_SUMMARY_V1'
      AND (a.id IS NULL OR v.lifecycle_status<>'LEGACY_INCOMPLETE' OR v.active<>b'0' OR v.deleted<>b'0'
        OR v.version_no<>'V1' OR NOT (v.remark <=> 'legacy route version backfill')
        OR v.create_time<>'2026-06-20 18:03:42' OR v.update_time<>'2026-06-20 18:03:42'
        OR v.source_route_version_id IS NOT NULL OR COALESCE(v.approval_process_instance_id,'')<>''
        OR NOT (v.route_snapshot_sha256 <=> a.raw_sha)
        OR NOT (LOWER(SHA2(CONVERT(v.route_snapshot_json USING utf8mb4),256)) <=> a.raw_sha)
        OR COALESCE(JSON_VALID(v.route_snapshot_json),0)<>1
        OR JSON_LENGTH(v.route_snapshot_json)<>6
        OR JSON_CONTAINS_PATH(v.route_snapshot_json,'all','$.routeId','$.routeCode','$.routeName','$.description','$.status','$.remark')<>1)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Historical summary provenance or terminal identity mismatch';
  END IF;
  IF EXISTS (
    SELECT 1 FROM mes_pro_route_version v
    WHERE v.route_snapshot_format_version='MES_ROUTE_LEGACY_SUMMARY_V1' AND (
      (SELECT COUNT(*) FROM controlled_content_version_ref r WHERE r.tenant_id=v.tenant_id
        AND r.content_type='MES_ROUTE' AND r.native_version_id=v.id)<>1
      OR (SELECT COUNT(*) FROM controlled_content_version_ref r WHERE r.tenant_id=v.tenant_id
        AND r.content_type='MES_ROUTE' AND r.content_key=CAST(v.route_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND r.native_master_id=v.route_id
        AND r.native_version_id=v.id AND r.version_no='V1' AND r.deleted=b'0'
        AND r.canonical_status='LEGACY_INCOMPLETE' AND r.domain_status='LEGACY_INCOMPLETE'
        AND r.active_unique_flag IS NULL AND r.open_candidate_unique_flag IS NULL
        AND r.source_version_ref_id IS NULL AND r.source_native_version_id IS NULL
        AND r.successor_version_ref_id IS NULL AND r.successor_native_version_id IS NULL
        AND COALESCE(r.approval_process_instance_id,'')='' AND r.last_transition_time IS NOT NULL
        AND r.create_time='2026-06-20 18:03:42' AND r.update_time='2026-06-20 18:03:42')<>1
      OR (SELECT COUNT(*) FROM controlled_content_transition_audit t JOIN controlled_content_version_ref r ON r.id=t.version_ref_id
        WHERE r.tenant_id=v.tenant_id AND r.content_type='MES_ROUTE' AND r.native_version_id=v.id)<>1
      OR (SELECT COUNT(*) FROM controlled_content_transition_audit t JOIN controlled_content_version_ref r ON r.id=t.version_ref_id
        WHERE r.tenant_id=v.tenant_id AND r.content_type='MES_ROUTE' AND r.native_version_id=v.id
        AND t.tenant_id=v.tenant_id AND t.content_type='MES_ROUTE' AND t.content_key=CAST(v.route_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        AND t.from_status='ACTIVE' AND t.domain_from_status='ACTIVE'
        AND t.to_status='LEGACY_INCOMPLETE' AND t.domain_to_status='LEGACY_INCOMPLETE'
        AND t.action='ARCHIVE_LEGACY_SUMMARY' AND t.event_key='20261023_mes_route_snapshot_legacy_summary_enforce'
        AND JSON_VALID(t.reason)=1 AND JSON_UNQUOTE(JSON_EXTRACT(t.reason,'$.schemaVersion'))='mes-route-legacy-summary/v1'
        AND JSON_UNQUOTE(JSON_EXTRACT(t.reason,'$.rawSha256'))=v.route_snapshot_sha256)<>1
    )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Historical summary controlled reference or audit mismatch';
  END IF;
  DROP TEMPORARY TABLE tmp_release_route_legacy_approved;
  ALTER TABLE mes_pro_route_version
    MODIFY COLUMN route_snapshot_sha256 varchar(64) NOT NULL COMMENT 'SHA-256 under explicit snapshot format',
    MODIFY COLUMN route_snapshot_format_version varchar(32) NOT NULL COMMENT 'Canonical executable or approved legacy summary format';
END$$
DELIMITER ;
CALL enforce_mes_route_snapshot_legacy_summary();
DROP PROCEDURE IF EXISTS enforce_mes_route_snapshot_legacy_summary;
