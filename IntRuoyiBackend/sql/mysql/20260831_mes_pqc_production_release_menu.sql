-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_production_release_roles; type=menu; riskLevel=low
DROP PROCEDURE IF EXISTS upgrade_mes_pqc_production_release_menu;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_pqc_production_release_menu()
BEGIN
  DECLARE v_menu_id BIGINT DEFAULT NULL;

  IF NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 900220 AND `type` = 1 AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch record parent menu 900220';
  END IF;

  IF (
      SELECT COUNT(*) FROM `system_menu`
      WHERE `permission` = 'mes:pro-production-release:query'
        AND `deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous production release query permission menu';
  END IF;

  SELECT `id` INTO v_menu_id
  FROM `system_menu`
  WHERE `permission` = 'mes:pro-production-release:query'
    AND `deleted` = b'0';

  UPDATE `system_menu`
  SET `name` = 'PQC生产放行',
      `type` = 2,
      `sort` = 1,
      `parent_id` = 900220,
      `path` = '/mes/production-release/pqc',
      `icon` = 'ep:circle-check',
      `component` = 'mes/pro/production-release/PqcProductionReleasePage',
      `component_name` = 'MesPqcProductionRelease',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `updater` = 'pqc-production-release-menu',
      `update_time` = NOW()
  WHERE `id` = v_menu_id;

  UPDATE `system_tenant_package`
  SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', v_menu_id),
      `updater` = 'pqc-production-release-menu',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND JSON_VALID(`menu_ids`)
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900220' AS JSON), '$')
    AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST(v_menu_id AS JSON), '$');
END$$
DELIMITER ;

CALL upgrade_mes_pqc_production_release_menu();
DROP PROCEDURE IF EXISTS upgrade_mes_pqc_production_release_menu;
