-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_d_notification_template; type=data; riskLevel=medium
-- Only the new DCC related file revision template is created. No existing template is overwritten.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_related_revision_template;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_related_revision_template()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_count FROM system_notify_template
  WHERE BINARY code = BINARY 'dcc_related_file_revision' AND deleted = b'0';
  IF v_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate DCC related file revision template';
  END IF;
  IF v_count = 0 THEN
    INSERT INTO system_notify_template
      (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
    VALUES ('DCC关联文件升版通知','dcc_related_file_revision',2,'DCC系统',
      '您编制的文件 {relatedFileNumber} 所关联的文件 {fileNumber} 已完成升版至 {versionNo}，请核对关联影响。',
      '["fileNumber","versionNo","relatedFileNumber","sourceControlledFileId","revisedControlledFileId","detailUrl"]',0,
      '仅关联文件升版受控成功后向反向关联文件的正式签名编制人发送','1',NOW(),'1',NOW(),b'0');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_notify_template
      WHERE BINARY code = BINARY 'dcc_related_file_revision' AND deleted = b'0' AND status=0 AND type=2
        AND BINARY nickname = BINARY 'DCC系统'
        AND BINARY content = BINARY '您编制的文件 {relatedFileNumber} 所关联的文件 {fileNumber} 已完成升版至 {versionNo}，请核对关联影响。'
        AND BINARY params = BINARY '["fileNumber","versionNo","relatedFileNumber","sourceControlledFileId","revisedControlledFileId","detailUrl"]') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC related file revision template differs from registered contract';
  END IF;
END $$
DELIMITER ;
CALL ensure_dcc_related_revision_template();
DROP PROCEDURE IF EXISTS ensure_dcc_related_revision_template;
