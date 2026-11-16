-- release-target-preflight: migrationId=20261112_mes_pqc_role_navigation_boundary; allowedEnvironments=test,backup,prod
-- Query button may be created by this migration; reject duplicates and invalid existing identities.
WITH tmp_pqc_navigation_role AS (SELECT r.id,r.tenant_id,r.code FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code IN('pqc_permission','pqc_leader_permission') AND r.deleted=0 AND r.status=0),
tmp_pqc_navigation_button AS (SELECT 9915801 AS id,5321 AS source_id,'mes:dv-machinery:query' AS permission UNION ALL SELECT 9915802 AS id,5551 AS source_id,'mes:pro-feedback:query' AS permission UNION ALL SELECT 9915803 AS id,5552 AS source_id,'mes:pro-feedback:create' AS permission UNION ALL SELECT 9915804 AS id,5631 AS source_id,'mes:qc-template:query' AS permission UNION ALL SELECT 9915805 AS id,991611 AS source_id,'mes:pro-process-pool-team-leader:review' AS permission UNION ALL SELECT 9915806 AS id,991612 AS source_id,'mes:pro-process-pool-team-leader:abnormal' AS permission UNION ALL SELECT 9915807 AS id,991613 AS source_id,'mes:pro-process-pool-team-leader:maintain' AS permission UNION ALL SELECT 9915808 AS id,9008301 AS source_id,'mes:pro-edhr-nonconformance-review:query' AS permission UNION ALL SELECT 9915809 AS id,9008302 AS source_id,'mes:pro-edhr-nonconformance-review:create' AS permission)
SELECT CASE
 WHEN (NOT EXISTS (SELECT 1 FROM tmp_pqc_navigation_role)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM tmp_pqc_navigation_role GROUP BY tenant_id,code HAVING COUNT(*)>1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM tmp_pqc_navigation_role r LEFT JOIN system_approval_authority_revision a
      ON a.tenant_id=r.tenant_id WHERE a.tenant_id IS NULL)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_3'
 WHEN ((SELECT COUNT(*) FROM system_menu m JOIN tmp_pqc_navigation_button b ON b.source_id=m.id
      AND BINARY b.permission=BINARY m.permission WHERE m.type=3 AND m.deleted=0 AND m.status=0)<>9) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_4'
 WHEN (EXISTS (SELECT 1 FROM system_menu m JOIN tmp_pqc_navigation_button b ON b.id=m.id
      WHERE m.deleted<>0 OR m.status<>0 OR m.type<>3 OR m.parent_id<>900435 OR BINARY m.permission<>BINARY b.permission)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_5'
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0
      AND ((id=900435 AND component='mes/pro/processpool/PqcLeaderWorkbenchPage')
        OR (id=900438 AND component='mes/pro/edhr-batch/BatchPqcFillPage')
        OR id IN(900220,991516,991512,900440,900439)))<>7) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_6'
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=1200 AND path='/approval-center') OR (id=1207 AND parent_id=1200 AND path='todo')
        OR (id=1221 AND parent_id=1207 AND type=3 AND permission='bpm:task:query')
        OR (id=1201 AND parent_id=1200 AND path='my-initiated')
        OR (id=1208 AND parent_id=1200 AND path='done')
        OR (id=2713 AND parent_id=1200 AND path='cc' AND permission='bpm:process-instance-cc:query')))<>6) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_7'
 WHEN (EXISTS (SELECT 1 FROM system_menu WHERE parent_id=900438
      AND permission='mes:pro-feedback:query' AND deleted=0 AND (type<>3 OR status<>0))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_8'
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE parent_id=900438
      AND permission='mes:pro-feedback:query' AND type=3 AND deleted=0 AND status=0)>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_9'
 WHEN (EXISTS (SELECT 1 FROM system_tenant t LEFT JOIN system_tenant_package p
      ON p.id=t.package_id AND p.deleted=0 AND p.status=0 WHERE EXISTS (SELECT 1 FROM tmp_pqc_navigation_role r WHERE r.tenant_id=t.id)
      AND t.package_id<>0 AND t.package_id<>-1
      AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids)))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_pqc_role_navigation_boundary:CONDITION_10'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_mes_pqc_role_navigation_boundary' END;
