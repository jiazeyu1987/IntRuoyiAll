-- Scope: local Docker MySQL int-ruoyi-mysql / ruoyi-vue-pro only.
-- Goal: bind tenant 1 admin user 1 as route-start production leader for active route versions 448 and 622.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_apply_pressure_pump_route_start_leaders;
DELIMITER //
CREATE PROCEDURE codex_apply_pressure_pump_route_start_leaders()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_updated_rows INT DEFAULT 0;
    DECLARE v_verified_rows INT DEFAULT 0;
    DECLARE v_message VARCHAR(255);
    DECLARE v_task_remark VARCHAR(128) DEFAULT 'codex-admin-pressure-pump-route-start-leader-20260806';
    DECLARE v_admin_snapshot_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_route_name_922119 VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_route_name_980091 VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_admin_snapshot_name = CONVERT(UNHEX('E7919BE6B3B0E7AEA1E79086E59198EFBC8861646D696EEFBC89') USING utf8mb4);
    SET v_route_name_922119 = CONVERT(UNHEX('E79083E59B8AE689A9E5BCA0E58E8BE58A9BE6B3B5') USING utf8mb4);
    SET v_route_name_980091 = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4);

    START TRANSACTION;

    SELECT COUNT(*) INTO v_count
    FROM system_tenant t
    JOIN system_users u ON u.tenant_id = t.id
    WHERE t.id = 1
      AND HEX(t.name) = 'E88A8BE98193E6BA90E7A081'
      AND t.deleted = b'0'
      AND u.id = 1
      AND u.username = 'admin'
      AND u.deleted = b'0'
      AND u.status = 0;

    IF v_count <> 1 THEN
        SET v_message = CONCAT('Expected tenant 1 admin user count=1, actual=', v_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id
    WHERE rv.id IN (448, 622)
      AND rv.tenant_id = 1
      AND r.tenant_id = 1
      AND rv.deleted = b'0'
      AND r.deleted = b'0'
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
      AND (
          (rv.id = 448 AND r.id = 922119 AND r.code = 'RT000028' AND HEX(r.name) = 'E79083E59B8AE689A9E5BCA0E58E8BE58A9BE6B3B5')
          OR
          (rv.id = 622 AND r.id = 980091 AND r.code = 'RT000028-IDI' AND HEX(r.name) = 'E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5')
      );

    IF v_count <> 2 THEN
        SET v_message = CONCAT('Expected exact active route version count=2, actual=', v_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pro_route_version
    WHERE id IN (448, 622)
      AND tenant_id = 1
      AND route_snapshot_json IS NOT NULL
      AND JSON_VALID(route_snapshot_json)
      AND JSON_TYPE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots')) = 'OBJECT';

    IF v_count <> 2 THEN
        SET v_message = CONCAT('Expected valid configSnapshots count=2, actual=', v_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pro_route_version
    WHERE id IN (448, 622)
      AND tenant_id = 1
      AND JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders') IS NOT NULL;

    IF v_count <> 0 THEN
        SET v_message = CONCAT('routeStartProductionLeaders already exists for target versions, count=', v_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    UPDATE mes_pro_route_version
    SET route_snapshot_json = JSON_SET(
            route_snapshot_json,
            '$.configSnapshots.routeStartProductionLeaders',
            JSON_ARRAY(JSON_OBJECT(
                'productionLineId', 922119,
                'productionLineCode', 'RT000028',
                'productionLineName', v_route_name_922119,
                'candidateSourceType', 'USERS',
                'candidateSourceIds', JSON_ARRAY(1),
                'candidateSourceNames', JSON_ARRAY(v_admin_snapshot_name),
                'sort', 1,
                'remark', v_task_remark
            ))
        ),
        updater = 'codex',
        update_time = NOW()
    WHERE id = 448
      AND tenant_id = 1
      AND route_id = 922119
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0';
    SET v_updated_rows = v_updated_rows + ROW_COUNT();

    UPDATE mes_pro_route_version
    SET route_snapshot_json = JSON_SET(
            route_snapshot_json,
            '$.configSnapshots.routeStartProductionLeaders',
            JSON_ARRAY(JSON_OBJECT(
                'productionLineId', 980091,
                'productionLineCode', 'RT000028-IDI',
                'productionLineName', v_route_name_980091,
                'candidateSourceType', 'USERS',
                'candidateSourceIds', JSON_ARRAY(1),
                'candidateSourceNames', JSON_ARRAY(v_admin_snapshot_name),
                'sort', 1,
                'remark', v_task_remark
            ))
        ),
        updater = 'codex',
        update_time = NOW()
    WHERE id = 622
      AND tenant_id = 1
      AND route_id = 980091
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0';
    SET v_updated_rows = v_updated_rows + ROW_COUNT();

    IF v_updated_rows <> 2 THEN
        SET v_message = CONCAT('Expected updated rows=2, actual=', v_updated_rows);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT COUNT(*) INTO v_verified_rows
    FROM mes_pro_route_version
    WHERE id IN (448, 622)
      AND tenant_id = 1
      AND JSON_LENGTH(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders')) = 1
      AND JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceType')) = 'USERS'
      AND JSON_CONTAINS(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceIds'), '1')
      AND JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].candidateSourceNames[0]')) = v_admin_snapshot_name
      AND (
          (id = 448 AND CAST(JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].productionLineId')) AS UNSIGNED) = 922119)
          OR
          (id = 622 AND CAST(JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders[0].productionLineId')) AS UNSIGNED) = 980091)
      );

    IF v_verified_rows <> 2 THEN
        SET v_message = CONCAT('Expected verified rows=2, actual=', v_verified_rows);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    COMMIT;

    SELECT 'APPLIED' AS status, v_updated_rows AS updated_rows, v_verified_rows AS verified_rows;
END//
DELIMITER ;

CALL codex_apply_pressure_pump_route_start_leaders();
DROP PROCEDURE IF EXISTS codex_apply_pressure_pump_route_start_leaders;
