-- release-target-preflight: migrationId=20260710_mes_schedule_order_topology_snapshot_backfill; allowedEnvironments=test,backup,prod
-- After topology-column dependency: reject ambiguous legacy identities/sort and partially populated snapshots before any backfill.
SELECT CASE WHEN

  NOT EXISTS (
    SELECT 1 FROM mes_pro_schedule_order_process WHERE deleted=b'0'
    GROUP BY tenant_id,schedule_order_id
    HAVING
      (SUM(predecessor_route_process_id IS NOT NULL OR COALESCE(root_process_flag,0)=1)>0
       AND SUM(predecessor_route_process_id IS NULL AND COALESCE(root_process_flag,0)=0)>0)
      OR (SUM(predecessor_route_process_id IS NOT NULL)=0 AND SUM(COALESCE(root_process_flag,0)=1)=0
          AND (COUNT(route_process_id)<>COUNT(*) OR COUNT(DISTINCT route_process_id)<>COUNT(*) OR COUNT(sort)<>COUNT(*)))
  )
THEN 'TARGET_PREFLIGHT_PASS:20260710_mes_schedule_order_topology_snapshot_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260710_mes_schedule_order_topology_snapshot_backfill' END;
