-- release-target-preflight: migrationId=20260907_dcc_publication_notification; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_notify_template','system_role','system_role_menu','system_tenant_package')) = 5
  AND (SELECT COUNT(*) FROM system_menu WHERE id = 6800 AND deleted = b'0') = 1
  AND (SELECT COUNT(*) FROM system_menu WHERE path = 'controlled-file/categories' AND deleted = b'0') = 1
  AND (SELECT COUNT(*) FROM system_menu WHERE permission = 'dcc:controlled-file:approve' AND deleted = b'0') = 1
  AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND menu_ids IS NOT NULL AND NOT JSON_VALID(menu_ids))
THEN 'TARGET_PREFLIGHT_PASS:20260907_dcc_publication_notification' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260907_dcc_publication_notification' END;
