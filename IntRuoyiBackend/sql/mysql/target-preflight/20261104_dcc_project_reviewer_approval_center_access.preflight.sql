-- release-target-preflight: migrationId=20261104_dcc_project_reviewer_approval_center_access; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN (NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='bpm:task:query' AND status=0 AND deleted=0)
     OR (SELECT COUNT(*) FROM system_menu WHERE component='approval-center/index'
       AND path IN ('todo','done') AND status=0 AND deleted=0)<>2) THEN 'TARGET_PREFLIGHT_BLOCKED:20261104_dcc_project_reviewer_approval_center_access:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_tenant_package p JOIN system_tenant t ON t.package_id=p.id
      JOIN system_role r ON r.tenant_id=t.id AND r.code='dcc_project_reviewer' AND r.deleted=0 AND r.status=0
      WHERE p.deleted=0 AND t.deleted=0 AND t.status=0 AND NOT JSON_VALID(p.menu_ids))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261104_dcc_project_reviewer_approval_center_access:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261104_dcc_project_reviewer_approval_center_access' END;
