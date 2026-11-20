-- release-target-preflight: migrationId=20261120_mes_schedule_order_menu_forward; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  EXISTS (SELECT 1 FROM system_menu WHERE id=900120 AND name='智能排产'
    AND type=1 AND parent_id=0 AND path='/mes/smart-scheduling'
    AND permission='mes:pro-smart-scheduling:query' AND status=0 AND deleted=b'0')
  AND EXISTS (SELECT 1 FROM system_menu WHERE id=5580 AND type=2
    AND component='mes/pro/scheduleorder/index' AND component_name='MesProScheduleOrder'
    AND status=0 AND visible=b'1' AND deleted=b'0'
    AND ((name='排产工单池' AND parent_id=5700 AND path='schedule-order' AND permission='' AND sort=2)
      OR (name='排产工单' AND parent_id=900120 AND path='/mes/pro/schedule-order'
        AND permission='mes:pro-schedule-order:query' AND sort=1)))
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>5580
    AND (path='/mes/pro/schedule-order' OR (type=2 AND component='mes/pro/scheduleorder/index')))
THEN 'TARGET_PREFLIGHT_PASS:20261120_mes_schedule_order_menu_forward'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20261120_mes_schedule_order_menu_forward:BASELINE_CONFLICT' END;
