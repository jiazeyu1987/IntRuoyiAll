-- release-target-preflight: migrationId=20261116_mes_deviation_administrator_role; allowedEnvironments=test,backup,prod
-- Same pre-write identity and scope guards as the migration, read-only.
WITH tmp_deviation_admin_permissions AS (SELECT 'mes:pro-edhr-deviation:query' AS permission UNION ALL SELECT 'mes:pro-edhr-deviation:create' UNION ALL SELECT 'mes:pro-edhr-deviation:handle' UNION ALL SELECT 'mes:pro-edhr-deviation:verify' UNION ALL SELECT 'mes:pro-edhr-deviation:department-confirm' UNION ALL SELECT 'mes:pro-edhr-deviation:qa-close' UNION ALL SELECT 'mes:pro-edhr-deviation:quality-approve' UNION ALL SELECT 'mes:pro-edhr-deviation:critical-management-approve' UNION ALL SELECT 'mes:pro-edhr-deviation:ncr-create' UNION ALL SELECT 'mes:pro-edhr-nonconformance-review:query' UNION ALL SELECT 'mes:pro-edhr-nonconformance-review:deviation-create' UNION ALL SELECT 'mes:pro-edhr-work-task:query'),
tmp_deviation_admin_tenants AS (SELECT id,package_id FROM system_tenant WHERE deleted=0 AND status=0 AND expire_time>CURRENT_TIMESTAMP)
SELECT CASE
 WHEN (EXISTS (SELECT 1 FROM tmp_deviation_admin_permissions p WHERE NOT EXISTS
      (SELECT 1 FROM system_menu m WHERE m.permission=p.permission AND m.deleted=0 AND m.status=0))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_mes_deviation_administrator_role:CONDITION_1'
 WHEN (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991530 AND parent_id=991516
      AND component='mes/pro/edhr-deviation/index' AND deleted=0 AND status=0)
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=9008300 AND parent_id=991516 AND component='mes/pro/edhr-nonconformance/NonconformanceReviewPage' AND deleted=0 AND status=0)
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0 AND type=1
      AND path='/mes/edhr-batch-processing' AND permission='mes:pro-edhr-batch-processing:query'
      AND deleted=0 AND status=0)
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991516 AND parent_id=900220 AND type=1
      AND path='/mes/edhr-production-execution' AND permission='' AND deleted=0 AND status=0)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_mes_deviation_administrator_role:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM tmp_deviation_admin_tenants t LEFT JOIN system_tenant_package p ON p.id=t.package_id
      WHERE t.package_id<>0 AND (p.id IS NULL OR p.deleted<>0 OR p.status<>0
        OR NOT JSON_VALID(p.menu_ids) OR JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_mes_deviation_administrator_role:CONDITION_3'
 WHEN (EXISTS (SELECT t.id FROM tmp_deviation_admin_tenants t LEFT JOIN system_role_category c
      ON c.tenant_id=t.id AND c.code='menu' AND c.deleted=0 AND c.status=0
      GROUP BY t.id HAVING COUNT(c.id)<>1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_mes_deviation_administrator_role:CONDITION_4'
 WHEN (EXISTS (SELECT 1 FROM system_role r JOIN tmp_deviation_admin_tenants t ON t.id=r.tenant_id
      WHERE r.code='deviation_admin_permission' AND (r.deleted<>0 OR r.status<>0 OR r.name<>'偏差管理员' OR r.type<>2
        OR r.category_id<>(SELECT c.id FROM system_role_category c WHERE c.tenant_id=t.id AND c.code='menu' AND c.deleted=0 AND c.status=0)
        OR r.category_id IS NULL))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_mes_deviation_administrator_role:CONDITION_5'
 WHEN (EXISTS (SELECT r.tenant_id FROM system_role r JOIN tmp_deviation_admin_tenants t ON t.id=r.tenant_id
      WHERE r.code='deviation_admin_permission' GROUP BY r.tenant_id HAVING COUNT(*)>1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_mes_deviation_administrator_role:CONDITION_6'
 ELSE 'TARGET_PREFLIGHT_PASS:20261116_mes_deviation_administrator_role' END;
