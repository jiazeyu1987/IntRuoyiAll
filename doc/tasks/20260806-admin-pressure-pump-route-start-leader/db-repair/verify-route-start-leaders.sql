-- GREEN verification for routeStartProductionLeaders binding.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_verify_pressure_pump_route_start_leaders;
DELIMITER //
CREATE PROCEDURE codex_verify_pressure_pump_route_start_leaders()
BEGIN
    DECLARE v_verified_rows INT DEFAULT 0;
    DECLARE v_non_target_changed INT DEFAULT 0;
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
    WHERE rv.id IN (448, 622)
    ORDER BY rv.id;

    SELECT COUNT(*) INTO v_verified_rows
    FROM mes_pro_route_version
    WHERE id IN (448, 622)
      AND tenant_id = 1
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0'
      AND JSON_LENGTH(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders')) = 1
      AND JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceType')) = 'USERS'
      AND JSON_CONTAINS(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceIds'), '1')
      AND JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceNames[0]')) = v_admin_snapshot_name
      AND (
          (id = 448 AND CAST(JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].productionLineId')) AS UNSIGNED) = 922119)
          OR
          (id = 622 AND CAST(JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].productionLineId')) AS UNSIGNED) = 980091)
      );

    SELECT COUNT(*) INTO v_non_target_changed
    FROM mes_pro_route_version
    WHERE (route_id = 922273 OR id = 490)
      AND JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders') IS NOT NULL;

    IF v_verified_rows <> 2 OR v_non_target_changed <> 0 THEN
        SET v_message = CONCAT('GREEN verification failed: verified=', v_verified_rows, ', nonTargetChanged=', v_non_target_changed);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT 'GREEN verified routeStartProductionLeaders for route versions 448 and 622 only' AS green_status;
END//
DELIMITER ;

CALL codex_verify_pressure_pump_route_start_leaders();
DROP PROCEDURE IF EXISTS codex_verify_pressure_pump_route_start_leaders;
