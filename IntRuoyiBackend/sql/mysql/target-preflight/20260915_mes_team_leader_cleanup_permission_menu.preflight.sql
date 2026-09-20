-- release-target-preflight: migrationId=20260915_mes_team_leader_cleanup_permission_menu; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (
    SELECT COUNT(*) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 2
      AND `permission` = 'mes:pro-process-pool-team-leader:query'
      AND `component` = 'mes/pro/processpool/ProductionLeaderWorkbenchPage'
  ) = 1
  AND (
    SELECT COUNT(*) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 3
      AND `permission` = 'mes:pro-process-pool-team-leader:maintain'
  ) = 1
  AND EXISTS (
    SELECT 1
    FROM `system_menu` AS `maintain`
    JOIN `system_menu` AS `page`
      ON `page`.`permission` = 'mes:pro-process-pool-team-leader:query'
     AND `page`.`component` = 'mes/pro/processpool/ProductionLeaderWorkbenchPage'
     AND `page`.`deleted` = b'0'
     AND `page`.`type` = 2
     AND `maintain`.`parent_id` = `page`.`id`
    WHERE `maintain`.`deleted` = b'0'
      AND `maintain`.`type` = 3
      AND `maintain`.`permission` = 'mes:pro-process-pool-team-leader:maintain'
      AND `maintain`.`name` = '生产组长维护'
      AND `maintain`.`sort` = 4
      AND `maintain`.`status` = 0
      AND `maintain`.`visible` = b'1'
  )
  THEN 'TARGET_PREFLIGHT_PASS:20260915_mes_team_leader_cleanup_permission_menu'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260915_mes_team_leader_cleanup_permission_menu'
END;
