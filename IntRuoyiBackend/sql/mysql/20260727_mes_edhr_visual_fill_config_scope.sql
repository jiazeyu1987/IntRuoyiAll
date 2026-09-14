-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260723_mes_edhr_process_form_permission_rule_version_index_repair; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_visual_fill_config_scope;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_visual_fill_config_scope()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES visual fill config requires mes_pro_edhr_process_form_permission_rule';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND column_name = 'scope_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_process_form_permission_rule`
      ADD COLUMN `scope_key` varchar(64) NOT NULL DEFAULT 'ALL' COMMENT 'Responsibility scope key: ALL or assist row rowKey'
      AFTER `rule_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND column_name = 'fillable_scope_json'
  ) THEN
    ALTER TABLE `mes_pro_edhr_process_form_permission_rule`
      ADD COLUMN `fillable_scope_json` json DEFAULT NULL COMMENT 'Precise fillable cell scope json'
      AFTER `enabled`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'jimu_report'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES visual fill config migration requires jimu_report';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'jimu_report'
      AND column_name = 'json_str'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES visual fill config migration requires jimu_report.json_str';
  END IF;

  SET @missing_version_rule_id := (
    SELECT `rule`.`id`
    FROM `mes_pro_edhr_process_form_permission_rule` AS `rule`
    WHERE `rule`.`deleted` = b'0'
      AND `rule`.`rule_type` = 'FILL'
      AND COALESCE(NULLIF(`rule`.`scope_key`, ''), 'ALL') = 'ALL'
      AND `rule`.`fillable_scope_json` IS NULL
      AND `rule`.`batch_record_version_id` IS NULL
    ORDER BY `rule`.`id`
    LIMIT 1
  );
  IF @missing_version_rule_id IS NOT NULL THEN
    SET @missing_version_message = CONCAT(
      'MES visual fill config migration requires batch_record_version_id for process form rule id=',
      @missing_version_rule_id
    );
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = @missing_version_message;
  END IF;

  SET @invalid_report_json_rule_id := (
    SELECT `rule`.`id`
    FROM `mes_pro_edhr_process_form_permission_rule` AS `rule`
    JOIN `mes_pro_batch_record_report` AS `report`
      ON `report`.`tenant_id` = `rule`.`tenant_id`
     AND `report`.`deleted` = b'0'
     AND `report`.`report_id` = `rule`.`batch_record_report_id`
     AND `report`.`batch_record_version_id` = `rule`.`batch_record_version_id`
    JOIN `jimu_report` AS `jimu`
      ON `jimu`.`id` = `report`.`report_id`
     AND COALESCE(`jimu`.`del_flag`, 0) = 0
    WHERE `rule`.`deleted` = b'0'
      AND `rule`.`rule_type` = 'FILL'
      AND COALESCE(NULLIF(`rule`.`scope_key`, ''), 'ALL') = 'ALL'
      AND `rule`.`fillable_scope_json` IS NULL
      AND NOT JSON_VALID(`jimu`.`json_str`)
    ORDER BY `rule`.`id`
    LIMIT 1
  );
  IF @invalid_report_json_rule_id IS NOT NULL THEN
    SET @missing_scope_message = CONCAT(
      'MES visual fill config migration requires valid Jimu json_str for process form rule id=',
      @invalid_report_json_rule_id
    );
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = @missing_scope_message;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_visual_fill_config_scope_cells`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_visual_fill_config_scope_cells` (
    `rule_id` bigint NOT NULL,
    `source_table_index` int NOT NULL,
    `row_index` int NOT NULL,
    `column_index` int NOT NULL,
    PRIMARY KEY (`rule_id`, `source_table_index`, `row_index`, `column_index`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_visual_fill_config_scope_cells` (
    `rule_id`,
    `source_table_index`,
    `row_index`,
    `column_index`
  )
  SELECT
    `rule`.`id`,
    COALESCE(`report`.`source_table_index`, 0) AS `source_table_index`,
    `cell_rule`.`row_index`,
    `cell_rule`.`column_index`
  FROM `mes_pro_edhr_process_form_permission_rule` AS `rule`
  JOIN `mes_pro_batch_record_report` AS `report`
    ON `report`.`tenant_id` = `rule`.`tenant_id`
   AND `report`.`deleted` = b'0'
   AND `report`.`report_id` = `rule`.`batch_record_report_id`
   AND `report`.`batch_record_version_id` = `rule`.`batch_record_version_id`
  JOIN `jimu_report` AS `jimu`
    ON `jimu`.`id` = `report`.`report_id`
   AND COALESCE(`jimu`.`del_flag`, 0) = 0
   AND JSON_VALID(`jimu`.`json_str`)
  JOIN JSON_TABLE(
    CAST(`jimu`.`json_str` AS JSON),
    '$.rows.*.cells.*' COLUMNS (
      `row_index` int PATH '$.edhrCellRule.rowIndex' NULL ON EMPTY NULL ON ERROR,
      `column_index` int PATH '$.edhrCellRule.columnIndex' NULL ON EMPTY NULL ON ERROR,
      `value_type` varchar(32) PATH '$.edhrCellRule.valueType' NULL ON EMPTY NULL ON ERROR,
      `reviewed` varchar(8) PATH '$.edhrCellRule.reviewed' NULL ON EMPTY NULL ON ERROR
    )
  ) AS `cell_rule`
  WHERE `rule`.`deleted` = b'0'
    AND `rule`.`rule_type` = 'FILL'
    AND COALESCE(NULLIF(`rule`.`scope_key`, ''), 'ALL') = 'ALL'
    AND `rule`.`fillable_scope_json` IS NULL
    AND `cell_rule`.`row_index` IS NOT NULL
    AND `cell_rule`.`column_index` IS NOT NULL
    AND `cell_rule`.`value_type` IS NOT NULL
    AND LOWER(COALESCE(`cell_rule`.`reviewed`, 'false')) = 'true';

  SET @missing_scope_rule_id := (
    SELECT `rule`.`id`
    FROM `mes_pro_edhr_process_form_permission_rule` AS `rule`
    LEFT JOIN `tmp_mes_edhr_visual_fill_config_scope_cells` AS `cell`
      ON `cell`.`rule_id` = `rule`.`id`
    WHERE `rule`.`deleted` = b'0'
      AND `rule`.`rule_type` = 'FILL'
      AND COALESCE(NULLIF(`rule`.`scope_key`, ''), 'ALL') = 'ALL'
      AND `rule`.`fillable_scope_json` IS NULL
    GROUP BY `rule`.`id`
    HAVING COUNT(`cell`.`rule_id`) = 0
    ORDER BY `rule`.`id`
    LIMIT 1
  );
  IF @missing_scope_rule_id IS NOT NULL THEN
    SET @missing_scope_message = CONCAT(
      'MES visual fill config migration requires reviewed edhrCellRule cells for process form rule id=',
      @missing_scope_rule_id
    );
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = @missing_scope_message;
  END IF;

  UPDATE `mes_pro_edhr_process_form_permission_rule` AS `rule`
  JOIN (
    SELECT
      `cell`.`rule_id`,
      JSON_OBJECT('schemaVersion', 2, 'cells',
        JSON_ARRAYAGG(JSON_OBJECT(
          'sourceTableIndex', `cell`.`source_table_index`,
          'rowIndex', `cell`.`row_index`,
          'columnIndex', `cell`.`column_index`
        ))
      ) AS `scope_json`
    FROM `tmp_mes_edhr_visual_fill_config_scope_cells` AS `cell`
    GROUP BY `cell`.`rule_id`
  ) AS `scope`
    ON `scope`.`rule_id` = `rule`.`id`
  SET `scope_key` = 'ALL',
      `rule`.`fillable_scope_json` = `scope`.`scope_json`
  WHERE `rule`.`deleted` = b'0'
    AND `rule`.`rule_type` = 'FILL'
    AND COALESCE(NULLIF(`rule`.`scope_key`, ''), 'ALL') = 'ALL'
    AND `rule`.`fillable_scope_json` IS NULL;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_visual_fill_config_scope_cells`;

  IF EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND index_name = 'uk_mes_pro_edhr_process_form_rule'
  ) THEN
    ALTER TABLE `mes_pro_edhr_process_form_permission_rule`
      DROP INDEX `uk_mes_pro_edhr_process_form_rule`;
  END IF;

  ALTER TABLE `mes_pro_edhr_process_form_permission_rule`
    ADD UNIQUE KEY `uk_mes_pro_edhr_process_form_rule` (
      `tenant_id`,
      `route_process_id`,
      `batch_record_report_id`,
      `batch_record_version_id`,
      `rule_type`,
      `scope_key`,
      `signature_cell_key`,
      `deleted`
    );
END//
DELIMITER ;

CALL ensure_mes_edhr_visual_fill_config_scope();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_visual_fill_config_scope;
