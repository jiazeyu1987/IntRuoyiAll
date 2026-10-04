-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260815_system_notify_message_business_key; type=seed; riskLevel=low
-- One new template only. No grants, history rewrite, approval registry or existing template UPDATE.
-- Existing routine name is never pre-dropped. Stop on any error; do not use mysql --force.
SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE g49_dcc_project_notify()
BEGIN
  DECLARE existing_count INT;
  DECLARE matching_count INT;
  SELECT COUNT(*) INTO existing_count FROM system_notify_template
    WHERE code='dcc-project-product-application-event';
  SELECT COUNT(*) INTO matching_count FROM system_notify_template
    WHERE BINARY code=BINARY 'dcc-project-product-application-event'
      AND BINARY name=BINARY 'DCC项目及产品申请通知' AND type=2
      AND BINARY nickname=BINARY 'DCC系统'
      AND BINARY content=BINARY '项目及产品申请《{businessTitle}》：{eventName}。说明：{reason}。请打开原申请查看。'
      AND BINARY params=BINARY '["businessTitle","eventName","reason","notifyTargetType","notifyTargetId","actionUrl"]'
      AND status=0 AND deleted=b'0'
      AND BINARY remark=BINARY 'DCC native project-product request notification';
  IF existing_count=0 THEN
    INSERT INTO system_notify_template
      (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
    VALUES ('DCC项目及产品申请通知','dcc-project-product-application-event',2,'DCC系统',
      '项目及产品申请《{businessTitle}》：{eventName}。说明：{reason}。请打开原申请查看。',
      '["businessTitle","eventName","reason","notifyTargetType","notifyTargetId","actionUrl"]',0,
      'DCC native project-product request notification','dcc-seed',CURRENT_TIMESTAMP,'dcc-seed',CURRENT_TIMESTAMP,b'0');
  ELSEIF existing_count<>1 OR matching_count<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G49 notification template payload conflict';
  END IF;
END$$
DELIMITER ;
START TRANSACTION;
CALL g49_dcc_project_notify();
COMMIT;
DROP PROCEDURE g49_dcc_project_notify;
