-- release-target-preflight: migrationId=20260829_mes_form_center_unified_import_menu; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=900365 AND deleted=b'0' AND visible=b'1' AND path='/mes/pro/batch-record-form-list' AND component='mes/pro/batchrecordformlist/index' AND component_name='MesProBatchRecordFormList' AND permission='mes:pro-batch-record-template:query')
 THEN 'TARGET_PREFLIGHT_PASS:20260829_mes_form_center_unified_import_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260829_mes_form_center_unified_import_menu:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
