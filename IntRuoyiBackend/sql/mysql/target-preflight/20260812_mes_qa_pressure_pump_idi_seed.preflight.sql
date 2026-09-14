-- release-target-preflight: migrationId=20260812_mes_qa_pressure_pump_idi_seed; allowedEnvironments=test,backup,prod
WITH
required_tables AS (
  SELECT COUNT(*) AS table_count
    FROM information_schema.tables
   WHERE table_schema = DATABASE()
     AND LOWER(table_name) IN (
       'dcc_project_code',
       'mes_qa_inspection_regulation',
       'mes_qa_inspection_regulation_item',
       'mes_qa_inspection_regulation_process',
       'mes_qa_inspection_regulation_version'
     )
),
dcc_match AS (
  SELECT COUNT(1) AS match_count
    FROM dcc_project_code
   WHERE project_code = 'IDI'
     AND project_name = '按压式球囊扩充压力泵'
     AND doc_control_no = '1'
     AND status = 'ENABLE'
     AND deleted = b'0'
),
dcc_project AS (
  SELECT id, tenant_id
    FROM dcc_project_code
   WHERE project_code = 'IDI'
     AND project_name = '按压式球囊扩充压力泵'
     AND doc_control_no = '1'
     AND status = 'ENABLE'
     AND deleted = b'0'
   LIMIT 1
),
direct_qa_summary AS (
  SELECT COUNT(DISTINCT regulation.id) AS root_count,
         COUNT(DISTINCT CASE
           WHEN JSON_UNQUOTE(JSON_EXTRACT(version_record.snapshot_json, '$.migrationKey')) = '20260812-IDI-PQC-IDI-001-B0-v1'
            AND process_counts.process_count = 3
            AND item_counts.logical_item_count = 22
            AND item_counts.item_row_count = 64
           THEN regulation.id
         END) AS compatible_root_count
    FROM dcc_project project
    LEFT JOIN mes_qa_inspection_regulation regulation
      ON regulation.tenant_id = project.tenant_id
     AND regulation.dcc_project_code_id = project.id
     AND regulation.deleted = b'0'
    LEFT JOIN mes_qa_inspection_regulation_version version_record
      ON version_record.id = regulation.current_version_id
     AND version_record.tenant_id = regulation.tenant_id
     AND version_record.deleted = b'0'
    LEFT JOIN (
      SELECT regulation_version_id, COUNT(1) AS process_count
        FROM mes_qa_inspection_regulation_process
       WHERE deleted = b'0'
       GROUP BY regulation_version_id
    ) process_counts
      ON process_counts.regulation_version_id = regulation.current_version_id
    LEFT JOIN (
      SELECT regulation_version_id,
             COUNT(DISTINCT item_code) AS logical_item_count,
             COUNT(1) AS item_row_count
        FROM mes_qa_inspection_regulation_item
       WHERE deleted = b'0'
       GROUP BY regulation_version_id
    ) item_counts
      ON item_counts.regulation_version_id = regulation.current_version_id
),
source_names AS (
  SELECT '按压式球囊扩充压力泵组装过程检验规程-清洗工序' AS source_name, 1 AS expected_logical_count, 2 AS expected_row_count
  UNION ALL
  SELECT '按压式球囊扩充压力泵组装过程检验规程-清洁工序', 1, 2
  UNION ALL
  SELECT '按压式球囊扩充压力泵组装过程检验规程-大包装工序', 20, 60
),
legacy_source_summary AS (
  SELECT COUNT(DISTINCT regulation.id) AS source_reg_count,
         COUNT(DISTINCT version_record.id) AS source_version_count,
         COUNT(DISTINCT source_item.item_code) AS logical_item_count,
         COUNT(source_item.id) AS item_row_count,
         COUNT(DISTINCT CASE
           WHEN item_counts.logical_count = source_names.expected_logical_count
            AND item_counts.row_count = source_names.expected_row_count
           THEN source_names.source_name
         END) AS valid_source_name_count
    FROM source_names
    LEFT JOIN dcc_project project
      ON 1 = 1
    LEFT JOIN mes_qa_inspection_regulation regulation
      ON regulation.tenant_id = project.tenant_id
     AND regulation.dcc_project_code_id IS NULL
     AND regulation.owner_module = 'MES_QA'
     AND regulation.regulation_name = source_names.source_name
     AND regulation.lifecycle_status = 'PUBLISHED'
     AND regulation.current_version_id IS NOT NULL
     AND regulation.deleted = b'0'
    LEFT JOIN mes_qa_inspection_regulation_version version_record
      ON version_record.id = regulation.current_version_id
     AND version_record.regulation_id = regulation.id
     AND version_record.tenant_id = regulation.tenant_id
     AND version_record.lifecycle_status = 'PUBLISHED'
     AND version_record.deleted = b'0'
    LEFT JOIN mes_qa_inspection_regulation_item source_item
      ON source_item.tenant_id = project.tenant_id
     AND source_item.regulation_version_id = version_record.id
     AND source_item.deleted = b'0'
    LEFT JOIN (
      SELECT regulation_version_id,
             COUNT(DISTINCT item_code) AS logical_count,
             COUNT(1) AS row_count
        FROM mes_qa_inspection_regulation_item
       WHERE deleted = b'0'
       GROUP BY regulation_version_id
    ) item_counts
      ON item_counts.regulation_version_id = version_record.id
)
SELECT CASE WHEN
  (SELECT table_count FROM required_tables) = 5
  AND (SELECT match_count FROM dcc_match) = 1
  AND (
    EXISTS (
      SELECT 1
        FROM direct_qa_summary
       WHERE root_count = 1
         AND compatible_root_count = 1
    )
    OR (
      EXISTS (
        SELECT 1
          FROM direct_qa_summary
         WHERE root_count = 0
      )
      AND (
        EXISTS (
          SELECT 1
            FROM legacy_source_summary
           WHERE source_reg_count = 0
             AND source_version_count = 0
             AND logical_item_count = 0
             AND item_row_count = 0
        )
        OR EXISTS (
          SELECT 1
            FROM legacy_source_summary
           WHERE source_reg_count = 3
             AND source_version_count = 3
             AND valid_source_name_count = 3
             AND logical_item_count = 22
             AND item_row_count = 64
        )
      )
    )
  )
THEN 'TARGET_PREFLIGHT_PASS:20260812_mes_qa_pressure_pump_idi_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260812_mes_qa_pressure_pump_idi_seed' END;
