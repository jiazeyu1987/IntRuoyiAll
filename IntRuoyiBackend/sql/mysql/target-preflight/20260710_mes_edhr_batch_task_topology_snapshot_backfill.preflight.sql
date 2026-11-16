-- release-target-preflight: migrationId=20260710_mes_edhr_batch_task_topology_snapshot_backfill; allowedEnvironments=test,backup,prod
-- Mirror all real topology migration aborts; multiple form tasks for one route process are valid when their sorts agree.
WITH route_tasks AS (
  SELECT tenant_id,batch_execution_id,route_process_id,route_process_sort,predecessor_route_process_id,root_process_flag
  FROM mes_pro_edhr_batch_execution_task WHERE deleted=b'0'
    AND (node_type='ROUTE_FORM' OR (node_type IS NULL AND route_process_id IS NOT NULL AND batch_record_report_id IS NOT NULL))
), batches AS (
  SELECT tenant_id,batch_execution_id,
         SUM(predecessor_route_process_id IS NOT NULL OR COALESCE(root_process_flag,0)=1) AS populated,
         SUM(predecessor_route_process_id IS NULL AND COALESCE(root_process_flag,0)=0) AS missing
  FROM route_tasks GROUP BY tenant_id,batch_execution_id
), legacy AS (
  SELECT t.* FROM route_tasks t JOIN batches b ON b.tenant_id=t.tenant_id AND b.batch_execution_id=t.batch_execution_id
  WHERE b.populated=0
)
SELECT CASE WHEN

  NOT EXISTS (SELECT 1 FROM batches WHERE populated>0 AND missing>0)
  AND NOT EXISTS (SELECT 1 FROM legacy WHERE route_process_id IS NULL OR route_process_sort IS NULL)
  AND NOT EXISTS (SELECT 1 FROM legacy GROUP BY tenant_id,batch_execution_id,route_process_id HAVING COUNT(DISTINCT route_process_sort)<>1)
  AND NOT EXISTS (SELECT 1 FROM legacy GROUP BY tenant_id,batch_execution_id,route_process_sort HAVING COUNT(DISTINCT route_process_id)>1)
THEN 'TARGET_PREFLIGHT_PASS:20260710_mes_edhr_batch_task_topology_snapshot_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260710_mes_edhr_batch_task_topology_snapshot_backfill' END;
