-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_qa_dcc_project_scope; type=data; riskLevel=high
-- 将 IDI / 按压式球囊扩充压力泵 / 1 的旧路线工序 QA 规程合并固化为 DCC 项目代码直接所属的正式发布版本。
-- 回滚范围：仅删除 migrationKey=20260812-IDI-PQC-IDI-001-B0-v1 对应的版本、工序、项目和 DCC QA 根。

DROP PROCEDURE IF EXISTS seed_mes_qa_pressure_pump_idi;
DELIMITER $$
CREATE PROCEDURE seed_mes_qa_pressure_pump_idi()
seed_block: BEGIN
  DECLARE v_dcc_match_count int DEFAULT 0;
  DECLARE v_existing_root_count int DEFAULT 0;
  DECLARE v_source_join_count int DEFAULT 0;
  DECLARE v_source_reg_count int DEFAULT 0;
  DECLARE v_source_invalid_count int DEFAULT 0;
  DECLARE v_process_count int DEFAULT 0;
  DECLARE v_logical_item_count int DEFAULT 0;
  DECLARE v_item_row_count int DEFAULT 0;
  DECLARE v_dcc_project_code_id bigint DEFAULT NULL;
  DECLARE v_tenant_id bigint DEFAULT NULL;
  DECLARE v_regulation_id bigint DEFAULT NULL;
  DECLARE v_regulation_version_id bigint DEFAULT NULL;
  DECLARE v_existing_migration_key varchar(128) DEFAULT NULL;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT COUNT(1)
    INTO v_dcc_match_count
    FROM dcc_project_code
   WHERE project_code = 'IDI'
     AND project_name = '按压式球囊扩充压力泵'
     AND doc_control_no = '1'
     AND status = 'ENABLE'
     AND deleted = b'0';
  IF v_dcc_match_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC项目业务键必须唯一命中1条';
  END IF;

  SELECT id, tenant_id
    INTO v_dcc_project_code_id, v_tenant_id
    FROM dcc_project_code
   WHERE project_code = 'IDI'
     AND project_name = '按压式球囊扩充压力泵'
     AND doc_control_no = '1'
     AND status = 'ENABLE'
     AND deleted = b'0';

  SELECT COUNT(1)
    INTO v_existing_root_count
    FROM mes_qa_inspection_regulation
   WHERE tenant_id = v_tenant_id
     AND dcc_project_code_id = v_dcc_project_code_id
     AND deleted = b'0';
  IF v_existing_root_count > 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '已有多个直接归属该DCC项目的QA规程';
  END IF;

  IF v_existing_root_count = 1 THEN
    SELECT regulation.id, regulation.current_version_id,
           JSON_UNQUOTE(JSON_EXTRACT(version_record.snapshot_json, '$.migrationKey'))
      INTO v_regulation_id, v_regulation_version_id, v_existing_migration_key
      FROM mes_qa_inspection_regulation regulation
      LEFT JOIN mes_qa_inspection_regulation_version version_record
        ON version_record.id = regulation.current_version_id
       AND version_record.tenant_id = regulation.tenant_id
       AND version_record.deleted = b'0'
     WHERE regulation.tenant_id = v_tenant_id
       AND regulation.dcc_project_code_id = v_dcc_project_code_id
       AND regulation.deleted = b'0';

    SELECT COUNT(1)
      INTO v_process_count
      FROM mes_qa_inspection_regulation_process
     WHERE tenant_id = v_tenant_id
       AND regulation_version_id = v_regulation_version_id
       AND deleted = b'0';
    SELECT COUNT(DISTINCT item_code), COUNT(1)
      INTO v_logical_item_count, v_item_row_count
      FROM mes_qa_inspection_regulation_item
     WHERE tenant_id = v_tenant_id
       AND regulation_version_id = v_regulation_version_id
       AND deleted = b'0';

    IF v_existing_migration_key = '20260812-IDI-PQC-IDI-001-B0-v1'
       AND v_process_count = 3
       AND v_logical_item_count = 22
       AND v_item_row_count = 64 THEN
      COMMIT;
      LEAVE seed_block;
    END IF;

    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '已有直接归属该DCC项目的QA规程与本次迁移不一致';
  END IF;

  CREATE TEMPORARY TABLE tmp_mes_qa_idi_source_process (
    process_code varchar(64) NOT NULL,
    process_name varchar(128) NOT NULL,
    process_sort int NOT NULL,
    source_regulation_name varchar(128) NOT NULL,
    expected_logical_count int NOT NULL,
    expected_row_count int NOT NULL,
    source_regulation_id bigint DEFAULT NULL,
    source_version_id bigint DEFAULT NULL,
    PRIMARY KEY (process_code),
    UNIQUE KEY uk_tmp_mes_qa_idi_source_name (source_regulation_name)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_mes_qa_idi_source_process (
    process_code, process_name, process_sort, source_regulation_name,
    expected_logical_count, expected_row_count
  ) VALUES
    ('IDI-QA-001', '清洗工序', 1, '按压式球囊扩充压力泵组装过程检验规程-清洗工序', 1, 2),
    ('IDI-QA-002', '清洁工序', 2, '按压式球囊扩充压力泵组装过程检验规程-清洁工序', 1, 2),
    ('IDI-QA-003', '大包装工序', 3, '按压式球囊扩充压力泵组装过程检验规程-大包装工序', 20, 60);

  SELECT COUNT(1), COUNT(DISTINCT regulation.id)
    INTO v_source_join_count, v_source_reg_count
    FROM tmp_mes_qa_idi_source_process source_process
    JOIN mes_qa_inspection_regulation regulation
      ON regulation.tenant_id = v_tenant_id
     AND regulation.dcc_project_code_id IS NULL
     AND regulation.owner_module = 'MES_QA'
     AND regulation.regulation_name = source_process.source_regulation_name
     AND regulation.lifecycle_status = 'PUBLISHED'
     AND regulation.current_version_id IS NOT NULL
     AND regulation.deleted = b'0'
    JOIN mes_qa_inspection_regulation_version version_record
      ON version_record.id = regulation.current_version_id
     AND version_record.regulation_id = regulation.id
     AND version_record.tenant_id = regulation.tenant_id
     AND version_record.lifecycle_status = 'PUBLISHED'
     AND version_record.deleted = b'0';
  IF v_source_join_count <> 3 OR v_source_reg_count <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI旧QA规程源数据必须唯一命中3条';
  END IF;

  UPDATE tmp_mes_qa_idi_source_process source_process
  JOIN mes_qa_inspection_regulation regulation
    ON regulation.tenant_id = v_tenant_id
   AND regulation.dcc_project_code_id IS NULL
   AND regulation.owner_module = 'MES_QA'
   AND regulation.regulation_name = source_process.source_regulation_name
   AND regulation.lifecycle_status = 'PUBLISHED'
   AND regulation.current_version_id IS NOT NULL
   AND regulation.deleted = b'0'
  JOIN mes_qa_inspection_regulation_version version_record
    ON version_record.id = regulation.current_version_id
   AND version_record.regulation_id = regulation.id
   AND version_record.tenant_id = regulation.tenant_id
   AND version_record.lifecycle_status = 'PUBLISHED'
   AND version_record.deleted = b'0'
     SET source_process.source_regulation_id = regulation.id,
         source_process.source_version_id = version_record.id;

  SELECT COUNT(1)
    INTO v_source_invalid_count
    FROM tmp_mes_qa_idi_source_process source_process
    LEFT JOIN (
      SELECT regulation_version_id, COUNT(DISTINCT item_code) AS logical_count, COUNT(1) AS row_count
        FROM mes_qa_inspection_regulation_item
       WHERE tenant_id = v_tenant_id
         AND deleted = b'0'
       GROUP BY regulation_version_id
    ) item_counts
      ON item_counts.regulation_version_id = source_process.source_version_id
   WHERE source_process.source_version_id IS NULL
      OR item_counts.logical_count <> source_process.expected_logical_count
      OR item_counts.row_count <> source_process.expected_row_count;
  IF v_source_invalid_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI旧QA规程源项目必须为22个逻辑项目和64条检验类型行';
  END IF;

  SELECT COUNT(DISTINCT source_item.item_code), COUNT(1)
    INTO v_logical_item_count, v_item_row_count
    FROM mes_qa_inspection_regulation_item source_item
    JOIN tmp_mes_qa_idi_source_process source_process
      ON source_process.source_version_id = source_item.regulation_version_id
   WHERE source_item.tenant_id = v_tenant_id
     AND source_item.deleted = b'0';
  IF v_logical_item_count <> 22 OR v_item_row_count <> 64 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI旧QA规程源项目必须为22个逻辑项目和64条检验类型行';
  END IF;

  INSERT INTO mes_qa_inspection_regulation (
    dcc_project_code_id, product_id, route_id, route_version_id, route_process_id, process_id,
    owner_module, regulation_code, regulation_name, lifecycle_status, current_version_id,
    creator, create_time, updater, update_time, deleted, tenant_id
  ) VALUES (
    v_dcc_project_code_id, NULL, NULL, NULL, NULL, NULL,
    'MES_QA', 'PQC-IDI-001', '按压式球囊扩充压力泵组装过程检验规程', 'PUBLISHED', NULL,
    'migration:20260812-qa-dcc-idi', NOW(), 'migration:20260812-qa-dcc-idi', NOW(), b'0', v_tenant_id
  );
  SET v_regulation_id = LAST_INSERT_ID();

  INSERT INTO mes_qa_inspection_regulation_version (
    regulation_id, version_no, lifecycle_status, effective_date, inspection_type_rules_json,
    published_at, retired_at, final_inspection_applicable,
    final_inspection_not_applicable_reason, snapshot_json,
    creator, create_time, updater, update_time, deleted, tenant_id
  ) VALUES (
    v_regulation_id, 'B/0', 'PUBLISHED', '2026-01-04',
    JSON_ARRAY(
      JSON_OBJECT('key', 'FIRST', 'inspectionType', 'FIRST', 'label', '首检', 'required', TRUE,
                  'taskRule', '按发布规程固定数量生成首检任务'),
      JSON_OBJECT('key', 'PATROL_AM', 'inspectionType', 'PATROL', 'label', '上午巡检', 'required', TRUE,
                  'taskRule', '按订单数量与项目抽样比例生成任务'),
      JSON_OBJECT('key', 'PATROL_PM', 'inspectionType', 'PATROL', 'label', '下午巡检', 'required', TRUE,
                  'taskRule', '按订单数量与项目抽样比例生成任务'),
      JSON_OBJECT('key', 'FINAL', 'inspectionType', 'FINAL', 'label', '末检', 'required', TRUE,
                  'fixedQuantity', 3, 'taskRule', '需要末检时生成末检任务')
    ),
    NOW(), NULL, b'1', NULL,
    JSON_OBJECT(
      'migrationKey', '20260812-IDI-PQC-IDI-001-B0-v1',
      'source', 'Legacy route-process QA regulations consolidated into DCC-owned QA regulation',
      'dccProjectCodeId', v_dcc_project_code_id,
      'projectCode', 'IDI',
      'projectName', '按压式球囊扩充压力泵',
      'docControlNo', '1',
      'regulationCode', 'PQC-IDI-001',
      'regulationName', '按压式球囊扩充压力泵组装过程检验规程',
      'versionNo', 'B/0',
      'effectiveDate', '2026-01-04',
      'sourceRegulationNames', JSON_ARRAY(
        '按压式球囊扩充压力泵组装过程检验规程-清洗工序',
        '按压式球囊扩充压力泵组装过程检验规程-清洁工序',
        '按压式球囊扩充压力泵组装过程检验规程-大包装工序'
      ),
      'processCount', 3,
      'logicalItemCount', 22,
      'itemRowCount', 64
    ),
    'migration:20260812-qa-dcc-idi', NOW(), 'migration:20260812-qa-dcc-idi', NOW(), b'0', v_tenant_id
  );
  SET v_regulation_version_id = LAST_INSERT_ID();

  INSERT INTO mes_qa_inspection_regulation_process (
    regulation_version_id, process_code, process_name, sort,
    creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_regulation_version_id, process_code, process_name, process_sort,
         'migration:20260812-qa-dcc-idi', NOW(), 'migration:20260812-qa-dcc-idi', NOW(), b'0', v_tenant_id
    FROM tmp_mes_qa_idi_source_process
   ORDER BY process_sort;

  CREATE TEMPORARY TABLE tmp_mes_qa_idi_source_item_sort
  ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  AS
  SELECT ranked.regulation_version_id,
         ranked.item_code,
         ROW_NUMBER() OVER (PARTITION BY ranked.regulation_version_id ORDER BY ranked.first_item_id) AS item_sort
    FROM (
      SELECT source_item.regulation_version_id, source_item.item_code, MIN(source_item.id) AS first_item_id
        FROM mes_qa_inspection_regulation_item source_item
        JOIN tmp_mes_qa_idi_source_process source_process
          ON source_process.source_version_id = source_item.regulation_version_id
       WHERE source_item.tenant_id = v_tenant_id
         AND source_item.deleted = b'0'
       GROUP BY source_item.regulation_version_id, source_item.item_code
    ) ranked;

  INSERT INTO mes_qa_inspection_regulation_item (
    regulation_version_id, qa_process_id, item_sort, inspection_type,
    item_code, item_name, inspection_method, inspection_tool, standard_text,
    sampling_plan_text, standard_lower_limit, standard_upper_limit, standard_unit,
    standard_precision, equipment_required, result_type, first_inspection_quantity,
    patrol_inspection_ratio, critical, failure_rule, source_note, source_original_page,
    source_original_item, source_original_excerpt, source_original_method,
    creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_regulation_version_id, target_process.id, item_sort.item_sort, source_item.inspection_type,
         source_item.item_code, source_item.item_name, source_item.inspection_method, source_item.inspection_tool,
         source_item.standard_text, source_item.sampling_plan_text,
         source_item.standard_lower_limit, source_item.standard_upper_limit, source_item.standard_unit,
         source_item.standard_precision, source_item.equipment_required, source_item.result_type,
         source_item.first_inspection_quantity, source_item.patrol_inspection_ratio,
         COALESCE(source_item.critical, b'0'),
         COALESCE(NULLIF(source_item.failure_rule, ''), '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。'),
         COALESCE(NULLIF(source_item.source_note, ''), CONCAT('迁移自旧IDI路线工序QA规程 ', source_regulation.regulation_code, ' / ', source_process.process_name, '。')),
         source_item.source_original_page,
         COALESCE(NULLIF(source_item.source_original_item, ''), CONCAT(source_process.process_name, ' / ', source_item.item_name)),
         COALESCE(NULLIF(source_item.source_original_excerpt, ''), source_item.standard_text),
         COALESCE(NULLIF(source_item.source_original_method, ''), source_item.inspection_method),
         'migration:20260812-qa-dcc-idi', NOW(), 'migration:20260812-qa-dcc-idi', NOW(), b'0', v_tenant_id
    FROM mes_qa_inspection_regulation_item source_item
    JOIN tmp_mes_qa_idi_source_process source_process
      ON source_process.source_version_id = source_item.regulation_version_id
    JOIN mes_qa_inspection_regulation source_regulation
      ON source_regulation.id = source_process.source_regulation_id
     AND source_regulation.tenant_id = v_tenant_id
     AND source_regulation.deleted = b'0'
    JOIN tmp_mes_qa_idi_source_item_sort item_sort
      ON item_sort.regulation_version_id = source_item.regulation_version_id
     AND item_sort.item_code = source_item.item_code
    JOIN mes_qa_inspection_regulation_process target_process
      ON target_process.tenant_id = v_tenant_id
     AND target_process.regulation_version_id = v_regulation_version_id
     AND target_process.process_code = source_process.process_code
     AND target_process.deleted = b'0'
   WHERE source_item.tenant_id = v_tenant_id
     AND source_item.deleted = b'0'
   ORDER BY source_process.process_sort, item_sort.item_sort,
            FIELD(source_item.inspection_type, 'FIRST', 'PATROL', 'FINAL'), source_item.id;

  SELECT COUNT(1)
    INTO v_process_count
    FROM mes_qa_inspection_regulation_process
   WHERE tenant_id = v_tenant_id
     AND regulation_version_id = v_regulation_version_id
     AND deleted = b'0';
  IF v_process_count <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA工序迁移结果必须为3条';
  END IF;

  SELECT COUNT(DISTINCT item_code), COUNT(1)
    INTO v_logical_item_count, v_item_row_count
    FROM mes_qa_inspection_regulation_item
   WHERE tenant_id = v_tenant_id
     AND regulation_version_id = v_regulation_version_id
     AND deleted = b'0';
  IF v_logical_item_count <> 22 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'COUNT(DISTINCT item_code) <> 22';
  END IF;
  IF v_item_row_count <> 64 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'COUNT(1) <> 64';
  END IF;
  IF (
    SELECT COUNT(DISTINCT qa_process_id)
      FROM mes_qa_inspection_regulation_item
     WHERE tenant_id = v_tenant_id
       AND regulation_version_id = v_regulation_version_id
       AND deleted = b'0'
  ) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'COUNT(DISTINCT qa_process_id) <> 3';
  END IF;

  UPDATE mes_qa_inspection_regulation
     SET current_version_id = v_regulation_version_id,
         updater = 'migration:20260812-qa-dcc-idi',
         update_time = NOW()
   WHERE id = v_regulation_id
     AND tenant_id = v_tenant_id
     AND deleted = b'0';

  DROP TEMPORARY TABLE tmp_mes_qa_idi_source_item_sort;
  DROP TEMPORARY TABLE tmp_mes_qa_idi_source_process;
  COMMIT;
END$$
DELIMITER ;

CALL seed_mes_qa_pressure_pump_idi();

DROP PROCEDURE IF EXISTS seed_mes_qa_pressure_pump_idi;
