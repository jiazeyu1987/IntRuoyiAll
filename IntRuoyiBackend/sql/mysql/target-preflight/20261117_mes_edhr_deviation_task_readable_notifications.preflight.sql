-- release-target-preflight: migrationId=20261117_mes_edhr_deviation_task_readable_notifications; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
 (SELECT COUNT(*) FROM system_notify_template WHERE code IN ('MES_EDHR_DEVIATION_RECEIVE_ASSIGNED','MES_EDHR_DEVIATION_CORRECT_ASSIGNED','MES_EDHR_DEVIATION_REVIEW_ASSIGNED') AND deleted=b'0')=3
 AND NOT EXISTS (SELECT code FROM system_notify_template WHERE code='MES_EDHR_DEVIATION_WORK_TASK_OVERDUE' AND deleted=b'0' GROUP BY code HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_PASS:20261117_mes_edhr_deviation_task_readable_notifications'
 ELSE 'TARGET_PREFLIGHT_BLOCKED:20261117_mes_edhr_deviation_task_readable_notifications:TEMPLATE_BASELINE_CONFLICT' END;
