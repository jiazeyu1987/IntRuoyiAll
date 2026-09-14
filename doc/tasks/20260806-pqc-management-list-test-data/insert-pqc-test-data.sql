SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @tenant_id := 1;
SET @admin_user_id := 1;
SET @pool_id := 37;
SET @creator := 'codex-pqc-test-data';
SET @marker := CONCAT('PQC_TEST_20260806_MGMT_LIST_', DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%f'));

DROP TEMPORARY TABLE IF EXISTS tmp_codex_assert;
CREATE TEMPORARY TABLE tmp_codex_assert (
  assertion_name VARCHAR(128) NOT NULL,
  value BIGINT NOT NULL
) ENGINE = MEMORY;

SELECT COUNT(*) INTO @admin_count
FROM system_users
WHERE id = @admin_user_id
  AND tenant_id = @tenant_id
  AND username = 'admin'
  AND deleted = b'0';

INSERT INTO tmp_codex_assert (assertion_name, value)
SELECT 'tenant_1_admin_user_required', NULL
WHERE @admin_count <> 1;

SELECT pp.work_order_id,
       pp.route_id,
       448,
       pp.route_process_id,
       pp.process_id,
       18,
       12,
       pp.device_id,
       pp.workstation_id,
       wo.code,
       wo.name,
       item.id,
       item.code,
       item.name,
       route.code,
       route.name,
       proc.code,
       proc.name,
       machinery.code,
       machinery.name,
       ws.code,
       ws.name
INTO @work_order_id,
     @route_id,
     @route_version_id,
     @route_process_id,
     @process_id,
     @regulation_version_id,
     @active_order_id,
     @device_id,
     @workstation_id,
     @work_order_code,
     @work_order_name,
     @product_id,
     @product_code,
     @product_name,
     @route_code,
     @route_name,
     @process_code,
     @process_name,
     @device_code,
     @device_name,
     @workstation_code,
     @workstation_name
FROM mes_pro_process_pool pp
JOIN mes_pro_work_order wo
  ON wo.id = pp.work_order_id
 AND wo.tenant_id = pp.tenant_id
 AND wo.deleted = b'0'
JOIN mes_md_item item
  ON item.id = wo.product_id
 AND item.tenant_id = pp.tenant_id
 AND item.deleted = b'0'
JOIN mes_pro_route route
  ON route.id = pp.route_id
 AND route.tenant_id = pp.tenant_id
 AND route.deleted = b'0'
JOIN mes_pro_process proc
  ON proc.id = pp.process_id
 AND proc.tenant_id = pp.tenant_id
 AND proc.deleted = b'0'
LEFT JOIN mes_dv_machinery machinery
  ON machinery.id = pp.device_id
 AND machinery.tenant_id = pp.tenant_id
 AND machinery.deleted = b'0'
LEFT JOIN mes_md_workstation ws
  ON ws.id = pp.workstation_id
 AND ws.tenant_id = pp.tenant_id
 AND ws.deleted = b'0'
WHERE pp.id = @pool_id
  AND pp.tenant_id = @tenant_id
  AND pp.deleted = b'0'
LIMIT 1;

SELECT IF(@work_order_id IS NULL, 1, 0) INTO @missing_pool;
INSERT INTO tmp_codex_assert (assertion_name, value)
SELECT 'process_pool_37_context_required', NULL
WHERE @missing_pool = 1;

SELECT id INTO @production_submit_event_id
FROM mes_pro_process_pool_event
WHERE tenant_id = @tenant_id
  AND deleted = b'0'
  AND pool_id = @pool_id
  AND event_type = 'PRODUCTION_SUBMIT'
ORDER BY server_submit_time DESC, id DESC
LIMIT 1;

SELECT IF(@production_submit_event_id IS NULL, 1, 0) INTO @missing_submit;
INSERT INTO tmp_codex_assert (assertion_name, value)
SELECT 'production_submit_event_required', NULL
WHERE @missing_submit = 1;

SET @round_no := CAST(DATE_FORMAT(CURDATE(), '%y%m%d') AS UNSIGNED);
SET @submit_time := NOW();
SET @signature_id := 990000000000000 + UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));

