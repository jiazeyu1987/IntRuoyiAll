-- release-target-preflight: migrationId=20261104_dcc_registration_approver_task_update; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE permission='bpm:task:update' AND status=0 AND deleted=0)<>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261104_dcc_registration_approver_task_update:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_tenant_package p JOIN system_tenant t ON t.package_id=p.id
      JOIN system_role r ON r.tenant_id=t.id AND r.code='dcc_registration_certificate_approver' AND r.deleted=0 AND r.status=0
      WHERE p.deleted=0 AND t.deleted=0 AND t.status=0 AND (p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids)))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261104_dcc_registration_approver_task_update:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261104_dcc_registration_approver_task_update' END;
