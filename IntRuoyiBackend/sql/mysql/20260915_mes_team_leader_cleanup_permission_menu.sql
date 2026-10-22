-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_team_leader; type=data; riskLevel=low
-- Attach the existing team-leader maintain permission to the current production-leader page menu.

DELIMITER $$
DROP PROCEDURE IF EXISTS sync_mes_team_leader_cleanup_permission_menu$$
CREATE PROCEDURE sync_mes_team_leader_cleanup_permission_menu()
BEGIN
  DECLARE production_leader_menu_id bigint DEFAULT NULL;
  DECLARE maintain_menu_id bigint DEFAULT NULL;

  SELECT `id` INTO production_leader_menu_id
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 2
    AND `permission` = 'mes:pro-process-pool-team-leader:query'
    AND `component` = 'mes/pro/processpool/ProductionLeaderWorkbenchPage'
  LIMIT 1;

  IF production_leader_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production leader page menu is missing; cannot sync cleanup permission';
  END IF;

  SELECT `id` INTO maintain_menu_id
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 3
    AND `permission` = 'mes:pro-process-pool-team-leader:maintain'
  LIMIT 1;

  IF maintain_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Team leader maintain permission menu is missing';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `type` = 3
        AND `permission` = 'mes:pro-process-pool-team-leader:maintain') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Team leader maintain permission menu is not unique';
  END IF;

  UPDATE `system_menu`
  SET `parent_id` = production_leader_menu_id,
      `name` = '生产组长维护',
      `sort` = 4,
      `status` = 0,
      `visible` = b'1',
      `updater` = 'team-leader-cleanup-permission',
      `update_time` = NOW()
  WHERE `id` = maintain_menu_id
    AND (`parent_id` <> production_leader_menu_id
      OR `name` <> '生产组长维护'
      OR `sort` <> 4
      OR `status` <> 0
      OR `visible` <> b'1');

  IF (SELECT COUNT(*) FROM `system_menu`
      WHERE `id` = maintain_menu_id
        AND `parent_id` = production_leader_menu_id
        AND `deleted` = b'0'
        AND `status` = 0) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Team leader cleanup permission menu sync failed';
  END IF;
END$$
DELIMITER ;

CALL sync_mes_team_leader_cleanup_permission_menu();

DROP PROCEDURE IF EXISTS sync_mes_team_leader_cleanup_permission_menu;
