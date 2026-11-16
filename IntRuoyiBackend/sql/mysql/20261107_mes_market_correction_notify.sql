-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261028_mes_pqc_correction_notify,20261107_mes_market_correction_transaction_round; type=data; riskLevel=low
-- Adds two shared templates only. Existing templates, notifications and business history are unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS seed_mes_market_correction_notify;
DELIMITER $$
CREATE PROCEDURE seed_mes_market_correction_notify()
BEGIN
  INSERT INTO system_notify_template
    (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.code,2,'eDHR上市放行补正',seed.content,
    '["workOrderCode","batchCode","workTaskId","reason","actionUrl"]',0,
    '仅原申请人补正；区分上市撤回与不予后QA让步，不改变PQC原决定',
    'market-correction',NOW(),'market-correction',NOW(),b'0'
  FROM (
    SELECT '上市撤回补正通知' AS name,
      'MES_EDHR_MARKET_WITHDRAW_CORRECTION_ASSIGNED' AS code,
      '工单{workOrderCode}批次{batchCode}的上市放行申请已撤回。原因：{reason}。请原申请人完成补正后重新提交。补正任务：{workTaskId}。补正入口：{actionUrl}' AS content
    UNION ALL
    SELECT '上市不予后QA让步补正通知',
      'MES_EDHR_MARKET_REJECT_CORRECTION_ASSIGNED',
      '工单{workOrderCode}批次{batchCode}原不予上市放行，现已取得本轮QA让步决定。原不予原因：{reason}。请原申请人完成补正后重新提交，QA让步不代替上市批准。补正任务：{workTaskId}。补正入口：{actionUrl}'
  ) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_notify_template existing WHERE existing.code=seed.code);

  IF (SELECT COUNT(*) FROM system_notify_template WHERE code IN (
      'MES_EDHR_MARKET_WITHDRAW_CORRECTION_ASSIGNED','MES_EDHR_MARKET_REJECT_CORRECTION_ASSIGNED')) <> 2
    OR (SELECT COUNT(*) FROM system_notify_template WHERE deleted=b'0' AND status=0
      AND JSON_VALID(params) AND JSON_LENGTH(params)=5
      AND JSON_CONTAINS(params,JSON_ARRAY('workOrderCode','batchCode','workTaskId','reason','actionUrl'))
      AND ((code='MES_EDHR_MARKET_WITHDRAW_CORRECTION_ASSIGNED'
        AND content='工单{workOrderCode}批次{batchCode}的上市放行申请已撤回。原因：{reason}。请原申请人完成补正后重新提交。补正任务：{workTaskId}。补正入口：{actionUrl}')
       OR (code='MES_EDHR_MARKET_REJECT_CORRECTION_ASSIGNED'
        AND content='工单{workOrderCode}批次{batchCode}原不予上市放行，现已取得本轮QA让步决定。原不予原因：{reason}。请原申请人完成补正后重新提交，QA让步不代替上市批准。补正任务：{workTaskId}。补正入口：{actionUrl}'))) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Market correction notification templates missing, duplicated, disabled or mismatched';
  END IF;
END$$
DELIMITER ;
CALL seed_mes_market_correction_notify();
DROP PROCEDURE seed_mes_market_correction_notify;
