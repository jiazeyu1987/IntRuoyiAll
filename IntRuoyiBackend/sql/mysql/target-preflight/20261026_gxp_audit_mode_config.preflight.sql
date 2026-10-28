-- release-target-preflight: migrationId=20261026_gxp_audit_mode_config; allowedEnvironments=test,prod,backup
SELECT CASE
  WHEN COUNT(*) <= 1 AND COALESCE(SUM(CASE
    WHEN value IS NULL OR BINARY value NOT IN ('FORMAL','LOCAL_POLICY','DEVELOPMENT_DISABLED')
      OR visible IS NULL OR visible <> b'1' OR type IS NULL OR type <> 1
    THEN 1 ELSE 0 END), 0) = 0
  THEN 'TARGET_PREFLIGHT_PASS:20261026_gxp_audit_mode_config'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_gxp_audit_mode_config'
END
FROM infra_config WHERE config_key='system.gxp.audit-mode' AND deleted=b'0';
