-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260722_mes_route_form_center_runtime_columns,20260829_mes_form_center_unified_import_menu; type=data; riskLevel=high
-- Switch old BPM form-template route bindings to MES form-center report bindings.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `migrate_mes_old_form_template_bindings_to_form_center`;
DELIMITER $$
CREATE PROCEDURE `migrate_mes_old_form_template_bindings_to_form_center`()
BEGIN
  DECLARE v_scope_count BIGINT DEFAULT 0;
  DECLARE v_snapshot_scope_count BIGINT DEFAULT 0;

  SET SESSION group_concat_max_len = 16777216;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_form_template_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'bpm_form_template_version is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_flow_process_batch_record is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_version is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_definition'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_definition is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_version is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_report'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_report is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'jimu_report_category'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'jimu_report_category is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'jimu_report'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'jimu_report is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_form_template_version'
      AND COLUMN_NAME = 'batch_record_report_id'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'bpm_form_template_version.batch_record_report_id is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'batch_record_report_id'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_flow_process_batch_record.batch_record_report_id is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_version'
      AND COLUMN_NAME = 'route_snapshot_json'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_version.route_snapshot_json is missing';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_binding_scope`;
  CREATE TEMPORARY TABLE `tmp_mes_old_form_template_binding_scope` AS
  SELECT
    rb.`id` AS `route_binding_id`,
    rb.`tenant_id`,
    rb.`route_id`,
    rb.`route_process_id`,
    rb.`use_type`,
    rb.`form_slot_type`,
    rb.`form_binding_key` AS `old_form_binding_key`,
    rb.`global_sync_key` AS `old_global_sync_key`,
    rb.`form_template_id` AS `old_form_template_id`,
    rb.`last_published_template_version_id` AS `old_template_version_id`,
    tv.`id` AS `resolved_template_version_id`,
    tv.`template_name`,
    tv.`version_no`,
    tv.`status` AS `template_version_status`,
    tv.`source_file_name`,
    tv.`source_file_content`,
    tv.`jimu_schema_json`,
    LOWER(SHA2(FROM_BASE64(tv.`source_file_content`), 256)) COLLATE utf8mb4_unicode_ci AS `source_file_sha256`,
    CONCAT('FORMTPL:', tv.`id`) COLLATE utf8mb4_unicode_ci AS `report_id`,
    CONCAT('FORMTPL_', tv.`id`) COLLATE utf8mb4_unicode_ci AS `report_code`,
    CONCAT(tv.`template_name`, ' ', tv.`version_no`) COLLATE utf8mb4_unicode_ci AS `report_name`,
    CONCAT('FORMTPLV_', tv.`id`, '_TN', rb.`tenant_id`) COLLATE utf8mb4_unicode_ci AS `sample_key`,
    cat.`id` COLLATE utf8mb4_unicode_ci AS `report_category_id`
  FROM `mes_pro_route_flow_process_batch_record` rb
  LEFT JOIN `bpm_form_template_version` tv
    ON tv.`tenant_id` = rb.`tenant_id`
   AND tv.`template_id` = rb.`form_template_id`
   AND tv.`id` = rb.`last_published_template_version_id`
   AND tv.`deleted` = b'0'
  LEFT JOIN `jimu_report_category` cat
    ON CAST(cat.`tenant_id` AS UNSIGNED) = rb.`tenant_id`
   AND cat.`del_flag` = 0
   AND cat.`name` COLLATE utf8mb4_unicode_ci = CONVERT(UNHEX('E794B5E5AD90E689B9E8AEB0E5BD95') USING utf8mb4) COLLATE utf8mb4_unicode_ci
  WHERE rb.`deleted` = b'0'
    AND rb.`form_template_id` IS NOT NULL
    AND rb.`batch_record_report_id` IS NULL;

  SELECT COUNT(*) INTO v_scope_count
  FROM `tmp_mes_old_form_template_binding_scope`;

  IF v_scope_count > 0 THEN
    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE `resolved_template_version_id` IS NULL
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Old form template published version is missing';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE `template_version_status` <> 'PUBLISHED'
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Old form template version is not published';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE `form_slot_type` NOT IN ('FORM', 'LOSS_REPORT', 'PROCESS_INSPECTION', 'PARAMETER_RECORD')
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Old form template slot type is invalid';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE `template_name` IS NULL
         OR `template_name` = ''
         OR CHAR_LENGTH(`template_name`) > 100
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Old form template name is missing or too long';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE `version_no` IS NULL
         OR `version_no` = ''
         OR CHAR_LENGTH(`version_no`) > 32
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Old form template version number is missing or too long';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE `source_file_content` IS NULL
         OR `source_file_content` = ''
         OR `source_file_sha256` IS NULL
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Source form template content is missing';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE IFNULL(JSON_VALID(`jimu_schema_json`), 0) <> 1
         OR JSON_EXTRACT(`jimu_schema_json`, '$.sheetLayoutJson') IS NULL
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Form template Jimu schema is invalid';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE `report_category_id` IS NULL
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Electronic batch record report category is missing';
    END IF;

    IF EXISTS (
      SELECT 1 FROM `tmp_mes_old_form_template_binding_scope`
      WHERE CHAR_LENGTH(`report_id`) > 32
         OR CHAR_LENGTH(`report_code`) > 50
         OR CHAR_LENGTH(`report_name`) > 100
         OR CHAR_LENGTH(`sample_key`) > 64
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Target FORM_TEMPLATE report identity is too long';
    END IF;

    IF EXISTS (
      SELECT 1
      FROM (
        SELECT `tenant_id`, `old_template_version_id`
        FROM `tmp_mes_old_form_template_binding_scope`
        GROUP BY `tenant_id`, `old_template_version_id`
        HAVING COUNT(DISTINCT `form_slot_type`) > 1
      ) conflict_scope
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Old form template version maps to multiple slot types';
    END IF;

    START TRANSACTION;

    INSERT IGNORE INTO `mes_pro_batch_record_definition` (
      `tenant_id`,
      `batch_record_name`,
      `route_key`,
      `current_version_id`,
      `remark`,
      `creator`,
      `create_time`,
      `updater`,
      `update_time`,
      `deleted`
    )
    SELECT DISTINCT
      s.`tenant_id`,
      s.`template_name`,
      s.`form_slot_type`,
      NULL,
      CONCAT('Migrated from old form template ', s.`old_form_template_id`),
      'codex',
      NOW(),
      'codex',
      NOW(),
      b'0'
    FROM `tmp_mes_old_form_template_binding_scope` s;

    INSERT IGNORE INTO `mes_pro_batch_record_version` (
      `tenant_id`,
      `definition_id`,
      `version_no`,
      `status`,
      `source_version_id`,
      `source_file_name`,
      `source_file_sha256`,
      `route_id`,
      `source_route_id`,
      `approval_instance_id`,
      `submitted_by`,
      `submitted_at`,
      `approved_by`,
      `approved_at`,
      `reject_reason`,
      `remark`,
      `creator`,
      `create_time`,
      `updater`,
      `update_time`,
      `deleted`,
      `child_form_member_count`,
      `child_form_member_hash`
    )
    SELECT DISTINCT
      s.`tenant_id`,
      d.`id`,
      s.`version_no`,
      'APPROVED',
      NULL,
      s.`source_file_name`,
      s.`source_file_sha256`,
      NULL,
      NULL,
      NULL,
      NULL,
      NULL,
      NULL,
      NOW(),
      NULL,
      CONCAT('Migrated from old form template version ', s.`old_template_version_id`),
      'codex',
      NOW(),
      'codex',
      NOW(),
      b'0',
      0,
      NULL
    FROM `tmp_mes_old_form_template_binding_scope` s
    JOIN `mes_pro_batch_record_definition` d
      ON d.`tenant_id` = s.`tenant_id`
     AND d.`batch_record_name` = s.`template_name`
     AND d.`route_key` = s.`form_slot_type`
     AND d.`deleted` = b'0';

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_binding_map`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_binding_map` AS
    SELECT
      s.*,
      d.`id` AS `definition_id`,
      v.`id` AS `version_id`
    FROM `tmp_mes_old_form_template_binding_scope` s
    JOIN `mes_pro_batch_record_definition` d
      ON d.`tenant_id` = s.`tenant_id`
     AND d.`batch_record_name` = s.`template_name`
     AND d.`route_key` = s.`form_slot_type`
     AND d.`deleted` = b'0'
    JOIN `mes_pro_batch_record_version` v
      ON v.`tenant_id` = s.`tenant_id`
     AND v.`definition_id` = d.`id`
     AND v.`version_no` = s.`version_no`
     AND v.`deleted` = b'0';

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_binding_scope` s
      LEFT JOIN `tmp_mes_old_form_template_binding_map` m
        ON m.`route_binding_id` = s.`route_binding_id`
      WHERE m.`route_binding_id` IS NULL
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Target batch record definition or version is missing';
    END IF;

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_binding_map` m
      JOIN `mes_pro_batch_record_version` v
        ON v.`id` = m.`version_id`
      WHERE v.`status` <> 'APPROVED'
         OR v.`source_file_sha256` <> m.`source_file_sha256`
         OR v.`source_file_name` <> m.`source_file_name`
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Target batch record version conflicts with old form template source';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_definition_ids`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_definition_ids` AS
    SELECT DISTINCT `definition_id`
    FROM `tmp_mes_old_form_template_binding_map`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_definition_versions`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_definition_versions` AS
    SELECT DISTINCT `definition_id`, `version_id`
    FROM `tmp_mes_old_form_template_binding_map`;

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_definition_ids` mapped_definition
      JOIN `mes_pro_batch_record_definition` d
        ON d.`id` = mapped_definition.`definition_id`
      LEFT JOIN `tmp_mes_old_form_template_definition_versions` mapped_version
        ON mapped_version.`definition_id` = d.`id`
       AND mapped_version.`version_id` = d.`current_version_id`
      WHERE d.`current_version_id` IS NOT NULL
        AND mapped_version.`version_id` IS NULL
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Target batch record definition current version conflicts';
    END IF;

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_binding_map` m
      JOIN `mes_pro_batch_record_report` r
        ON r.`report_id` = m.`report_id`
      WHERE NOT (
        r.`deleted` = b'0'
        AND r.`tenant_id` = m.`tenant_id`
        AND r.`batch_record_definition_id` = m.`definition_id`
        AND r.`batch_record_version_id` = m.`version_id`
        AND r.`batch_record_name` = m.`template_name`
        AND r.`form_slot_type` = m.`form_slot_type`
        AND r.`route_key` = m.`form_slot_type`
        AND r.`source_file_sha256` = m.`source_file_sha256`
        AND r.`source_table_index` = 1
      )
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Target FORM_TEMPLATE report id conflicts';
    END IF;

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_binding_map` m
      JOIN `mes_pro_batch_record_report` r
        ON r.`sample_key` = m.`sample_key`
       AND r.`form_slot_type` = m.`form_slot_type`
       AND r.`route_key` = m.`form_slot_type`
       AND r.`source_table_index` = 1
       AND r.`deleted` = b'0'
      WHERE r.`report_id` <> m.`report_id`
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Target FORM_TEMPLATE sample key conflicts';
    END IF;

    INSERT INTO `mes_pro_batch_record_report` (
      `tenant_id`,
      `sample_key`,
      `batch_record_name`,
      `product_name`,
      `form_slot_type`,
      `route_key`,
      `source_file_name`,
      `source_file_sha256`,
      `source_table_index`,
      `table_title`,
      `report_id`,
      `report_code`,
      `report_name`,
      `report_category_id`,
      `last_import_time`,
      `creator`,
      `create_time`,
      `updater`,
      `update_time`,
      `deleted`,
      `batch_record_definition_id`,
      `batch_record_version_id`,
      `form_definition_id`,
      `form_version_id`
    )
    SELECT DISTINCT
      m.`tenant_id`,
      m.`sample_key`,
      m.`template_name`,
      m.`template_name`,
      m.`form_slot_type`,
      m.`form_slot_type`,
      m.`source_file_name`,
      m.`source_file_sha256`,
      1,
      m.`template_name`,
      m.`report_id`,
      m.`report_code`,
      m.`report_name`,
      m.`report_category_id`,
      NOW(),
      'codex',
      NOW(),
      'codex',
      NOW(),
      b'0',
      m.`definition_id`,
      m.`version_id`,
      NULL,
      NULL
    FROM `tmp_mes_old_form_template_binding_map` m
    WHERE NOT EXISTS (
      SELECT 1 FROM `mes_pro_batch_record_report` r
      WHERE r.`report_id` = m.`report_id`
    );

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_latest_old`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_latest_old` AS
    SELECT `definition_id`, MAX(`old_template_version_id`) AS `latest_old_template_version_id`
    FROM `tmp_mes_old_form_template_binding_map`
    GROUP BY `definition_id`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_latest_version`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_latest_version` AS
    SELECT m.`definition_id`, m.`version_id`
    FROM `tmp_mes_old_form_template_binding_map` m
    JOIN `tmp_mes_old_form_template_latest_old` latest
      ON latest.`definition_id` = m.`definition_id`
     AND latest.`latest_old_template_version_id` = m.`old_template_version_id`
    GROUP BY m.`definition_id`, m.`version_id`;

    UPDATE `mes_pro_batch_record_definition` d
    JOIN `tmp_mes_old_form_template_latest_version` latest_version
      ON latest_version.`definition_id` = d.`id`
    SET d.`current_version_id` = latest_version.`version_id`,
        d.`updater` = 'codex',
        d.`update_time` = NOW()
    WHERE d.`deleted` = b'0';

    UPDATE `bpm_form_template_version` tv
    JOIN (
      SELECT DISTINCT
        `tenant_id`,
        `old_template_version_id`,
        `report_id`,
        `report_name`,
        `template_name`,
        `version_no`,
        `form_slot_type`
      FROM `tmp_mes_old_form_template_binding_map`
    ) m
      ON m.`tenant_id` = tv.`tenant_id`
     AND m.`old_template_version_id` = tv.`id`
    SET tv.`batch_record_report_id` = m.`report_id`,
        tv.`batch_record_report_name` = m.`report_name`,
        tv.`batch_record_name` = m.`template_name`,
        tv.`batch_record_version_no` = m.`version_no`,
        tv.`batch_record_form_slot_type` = m.`form_slot_type`,
        tv.`batch_record_binding_status` = 'BOUND',
        tv.`batch_record_binding_error` = NULL,
        tv.`updater` = 'codex',
        tv.`update_time` = NOW()
    WHERE tv.`deleted` = b'0';

    UPDATE `mes_pro_route_flow_process_batch_record` rb
    JOIN `tmp_mes_old_form_template_binding_map` m
      ON m.`route_binding_id` = rb.`id`
    SET rb.`batch_record_report_id` = m.`report_id`,
        rb.`batch_record_definition_id` = m.`definition_id`,
        rb.`batch_record_version_id` = m.`version_id`,
        rb.`record_category_snapshot_hash` = LOWER(SHA2(CONCAT(
          IFNULL(CAST(rb.`route_id` AS CHAR), ''), '|',
          IFNULL(CAST(rb.`route_process_id` AS CHAR), ''), '|',
          m.`report_id`, '|',
          IFNULL(rb.`record_category`, ''), '|',
          IFNULL(rb.`validation_profile`, ''), '|',
          CASE
            WHEN rb.`record_category` = 'INTERNAL_RECORD' THEN 'false'
            WHEN rb.`recordbook_enabled` = b'1' THEN 'true'
            ELSE 'false'
          END, '|',
          IFNULL(CAST(rb.`permission_scope_id` AS CHAR), ''), '|',
          IFNULL(CAST(rb.`report_sort` AS CHAR), '')
        ), 256)),
        rb.`slot_config_snapshot_hash` = LOWER(SHA2(CONCAT(
          IFNULL(CAST(rb.`route_id` AS CHAR), ''), '|',
          IFNULL(CAST(rb.`route_process_id` AS CHAR), ''), '|',
          m.`report_id`, '|',
          IFNULL(rb.`form_slot_type`, ''), '|',
          IFNULL(rb.`record_category`, ''), '|',
          IFNULL(rb.`validation_profile`, ''), '|',
          CASE
            WHEN rb.`record_category` = 'INTERNAL_RECORD' THEN 'false'
            WHEN rb.`recordbook_enabled` = b'1' THEN 'true'
            ELSE 'false'
          END, '|',
          IFNULL(CAST(rb.`permission_scope_id` AS CHAR), ''), '|',
          IFNULL(rb.`required_policy`, ''), '|',
          IFNULL(rb.`required_condition_json`, ''), '|',
          IFNULL(rb.`owner_role_key`, ''), '|',
          IFNULL(rb.`archive_visibility`, ''), '|',
          IFNULL(CAST(rb.`report_sort` AS CHAR), ''), '|',
          IFNULL(rb.`instance_scope`, ''), '|',
          IFNULL(rb.`shared_form_key`, ''), '|',
          IFNULL(CAST(rb.`fillable_scope_json` AS CHAR), '')
        ), 256)),
        rb.`updater` = 'codex',
        rb.`update_time` = NOW()
    WHERE rb.`deleted` = b'0'
      AND rb.`form_template_id` IS NOT NULL
      AND rb.`batch_record_report_id` IS NULL;

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_binding_map` m
      LEFT JOIN `mes_pro_batch_record_report` r
        ON r.`report_id` = m.`report_id`
       AND r.`deleted` = b'0'
      WHERE r.`report_id` IS NULL
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Migrated route bindings missing target report metadata';
    END IF;

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_binding_map` m
      JOIN `mes_pro_route_flow_process_batch_record` rb
        ON rb.`id` = m.`route_binding_id`
      WHERE rb.`deleted` = b'0'
        AND (rb.`batch_record_report_id` IS NULL
          OR rb.`batch_record_definition_id` IS NULL
          OR rb.`batch_record_version_id` IS NULL
          OR rb.`slot_config_snapshot_hash` IS NULL
          OR rb.`record_category_snapshot_hash` IS NULL)
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Migrated route bindings remain incomplete';
    END IF;

    COMMIT;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_jimu_report_scope`;
  CREATE TEMPORARY TABLE `tmp_mes_old_form_template_jimu_report_scope` AS
  SELECT
    tv.`tenant_id`,
    tv.`id` AS `old_template_version_id`,
    tv.`batch_record_report_id` AS `report_id`,
    tv.`batch_record_report_id` AS `report_code`,
    IFNULL(tv.`batch_record_report_name`, CONCAT(tv.`template_name`, ' ', tv.`version_no`)) AS `report_name`,
    JSON_UNQUOTE(JSON_EXTRACT(tv.`jimu_schema_json`, '$.sheetLayoutJson')) AS `designer_json`,
    cat.`id` COLLATE utf8mb4_unicode_ci AS `report_category_id`
  FROM `bpm_form_template_version` tv
  JOIN `mes_pro_batch_record_report` r
    ON r.`report_id` = tv.`batch_record_report_id`
   AND r.`deleted` = b'0'
  LEFT JOIN `jimu_report` report_by_id
    ON report_by_id.`id` = tv.`batch_record_report_id`
   AND report_by_id.`del_flag` = 0
  LEFT JOIN `jimu_report_category` cat
    ON CAST(cat.`tenant_id` AS UNSIGNED) = tv.`tenant_id`
   AND cat.`del_flag` = 0
   AND cat.`name` COLLATE utf8mb4_unicode_ci = CONVERT(UNHEX('E794B5E5AD90E689B9E8AEB0E5BD95') USING utf8mb4) COLLATE utf8mb4_unicode_ci
  WHERE tv.`deleted` = b'0'
    AND tv.`batch_record_report_id` LIKE 'FORMTPL:%'
    AND report_by_id.`id` IS NULL;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_old_form_template_jimu_report_scope`
    WHERE `report_category_id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Electronic batch record report category is missing for Jimu report';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_old_form_template_jimu_report_scope`
    WHERE `report_name` IS NULL
       OR `report_name` = ''
       OR CHAR_LENGTH(`report_name`) > 50
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template Jimu report name is missing or too long';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_old_form_template_jimu_report_scope`
    WHERE IFNULL(JSON_VALID(`designer_json`), 0) <> 1
       OR JSON_EXTRACT(`designer_json`, '$.rows') IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template designer json is invalid';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_old_form_template_jimu_report_scope` s
    JOIN `jimu_report` jr
      ON jr.`code` = s.`report_code`
     AND jr.`id` <> s.`report_id`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Target Jimu report code conflicts';
  END IF;

  INSERT INTO `jimu_report` (
    `id`,
    `code`,
    `name`,
    `type`,
    `json_str`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`,
    `del_flag`,
    `template`,
    `view_count`,
    `css_str`,
    `tenant_id`,
    `update_count`,
    `submit_form`,
    `is_multi_sheet`
  )
  SELECT
    s.`report_id`,
    s.`report_code`,
    s.`report_name`,
    s.`report_category_id`,
    s.`designer_json`,
    'codex',
    NOW(),
    'codex',
    NOW(),
    0,
    0,
    0,
    '.fillForm-box,
.fillForm-box .ivu-form-item,
.fillForm-box .ivu-form-item-content {
  background: transparent !important;
}
.fillForm-box .inputText,
.fillForm-box textarea,
.fillForm-box .ivu-input {
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  padding: 0 2px !important;
}
.fillForm-box .ivu-input-wrapper,
.fillForm-box .ivu-input-type-text,
.fillForm-box .ivu-input-type-textarea {
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}',
    CAST(s.`tenant_id` AS CHAR),
    0,
    1,
    0
  FROM `tmp_mes_old_form_template_jimu_report_scope` s;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_version`
    WHERE `deleted` = b'0'
      AND `route_snapshot_json` IS NOT NULL
      AND JSON_VALID(`route_snapshot_json`) <> 1
      AND `route_snapshot_json` LIKE '%"formTemplateId"%'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Route snapshot JSON is invalid and contains old form template binding';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_versions`;
  CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_versions` AS
  SELECT
    `id` AS `route_version_id`,
    `tenant_id`,
    `route_snapshot_json`
  FROM `mes_pro_route_version`
  WHERE `deleted` = b'0'
    AND `route_snapshot_json` IS NOT NULL
    AND JSON_VALID(`route_snapshot_json`) = 1
    AND JSON_CONTAINS_PATH(`route_snapshot_json`, 'one', '$.configSnapshots.batchUseConfigs');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_configs`;
  CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_configs` AS
  SELECT
    rv.`route_version_id`,
    rv.`tenant_id`,
    cfg.`config_ord`,
    cfg.`config_json`,
    JSON_EXTRACT(cfg.`config_json`, '$.formBindings') AS `form_bindings_json`,
    JSON_EXTRACT(cfg.`config_json`, '$.batchRecordReports') AS `batch_record_reports_json`
  FROM `tmp_mes_old_form_template_snapshot_versions` rv
  JOIN JSON_TABLE(rv.`route_snapshot_json`, '$.configSnapshots.batchUseConfigs[*]'
    COLUMNS (
      `config_ord` FOR ORDINALITY,
      `config_json` JSON PATH '$'
    )
  ) cfg;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_form_items`;
  CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_form_items` AS
  SELECT
    cfg.`route_version_id`,
    cfg.`tenant_id`,
    cfg.`config_ord`,
    item.`binding_ord`,
    item.`binding_json`,
    item.`form_template_id`,
    item.`old_template_version_id`,
    tv.`batch_record_report_id`,
    r.`report_code`,
    r.`report_name`,
    r.`batch_record_definition_id`,
    r.`batch_record_version_id`
  FROM `tmp_mes_old_form_template_snapshot_configs` cfg
  JOIN JSON_TABLE(COALESCE(cfg.`form_bindings_json`, JSON_ARRAY()), '$[*]'
    COLUMNS (
      `binding_ord` FOR ORDINALITY,
      `binding_json` JSON PATH '$',
      `form_template_id` BIGINT PATH '$.formTemplateId' NULL ON EMPTY,
      `old_template_version_id` BIGINT PATH '$.lastPublishedTemplateVersionId' NULL ON EMPTY
    )
  ) item
  LEFT JOIN `bpm_form_template_version` tv
    ON tv.`tenant_id` = cfg.`tenant_id`
   AND tv.`template_id` = item.`form_template_id`
   AND tv.`id` = item.`old_template_version_id`
   AND tv.`deleted` = b'0'
  LEFT JOIN `mes_pro_batch_record_report` r
    ON r.`report_id` = tv.`batch_record_report_id`
   AND r.`deleted` = b'0'
  WHERE item.`form_template_id` IS NOT NULL;

  SELECT COUNT(*) INTO v_snapshot_scope_count
  FROM `tmp_mes_old_form_template_snapshot_form_items`;

  IF v_snapshot_scope_count > 0 THEN
    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_snapshot_form_items`
      WHERE `old_template_version_id` IS NULL
         OR `batch_record_report_id` IS NULL
         OR `report_code` IS NULL
         OR `report_name` IS NULL
         OR `batch_record_definition_id` IS NULL
         OR `batch_record_version_id` IS NULL
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Route snapshot old form template binding is missing target report metadata';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_existing_reports`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_existing_reports` AS
    SELECT
      cfg.`route_version_id`,
      cfg.`config_ord`,
      report.`batch_record_report_id`
    FROM `tmp_mes_old_form_template_snapshot_configs` cfg
    JOIN JSON_TABLE(COALESCE(cfg.`batch_record_reports_json`, JSON_ARRAY()), '$[*]'
      COLUMNS (
        `batch_record_report_id` VARCHAR(64) PATH '$.batchRecordReportId' NULL ON EMPTY
      )
    ) report
    WHERE report.`batch_record_report_id` IS NOT NULL;

    IF EXISTS (
      SELECT 1
      FROM `tmp_mes_old_form_template_snapshot_form_items` item
      JOIN `tmp_mes_old_form_template_snapshot_existing_reports` existing_report
        ON existing_report.`route_version_id` = item.`route_version_id`
       AND existing_report.`config_ord` = item.`config_ord`
       AND existing_report.`batch_record_report_id` = item.`batch_record_report_id`
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Route snapshot target report already exists beside old form binding';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_converted_reports`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_converted_reports` AS
    SELECT
      item.`route_version_id`,
      item.`config_ord`,
      item.`binding_ord`,
      JSON_SET(
        JSON_REMOVE(
          item.`binding_json`,
          '$.formBindingKey',
          '$.globalSyncKey',
          '$.formTemplateId',
          '$.formTemplateName',
          '$.formTemplateNameSnapshot',
          '$.lastPublishedTemplateVersionId',
          '$.lastPublishedTemplateVersionNo',
          '$.candidateSourceType',
          '$.candidateSourceIds',
          '$.candidateSourceNames',
          '$.permissionRule'
        ),
        '$.batchRecordReportId', item.`batch_record_report_id`,
        '$.batchRecordReportCode', item.`report_code`,
        '$.batchRecordReportName', item.`report_name`,
        '$.batchRecordDefinitionId', item.`batch_record_definition_id`,
        '$.batchRecordVersionId', item.`batch_record_version_id`
      ) AS `report_json`
    FROM `tmp_mes_old_form_template_snapshot_form_items` item;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_converted_report_arrays`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_converted_report_arrays` AS
    SELECT
      `route_version_id`,
      `config_ord`,
      JSON_EXTRACT(CONCAT(
        '[',
        GROUP_CONCAT(CAST(`report_json` AS CHAR CHARACTER SET utf8mb4) ORDER BY `binding_ord` SEPARATOR ','),
        ']'
      ), '$') AS `converted_reports_json`
    FROM `tmp_mes_old_form_template_snapshot_converted_reports`
    GROUP BY `route_version_id`, `config_ord`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_target_versions`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_target_versions` AS
    SELECT DISTINCT `route_version_id`
    FROM `tmp_mes_old_form_template_snapshot_converted_reports`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_new_configs`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_new_configs` AS
    SELECT
      cfg.`route_version_id`,
      cfg.`config_ord`,
      JSON_SET(
        cfg.`config_json`,
        '$.formBindings', JSON_ARRAY(),
        '$.batchRecordReports',
        CASE
          WHEN arrays.`converted_reports_json` IS NULL THEN COALESCE(cfg.`batch_record_reports_json`, JSON_ARRAY())
          ELSE JSON_MERGE_PRESERVE(COALESCE(cfg.`batch_record_reports_json`, JSON_ARRAY()), arrays.`converted_reports_json`)
        END
      ) AS `config_json`
    FROM `tmp_mes_old_form_template_snapshot_configs` cfg
    JOIN `tmp_mes_old_form_template_snapshot_target_versions` target_version
      ON target_version.`route_version_id` = cfg.`route_version_id`
    LEFT JOIN `tmp_mes_old_form_template_snapshot_converted_report_arrays` arrays
      ON arrays.`route_version_id` = cfg.`route_version_id`
     AND arrays.`config_ord` = cfg.`config_ord`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_batch_arrays`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_batch_arrays` AS
    SELECT
      `route_version_id`,
      JSON_EXTRACT(CONCAT(
        '[',
        GROUP_CONCAT(CAST(`config_json` AS CHAR CHARACTER SET utf8mb4) ORDER BY `config_ord` SEPARATOR ','),
        ']'
      ), '$') AS `batch_use_configs_json`
    FROM `tmp_mes_old_form_template_snapshot_new_configs`
    GROUP BY `route_version_id`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_old_form_template_snapshot_new_json`;
    CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_new_json` AS
    SELECT
      rv.`route_version_id`,
      JSON_SET(
        rv.`route_snapshot_json`,
        '$.configSnapshots.batchUseConfigs',
        batch_arrays.`batch_use_configs_json`
      ) AS `route_snapshot_json`
    FROM `tmp_mes_old_form_template_snapshot_versions` rv
    JOIN `tmp_mes_old_form_template_snapshot_batch_arrays` batch_arrays
      ON batch_arrays.`route_version_id` = rv.`route_version_id`;

    START TRANSACTION;

    UPDATE `mes_pro_route_version` rv
    JOIN `tmp_mes_old_form_template_snapshot_new_json` snapshot_json
      ON snapshot_json.`route_version_id` = rv.`id`
    SET rv.`route_snapshot_json` = CAST(snapshot_json.`route_snapshot_json` AS CHAR CHARACTER SET utf8mb4),
        rv.`route_snapshot_sha256` = LOWER(SHA2(CAST(snapshot_json.`route_snapshot_json` AS CHAR CHARACTER SET utf8mb4), 256)),
        rv.`updater` = 'codex',
        rv.`update_time` = NOW()
    WHERE rv.`deleted` = b'0';

    IF EXISTS (
      SELECT 1
      FROM `mes_pro_route_version` rv
      JOIN JSON_TABLE(rv.`route_snapshot_json`, '$.configSnapshots.batchUseConfigs[*]'
        COLUMNS (`form_bindings_json` JSON PATH '$.formBindings')
      ) cfg
      JOIN JSON_TABLE(COALESCE(cfg.`form_bindings_json`, JSON_ARRAY()), '$[*]'
        COLUMNS (
          `form_template_id` BIGINT PATH '$.formTemplateId' NULL ON EMPTY,
          `old_template_version_id` BIGINT PATH '$.lastPublishedTemplateVersionId' NULL ON EMPTY
        )
      ) item
      JOIN `bpm_form_template_version` tv
        ON tv.`tenant_id` = rv.`tenant_id`
       AND tv.`template_id` = item.`form_template_id`
       AND tv.`id` = item.`old_template_version_id`
       AND tv.`deleted` = b'0'
       AND tv.`batch_record_report_id` IS NOT NULL
      WHERE rv.`deleted` = b'0'
        AND JSON_VALID(rv.`route_snapshot_json`) = 1
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Old form template route snapshot bindings remain after switch';
    END IF;

    COMMIT;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_flow_process_batch_record`
    WHERE `deleted` = b'0'
      AND `form_template_id` IS NOT NULL
      AND `batch_record_report_id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Old form template route bindings remain after switch';
  END IF;
END$$
DELIMITER ;

CALL `migrate_mes_old_form_template_bindings_to_form_center`();
DROP PROCEDURE IF EXISTS `migrate_mes_old_form_template_bindings_to_form_center`;
