-- M6 local runtime formal QA/PQC fixture for RRM-20260801.
-- Scope: local tenant 1, pressure-pump route 922119 / version 448, work order 980008 only.
-- Purpose: make the M3 formal QA regulation and PQC task model executable in real E2E.
-- This is task-owned local test data. It does not add a runtime fallback to the M0 QC template.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_rrm_assert;
DELIMITER $$
CREATE PROCEDURE codex_rrm_assert(IN p_condition tinyint, IN p_message varchar(255))
BEGIN
    IF p_condition = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_message;
    END IF;
END$$
DELIMITER ;

SET @rrm_tenant_id := 1;
SET @rrm_product_id := 902149;
SET @rrm_work_order_id := 980008;
SET @rrm_route_id := 922119;
SET @rrm_route_version_id := 448;
SET @rrm_active_order_id := 12;
SET @rrm_source_template_id := 6;
SET @rrm_actor := 'codex-rrm-m6';
SET @rrm_business_date := CURDATE();
SET @rrm_shift_code := 'DAY';
SET @rrm_round_no := 1;
SET @rrm_default_first_qty := 5;
SET @rrm_default_patrol_ratio := 0.05;

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_work_order
    WHERE id = @rrm_work_order_id
      AND product_id = @rrm_product_id
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM work order fixture is missing or product mismatched');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_process_pool_active_order
    WHERE id = @rrm_active_order_id
      AND work_order_id = @rrm_work_order_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND active_status IN ('ACTIVE', 'REMOVED')
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM active order fixture is missing, cleaned up, or route version mismatched');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 14
    FROM mes_pro_route_process
    WHERE route_id = @rrm_route_id
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM current route V21 must have exactly 14 active processes');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 49
    FROM mes_qc_template_indicator
    WHERE template_id = @rrm_source_template_id
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM M0 derived QC template must have exactly 49 source method rows');

DROP TEMPORARY TABLE IF EXISTS tmp_rrm_current_route_process;
CREATE TEMPORARY TABLE tmp_rrm_current_route_process (
    route_process_id bigint NOT NULL PRIMARY KEY,
    process_id bigint NOT NULL,
    process_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    product_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    route_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    route_version_no varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    batch_record_report_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    batch_record_report_name varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sort_no int NOT NULL,
    source_process varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    source_mode varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_rrm_current_route_process (
    route_process_id, process_id, process_name, product_name, route_name, route_version_no,
    batch_record_report_id, batch_record_report_name, sort_no, source_process, source_mode
)
SELECT
    rp.id,
    rp.process_id,
    p.name,
    br.batch_record_name,
    route.name,
    route_version.version_no,
    rp.batch_record_report_id,
    COALESCE(NULLIF(br.report_name, ''), NULLIF(br.batch_record_name, ''), br.report_id),
    rp.sort,
    CASE p.name
        WHEN '粗洗工序' THEN '清洗'
        WHEN '精洗工序' THEN '精洗'
        WHEN '清洗工序' THEN '清洗'
        WHEN '清洁工序' THEN '清洁'
        WHEN '组装Ⅰ工序' THEN '组装I'
        WHEN '光固Ⅰ工序' THEN '光固'
        WHEN '硅化Ⅰ工序' THEN '硅化I'
        WHEN '硅化Ⅱ工序' THEN '硅化Ⅱ、Ⅲ'
        WHEN '组装Ⅱ工序' THEN '组装II'
        WHEN '检测工序' THEN '检测'
        WHEN '光固Ⅱ工序' THEN '光固'
        WHEN '单包装工序' THEN '包装'
        WHEN '中包装工序' THEN '包装'
        WHEN '大包装工序' THEN '包装'
        ELSE ''
    END,
    CASE
        WHEN p.name IN ('单包装工序', '中包装工序', '大包装工序') THEN 'LOCAL_PACKAGING_FIXTURE'
        ELSE 'M0_DERIVED_PROCESS_INSPECTION'
    END
FROM mes_pro_route_process rp
JOIN mes_pro_process p
  ON p.id = rp.process_id
 AND p.deleted = b'0'
 AND p.tenant_id = rp.tenant_id
JOIN mes_pro_route route
  ON route.id = rp.route_id
 AND route.deleted = b'0'
 AND route.tenant_id = rp.tenant_id
JOIN mes_pro_route_version route_version
  ON route_version.id = @rrm_route_version_id
 AND route_version.route_id = rp.route_id
 AND route_version.deleted = b'0'
 AND route_version.tenant_id = rp.tenant_id
JOIN mes_pro_batch_record_report br
  ON br.report_id = rp.batch_record_report_id
 AND br.deleted = b'0'
 AND br.tenant_id = rp.tenant_id
WHERE rp.route_id = @rrm_route_id
  AND rp.deleted = b'0'
  AND rp.tenant_id = @rrm_tenant_id;

CALL codex_rrm_assert((
    SELECT COUNT(*) = 14
    FROM tmp_rrm_current_route_process
    WHERE source_process <> ''
), 'RRM route process to inspection source mapping is incomplete');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 14
    FROM tmp_rrm_current_route_process
    WHERE batch_record_report_id <> ''
      AND batch_record_report_name <> ''
), 'Every RRM route process must have a formal batch-record report binding snapshot source');

