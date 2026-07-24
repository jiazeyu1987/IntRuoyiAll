-- Explicit public site binding for the TEST showroom environment only.
-- Repeatable: inserts the missing TEST binding and fails if an existing binding points at a different tenant.
-- This default test/E2E seed must not write tenant_id=1 or the 芋道源码/admin tenant.
-- Apply only after verifying these tenant ids in system_tenant:
--   TEST -> tenant_id 122, name '测试租户'

DELIMITER $$

DROP PROCEDURE IF EXISTS `showroom_round13_seed_public_site_binding_test` $$
CREATE PROCEDURE `showroom_round13_seed_public_site_binding_test`()
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
  WHERE `id` = 122
    AND `name` = '测试租户'
    AND `deleted` = b'0';
  IF v_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected TEST tenant id=122 name=测试租户 before binding yingtai-showroom/TEST.';
  END IF;

  INSERT INTO `showroom_public_site_binding` (
    `site_key`, `stage`, `tenant_id`, `display_name`, `enabled`, `creator`, `updater`
  )
  SELECT 'yingtai-showroom', 'TEST', 122, 'Yingtai TEST - 测试租户', b'1',
         'showroom-round13-test-seed', 'showroom-round13-test-seed'
  WHERE NOT EXISTS (
    SELECT 1 FROM `showroom_public_site_binding`
    WHERE `site_key` = 'yingtai-showroom'
      AND `stage` = 'TEST'
  );

  SELECT COUNT(*) INTO v_count
  FROM `showroom_public_site_binding`
  WHERE `site_key` = 'yingtai-showroom'
    AND `stage` = 'TEST'
    AND `tenant_id` = 122
    AND `enabled` = b'1'
    AND `deleted` = b'0';
  IF v_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'yingtai-showroom/TEST binding exists but is not enabled for tenant 122.';
  END IF;
END $$

DELIMITER ;

CALL `showroom_round13_seed_public_site_binding_test`();

DROP PROCEDURE IF EXISTS `showroom_round13_seed_public_site_binding_test`;
