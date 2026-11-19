-- release-target-preflight: migrationId=20261118_mes_workshop_export_permission; allowedEnvironments=test,backup,prod
SELECT CASE
 WHEN NOT EXISTS(SELECT 1 FROM system_menu WHERE id=5160 AND component='mes/md/workstation/workshop/index' AND status=0 AND deleted=b'0') OR NOT EXISTS(SELECT 1 FROM system_menu WHERE id=5325 AND permission='mes:dv-machinery:export' AND status=0 AND deleted=b'0') THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_mes_workshop_export_permission:MENU_BASELINE_CONFLICT'
 WHEN (SELECT COUNT(*) FROM system_menu WHERE permission='mes:md-workshop:export')>1 OR EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:md-workshop:export' AND (parent_id<>5160 OR type<>3 OR status<>0 OR deleted<>b'0')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_mes_workshop_export_permission:PERMISSION_CONFLICT'
 WHEN EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND (menu_ids IS NULL OR NOT JSON_VALID(menu_ids))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_mes_workshop_export_permission:PACKAGE_JSON_INVALID'
 ELSE 'TARGET_PREFLIGHT_PASS:20261118_mes_workshop_export_permission' END;
