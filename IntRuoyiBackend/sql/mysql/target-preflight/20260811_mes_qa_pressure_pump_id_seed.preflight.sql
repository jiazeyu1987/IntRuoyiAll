-- release-target-preflight: migrationId=20260811_mes_qa_pressure_pump_id_seed; allowedEnvironments=test,backup,prod
WITH project_match AS (
  SELECT id, tenant_id FROM dcc_project_code
  WHERE project_code = 'ID' AND project_name = '球囊扩张压力泵'
    AND doc_control_no = '112' AND status = 'ENABLE' AND deleted = 0
), existing_root AS (
  SELECT regulation.id, regulation.tenant_id, regulation.current_version_id
  FROM mes_qa_inspection_regulation regulation
  JOIN project_match project ON project.id = regulation.dcc_project_code_id
    AND project.tenant_id = regulation.tenant_id
  WHERE regulation.deleted = b'0'
)
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_project_code','mes_qa_inspection_regulation','mes_qa_inspection_regulation_item','mes_qa_inspection_regulation_process','mes_qa_inspection_regulation_version')) = 5
  AND (SELECT COUNT(*) FROM project_match) = 1
  AND (SELECT COUNT(*) FROM existing_root) <= 1
  AND NOT EXISTS (
    SELECT 1 FROM existing_root root
    LEFT JOIN mes_qa_inspection_regulation_version version_record
      ON version_record.id = root.current_version_id
      AND version_record.tenant_id = root.tenant_id AND version_record.deleted = b'0'
    WHERE CASE WHEN JSON_VALID(version_record.snapshot_json) THEN
      COALESCE(JSON_UNQUOTE(JSON_EXTRACT(version_record.snapshot_json, '$.migrationKey'))
        <> '20260811-ID-PQC-ID-001-G0-v1', TRUE) ELSE TRUE END
      OR (SELECT COUNT(*) FROM mes_qa_inspection_regulation_process process
        WHERE process.tenant_id = root.tenant_id AND process.regulation_version_id = root.current_version_id
          AND process.deleted = b'0') <> 8
      OR (SELECT COUNT(DISTINCT item.item_code) FROM mes_qa_inspection_regulation_item item
        WHERE item.tenant_id = root.tenant_id AND item.regulation_version_id = root.current_version_id
          AND item.deleted = b'0') <> 18
      OR (SELECT COUNT(*) FROM mes_qa_inspection_regulation_item item
        WHERE item.tenant_id = root.tenant_id AND item.regulation_version_id = root.current_version_id
          AND item.deleted = b'0') <> 51
  )
THEN 'TARGET_PREFLIGHT_PASS:20260811_mes_qa_pressure_pump_id_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260811_mes_qa_pressure_pump_id_seed' END;
