-- release-target-preflight: migrationId=20260720_mes_edhr_approval_reviewer_runtime_entitlement; allowedEnvironments=test,prod,backup
-- Read-only identity/scope checks; this check never grants permissions or modifies accounts.
SELECT CASE WHEN
 (SELECT COUNT(*) FROM system_entitlement_policy WHERE policy_code='MES_EDHR_APPROVAL_REVIEWER_MINIMAL')<=1
 AND NOT EXISTS(SELECT 1 FROM system_entitlement_policy WHERE policy_code='MES_EDHR_APPROVAL_REVIEWER_MINIMAL' AND (deleted<>b'0' OR status<>0 OR allowed_permission_codes_json IS NULL OR NOT JSON_VALID(allowed_permission_codes_json) OR allowed_menu_refs_json IS NULL OR NOT JSON_VALID(allowed_menu_refs_json) OR forbidden_permission_codes_json IS NULL OR NOT JSON_VALID(forbidden_permission_codes_json)))
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution:approve' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution:track' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution:signature-query' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution-archive:query' AND status=0 AND deleted=b'0')
 THEN 'TARGET_PREFLIGHT_PASS:20260720_mes_edhr_approval_reviewer_runtime_entitlement' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260720_mes_edhr_approval_reviewer_runtime_entitlement:ROLE_PERMISSION_POLICY_OR_TENANT_IDENTITY_CONFLICT' END;
