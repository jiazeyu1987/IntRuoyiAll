-- release-target-preflight: migrationId=20261026_dcc_project_code_to_mdm_hide_edhr_config; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=990210 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('dcc/controlled-file/basic-data/project-code/index') AND BINARY path IN ('dcc-project-code','/mes/md/dcc-project-code')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=991501 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/edhr-config/index') AND BINARY path IN ('pro/edhr-config','/mes/pro/edhr-config')))
  AND (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=990200 AND parent_id=0 AND path='/mdm' AND type=1 AND status=0 AND deleted=b'0'))
THEN 'TARGET_PREFLIGHT_PASS:20261026_dcc_project_code_to_mdm_hide_edhr_config' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_dcc_project_code_to_mdm_hide_edhr_config:BASELINE_CONFLICT' END;
