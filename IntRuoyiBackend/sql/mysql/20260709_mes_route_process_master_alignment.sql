-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_mes_base_schema; type=data; riskLevel=medium
-- Goal:
--   Align active mes_pro_route_process.process_id values to the canonical
--   mes_pro_process master row for the same tenant_id + code.
-- Scope:
--   All active routes and route-process rows. This script does not modify
--   mes_pro_process and does not merge or delete duplicated process masters.

DROP PROCEDURE IF EXISTS intruoyi_align_mes_route_process_to_process_master_by_code;
DELIMITER $$
CREATE PROCEDURE intruoyi_align_mes_route_process_to_process_master_by_code()
BEGIN
    DECLARE route_process_alignment_error_message VARCHAR(128);
    SET route_process_alignment_error_message = @alignment_error_message;

    IF route_process_alignment_error_message IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = route_process_alignment_error_message;
    END IF;
END$$
DELIMITER ;

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_missing_process_id;
CREATE TEMPORARY TABLE tmp_route_process_alignment_missing_process_id AS
SELECT
    rp.`id` AS `route_process_id`,
    rp.`tenant_id`,
    rp.`route_id`
FROM `mes_pro_route_process` rp
INNER JOIN `mes_pro_route` r
        ON r.`id` = rp.`route_id`
       AND r.`tenant_id` = rp.`tenant_id`
       AND r.`deleted` = b'0'
WHERE rp.`deleted` = b'0'
  AND rp.`process_id` IS NULL;

SET @missing_process_id_count = (
    SELECT COUNT(*) FROM tmp_route_process_alignment_missing_process_id
);

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_missing_master;
CREATE TEMPORARY TABLE tmp_route_process_alignment_missing_master AS
SELECT
    rp.`id` AS `route_process_id`,
    rp.`tenant_id`,
    rp.`route_id`,
    rp.`process_id`
FROM `mes_pro_route_process` rp
INNER JOIN `mes_pro_route` r
        ON r.`id` = rp.`route_id`
       AND r.`tenant_id` = rp.`tenant_id`
       AND r.`deleted` = b'0'
LEFT JOIN `mes_pro_process` p
       ON p.`id` = rp.`process_id`
      AND p.`tenant_id` = rp.`tenant_id`
      AND p.`deleted` = b'0'
WHERE rp.`deleted` = b'0'
  AND rp.`process_id` IS NOT NULL
  AND p.`id` IS NULL;

SET @missing_process_master_count = (
    SELECT COUNT(*) FROM tmp_route_process_alignment_missing_master
);

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_missing_process_code;
CREATE TEMPORARY TABLE tmp_route_process_alignment_missing_process_code AS
SELECT
    rp.`id` AS `route_process_id`,
    rp.`tenant_id`,
    rp.`route_id`,
    rp.`process_id`
FROM `mes_pro_route_process` rp
INNER JOIN `mes_pro_route` r
        ON r.`id` = rp.`route_id`
       AND r.`tenant_id` = rp.`tenant_id`
       AND r.`deleted` = b'0'
INNER JOIN `mes_pro_process` p
        ON p.`id` = rp.`process_id`
       AND p.`tenant_id` = rp.`tenant_id`
       AND p.`deleted` = b'0'
WHERE rp.`deleted` = b'0'
  AND (p.`code` IS NULL OR p.`code` = '');

SET @missing_process_code_count = (
    SELECT COUNT(*) FROM tmp_route_process_alignment_missing_process_code
);

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_referenced_process_code;
CREATE TEMPORARY TABLE tmp_route_process_alignment_referenced_process_code AS
SELECT DISTINCT
    p.`tenant_id`,
    p.`code`
FROM `mes_pro_route_process` rp
INNER JOIN `mes_pro_route` r
        ON r.`id` = rp.`route_id`
       AND r.`tenant_id` = rp.`tenant_id`
       AND r.`deleted` = b'0'
INNER JOIN `mes_pro_process` p
        ON p.`id` = rp.`process_id`
       AND p.`tenant_id` = rp.`tenant_id`
       AND p.`deleted` = b'0'
WHERE rp.`deleted` = b'0'
  AND p.`code` IS NOT NULL
  AND p.`code` <> '';

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_code_name_conflict;
CREATE TEMPORARY TABLE tmp_route_process_alignment_code_name_conflict AS
SELECT
    p.`tenant_id`,
    p.`code`,
    COUNT(DISTINCT p.`name`) AS `distinct_name_count`,
    GROUP_CONCAT(DISTINCT p.`name` ORDER BY p.`name` SEPARATOR ',') AS `names`
FROM `mes_pro_process` p
INNER JOIN tmp_route_process_alignment_referenced_process_code rc
        ON rc.`tenant_id` = p.`tenant_id`
       AND rc.`code` = p.`code`
WHERE p.`deleted` = b'0'
  AND p.`code` IS NOT NULL
  AND p.`code` <> ''
GROUP BY p.`tenant_id`, p.`code`
HAVING COUNT(DISTINCT p.`name`) > 1;

