-- release-target-preflight: migrationId=20261028_mes_pqc_correction_notify; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  ((SELECT COUNT(*) FROM system_notify_template WHERE code='MES_EDHR_PQC_CORRECTION_ASSIGNED')<=1)
  AND (NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_PQC_CORRECTION_ASSIGNED' AND (deleted<>b'0' OR status<>0 OR CASE WHEN params IS NULL OR NOT JSON_VALID(params) THEN 1 ELSE JSON_TYPE(CAST(params AS JSON))<>'ARRAY' OR JSON_LENGTH(params)<>5 OR NOT JSON_CONTAINS(params,JSON_ARRAY('workOrderCode','batchCode','workTaskId','reason','actionUrl')) END)))
THEN 'TARGET_PREFLIGHT_PASS:20261028_mes_pqc_correction_notify' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261028_mes_pqc_correction_notify:BASELINE_CONFLICT' END;
