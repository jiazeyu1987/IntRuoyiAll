-- RED: tenant 1 admin must be directly configured on exactly the two target active routes.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_red_admin_pressure_pump_only_scope;
DELIMITER //
CREATE PROCEDURE codex_red_admin_pressure_pump_only_scope()
BEGIN
    DECLARE v_admin_count INT DEFAULT 0;
    DECLARE v_active_target_count INT DEFAULT 0;
    DECLARE v_direct_target_count INT DEFAULT 0;
    DECLARE v_effective_non_target_count INT DEFAULT 0;
    DECLARE v_message VARCHAR(255);

    SELECT COUNT(*) INTO v_admin_count
    FROM system_tenant t
    JOIN system_users u ON u.tenant_id = t.id
    WHERE t.id = 1
      AND HEX(t.name) = 'E88A8BE98193E6BA90E7A081'
      AND t.deleted = b'0'
      AND u.id = 1
      AND u.username = 'admin'
      AND u.deleted = b'0'
      AND u.status = 0;

    IF v_admin_count <> 1 THEN
        SET v_message = CONCAT('RED precondition failed: tenant 1 admin count=', v_admin_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

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

    IF v_active_target_count <> 2 THEN
        SET v_message = CONCAT('RED precondition failed: active target count=', v_active_target_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

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

    IF v_direct_target_count <> 2 OR v_effective_non_target_count <> 0 THEN
        SET v_message = CONCAT(
            'RED expected failure: directTargetRoutes=', v_direct_target_count,
            ', effectiveNonTargetRoutes=', v_effective_non_target_count,
            ', expected=2/0'
        );
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT 'RED unexpectedly passed: admin scope already exact' AS red_status;
END//
DELIMITER ;

CALL codex_red_admin_pressure_pump_only_scope();
DROP PROCEDURE IF EXISTS codex_red_admin_pressure_pump_only_scope;
