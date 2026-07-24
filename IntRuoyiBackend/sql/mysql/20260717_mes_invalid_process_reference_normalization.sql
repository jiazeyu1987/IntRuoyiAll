-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_mes_balloon_excel_device_workstation_binding; type=data; riskLevel=high
-- Goal:
--   Normalize every direct MES process reference so it either points to a current process in mes_pro_process
--   where deleted = b'0', or becomes 0 when no unique current process can be proven.
-- Safety:
--   This migration is non-destructive: no business rows are deleted. It backs up each changed reference before
--   updating it, covers historical rows as requested, and does not filter out deleted business records.
-- Rollback:
--   Restore each table reference from mes_invalid_process_reference_normalization_20260717 using
--   table_name + column_name + row_id.

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_mes_invalid_process_reference_normalization`$$
CREATE PROCEDURE `migrate_mes_invalid_process_reference_normalization`()
BEGIN
    DECLARE candidate_reference_count bigint DEFAULT 0;
    DECLARE rebound_reference_count bigint DEFAULT 0;
    DECLARE zeroed_reference_count bigint DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    CREATE TABLE IF NOT EXISTS `mes_invalid_process_reference_normalization_20260717` (
        `table_name` varchar(128) NOT NULL,
        `column_name` varchar(64) NOT NULL,
        `row_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `old_process_id` bigint NOT NULL,
        `new_process_id` bigint NOT NULL,
        `mapping_rule` varchar(64) NOT NULL,
        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`table_name`, `column_name`, `row_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='20260717 无效工序引用归一备份';

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_invalid_process_reference_candidate`;
    CREATE TEMPORARY TABLE `tmp_mes_invalid_process_reference_candidate` (
        `table_name` varchar(128) NOT NULL,
        `column_name` varchar(64) NOT NULL,
        `row_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `old_process_id` bigint NOT NULL,
        `new_process_id` bigint NOT NULL,
        `mapping_rule` varchar(64) NOT NULL,
        PRIMARY KEY (`table_name`, `column_name`, `row_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    INSERT INTO `tmp_mes_invalid_process_reference_candidate` (
        `table_name`,
        `column_name`,
        `row_id`,
        `tenant_id`,
        `old_process_id`,
        `new_process_id`,
        `mapping_rule`
    )
    WITH process_map AS (
        SELECT
            old_process.`id` AS `old_process_id`,
            old_process.`tenant_id`,
            MIN(current_process.`id`) AS `new_process_id`
          FROM `mes_pro_process` old_process
          JOIN `mes_pro_process` current_process
            ON current_process.`tenant_id` = old_process.`tenant_id`
           AND current_process.`code` = old_process.`code`
           AND current_process.`deleted` = b'0'
         WHERE old_process.`deleted` = b'1'
           AND old_process.`code` IS NOT NULL
           AND old_process.`code` <> ''
         GROUP BY old_process.`id`, old_process.`tenant_id`
        HAVING COUNT(DISTINCT current_process.`id`) = 1
    )
    -- target `mes_dv_machinery_process`.`process_id`
    SELECT 'mes_dv_machinery_process', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_dv_machinery_process` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_md_product_sip`.`process_id`
    SELECT 'mes_md_product_sip', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_md_product_sip` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_md_product_sop`.`process_id`
    SELECT 'mes_md_product_sop', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_md_product_sop` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_md_workstation`.`process_id`
    SELECT 'mes_md_workstation', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_md_workstation` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_andon_record`.`process_id`
    SELECT 'mes_pro_andon_record', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_andon_record` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_batch_record_template`.`process_id`
    SELECT 'mes_pro_batch_record_template', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_batch_record_template` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_card_process`.`process_id`
    SELECT 'mes_pro_card_process', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_card_process` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_edhr_batch_execution_task`.`process_id`
    SELECT 'mes_pro_edhr_batch_execution_task', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_edhr_batch_execution_task` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_edhr_traveler_instance`.`process_id`
    SELECT 'mes_pro_edhr_traveler_instance', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_edhr_traveler_instance` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_edhr_traveler_template`.`applicable_process_id`
    SELECT 'mes_pro_edhr_traveler_template', 'applicable_process_id', target.`id`, target.`tenant_id`, target.`applicable_process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_edhr_traveler_template` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`applicable_process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`applicable_process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`applicable_process_id` IS NOT NULL
       AND target.`applicable_process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_edhr_work_task`.`process_id`
    SELECT 'mes_pro_edhr_work_task', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_edhr_work_task` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_feedback`.`process_id`
    SELECT 'mes_pro_feedback', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_feedback` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_feedback_surplus_pool`.`process_id`
    SELECT 'mes_pro_feedback_surplus_pool', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_feedback_surplus_pool` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_process_content`.`process_id`
    SELECT 'mes_pro_process_content', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_process_content` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_route_process`.`next_process_id`
    SELECT 'mes_pro_route_process', 'next_process_id', target.`id`, target.`tenant_id`, target.`next_process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_route_process` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`next_process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`next_process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`next_process_id` IS NOT NULL
       AND target.`next_process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_route_process`.`process_id`
    SELECT 'mes_pro_route_process', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_route_process` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_route_product_bom`.`process_id`
    SELECT 'mes_pro_route_product_bom', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_route_product_bom` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_schedule_issue`.`process_id`
    SELECT 'mes_pro_schedule_issue', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_schedule_issue` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_schedule_order_daily_compare`.`process_id`
    SELECT 'mes_pro_schedule_order_daily_compare', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_schedule_order_daily_compare` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_schedule_order_process`.`process_id`
    SELECT 'mes_pro_schedule_order_process', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_schedule_order_process` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_task`.`process_id`
    SELECT 'mes_pro_task', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_task` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_task_dependency`.`source_process_id`
    SELECT 'mes_pro_task_dependency', 'source_process_id', target.`id`, target.`tenant_id`, target.`source_process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_task_dependency` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`source_process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`source_process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`source_process_id` IS NOT NULL
       AND target.`source_process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_pro_task_dependency`.`target_process_id`
    SELECT 'mes_pro_task_dependency', 'target_process_id', target.`id`, target.`tenant_id`, target.`target_process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_pro_task_dependency` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`target_process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`target_process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`target_process_id` IS NOT NULL
       AND target.`target_process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_qc_ipqc`.`process_id`
    SELECT 'mes_qc_ipqc', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_qc_ipqc` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_wm_item_consume`.`process_id`
    SELECT 'mes_wm_item_consume', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_wm_item_consume` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL
    UNION ALL
    -- target `mes_wm_product_produce`.`process_id`
    SELECT 'mes_wm_product_produce', 'process_id', target.`id`, target.`tenant_id`, target.`process_id`,
           COALESCE(process_map.`new_process_id`, 0) AS `new_process_id`,
           IF(process_map.`new_process_id` IS NULL, 'zero_invalid_or_ambiguous_process', 'same_code_unique_active_process')
      FROM `mes_wm_product_produce` target
      LEFT JOIN `mes_pro_process` valid_process
        ON valid_process.`id` = target.`process_id`
       AND valid_process.`tenant_id` = target.`tenant_id`
       AND valid_process.`deleted` = b'0'
      LEFT JOIN process_map
        ON process_map.`old_process_id` = target.`process_id`
       AND process_map.`tenant_id` = target.`tenant_id`
     WHERE target.`process_id` IS NOT NULL
       AND target.`process_id` <> 0
       AND valid_process.`id` IS NULL;

    SELECT COUNT(*)
      INTO candidate_reference_count
      FROM `tmp_mes_invalid_process_reference_candidate`;

    SELECT COUNT(*)
      INTO rebound_reference_count
      FROM `tmp_mes_invalid_process_reference_candidate`
     WHERE `new_process_id` <> 0;

    SELECT COUNT(*)
      INTO zeroed_reference_count
      FROM `tmp_mes_invalid_process_reference_candidate`
     WHERE `new_process_id` = 0;

    START TRANSACTION;

    INSERT IGNORE INTO `mes_invalid_process_reference_normalization_20260717` (
        `table_name`,
        `column_name`,
        `row_id`,
        `tenant_id`,
        `old_process_id`,
        `new_process_id`,
        `mapping_rule`
    )
    SELECT
        candidate.`table_name`,
        candidate.`column_name`,
        candidate.`row_id`,
        candidate.`tenant_id`,
        candidate.`old_process_id`,
        candidate.`new_process_id`,
        candidate.`mapping_rule`
      FROM `tmp_mes_invalid_process_reference_candidate` candidate;

    UPDATE `mes_dv_machinery_process` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_dv_machinery_process'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_md_product_sip` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_md_product_sip'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_md_product_sop` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_md_product_sop'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_md_workstation` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_md_workstation'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_andon_record` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_andon_record'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_batch_record_template` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_batch_record_template'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_card_process` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_card_process'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_edhr_batch_execution_task` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_edhr_batch_execution_task'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_edhr_traveler_instance` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_edhr_traveler_instance'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_edhr_traveler_template` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_edhr_traveler_template'
       AND candidate.`column_name` = 'applicable_process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`applicable_process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`applicable_process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_edhr_work_task` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_edhr_work_task'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_feedback` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_feedback'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_feedback_surplus_pool` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_feedback_surplus_pool'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_process_content` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_process_content'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_route_process` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_route_process'
       AND candidate.`column_name` = 'next_process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`next_process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`next_process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_route_process` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_route_process'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_route_product_bom` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_route_product_bom'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_schedule_issue` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_schedule_issue'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_schedule_order_daily_compare` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_schedule_order_daily_compare'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_schedule_order_process` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_schedule_order_process'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_task` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_task'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_task_dependency` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_task_dependency'
       AND candidate.`column_name` = 'source_process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`source_process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`source_process_id` = candidate.`old_process_id`;

    UPDATE `mes_pro_task_dependency` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_pro_task_dependency'
       AND candidate.`column_name` = 'target_process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`target_process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`target_process_id` = candidate.`old_process_id`;

    UPDATE `mes_qc_ipqc` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_qc_ipqc'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_wm_item_consume` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_wm_item_consume'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    UPDATE `mes_wm_product_produce` target
      JOIN `tmp_mes_invalid_process_reference_candidate` candidate
        ON candidate.`table_name` = 'mes_wm_product_produce'
       AND candidate.`column_name` = 'process_id'
       AND candidate.`row_id` = target.`id`
       SET target.`process_id` = candidate.`new_process_id`,
           target.`update_time` = CURRENT_TIMESTAMP
     WHERE target.`process_id` = candidate.`old_process_id`;

    COMMIT;

    SELECT
        candidate_reference_count AS `candidate_reference_count`,
        rebound_reference_count AS `rebound_reference_count`,
        zeroed_reference_count AS `zeroed_reference_count`;

    SELECT
        candidate.`table_name`,
        candidate.`column_name`,
        COUNT(*) AS `candidate_count`,
        SUM(candidate.`new_process_id` <> 0) AS `rebound_count`,
        SUM(candidate.`new_process_id` = 0) AS `zeroed_count`
      FROM `tmp_mes_invalid_process_reference_candidate` candidate
     GROUP BY candidate.`table_name`, candidate.`column_name`
     ORDER BY candidate.`table_name`, candidate.`column_name`;
END$$

DELIMITER ;

CALL `migrate_mes_invalid_process_reference_normalization`();

DROP PROCEDURE IF EXISTS `migrate_mes_invalid_process_reference_normalization`;
