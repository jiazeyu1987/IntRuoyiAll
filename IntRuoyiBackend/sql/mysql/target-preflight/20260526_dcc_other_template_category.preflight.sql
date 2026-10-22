-- release-target-preflight: migrationId=20260526_dcc_other_template_category; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_file_category','dcc_category_approval_route','dcc_category_approval_route_node','dcc_file_category_distribution_rule','dcc_file_category_permission_rule','dcc_file_category_training_rule','system_tenant')) = 7
  AND (SELECT COUNT(*) FROM dcc_file_category WHERE name IN ('产品技术要求','其他') AND deleted = b'0') >= 2
THEN 'TARGET_PREFLIGHT_PASS:20260526_dcc_other_template_category' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260526_dcc_other_template_category' END;
