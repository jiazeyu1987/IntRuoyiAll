-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260830_mes_edhr_nonconformance_review_mvp; type=data; riskLevel=medium
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP PROCEDURE IF EXISTS grant_mes_pqc_leader_nonconformance_review_permission_20260901;
DELIMITER $$
CREATE PROCEDURE grant_mes_pqc_leader_nonconformance_review_permission_20260901()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_nonconformance_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_pqc_leader_nonconformance_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY,
    `permission` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_pqc_leader_nonconformance_menu` (`menu_id`, `permission`)
  VALUES
    (9008301, 'mes:pro-edhr-nonconformance-review:query'),
    (9008302, 'mes:pro-edhr-nonconformance-review:create');

  IF EXISTS (
      SELECT 1
        FROM `tmp_mes_pqc_leader_nonconformance_menu` AS `expected`
        LEFT JOIN `system_menu` AS `menu`
          ON `menu`.`id` = `expected`.`menu_id`
         AND `menu`.`permission` = `expected`.`permission`
         AND `menu`.`status` = 0
         AND `menu`.`deleted` = b'0'
       WHERE `menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR nonconformance review query/create menu permission';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `tmp_mes_pqc_leader_nonconformance_menu` AS `expected`
        JOIN `system_menu` AS `menu`
          ON `menu`.`permission` = `expected`.`permission`
         AND `menu`.`status` = 0
         AND `menu`.`deleted` = b'0'
       GROUP BY `expected`.`permission`
      HAVING COUNT(*) > 2
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate eDHR nonconformance review permission menu';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu` AS `menu`
       WHERE `menu`.`permission` = 'mes:pro-edhr-nonconformance-review:create'
         AND `menu`.`status` = 0
         AND `menu`.`deleted` = b'0'
       GROUP BY `menu`.`permission`
      HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate eDHR nonconformance review create permission menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_nonconformance_role`;
  CREATE TEMPORARY TABLE `tmp_mes_pqc_leader_nonconformance_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_pqc_leader_nonconformance_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
   WHERE `role`.`code` = 'pqc_leader_permission'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_pqc_leader_nonconformance_role`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing active pqc_leader_permission role';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_role` AS `role`
       WHERE `role`.`code` = 'pqc_leader_permission'
         AND `role`.`status` = 0
         AND `role`.`deleted` = b'0'
       GROUP BY `role`.`tenant_id`
      HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate pqc_leader_permission role in one tenant';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_pqc_leader_nonconformance_role` AS `pqc_leader_role`
      ON `pqc_leader_role`.`role_id` = `role_menu`.`role_id`
     AND `pqc_leader_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `system_menu` AS `menu`
      ON `menu`.`id` = `role_menu`.`menu_id`
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0'
    JOIN `tmp_mes_pqc_leader_nonconformance_menu` AS `expected`
      ON `expected`.`menu_id` = `menu`.`id`
     AND `expected`.`permission` = `menu`.`permission`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'mes-pqc-leader-nonconformance-review',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `pqc_leader_role`.`role_id`,
    `menu`.`id`,
    'mes-pqc-leader-nonconformance-review',
    NOW(),
    'mes-pqc-leader-nonconformance-review',
    NOW(),
    b'0',
    `pqc_leader_role`.`tenant_id`
    FROM `tmp_mes_pqc_leader_nonconformance_role` AS `pqc_leader_role`
    CROSS JOIN `tmp_mes_pqc_leader_nonconformance_menu` AS `expected`
    JOIN `system_menu` AS `menu`
      ON `menu`.`id` = `expected`.`menu_id`
     AND `menu`.`permission` = `expected`.`permission`
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `pqc_leader_role`.`role_id`
        AND `existing`.`tenant_id` = `pqc_leader_role`.`tenant_id`
        AND `existing`.`menu_id` = `menu`.`id`
   );

  IF EXISTS (
      SELECT 1
        FROM `tmp_mes_pqc_leader_nonconformance_role` AS `pqc_leader_role`
        CROSS JOIN `tmp_mes_pqc_leader_nonconformance_menu` AS `expected`
       WHERE NOT EXISTS (
         SELECT 1
           FROM `system_role_menu` AS `role_menu`
           JOIN `system_menu` AS `menu`
             ON `menu`.`id` = `role_menu`.`menu_id`
            AND `menu`.`status` = 0
            AND `menu`.`deleted` = b'0'
          WHERE `role_menu`.`role_id` = `pqc_leader_role`.`role_id`
            AND `role_menu`.`tenant_id` = `pqc_leader_role`.`tenant_id`
            AND `role_menu`.`deleted` = b'0'
            AND `role_menu`.`menu_id` = `expected`.`menu_id`
            AND `menu`.`permission` = `expected`.`permission`
       )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC leader nonconformance review grant incomplete';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `tmp_mes_pqc_leader_nonconformance_role` AS `pqc_leader_role`
        JOIN `system_role_menu` AS `role_menu`
          ON `role_menu`.`role_id` = `pqc_leader_role`.`role_id`
         AND `role_menu`.`tenant_id` = `pqc_leader_role`.`tenant_id`
         AND `role_menu`.`deleted` = b'0'
        JOIN `system_menu` AS `menu`
          ON `menu`.`id` = `role_menu`.`menu_id`
         AND `menu`.`permission` = 'mes:pro-edhr-nonconformance-review:dispose'
         AND `menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC leader role must not own nonconformance dispose permission';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_nonconformance_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_nonconformance_menu`;
END$$
DELIMITER ;

START TRANSACTION;
CALL grant_mes_pqc_leader_nonconformance_review_permission_20260901();
COMMIT;
DROP PROCEDURE IF EXISTS grant_mes_pqc_leader_nonconformance_review_permission_20260901;
