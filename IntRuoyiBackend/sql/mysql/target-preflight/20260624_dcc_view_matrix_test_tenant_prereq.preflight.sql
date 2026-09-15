-- release-target-preflight: migrationId=20260624_dcc_view_matrix_test_tenant_prereq; allowedEnvironments=test
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_users','system_dept')) = 2
  AND (SELECT COUNT(*) FROM system_users WHERE tenant_id = 122 AND username = 'aoteman' AND deleted = b'0' AND status = 0) = 1
  AND (SELECT COUNT(*) FROM system_dept WHERE tenant_id = 122 AND name = '顶级部门' AND parent_id = 0 AND deleted = b'0' AND status = 0) = 1
THEN 'TARGET_PREFLIGHT_PASS:20260624_dcc_view_matrix_test_tenant_prereq' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260624_dcc_view_matrix_test_tenant_prereq' END;
