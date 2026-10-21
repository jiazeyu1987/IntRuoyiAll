-- release-target-preflight: migrationId=20260720_mes_edhr_filler_tracking_signature_entitlement; allowedEnvironments=test,prod,backup
-- Read-only identity/scope checks; this check never grants permissions or modifies accounts.
SELECT CASE WHEN
 (SELECT COUNT(*) FROM system_entitlement_policy WHERE policy_code='MES_EDHR_FILLER_MINIMAL' AND status=0 AND deleted=b'0')=1
 AND NOT EXISTS(SELECT 1 FROM system_entitlement_policy WHERE policy_code='MES_EDHR_FILLER_MINIMAL' AND (allowed_permission_codes_json IS NULL OR NOT JSON_VALID(allowed_permission_codes_json) OR allowed_menu_refs_json IS NULL OR NOT JSON_VALID(allowed_menu_refs_json)))
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-edhr-batch-execution:query' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-edhr-batch-execution:update' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution:query' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution:update' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution:track' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-batch-record-execution:signature-query' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-edhr-work-task:query' AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_entitlement_claim c LEFT JOIN system_users u ON u.id=c.resolved_user_id AND u.tenant_id=c.tenant_id AND u.deleted=b'0' WHERE c.policy_code='MES_EDHR_FILLER_MINIMAL' AND c.status='ACTIVE' AND c.deleted=b'0' AND u.id IS NULL)
 THEN 'TARGET_PREFLIGHT_PASS:20260720_mes_edhr_filler_tracking_signature_entitlement' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260720_mes_edhr_filler_tracking_signature_entitlement:ROLE_PERMISSION_POLICY_OR_TENANT_IDENTITY_CONFLICT' END;
