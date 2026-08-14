-- RED check: fail while target active route versions are missing routeStartProductionLeaders.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_red_missing_route_start_leaders;
DELIMITER //
CREATE PROCEDURE codex_red_missing_route_start_leaders()
BEGIN
    DECLARE v_missing_count INT DEFAULT 0;
    DECLARE v_message VARCHAR(255);

    SELECT COUNT(*) INTO v_missing_count
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id
    WHERE rv.id IN (448, 622)
      AND rv.tenant_id = 1
      AND r.tenant_id = 1
      AND rv.deleted = b'0'
      AND r.deleted = b'0'
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
      AND JSON_VALID(rv.route_snapshot_json)
      AND JSON_EXTRACT(rv.route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders') IS NULL;

    SELECT v_missing_count AS missing_route_start_production_leader_snapshots;

    IF v_missing_count = 2 THEN
        SET v_message = CONCAT('RED expected missing routeStartProductionLeaders count=', v_missing_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT 'RED precondition no longer reproduces' AS red_status;
END//
DELIMITER ;

CALL codex_red_missing_route_start_leaders();
DROP PROCEDURE IF EXISTS codex_red_missing_route_start_leaders;
