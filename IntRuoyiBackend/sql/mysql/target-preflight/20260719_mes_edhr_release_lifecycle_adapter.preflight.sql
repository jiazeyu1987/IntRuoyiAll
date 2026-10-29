-- release-target-preflight: migrationId=20260719_mes_edhr_release_lifecycle_adapter; allowedEnvironments=test,backup,prod
-- NOT EXISTS ignores deleted entitlement rows; reject those collisions and any broader/conflicting minimal approver permission policy instead of silently retaining it.
SELECT CASE WHEN

  (SELECT COUNT(*) FROM system_entitlement_policy WHERE policy_code='MES_EDHR_RELEASE_APPROVER_MINIMAL')<=1
  AND NOT EXISTS (SELECT 1 FROM system_entitlement_policy WHERE policy_code='MES_EDHR_RELEASE_APPROVER_MINIMAL'
    AND (deleted<>b'0' OR status<>0 OR module_code<>'mes'
      OR COALESCE(JSON_TYPE(allowed_permission_codes_json),'')<>'ARRAY'
      OR COALESCE(JSON_LENGTH(allowed_permission_codes_json),-1)<>8
      OR COALESCE(JSON_CONTAINS(allowed_permission_codes_json,JSON_ARRAY('bpm:task:query','bpm:task:update','mes:pro-edhr-work-task:query','mes:pro-edhr-batch-execution:query','mes:pro-edhr-release:query','mes:pro-edhr-release:approve','mes:pro-edhr-release:reject','mes:pro-edhr-release:event-query')),0)<>1
      OR COALESCE(JSON_TYPE(forbidden_permission_codes_json),'')<>'ARRAY'
      OR COALESCE(JSON_CONTAINS(forbidden_permission_codes_json,JSON_ARRAY('mes:pro-edhr-batch-execution:create','mes:pro-edhr-batch-execution:close','mes:pro-edhr-batch-execution-archive:create','mes:pro-edhr-batch-execution:quality-reject','mes:pro-edhr-release:precheck','mes:pro-edhr-release:submit','mes:pro-edhr-release:withdraw','system:permission:assign-role-menu','system:permission:assign-user-role')),0)<>1
      OR COALESCE(JSON_OVERLAPS(allowed_permission_codes_json,forbidden_permission_codes_json),1)<>0))
  AND (SELECT COUNT(*) FROM system_notify_template WHERE code='MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED' AND deleted=b'0')<=1
  AND NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED' AND deleted=b'0'
    AND CASE WHEN JSON_VALID(params) THEN JSON_TYPE(CAST(params AS JSON))<>'ARRAY' ELSE 1 END)
THEN 'TARGET_PREFLIGHT_PASS:20260719_mes_edhr_release_lifecycle_adapter' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260719_mes_edhr_release_lifecycle_adapter' END;