SET @duplicate_code_name_conflict_count = (
    SELECT COUNT(*) FROM tmp_route_process_alignment_code_name_conflict
);

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_canonical_process;
CREATE TEMPORARY TABLE tmp_route_process_alignment_canonical_process AS
SELECT
    p.`tenant_id`,
    p.`code`,
    MIN(`id`) AS `canonical_process_id`
FROM `mes_pro_process` p
INNER JOIN tmp_route_process_alignment_referenced_process_code rc
        ON rc.`tenant_id` = p.`tenant_id`
       AND rc.`code` = p.`code`
WHERE p.`deleted` = b'0'
  AND p.`code` IS NOT NULL
  AND p.`code` <> ''
GROUP BY p.`tenant_id`, p.`code`;

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_updates;
CREATE TEMPORARY TABLE tmp_route_process_alignment_updates AS
SELECT
    rp.`id` AS `route_process_id`,
    rp.`tenant_id`,
    rp.`route_id`,
    rp.`process_id` AS `current_process_id`,
    p.`code`,
    p.`name`,
    cp.`canonical_process_id`
FROM `mes_pro_route_process` rp
INNER JOIN `mes_pro_route` r
        ON r.`id` = rp.`route_id`
       AND r.`tenant_id` = rp.`tenant_id`
       AND r.`deleted` = b'0'
INNER JOIN `mes_pro_process` p
        ON p.`id` = rp.`process_id`
       AND p.`tenant_id` = rp.`tenant_id`
       AND p.`deleted` = b'0'
LEFT JOIN tmp_route_process_alignment_canonical_process cp
       ON cp.`tenant_id` = p.`tenant_id`
      AND cp.`code` = p.`code`
WHERE rp.`deleted` = b'0'
  AND p.`code` IS NOT NULL
  AND p.`code` <> ''
  AND cp.`canonical_process_id` IS NOT NULL
  AND rp.`process_id` <> cp.`canonical_process_id`;

DROP TEMPORARY TABLE IF EXISTS tmp_route_process_alignment_canonical_missing;
CREATE TEMPORARY TABLE tmp_route_process_alignment_canonical_missing AS
SELECT
    rp.`id` AS `route_process_id`,
    rp.`tenant_id`,
    rp.`route_id`,
    rp.`process_id`,
    p.`code`
FROM `mes_pro_route_process` rp
INNER JOIN `mes_pro_route` r
        ON r.`id` = rp.`route_id`
       AND r.`tenant_id` = rp.`tenant_id`
       AND r.`deleted` = b'0'
INNER JOIN `mes_pro_process` p
        ON p.`id` = rp.`process_id`
       AND p.`tenant_id` = rp.`tenant_id`
       AND p.`deleted` = b'0'
LEFT JOIN tmp_route_process_alignment_canonical_process cp
       ON cp.`tenant_id` = p.`tenant_id`
      AND cp.`code` = p.`code`
WHERE rp.`deleted` = b'0'
  AND p.`code` IS NOT NULL
  AND p.`code` <> ''
  AND cp.`canonical_process_id` IS NULL;

SET @canonical_process_missing_count = (
    SELECT COUNT(*) FROM tmp_route_process_alignment_canonical_missing
);

SELECT 'ROUTE_PROCESS_ALIGNMENT_PREVIEW' AS `check_name`,
    @missing_process_id_count AS `missing_process_id_count`,
    @missing_process_master_count AS `missing_process_master_count`,
    @missing_process_code_count AS `missing_process_code_count`,
    @duplicate_code_name_conflict_count AS `duplicate_code_name_conflict_count`,
    @canonical_process_missing_count AS `canonical_process_missing_count`,
    COUNT(*) AS `rows_to_update`
FROM tmp_route_process_alignment_updates;

SET @alignment_error_message = CASE
    WHEN @missing_process_id_count > 0
        THEN 'MES ROUTE PROCESS ALIGNMENT MISSING PROCESS_ID'
    WHEN @missing_process_master_count > 0
        THEN 'MES ROUTE PROCESS ALIGNMENT MISSING PROCESS MASTER'
    WHEN @missing_process_code_count > 0
        THEN 'MES ROUTE PROCESS ALIGNMENT MISSING PROCESS CODE'
    WHEN @duplicate_code_name_conflict_count > 0
        THEN 'MES ROUTE PROCESS ALIGNMENT DUPLICATE CODE NAME CONFLICT'
    WHEN @canonical_process_missing_count > 0
        THEN 'MES ROUTE PROCESS ALIGNMENT CANONICAL PROCESS MISSING'
    ELSE NULL
END;

CALL intruoyi_align_mes_route_process_to_process_master_by_code();

START TRANSACTION;

UPDATE `mes_pro_route_process` rp
INNER JOIN tmp_route_process_alignment_updates u
        ON rp.`id` = u.`route_process_id`
SET rp.`process_id` = u.`canonical_process_id`,
    rp.`update_time` = NOW()
WHERE rp.`deleted` = b'0'
  AND u.`current_process_id` <> u.`canonical_process_id`;

SELECT 'ROUTE_PROCESS_ALIGNMENT_APPLIED' AS `check_name`,
    ROW_COUNT() AS `updated_rows`;

COMMIT;

DROP PROCEDURE IF EXISTS intruoyi_align_mes_route_process_to_process_master_by_code;
