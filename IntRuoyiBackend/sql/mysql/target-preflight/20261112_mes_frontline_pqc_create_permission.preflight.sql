-- release-target-preflight: migrationId=20261112_mes_frontline_pqc_create_permission; allowedEnvironments=test,backup,prod
-- Navigation dependency grants page 900438 to these existing active roles and their packages.
-- Check the pre-dependency baseline here; runtime retains the exact post-dependency page grant checks.
WITH tmp_frontline_pqc_create_role AS (SELECT r.id,r.tenant_id,t.package_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code IN('pqc_permission','pqc_leader_permission') AND r.deleted=0 AND r.status=0
)
SELECT CASE
 WHEN (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900438 AND deleted=0 AND status=0
      AND component='mes/pro/edhr-batch/BatchPqcFillPage')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_frontline_pqc_create_permission:CONDITION_1'
 WHEN (NOT EXISTS (SELECT 1 FROM tmp_frontline_pqc_create_role)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_frontline_pqc_create_permission:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM tmp_frontline_pqc_create_role r LEFT JOIN system_approval_authority_revision a
      ON a.tenant_id=r.tenant_id WHERE a.tenant_id IS NULL)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_frontline_pqc_create_permission:CONDITION_3'
 WHEN (EXISTS (SELECT 1 FROM tmp_frontline_pqc_create_role r LEFT JOIN system_tenant_package p
      ON p.id=r.package_id AND p.deleted=0 AND p.status=0
      WHERE r.package_id<>-1 AND r.package_id<>0
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids)))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_frontline_pqc_create_permission:CONDITION_4'
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE parent_id=900438
      AND permission='mes:pro-feedback:create' AND deleted=0)>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_frontline_pqc_create_permission:CONDITION_5'
 WHEN (EXISTS (SELECT 1 FROM system_menu WHERE parent_id=900438 AND permission='mes:pro-feedback:create' AND deleted=0 AND (status<>0 OR type<>3))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_frontline_pqc_create_permission:CONDITION_6'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_mes_frontline_pqc_create_permission' END;
