-- release-target-preflight: migrationId=20260829_mes_old_form_template_binding_switch; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('bpm_form_template_version','jimu_report','jimu_report_category','mes_pro_batch_record_definition','mes_pro_batch_record_report','mes_pro_batch_record_version','mes_pro_route_flow_process_batch_record','mes_pro_route_version')) = 8
THEN 'TARGET_PREFLIGHT_PASS:20260829_mes_old_form_template_binding_switch' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260829_mes_old_form_template_binding_switch' END;
