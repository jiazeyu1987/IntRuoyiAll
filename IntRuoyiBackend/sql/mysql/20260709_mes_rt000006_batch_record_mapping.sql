-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260709_mes_route_flow_config_unification; type=data; riskLevel=medium
-- RT000006 / 球囊扩张压力泵 / route_id=922067：按“工序名称 == 批记录表单名称”补齐工艺流程批记录配置。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS ensure_mes_rt000006_batch_record_mapping;
DELIMITER $$
CREATE PROCEDURE ensure_mes_rt000006_batch_record_mapping()
BEGIN
  SET @target_route_id = 922067;
  SET @target_route_code = 'RT000006';
  SET @target_route_name = '球囊扩张压力泵';
  SET @production_role_name = '压力泵生产填写员';
  SET @quality_role_name = '压力泵质量填写员';
  SET @equipment_role_name = '压力泵设备填写员';

  IF (
    SELECT COUNT(1)
      FROM `mes_pro_route` route
     WHERE route.`id` = @target_route_id
       AND route.`code` COLLATE utf8mb4_unicode_ci = @target_route_code COLLATE utf8mb4_unicode_ci
       AND route.`name` COLLATE utf8mb4_unicode_ci = @target_route_name COLLATE utf8mb4_unicode_ci
       AND route.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing RT000006 pressure pump route';
  END IF;

  IF (
    SELECT COUNT(1)
      FROM `system_role` role
     WHERE role.`name` COLLATE utf8mb4_unicode_ci IN (
       @production_role_name COLLATE utf8mb4_unicode_ci,
       @quality_role_name COLLATE utf8mb4_unicode_ci,
       @equipment_role_name COLLATE utf8mb4_unicode_ci
     )
       AND role.`deleted` = b'0'
       AND role.`status` = 0
  ) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing RT000006 pressure pump role';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_route_process` route_process
      JOIN `mes_pro_process` process
        ON process.`id` = route_process.`process_id`
       AND process.`tenant_id` = route_process.`tenant_id`
       AND process.`deleted` = b'0'
      LEFT JOIN `mes_pro_batch_record_report` report
        ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
       AND report.`tenant_id` = route_process.`tenant_id`
       AND report.`deleted` = b'0'
     WHERE route_process.`route_id` = @target_route_id
       AND route_process.`deleted` = b'0'
       AND report.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing RT000006 batch record report';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_route_process` route_process
      JOIN `mes_pro_process` process
        ON process.`id` = route_process.`process_id`
       AND process.`tenant_id` = route_process.`tenant_id`
       AND process.`deleted` = b'0'
      JOIN `mes_pro_batch_record_report` report
        ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
       AND report.`tenant_id` = route_process.`tenant_id`
       AND report.`deleted` = b'0'
     WHERE route_process.`route_id` = @target_route_id
       AND route_process.`deleted` = b'0'
     GROUP BY route_process.`tenant_id`, route_process.`id`
    HAVING COUNT(1) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate RT000006 batch record report';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_route_process` route_process
      JOIN `mes_pro_route_flow_process_batch_record` existing_record
        ON existing_record.`tenant_id` = route_process.`tenant_id`
       AND existing_record.`route_process_id` = route_process.`id`
       AND existing_record.`use_type` = 'BATCH'
       AND existing_record.`report_sort` = route_process.`sort`
       AND existing_record.`deleted` = b'0'
      JOIN `mes_pro_process` process
        ON process.`id` = route_process.`process_id`
       AND process.`tenant_id` = route_process.`tenant_id`
       AND process.`deleted` = b'0'
      JOIN `mes_pro_batch_record_report` report
        ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
       AND report.`tenant_id` = route_process.`tenant_id`
       AND report.`deleted` = b'0'
     WHERE route_process.`route_id` = @target_route_id
       AND route_process.`deleted` = b'0'
       AND existing_record.`batch_record_report_id` <> report.`report_id`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'RT000006 batch record sort conflict';
  END IF;

  INSERT INTO `mes_pro_route_flow_config`
    (`route_id`, `use_type`, `enabled`, `config_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT DISTINCT route_process.`route_id`, 'BATCH', b'1', 'rt000006-pressure-pump-batch-record-mapping',
         'RT000006 球囊扩张压力泵批记录配置映射', 'rt000006-batch-record-mapping', NOW(),
         'rt000006-batch-record-mapping', NOW(), b'0', route_process.`tenant_id`
    FROM `mes_pro_route_process` route_process
   WHERE NOT EXISTS (
       SELECT 1
          FROM `mes_pro_route_flow_config` existing_config
        WHERE existing_config.`tenant_id` = route_process.`tenant_id`
          AND existing_config.`route_id` = route_process.`route_id`
          AND existing_config.`use_type` = 'BATCH'
          AND existing_config.`deleted` = b'0'
     )
     AND route_process.`route_id` = @target_route_id
     AND route_process.`deleted` = b'0';

  INSERT INTO `mes_pro_route_flow_process_config`
    (`route_flow_config_id`, `route_id`, `route_process_id`, `use_type`, `enabled`, `execution_mode`,
     `batch_record_report_id`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT route_flow_config.`id`, route_process.`route_id`, route_process.`id`, 'BATCH', b'1', 'SEQUENTIAL',
         report.`report_id`, 'RT000006 球囊扩张压力泵批记录工序配置映射',
         'rt000006-batch-record-mapping', NOW(), 'rt000006-batch-record-mapping', NOW(), b'0',
         route_process.`tenant_id`
    FROM `mes_pro_route_process` route_process
    JOIN `mes_pro_route_flow_config` route_flow_config
      ON route_flow_config.`tenant_id` = route_process.`tenant_id`
     AND route_flow_config.`route_id` = route_process.`route_id`
     AND route_flow_config.`use_type` = 'BATCH'
     AND route_flow_config.`deleted` = b'0'
    JOIN `mes_pro_process` process
      ON process.`id` = route_process.`process_id`
     AND process.`tenant_id` = route_process.`tenant_id`
     AND process.`deleted` = b'0'
    JOIN `mes_pro_batch_record_report` report
      ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
     AND report.`tenant_id` = route_process.`tenant_id`
     AND report.`deleted` = b'0'
    LEFT JOIN `mes_pro_route_flow_process_config` route_flow_process_config
      ON route_flow_process_config.`tenant_id` = route_process.`tenant_id`
     AND route_flow_process_config.`route_process_id` = route_process.`id`
     AND route_flow_process_config.`use_type` = 'BATCH'
     AND route_flow_process_config.`deleted` = b'0'
   WHERE route_process.`route_id` = @target_route_id
     AND route_process.`deleted` = b'0'
     AND route_flow_process_config.`id` IS NULL;

  UPDATE `mes_pro_route_flow_process_config` route_flow_process_config
    JOIN `mes_pro_route_process` route_process
      ON route_process.`id` = route_flow_process_config.`route_process_id`
     AND route_process.`tenant_id` = route_flow_process_config.`tenant_id`
     AND route_process.`deleted` = b'0'
    JOIN `mes_pro_process` process
      ON process.`id` = route_process.`process_id`
     AND process.`tenant_id` = route_process.`tenant_id`
     AND process.`deleted` = b'0'
    JOIN `mes_pro_batch_record_report` report
      ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
     AND report.`tenant_id` = route_process.`tenant_id`
     AND report.`deleted` = b'0'
     SET route_flow_process_config.`enabled` = b'1',
         route_flow_process_config.`batch_record_report_id` = report.`report_id`,
         route_flow_process_config.`updater` = 'rt000006-batch-record-mapping',
         route_flow_process_config.`update_time` = NOW()
   WHERE route_process.`route_id` = @target_route_id
     AND route_flow_process_config.`use_type` = 'BATCH'
     AND route_flow_process_config.`deleted` = b'0';

  INSERT INTO `mes_pro_route_flow_process_batch_record`
    (`route_flow_process_config_id`, `route_id`, `route_process_id`, `use_type`, `batch_record_report_id`,
     `form_slot_type`, `report_sort`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT route_flow_process_config.`id`, route_process.`route_id`, route_process.`id`, 'BATCH',
         report.`report_id`, 'MAIN', route_process.`sort`, 'RT000006 球囊扩张压力泵批记录表单配置映射',
         'rt000006-batch-record-mapping', NOW(), 'rt000006-batch-record-mapping', NOW(), b'0',
         route_process.`tenant_id`
    FROM `mes_pro_route_process` route_process
    JOIN `mes_pro_route_flow_process_config` route_flow_process_config
      ON route_flow_process_config.`tenant_id` = route_process.`tenant_id`
     AND route_flow_process_config.`route_process_id` = route_process.`id`
     AND route_flow_process_config.`use_type` = 'BATCH'
     AND route_flow_process_config.`deleted` = b'0'
    JOIN `mes_pro_process` process
      ON process.`id` = route_process.`process_id`
     AND process.`tenant_id` = route_process.`tenant_id`
     AND process.`deleted` = b'0'
    JOIN `mes_pro_batch_record_report` report
      ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
     AND report.`tenant_id` = route_process.`tenant_id`
     AND report.`deleted` = b'0'
    LEFT JOIN `mes_pro_route_flow_process_batch_record` batch_record
      ON batch_record.`tenant_id` = route_process.`tenant_id`
     AND batch_record.`route_process_id` = route_process.`id`
     AND batch_record.`use_type` = 'BATCH'
     AND batch_record.`batch_record_report_id` = report.`report_id`
     AND batch_record.`deleted` = b'0'
   WHERE route_process.`route_id` = @target_route_id
     AND route_process.`deleted` = b'0'
     AND batch_record.`id` IS NULL;

  INSERT INTO `mes_pro_edhr_process_form_permission_rule`
    (`route_process_id`, `batch_record_report_id`, `rule_type`, `signature_cell_key`, `signature_role`,
     `candidate_source_type`, `candidate_source_ids`, `completion_policy`, `due_minutes`, `enabled`,
     `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT route_process.`id`, report.`report_id`, role_seed.`rule_type`, '', NULL, 'ROLE',
         CAST(role.`id` AS CHAR), 'ANY_ONE', 0, b'1',
         CONCAT('RT000006 ', role_seed.`role_name`),
         'rt000006-batch-record-mapping', NOW(), 'rt000006-batch-record-mapping', NOW(), b'0',
         route_process.`tenant_id`
    FROM `mes_pro_route_process` route_process
    JOIN `mes_pro_process` process
      ON process.`id` = route_process.`process_id`
     AND process.`tenant_id` = route_process.`tenant_id`
     AND process.`deleted` = b'0'
    JOIN `mes_pro_batch_record_report` report
      ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
     AND report.`tenant_id` = route_process.`tenant_id`
     AND report.`deleted` = b'0'
    JOIN (
      SELECT 'FILL' AS `rule_type`, @production_role_name AS `role_name`
      UNION ALL SELECT 'QUALITY_FILL', @quality_role_name
      UNION ALL SELECT 'EQUIPMENT_FILL', @equipment_role_name
    ) role_seed
    JOIN `system_role` role
      ON role.`tenant_id` = route_process.`tenant_id`
     AND role.`name` COLLATE utf8mb4_unicode_ci = role_seed.`role_name` COLLATE utf8mb4_unicode_ci
     AND role.`deleted` = b'0'
     AND role.`status` = 0
    LEFT JOIN `mes_pro_edhr_process_form_permission_rule` permission_rule
      ON permission_rule.`tenant_id` = route_process.`tenant_id`
     AND permission_rule.`route_process_id` = route_process.`id`
     AND permission_rule.`batch_record_report_id` = report.`report_id`
     AND permission_rule.`rule_type` = role_seed.`rule_type`
     AND permission_rule.`signature_cell_key` = ''
     AND permission_rule.`deleted` = b'0'
   WHERE route_process.`route_id` = @target_route_id
     AND route_process.`deleted` = b'0'
     AND permission_rule.`id` IS NULL;

  UPDATE `mes_pro_edhr_process_form_permission_rule` permission_rule
    JOIN `mes_pro_route_process` route_process
      ON route_process.`id` = permission_rule.`route_process_id`
     AND route_process.`tenant_id` = permission_rule.`tenant_id`
     AND route_process.`route_id` = @target_route_id
     AND route_process.`deleted` = b'0'
    JOIN (
      SELECT 'FILL' AS `rule_type`, @production_role_name AS `role_name`
      UNION ALL SELECT 'QUALITY_FILL', @quality_role_name
      UNION ALL SELECT 'EQUIPMENT_FILL', @equipment_role_name
    ) role_seed
      ON role_seed.`rule_type` = permission_rule.`rule_type`
    JOIN `system_role` role
      ON role.`tenant_id` = permission_rule.`tenant_id`
     AND role.`name` COLLATE utf8mb4_unicode_ci = role_seed.`role_name` COLLATE utf8mb4_unicode_ci
     AND role.`deleted` = b'0'
     AND role.`status` = 0
     SET permission_rule.`candidate_source_type` = 'ROLE',
         permission_rule.`candidate_source_ids` = CAST(role.`id` AS CHAR),
         permission_rule.`enabled` = b'1',
         permission_rule.`updater` = 'rt000006-batch-record-mapping',
         permission_rule.`update_time` = NOW()
   WHERE permission_rule.`deleted` = b'0'
     AND permission_rule.`signature_cell_key` = ''
     AND permission_rule.`rule_type` IN ('FILL', 'QUALITY_FILL', 'EQUIPMENT_FILL');

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_route_process` route_process
      JOIN `mes_pro_process` process
        ON process.`id` = route_process.`process_id`
       AND process.`tenant_id` = route_process.`tenant_id`
       AND process.`deleted` = b'0'
      JOIN `mes_pro_batch_record_report` report
        ON process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci
       AND report.`tenant_id` = route_process.`tenant_id`
       AND report.`deleted` = b'0'
      LEFT JOIN `mes_pro_route_flow_process_batch_record` batch_record
        ON batch_record.`tenant_id` = route_process.`tenant_id`
       AND batch_record.`route_process_id` = route_process.`id`
       AND batch_record.`use_type` = 'BATCH'
       AND batch_record.`batch_record_report_id` = report.`report_id`
       AND batch_record.`deleted` = b'0'
      LEFT JOIN `mes_pro_edhr_process_form_permission_rule` production_rule
        ON production_rule.`tenant_id` = route_process.`tenant_id`
       AND production_rule.`route_process_id` = route_process.`id`
       AND production_rule.`batch_record_report_id` = report.`report_id`
       AND production_rule.`rule_type` = 'FILL'
       AND production_rule.`enabled` = b'1'
       AND production_rule.`deleted` = b'0'
      LEFT JOIN `mes_pro_edhr_process_form_permission_rule` quality_rule
        ON quality_rule.`tenant_id` = route_process.`tenant_id`
       AND quality_rule.`route_process_id` = route_process.`id`
       AND quality_rule.`batch_record_report_id` = report.`report_id`
       AND quality_rule.`rule_type` = 'QUALITY_FILL'
       AND quality_rule.`enabled` = b'1'
       AND quality_rule.`deleted` = b'0'
      LEFT JOIN `mes_pro_edhr_process_form_permission_rule` equipment_rule
        ON equipment_rule.`tenant_id` = route_process.`tenant_id`
       AND equipment_rule.`route_process_id` = route_process.`id`
       AND equipment_rule.`batch_record_report_id` = report.`report_id`
       AND equipment_rule.`rule_type` = 'EQUIPMENT_FILL'
       AND equipment_rule.`enabled` = b'1'
       AND equipment_rule.`deleted` = b'0'
     WHERE route_process.`route_id` = @target_route_id
       AND route_process.`deleted` = b'0'
       AND (
         batch_record.`id` IS NULL
         OR production_rule.`id` IS NULL
         OR quality_rule.`id` IS NULL
         OR equipment_rule.`id` IS NULL
       )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Incomplete RT000006 batch record mapping';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_rt000006_batch_record_mapping();
DROP PROCEDURE IF EXISTS ensure_mes_rt000006_batch_record_mapping;
