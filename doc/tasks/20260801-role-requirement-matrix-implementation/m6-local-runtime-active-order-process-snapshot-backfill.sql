-- M6 local runtime active-order process snapshot backfill for RRM-20260801.
-- Run after 20260802_mes_process_pool_active_order_process_snapshot.sql.

DROP PROCEDURE IF EXISTS codex_rrm_assert;
DELIMITER $$
CREATE PROCEDURE codex_rrm_assert(IN p_condition tinyint, IN p_message varchar(255))
BEGIN
    IF p_condition = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_message;
    END IF;
END$$
DELIMITER ;

SET @rrm_tenant_id := 1;
SET @rrm_work_order_id := 980008;
SET @rrm_route_id := 922119;
SET @rrm_route_version_id := 448;
SET @rrm_actor := 'codex-rrm-m6';

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_process_pool_active_order
    WHERE work_order_id = @rrm_work_order_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND erp_fixed_quantity_snapshot IS NOT NULL
      AND business_status = active_status
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM active order authority fields are not backfilled');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_schedule_order
    WHERE work_order_id = @rrm_work_order_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM schedule order snapshot is missing');

START TRANSACTION;

INSERT INTO mes_pro_process_pool_active_order_process_snapshot (
    active_order_id, work_order_id, route_id, route_version_id, route_process_id,
    process_id, erp_fixed_quantity_snapshot, production_quantity_factor_snapshot,
    planned_quantity_snapshot, creator, updater, tenant_id
)
SELECT
    ao.id,
    ao.work_order_id,
    ao.route_id,
    ao.route_version_id,
    sop.route_process_id,
    sop.process_id,
    ao.erp_fixed_quantity_snapshot,
    sop.production_quantity_factor,
    sop.planned_quantity,
    @rrm_actor,
    @rrm_actor,
    ao.tenant_id
FROM mes_pro_process_pool_active_order ao
JOIN mes_pro_schedule_order so
  ON so.work_order_id = ao.work_order_id
 AND so.route_id = ao.route_id
 AND so.route_version_id = ao.route_version_id
 AND so.deleted = b'0'
 AND so.tenant_id = ao.tenant_id
JOIN mes_pro_schedule_order_process sop
  ON sop.schedule_order_id = so.id
 AND sop.enabled = b'1'
 AND sop.deleted = b'0'
 AND sop.tenant_id = so.tenant_id
WHERE ao.work_order_id = @rrm_work_order_id
  AND ao.route_id = @rrm_route_id
  AND ao.route_version_id = @rrm_route_version_id
  AND ao.deleted = b'0'
  AND ao.tenant_id = @rrm_tenant_id
  AND NOT EXISTS (
      SELECT 1
      FROM mes_pro_process_pool_active_order_process_snapshot existing
      WHERE existing.active_order_id = ao.id
        AND existing.route_process_id = sop.route_process_id
        AND existing.process_id = sop.process_id
        AND existing.deleted = b'0'
        AND existing.tenant_id = ao.tenant_id
  );

CALL codex_rrm_assert((
    SELECT COUNT(*)
    FROM mes_pro_process_pool_active_order_process_snapshot snap
    JOIN mes_pro_process_pool_active_order ao ON ao.id = snap.active_order_id
    WHERE ao.work_order_id = @rrm_work_order_id
      AND snap.deleted = b'0'
      AND snap.tenant_id = @rrm_tenant_id
) = (
    SELECT COUNT(*)
    FROM mes_pro_schedule_order_process sop
    JOIN mes_pro_schedule_order so ON so.id = sop.schedule_order_id
    WHERE so.work_order_id = @rrm_work_order_id
      AND sop.enabled = b'1'
      AND sop.deleted = b'0'
      AND sop.tenant_id = @rrm_tenant_id
), 'RRM active-order process snapshot count does not match schedule snapshot count');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 0
    FROM mes_pro_process_pool_active_order_process_snapshot snap
    WHERE snap.work_order_id = @rrm_work_order_id
      AND snap.deleted = b'0'
      AND snap.tenant_id = @rrm_tenant_id
      AND ROUND(snap.erp_fixed_quantity_snapshot * snap.production_quantity_factor_snapshot, 6)
          <> ROUND(snap.planned_quantity_snapshot, 6)
), 'RRM active-order process snapshot planned quantity is not factor-derived');

COMMIT;

DROP PROCEDURE IF EXISTS codex_rrm_assert;
