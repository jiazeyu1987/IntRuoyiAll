SELECT COUNT(*) AS task_count,
       SUM(task.task_status = 'SUBMITTED') AS submitted_task_count,
       SUM(task.actual_inspection_quantity = task.planned_inspection_quantity) AS quantity_match_count,
       COUNT(DISTINCT event.id) AS event_count,
       COUNT(DISTINCT record.id) AS pqc_record_count,
       COUNT(DISTINCT detail.task_id) AS task_with_piece_details_count,
       SUM(event.event_idempotency_key LIKE 'CODX-PQC-20260807%') AS marker_event_count
  FROM mes_pqc_inspection_task task
  LEFT JOIN mes_pro_process_pool_event event
    ON event.tenant_id = task.tenant_id
   AND event.deleted = b'0'
   AND event.pqc_task_id = task.id
   AND event.event_type = 'PQC_INSPECTION'
  LEFT JOIN mes_pro_process_pool_pqc_record record
    ON record.tenant_id = event.tenant_id
   AND record.deleted = b'0'
   AND record.event_id = event.id
  LEFT JOIN mes_pqc_inspection_piece_detail detail
    ON detail.tenant_id = task.tenant_id
   AND detail.deleted = b'0'
   AND detail.task_id = task.id
 WHERE task.tenant_id = 1
   AND task.deleted = b'0'
   AND task.creator = 'CODX-PQC-20260807';

SELECT task.id AS task_id, task.round_no, task.task_status, task.actual_inspection_quantity,
       event.id AS event_id, event.event_idempotency_key, event.actual_employee_id,
       event.production_submit_event_id, event.pqc_task_id, event.server_submit_time,
       record.id AS pqc_record_id, record.inspection_result,
       COUNT(detail.id) AS piece_detail_count
  FROM mes_pqc_inspection_task task
  JOIN mes_pro_process_pool_event event
    ON event.tenant_id = task.tenant_id
   AND event.deleted = b'0'
   AND event.pqc_task_id = task.id
   AND event.event_type = 'PQC_INSPECTION'
  JOIN mes_pro_process_pool_pqc_record record
    ON record.tenant_id = event.tenant_id
   AND record.deleted = b'0'
   AND record.event_id = event.id
  JOIN mes_pqc_inspection_piece_detail detail
    ON detail.tenant_id = task.tenant_id
   AND detail.deleted = b'0'
   AND detail.task_id = task.id
 WHERE task.tenant_id = 1
   AND task.deleted = b'0'
   AND task.creator = 'CODX-PQC-20260807'
 GROUP BY task.id, task.round_no, task.task_status, task.actual_inspection_quantity,
          event.id, event.event_idempotency_key, event.actual_employee_id,
          event.production_submit_event_id, event.pqc_task_id, event.server_submit_time,
          record.id, record.inspection_result
 ORDER BY task.round_no;
