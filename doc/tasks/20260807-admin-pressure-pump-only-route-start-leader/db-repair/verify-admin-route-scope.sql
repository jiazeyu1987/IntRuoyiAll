-- GREEN: current active route scope for tenant 1 admin must be exactly routes 922119 and 980091.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_verify_admin_pressure_pump_only_scope;
DELIMITER //
CREATE PROCEDURE codex_verify_admin_pressure_pump_only_scope()
BEGIN
    DECLARE v_active_target_count INT DEFAULT 0;
    DECLARE v_direct_target_count INT DEFAULT 0;
    DECLARE v_effective_non_target_count INT DEFAULT 0;
    DECLARE v_message VARCHAR(255);

    SELECT COUNT(*) INTO v_active_target_count
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id AND r.tenant_id = rv.tenant_id
    WHERE rv.tenant_id = 1
      AND rv.route_id IN (922119, 980091)
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
      AND rv.deleted = b'0'
      AND r.deleted = b'0'
      AND JSON_VALID(rv.route_snapshot_json) = 1;

    DROP TEMPORARY TABLE IF EXISTS tmp_codex_admin_route_matches;
    CREATE TEMPORARY TABLE tmp_codex_admin_route_matches AS
    SELECT DISTINCT rv.id AS version_id,
           rv.route_id,
           UPPER(leader_item.source_type) AS source_type,
           source_item.source_id,
           CASE
               WHEN UPPER(leader_item.source_type) IN ('USER', 'USERS')
                    AND source_item.source_id = 1 THEN 'DIRECT_ADMIN'
               WHEN UPPER(leader_item.source_type) = 'ROLE'
                    AND sur.role_id IS NOT NULL THEN 'ADMIN_ROLE'
               ELSE 'OTHER'
           END AS match_type
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id AND r.tenant_id = rv.tenant_id
    JOIN JSON_TABLE(
        IF(JSON_VALID(rv.route_snapshot_json), rv.route_snapshot_json, JSON_OBJECT()),
        '$.configSnapshots.routeStartProductionLeaders[*]'
        COLUMNS(
            source_type VARCHAR(16) PATH '$.candidateSourceType',
            source_ids JSON PATH '$.candidateSourceIds'
        )
    ) AS leader_item
    JOIN JSON_TABLE(
        leader_item.source_ids,
        '$[*]' COLUMNS(source_id BIGINT PATH '$')
    ) AS source_item
    LEFT JOIN system_user_role sur
      ON sur.tenant_id = 1
     AND sur.user_id = 1
     AND sur.role_id = source_item.source_id
     AND sur.deleted = b'0'
    WHERE rv.tenant_id = 1
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
      AND rv.deleted = b'0'
      AND r.deleted = b'0'
      AND (
          (UPPER(leader_item.source_type) IN ('USER', 'USERS') AND source_item.source_id = 1)
          OR (UPPER(leader_item.source_type) = 'ROLE' AND sur.role_id IS NOT NULL)
      );

    SELECT matches.version_id,
           matches.route_id,
           r.code AS route_code,
           r.name AS route_name,
           matches.source_type,
           matches.source_id,
           matches.match_type
    FROM tmp_codex_admin_route_matches matches
    JOIN mes_pro_route r ON r.id = matches.route_id AND r.tenant_id = 1
    ORDER BY matches.route_id, matches.source_type, matches.source_id;

    SELECT COUNT(DISTINCT route_id) INTO v_direct_target_count
    FROM tmp_codex_admin_route_matches
    WHERE route_id IN (922119, 980091)
      AND match_type = 'DIRECT_ADMIN';

    SELECT COUNT(DISTINCT route_id) INTO v_effective_non_target_count
    FROM tmp_codex_admin_route_matches
    WHERE route_id NOT IN (922119, 980091)
      AND match_type IN ('DIRECT_ADMIN', 'ADMIN_ROLE');

    IF v_active_target_count <> 2 OR v_direct_target_count <> 2 OR v_effective_non_target_count <> 0 THEN
        SET v_message = CONCAT(
            'GREEN failed: activeTargets=', v_active_target_count,
            ', directTargetRoutes=', v_direct_target_count,
            ', effectiveNonTargetRoutes=', v_effective_non_target_count,
            ', expected=2/2/0'
        );
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT rv.id AS version_id,
           rv.route_id,
           r.code AS route_code,
           r.name AS route_name,
           JSON_EXTRACT(rv.route_snapshot_json,
               '$.configSnapshots.routeStartProductionLeaders') AS leaders
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id AND r.tenant_id = rv.tenant_id
    WHERE rv.tenant_id = 1
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
      AND rv.deleted = b'0'
      AND r.deleted = b'0'
    ORDER BY rv.route_id;

    SELECT 'GREEN verified exact admin route-start production leader scope' AS green_status;
END//
DELIMITER ;

CALL codex_verify_admin_pressure_pump_only_scope();
DROP PROCEDURE IF EXISTS codex_verify_admin_pressure_pump_only_scope;
