SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @tenant_id := 1;
SET @event_id := 160;
SET @pqc_record_id := 103;
SET @pqc_task_id := 189;
SET @admin_user_id := 1;
SET @creator := 'codex-pqc-test-data';

DROP TEMPORARY TABLE IF EXISTS tmp_codex_assert;
CREATE TEMPORARY TABLE tmp_codex_assert (
  assertion_name VARCHAR(128) NOT NULL,
  value BIGINT NOT NULL
) ENGINE = MEMORY;

SELECT event_idempotency_key,
       pool_id,
       work_order_id,
       route_id,
       route_process_id,
       process_id,
       device_id,
       workstation_id,
       server_submit_time,
       signature_id,
       signature_user_id
INTO @marker,
     @pool_id,
     @work_order_id,
     @route_id,
     @route_process_id,
     @process_id,
     @device_id,
     @workstation_id,
     @submit_time,
     @signature_id,
     @signature_user_id
FROM mes_pro_process_pool_event
WHERE id = @event_id
  AND tenant_id = @tenant_id
  AND event_idempotency_key LIKE 'PQC_TEST_20260806_MGMT_LIST_%'
  AND deleted = b'0'
LIMIT 1;

INSERT INTO tmp_codex_assert (assertion_name, value)
SELECT 'target_event_160_required', NULL
WHERE @marker IS NULL;

SELECT t.active_order_id,
       t.route_version_id,
       t.regulation_version_id,
       t.round_no
INTO @active_order_id,
     @route_version_id,
     @regulation_version_id,
     @round_no
FROM mes_pqc_inspection_task t
WHERE t.id = @pqc_task_id
  AND t.tenant_id = @tenant_id
  AND t.deleted = b'0'
LIMIT 1;

INSERT INTO tmp_codex_assert (assertion_name, value)
SELECT 'target_pqc_task_189_required', NULL
WHERE @active_order_id IS NULL;

SELECT wo.code,
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
INTO @work_order_code,
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
FROM mes_pro_work_order wo
JOIN mes_md_item item
  ON item.id = wo.product_id
 AND item.tenant_id = wo.tenant_id
 AND item.deleted = b'0'
JOIN mes_pro_route route
  ON route.id = @route_id
 AND route.tenant_id = wo.tenant_id
 AND route.deleted = b'0'
JOIN mes_pro_process proc
  ON proc.id = @process_id
 AND proc.tenant_id = wo.tenant_id
 AND proc.deleted = b'0'
LEFT JOIN mes_dv_machinery machinery
  ON machinery.id = @device_id
 AND machinery.tenant_id = wo.tenant_id
 AND machinery.deleted = b'0'
LEFT JOIN mes_md_workstation ws
  ON ws.id = @workstation_id
 AND ws.tenant_id = wo.tenant_id
 AND ws.deleted = b'0'
WHERE wo.id = @work_order_id
  AND wo.tenant_id = @tenant_id
  AND wo.deleted = b'0'
LIMIT 1;

INSERT INTO tmp_codex_assert (assertion_name, value)
SELECT 'work_order_context_required', NULL
WHERE @work_order_code IS NULL;

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
  'fieldValues', JSON_OBJECT('PQC_RESULT', 'DETECTION_FAILED', 'SCRAP_QUANTITY', 1),
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
  'lossReasonDetails', JSON_ARRAY(JSON_OBJECT('reasonCode', 'PQC_APPEARANCE_NG', 'reasonName', '外观不合格', 'quantity', 1)),
  'defectReasonDetails', JSON_ARRAY(JSON_OBJECT('reasonCode', 'PQC_APPEARANCE_NG', 'reasonName', '外观不合格', 'quantity', 1)),
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
  'selectedEmployee', JSON_OBJECT('userId', @admin_user_id, 'nickname', '瑛泰管理员', 'username', 'admin'),
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

UPDATE mes_pro_process_pool_event
SET raw_payload = @payload,
    updater = @creator,
    update_time = NOW()
WHERE id = @event_id
  AND tenant_id = @tenant_id
  AND event_idempotency_key = @marker
  AND deleted = b'0';

UPDATE mes_pro_process_pool_pqc_record
SET raw_payload = @payload,
    updater = @creator,
    update_time = NOW()
WHERE id = @pqc_record_id
  AND event_id = @event_id
  AND tenant_id = @tenant_id
  AND deleted = b'0';

COMMIT;

SELECT @marker AS test_marker,
       @event_id AS event_id,
       JSON_TYPE(JSON_EXTRACT(@payload, '$.pqcItemDetails')) AS item_details_type,
       JSON_LENGTH(@payload, '$.pqcItemDetails') AS item_details_count,
       JSON_LENGTH(@payload, CONCAT('$.pqcPieceValues."', @pressure_item_code, '"')) AS pressure_sample_count,
       JSON_UNQUOTE(JSON_EXTRACT(@payload, '$.pqcItemDetails[1].sampleValues[11]')) AS pressure_sample_12;
