-- release-target-preflight: migrationId=20260808_mes_qa_optional_equipment_items; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_qa_inspection_regulation_item','mes_qa_inspection_regulation_item_equipment')) = 2
THEN 'TARGET_PREFLIGHT_PASS:20260808_mes_qa_optional_equipment_items' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260808_mes_qa_optional_equipment_items' END;
