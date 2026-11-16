-- release-target-preflight: migrationId=20261109_mes_pqc_packaging_leader_role; allowedEnvironments=test,backup,prod
-- Read-only baseline checks. Existing migration guards verify post-dependency state again at apply time.
WITH tmp_pqc_packaging_menu AS (SELECT id FROM system_menu WHERE deleted=b'0' AND status=0
    AND (id=(SELECT MAX(id) FROM system_menu WHERE deleted=0 AND status=0 AND component='mes/pro/processpool/PqcLeaderWorkbenchPage') OR (type=3 AND permission IN ('mes:pro-process-pool-team-leader:query', 'mes:pro-process-pool-team-leader:maintain', 'mes:pro-process-pool-team-leader:review', 'mes:pro-process-pool-team-leader:abnormal', 'mes:pro-process-pool-pqc-leader:query'))
      OR permission='mes:pro-process-pool:event-revision:update'
      OR permission IN ('mes:pro-edhr-work-task:query','mes:pro-feedback:query','mes:pro-feedback:create',
        'mes:pro-edhr-nonconformance-review:query','mes:pro-edhr-nonconformance-review:create'))),
tmp_pqc_packaging_tenant AS (SELECT t.id FROM system_tenant t WHERE t.deleted=0 AND t.status=0 AND (t.package_id IN(-1,0) OR EXISTS (SELECT 1 FROM system_tenant_package p WHERE p.id=t.package_id AND p.deleted=0 AND p.status=0 AND CASE WHEN JSON_VALID(p.menu_ids) THEN JSON_CONTAINS(p.menu_ids,CAST((SELECT MAX(id) FROM system_menu WHERE deleted=0 AND status=0 AND component='mes/pro/processpool/PqcLeaderWorkbenchPage') AS JSON),'$') ELSE FALSE END)))
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND status=0
      AND component='mes/pro/processpool/PqcLeaderWorkbenchPage') <> 1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_pqc_packaging_leader_role:CONDITION_1'
 WHEN ((SELECT COUNT(DISTINCT permission) FROM system_menu WHERE id IN (SELECT id FROM tmp_pqc_packaging_menu)
      AND permission IN ('mes:pro-process-pool-team-leader:query','mes:pro-process-pool-team-leader:maintain',
        'mes:pro-process-pool-team-leader:review','mes:pro-process-pool-pqc-leader:query',
        'mes:pro-edhr-work-task:query',
        'mes:pro-feedback:query','mes:pro-feedback:create',
        'mes:pro-edhr-nonconformance-review:query','mes:pro-edhr-nonconformance-review:create')) <> 9) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_pqc_packaging_leader_role:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM tmp_pqc_packaging_tenant t WHERE
      (SELECT COUNT(*) FROM system_role_category c WHERE c.tenant_id=t.id
        AND c.code='menu' AND c.deleted=b'0' AND c.status=0) <> 1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_pqc_packaging_leader_role:CONDITION_3'
 WHEN (EXISTS (SELECT 1 FROM system_role r JOIN tmp_pqc_packaging_tenant t ON t.id=r.tenant_id
      WHERE r.code='pqc_packaging_leader_permission' GROUP BY r.tenant_id HAVING COUNT(*)>1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_pqc_packaging_leader_role:CONDITION_4'
 WHEN (EXISTS (SELECT 1 FROM system_role r JOIN tmp_pqc_packaging_tenant t ON t.id=r.tenant_id
      WHERE r.code='pqc_packaging_leader_permission' AND (r.deleted<>b'0' OR r.status<>0))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_pqc_packaging_leader_role:CONDITION_5'
 WHEN (EXISTS (SELECT 1 FROM system_menu WHERE permission='mes:pro-process-pool:event-revision:update'
 AND (deleted<>0 OR status<>0 OR type<>3 OR parent_id<>(SELECT MAX(id) FROM system_menu WHERE deleted=0 AND status=0 AND component='mes/pro/processpool/PqcLeaderWorkbenchPage')))
 OR (SELECT COUNT(*) FROM system_menu WHERE permission='mes:pro-process-pool:event-revision:update')>1)
 THEN 'TARGET_PREFLIGHT_BLOCKED:20261109_mes_pqc_packaging_leader_role:EVENT_PERMISSION_IDENTITY'
 ELSE 'TARGET_PREFLIGHT_PASS:20261109_mes_pqc_packaging_leader_role' END;
