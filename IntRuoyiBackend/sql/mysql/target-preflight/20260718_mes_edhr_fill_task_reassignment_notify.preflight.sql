-- release-target-preflight: migrationId=20260718_mes_edhr_fill_task_reassignment_notify; allowedEnvironments=test,backup,prod
-- Reject ambiguous active template selection; customized existing text is preserved by this seed.
SELECT CASE WHEN
  (SELECT COUNT(*) FROM system_notify_template WHERE code='MES_EDHR_FILL_TASK_REASSIGNED' AND deleted=b'0')<=1

  AND NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_FILL_TASK_REASSIGNED' AND deleted=b'0'
    AND CASE WHEN JSON_VALID(params) THEN JSON_TYPE(CAST(params AS JSON))<>'ARRAY' ELSE 1 END)
THEN 'TARGET_PREFLIGHT_PASS:20260718_mes_edhr_fill_task_reassignment_notify' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260718_mes_edhr_fill_task_reassignment_notify' END;
