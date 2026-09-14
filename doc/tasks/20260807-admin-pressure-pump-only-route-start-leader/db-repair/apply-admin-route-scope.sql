-- Scope: local Docker MySQL int-ruoyi-mysql / ruoyi-vue-pro only.
-- Goal: add tenant 1 admin to active version 627 for route 922119.
-- Current-state contract: route 980091 already has admin; no non-target active route grants admin directly or by role.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_apply_admin_pressure_pump_only_scope;
DELIMITER //
CREATE PROCEDURE codex_apply_admin_pressure_pump_only_scope()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_updated_rows INT DEFAULT 0;
    DECLARE v_direct_target_count INT DEFAULT 0;
    DECLARE v_effective_non_target_count INT DEFAULT 0;
    DECLARE v_before_other_hash CHAR(64);
    DECLARE v_after_other_hash CHAR(64);
    DECLARE v_route_980091_hash CHAR(64);
    DECLARE v_route_980091_hash_after CHAR(64);
    DECLARE v_message VARCHAR(255);
    DECLARE v_admin_snapshot_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_route_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_admin_snapshot_name = CONVERT(UNHEX('E7919BE6B3B0E7AEA1E79086E59198EFBC8861646D696EEFBC89') USING utf8mb4);
    SET v_route_name = CONVERT(UNHEX('E79083E59B8AE689A9E5BCA0E58E8BE58A9BE6B3B5') USING utf8mb4);

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
        SET v_message = CONCAT('Expected tenant 1 admin count=1, actual=', v_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT rv.id
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id AND r.tenant_id = rv.tenant_id
    WHERE rv.id IN (622, 627)
      AND rv.tenant_id = 1
      AND rv.route_id IN (922119, 980091)
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
      AND rv.deleted = b'0'
      AND r.deleted = b'0'
    ORDER BY rv.id
    FOR UPDATE;

    SELECT COUNT(*) INTO v_count
    FROM mes_pro_route_version rv
    JOIN mes_pro_route r ON r.id = rv.route_id AND r.tenant_id = rv.tenant_id
    WHERE rv.tenant_id = 1
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
      AND rv.deleted = b'0'
      AND r.deleted = b'0'
      AND JSON_VALID(rv.route_snapshot_json) = 1
      AND (
          (rv.id = 627 AND rv.route_id = 922119 AND r.code = 'RT000028'
              AND HEX(r.name) = 'E79083E59B8AE689A9E5BCA0E58E8BE58A9BE6B3B5')
          OR
          (rv.id = 622 AND rv.route_id = 980091 AND r.code = 'RT000028-IDI'
              AND HEX(r.name) = 'E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5')
      );

    IF v_count <> 2 THEN
        SET v_message = CONCAT('Expected exact active versions 627/622 count=2, actual=', v_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pro_route_version
    WHERE id = 627
      AND tenant_id = 1
      AND route_id = 922119
      AND JSON_TYPE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots')) = 'OBJECT'
      AND JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.routeStartProductionLeaders') IS NULL;

    IF v_count <> 1 THEN
        SET v_message = CONCAT('Expected version 627 missing leader snapshot count=1, actual=', v_count);
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

    SELECT COUNT(DISTINCT route_id) INTO v_direct_target_count
    FROM tmp_codex_admin_route_matches
    WHERE route_id IN (922119, 980091)
      AND match_type = 'DIRECT_ADMIN';

    SELECT COUNT(DISTINCT route_id) INTO v_effective_non_target_count
    FROM tmp_codex_admin_route_matches
    WHERE route_id NOT IN (922119, 980091)
      AND match_type IN ('DIRECT_ADMIN', 'ADMIN_ROLE');

    IF v_direct_target_count <> 1 OR v_effective_non_target_count <> 0 THEN
        SET v_message = CONCAT(
            'Current scope changed before apply: directTargetRoutes=', v_direct_target_count,
            ', effectiveNonTargetRoutes=', v_effective_non_target_count,
            ', expected=1/0'
        );
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT SHA2(CAST(JSON_REMOVE(
               route_snapshot_json,
               '$.configSnapshots.routeStartProductionLeaders'
           ) AS CHAR CHARACTER SET utf8mb4), 256)
      INTO v_before_other_hash
    FROM mes_pro_route_version
    WHERE id = 627 AND tenant_id = 1;

    SELECT SHA2(route_snapshot_json, 256)
      INTO v_route_980091_hash
    FROM mes_pro_route_version
    WHERE id = 622 AND tenant_id = 1;

    UPDATE mes_pro_route_version
    SET route_snapshot_json = JSON_SET(
            route_snapshot_json,
            '$.configSnapshots.routeStartProductionLeaders',
            JSON_ARRAY(JSON_OBJECT(
                'productionLineId', 922119,
                'productionLineCode', 'RT000028',
                'productionLineName', v_route_name,
                'candidateSourceType', 'USERS',
                'candidateSourceIds', JSON_ARRAY(1),
                'candidateSourceNames', JSON_ARRAY(v_admin_snapshot_name),
                'sort', 1,
                'remark', 'codex-admin-pressure-pump-only-route-start-leader-20260807'
            ))
        ),
        updater = 'codex',
        update_time = NOW()
    WHERE id = 627
      AND tenant_id = 1
      AND route_id = 922119
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0';
    SET v_updated_rows = ROW_COUNT();

    IF v_updated_rows <> 1 THEN
        SET v_message = CONCAT('Expected updated rows=1, actual=', v_updated_rows);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    SELECT SHA2(CAST(JSON_REMOVE(
               route_snapshot_json,
               '$.configSnapshots.routeStartProductionLeaders'
           ) AS CHAR CHARACTER SET utf8mb4), 256)
      INTO v_after_other_hash
    FROM mes_pro_route_version
    WHERE id = 627 AND tenant_id = 1;

    SELECT SHA2(route_snapshot_json, 256)
      INTO v_route_980091_hash_after
    FROM mes_pro_route_version
    WHERE id = 622 AND tenant_id = 1;

    IF v_before_other_hash <> v_after_other_hash OR v_route_980091_hash <> v_route_980091_hash_after THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Non-target route snapshot content changed';
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
            'GREEN verification failed: directTargetRoutes=', v_direct_target_count,
            ', effectiveNonTargetRoutes=', v_effective_non_target_count,
            ', expected=2/0'
        );
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    COMMIT;

    SELECT 'APPLIED' AS status,
           v_updated_rows AS updated_rows,
           v_direct_target_count AS direct_target_routes,
           v_effective_non_target_count AS effective_non_target_routes;
END//
DELIMITER ;

CALL codex_apply_admin_pressure_pump_only_scope();
DROP PROCEDURE IF EXISTS codex_apply_admin_pressure_pump_only_scope;
