-- release-target-preflight: migrationId=20261112_unified_operation_log_navigation; allowedEnvironments=test,backup,prod
-- Read-only baseline checks. Existing migration guards verify post-dependency state again at apply time.
WITH RECURSIVE nav AS (
 SELECT id,parent_id,permission,status,deleted,1 depth FROM system_menu
 WHERE component='system/operatelog/index' AND deleted=0 AND status=0
 UNION ALL
 SELECT p.id,p.parent_id,p.permission,p.status,p.deleted,n.depth+1
 FROM nav n JOIN system_menu p ON p.id=n.parent_id WHERE n.parent_id<>0 AND n.depth<21
)
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE component='system/operatelog/index' AND deleted=0 AND status=0)<>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_unified_operation_log_navigation:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM nav WHERE depth>20 OR deleted<>0 OR status<>0 OR COALESCE(permission,'')<>'')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_unified_operation_log_navigation:CONDITION_2'
 WHEN (NOT EXISTS (SELECT 1 FROM nav WHERE parent_id=0)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_unified_operation_log_navigation:CONDITION_3'
 WHEN (EXISTS (SELECT 1 FROM nav n WHERE n.parent_id<>0 AND NOT EXISTS (SELECT 1 FROM system_menu p WHERE p.id=n.parent_id))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_unified_operation_log_navigation:CONDITION_4'
 WHEN (EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=0 AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN TRUE ELSE JSON_TYPE(menu_ids)<>'ARRAY' END)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_unified_operation_log_navigation:CONDITION_5'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_unified_operation_log_navigation' END;
