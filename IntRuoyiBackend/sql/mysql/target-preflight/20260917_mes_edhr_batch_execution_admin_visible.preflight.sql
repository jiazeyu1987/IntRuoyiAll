-- release-target-preflight: migrationId=20260917_mes_edhr_batch_execution_admin_visible; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `permission` = 'mes:pro-edhr-batch-processing:query' AND `type` = 1 AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900033 AND `permission` = 'mes:pro-edhr-batch-execution:query' AND `type` = 2 AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900034 AND `permission` = 'mes:pro-edhr-batch-execution:query' AND `type` = 3 AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900036 AND `permission` = 'mes:pro-edhr-batch-execution:update' AND `type` = 3 AND `status` = 0 AND `deleted` = b'0')
   AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path` = '/mes/pro/feedback/edhr-batch-execution' AND `id` <> 900033 AND `deleted` = b'0')
   AND NOT EXISTS (
     SELECT 1 FROM `system_tenant_package`
     WHERE `menu_ids` IS NOT NULL AND `menu_ids` <> '' AND NOT JSON_VALID(`menu_ids`)
   )
   -- The page parent is assigned by the migration; preserve its input identity checks.
   AND EXISTS (SELECT 1 FROM system_users WHERE username='admin' AND deleted=b'0')
   AND NOT EXISTS (
     SELECT 1 FROM system_users u WHERE u.username='admin' AND u.deleted=b'0'
       AND NOT EXISTS (
         SELECT 1 FROM system_user_role ur JOIN system_role r
           ON r.id=ur.role_id AND r.tenant_id=u.tenant_id
           AND r.code='super_admin' AND r.status=0 AND r.deleted=b'0'
         WHERE ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=b'0'
       )
   )
   AND NOT EXISTS (
     SELECT 1 FROM system_users u
     JOIN system_tenant t ON t.id=u.tenant_id AND t.deleted=b'0'
     JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0'
     WHERE u.username='admin' AND u.deleted=b'0' AND t.package_id<>0
       AND (p.menu_ids IS NULL OR p.menu_ids='' OR NOT JSON_VALID(p.menu_ids))
   )
  THEN 'TARGET_PREFLIGHT_PASS:20260917_mes_edhr_batch_execution_admin_visible'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260917_mes_edhr_batch_execution_admin_visible'
END;
