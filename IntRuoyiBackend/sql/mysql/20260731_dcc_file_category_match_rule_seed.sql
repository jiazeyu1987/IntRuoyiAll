-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260731_dcc_file_category_match_rule; type=seed; riskLevel=low
-- Seed explicit DCC category match rules for test-server OQ/PQ and component drawing classification gaps.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_dcc_file_category_match_rule_seed;

DELIMITER //
CREATE PROCEDURE apply_dcc_file_category_match_rule_seed()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_file_category_match_rule_seed`;
  CREATE TEMPORARY TABLE `tmp_dcc_file_category_match_rule_seed` (
    `category_name` VARCHAR(128) NOT NULL,
    `match_text` VARCHAR(255) NOT NULL,
    `match_type` VARCHAR(32) NOT NULL,
    `weight` INT NOT NULL,
    PRIMARY KEY (`category_name`, `match_text`, `match_type`)
  ) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO `tmp_dcc_file_category_match_rule_seed` (
    `category_name`, `match_text`, `match_type`, `weight`
  )
  VALUES
    ('过程运行确认（OQ）方案', 'OQ方案', 'CONTAINS', 1000),
    ('过程运行确认（OQ）方案', 'OQ验证方案', 'CONTAINS', 1000),
    ('过程运行确认（OQ）方案', 'OQPQ验证方案', 'CONTAINS', 1000),
    ('过程运行确认（OQ）报告', 'OQ报告', 'CONTAINS', 1000),
    ('过程运行确认（OQ）报告', 'OQ验证报告', 'CONTAINS', 1000),
    ('过程运行确认（OQ）报告', 'OQPQ验证报告', 'CONTAINS', 1000),
    ('过程性能确认（PQ）方案', 'PQ方案', 'CONTAINS', 1000),
    ('过程性能确认（PQ）方案', 'PQ验证方案', 'CONTAINS', 1000),
    ('过程性能确认（PQ）方案', '性能确认方案PQ', 'CONTAINS', 1000),
    ('过程性能确认（PQ）报告', 'PQ报告', 'CONTAINS', 1000),
    ('过程性能确认（PQ）报告', 'PQ验证报告', 'CONTAINS', 1000),
    ('过程性能确认（PQ）报告', '性能确认报告PQ', 'CONTAINS', 1000),
    ('零配件图纸', 'sldprt', 'EXTENSION', 800),
    ('零配件图纸', 'sldasm', 'EXTENSION', 800),
    ('零配件图纸', 'x_t', 'EXTENSION', 800),
    ('零配件图纸', 'step', 'EXTENSION', 800),
    ('零配件图纸', 'stp', 'EXTENSION', 800),
    ('零配件图纸', '零件图纸', 'CONTAINS', 500),
    ('零配件图纸', '组件图纸', 'CONTAINS', 500);

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT DISTINCT `category_name`
      FROM `tmp_dcc_file_category_match_rule_seed`
    ) expected
    LEFT JOIN `dcc_file_category` category
      ON category.`name` = expected.`category_name`
     AND category.`deleted` = 0
     AND category.`active` = 1
    WHERE category.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FILE_CATEGORY_MATCH_RULE_SEED_CATEGORY_MISSING';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT DISTINCT `category_name`
      FROM `tmp_dcc_file_category_match_rule_seed`
    ) expected
    JOIN `dcc_file_category` category
      ON category.`name` = expected.`category_name`
     AND category.`deleted` = 0
     AND category.`active` = 1
    GROUP BY category.`tenant_id`, expected.`category_name`
    HAVING COUNT(DISTINCT category.`id`) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FILE_CATEGORY_MATCH_RULE_SEED_CATEGORY_AMBIGUOUS';
  END IF;

  INSERT INTO `dcc_file_category_match_rule` (
    `category_id`, `match_text`, `match_type`, `weight`, `active`, `remark`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    category.`id`,
    seed.`match_text`,
    seed.`match_type`,
    seed.`weight`,
    1,
    'Seeded explicit project-code file category recognition rule',
    category.`tenant_id`,
    NOW(),
    NOW(),
    'migration',
    'migration',
    0
  FROM `dcc_file_category` category
  JOIN `tmp_dcc_file_category_match_rule_seed` seed ON seed.`category_name` = category.`name`
  WHERE category.`deleted` = 0
    AND category.`active` = 1
    AND NOT EXISTS (
      SELECT 1
      FROM `dcc_file_category_match_rule` existing
      WHERE existing.`tenant_id` = category.`tenant_id`
        AND existing.`category_id` = category.`id`
        AND existing.`match_text` = seed.`match_text`
        AND existing.`match_type` = seed.`match_type`
        AND existing.`deleted` = 0
    );

  IF EXISTS (
    SELECT 1
    FROM `tmp_dcc_file_category_match_rule_seed` seed
    JOIN `dcc_file_category` category
      ON seed.`category_name` = category.`name`
     AND category.`deleted` = 0
     AND category.`active` = 1
    LEFT JOIN `dcc_file_category_match_rule` rule_record
      ON rule_record.`tenant_id` = category.`tenant_id`
     AND rule_record.`category_id` = category.`id`
     AND rule_record.`match_text` = seed.`match_text`
     AND rule_record.`match_type` = seed.`match_type`
     AND rule_record.`deleted` = 0
    WHERE rule_record.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FILE_CATEGORY_MATCH_RULE_SEED_INSERT_INCOMPLETE';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_file_category_match_rule_seed`;
END//
DELIMITER ;

CALL apply_dcc_file_category_match_rule_seed();

DROP PROCEDURE IF EXISTS apply_dcc_file_category_match_rule_seed;
