-- release-target-preflight: migrationId=20261112_operation_log_admin_auditor_only; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=500 AND component='system/operatelog/index')
        OR (id=1040 AND permission='system:operate-log:query')
        OR (id=1042 AND permission='system:operate-log:export')))<>3) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_operation_log_admin_auditor_only:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
    WHERE r.deleted=0 AND a.tenant_id IS NULL)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_operation_log_admin_auditor_only:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_operation_log_admin_auditor_only' END;
