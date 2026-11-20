-- release-target-preflight: migrationId=20261112_mes_production_leader_frontline_tab; allowedEnvironments=test,backup,prod
-- Only existing production permission roles are applicable; empty scope is explicitly reported at apply time.
WITH tmp_production_frontline_role AS (SELECT r.id,r.tenant_id,t.package_id FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0 WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0)
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=900437 AND type=2 AND component='mes/pro/edhr-batch/BatchProductionFillPage'
        AND permission='mes:pro-edhr-batch-execution:query')
       OR (id=900435 AND type=2 AND component='mes/pro/processpool/PqcLeaderWorkbenchPage'
        AND permission='mes:pro-process-pool-pqc-leader:query')
       OR (id=5551 AND type=3 AND permission='mes:pro-feedback:query')
       OR (id=5552 AND type=3 AND permission='mes:pro-feedback:create')))<>4) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_production_leader_frontline_tab:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM tmp_production_frontline_role GROUP BY tenant_id HAVING COUNT(*)>1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_production_leader_frontline_tab:CONDITION_3'
 WHEN (EXISTS (SELECT 1 FROM tmp_production_frontline_role r
      LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id WHERE a.tenant_id IS NULL)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_production_leader_frontline_tab:CONDITION_4'
 WHEN (EXISTS (SELECT 1 FROM tmp_production_frontline_role r
      LEFT JOIN system_tenant_package p ON p.id=r.package_id AND p.deleted=0 AND p.status=0
      WHERE NOT (r.tenant_id=1 AND r.package_id=0) AND r.package_id<>-1
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids)))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_production_leader_frontline_tab:CONDITION_5'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_mes_production_leader_frontline_tab' END;
