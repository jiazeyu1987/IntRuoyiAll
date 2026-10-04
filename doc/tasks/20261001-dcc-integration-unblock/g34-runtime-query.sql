SELECT JSON_OBJECT('kind','identity','database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',VERSION(),'capturedAtUtc',DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-%dT%H:%i:%sZ'));
SELECT JSON_OBJECT('kind','sidecarTables','count',COUNT(*)) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation');
SELECT JSON_OBJECT('kind','sidecarLedger','count',COUNT(*)) FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy' AND deleted=b'0';
SELECT JSON_OBJECT('kind','newQualityPolicyVersion','count',COUNT(*)) FROM gxp_audit_policy_version WHERE tenant_id=1 AND policy_version='2026-10-dcc-integration-01';
SELECT JSON_OBJECT('kind','newActiveAuditOperations','count',COUNT(*)) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND policy_version='2026-10-dcc-integration-01' AND active=b'1' AND deleted=b'0';
SELECT JSON_OBJECT('kind','legacyClaimsMissingOriginalName','count',COUNT(*)) FROM dcc_controlled_file_name_claim WHERE tenant_id=1 AND deleted=b'0' AND source_original_file_name IS NULL;
