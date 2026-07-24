-- Reset complete auto-schedule demo data for tenant 1.
DELETE ext
FROM mes_pro_task_schedule_ext ext
JOIN mes_pro_task task ON task.id = ext.task_id
WHERE task.work_order_id IN (900080, 900082);

DELETE dep
FROM mes_pro_task_dependency dep
WHERE dep.source_task_id IN (SELECT id FROM (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082)) t)
   OR dep.target_task_id IN (SELECT id FROM (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082)) t);

DELETE issue
FROM mes_pro_schedule_issue issue
WHERE issue.work_order_id IN (900080, 900082)
   OR issue.task_id IN (SELECT id FROM (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082)) t);

DELETE FROM mes_pro_task WHERE work_order_id IN (900080, 900082);
DELETE FROM mes_pro_work_order_bom WHERE id IN (900081, 900083) OR work_order_id IN (900080, 900082);
DELETE FROM mes_pro_work_order WHERE id IN (900080, 900082);
DELETE FROM mes_pro_capacity_plan WHERE id IN (900060, 900061, 900062) OR line_id IN (900040, 900041);
DELETE FROM mes_pro_capacity_actual WHERE line_id IN (900040, 900041);
DELETE FROM mes_md_workstation WHERE id IN (900050, 900051);
DELETE FROM mes_md_production_line WHERE id IN (900040, 900041);
DELETE FROM mes_cal_plan_shift WHERE id = 900031 OR plan_id = 900030;
DELETE FROM mes_cal_plan WHERE id = 900030;
DELETE FROM mes_pro_route_process WHERE id IN (900022, 900023) OR route_id = 900020;
DELETE FROM mes_pro_route_product WHERE id = 900021 OR route_id = 900020;
DELETE FROM mes_pro_route WHERE id = 900020;
DELETE FROM mes_pro_process WHERE id IN (900300, 900301);
DELETE FROM mes_md_workshop WHERE id = 900010;
DELETE FROM mes_wm_material_stock WHERE id = 900090 OR item_id = 900200;
DELETE FROM mes_md_item WHERE id IN (900100, 900200);
DELETE FROM mes_md_unit_measure WHERE id = 900001;
DELETE FROM mes_md_auto_code_record WHERE rule_id = 900070;
DELETE FROM mes_md_auto_code_part WHERE id IN (900071, 900072) OR rule_id = 900070;
DELETE FROM mes_md_auto_code_rule WHERE id = 900070;

SET @sim_date = (
    SELECT COALESCE(
        (
            SELECT DATE_FORMAT(simulation_date, '%Y-%m-%d')
            FROM mes_pro_schedule_calendar_simulation
            WHERE deleted = b'0' AND tenant_id = 1
            ORDER BY id DESC
            LIMIT 1
        ),
        DATE_FORMAT(CURDATE(), '%Y-%m-%d')
    )
);
SET @sim_date_plus_1 = DATE_FORMAT(DATE_ADD(STR_TO_DATE(@sim_date, '%Y-%m-%d'), INTERVAL 1 DAY), '%Y-%m-%d');
SET @sim_date_start = CONCAT(@sim_date, ' 08:00:00');
SET @sim_date_zero = CONCAT(@sim_date, ' 00:00:00');
SET @sim_date_plus_1_zero = CONCAT(@sim_date_plus_1, ' 00:00:00');
SET @date_shift_override_json = '{}';
SET @date_shift_override_json = IF(
    WEEKDAY(STR_TO_DATE(@sim_date, '%Y-%m-%d')) >= 5,
    JSON_MERGE_PATCH(@date_shift_override_json, JSON_OBJECT(@sim_date, 'DAY')),
    @date_shift_override_json
);
SET @date_shift_override_json = IF(
    WEEKDAY(STR_TO_DATE(@sim_date_plus_1, '%Y-%m-%d')) >= 5,
    JSON_MERGE_PATCH(@date_shift_override_json, JSON_OBJECT(@sim_date_plus_1, 'DAY')),
    @date_shift_override_json
);

INSERT INTO mes_md_unit_measure
    (id, code, name, primary_flag, change_rate, status, creator, updater, tenant_id)
VALUES
    (900001, 'PCS', 'PCS', b'1', 1, 0, 'codex', 'codex', 1);

INSERT INTO mes_md_item
    (id, code, name, specification, unit_measure_id, status, safe_stock_flag, high_value, batch_flag, creator, updater, tenant_id)
VALUES
    (900100, 'AUTO-PROD-001', 'AutoScheduleProduct', 'AUTO-PROD-SPEC', 900001, 0, b'0', b'0', b'0', 'codex', 'codex', 1),
    (900200, 'AUTO-MAT-001', 'AutoScheduleMaterial', 'AUTO-MAT-SPEC', 900001, 0, b'0', b'0', b'0', 'codex', 'codex', 1);

INSERT INTO mes_md_workshop
    (id, code, name, area, status, creator, updater, tenant_id)
VALUES
    (900010, 'AUTO-WSHOP', 'AutoScheduleWorkshop', 100, 0, 'codex', 'codex', 1);

INSERT INTO mes_pro_process
    (id, code, name, attention, status, creator, updater, tenant_id)
VALUES
    (900300, 'AUTO-PROC-01', 'AutoScheduleCutting', 'AUTO', 0, 'codex', 'codex', 1),
    (900301, 'AUTO-PROC-02', 'AutoScheduleAssembly', 'AUTO', 0, 'codex', 'codex', 1);

INSERT INTO mes_pro_route
    (id, code, name, description, status, creator, updater, tenant_id)
VALUES
    (900020, 'AUTO-ROUTE-01', 'AutoScheduleRoute', 'AUTO', 0, 'codex', 'codex', 1);

