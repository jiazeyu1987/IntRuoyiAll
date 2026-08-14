-- GREEN verification for current active routeStartProductionLeaders binding.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_verify_pressure_pump_route_start_leaders;
DELIMITER //
CREATE PROCEDURE codex_verify_pressure_pump_route_start_leaders()
BEGIN
    DECLARE v_verified_rows INT DEFAULT 0;
    DECLARE v_active_target_rows INT DEFAULT 0;
    DECLARE v_tenant122_changed INT DEFAULT 0;
    DECLARE v_target_draft_changed INT DEFAULT 0;
    DECLARE v_message VARCHAR(255);
    DECLARE v_admin_snapshot_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

    SET v_admin_snapshot_name = CONVERT(UNHEX('E7919BE6B3B0E7AEA1E79086E59198EFBC8861646D696EEFBC89') USING utf8mb4);

    SELECT rv.id AS route_version_id,
           rv.route_id,
           r.code AS route_code,
           r.name AS route_name,
           JSON_UNQUOTE(JSON_EXTRACT(rv.route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceType')) AS source_type,
           JSON_EXTRACT(rv.route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceIds') AS source_ids,
           JSON_EXTRACT(rv.route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceNames') AS source_names,
           JSON_EXTRACT(rv.route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].productionLineId') AS production_line_id
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id
    WHERE rv.tenant_id IN (1, 122)
      AND (rv.route_id IN (922119, 980091, 922273) OR rv.id IN (448, 490, 622))
    ORDER BY rv.tenant_id, rv.route_id, rv.id;

    SELECT COUNT(*) INTO v_active_target_rows
    FROM mes_pro_route_version
    WHERE tenant_id = 1
      AND route_id IN (922119, 980091)
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0';

    IF v_active_target_rows <> 2 THEN
        SET v_message = CONCAT('GREEN verification failed: activeTargetRows=', v_active_target_rows);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT COUNT(*) INTO v_verified_rows
    FROM mes_pro_route_version
    WHERE tenant_id = 1
      AND route_id IN (922119, 980091)
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0'
      AND JSON_LENGTH(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders')) = 1
      AND JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceType')) = 'USERS'
      AND JSON_CONTAINS(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceIds'), '1')
      AND JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceNames[0]')) = v_admin_snapshot_name
      AND CAST(JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].productionLineId')) AS UNSIGNED) = route_id;

    SELECT COUNT(*) INTO v_tenant122_changed
    FROM mes_pro_route_version
    WHERE tenant_id = 122
      AND route_id = 922273
      AND JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders') IS NOT NULL;

    SELECT COUNT(*) INTO v_target_draft_changed
    FROM mes_pro_route_version
    WHERE tenant_id = 1
      AND route_id IN (922119, 980091)
      AND lifecycle_status = 'DRAFT'
      AND JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders') IS NOT NULL;

    IF v_verified_rows <> 2 OR v_tenant122_changed <> 0 OR v_target_draft_changed <> 0 THEN
        SET v_message = CONCAT(
            'GREEN verification failed: verified=',
            v_verified_rows,
            ', tenant122Changed=',
            v_tenant122_changed,
            ', targetDraftChanged=',
            v_target_draft_changed
        );
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT 'GREEN verified routeStartProductionLeaders for current active target route versions' AS green_status;
END//
DELIMITER ;

CALL codex_verify_pressure_pump_route_start_leaders();
DROP PROCEDURE IF EXISTS codex_verify_pressure_pump_route_start_leaders;
