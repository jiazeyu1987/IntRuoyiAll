-- release-target-preflight: migrationId=20261107_mes_market_correction_notify; allowedEnvironments=test,backup,prod
-- Reject existing conflicting data; absent seed rows are created by the migration.
SELECT CASE
 WHEN (EXISTS (SELECT code FROM system_notify_template WHERE code IN
 ('MES_EDHR_MARKET_WITHDRAW_CORRECTION_ASSIGNED','MES_EDHR_MARKET_REJECT_CORRECTION_ASSIGNED') GROUP BY code HAVING COUNT(*)>1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261107_mes_market_correction_notify:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_notify_template WHERE code IN
 ('MES_EDHR_MARKET_WITHDRAW_CORRECTION_ASSIGNED','MES_EDHR_MARKET_REJECT_CORRECTION_ASSIGNED')
 AND (deleted<>0 OR status<>0 OR params IS NULL OR CASE WHEN NOT JSON_VALID(params) THEN 1
 ELSE JSON_LENGTH(params)<>5 OR NOT JSON_CONTAINS(params,JSON_ARRAY('workOrderCode','batchCode','workTaskId','reason','actionUrl')) END
 OR (code='MES_EDHR_MARKET_WITHDRAW_CORRECTION_ASSIGNED' AND NOT (content <=> '工单{workOrderCode}批次{batchCode}的上市放行申请已撤回。原因：{reason}。请原申请人完成补正后重新提交。补正任务：{workTaskId}。补正入口：{actionUrl}'))
 OR (code='MES_EDHR_MARKET_REJECT_CORRECTION_ASSIGNED' AND NOT (content <=> '工单{workOrderCode}批次{batchCode}原不予上市放行，现已取得本轮QA让步决定。原不予原因：{reason}。请原申请人完成补正后重新提交，QA让步不代替上市批准。补正任务：{workTaskId}。补正入口：{actionUrl}'))))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261107_mes_market_correction_notify:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261107_mes_market_correction_notify' END;
