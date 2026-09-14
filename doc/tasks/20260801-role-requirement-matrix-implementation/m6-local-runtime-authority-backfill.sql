-- M6 local runtime fixture backfill for RRM-20260801.
-- Scope: local tenant 1, work order RRM-20260801-PP-MO-001 only.
-- Purpose: unblock official M1 authority migration without defaulting or guessing source data.

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
SET @rrm_work_order_code := 'RRM-20260801-PP-MO-001';
SET @rrm_route_id := 922119;
SET @rrm_route_version_id := 448;
SET @rrm_schedule_order_code := 'SCH-RRM-20260801-PP-MO-001-M6';
SET @rrm_actor := 'codex-rrm-m6';

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_work_order
    WHERE id = @rrm_work_order_id
      AND code = @rrm_work_order_code
      AND quantity IS NOT NULL
      AND quantity > 0
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM work order fixture is missing or not formally usable');

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_route_version
    WHERE id = @rrm_route_version_id
      AND route_id = @rrm_route_id
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM route version fixture is not the active V21 source');

CALL codex_rrm_assert((
    SELECT COUNT(*) > 0
    FROM mes_pro_route_process rp
    JOIN mes_pro_route_flow_process_config cfg
      ON cfg.route_id = rp.route_id
     AND cfg.route_process_id = rp.id
     AND cfg.use_type = 'SCHEDULE'
     AND cfg.enabled = b'1'
     AND cfg.production_quantity_factor > 0
     AND cfg.deleted = b'0'
     AND cfg.tenant_id = rp.tenant_id
    WHERE rp.route_id = @rrm_route_id
      AND rp.deleted = b'0'
      AND rp.tenant_id = @rrm_tenant_id
), 'RRM route has no usable SCHEDULE production quantity factors');

CALL codex_rrm_assert((
    SELECT COUNT(*)
    FROM mes_pro_route_process rp
    WHERE rp.route_id = @rrm_route_id
      AND rp.deleted = b'0'
      AND rp.tenant_id = @rrm_tenant_id
) = (
    SELECT COUNT(DISTINCT rp.id)
    FROM mes_pro_route_process rp
    JOIN mes_pro_route_flow_process_config cfg
      ON cfg.route_id = rp.route_id
     AND cfg.route_process_id = rp.id
     AND cfg.use_type = 'SCHEDULE'
     AND cfg.enabled = b'1'
     AND cfg.production_quantity_factor > 0
     AND cfg.deleted = b'0'
     AND cfg.tenant_id = rp.tenant_id
    WHERE rp.route_id = @rrm_route_id
      AND rp.deleted = b'0'
      AND rp.tenant_id = @rrm_tenant_id
), 'Not every RRM route process has one formal SCHEDULE factor');

START TRANSACTION;

UPDATE mes_pro_process_pool_active_order ao
JOIN mes_pro_work_order wo
  ON wo.id = ao.work_order_id
 AND wo.code = @rrm_work_order_code
 AND wo.deleted = b'0'
 AND wo.tenant_id = ao.tenant_id
JOIN mes_pro_route_version rv
  ON rv.id = @rrm_route_version_id
 AND rv.route_id = @rrm_route_id
 AND rv.active = b'1'
 AND rv.lifecycle_status = 'ACTIVE'
 AND rv.deleted = b'0'
 AND rv.tenant_id = ao.tenant_id
SET ao.route_id = rv.route_id,
    ao.route_version_id = rv.id,
    ao.erp_fixed_quantity_snapshot = wo.quantity,
    ao.business_status = ao.active_status,
    ao.updater = @rrm_actor
WHERE ao.work_order_id = @rrm_work_order_id
  AND ao.deleted = b'0'
  AND ao.tenant_id = @rrm_tenant_id;

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_process_pool_active_order
    WHERE work_order_id = @rrm_work_order_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND erp_fixed_quantity_snapshot IS NOT NULL
      AND business_status IS NOT NULL
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM active order authority backfill did not produce exactly one row');

INSERT INTO mes_pro_schedule_order (
    code, source_work_order_id, source_work_order_code, source_order_code,
    work_order_id, erp_work_order_code, product_id, quantity, promise_date,
    priority_no, status, diff_status, risk_status, route_status, auto_schedulable,
    route_id, route_version_id, route_version, total_quantity, completed_quantity,
    uncompleted_quantity, progress_percent, frozen, manual_finished,
    source_snapshot_json, route_snapshot_json, remark, creator, updater, tenant_id,
    planned_quantity, priority, active_flag, scheduled_quantity, reported_quantity,
    product_code, product_name, product_specification, route_code, route_name
)
SELECT
    @rrm_schedule_order_code,
    wo.id,
    wo.code,
    wo.order_source_code,
    wo.id,
    wo.code,
    wo.product_id,
    wo.quantity,
    CURDATE(),
    100,
    0,
    0,
    0,
    1,
    b'0',
    r.id,
    rv.id,
    rv.version_no,
    wo.quantity,
    0,
    wo.quantity,
    0,
    b'0',
    b'0',
    JSON_OBJECT('workOrderId', wo.id, 'workOrderCode', wo.code, 'quantity', wo.quantity),
    JSON_OBJECT('routeId', r.id, 'routeCode', r.code, 'routeVersionId', rv.id, 'routeVersion', rv.version_no),
    'RRM M6 local runtime fixture schedule order',
    @rrm_actor,
    @rrm_actor,
    wo.tenant_id,
    wo.quantity,
    5,
    1,
    wo.quantity,
    0,
    item.code,
    item.name,
    item.specification,
    r.code,
    r.name
