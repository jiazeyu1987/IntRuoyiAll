-- release-target-preflight: migrationId=20260829_registration_certificate_management_menu_hierarchy; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role_menu','system_tenant_package')) = 3
  AND (SELECT COUNT(*) FROM system_menu WHERE id = 990200 AND deleted = b'0') = 1
  AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND menu_ids IS NOT NULL AND NOT JSON_VALID(menu_ids))
THEN 'TARGET_PREFLIGHT_PASS:20260829_registration_certificate_management_menu_hierarchy' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260829_registration_certificate_management_menu_hierarchy' END;
