-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260702_mes_edhr_seven_visible_tabs; type=menu; riskLevel=low
-- Rename the eDHR release page to a read-only trace menu. Operation APIs stay available from batch execution detail.
SET NAMES utf8mb4;

UPDATE `system_menu`
SET `name` = '放行追溯',
    `permission` = 'mes:pro-edhr-release:query',
    `type` = 2,
    `sort` = 5,
    `parent_id` = 900220,
    `path` = '/mes/pro/feedback/edhr-release',
    `icon` = 'ep:finished',
    `component` = 'mes/pro/edhr-release/ReleasePage',
    `component_name` = 'MesProEdhrReleasePage',
    `updater` = 'edhr-release-trace-menu',
    `update_time` = NOW()
WHERE `id` = 900260
  AND `deleted` = b'0';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_trace_menu;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_release_trace_menu()
BEGIN
  IF (SELECT COUNT(*)
      FROM `system_menu`
      WHERE `id` = 900260
        AND `name` = '放行追溯'
        AND `permission` = 'mes:pro-edhr-release:query'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/feedback/edhr-release'
        AND `component` = 'mes/pro/edhr-release/ReleasePage'
        AND `component_name` = 'MesProEdhrReleasePage'
        AND `deleted` = b'0') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR release trace menu row; cannot complete menu rename';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_release_trace_menu();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_trace_menu;
