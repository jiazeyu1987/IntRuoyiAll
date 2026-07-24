-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_mes_schedule_order_topology_snapshot_backfill; type=data; riskLevel=medium
-- MES eDHR 历史工序拓扑快照补全：按批次内不同路线工序的原始排序恢复线性直接前置关系。
-- Rollback: restore mes_pro_edhr_batch_execution_task predecessor_route_process_id and root_process_flag from the pre-migration database backup.

DROP PROCEDURE IF EXISTS intruoyi_backfill_mes_edhr_batch_task_topology_snapshot;

DELIMITER //

CREATE PROCEDURE intruoyi_backfill_mes_edhr_batch_task_topology_snapshot()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'missing mes_pro_edhr_batch_execution_task';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution_task'
      AND column_name = 'predecessor_route_process_id'
  ) OR NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution_task'
      AND column_name = 'root_process_flag'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'missing eDHR topology snapshot columns';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM mes_pro_edhr_batch_execution_task task
    WHERE task.deleted = b'0'
      AND (
        task.node_type = 'ROUTE_FORM'
        OR (
          task.node_type IS NULL
          AND task.route_process_id IS NOT NULL
          AND task.batch_record_report_id IS NOT NULL
        )
      )
    GROUP BY task.tenant_id, task.batch_execution_id
    HAVING SUM(
             CASE
               WHEN task.predecessor_route_process_id IS NOT NULL
                 OR COALESCE(task.root_process_flag, b'0') = b'1'
               THEN 1 ELSE 0
             END
           ) > 0
       AND SUM(
             CASE
               WHEN task.predecessor_route_process_id IS NULL
                AND COALESCE(task.root_process_flag, b'0') = b'0'
               THEN 1 ELSE 0
             END
           ) > 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'legacy eDHR topology snapshot is partially populated';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_mes_edhr_legacy_topology_batch;
  CREATE TEMPORARY TABLE tmp_mes_edhr_legacy_topology_batch AS
  SELECT task.tenant_id,
         task.batch_execution_id
  FROM mes_pro_edhr_batch_execution_task task
  WHERE task.deleted = b'0'
    AND (
      task.node_type = 'ROUTE_FORM'
      OR (
        task.node_type IS NULL
        AND task.route_process_id IS NOT NULL
        AND task.batch_record_report_id IS NOT NULL
      )
    )
  GROUP BY task.tenant_id, task.batch_execution_id
  HAVING SUM(CASE WHEN task.predecessor_route_process_id IS NOT NULL THEN 1 ELSE 0 END) = 0
     AND SUM(CASE WHEN COALESCE(task.root_process_flag, b'0') = b'1' THEN 1 ELSE 0 END) = 0;

  IF EXISTS (
    SELECT 1
    FROM mes_pro_edhr_batch_execution_task task
    INNER JOIN tmp_mes_edhr_legacy_topology_batch legacy
      ON legacy.tenant_id = task.tenant_id
     AND legacy.batch_execution_id = task.batch_execution_id
    WHERE task.deleted = b'0'
      AND (
        task.node_type = 'ROUTE_FORM'
        OR (
          task.node_type IS NULL
          AND task.route_process_id IS NOT NULL
          AND task.batch_record_report_id IS NOT NULL
        )
      )
      AND (
        task.route_process_id IS NULL
        OR task.route_process_sort IS NULL
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'legacy eDHR topology snapshot contains null route process or sort';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM mes_pro_edhr_batch_execution_task task
    INNER JOIN tmp_mes_edhr_legacy_topology_batch legacy
      ON legacy.tenant_id = task.tenant_id
     AND legacy.batch_execution_id = task.batch_execution_id
    WHERE task.deleted = b'0'
      AND (
        task.node_type = 'ROUTE_FORM'
        OR (
          task.node_type IS NULL
          AND task.route_process_id IS NOT NULL
          AND task.batch_record_report_id IS NOT NULL
        )
      )
    GROUP BY task.tenant_id, task.batch_execution_id, task.route_process_id
    HAVING COUNT(DISTINCT task.route_process_sort) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'legacy eDHR topology snapshot has inconsistent process sort';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT task.tenant_id,
             task.batch_execution_id,
             task.route_process_sort
      FROM mes_pro_edhr_batch_execution_task task
      INNER JOIN tmp_mes_edhr_legacy_topology_batch legacy
        ON legacy.tenant_id = task.tenant_id
       AND legacy.batch_execution_id = task.batch_execution_id
      WHERE task.deleted = b'0'
        AND (
          task.node_type = 'ROUTE_FORM'
          OR (
            task.node_type IS NULL
            AND task.route_process_id IS NOT NULL
            AND task.batch_record_report_id IS NOT NULL
          )
        )
      GROUP BY task.tenant_id, task.batch_execution_id, task.route_process_sort
      HAVING COUNT(DISTINCT task.route_process_id) > 1
    ) ambiguous_process_order
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'legacy eDHR topology snapshot has ambiguous process ordering';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_mes_edhr_route_process_snapshot;
  CREATE TEMPORARY TABLE tmp_mes_edhr_route_process_snapshot AS
  SELECT task.tenant_id,
         task.batch_execution_id,
         task.route_process_id,
         MIN(task.route_process_sort) AS route_process_sort
  FROM mes_pro_edhr_batch_execution_task task
  INNER JOIN tmp_mes_edhr_legacy_topology_batch legacy
    ON legacy.tenant_id = task.tenant_id
   AND legacy.batch_execution_id = task.batch_execution_id
  WHERE task.deleted = b'0'
    AND (
      task.node_type = 'ROUTE_FORM'
      OR (
        task.node_type IS NULL
        AND task.route_process_id IS NOT NULL
        AND task.batch_record_report_id IS NOT NULL
      )
    )
  GROUP BY task.tenant_id, task.batch_execution_id, task.route_process_id;

  DROP TEMPORARY TABLE IF EXISTS tmp_mes_edhr_topology_backfill;
  CREATE TEMPORARY TABLE tmp_mes_edhr_topology_backfill AS
  SELECT process_snapshot.tenant_id,
         process_snapshot.batch_execution_id,
         process_snapshot.route_process_id,
         LAG(process_snapshot.route_process_id) OVER (
           PARTITION BY process_snapshot.tenant_id, process_snapshot.batch_execution_id
           ORDER BY process_snapshot.route_process_sort, process_snapshot.route_process_id
         ) AS predecessor_route_process_id
  FROM tmp_mes_edhr_route_process_snapshot process_snapshot;

  START TRANSACTION;

  UPDATE mes_pro_edhr_batch_execution_task task
  INNER JOIN tmp_mes_edhr_topology_backfill backfill
    ON backfill.tenant_id = task.tenant_id
   AND backfill.batch_execution_id = task.batch_execution_id
   AND backfill.route_process_id = task.route_process_id
  SET task.predecessor_route_process_id = backfill.predecessor_route_process_id,
      task.root_process_flag =
        IF(backfill.predecessor_route_process_id IS NULL, b'1', b'0')
  WHERE task.deleted = b'0'
    AND (
      task.node_type = 'ROUTE_FORM'
      OR (
        task.node_type IS NULL
        AND task.route_process_id IS NOT NULL
        AND task.batch_record_report_id IS NOT NULL
      )
    );

  IF EXISTS (
    SELECT 1
    FROM mes_pro_edhr_batch_execution_task task
    INNER JOIN tmp_mes_edhr_topology_backfill backfill
      ON backfill.tenant_id = task.tenant_id
     AND backfill.batch_execution_id = task.batch_execution_id
     AND backfill.route_process_id = task.route_process_id
    WHERE task.deleted = b'0'
      AND (
        NOT (task.predecessor_route_process_id <=> backfill.predecessor_route_process_id)
        OR task.root_process_flag <>
           IF(backfill.predecessor_route_process_id IS NULL, b'1', b'0')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR topology snapshot backfill verification failed';
  END IF;

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS tmp_mes_edhr_topology_backfill;
  DROP TEMPORARY TABLE IF EXISTS tmp_mes_edhr_route_process_snapshot;
  DROP TEMPORARY TABLE IF EXISTS tmp_mes_edhr_legacy_topology_batch;
END//

DELIMITER ;

CALL intruoyi_backfill_mes_edhr_batch_task_topology_snapshot();

DROP PROCEDURE IF EXISTS intruoyi_backfill_mes_edhr_batch_task_topology_snapshot;
