DELIMITER $$

DROP PROCEDURE IF EXISTS apply_codx_rpt_20260807$$
CREATE PROCEDURE apply_codx_rpt_20260807()
BEGIN
  DECLARE v_i INT DEFAULT 1;
  DECLARE v_existing INT DEFAULT 0;
  DECLARE v_code VARCHAR(64);
  DECLARE v_idempotency_key VARCHAR(128);
  DECLARE v_qty DECIMAL(18, 2);
  DECLARE v_pressure DECIMAL(18, 2);
  DECLARE v_temperature DECIMAL(18, 2);
  DECLARE v_base_time DATETIME DEFAULT NOW();
  DECLARE v_submit_time DATETIME;
  DECLARE v_feedback_id BIGINT;
  DECLARE v_entry_id BIGINT;
  DECLARE v_recordbook_event_id BIGINT;
  DECLARE v_pool_event_id BIGINT;
  DECLARE v_last_pool_event_id BIGINT;
  DECLARE v_entry_content JSON;
  DECLARE v_process_payload JSON;
  DECLARE v_fragment_payload JSON;
  DECLARE v_previous_latest_event_id BIGINT;
  DECLARE v_previous_latest_submit_time DATETIME;
  DECLARE v_previous_total_event_count INT;
  DECLARE v_previous_last_actual_employee_id BIGINT;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT
    (SELECT COUNT(*) FROM mes_pro_feedback WHERE code LIKE 'CODX-RPT-20260807-%') +
    (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_entry WHERE idempotency_key LIKE 'CODX-RPT-20260807-%') +
    (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_event WHERE idempotency_key LIKE 'CODX-RPT-20260807-%') +
    (SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE event_idempotency_key LIKE 'CODX-RPT-20260807-%') +
    (SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE signature_id BETWEEN 202608070001 AND 202608070005)
  INTO v_existing;
  IF v_existing <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CODX-RPT-20260807 task data already exists';
  END IF;

  IF (SELECT COUNT(*) FROM system_users WHERE id = 964 AND tenant_id = 1 AND deleted = b'0' AND status = 0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Frontline employee 964 is missing or inactive';
  END IF;
  IF (SELECT COUNT(*) FROM system_users WHERE id = 1520 AND tenant_id = 1 AND deleted = b'0' AND status = 0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Production leader 1520 is missing or inactive';
  END IF;
  IF (SELECT COUNT(*) FROM mes_pro_process_pool_team_leader_scope WHERE leader_user_id = 1520 AND leader_type = 'PRODUCTION' AND scope_type = 'EMPLOYEE' AND employee_user_id = 964 AND enabled = b'1' AND tenant_id = 1 AND deleted = b'0') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Production leader employee scope is missing';
  END IF;
  IF (SELECT COUNT(*) FROM mes_pro_work_order WHERE id = 980008 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_task WHERE id = 981941 AND work_order_id = 980008 AND process_id = 922987 AND workstation_id = 980009 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_route_process WHERE id = 928611 AND route_id = 922119 AND process_id = 922987 AND workstation_id = 980009 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_edhr_recordbook WHERE id = 980011 AND template_id = 980010 AND owner_user_id = 964 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_dv_machinery WHERE id = 41 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_process_pool WHERE id = 37 AND work_order_id = 980008 AND route_process_id = 928611 AND process_id = 922987 AND device_id = 41 AND workstation_id = 980009 AND tenant_id = 1 AND deleted = b'0') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Formal frontline business context is incomplete';
  END IF;
  IF (SELECT COUNT(*) FROM mes_pro_feedback WHERE id = 850 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_entry WHERE id = 980090 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_edhr_recordbook_event WHERE id = 980090 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE id = 161 AND tenant_id = 1 AND deleted = b'0') <> 1 OR
     (SELECT COUNT(*) FROM mes_pro_process_pool_quantity_fragment WHERE id = 48 AND tenant_id = 1 AND deleted = b'0') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Validated formal seed pattern is missing';
  END IF;

  SELECT latest_event_id, latest_submit_time, total_event_count, last_actual_employee_id
    INTO v_previous_latest_event_id, v_previous_latest_submit_time, v_previous_total_event_count, v_previous_last_actual_employee_id
    FROM mes_pro_process_pool
    WHERE id = 37
    FOR UPDATE;

  WHILE v_i <= 5 DO
    SET v_code = CONCAT('CODX-RPT-20260807-', LPAD(v_i, 3, '0'));
    SET v_idempotency_key = CONCAT(v_code, '-IDEMP-PROCESS-POOL');
    SET v_submit_time = DATE_ADD(v_base_time, INTERVAL v_i SECOND);
    SET v_qty = CASE v_i WHEN 1 THEN 4.50 WHEN 2 THEN 5.25 WHEN 3 THEN 6.00 WHEN 4 THEN 6.75 ELSE 7.50 END;
    SET v_pressure = CASE v_i WHEN 1 THEN 0.31 WHEN 2 THEN 0.33 WHEN 3 THEN 0.35 WHEN 4 THEN 0.34 ELSE 0.36 END;
    SET v_temperature = CASE v_i WHEN 1 THEN 22.10 WHEN 2 THEN 22.30 WHEN 3 THEN 22.50 WHEN 4 THEN 22.70 ELSE 22.90 END;

    INSERT INTO mes_pro_feedback (
      code, type, channel, feedback_time, workstation_id, route_id, process_id, work_order_id, task_id,
      schedule_order_id, schedule_order_process_id, source_import_record_id, item_id, expire_date, lot_number,
      scheduled_quantity, feedback_quantity, qualified_quantity, unqualified_quantity, uncheck_quantity,
      labor_scrap_quantity, material_scrap_quantity, other_scrap_quantity, loss_reason_id,
      loss_reason_code_snapshot, loss_reason_name_snapshot, feedback_user_id, approve_user_id, status, remark,
      creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT
      v_code, type, channel, v_submit_time, workstation_id, route_id, process_id, work_order_id, task_id,
      schedule_order_id, schedule_order_process_id, source_import_record_id, item_id, expire_date, lot_number,
      scheduled_quantity, v_qty, v_qty, 0.00, uncheck_quantity,
      0.00, 0.00, 0.00, NULL, NULL, NULL, 964, 1520, 2,
      CONCAT('CODX-RPT-20260807 task-owned frontline-format sample ', LPAD(v_i, 3, '0')),
      964, v_submit_time, 964, v_submit_time, deleted, tenant_id
    FROM mes_pro_feedback WHERE id = 850;
    SET v_feedback_id = LAST_INSERT_ID();

    SELECT JSON_SET(
      entry_content_json,
      '$.rawPayload.marker', 'CODX-RPT-20260807',
      '$.rawPayload.outputQuantity', v_qty,
      '$.rawPayload.fieldValues.OUTPUT_QUANTITY', v_qty,
      '$.rawPayload.fieldValues.DEVICE_PARAMETERS."Frontline Pump A03190".pressure', v_pressure,
      '$.rawPayload.fieldValues.DEVICE_PARAMETERS."Frontline Pump A03190".temperature', v_temperature,
      '$.rawPayload.equipmentParameters."Frontline Pump A03190".pressure', v_pressure,
      '$.rawPayload.equipmentParameters."Frontline Pump A03190".temperature', v_temperature,
      '$.rawPayload.deviceParameterReadings[0].value', v_pressure,
      '$.rawPayload.deviceParameterReadings[1].value', v_temperature,
      '$.fieldValues.OUTPUT_QUANTITY', v_qty,
      '$.fieldValues.DEVICE_PARAMETERS."Frontline Pump A03190".pressure', v_pressure,
      '$.fieldValues.DEVICE_PARAMETERS."Frontline Pump A03190".temperature', v_temperature,
      '$.equipmentParameters."Frontline Pump A03190".pressure', v_pressure,
      '$.equipmentParameters."Frontline Pump A03190".temperature', v_temperature
    ) INTO v_entry_content
    FROM mes_pro_edhr_recordbook_entry WHERE id = 980090;

    INSERT INTO mes_pro_edhr_recordbook_entry (
      entry_code, recordbook_id, recordbook_code, template_id, template_code, template_version, status, version,
      entry_title, entry_content_json, tag_snapshot_json, submitted_by, submitted_at, locked_at, idempotency_key,
      remark, creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT
      CONCAT('EDHR-RBE-980011-', v_code), recordbook_id, recordbook_code, template_id, template_code,
      template_version, status, version, CONCAT('frontline-', v_code), v_entry_content, tag_snapshot_json,
      submitted_by, submitted_at, locked_at, v_idempotency_key, 'CODX-RPT-20260807',
      964, v_submit_time, 964, v_submit_time, deleted, tenant_id
    FROM mes_pro_edhr_recordbook_entry WHERE id = 980090;
    SET v_entry_id = LAST_INSERT_ID();

    INSERT INTO mes_pro_edhr_recordbook_event (
      recordbook_id, entry_id, event_type, from_status, to_status, result_status, failure_reason,
      operator_user_id, operator_username, occurred_at, event_snapshot_json, idempotency_key,
      creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT
      recordbook_id, v_entry_id, event_type, from_status, to_status, result_status, failure_reason,
      964, 'USER-964', v_submit_time, v_entry_content, v_idempotency_key,
      964, v_submit_time, 964, v_submit_time, deleted, tenant_id
    FROM mes_pro_edhr_recordbook_event WHERE id = 980090;
    SET v_recordbook_event_id = LAST_INSERT_ID();

    SELECT JSON_SET(
      raw_payload,
      '$.marker', 'CODX-RPT-20260807',
      '$.outputQuantity', v_qty,
      '$.fieldValues.OUTPUT_QUANTITY', v_qty,
      '$.fieldValues.DEVICE_PARAMETERS."Frontline Pump A03190".pressure', v_pressure,
      '$.fieldValues.DEVICE_PARAMETERS."Frontline Pump A03190".temperature', v_temperature,
      '$.equipmentParameters."Frontline Pump A03190".pressure', v_pressure,
      '$.equipmentParameters."Frontline Pump A03190".temperature', v_temperature,
      '$.deviceParameterReadings[0].value', v_pressure,
      '$.deviceParameterReadings[1].value', v_temperature
    ) INTO v_process_payload
    FROM mes_pro_process_pool_event WHERE id = 161;

    INSERT INTO mes_pro_process_pool_event (
      pool_id, event_type, event_idempotency_key, work_order_id, route_id, route_process_id, process_id,
      actual_employee_id, device_account_id, device_id, workstation_id, template_type,
      feedback_source_type, feedback_source_id, recordbook_entry_id, recordbook_source_type,
      recordbook_source_id, raw_payload, pqc_task_id, server_submit_time, signature_id, signature_user_id,
      signature_snapshot, creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT
      pool_id, event_type, v_idempotency_key, work_order_id, route_id, route_process_id, process_id,
      964, 964, device_id, workstation_id, template_type, feedback_source_type, v_feedback_id,
      v_entry_id, recordbook_source_type, v_recordbook_event_id, v_process_payload, pqc_task_id,
      v_submit_time, 202608070000 + v_i, 964, signature_snapshot,
      964, v_submit_time, 964, v_submit_time, deleted, tenant_id
    FROM mes_pro_process_pool_event WHERE id = 161;
    SET v_pool_event_id = LAST_INSERT_ID();
    SET v_last_pool_event_id = v_pool_event_id;

    SELECT JSON_SET(
      raw_payload,
      '$.feedbackId', v_feedback_id,
      '$.recordbookEntryId', v_entry_id,
      '$.recordbookEventId', v_recordbook_event_id,
      '$.equipmentParameters."Frontline Pump A03190".pressure', v_pressure,
      '$.equipmentParameters."Frontline Pump A03190".temperature', v_temperature,
      '$.deviceParameterReadings[0].value', v_pressure,
      '$.deviceParameterReadings[1].value', v_temperature
    ) INTO v_fragment_payload
    FROM mes_pro_process_pool_quantity_fragment WHERE id = 48;

    INSERT INTO mes_pro_process_pool_quantity_fragment (
      pool_id, event_id, production_submit_event_id, work_order_id, route_id, route_process_id, process_id,
      source_quantity_type, quality_status, total_quantity, allocated_quantity, available_quantity,
      allocation_status, locked, raw_payload, creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT
      pool_id, v_pool_event_id, v_pool_event_id, work_order_id, route_id, route_process_id, process_id,
      source_quantity_type, quality_status, v_qty, 0.000000, v_qty, 'AVAILABLE', b'0', v_fragment_payload,
      964, v_submit_time, 964, v_submit_time, deleted, tenant_id
    FROM mes_pro_process_pool_quantity_fragment WHERE id = 48;

    SET v_i = v_i + 1;
  END WHILE;

  UPDATE mes_pro_process_pool
  SET latest_event_id = v_last_pool_event_id,
      latest_submit_time = DATE_ADD(v_base_time, INTERVAL 5 SECOND),
      total_event_count = v_previous_total_event_count + 5,
      last_actual_employee_id = 964,
      updater = 964,
      update_time = DATE_ADD(v_base_time, INTERVAL 5 SECOND)
  WHERE id = 37 AND latest_event_id = v_previous_latest_event_id AND total_event_count = v_previous_total_event_count;
  IF ROW_COUNT() <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Process pool summary changed concurrently';
  END IF;

  COMMIT;

  SELECT
    v_previous_latest_event_id AS previous_latest_event_id,
    v_previous_latest_submit_time AS previous_latest_submit_time,
    v_previous_total_event_count AS previous_total_event_count,
    v_previous_last_actual_employee_id AS previous_last_actual_employee_id,
    v_last_pool_event_id AS new_latest_event_id,
    DATE_ADD(v_base_time, INTERVAL 5 SECOND) AS new_latest_submit_time;
END$$

CALL apply_codx_rpt_20260807()$$
DROP PROCEDURE apply_codx_rpt_20260807$$

DELIMITER ;
