-- release-target-preflight: migrationId=20260901_dcc_registration_certificate_change_submit_role_permission; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_menu')) = 3
  AND (SELECT COUNT(*) FROM system_menu WHERE permission = 'dcc:registration-certificate:change:submit' AND deleted = b'0') <= 1
THEN 'TARGET_PREFLIGHT_PASS:20260901_dcc_registration_certificate_change_submit_role_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260901_dcc_registration_certificate_change_submit_role_permission' END;
