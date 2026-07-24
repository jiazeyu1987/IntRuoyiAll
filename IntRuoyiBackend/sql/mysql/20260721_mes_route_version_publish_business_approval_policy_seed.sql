-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_mes_route_version_approval_bpm_seed,20260719_business_approval_policy; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_route_version_publish_policy;

DELIMITER //
CREATE PROCEDURE ensure_mes_route_version_publish_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route version publish policy seed requires bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'system_tenant'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route version publish policy seed requires system_tenant';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'ROUTE_VERSION'
      AND `policy`.`action_code` = 'PUBLISH'
      AND `policy`.`object_state` = 'DRAFT'
      AND `policy`.`status` = 'PUBLISHED'
      AND COALESCE(`policy`.`effect_executor_code`, '') <> 'MES_ROUTE_VERSION_PUBLISH'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route version publish policy conflict';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_version_publish_policy_tenants`;
  CREATE TEMPORARY TABLE `tmp_mes_route_version_publish_policy_tenants` AS
  SELECT `tenant`.`id` AS `tenant_id`
  FROM `system_tenant` AS `tenant`
  WHERE `tenant`.`deleted` = b'0'
    AND `tenant`.`status` = 0;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_route_version_publish_policy_tenants`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route version publish policy requires active tenants';
  END IF;

  UPDATE `bpm_business_approval_policy` AS `policy`
  SET `policy`.`policy_mode` = 'BPM_REQUIRED',
      `policy`.`process_definition_key` = 'mes-route-version-approval-v1',
      `policy`.`remark` = 'MES route version DRAFT submit publish starts BPM approval; reviewer signature is captured by approval task',
      `policy`.`updater` = '1',
      `policy`.`update_time` = NOW()
  WHERE `policy`.`deleted` = b'0'
    AND `policy`.`data_domain` = 'MES'
    AND `policy`.`system_code` = 'MES'
    AND `policy`.`object_type` = 'ROUTE_VERSION'
    AND `policy`.`action_code` = 'PUBLISH'
    AND `policy`.`object_state` = 'DRAFT'
    AND `policy`.`status` = 'PUBLISHED'
    AND `policy`.`effect_executor_code` = 'MES_ROUTE_VERSION_PUBLISH'
    AND (
      COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'
      OR COALESCE(`policy`.`process_definition_key`, '') <> 'mes-route-version-approval-v1'
    );

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `source`.`tenant_id`, 'MES', 'MES', 'ROUTE_VERSION', 'PUBLISH', 'DRAFT',
         'BPM_REQUIRED', 'mes-route-version-approval-v1', 'MES_ROUTE_VERSION_PUBLISH', 'PUBLISHED',
         'MES route version DRAFT submit publish starts BPM approval; reviewer signature is captured by approval task',
         '1', NOW(), '1', NOW(), b'0'
  FROM `tmp_mes_route_version_publish_policy_tenants` AS `source`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'MES'
      AND `existing`.`system_code` = 'MES'
      AND `existing`.`object_type` = 'ROUTE_VERSION'
      AND `existing`.`action_code` = 'PUBLISH'
      AND `existing`.`object_state` = 'DRAFT'
      AND `existing`.`status` = 'PUBLISHED'
  );

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'MES'
      AND `system_code` = 'MES'
      AND `object_type` = 'ROUTE_VERSION'
      AND `action_code` = 'PUBLISH'
      AND `object_state` = 'DRAFT'
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`, `object_type`, `action_code`, `object_state`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route version publish policy duplicate';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_version_publish_policy_tenants`;
END//
DELIMITER ;

CALL ensure_mes_route_version_publish_policy();

DROP PROCEDURE IF EXISTS ensure_mes_route_version_publish_policy;
