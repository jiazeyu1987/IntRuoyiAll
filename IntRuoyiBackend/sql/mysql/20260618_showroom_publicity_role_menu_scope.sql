-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260524_showroom_company_version_menu_visibility,20260524_showroom_prompt_menu_visibility; type=menu; riskLevel=medium
-- Keep the showroom_publicity role scoped to the approved showroom admin tabs only.

SET @showroom_publicity_role_count := (
    SELECT COUNT(*)
    FROM `system_role`
    WHERE `code` = 'showroom_publicity'
      AND `deleted` = b'0'
);

SET @showroom_publicity_required_menu_count := (
    SELECT COUNT(DISTINCT `id`)
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` IN (980100, 980101, 980118, 980102, 980119, 980103, 980104)
);

DROP PROCEDURE IF EXISTS `_showroom_publicity_role_menu_scope_requirements`;
DELIMITER //
CREATE PROCEDURE `_showroom_publicity_role_menu_scope_requirements`()
BEGIN
    IF @showroom_publicity_role_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing prerequisite role showroom_publicity';
    END IF;

    IF @showroom_publicity_required_menu_count <> 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing prerequisite showroom publicity menu ids: 980100, 980101, 980118, 980102, 980119, 980103, 980104';
    END IF;
END//
DELIMITER ;
CALL `_showroom_publicity_role_menu_scope_requirements`();
DROP PROCEDURE `_showroom_publicity_role_menu_scope_requirements`;

UPDATE `system_role_menu` AS `role_menu`
JOIN `system_role` AS `role`
  ON `role`.`id` = `role_menu`.`role_id`
 AND `role`.`tenant_id` = `role_menu`.`tenant_id`
 AND `role`.`code` = 'showroom_publicity'
 AND `role`.`deleted` = b'0'
SET `role_menu`.`deleted` = b'0',
    `role_menu`.`updater` = 'showroom-publicity-role-scope',
    `role_menu`.`update_time` = NOW()
WHERE `role_menu`.`menu_id` IN (980100, 980101, 980118, 980102, 980119, 980103, 980104)
  AND `role_menu`.`deleted` = b'1';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
    `role`.`id`,
    `menu`.`id`,
    'showroom-publicity-role-scope',
    NOW(),
    'showroom-publicity-role-scope',
    NOW(),
    b'0',
    `role`.`tenant_id`
FROM `system_role` AS `role`
JOIN `system_menu` AS `menu`
  ON `menu`.`id` IN (980100, 980101, 980118, 980102, 980119, 980103, 980104)
 AND `menu`.`deleted` = b'0'
WHERE `role`.`code` = 'showroom_publicity'
  AND `role`.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
  );

-- DCC menu bindings are removed because they are outside the approved showroom tab set.
UPDATE `system_role_menu` AS `role_menu`
JOIN `system_role` AS `role`
  ON `role`.`id` = `role_menu`.`role_id`
 AND `role`.`tenant_id` = `role_menu`.`tenant_id`
 AND `role`.`code` = 'showroom_publicity'
 AND `role`.`deleted` = b'0'
SET `role_menu`.`deleted` = b'1',
    `role_menu`.`updater` = 'showroom-publicity-role-scope',
    `role_menu`.`update_time` = NOW()
WHERE `role_menu`.`deleted` = b'0'
  AND `role_menu`.`menu_id` NOT IN (980100, 980101, 980118, 980102, 980119, 980103, 980104);