DROP TEMPORARY TABLE IF EXISTS tmp_rrm_source_method;
CREATE TEMPORARY TABLE tmp_rrm_source_method (
    source_process varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    source_item_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    source_item_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    inspection_type varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    inspection_method varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    standard_text varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    result_type varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    first_qty int DEFAULT NULL,
    patrol_ratio decimal(18,6) DEFAULT NULL,
    source_sort int NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_rrm_source_method (
    source_process, source_item_code, source_item_name, inspection_type,
    inspection_method, standard_text, result_type, first_qty, patrol_ratio, source_sort
)
SELECT
    SUBSTRING_INDEX(SUBSTRING_INDEX(ti.remark, 'sourceProcess=', -1), ';', 1),
    ind.code,
    ind.name,
    CASE
        WHEN ti.check_method LIKE '首检%' THEN 'FIRST'
        WHEN ti.check_method LIKE '抽检%' THEN 'PATROL'
        ELSE 'PATROL'
    END,
    LEFT(CONCAT(ti.check_method, '；正式QA规程来源=M0派生过程检验记录V3.0'), 512),
    COALESCE(NULLIF(ind.result_specification, ''), '符合/不符合'),
    'CHOICE',
    CAST(NULLIF(SUBSTRING_INDEX(SUBSTRING_INDEX(ti.remark, 'tempFirstQty=', -1), ';', 1), '') AS UNSIGNED),
    CAST(NULLIF(SUBSTRING_INDEX(SUBSTRING_INDEX(ti.remark, 'tempPatrolCoeff=', -1), ';', 1), '') AS DECIMAL(18,6)),
    ti.id
FROM mes_qc_template_indicator ti
JOIN mes_qc_indicator ind
  ON ind.id = ti.indicator_id
 AND ind.deleted = b'0'
 AND ind.tenant_id = ti.tenant_id
WHERE ti.template_id = @rrm_source_template_id
  AND ti.deleted = b'0'
  AND ti.tenant_id = @rrm_tenant_id
  AND SUBSTRING_INDEX(SUBSTRING_INDEX(ti.remark, 'sourceProcess=', -1), ';', 1) <> '组装Ⅲ';

CALL codex_rrm_assert((
    SELECT COUNT(*) = 39
    FROM tmp_rrm_source_method
), 'RRM derived source methods excluding unmatched assembly III must have exactly 39 rows');

DROP TEMPORARY TABLE IF EXISTS tmp_rrm_method_seed;
CREATE TEMPORARY TABLE tmp_rrm_method_seed (
    route_process_id bigint NOT NULL,
    process_id bigint NOT NULL,
    process_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sort_no int NOT NULL,
    source_mode varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    item_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    item_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    inspection_type varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    inspection_method varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    standard_text varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    result_type varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    first_qty int DEFAULT NULL,
    patrol_ratio decimal(18,6) DEFAULT NULL,
    source_sort int NOT NULL,
    UNIQUE KEY uk_tmp_rrm_method_seed (route_process_id, inspection_type, item_code)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_rrm_method_seed (
    route_process_id, process_id, process_name, sort_no, source_mode,
    item_code, item_name, inspection_type, inspection_method, standard_text,
    result_type, first_qty, patrol_ratio, source_sort
)
SELECT
    rp.route_process_id,
    rp.process_id,
    rp.process_name,
    rp.sort_no,
    rp.source_mode,
    CONCAT(sm.source_item_code, '-RP', rp.route_process_id),
    sm.source_item_name,
    sm.inspection_type,
    sm.inspection_method,
    sm.standard_text,
    sm.result_type,
    COALESCE(sm.first_qty, @rrm_default_first_qty),
    sm.patrol_ratio,
    sm.source_sort
FROM tmp_rrm_current_route_process rp
JOIN tmp_rrm_source_method sm
  ON sm.source_process = rp.source_process
WHERE rp.source_mode = 'M0_DERIVED_PROCESS_INSPECTION';

INSERT INTO tmp_rrm_method_seed (
    route_process_id, process_id, process_name, sort_no, source_mode,
    item_code, item_name, inspection_type, inspection_method, standard_text,
    result_type, first_qty, patrol_ratio, source_sort
)
SELECT
    rp.route_process_id,
    rp.process_id,
    rp.process_name,
    rp.sort_no,
    rp.source_mode,
    CONCAT('RRM-PPV21-QA-PACK-', LPAD(rp.sort_no, 2, '0'), '-RP', rp.route_process_id),
    CONCAT(REPLACE(rp.process_name, '工序', ''), '-包装外观与记录完整性-抽检'),
    'PATROL',
    CONCAT('抽检；项目=包装外观、标识和批记录完整性；设备=目测；默认首检数量=',
           @rrm_default_first_qty, '；巡检系数=', @rrm_default_patrol_ratio,
           '；来源=V21包装工序本地测试夹具，源过程检验记录无精确包装行'),
    '符合/不符合',
    'CHOICE',
    @rrm_default_first_qty,
    @rrm_default_patrol_ratio,
    9000 + rp.sort_no
FROM tmp_rrm_current_route_process rp
WHERE rp.source_mode = 'LOCAL_PACKAGING_FIXTURE';

DROP TEMPORARY TABLE IF EXISTS tmp_rrm_missing_first_route_process;
CREATE TEMPORARY TABLE tmp_rrm_missing_first_route_process
ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
AS
SELECT rp.*
FROM tmp_rrm_current_route_process rp
WHERE NOT EXISTS (
    SELECT 1
    FROM tmp_rrm_method_seed existing
    WHERE existing.route_process_id = rp.route_process_id
      AND existing.inspection_type = 'FIRST'
);

INSERT INTO tmp_rrm_method_seed (
    route_process_id, process_id, process_name, sort_no, source_mode,
    item_code, item_name, inspection_type, inspection_method, standard_text,
    result_type, first_qty, patrol_ratio, source_sort
)
SELECT
    rp.route_process_id,
    rp.process_id,
    rp.process_name,
    rp.sort_no,
    rp.source_mode,
    CONCAT('RRM-PPV21-QA-FIRST-', LPAD(rp.sort_no, 2, '0'), '-RP', rp.route_process_id),
    CONCAT(REPLACE(rp.process_name, '工序', ''), '-默认首检规则'),
    'FIRST',
    CONCAT('首检；默认首检数量=', @rrm_default_first_qty,
           '；来源=V21工序本地测试夹具补齐，正式方法仍来自过程检验记录优先'),
    '符合/不符合',
    'CHOICE',
    @rrm_default_first_qty,
    NULL,
    9100 + rp.sort_no
FROM tmp_rrm_missing_first_route_process rp;

DROP TEMPORARY TABLE IF EXISTS tmp_rrm_missing_final_route_process;
CREATE TEMPORARY TABLE tmp_rrm_missing_final_route_process
ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
AS
SELECT rp.*
FROM tmp_rrm_current_route_process rp
WHERE NOT EXISTS (
    SELECT 1
    FROM tmp_rrm_method_seed existing
    WHERE existing.route_process_id = rp.route_process_id
      AND existing.inspection_type = 'FINAL'
);

INSERT INTO tmp_rrm_method_seed (
    route_process_id, process_id, process_name, sort_no, source_mode,
    item_code, item_name, inspection_type, inspection_method, standard_text,
    result_type, first_qty, patrol_ratio, source_sort
)
SELECT
    rp.route_process_id,
    rp.process_id,
    rp.process_name,
    rp.sort_no,
    rp.source_mode,
    CONCAT('RRM-PPV21-QA-FINAL-', LPAD(rp.sort_no, 2, '0'), '-RP', rp.route_process_id),
    CONCAT(REPLACE(rp.process_name, '工序', ''), '-末检适用性确认'),
    'FINAL',
    CONCAT('末检；适用性=需要；来源=V21工序本地测试夹具补齐，正式发布版本必须显式记录末检规则'),
    '符合/不符合',
    'CHOICE',
    NULL,
    NULL,
    9200 + rp.sort_no
FROM tmp_rrm_missing_final_route_process rp;

CALL codex_rrm_assert((
    SELECT COUNT(DISTINCT route_process_id) = 14
    FROM tmp_rrm_method_seed
    WHERE inspection_type = 'FIRST'
), 'Every RRM route process must have at least one FIRST QA method');

CALL codex_rrm_assert((
    SELECT COUNT(DISTINCT route_process_id) = 14
    FROM tmp_rrm_method_seed
    WHERE inspection_type = 'PATROL'
), 'Every RRM route process must have at least one PATROL QA method');

CALL codex_rrm_assert((
    SELECT COUNT(DISTINCT route_process_id) = 14
    FROM tmp_rrm_method_seed
    WHERE inspection_type = 'FINAL'
), 'Every RRM route process must have at least one FINAL QA method');

START TRANSACTION;

INSERT INTO mes_qa_inspection_regulation (
    product_id, route_id, route_version_id, route_process_id, process_id,
    owner_module, regulation_code, regulation_name, lifecycle_status,
    current_version_id, creator, updater, tenant_id
)
SELECT
    @rrm_product_id,
    @rrm_route_id,
    @rrm_route_version_id,
    rp.route_process_id,
    rp.process_id,
    'RRM_QA',
    CONCAT('RRM-20260801-QA-REG-PP-V21-RP', rp.route_process_id),
    CONCAT('RRM-20260801 球囊扩张压力泵V21 ', rp.process_name, ' QA检验规程'),
    'PUBLISHED',
    NULL,
    @rrm_actor,
    @rrm_actor,
    @rrm_tenant_id
FROM tmp_rrm_current_route_process rp
WHERE NOT EXISTS (
    SELECT 1
    FROM mes_qa_inspection_regulation existing
    WHERE existing.product_id = @rrm_product_id
      AND existing.route_id = @rrm_route_id
      AND existing.route_version_id = @rrm_route_version_id
      AND existing.route_process_id = rp.route_process_id
      AND existing.deleted = b'0'
      AND existing.tenant_id = @rrm_tenant_id
);

CALL codex_rrm_assert((
    SELECT COUNT(*) = 14
    FROM mes_qa_inspection_regulation
    WHERE product_id = @rrm_product_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND lifecycle_status = 'PUBLISHED'
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM formal QA regulation master rows were not created for all 14 processes');

INSERT INTO mes_qa_inspection_regulation_version (
    regulation_id, version_no, lifecycle_status, published_at, snapshot_json,
    creator, updater, tenant_id
)
SELECT
    reg.id,
    'V1',
    'PUBLISHED',
    NOW(),
    JSON_OBJECT(
        'fixture', 'RRM-20260801-M6',
        'productName', rp.product_name,
        'routeId', @rrm_route_id,
        'routeName', rp.route_name,
        'routeVersionId', @rrm_route_version_id,
        'routeVersionNo', rp.route_version_no,
        'routeProcessId', reg.route_process_id,
        'routeProcessName', rp.process_name,
        'processId', reg.process_id,
        'sourceMode', rp.source_mode,
        'sourceTemplateId', @rrm_source_template_id,
        'batchRecordReports', JSON_ARRAY(JSON_OBJECT(
            'batchRecordReportId', rp.batch_record_report_id,
            'batchRecordReportName', rp.batch_record_report_name,
            'sourceTable', 'mes_pro_route_process.batch_record_report_id',
            'routeProcessId', rp.route_process_id
        ))
    ),
    @rrm_actor,
    @rrm_actor,
    @rrm_tenant_id
FROM mes_qa_inspection_regulation reg
JOIN tmp_rrm_current_route_process rp
  ON rp.route_process_id = reg.route_process_id
WHERE reg.product_id = @rrm_product_id
  AND reg.route_id = @rrm_route_id
  AND reg.route_version_id = @rrm_route_version_id
  AND reg.deleted = b'0'
  AND reg.tenant_id = @rrm_tenant_id
  AND NOT EXISTS (
      SELECT 1
      FROM mes_qa_inspection_regulation_version existing
      WHERE existing.regulation_id = reg.id
        AND existing.version_no = 'V1'
        AND existing.deleted = b'0'
      AND existing.tenant_id = @rrm_tenant_id
  );

UPDATE mes_qa_inspection_regulation_version ver
JOIN mes_qa_inspection_regulation reg
  ON reg.id = ver.regulation_id
 AND reg.deleted = b'0'
 AND reg.tenant_id = ver.tenant_id
JOIN tmp_rrm_current_route_process rp
  ON rp.route_process_id = reg.route_process_id
SET ver.snapshot_json = JSON_OBJECT(
        'fixture', 'RRM-20260801-M6',
        'productName', rp.product_name,
        'routeId', @rrm_route_id,
        'routeName', rp.route_name,
        'routeVersionId', @rrm_route_version_id,
        'routeVersionNo', rp.route_version_no,
        'routeProcessId', reg.route_process_id,
        'routeProcessName', rp.process_name,
        'processId', reg.process_id,
        'sourceMode', rp.source_mode,
        'sourceTemplateId', @rrm_source_template_id,
        'batchRecordReports', JSON_ARRAY(JSON_OBJECT(
            'batchRecordReportId', rp.batch_record_report_id,
            'batchRecordReportName', rp.batch_record_report_name,
            'sourceTable', 'mes_pro_route_process.batch_record_report_id',
            'routeProcessId', rp.route_process_id
        ))
    ),
    ver.updater = @rrm_actor
WHERE reg.product_id = @rrm_product_id
  AND reg.route_id = @rrm_route_id
  AND reg.route_version_id = @rrm_route_version_id
  AND ver.version_no = 'V1'
  AND ver.lifecycle_status = 'PUBLISHED'
  AND ver.deleted = b'0'
  AND ver.tenant_id = @rrm_tenant_id;

UPDATE mes_qa_inspection_regulation reg
JOIN mes_qa_inspection_regulation_version ver
  ON ver.regulation_id = reg.id
 AND ver.version_no = 'V1'
 AND ver.lifecycle_status = 'PUBLISHED'
 AND ver.deleted = b'0'
 AND ver.tenant_id = reg.tenant_id
SET reg.current_version_id = ver.id,
    reg.lifecycle_status = 'PUBLISHED',
    reg.updater = @rrm_actor
WHERE reg.product_id = @rrm_product_id
  AND reg.route_id = @rrm_route_id
  AND reg.route_version_id = @rrm_route_version_id
  AND reg.deleted = b'0'
  AND reg.tenant_id = @rrm_tenant_id;

CALL codex_rrm_assert((
    SELECT COUNT(*) = 14
    FROM mes_qa_inspection_regulation
    WHERE product_id = @rrm_product_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND lifecycle_status = 'PUBLISHED'
      AND current_version_id IS NOT NULL
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM formal QA regulation current published versions were not frozen');

INSERT INTO mes_qa_inspection_regulation_item (
    regulation_version_id, inspection_type, item_code, item_name, inspection_method,
    standard_text, result_type, first_inspection_quantity, patrol_inspection_ratio,
    creator, updater, tenant_id
)
SELECT
    reg.current_version_id,
    seed.inspection_type,
    seed.item_code,
    seed.item_name,
    seed.inspection_method,
    seed.standard_text,
    seed.result_type,
    seed.first_qty,
    seed.patrol_ratio,
    @rrm_actor,
    @rrm_actor,
    @rrm_tenant_id
FROM tmp_rrm_method_seed seed
JOIN mes_qa_inspection_regulation reg
  ON reg.route_process_id = seed.route_process_id
 AND reg.process_id = seed.process_id
 AND reg.product_id = @rrm_product_id
 AND reg.route_id = @rrm_route_id
 AND reg.route_version_id = @rrm_route_version_id
 AND reg.current_version_id IS NOT NULL
 AND reg.deleted = b'0'
 AND reg.tenant_id = @rrm_tenant_id
WHERE NOT EXISTS (
    SELECT 1
    FROM mes_qa_inspection_regulation_item existing
    WHERE existing.regulation_version_id = reg.current_version_id
      AND existing.inspection_type = seed.inspection_type
      AND existing.item_code = seed.item_code
      AND existing.deleted = b'0'
      AND existing.tenant_id = @rrm_tenant_id
);

CALL codex_rrm_assert((
    SELECT COUNT(DISTINCT reg.route_process_id) = 14
    FROM mes_qa_inspection_regulation reg
    JOIN mes_qa_inspection_regulation_item item
      ON item.regulation_version_id = reg.current_version_id
     AND item.inspection_type = 'PATROL'
     AND item.deleted = b'0'
     AND item.tenant_id = reg.tenant_id
    WHERE reg.product_id = @rrm_product_id
      AND reg.route_id = @rrm_route_id
      AND reg.route_version_id = @rrm_route_version_id
      AND reg.deleted = b'0'
      AND reg.tenant_id = @rrm_tenant_id
), 'Every RRM formal QA regulation version must have PATROL items');

-- Repeatable local E2E seed: keep SUBMITTED history immutable and top up future PENDING slots.
DROP TEMPORARY TABLE IF EXISTS tmp_rrm_pqc_task_slot;
CREATE TEMPORARY TABLE tmp_rrm_pqc_task_slot (
    slot_offset int NOT NULL PRIMARY KEY
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_rrm_pqc_task_slot (slot_offset)
VALUES (1), (2), (3), (4), (5), (6), (7), (8);

INSERT IGNORE INTO mes_pqc_inspection_task (
    active_order_id, work_order_id, route_id, route_version_id, route_process_id,
    process_id, regulation_version_id, inspection_type, business_date, shift_code,
    round_no, planned_inspection_quantity, actual_inspection_quantity, task_status,
    creator, updater, tenant_id
)
SELECT
    @rrm_active_order_id,
    @rrm_work_order_id,
    @rrm_route_id,
    @rrm_route_version_id,
    reg.route_process_id,
    reg.process_id,
    reg.current_version_id,
    'PATROL',
    DATE_ADD(@rrm_business_date, INTERVAL slot.slot_offset DAY),
    @rrm_shift_code,
    @rrm_round_no,
    GREATEST(1, CEIL(wo.quantity * COALESCE(MAX(item.patrol_inspection_ratio), @rrm_default_patrol_ratio))),
    0,
    'PENDING',
    @rrm_actor,
    @rrm_actor,
    @rrm_tenant_id
FROM mes_qa_inspection_regulation reg
CROSS JOIN tmp_rrm_pqc_task_slot slot
JOIN mes_pro_work_order wo
  ON wo.id = @rrm_work_order_id
 AND wo.deleted = b'0'
 AND wo.tenant_id = reg.tenant_id
JOIN mes_qa_inspection_regulation_item item
  ON item.regulation_version_id = reg.current_version_id
 AND item.inspection_type = 'PATROL'
 AND item.deleted = b'0'
 AND item.tenant_id = reg.tenant_id
WHERE reg.product_id = @rrm_product_id
  AND reg.route_id = @rrm_route_id
  AND reg.route_version_id = @rrm_route_version_id
  AND reg.deleted = b'0'
  AND reg.tenant_id = @rrm_tenant_id
GROUP BY reg.route_process_id, reg.process_id, reg.current_version_id, wo.quantity, slot.slot_offset;

DELETE detail
FROM mes_pqc_inspection_piece_detail detail
JOIN mes_pqc_inspection_task task
  ON task.id = detail.task_id
 AND task.tenant_id = detail.tenant_id
WHERE task.active_order_id = @rrm_active_order_id
  AND task.work_order_id = @rrm_work_order_id
  AND task.route_id = @rrm_route_id
  AND task.route_version_id = @rrm_route_version_id
  AND task.inspection_type = 'PATROL'
  AND task.business_date > @rrm_business_date
  AND task.shift_code = @rrm_shift_code
  AND task.round_no = @rrm_round_no
  AND task.task_status = 'PENDING'
  AND task.creator = @rrm_actor
  AND task.deleted = b'0'
  AND task.tenant_id = @rrm_tenant_id
  AND detail.deleted = b'0';

CALL codex_rrm_assert((
    SELECT COUNT(DISTINCT route_process_id) = 14
    FROM mes_pqc_inspection_task
    WHERE active_order_id = @rrm_active_order_id
      AND work_order_id = @rrm_work_order_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND inspection_type = 'PATROL'
      AND business_date > @rrm_business_date
      AND shift_code = @rrm_shift_code
      AND round_no = @rrm_round_no
      AND task_status = 'PENDING'
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM repeatable pending PQC task rows were not available for all 14 processes');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 0
    FROM mes_pqc_inspection_task task
    JOIN mes_pqc_inspection_piece_detail detail
      ON detail.task_id = task.id
     AND detail.deleted = b'0'
     AND detail.tenant_id = task.tenant_id
    WHERE task.active_order_id = @rrm_active_order_id
      AND task.work_order_id = @rrm_work_order_id
      AND task.route_id = @rrm_route_id
      AND task.route_version_id = @rrm_route_version_id
      AND task.inspection_type = 'PATROL'
      AND task.business_date > @rrm_business_date
      AND task.shift_code = @rrm_shift_code
      AND task.round_no = @rrm_round_no
      AND task.task_status = 'PENDING'
      AND task.deleted = b'0'
      AND task.tenant_id = @rrm_tenant_id
), 'RRM repeatable pending PQC tasks must not retain old piece-detail rows');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 0
    FROM mes_pqc_inspection_task task
    LEFT JOIN mes_qa_inspection_regulation reg
      ON reg.route_process_id = task.route_process_id
     AND reg.process_id = task.process_id
     AND reg.current_version_id = task.regulation_version_id
     AND reg.product_id = @rrm_product_id
     AND reg.route_id = @rrm_route_id
     AND reg.route_version_id = @rrm_route_version_id
     AND reg.deleted = b'0'
     AND reg.tenant_id = task.tenant_id
    WHERE task.active_order_id = @rrm_active_order_id
      AND task.deleted = b'0'
      AND task.tenant_id = @rrm_tenant_id
      AND reg.id IS NULL
), 'RRM pending PQC task regulation version is not aligned to current QA regulation');

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_rrm_method_seed;
DROP TEMPORARY TABLE IF EXISTS tmp_rrm_missing_final_route_process;
DROP TEMPORARY TABLE IF EXISTS tmp_rrm_missing_first_route_process;
DROP TEMPORARY TABLE IF EXISTS tmp_rrm_source_method;
DROP TEMPORARY TABLE IF EXISTS tmp_rrm_current_route_process;
DROP PROCEDURE IF EXISTS codex_rrm_assert;
