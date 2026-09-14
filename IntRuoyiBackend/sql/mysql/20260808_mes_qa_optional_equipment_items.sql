-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_pqc_item_equipment_standard_snapshot; type=data; riskLevel=medium
-- QA optional equipment repair: keep `equipment_required` consistent with formal item-equipment bindings.
-- Recovery: restore affected `mes_qa_inspection_regulation_item.equipment_required` values from a pre-migration backup.
-- Rollback blocker: do not roll back after PQC submissions depend on optional-equipment items with null selected equipment snapshots.

DROP PROCEDURE IF EXISTS repair_mes_qa_optional_equipment_items;

DELIMITER //
CREATE PROCEDURE repair_mes_qa_optional_equipment_items()
BEGIN
  DECLARE v_required_table_count INT DEFAULT 0;
  DECLARE v_remaining_mismatch_count INT DEFAULT 0;

  SELECT COUNT(*)
    INTO v_required_table_count
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN (
      'mes_qa_inspection_regulation_item',
      'mes_qa_inspection_regulation_item_equipment'
    );

  IF v_required_table_count <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA optional equipment repair requires mes_qa_inspection_regulation_item and item_equipment';
  END IF;

  SELECT COUNT(*)
    INTO v_required_table_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND (
      (TABLE_NAME = 'mes_qa_inspection_regulation_item'
       AND COLUMN_NAME IN ('equipment_required', 'regulation_version_id', 'inspection_type', 'item_code', 'deleted'))
      OR
      (TABLE_NAME = 'mes_qa_inspection_regulation_item_equipment'
       AND COLUMN_NAME IN ('regulation_version_id', 'inspection_type', 'item_code', 'deleted'))
    );

  IF v_required_table_count <> 9 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA optional equipment repair requires item/equipment identity columns';
  END IF;

  UPDATE `mes_qa_inspection_regulation_item` `item`
  SET `item`.`equipment_required` =
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM `mes_qa_inspection_regulation_item_equipment` `equipment`
          WHERE `equipment`.`regulation_version_id` = `item`.`regulation_version_id`
            AND `equipment`.`inspection_type` = `item`.`inspection_type`
            AND `equipment`.`item_code` = `item`.`item_code`
            AND `equipment`.`deleted` = b'0'
        ) THEN b'1'
        ELSE b'0'
      END,
      `item`.`updater` = 'system',
      `item`.`update_time` = NOW()
  WHERE `item`.`deleted` = b'0'
    AND `item`.`equipment_required` <>
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM `mes_qa_inspection_regulation_item_equipment` `equipment`
          WHERE `equipment`.`regulation_version_id` = `item`.`regulation_version_id`
            AND `equipment`.`inspection_type` = `item`.`inspection_type`
            AND `equipment`.`item_code` = `item`.`item_code`
            AND `equipment`.`deleted` = b'0'
        ) THEN b'1'
        ELSE b'0'
      END;

  SELECT COUNT(*)
    INTO v_remaining_mismatch_count
  FROM `mes_qa_inspection_regulation_item` `item`
  WHERE `item`.`deleted` = b'0'
    AND (
      (`item`.`equipment_required` = b'1'
       AND NOT EXISTS (
         SELECT 1
         FROM `mes_qa_inspection_regulation_item_equipment` `equipment`
         WHERE `equipment`.`regulation_version_id` = `item`.`regulation_version_id`
           AND `equipment`.`inspection_type` = `item`.`inspection_type`
           AND `equipment`.`item_code` = `item`.`item_code`
           AND `equipment`.`deleted` = b'0'
       ))
      OR
      (`item`.`equipment_required` = b'0'
       AND EXISTS (
         SELECT 1
         FROM `mes_qa_inspection_regulation_item_equipment` `equipment`
         WHERE `equipment`.`regulation_version_id` = `item`.`regulation_version_id`
           AND `equipment`.`inspection_type` = `item`.`inspection_type`
           AND `equipment`.`item_code` = `item`.`item_code`
           AND `equipment`.`deleted` = b'0'
       ))
    );

  IF v_remaining_mismatch_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA optional equipment repair still has inconsistent item equipment flags';
  END IF;
END//
DELIMITER ;

CALL repair_mes_qa_optional_equipment_items();

DROP PROCEDURE IF EXISTS repair_mes_qa_optional_equipment_items;
