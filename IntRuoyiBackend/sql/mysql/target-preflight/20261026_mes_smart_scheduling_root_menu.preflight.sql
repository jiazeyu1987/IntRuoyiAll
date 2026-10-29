-- release-target-preflight: migrationId=20261026_mes_smart_scheduling_root_menu; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (NOT (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900120 AND name='智能排产'
      AND type=1 AND permission='mes:pro-smart-scheduling:query'
      AND status=0 AND deleted=b'0'
      AND ((parent_id=5100 AND path='smart-scheduling')
        OR (parent_id=0 AND path IN ('smart-scheduling','/mes/smart-scheduling'))))))
  AND (NOT (EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>900120
      AND path='/mes/smart-scheduling')))
  AND (NOT (EXISTS (SELECT 1 FROM system_menu WHERE parent_id=900120 AND deleted=b'0'
      AND type<>3 AND path NOT LIKE '/%')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_mes_smart_scheduling_root_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_mes_smart_scheduling_root_menu:BASELINE_CONFLICT' END;