DROP TEMPORARY TABLE IF EXISTS tmp_codex_pqc_seq;
CREATE TEMPORARY TABLE tmp_codex_pqc_seq (
  n INT PRIMARY KEY
) ENGINE = MEMORY;

INSERT INTO tmp_codex_pqc_seq (n) VALUES
(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
(11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
(21),(22),(23),(24),(25),(26),(27),(28),(29),(30);

SELECT CAST(CONCAT('[', GROUP_CONCAT(JSON_QUOTE(IF(n = 18, '32.60', '32.50')) ORDER BY n SEPARATOR ','), ']') AS JSON)
INTO @length_samples
FROM tmp_codex_pqc_seq;

SELECT CAST(CONCAT('[', GROUP_CONCAT(JSON_QUOTE(IF(n = 12, '53.00', '50.00')) ORDER BY n SEPARATOR ','), ']') AS JSON)
INTO @pressure_samples
FROM tmp_codex_pqc_seq;

SELECT CAST(CONCAT('[', GROUP_CONCAT(JSON_QUOTE(IF(n = 12, '不合格', '合格')) ORDER BY n SEPARATOR ','), ']') AS JSON)
INTO @appearance_samples
FROM tmp_codex_pqc_seq;

INSERT INTO mes_pqc_inspection_task (
  active_order_id,
  work_order_id,
  route_id,
  route_version_id,
  route_process_id,
  process_id,
  regulation_version_id,
  inspection_type,
  business_date,
  shift_code,
  round_no,
  planned_inspection_quantity,
  actual_inspection_quantity,
  task_status,
  creator,
  create_time,
  updater,
  update_time,
  deleted,
  tenant_id
) VALUES (
  @active_order_id,
  @work_order_id,
  @route_id,
  @route_version_id,
  @route_process_id,
  @process_id,
  @regulation_version_id,
  'PATROL',
  CURDATE(),
  'DAY',
  @round_no,
  30,
  30,
  'SUBMITTED',
  @creator,
  @submit_time,
  @creator,
  @submit_time,
  b'0',
  @tenant_id
);

SET @pqc_task_id := LAST_INSERT_ID();

SET @length_item_code := CONCAT('PQC-TEST-LENGTH-', @pqc_task_id);
SET @pressure_item_code := CONCAT('PQC-TEST-PRESSURE-', @pqc_task_id);
SET @appearance_item_code := CONCAT('PQC-TEST-APPEARANCE-', @pqc_task_id);

SET @pqc_item_details := JSON_ARRAY(
  JSON_OBJECT(
    'itemCode', @length_item_code,
    'itemName', '测试-长度参数',
    'judgement', 'SUCCESS',
    'resultType', 'NUMBER',
    'sampleValues', JSON_EXTRACT(@length_samples, '$'),
    'standardText', '32.40 ~ 32.60 mm',
    'standardUnit', 'mm',
    'inspectionMethod', '卡尺逐件测量；测试数据用于PQC管理列表结构化展示',
    'standardPrecision', 2,
    'standardLowerLimit', 32.40,
    'standardUpperLimit', 32.60,
    'selectedEquipmentId', @device_id,
    'selectedEquipmentCode', @device_code,
    'selectedEquipmentName', @device_name,
    'selectedEquipmentNumber', @device_code
  ),
  JSON_OBJECT(
    'itemCode', @pressure_item_code,
    'itemName', '测试-压力参数',
    'judgement', 'FAILURE',
    'resultType', 'NUMBER',
    'sampleValues', JSON_EXTRACT(@pressure_samples, '$'),
    'standardText', '48.00 ~ 52.00 MPa',
    'standardUnit', 'MPa',
    'inspectionMethod', '压力表逐件测量；第12件故意超上限用于红色异常提醒',
    'standardPrecision', 2,
    'standardLowerLimit', 48.00,
    'standardUpperLimit', 52.00,
    'selectedEquipmentId', @device_id,
    'selectedEquipmentCode', @device_code,
    'selectedEquipmentName', @device_name,
    'selectedEquipmentNumber', @device_code
  ),
  JSON_OBJECT(
    'itemCode', @appearance_item_code,
    'itemName', '测试-外观判定',
    'judgement', 'FAILURE',
    'resultType', 'CHOICE',
    'sampleValues', JSON_EXTRACT(@appearance_samples, '$'),
    'standardText', '合格/不合格',
    'standardUnit', NULL,
    'inspectionMethod', '目视检查；第12件外观不合格计入损耗',
    'standardPrecision', NULL,
    'standardLowerLimit', NULL,
    'standardUpperLimit', NULL,
    'selectedEquipmentId', @device_id,
    'selectedEquipmentCode', @device_code,
    'selectedEquipmentName', @device_name,
    'selectedEquipmentNumber', @device_code
  )
);

SET @item_results := JSON_ARRAY(
  JSON_OBJECT('itemCode', @length_item_code, 'sampleValues', JSON_EXTRACT(@length_samples, '$'), 'selectedEquipmentId', @device_id, 'selectedEquipmentNumber', @device_code),
  JSON_OBJECT('itemCode', @pressure_item_code, 'sampleValues', JSON_EXTRACT(@pressure_samples, '$'), 'selectedEquipmentId', @device_id, 'selectedEquipmentNumber', @device_code),
  JSON_OBJECT('itemCode', @appearance_item_code, 'sampleValues', JSON_EXTRACT(@appearance_samples, '$'), 'selectedEquipmentId', @device_id, 'selectedEquipmentNumber', @device_code)
);

SET @payload := JSON_OBJECT(
  'testMarker', @marker,
  'roundNo', @round_no,
  'routeId', @route_id,
  'pqcDraft', JSON_OBJECT(
    'patrolRound', @round_no,
    'scrapQuantity', 1,
    'lossQuantity', 1,
    'inspectionType', 'PATROL',
    'inspectionQuantity', 30,
    'defectDescription', '外观不合格1件'
  ),
  'pqcTaskId', @pqc_task_id,
  'processId', @process_id,
  'shiftCode', 'DAY',
  'fieldValues', JSON_OBJECT(
    'PQC_RESULT', 'DETECTION_FAILED',
    'SCRAP_QUANTITY', 1
  ),
  'itemResults', JSON_EXTRACT(@item_results, '$'),
  'workOrderId', @work_order_id,
  'businessDate', JSON_ARRAY(YEAR(CURDATE()), MONTH(CURDATE()), DAY(CURDATE())),
  'activeOrderId', @active_order_id,
  'inspectionType', 'PATROL',
  'inspectionQuantity', 30,
  'actualInspectionQuantity', 30,
  'scrapQuantity', 1,
  'lossQuantity', 1,
  'defectDescription', '外观不合格1件',
  'nonconformanceDescription', '外观不合格1件；压力第12件超上限',
  'lossReasonDetails', JSON_ARRAY(
    JSON_OBJECT('reasonCode', 'PQC_APPEARANCE_NG', 'reasonName', '外观不合格', 'quantity', 1)
  ),
  'defectReasonDetails', JSON_ARRAY(
    JSON_OBJECT('reasonCode', 'PQC_APPEARANCE_NG', 'reasonName', '外观不合格', 'quantity', 1)
  ),
  'pqcItemDetails', JSON_EXTRACT(@pqc_item_details, '$'),
  'pqcPieceValues', JSON_OBJECT(
    @length_item_code, JSON_EXTRACT(@length_samples, '$'),
    @pressure_item_code, JSON_EXTRACT(@pressure_samples, '$'),
    @appearance_item_code, JSON_EXTRACT(@appearance_samples, '$')
  ),
  'routeProcessId', @route_process_id,
  'selectedProcess', JSON_OBJECT(
    'sort', 3,
    'roundNo', @round_no,
    'routeId', @route_id,
    'deviceId', @device_id,
    'pqcTaskId', @pqc_task_id,
    'processId', @process_id,
    'routeCode', @route_code,
    'routeName', @route_name,
    'shiftCode', 'DAY',
    'deviceCode', @device_code,
    'deviceName', @device_name,
    'processCode', @process_code,
    'processName', @process_name,
    'businessDate', JSON_ARRAY(YEAR(CURDATE()), MONTH(CURDATE()), DAY(CURDATE())),
    'activeOrderId', @active_order_id,
    'workstationId', @workstation_id,
    'inspectionType', 'PATROL',
    'routeProcessId', @route_process_id,
    'inspectionItems', JSON_EXTRACT(@pqc_item_details, '$'),
    'workstationCode', @workstation_code,
    'workstationName', @workstation_name,
    'regulationVersionId', @regulation_version_id,
    'plannedInspectionQuantity', 30
  ),
  'templatePayload', JSON_OBJECT(
    'routeId', @route_id,
    'processId', @process_id,
    'fieldValues', JSON_OBJECT('PQC_RESULT', 'DETECTION_FAILED'),
    'workOrderId', @work_order_id,
    'templateCode', 'PQC_SIMPLIFIED',
    'routeProcessId', @route_process_id,
    'actualEmployeeId', @admin_user_id
  ),
  'inspectionResult', 'DETECTION_FAILED',
  'pieceDetailCount', 90,
  'selectedEmployee', JSON_OBJECT(
    'userId', @admin_user_id,
    'nickname', '瑛泰管理员',
    'username', 'admin'
  ),
  'regulationVersionId', @regulation_version_id,
  'selectedActiveOrder', JSON_OBJECT(
    'routeId', @route_id,
    'productId', @product_id,
    'routeCode', @route_code,
    'routeName', @route_name,
    'productCode', @product_code,
    'productName', @product_name,
    'workOrderId', @work_order_id,
    'workOrderCode', @work_order_code,
    'workOrderName', @work_order_name,
    'latestSubmitTime', UNIX_TIMESTAMP(@submit_time) * 1000
  ),
  'actualInspectionQuantity', 30
);

INSERT INTO mes_pro_process_pool_event (
  pool_id,
  event_type,
  event_idempotency_key,
  work_order_id,
  route_id,
  route_process_id,
  process_id,
  actual_employee_id,
  device_account_id,
  device_id,
  workstation_id,
  template_type,
  feedback_source_type,
  feedback_source_id,
  recordbook_entry_id,
  recordbook_source_type,
  recordbook_source_id,
  raw_payload,
  server_submit_time,
  signature_id,
  signature_user_id,
  signature_snapshot,
  creator,
  create_time,
  updater,
  update_time,
  deleted,
  tenant_id
) VALUES (
  @pool_id,
  'PQC_INSPECTION',
  @marker,
  @work_order_id,
  @route_id,
  @route_process_id,
  @process_id,
  @admin_user_id,
  NULL,
  @device_id,
  @workstation_id,
  'PQC_SIMPLIFIED',
  'PQC_TASK',
  @pqc_task_id,
  NULL,
  'PQC_TASK',
  @pqc_task_id,
  @payload,
  @submit_time,
  @signature_id,
  @admin_user_id,
  JSON_OBJECT('testMarker', @marker, 'signatureUserId', @admin_user_id, 'source', 'codex-pqc-management-list-test-data'),
  @creator,
  @submit_time,
  @creator,
  @submit_time,
  b'0',
  @tenant_id
);

SET @event_id := LAST_INSERT_ID();

INSERT INTO mes_pro_process_pool_pqc_record (
  pool_id,
  event_id,
  production_submit_event_id,
  work_order_id,
  route_id,
  route_process_id,
  process_id,
  actual_employee_id,
  signature_id,
  signature_user_id,
  inspection_result,
  server_submit_time,
  raw_payload,
  process_inspection_aggregation_status,
  process_inspection_review_id,
  process_inspection_aggregated_at,
  creator,
  create_time,
  updater,
  update_time,
  deleted,
  tenant_id
) VALUES (
  @pool_id,
  @event_id,
  @production_submit_event_id,
  @work_order_id,
  @route_id,
  @route_process_id,
  @process_id,
  @admin_user_id,
  @signature_id,
  @admin_user_id,
  'FAILURE',
  @submit_time,
  @payload,
  'PENDING',
  NULL,
  NULL,
  @creator,
  @submit_time,
  @creator,
  @submit_time,
  b'0',
  @tenant_id
);

SET @pqc_record_id := LAST_INSERT_ID();

INSERT INTO mes_pqc_inspection_piece_detail (
  task_id,
  sample_no,
  item_code,
  item_name,
  inspection_method,
  standard_text,
  selected_equipment_id,
  selected_equipment_code,
  selected_equipment_name,
  selected_equipment_number,
  standard_lower_limit,
  standard_upper_limit,
  standard_unit,
  standard_precision,
  result_type,
  item_result,
  measured_value,
  judgement,
  creator,
  create_time,
  updater,
  update_time,
  deleted,
  tenant_id
)
SELECT @pqc_task_id,
       n,
       @length_item_code,
       '测试-长度参数',
       '卡尺逐件测量；测试数据用于PQC管理列表结构化展示',
       '32.40 ~ 32.60 mm',
       @device_id,
       @device_code,
       @device_name,
       @device_code,
       32.40,
       32.60,
       'mm',
       2,
       'NUMBER',
       IF(n = 18, '32.60', '32.50'),
       IF(n = 18, '32.60', '32.50'),
       'SUCCESS',
       @creator,
       @submit_time,
       @creator,
       @submit_time,
       b'0',
       @tenant_id
FROM tmp_codex_pqc_seq seq
ORDER BY n;

INSERT INTO mes_pqc_inspection_piece_detail (
  task_id,
  sample_no,
  item_code,
  item_name,
  inspection_method,
  standard_text,
  selected_equipment_id,
  selected_equipment_code,
  selected_equipment_name,
  selected_equipment_number,
  standard_lower_limit,
  standard_upper_limit,
  standard_unit,
  standard_precision,
  result_type,
  item_result,
  measured_value,
  judgement,
  creator,
  create_time,
  updater,
  update_time,
  deleted,
  tenant_id
)
SELECT @pqc_task_id,
       n,
       @pressure_item_code,
       '测试-压力参数',
       '压力表逐件测量；第12件故意超上限用于红色异常提醒',
       '48.00 ~ 52.00 MPa',
       @device_id,
       @device_code,
       @device_name,
       @device_code,
       48.00,
       52.00,
       'MPa',
       2,
       'NUMBER',
       IF(n = 12, '53.00', '50.00'),
       IF(n = 12, '53.00', '50.00'),
       IF(n = 12, 'FAILURE', 'SUCCESS'),
       @creator,
       @submit_time,
       @creator,
       @submit_time,
       b'0',
       @tenant_id
FROM tmp_codex_pqc_seq
ORDER BY n;

INSERT INTO mes_pqc_inspection_piece_detail (
  task_id,
  sample_no,
  item_code,
  item_name,
  inspection_method,
  standard_text,
  selected_equipment_id,
  selected_equipment_code,
  selected_equipment_name,
  selected_equipment_number,
  standard_lower_limit,
  standard_upper_limit,
  standard_unit,
  standard_precision,
  result_type,
  item_result,
  measured_value,
  judgement,
  creator,
  create_time,
  updater,
  update_time,
  deleted,
  tenant_id
)
SELECT @pqc_task_id,
       n,
       @appearance_item_code,
       '测试-外观判定',
       '目视检查；第12件外观不合格计入损耗',
       '合格/不合格',
       @device_id,
       @device_code,
       @device_name,
       @device_code,
       NULL,
       NULL,
       NULL,
       NULL,
       'CHOICE',
       IF(n = 12, '不合格', '合格'),
       IF(n = 12, '不合格', '合格'),
       IF(n = 12, 'FAILURE', 'SUCCESS'),
       @creator,
       @submit_time,
       @creator,
       @submit_time,
       b'0',
       @tenant_id
FROM tmp_codex_pqc_seq
ORDER BY n;

SELECT COUNT(*) INTO @piece_detail_count
FROM mes_pqc_inspection_piece_detail
WHERE tenant_id = @tenant_id
  AND deleted = b'0'
  AND task_id = @pqc_task_id;

SET @bad_piece_count_message := 'PQC piece detail insert count mismatch; expected 90 rows.';
SELECT IF(@piece_detail_count = 90, 0, 1) INTO @bad_piece_count;
INSERT INTO tmp_codex_assert (assertion_name, value)
SELECT 'pqc_piece_detail_count_must_equal_90', NULL
WHERE @bad_piece_count = 1;

COMMIT;

SELECT @marker AS test_marker,
       @pqc_task_id AS pqc_task_id,
       @event_id AS event_id,
       @pqc_record_id AS pqc_record_id,
       @signature_id AS signature_id,
       @piece_detail_count AS piece_detail_count,
       @production_submit_event_id AS production_submit_event_id;