INSERT INTO mes_pro_route_product
    (id, route_id, item_id, quantity, production_time, time_unit_type, creator, updater, tenant_id)
VALUES
    (900021, 900020, 900100, 1, 8, 'HOUR', 'codex', 'codex', 1);

INSERT INTO mes_pro_route_process
    (id, route_id, process_id, sort, next_process_id, link_type, prepare_time, wait_time, color_code, key_flag, check_flag, creator, updater, tenant_id)
VALUES
    (900022, 900020, 900300, 1, 900301, 1, 0, 0, '#1677ff', b'1', b'0', 'codex', 'codex', 1),
    (900023, 900020, 900301, 2, NULL, 1, 0, 0, '#13c2c2', b'0', b'0', 'codex', 'codex', 1);

INSERT INTO mes_cal_plan
    (id, code, name, calendar_type, start_date, end_date, shift_type, shift_method, shift_count, status, creator, updater, tenant_id)
VALUES
    (900030, 'AUTO-PLAN-01', 'AutoSchedulePlan', 1, @sim_date_zero, '2027-12-31 00:00:00', 1, 1, 1, 0, 'codex', 'codex', 1);

INSERT INTO mes_cal_plan_shift
    (id, plan_id, sort, name, start_time, end_time, creator, updater, tenant_id)
VALUES
    (900031, 900030, 1, 'AUTO-DAY', '08:00', '20:00', 'codex', 'codex', 1);

INSERT INTO mes_md_production_line
    (id, code, name, workshop_id, calendar_plan_id, status, creator, updater, tenant_id)
VALUES
    (900040, 'AUTO-LINE-01', 'AutoScheduleLineA', 900010, 900030, 0, 'codex', 'codex', 1),
    (900041, 'AUTO-LINE-02', 'AutoScheduleLineB', 900010, 900030, 0, 'codex', 'codex', 1);

INSERT INTO mes_md_workstation
    (id, code, name, address, workshop_id, process_id, production_line_id, single_standard_hourly_capacity, status, creator, updater, tenant_id)
VALUES
    (900050, 'AUTO-WS-01', 'AutoScheduleWorkstationA', 'AUTO-A', 900010, 900300, 900040, 1.00, 0, 'codex', 'codex', 1),
    (900051, 'AUTO-WS-02', 'AutoScheduleWorkstationB', 'AUTO-B', 900010, 900301, 900041, 1.00, 0, 'codex', 'codex', 1);

INSERT INTO mes_pro_capacity_plan
    (id, line_id, calendar_date, shift_id, capacity_minutes, enabled, creator, updater, tenant_id)
VALUES
    (900060, 900040, @sim_date_zero, 900031, 720, b'1', 'codex', 'codex', 1),
    (900061, 900041, @sim_date_zero, 900031, 720, b'1', 'codex', 'codex', 1),
    (900062, 900041, @sim_date_plus_1_zero, 900031, 720, b'1', 'codex', 'codex', 1);

INSERT INTO mes_pro_schedule_calendar_rule
    (tenant_id, skip_statutory_holidays, weekend_rest_mode, date_shift_mode_by_date_json, temporary_freeze_enabled, remark, creator, updater)
VALUES
    (1, b'0', 'DOUBLE', @date_shift_override_json, b'0', 'AUTO-DEMO-COMPLETE', 'codex', 'codex')
ON DUPLICATE KEY UPDATE
    skip_statutory_holidays = VALUES(skip_statutory_holidays),
    weekend_rest_mode = VALUES(weekend_rest_mode),
    date_shift_mode_by_date_json = VALUES(date_shift_mode_by_date_json),
    temporary_freeze_enabled = VALUES(temporary_freeze_enabled),
    updater = 'codex',
    update_time = CURRENT_TIMESTAMP;

INSERT INTO mes_md_auto_code_rule
    (id, code, name, description, max_length, padded, padded_char, padded_method, status, creator, updater, tenant_id)
VALUES
    (900070, 'PRO_TASK_CODE', 'AutoScheduleTaskCode', 'AUTO', 32, b'0', '0', 1, 0, 'codex', 'codex', 1);

INSERT INTO mes_md_auto_code_part
    (id, rule_id, sort, type, length, fix_character, serial_start_no, serial_step, cycle_flag, creator, updater, tenant_id)
VALUES
    (900071, 900070, 1, 3, 3, 'PT-', NULL, NULL, b'0', 'codex', 'codex', 1),
    (900072, 900070, 2, 4, 4, NULL, 1, 1, b'0', 'codex', 'codex', 1);

INSERT INTO mes_pro_work_order
    (id, code, name, type, order_source_type, product_id, quantity, quantity_produced, quantity_changed, quantity_scheduled, batch_code, request_date, parent_id, status, creator, updater, tenant_id)
VALUES
    (900080, 'AUTO-WO-001', 'AutoScheduleWorkOrderA', 1, 1, 900100, 1.00, 0.00, 0.00, 0.00, 'AUTO-BATCH-001', @sim_date_start, 0, 1, 'codex', 'codex', 1),
    (900082, 'AUTO-WO-002', 'AutoScheduleWorkOrderB', 1, 1, 900100, 2.00, 0.00, 0.00, 0.00, 'AUTO-BATCH-002', @sim_date_start, 0, 1, 'codex', 'codex', 1);

INSERT INTO mes_pro_work_order_bom
    (id, work_order_id, item_id, quantity, creator, updater, tenant_id)
VALUES
    (900081, 900080, 900200, 1.000000, 'codex', 'codex', 1),
    (900083, 900082, 900200, 2.000000, 'codex', 'codex', 1);

INSERT INTO mes_wm_material_stock
    (id, item_id, quantity, frozen, creator, updater, tenant_id)
VALUES
    (900090, 900200, 10.00, b'0', 'codex', 'codex', 1);
