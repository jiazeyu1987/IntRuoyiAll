SET NAMES utf8mb4;
CREATE TEMPORARY TABLE task_assert_value (value INT NOT NULL);
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery
       WHERE tenant_id = 122 AND deleted = b'0' AND BINARY code = BINARY 'A05059') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery
       WHERE tenant_id = 122 AND deleted = b'0' AND BINARY code = BINARY 'B09041') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery
       WHERE tenant_id = 122 AND deleted = b'0'
         AND BINARY code IN (BINARY 'A05075', BINARY 'B04091', BINARY 'C01017')) <> 0;
DELETE FROM task_assert_value;

SELECT id INTO @light_machine_id
FROM mes_dv_machinery
WHERE tenant_id = 122 AND deleted = b'0' AND BINARY code = BINARY 'A05059'
FOR UPDATE;

SELECT id INTO @dryer_machine_id
FROM mes_dv_machinery
WHERE tenant_id = 122 AND deleted = b'0' AND BINARY code = BINARY 'B09041'
FOR UPDATE;

INSERT INTO task_assert_value
SELECT NULL
WHERE @light_machine_id <> 202 OR @dryer_machine_id <> 198;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery_type
       WHERE id = 5 AND tenant_id = 122 AND deleted = b'0'
         AND BINARY code = BINARY 'DEFAULT-MACHINERY-TYPE') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_md_workshop
       WHERE id = 900066 AND tenant_id = 122 AND deleted = b'0'
         AND BINARY code = BINARY 'AUTO-WSHOP') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery_process
       WHERE tenant_id = 122 AND deleted = b'0' AND machinery_id = @light_machine_id
         AND BINARY machinery_code = BINARY 'A05059') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery_process
       WHERE tenant_id = 122 AND deleted = b'0' AND machinery_id = @dryer_machine_id
         AND BINARY machinery_code = BINARY 'B09041') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_pro_mes_process_catalog
       WHERE id = 9003131008 AND tenant_id = 0 AND deleted = b'0'
         AND BINARY source_machinery_codes = BINARY 'A05059') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_pro_mes_process_catalog
       WHERE id = 9003131004 AND tenant_id = 0 AND deleted = b'0'
         AND BINARY source_machinery_codes = BINARY 'B09041') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_pro_mes_process_catalog_machinery
       WHERE id = 9003132008 AND catalog_id = 9003131008 AND tenant_id = 0 AND deleted = b'0'
         AND BINARY machinery_code = BINARY 'A05059') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_pro_mes_process_catalog_machinery
       WHERE id = 9003132004 AND catalog_id = 9003131004 AND tenant_id = 0 AND deleted = b'0'
         AND BINARY machinery_code = BINARY 'B09041') <> 1;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_wm_barcode_config
       WHERE tenant_id = 122 AND deleted = b'0' AND biz_type = 400) <> 0;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_wm_barcode
       WHERE tenant_id = 122 AND biz_type = 400 AND biz_id IN (@light_machine_id, @dryer_machine_id)) <> 0;
DELETE FROM task_assert_value;

UPDATE mes_dv_machinery
SET code = 'A05075',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE id = @light_machine_id AND tenant_id = 122 AND deleted = b'0'
  AND BINARY code = BINARY 'A05059';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

UPDATE mes_dv_machinery
SET code = 'B04091',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE id = @dryer_machine_id AND tenant_id = 122 AND deleted = b'0'
  AND BINARY code = BINARY 'B09041';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

UPDATE mes_dv_machinery_process
SET machinery_code = 'A05075',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 122 AND deleted = b'0' AND machinery_id = @light_machine_id
  AND BINARY machinery_code = BINARY 'A05059';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

UPDATE mes_dv_machinery_process
SET machinery_code = 'B04091',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 122 AND deleted = b'0' AND machinery_id = @dryer_machine_id
  AND BINARY machinery_code = BINARY 'B09041';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

UPDATE mes_pro_mes_process_catalog
SET source_machinery_codes = 'A05075',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9003131008 AND tenant_id = 0 AND deleted = b'0'
  AND BINARY source_machinery_codes = BINARY 'A05059';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

UPDATE mes_pro_mes_process_catalog
SET source_machinery_codes = 'B04091',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9003131004 AND tenant_id = 0 AND deleted = b'0'
  AND BINARY source_machinery_codes = BINARY 'B09041';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

