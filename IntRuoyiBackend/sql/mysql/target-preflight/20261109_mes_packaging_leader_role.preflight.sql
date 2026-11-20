-- release-target-preflight: migrationId=20261109_mes_packaging_leader_role; allowedEnvironments=test,backup,prod
-- Read-only baseline checks. Existing migration guards verify post-dependency state again at apply time.
WITH tmp_packaging_tenant AS (SELECT t.id FROM system_tenant t WHERE t.deleted=0 AND t.status=0 AND (t.package_id=-1 OR EXISTS (SELECT 1 FROM system_tenant_package p WHERE p.id=t.package_id AND p.deleted=0 AND p.status=0 AND CASE WHEN JSON_VALID(p.menu_ids) THEN JSON_CONTAINS(p.menu_ids,CAST((SELECT MAX(id) FROM system_menu WHERE deleted=0 AND status=0 AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage') AS JSON),'$') ELSE FALSE END)))
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND status=0
      AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage') <> 1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_packaging_leader_role:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM tmp_packaging_tenant t WHERE
      (SELECT COUNT(*) FROM system_role_category c WHERE c.tenant_id=t.id
        AND c.code='menu' AND c.deleted=b'0' AND c.status=0) <> 1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_packaging_leader_role:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM system_role r JOIN tmp_packaging_tenant t ON t.id=r.tenant_id
      WHERE r.code='packaging_leader_permission' GROUP BY r.tenant_id HAVING COUNT(*)>1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_packaging_leader_role:CONDITION_3'
 WHEN (EXISTS (SELECT 1 FROM system_role r JOIN tmp_packaging_tenant t ON t.id=r.tenant_id
      WHERE r.code='packaging_leader_permission' AND (r.deleted<>b'0' OR r.status<>0))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_packaging_leader_role:CONDITION_4'
 ELSE 'TARGET_PREFLIGHT_PASS:20261109_mes_packaging_leader_role' END;
