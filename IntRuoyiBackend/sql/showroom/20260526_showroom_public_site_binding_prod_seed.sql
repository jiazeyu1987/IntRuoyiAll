-- Explicit PROD public site binding for production release preparation only.
-- Do not execute this script during test, E2E, or reviewer verification.
-- Execute only before a production release after explicit approval that writing
-- tenant_id=1 / 芋道源码 is allowed for that release window.

DELIMITER $$

DROP PROCEDURE IF EXISTS `showroom_round13_seed_public_site_binding_prod` $$
CREATE PROCEDURE `showroom_round13_seed_public_site_binding_prod`()
BEGIN
  DECLARE v_count int DEFAULT 0;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'showroom_public_site_binding'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing showroom_public_site_binding; run 20260526_showroom_release_scope_migration.sql first.';
  END IF;

  SELECT COUNT(*) INTO v_count
  FROM `system_tenant`
  WHERE `id` = 1
    AND `name` = '芋道源码'
    AND `deleted` = b'0';
  IF v_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected PROD tenant id=1 name=芋道源码 before binding yingtai-showroom/PROD.';
  END IF;

  INSERT INTO `showroom_public_site_binding` (
    `site_key`, `stage`, `tenant_id`, `display_name`, `enabled`, `creator`, `updater`
  )
  SELECT 'yingtai-showroom', 'PROD', 1, 'Yingtai PROD - 芋道源码', b'1',
         'showroom-round13-prod-seed', 'showroom-round13-prod-seed'
  WHERE NOT EXISTS (
    SELECT 1 FROM `showroom_public_site_binding`
    WHERE `site_key` = 'yingtai-showroom'
      AND `stage` = 'PROD'
  );

  SELECT COUNT(*) INTO v_count
  FROM `showroom_public_site_binding`
  WHERE `site_key` = 'yingtai-showroom'
    AND `stage` = 'PROD'
    AND `tenant_id` = 1
    AND `enabled` = b'1'
    AND `deleted` = b'0';
  IF v_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'yingtai-showroom/PROD binding exists but is not enabled for tenant 1.';
  END IF;
END $$

DELIMITER ;

CALL `showroom_round13_seed_public_site_binding_prod`();

DROP PROCEDURE IF EXISTS `showroom_round13_seed_public_site_binding_prod`;
