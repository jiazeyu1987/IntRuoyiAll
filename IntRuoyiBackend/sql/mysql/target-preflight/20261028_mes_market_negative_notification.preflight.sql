-- release-target-preflight: migrationId=20261028_mes_market_negative_notification; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  ((SELECT COUNT(*) FROM system_notify_template WHERE code='MES_EDHR_RELEASE_MARKET_REJECTED')<=1)
  AND (NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_RELEASE_MARKET_REJECTED' AND (deleted<>b'0' OR status<>0 OR CASE WHEN params IS NULL OR NOT JSON_VALID(params) THEN 1 ELSE JSON_TYPE(CAST(params AS JSON))<>'ARRAY' OR JSON_LENGTH(params)<>5 OR NOT JSON_CONTAINS(params,JSON_ARRAY('workOrderCode','batchCode','workTaskId','reason','actionUrl')) END)))
  AND ((SELECT COUNT(*) FROM system_notify_template WHERE code='MES_EDHR_RELEASE_MARKET_WITHDRAWN')<=1)
  AND (NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_RELEASE_MARKET_WITHDRAWN' AND (deleted<>b'0' OR status<>0 OR CASE WHEN params IS NULL OR NOT JSON_VALID(params) THEN 1 ELSE JSON_TYPE(CAST(params AS JSON))<>'ARRAY' OR JSON_LENGTH(params)<>5 OR NOT JSON_CONTAINS(params,JSON_ARRAY('workOrderCode','batchCode','workTaskId','reason','actionUrl')) END)))
THEN 'TARGET_PREFLIGHT_PASS:20261028_mes_market_negative_notification' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261028_mes_market_negative_notification:BASELINE_CONFLICT' END;