FROM mes_pro_work_order wo
JOIN mes_pro_route r
  ON r.id = @rrm_route_id
 AND r.deleted = b'0'
 AND r.tenant_id = wo.tenant_id
JOIN mes_pro_route_version rv
  ON rv.id = @rrm_route_version_id
 AND rv.route_id = r.id
 AND rv.active = b'1'
 AND rv.lifecycle_status = 'ACTIVE'
 AND rv.deleted = b'0'
 AND rv.tenant_id = wo.tenant_id
JOIN mes_md_item item
  ON item.id = wo.product_id
 AND item.deleted = b'0'
 AND item.tenant_id = wo.tenant_id
WHERE wo.id = @rrm_work_order_id
  AND wo.code = @rrm_work_order_code
  AND wo.deleted = b'0'
  AND wo.tenant_id = @rrm_tenant_id
  AND NOT EXISTS (
      SELECT 1
      FROM mes_pro_schedule_order existing
      WHERE existing.work_order_id = wo.id
        AND existing.deleted = b'0'
        AND existing.tenant_id = wo.tenant_id
  );

CALL codex_rrm_assert((
    SELECT COUNT(*) = 1
    FROM mes_pro_schedule_order
    WHERE work_order_id = @rrm_work_order_id
      AND route_id = @rrm_route_id
      AND route_version_id = @rrm_route_version_id
      AND deleted = b'0'
      AND tenant_id = @rrm_tenant_id
), 'RRM schedule order fixture did not produce exactly one effective row');

INSERT INTO mes_pro_schedule_order_process (
    schedule_order_id, route_process_id, predecessor_route_process_id, root_process_flag,
    route_version_id, process_id, process_code, process_name, sort, enabled,
    planned_quantity, reported_quantity, remaining_quantity, progress_percent,
    key_process_flag, bottleneck_flag, remark, creator, updater, tenant_id,
    source_work_order_id, route_id, scheduling_enabled, status,
    production_quantity_factor, resource_snapshot_json
)
SELECT
    so.id,
    rp.id,
    prev.id,
    IF(prev.id IS NULL, b'1', b'0'),
    @rrm_route_version_id,
    rp.process_id,
    proc.code,
    proc.name,
    rp.sort,
    cfg.enabled,
    ROUND(wo.quantity * cfg.production_quantity_factor, 6),
    0,
    ROUND(wo.quantity * cfg.production_quantity_factor, 6),
    0,
    COALESCE(rp.key_flag, b'0'),
    b'0',
    rp.remark,
    @rrm_actor,
    @rrm_actor,
    so.tenant_id,
    wo.id,
    rp.route_id,
    b'1',
    0,
    cfg.production_quantity_factor,
    JSON_OBJECT(
        'source', 'RRM_M6_LOCAL_RUNTIME',
        'routeProcessId', rp.id,
        'processId', rp.process_id,
        'productionQuantityFactor', cfg.production_quantity_factor,
        'plannedQuantity', ROUND(wo.quantity * cfg.production_quantity_factor, 6)
    )
FROM mes_pro_schedule_order so
JOIN mes_pro_work_order wo
  ON wo.id = so.work_order_id
 AND wo.deleted = b'0'
 AND wo.tenant_id = so.tenant_id
JOIN mes_pro_route_process rp
  ON rp.route_id = so.route_id
 AND rp.deleted = b'0'
 AND rp.tenant_id = so.tenant_id
JOIN mes_pro_route_flow_process_config cfg
  ON cfg.route_id = rp.route_id
 AND cfg.route_process_id = rp.id
 AND cfg.use_type = 'SCHEDULE'
 AND cfg.enabled = b'1'
 AND cfg.production_quantity_factor > 0
 AND cfg.deleted = b'0'
 AND cfg.tenant_id = rp.tenant_id
JOIN mes_pro_process proc
  ON proc.id = rp.process_id
 AND proc.deleted = b'0'
 AND proc.tenant_id = rp.tenant_id
LEFT JOIN mes_pro_route_process prev
  ON prev.route_id = rp.route_id
 AND prev.deleted = b'0'
 AND prev.tenant_id = rp.tenant_id
 AND prev.sort = (
      SELECT MAX(prev2.sort)
      FROM mes_pro_route_process prev2
      WHERE prev2.route_id = rp.route_id
        AND prev2.deleted = b'0'
        AND prev2.tenant_id = rp.tenant_id
        AND prev2.sort < rp.sort
 )
WHERE so.work_order_id = @rrm_work_order_id
  AND so.deleted = b'0'
  AND so.tenant_id = @rrm_tenant_id
  AND NOT EXISTS (
      SELECT 1
      FROM mes_pro_schedule_order_process existing
      WHERE existing.schedule_order_id = so.id
        AND existing.route_process_id = rp.id
        AND existing.deleted = b'0'
        AND existing.tenant_id = so.tenant_id
  );

CALL codex_rrm_assert((
    SELECT COUNT(*)
    FROM mes_pro_schedule_order_process sop
    JOIN mes_pro_schedule_order so ON so.id = sop.schedule_order_id
    WHERE so.work_order_id = @rrm_work_order_id
      AND sop.enabled = b'1'
      AND sop.deleted = b'0'
      AND sop.tenant_id = @rrm_tenant_id
) = (
    SELECT COUNT(*)
    FROM mes_pro_route_process rp
    WHERE rp.route_id = @rrm_route_id
      AND rp.deleted = b'0'
      AND rp.tenant_id = @rrm_tenant_id
), 'RRM schedule process snapshot count does not match active route process count');

COMMIT;

DROP PROCEDURE IF EXISTS codex_rrm_assert;
