-- release-target-preflight: migrationId=20260806_mes_pqc_leader_role_permission_tab; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 NOT COALESCE((EXISTS (
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
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_pqc_leader_role_permission_tab_20260806')
 THEN 'TARGET_PREFLIGHT_PASS:20260806_mes_pqc_leader_role_permission_tab' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260806_mes_pqc_leader_role_permission_tab:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
