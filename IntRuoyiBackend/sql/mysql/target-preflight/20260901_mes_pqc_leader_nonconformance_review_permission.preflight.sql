-- release-target-preflight: migrationId=20260901_mes_pqc_leader_nonconformance_review_permission; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_menu')) = 3
  AND (SELECT COUNT(*) FROM system_menu WHERE permission IN ('mes:pro-edhr-nonconformance-review:create','mes:pro-edhr-nonconformance-review:dispose') AND deleted = b'0') >= 2
THEN 'TARGET_PREFLIGHT_PASS:20260901_mes_pqc_leader_nonconformance_review_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260901_mes_pqc_leader_nonconformance_review_permission' END;
