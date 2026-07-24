-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Seed real DCC template categories used by NAS transfer when users choose "其他".
-- Safe to run repeatedly: for each enabled real tenant with one active "产品技术要求", create or update
-- one active "其他" category and insert only missing governance rows copied from that tenant's source template.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS seed_dcc_other_template_category;
DELIMITER $$
CREATE PROCEDURE seed_dcc_other_template_category()
BEGIN
  DECLARE source_total INT DEFAULT 0;
  DECLARE duplicate_source_count INT DEFAULT 0;
  DECLARE missing_route_count INT DEFAULT 0;
  DECLARE duplicate_other_count INT DEFAULT 0;
  DECLARE mapping_count INT DEFAULT 0;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_other_template_source;
  CREATE TEMPORARY TABLE tmp_dcc_other_template_source AS
  SELECT
    source_category.`tenant_id`,
    MIN(source_category.`id`) AS `source_category_id`,
    COUNT(*) AS `source_count`
  FROM `dcc_file_category` source_category
  JOIN `system_tenant` tenant
    ON tenant.`id` = source_category.`tenant_id`
   AND tenant.`status` = 0
   AND tenant.`deleted` = 0
  WHERE source_category.`name` = '产品技术要求'
    AND source_category.`active` = 1
    AND source_category.`deleted` = 0
  GROUP BY source_category.`tenant_id`;

  SELECT COUNT(*)
    INTO source_total
    FROM tmp_dcc_other_template_source;

  IF source_total = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing active DCC source template category: 产品技术要求';
  END IF;

  SELECT COUNT(*)
    INTO duplicate_source_count
    FROM tmp_dcc_other_template_source
   WHERE `source_count` <> 1;

  IF duplicate_source_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ambiguous active DCC source template category: 产品技术要求';
  END IF;

  SELECT COUNT(*)
    INTO missing_route_count
    FROM tmp_dcc_other_template_source source
   WHERE NOT EXISTS (
     SELECT 1
       FROM `dcc_category_approval_route` route
      WHERE route.`category_id` = source.`source_category_id`
        AND route.`active` = 1
        AND route.`deleted` = 0
   );

  IF missing_route_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing active DCC source approval route: 产品技术要求';
  END IF;

  SELECT COUNT(*)
    INTO duplicate_other_count
    FROM (
      SELECT source.`tenant_id`, COUNT(target.`id`) AS `target_count`
        FROM tmp_dcc_other_template_source source
        LEFT JOIN `dcc_file_category` target
          ON target.`tenant_id` = source.`tenant_id`
         AND target.`name` = '其他'
         AND target.`active` = 1
         AND target.`deleted` = 0
       GROUP BY source.`tenant_id`
      HAVING COUNT(target.`id`) > 1
    ) duplicate_target;

  IF duplicate_other_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ambiguous active DCC target template category: 其他';
  END IF;

  INSERT INTO `dcc_file_category` (
    `code`, `name`, `parent_id`, `active`, `sort`, `source`, `remark`, `description`,
    `distribution_required`, `training_required`, `tenant_id`, `create_time`, `update_time`,
    `creator`, `updater`, `deleted`
  )
  SELECT
    CONCAT('DCC_OTHER_TEMPLATE_', source_category.`id`),
    '其他',
    NULL,
    1,
    COALESCE((
      SELECT MAX(existing.`sort`) + 1
        FROM `dcc_file_category` existing
       WHERE existing.`tenant_id` = source_category.`tenant_id`
    ), 1),
    'LOCAL',
    'Created for NAS transfer from 产品技术要求 template',
    source_category.`description`,
    source_category.`distribution_required`,
    source_category.`training_required`,
    source_category.`tenant_id`,
    NOW(),
    NOW(),
    'dcc-other-template-seed',
    'dcc-other-template-seed',
    0
  FROM tmp_dcc_other_template_source source
  JOIN `dcc_file_category` source_category
    ON source_category.`id` = source.`source_category_id`
  WHERE NOT EXISTS (
    SELECT 1
      FROM `dcc_file_category` target
     WHERE target.`tenant_id` = source_category.`tenant_id`
       AND target.`name` = '其他'
       AND target.`active` = 1
       AND target.`deleted` = 0
  )
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `parent_id` = VALUES(`parent_id`),
    `active` = VALUES(`active`),
    `description` = VALUES(`description`),
    `distribution_required` = VALUES(`distribution_required`),
    `training_required` = VALUES(`training_required`),
    `tenant_id` = VALUES(`tenant_id`),
    `deleted` = 0,
    `update_time` = NOW(),
    `updater` = 'dcc-other-template-seed';

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_other_template_mapping;
  CREATE TEMPORARY TABLE tmp_dcc_other_template_mapping AS
  SELECT
    source.`source_category_id`,
    target.`id` AS `other_category_id`
  FROM tmp_dcc_other_template_source source
  JOIN `dcc_file_category` target
    ON target.`tenant_id` = source.`tenant_id`
   AND target.`name` = '其他'
   AND target.`active` = 1
   AND target.`deleted` = 0;

  SELECT COUNT(*)
    INTO mapping_count
    FROM tmp_dcc_other_template_mapping;

  IF mapping_count <> source_total THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Failed to resolve DCC target template category: 其他';
  END IF;

  UPDATE `dcc_file_category` target
  JOIN tmp_dcc_other_template_mapping mapping
    ON mapping.`other_category_id` = target.`id`
  JOIN `dcc_file_category` source
    ON source.`id` = mapping.`source_category_id`
     SET target.`parent_id` = NULL,
         target.`active` = 1,
         target.`description` = source.`description`,
         target.`distribution_required` = source.`distribution_required`,
         target.`training_required` = source.`training_required`,
         target.`tenant_id` = source.`tenant_id`,
         target.`deleted` = 0,
         target.`update_time` = NOW(),
         target.`updater` = 'dcc-other-template-seed';

  INSERT INTO `dcc_file_category_permission_rule` (
    `category_id`, `action_type`, `subject_type`, `subject_id`, `active`, `remark`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    mapping.`other_category_id`, source.`action_type`, source.`subject_type`, source.`subject_id`,
    source.`active`, source.`remark`, source.`tenant_id`, NOW(), NOW(),
    'dcc-other-template-seed', 'dcc-other-template-seed', 0
  FROM `dcc_file_category_permission_rule` source
  JOIN tmp_dcc_other_template_mapping mapping
    ON mapping.`source_category_id` = source.`category_id`
  WHERE source.`deleted` = 0
  ON DUPLICATE KEY UPDATE
    `active` = VALUES(`active`),
    `remark` = VALUES(`remark`),
    `tenant_id` = VALUES(`tenant_id`),
    `deleted` = 0,
    `update_time` = NOW(),
    `updater` = 'dcc-other-template-seed';

  INSERT INTO `dcc_file_category_distribution_rule` (
    `category_id`, `department_id`, `distribution_medium`, `active`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    mapping.`other_category_id`, source.`department_id`, source.`distribution_medium`, source.`active`,
    source.`tenant_id`, NOW(), NOW(), 'dcc-other-template-seed', 'dcc-other-template-seed', 0
  FROM `dcc_file_category_distribution_rule` source
  JOIN tmp_dcc_other_template_mapping mapping
    ON mapping.`source_category_id` = source.`category_id`
  WHERE source.`deleted` = 0
  ON DUPLICATE KEY UPDATE
    `distribution_medium` = VALUES(`distribution_medium`),
    `active` = VALUES(`active`),
    `tenant_id` = VALUES(`tenant_id`),
    `deleted` = 0,
    `update_time` = NOW(),
    `updater` = 'dcc-other-template-seed';

  INSERT INTO `dcc_file_category_training_rule` (
    `category_id`, `department_id`, `active`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    mapping.`other_category_id`, source.`department_id`, source.`active`,
    source.`tenant_id`, NOW(), NOW(), 'dcc-other-template-seed', 'dcc-other-template-seed', 0
  FROM `dcc_file_category_training_rule` source
  JOIN tmp_dcc_other_template_mapping mapping
    ON mapping.`source_category_id` = source.`category_id`
  WHERE source.`deleted` = 0
  ON DUPLICATE KEY UPDATE
    `active` = VALUES(`active`),
    `tenant_id` = VALUES(`tenant_id`),
    `deleted` = 0,
    `update_time` = NOW(),
    `updater` = 'dcc-other-template-seed';

  INSERT INTO `dcc_category_approval_route` (
    `category_id`, `version_no`, `active`, `effective_time`, `remark`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    mapping.`other_category_id`, source.`version_no`, source.`active`, source.`effective_time`, source.`remark`,
    source.`tenant_id`, NOW(), NOW(), 'dcc-other-template-seed', 'dcc-other-template-seed', 0
  FROM `dcc_category_approval_route` source
  JOIN tmp_dcc_other_template_mapping mapping
    ON mapping.`source_category_id` = source.`category_id`
  WHERE source.`deleted` = 0
  ON DUPLICATE KEY UPDATE
    `active` = VALUES(`active`),
    `effective_time` = VALUES(`effective_time`),
    `remark` = VALUES(`remark`),
    `tenant_id` = VALUES(`tenant_id`),
    `deleted` = 0,
    `update_time` = NOW(),
    `updater` = 'dcc-other-template-seed';

  UPDATE `dcc_category_approval_route_node` target_node
  JOIN `dcc_category_approval_route` target_route
    ON target_route.`id` = target_node.`route_id`
  JOIN tmp_dcc_other_template_mapping mapping
    ON mapping.`other_category_id` = target_route.`category_id`
  JOIN `dcc_category_approval_route` source_route
    ON source_route.`category_id` = mapping.`source_category_id`
   AND source_route.`version_no` = target_route.`version_no`
  JOIN `dcc_category_approval_route_node` source_node
    ON source_node.`route_id` = source_route.`id`
   AND source_node.`stage_no` = target_node.`stage_no`
   AND COALESCE(source_node.`stage_code`, '') = COALESCE(target_node.`stage_code`, '')
     SET target_node.`stage_name` = source_node.`stage_name`,
         target_node.`stage_order` = source_node.`stage_order`,
         target_node.`candidate_source_type` = source_node.`candidate_source_type`,
         target_node.`candidate_source_id` = source_node.`candidate_source_id`,
         target_node.`candidate_source_ids` = source_node.`candidate_source_ids`,
         target_node.`approve_method` = source_node.`approve_method`,
         target_node.`approve_ratio` = source_node.`approve_ratio`,
         target_node.`require_all_approvals` = source_node.`require_all_approvals`,
         target_node.`required` = source_node.`required`,
         target_node.`sort` = source_node.`sort`,
         target_node.`tenant_id` = source_node.`tenant_id`,
         target_node.`deleted` = 0,
         target_node.`update_time` = NOW(),
         target_node.`updater` = 'dcc-other-template-seed'
   WHERE source_route.`deleted` = 0
     AND source_node.`deleted` = 0;

  INSERT INTO `dcc_category_approval_route_node` (
    `route_id`, `stage_no`, `stage_code`, `stage_name`, `stage_order`,
    `candidate_source_type`, `candidate_source_id`, `candidate_source_ids`,
    `approve_method`, `approve_ratio`, `require_all_approvals`, `required`, `sort`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    target_route.`id`, source_node.`stage_no`, source_node.`stage_code`, source_node.`stage_name`,
    source_node.`stage_order`, source_node.`candidate_source_type`, source_node.`candidate_source_id`,
    source_node.`candidate_source_ids`, source_node.`approve_method`, source_node.`approve_ratio`,
    source_node.`require_all_approvals`, source_node.`required`, source_node.`sort`,
    source_node.`tenant_id`, NOW(), NOW(), 'dcc-other-template-seed', 'dcc-other-template-seed', 0
  FROM `dcc_category_approval_route_node` source_node
  JOIN `dcc_category_approval_route` source_route
    ON source_route.`id` = source_node.`route_id`
  JOIN tmp_dcc_other_template_mapping mapping
    ON mapping.`source_category_id` = source_route.`category_id`
  JOIN `dcc_category_approval_route` target_route
    ON target_route.`category_id` = mapping.`other_category_id`
   AND target_route.`version_no` = source_route.`version_no`
  WHERE source_route.`deleted` = 0
    AND source_node.`deleted` = 0
    AND NOT EXISTS (
      SELECT 1
      FROM `dcc_category_approval_route_node` target_node
      WHERE target_node.`route_id` = target_route.`id`
        AND target_node.`stage_no` = source_node.`stage_no`
        AND COALESCE(target_node.`stage_code`, '') = COALESCE(source_node.`stage_code`, '')
    );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_other_template_mapping;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_other_template_source;
END$$
DELIMITER ;

CALL seed_dcc_other_template_category();

DROP PROCEDURE IF EXISTS seed_dcc_other_template_category;
