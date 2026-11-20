-- release-target-preflight: migrationId=20261117_mes_edhr_deviation_workbench_category; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
 (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_profile_workbench_task_visibility','system_workbench_visibility_context','mes_pro_edhr_work_task') AND ENGINE='InnoDB')=3
 AND NOT EXISTS (SELECT 1 FROM system_profile_workbench_task_visibility v JOIN mes_pro_edhr_work_task w ON w.tenant_id=v.tenant_id AND CAST(v.task_key AS BINARY)=CAST(CONCAT('EDHR_WORK_TASK:',w.id) AS BINARY) LEFT JOIN system_workbench_visibility_context c ON c.tenant_id=v.tenant_id AND c.user_id=v.user_id WHERE w.task_type IN ('DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY','DEVIATION_SIGN','DEVIATION_APPROVE','DEVIATION_CLOSE') AND c.user_id IS NULL)
 THEN 'TARGET_PREFLIGHT_PASS:20261117_mes_edhr_deviation_workbench_category'
 ELSE 'TARGET_PREFLIGHT_BLOCKED:20261117_mes_edhr_deviation_workbench_category:VISIBILITY_CONTEXT_MISSING' END;
