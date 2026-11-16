-- release-target-preflight: migrationId=20261112_mes_production_leader_pqc_parent_grant; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0
    AND permission='mes:pro-process-pool-team-leader:query'
    AND ((id=900439 AND type=3 AND parent_id=900435)
      OR (id=900436 AND type=2 AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')))<>2) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_production_leader_pqc_parent_grant:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0
      AND (a.tenant_id IS NULL OR NOT EXISTS (SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=900436 AND rm.deleted=0)))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_production_leader_pqc_parent_grant:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_mes_production_leader_pqc_parent_grant' END;
