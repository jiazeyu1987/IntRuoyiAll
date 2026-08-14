-- AC-M04 / RRM local primary-process runtime prerequisite repair.
-- Scope: local Docker MySQL ruoyi-vue-pro only.
-- Inserts exactly four task-owned rows after strict source and collision checks.

DROP PROCEDURE IF EXISTS codex_acm04_rrm_primary_process_runtime_prereq_apply;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_primary_process_runtime_prereq_apply()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_display_name VARCHAR(128)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_route_process
     WHERE id = 928609
       AND route_id = 922119
       AND process_id = 922985
       AND workstation_id = 980010
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary route-process identity is missing or ambiguous';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_device
     WHERE id = 41
       AND leader_user_id = 1520
       AND device_status = 'ENABLED'
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM team device is missing or not enabled';
    END IF;

    SELECT COALESCE(NULLIF(TRIM(display_name), ''), NULLIF(TRIM(employee_name), ''))
      INTO v_display_name
      FROM mes_pro_process_pool_team_employee_profile
     WHERE id = 980022
       AND leader_user_id = 1520
       AND system_user_id = 964
       AND employee_type = 'FORMAL'
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_display_name IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM employee profile is missing, disabled, or unnamed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE leader_user_id = 1520
       AND leader_type = 'PRODUCTION'
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1
       AND (
            (scope_type = 'EMPLOYEE' AND employee_user_id = 964)
         OR (scope_type = 'EQUIPMENT' AND equipment_id = 41)
         OR (scope_type = 'ORDER' AND work_order_id = 980008)
         OR (scope_type = 'PROCESS' AND process_id IN (922986, 922987))
         OR (scope_type = 'WORKSTATION' AND workstation_id IN (980008, 980009))
       );
    IF v_count <> 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Adjacent RRM leader scopes are incomplete or ambiguous';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_process_device
     WHERE leader_user_id = 1520
       AND process_id IN (922986, 922987)
       AND device_id = 41
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Adjacent RRM process-device bindings are incomplete or ambiguous';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_employee_binding
     WHERE leader_user_id = 1520
       AND process_id IN (922986, 922987)
       AND employee_profile_id = 980022
       AND employee_user_id = 964
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Adjacent RRM process-employee bindings are incomplete or ambiguous';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE id IN (980039, 980040)
        OR (leader_user_id = 1520 AND tenant_id = 1 AND (
               (scope_type = 'PROCESS' AND process_id = 922985)
            OR (scope_type = 'WORKSTATION' AND workstation_id = 980010)
        ));
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM scope id or semantic-key collision';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_process_device
     WHERE id = 15
        OR (leader_user_id = 1520 AND process_id = 922985 AND device_id = 41 AND tenant_id = 1);
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-device id or semantic-key collision';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_employee_binding
     WHERE id = 21
        OR (leader_user_id = 1520
            AND process_id = 922985
            AND employee_profile_id = 980022
            AND employee_user_id = 964
            AND tenant_id = 1);
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-employee id or semantic-key collision';
    END IF;

    INSERT INTO mes_pro_process_pool_team_leader_scope (
        id, leader_user_id, leader_type, scope_type,
        employee_user_id, process_id, workstation_id,
        production_line_id, equipment_id, work_order_id,
        enabled, remark, creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES
        (980039, 1520, 'PRODUCTION', 'PROCESS',
         NULL, 922985, NULL, NULL, NULL, NULL,
         b'1', 'rrm-ac-m04-runtime-prereq', 'rrm-acm04', NOW(), 'rrm-acm04', NOW(), b'0', 1),
        (980040, 1520, 'PRODUCTION', 'WORKSTATION',
         NULL, NULL, 980010, NULL, NULL, NULL,
         b'1', 'rrm-ac-m04-runtime-prereq', 'rrm-acm04', NOW(), 'rrm-acm04', NOW(), b'0', 1);
    IF ROW_COUNT() <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM scope insert did not affect exactly two rows';
    END IF;

    INSERT INTO mes_pro_process_pool_team_process_device (
        id, leader_user_id, process_id, device_id, enabled, disabled_at,
        remark, creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES (
        15, 1520, 922985, 41, b'1', NULL,
        'rrm-ac-m04-runtime-prereq', 'rrm-acm04', NOW(), 'rrm-acm04', NOW(), b'0', 1
    );
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-device insert did not affect exactly one row';
    END IF;

    INSERT INTO mes_pro_process_pool_team_employee_binding (
        id, leader_user_id, process_id, employee_profile_id, employee_user_id,
        display_name_snapshot, enabled, disabled_at,
        remark, creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES (
        21, 1520, 922985, 980022, 964,
        v_display_name, b'1', NULL,
        'rrm-ac-m04-runtime-prereq', 'rrm-acm04', NOW(), 'rrm-acm04', NOW(), b'0', 1
    );
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-employee insert did not affect exactly one row';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE id IN (980039, 980040)
       AND leader_user_id = 1520
       AND leader_type = 'PRODUCTION'
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq';
    IF v_count <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM scope post-verification failed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_process_device
     WHERE id = 15
       AND leader_user_id = 1520
       AND process_id = 922985
       AND device_id = 41
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-device post-verification failed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_employee_binding
     WHERE id = 21
       AND leader_user_id = 1520
       AND process_id = 922985
       AND employee_profile_id = 980022
       AND employee_user_id = 964
       AND display_name_snapshot = v_display_name
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-employee post-verification failed';
    END IF;
END//
DELIMITER ;

-- Keep procedure DDL outside the transaction because MySQL implicitly commits around DDL.
START TRANSACTION;

CALL codex_acm04_rrm_primary_process_runtime_prereq_apply();

COMMIT;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_primary_process_runtime_prereq_apply;