UPDATE mes_pro_mes_process_catalog_machinery
SET machinery_code = 'A05075',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9003132008 AND catalog_id = 9003131008 AND tenant_id = 0 AND deleted = b'0'
  AND BINARY machinery_code = BINARY 'A05059';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

UPDATE mes_pro_mes_process_catalog_machinery
SET machinery_code = 'B04091',
    updater = 'codex-pressure-pump-ledger-correction-20260807',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9003132004 AND catalog_id = 9003131004 AND tenant_id = 0 AND deleted = b'0'
  AND BINARY machinery_code = BINARY 'B09041';
SET @affected_rows = ROW_COUNT();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1;
DELETE FROM task_assert_value;

INSERT INTO mes_dv_machinery (
  code, name, brand, specification, machinery_type_id, workshop_id,
  process_name, standard_hourly_capacity, status, last_mainten_time,
  last_check_time, remark, creator, updater, deleted, tenant_id
) VALUES (
  'C01017', CONVERT(UNHEX('E692A4E58E8BE69CBA') USING utf8mb4), NULL, NULL, 5, 900066,
  NULL, NULL, 2, NULL,
  NULL, NULL,
  'codex-pressure-pump-ledger-correction-20260807',
  'codex-pressure-pump-ledger-correction-20260807', b'0', 122
);
SET @affected_rows = ROW_COUNT();
SET @new_machine_id = LAST_INSERT_ID();
INSERT INTO task_assert_value SELECT NULL WHERE @affected_rows <> 1 OR @new_machine_id IS NULL;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery
       WHERE tenant_id = 122 AND deleted = b'0'
         AND BINARY code IN (BINARY 'A05075', BINARY 'B04091', BINARY 'C01017')) <> 3;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery
       WHERE tenant_id = 122 AND deleted = b'0'
         AND BINARY code IN (BINARY 'A05059', BINARY 'B09041')) <> 0;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT COUNT(*) FROM mes_dv_machinery WHERE tenant_id = 122 AND deleted = b'0') <> 50;
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT MD5(CONCAT_WS('|', id, COALESCE(name, '<NULL>'), COALESCE(brand, '<NULL>'),
       COALESCE(specification, '<NULL>'), COALESCE(machinery_type_id, '<NULL>'),
       COALESCE(workshop_id, '<NULL>'), COALESCE(process_name, '<NULL>'),
       COALESCE(standard_hourly_capacity, '<NULL>'), COALESCE(status, '<NULL>'),
       COALESCE(last_mainten_time, '<NULL>'), COALESCE(last_check_time, '<NULL>'),
       COALESCE(remark, '<NULL>'), COALESCE(creator, '<NULL>'), create_time,
       deleted + 0, tenant_id)) FROM mes_dv_machinery WHERE id = @dryer_machine_id)
      <> 'd03fcab9d173520de79485ab0f4d678e';
DELETE FROM task_assert_value;

INSERT INTO task_assert_value
SELECT NULL
WHERE (SELECT MD5(CONCAT_WS('|', id, COALESCE(name, '<NULL>'), COALESCE(brand, '<NULL>'),
       COALESCE(specification, '<NULL>'), COALESCE(machinery_type_id, '<NULL>'),
       COALESCE(workshop_id, '<NULL>'), COALESCE(process_name, '<NULL>'),
       COALESCE(standard_hourly_capacity, '<NULL>'), COALESCE(status, '<NULL>'),
       COALESCE(last_mainten_time, '<NULL>'), COALESCE(last_check_time, '<NULL>'),
       COALESCE(remark, '<NULL>'), COALESCE(creator, '<NULL>'), create_time,
       deleted + 0, tenant_id)) FROM mes_dv_machinery WHERE id = @light_machine_id)
      <> '9b04a596ce68241a70a32d2a0904d405';
DELETE FROM task_assert_value;

COMMIT;

SELECT @new_machine_id AS new_machine_id,
       @light_machine_id AS light_machine_id,
       @dryer_machine_id AS dryer_machine_id;
SELECT id, code, name, machinery_type_id, workshop_id, process_name,
       standard_hourly_capacity, status, deleted + 0, tenant_id
FROM mes_dv_machinery
WHERE tenant_id = 122 AND deleted = b'0'
  AND BINARY code IN (BINARY 'A05075', BINARY 'B04091', BINARY 'C01017')
ORDER BY code;
