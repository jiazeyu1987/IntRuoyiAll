-- release-target-preflight: migrationId=20260717_mes_invalid_process_reference_normalization; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_process','mes_pro_route_process','mes_dv_machinery_process','mes_md_workstation','mes_pro_task','mes_qc_ipqc')) = 6
THEN 'TARGET_PREFLIGHT_PASS:20260717_mes_invalid_process_reference_normalization' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260717_mes_invalid_process_reference_normalization' END;
