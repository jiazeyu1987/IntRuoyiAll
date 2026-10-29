-- release-target-preflight: migrationId=20261023_mes_team_leader_permission_identity; allowedEnvironments=test,backup,prod
SELECT CASE WHEN (( NOT COALESCE((EXISTS (
    SELECT 1
      FROM `system_tenant_package`
     WHERE `deleted` = b'0'
       AND NOT JSON_VALID(`menu_ids`)
  )),TRUE)
 AND NOT COALESCE((NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900435
       AND `status` = 0
       AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900435
       AND `deleted` = b'0'
       AND NOT (
         `name` = 'PQC组长'
         AND `path` = '/mes/pro/process-pool/pqc-leader'
         AND `component` = 'mes/pro/processpool/PqcLeaderWorkbenchPage'
         AND `component_name` = 'MesProProcessPoolPqcLeaderWorkbench'
       )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900439
       AND `deleted` = b'0'
       AND NOT (
         `name` = 'PQC组长通用查询'
         AND `permission` = 'mes:pro-process-pool-team-leader:query'
         AND `type` = 3
         AND `parent_id` = 900435
       )
  )),TRUE)
 AND NOT COALESCE((NOT EXISTS (
    SELECT 1
      FROM `system_users` AS `user`
     WHERE `user`.`username` = 'admin'
       AND `user`.`tenant_id` = 1
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
  )),TRUE)
) AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND engine='InnoDB'
 AND table_name IN('system_menu','system_role','system_role_menu','system_tenant','system_tenant_package','system_role_category','system_users','system_user_role'))=8 AND (SELECT COUNT(*) FROM system_menu WHERE id=900436 AND deleted=0 AND status=0 AND type=2
 AND permission='mes:pro-process-pool-team-leader:query'
 AND path='/mes/pro/process-pool/production-leader'
 AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')=1 AND NOT EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) AND NOT EXISTS (SELECT 1 FROM (SELECT 991611 id, CONVERT('mes:pro-process-pool-team-leader:review' USING utf8mb4) COLLATE utf8mb4_unicode_ci permission, '班组长提交复核' name, 2 sort
UNION ALL SELECT 991612, 'mes:pro-process-pool-team-leader:abnormal', '生产工单异常上报', 3
UNION ALL SELECT 991613, 'mes:pro-process-pool-team-leader:maintain', '生产组长维护', 4) d JOIN system_menu m ON m.id=d.id OR m.permission=d.permission
 WHERE NOT (m.id=d.id AND m.permission=d.permission AND m.type=3 AND m.parent_id=900436
 AND m.deleted=0 AND m.status=0 AND COALESCE(m.path,'')='' AND COALESCE(m.component,'')=''))
 AND NOT EXISTS (SELECT 1 FROM (SELECT 991611 id, CONVERT('mes:pro-process-pool-team-leader:review' USING utf8mb4) COLLATE utf8mb4_unicode_ci permission, '班组长提交复核' name, 2 sort
UNION ALL SELECT 991612, 'mes:pro-process-pool-team-leader:abnormal', '生产工单异常上报', 3
UNION ALL SELECT 991613, 'mes:pro-process-pool-team-leader:maintain', '生产组长维护', 4) d JOIN system_role_menu rm ON rm.menu_id=d.id
 WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=d.id))
 AND NOT EXISTS (SELECT 1 FROM (SELECT 991611 id, CONVERT('mes:pro-process-pool-team-leader:review' USING utf8mb4) COLLATE utf8mb4_unicode_ci permission, '班组长提交复核' name, 2 sort
UNION ALL SELECT 991612, 'mes:pro-process-pool-team-leader:abnormal', '生产工单异常上报', 3
UNION ALL SELECT 991613, 'mes:pro-process-pool-team-leader:maintain', '生产组长维护', 4) d JOIN system_tenant_package p
 ON JSON_CONTAINS(p.menu_ids, CAST(d.id AS JSON), '$') WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=d.id)) AND NOT EXISTS(SELECT 1 FROM system_menu WHERE id=900439 AND NOT (
 deleted=0 AND type=3 AND permission='mes:pro-process-pool-team-leader:query' AND parent_id=900435)) AND EXISTS(SELECT 1 FROM system_tenant WHERE id=1 AND deleted=0)
 AND NOT EXISTS(SELECT 1 FROM (SELECT t.id FROM system_tenant t WHERE t.deleted=0 AND (t.id=1 OR EXISTS(
 SELECT 1 FROM system_tenant_package p WHERE p.id=t.package_id AND p.deleted=0 AND JSON_CONTAINS(p.menu_ids,CAST('900435' AS JSON),'$')))) t WHERE (SELECT COUNT(*) FROM system_role_category c WHERE c.tenant_id=t.id AND c.code='menu' AND c.status=0 AND c.deleted=0)<>1)
 AND NOT EXISTS(SELECT r.tenant_id FROM system_role r JOIN (SELECT t.id FROM system_tenant t WHERE t.deleted=0 AND (t.id=1 OR EXISTS(
 SELECT 1 FROM system_tenant_package p WHERE p.id=t.package_id AND p.deleted=0 AND JSON_CONTAINS(p.menu_ids,CAST('900435' AS JSON),'$')))) t ON t.id=r.tenant_id WHERE r.code='pqc_leader_permission' GROUP BY r.tenant_id HAVING COUNT(*)>1)) AND NOT EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_team_leader_permissions_20261023')
THEN 'TARGET_PREFLIGHT_PASS:20261023_mes_team_leader_permission_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_mes_team_leader_permission_identity:PREREQUISITE_OR_IDENTITY_CONFLICT' END;
