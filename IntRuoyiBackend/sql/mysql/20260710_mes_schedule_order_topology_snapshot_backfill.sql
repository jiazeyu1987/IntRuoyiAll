-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_mes_route_process_single_entry_multi_exit; type=data; riskLevel=medium
-- MES 排产工序拓扑快照补全：仅将升级前全部缺少拓扑字段的历史快照按原 sort/id 顺序还原为线性链。
-- Rollback: restore mes_pro_schedule_order_process from the pre-migration database backup.

DROP PROCEDURE IF EXISTS intruoyi_backfill_mes_schedule_order_topology_snapshot;

DELIMITER //

CREATE PROCEDURE intruoyi_backfill_mes_schedule_order_topology_snapshot()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing mes_pro_schedule_order_process';
  END IF;
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
      AND column_name = 'predecessor_route_process_id'
  ) OR NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
      AND column_name = 'root_process_flag'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing schedule order topology snapshot columns';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM mes_pro_schedule_order_process
    WHERE deleted = b'0'
    GROUP BY tenant_id, schedule_order_id
    HAVING SUM(CASE WHEN predecessor_route_process_id IS NOT NULL THEN 1 ELSE 0 END) = 0
       AND SUM(CASE WHEN root_process_flag = b'1' THEN 1 ELSE 0 END) = 0
       AND (
         COUNT(route_process_id) <> COUNT(*)
         OR COUNT(DISTINCT route_process_id) <> COUNT(*)
       )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'legacy schedule order topology snapshot contains null route process';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_mes_schedule_order_topology_backfill;
  CREATE TEMPORARY TABLE tmp_mes_schedule_order_topology_backfill AS
  SELECT snapshot.id,
         LAG(snapshot.route_process_id) OVER (
           PARTITION BY snapshot.tenant_id, snapshot.schedule_order_id
           ORDER BY snapshot.sort, snapshot.id
         ) AS predecessor_route_process_id
  FROM mes_pro_schedule_order_process snapshot
  INNER JOIN (
    SELECT tenant_id, schedule_order_id
    FROM mes_pro_schedule_order_process
    WHERE deleted = b'0'
    GROUP BY tenant_id, schedule_order_id
    HAVING SUM(CASE WHEN predecessor_route_process_id IS NOT NULL THEN 1 ELSE 0 END) = 0
       AND SUM(CASE WHEN root_process_flag = b'1' THEN 1 ELSE 0 END) = 0
  ) legacy
    ON legacy.tenant_id = snapshot.tenant_id
   AND legacy.schedule_order_id = snapshot.schedule_order_id
  WHERE snapshot.deleted = b'0';

  UPDATE mes_pro_schedule_order_process snapshot
  INNER JOIN tmp_mes_schedule_order_topology_backfill backfill
    ON backfill.id = snapshot.id
  SET snapshot.predecessor_route_process_id = backfill.predecessor_route_process_id,
      snapshot.root_process_flag =
        IF(backfill.predecessor_route_process_id IS NULL, b'1', b'0');

  DROP TEMPORARY TABLE IF EXISTS tmp_mes_schedule_order_topology_backfill;
END//

DELIMITER ;

CALL intruoyi_backfill_mes_schedule_order_topology_snapshot();

DROP PROCEDURE IF EXISTS intruoyi_backfill_mes_schedule_order_topology_snapshot;
