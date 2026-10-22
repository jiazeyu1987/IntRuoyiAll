-- release-target-preflight: migrationId=20260624_dcc_view_matrix_independent_seed; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_file_category','dcc_category_view_matrix_rule','system_dept','system_role','system_user_role','system_users')) = 6
THEN 'TARGET_PREFLIGHT_PASS:20260624_dcc_view_matrix_independent_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260624_dcc_view_matrix_independent_seed' END;
