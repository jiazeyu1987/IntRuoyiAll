-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260907_dcc_publication_notification; type=data; riskLevel=medium
-- Only the new DCC relation remediation template is created. No existing template is overwritten.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_d_remediation_template;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_d_remediation_template()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_count FROM system_notify_template
  WHERE BINARY code = BINARY 'dcc_relation_remediation' AND deleted = b'0';
  IF v_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate DCC relation remediation template';
  END IF;
  IF v_count = 0 THEN
    INSERT INTO system_notify_template
      (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
    VALUES ('DCC关联整改通知','dcc_relation_remediation',2,'DCC系统',
      '受控文件 {fileNumber} {versionNo} 已完成受控。您被指派处理关联文件整改，期限为 {dueAt}，请查看本次受控申请。',
      '["fileNumber","versionNo","relatedMasterId","dueAt","sourceControlledFileId","detailUrl"]',0,
      '仅受控成功后向会签明确所选整改人发送','1',NOW(),'1',NOW(),b'0');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_notify_template
      WHERE BINARY code = BINARY 'dcc_relation_remediation' AND deleted = b'0' AND status=0 AND type=2
        AND BINARY nickname = BINARY 'DCC系统'
        AND BINARY content = BINARY '受控文件 {fileNumber} {versionNo} 已完成受控。您被指派处理关联文件整改，期限为 {dueAt}，请查看本次受控申请。'
        AND BINARY params = BINARY '["fileNumber","versionNo","relatedMasterId","dueAt","sourceControlledFileId","detailUrl"]') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC relation remediation template differs from registered contract';
  END IF;
END $$
DELIMITER ;
CALL ensure_dcc_d_remediation_template();
DROP PROCEDURE IF EXISTS ensure_dcc_d_remediation_template;
