-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260815_system_notify_message_business_key,20261028_mes_pqc_rejection_correction_round; type=data; riskLevel=low
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS seed_mes_pqc_correction_notify;
DELIMITER $$
CREATE PROCEDURE seed_mes_pqc_correction_notify()
BEGIN
  INSERT INTO system_notify_template
    (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
  SELECT 'PQC生产放行驳回补正通知','MES_EDHR_PQC_CORRECTION_ASSIGNED',2,'eDHR放行补正',
    '工单{workOrderCode}批次{batchCode}的PQC生产放行已驳回。原因：{reason}。请原申请人完成补正后重新提交。补正入口：{actionUrl}',
    '["workOrderCode","batchCode","workTaskId","reason","actionUrl"]',0,
    '原申请人显式补正责任；新驳回和历史接收共用唯一任务通知','pqc-correction',NOW(),'pqc-correction',NOW(),b'0'
  WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_PQC_CORRECTION_ASSIGNED');
  IF (SELECT COUNT(*) FROM system_notify_template WHERE code='MES_EDHR_PQC_CORRECTION_ASSIGNED') <> 1
      OR (SELECT COUNT(*) FROM system_notify_template WHERE code='MES_EDHR_PQC_CORRECTION_ASSIGNED'
          AND deleted=b'0' AND status=0 AND JSON_VALID(params) AND JSON_LENGTH(params)=5
          AND JSON_CONTAINS(params,JSON_ARRAY('workOrderCode','batchCode','workTaskId','reason','actionUrl'))) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC correction notification template missing, disabled, duplicated or invalid';
  END IF;
END$$
DELIMITER ;
CALL seed_mes_pqc_correction_notify();
DROP PROCEDURE seed_mes_pqc_correction_notify;
