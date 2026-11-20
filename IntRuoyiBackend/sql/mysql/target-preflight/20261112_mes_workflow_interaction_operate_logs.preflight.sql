-- release-target-preflight: migrationId=20261112_mes_workflow_interaction_operate_logs; allowedEnvironments=test,backup,prod
-- Check only historical rows that this migration will copy; preserve source logs and reject malformed metadata or action truncation.
SELECT CASE
 WHEN (EXISTS (SELECT 1 FROM mes_pro_edhr_operation_audit_event e
WHERE e.object_type='WORKFLOW_INTERACTION' AND e.result_status IN ('CANCELED','BLOCKED','FAILED') AND e.deleted=0
AND NOT EXISTS (SELECT 1 FROM system_operate_log l WHERE l.tenant_id=e.tenant_id AND l.type='生产业务交互' AND l.biz_id=e.id)
AND (e.metadata_json IS NULL OR NOT JSON_VALID(e.metadata_json)
 OR CHAR_LENGTH(e.action_name)>(SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_operate_log' AND COLUMN_NAME='action')))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_workflow_interaction_operate_logs:CONDITION_1'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_mes_workflow_interaction_operate_logs' END;
