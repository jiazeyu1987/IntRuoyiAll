-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261027_mes_release_outcome_notification; type=data; riskLevel=low
-- Result notices use the original applicant and, on withdrawal, the frozen original approvers. No backfill or task creation.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS seed_mes_market_negative_notification;
DELIMITER $$
CREATE PROCEDURE seed_mes_market_negative_notification()
BEGIN
  DECLARE v_invalid INT DEFAULT 0;
  INSERT INTO system_notify_template
    (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
  SELECT seeds.name,seeds.code,2,'eDHR放行结果',seeds.content,
    '["workOrderCode","batchCode","workTaskId","reason","actionUrl"]',0,
    '结果告知正式收件人，不生成待办','release-outcome',NOW(),'release-outcome',NOW(),b'0'
  FROM (
    SELECT '上市放行不予批准结果' AS name,'MES_EDHR_RELEASE_MARKET_REJECTED' AS code,
      '工单{workOrderCode}批次{batchCode}不予上市放行，已转交指定QA评审。原因：{reason}。原上市任务{workTaskId}已结束。消息中心：{actionUrl}' AS content
    UNION ALL SELECT '上市放行申请撤回结果','MES_EDHR_RELEASE_MARKET_WITHDRAWN',
      '工单{workOrderCode}批次{batchCode}的上市放行申请已由原申请人撤回，原上市任务{workTaskId}已取消。原因：{reason}。申请人补正后重新提交正式审查。消息中心：{actionUrl}'
  ) seeds
  WHERE NOT EXISTS (SELECT 1 FROM system_notify_template t WHERE t.code=seeds.code);
  SELECT COUNT(*) INTO v_invalid FROM (
    SELECT expected.code FROM (
      SELECT 'MES_EDHR_RELEASE_MARKET_REJECTED' AS code
      UNION ALL SELECT 'MES_EDHR_RELEASE_MARKET_WITHDRAWN'
    ) expected LEFT JOIN system_notify_template t ON t.code=expected.code
    GROUP BY expected.code
    HAVING COUNT(t.id)<>1 OR SUM(t.deleted=b'0' AND t.status=0 AND JSON_VALID(t.params)
      AND JSON_LENGTH(t.params)=5
      AND JSON_CONTAINS(t.params,JSON_ARRAY('workOrderCode','batchCode','workTaskId','reason','actionUrl')))<>1
  ) invalid_templates;
  IF v_invalid<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Release outcome templates missing, ambiguous, disabled or invalid; existing configuration was not overwritten';
  END IF;
END$$
DELIMITER ;
CALL seed_mes_market_negative_notification();
DROP PROCEDURE seed_mes_market_negative_notification;
