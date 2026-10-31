-- release-target-preflight: migrationId=20260710_mes_route_schedule_config_unification; allowedEnvironments=test,backup,prod
-- Active WIP must resolve to one generic configuration before irreversible product-config retirement.
WITH valid_config AS (
  SELECT sc.* FROM mes_pro_route_schedule_config sc
  JOIN mes_pro_route_version rv ON rv.id=sc.route_version_id AND rv.tenant_id=sc.tenant_id AND rv.deleted=b'0'
  JOIN mes_pro_route_process rp ON rp.id=sc.route_process_id AND rp.tenant_id=sc.tenant_id
    AND rp.deleted=b'0' AND rp.route_id=rv.route_id
  WHERE sc.deleted=b'0'
), active_wip AS (
  SELECT ps.tenant_id,ps.route_process_id,so.route_id,COALESCE(ps.route_version_id,so.route_version_id) AS version_id
  FROM mes_pro_schedule_order_process ps
  JOIN mes_pro_schedule_order so ON so.id=ps.schedule_order_id AND so.tenant_id=ps.tenant_id
  WHERE ps.deleted=b'0' AND ps.enabled=b'1' AND (ps.progress_percent IS NULL OR ps.progress_percent<100)
    AND so.deleted=b'0' AND so.status IN (0,1,2) AND so.frozen=b'0' AND so.manual_finished=b'0'
)
SELECT CASE WHEN
  NOT EXISTS (
    SELECT tenant_id,route_version_id,route_process_id FROM valid_config
    GROUP BY tenant_id,route_version_id,route_process_id
    HAVING SUM(CASE WHEN item_id IS NULL THEN 1 ELSE 0 END)<>1
  )
  AND NOT EXISTS (SELECT 1 FROM active_wip WHERE route_id IS NULL OR route_process_id IS NULL)
  AND NOT EXISTS (
    SELECT 1 FROM active_wip w WHERE
      (SELECT COUNT(*) FROM valid_config c WHERE c.tenant_id=w.tenant_id AND c.route_process_id=w.route_process_id
        AND c.item_id IS NULL AND (w.version_id IS NULL OR c.route_version_id=w.version_id))<>1
  )
  THEN 'TARGET_PREFLIGHT_PASS:20260710_mes_route_schedule_config_unification'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:generic-config-or-active-wip-ambiguous' END;
