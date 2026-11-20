-- release-target-preflight: migrationId=20261114_mes_production_leader_deviation_create; allowedEnvironments=test,backup,prod
-- Only existing production permission roles are applicable; empty scope is explicitly reported at apply time.
WITH tmp_production_deviation_create_role AS (SELECT r.id,r.tenant_id,t.package_id FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0 WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0)
SELECT CASE
 WHEN (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900436 AND deleted=0 AND status=0 AND type=2
      AND permission='mes:pro-process-pool-team-leader:query'
      AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261114_mes_production_leader_deviation_create:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_menu WHERE id=9915822 AND
      (deleted<>0 OR status<>0 OR type<>3 OR parent_id<>900436
       OR permission<>'mes:pro-process-pool-team-leader:deviation-create'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND id<>9915822
       AND permission='mes:pro-process-pool-team-leader:deviation-create')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261114_mes_production_leader_deviation_create:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM tmp_production_deviation_create_role r
      LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
      WHERE a.tenant_id IS NULL OR NOT EXISTS (SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=900436 AND rm.deleted=0))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261114_mes_production_leader_deviation_create:CONDITION_4'
 WHEN (EXISTS (SELECT 1 FROM tmp_production_deviation_create_role r
      LEFT JOIN system_tenant_package p ON p.id=r.package_id AND p.deleted=0 AND p.status=0
      WHERE NOT (r.tenant_id=1 AND r.package_id=0) AND r.package_id<>-1
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids)))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261114_mes_production_leader_deviation_create:CONDITION_5'
 WHEN (EXISTS (SELECT 1 FROM tmp_production_deviation_create_role r JOIN system_tenant_package p ON p.id=r.package_id
      WHERE JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY'
        OR NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'900436','$'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261114_mes_production_leader_deviation_create:CONDITION_6'
 ELSE 'TARGET_PREFLIGHT_PASS:20261114_mes_production_leader_deviation_create' END;
