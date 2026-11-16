-- release-target-preflight: migrationId=20261112_all_roles_approval_query; allowedEnvironments=test,backup,prod
-- Read-only baseline checks. Existing migration guards verify post-dependency state again at apply time.
WITH tmp_approval_query_role AS (SELECT r.id,r.tenant_id FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 WHERE r.deleted=0)
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=1200 AND path='/approval-center') OR (id=1207 AND parent_id=1200 AND path='todo')
        OR (id=1221 AND parent_id=1207 AND type=3 AND permission='bpm:task:query')
        OR (id=1201 AND parent_id=1200 AND path='my-initiated' AND component='approval-center/index')
        OR (id=1208 AND parent_id=1200 AND path='done' AND component='approval-center/index')
        OR (id=2713 AND parent_id=1200 AND path='cc' AND component='approval-center/index'
          AND permission='bpm:process-instance-cc:query')))<>6) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_all_roles_approval_query:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_tenant t JOIN system_role r ON r.tenant_id=t.id AND r.deleted=0
      LEFT JOIN system_approval_authority_revision ar ON ar.tenant_id=t.id
      WHERE t.deleted=0 AND ar.tenant_id IS NULL)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_all_roles_approval_query:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM system_tenant t JOIN tmp_approval_query_role r ON r.tenant_id=t.id
      LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 AND p.status=0
      WHERE t.package_id<>0 AND t.package_id<>-1
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids)))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_all_roles_approval_query:CONDITION_3'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_all_roles_approval_query' END;
