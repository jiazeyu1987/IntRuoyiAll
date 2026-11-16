-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260815_system_notify_message_business_key; type=schema; riskLevel=high

-- Source tables are retained. Run with the paired application release and no pending mail/SMS jobs.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `system_tenant_mail_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `port` int NOT NULL,
  `ssl_enable` bit(1) NOT NULL DEFAULT b'0',
  `starttls_enable` bit(1) NOT NULL DEFAULT b'0',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  `source_id` bigint NULL,
  UNIQUE KEY `uk_tenant_source` (`tenant_id`, `source_id`),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_sms_channel`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `signature` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `api_secret` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `callback_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  `source_id` bigint NULL,
  UNIQUE KEY `uk_tenant_source` (`tenant_id`, `source_id`),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_mail_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(10240) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  `source_id` bigint NULL,
  UNIQUE KEY `uk_tenant_source` (`tenant_id`, `source_id`),
  `active_code` varchar(64) GENERATED ALWAYS AS (CASE WHEN deleted=b'0' THEN code ELSE NULL END) STORED,
  UNIQUE KEY `uk_tenant_active_code` (`tenant_id`, `active_code`),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_sms_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` tinyint NOT NULL,
  `status` tinyint NOT NULL,
  `code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_template_id` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel_id` bigint NOT NULL,
  `channel_code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  `source_id` bigint NULL,
  UNIQUE KEY `uk_tenant_source` (`tenant_id`, `source_id`),
  `active_code` varchar(64) GENERATED ALWAYS AS (CASE WHEN deleted=b'0' THEN code ELSE NULL END) STORED,
  UNIQUE KEY `uk_tenant_active_code` (`tenant_id`, `active_code`),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_notify_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` tinyint NOT NULL,
  `params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  `source_id` bigint NULL,
  UNIQUE KEY `uk_tenant_source` (`tenant_id`, `source_id`),
  `active_code` varchar(64) GENERATED ALWAYS AS (CASE WHEN deleted=b'0' THEN code ELSE NULL END) STORED,
  UNIQUE KEY `uk_tenant_active_code` (`tenant_id`, `active_code`),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_mail_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `user_type` tinyint NULL DEFAULT NULL,
  `to_mails` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cc_mails` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `bcc_mails` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `from_mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_id` bigint NOT NULL,
  `template_code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `template_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `send_status` tinyint NOT NULL DEFAULT 0,
  `send_time` datetime NULL DEFAULT NULL,
  `send_message_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `send_exception` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_sms_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `channel_code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_id` bigint NOT NULL,
  `template_code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_type` tinyint NOT NULL,
  `template_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `api_template_id` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  `user_type` tinyint NULL DEFAULT NULL,
  `send_status` tinyint NOT NULL DEFAULT 0,
  `send_time` datetime NULL DEFAULT NULL,
  `api_send_code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_send_msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_request_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_serial_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `receive_status` tinyint NOT NULL DEFAULT 0,
  `receive_time` datetime NULL DEFAULT NULL,
  `api_receive_code` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_receive_msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_sms_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_ip` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `scene` tinyint NOT NULL,
  `today_index` tinyint NOT NULL,
  `used` tinyint NOT NULL,
  `used_time` datetime NULL DEFAULT NULL,
  `used_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mobile`(`mobile` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_tenant_notify_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `user_type` tinyint NOT NULL,
  `business_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `template_id` bigint NULL COMMENT '当前租户模板引用；原始模板已删除的历史通知为空',
  `source_template_id` bigint NULL COMMENT '迁移前的原始模板编号，保留历史来源',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_nickname` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_type` int NOT NULL,
  `template_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_status` bit(1) NOT NULL,
  `read_time` datetime NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_system_notify_message_tenant_business_key` (`tenant_id`, `business_key`),
  INDEX `idx_user_id_user_type_read_status`(`user_id` ASC, `user_type` ASC, `read_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Match the source notification parameter capacity, including tables created by an earlier run.
ALTER TABLE system_tenant_notify_message
  MODIFY COLUMN template_params text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

-- A historical message is a frozen snapshot. Its original template may have been
-- physically removed. Preserve that source identity separately from a live tenant template.
-- Recovery: retain both columns; template_id cannot be made NOT NULL while archived
-- messages without a live template exist. Never recreate template definitions from rendered text.
DROP PROCEDURE IF EXISTS ensure_notify_message_source_identity;
DELIMITER //
CREATE PROCEDURE ensure_notify_message_source_identity()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_tenant_notify_message' AND COLUMN_NAME='source_template_id') THEN
    ALTER TABLE system_tenant_notify_message ADD COLUMN source_template_id bigint NULL
      COMMENT '迁移前的原始模板编号，保留历史来源' AFTER template_id;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_tenant_notify_message' AND COLUMN_NAME='template_id' AND IS_NULLABLE='NO') THEN
    ALTER TABLE system_tenant_notify_message MODIFY COLUMN template_id bigint NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()
      AND TABLE_NAME='system_tenant_notify_message' AND CONSTRAINT_NAME='chk_notify_template_identity') THEN
    ALTER TABLE system_tenant_notify_message ADD CONSTRAINT chk_notify_template_identity
      CHECK (template_id IS NOT NULL OR source_template_id IS NOT NULL);
  END IF;
END//
DELIMITER ;
CALL ensure_notify_message_source_identity();
DROP PROCEDURE ensure_notify_message_source_identity;

CREATE TABLE IF NOT EXISTS system_tenant_message_log_owner (
  log_kind varchar(8) NOT NULL, log_id bigint NOT NULL, tenant_id bigint NOT NULL,
  PRIMARY KEY (log_kind,log_id)
) ENGINE=InnoDB;
DROP PROCEDURE IF EXISTS seed_tenant_notification_resources;
DELIMITER //
CREATE PROCEDURE seed_tenant_notification_resources()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  INSERT INTO system_tenant_message_log_owner (log_kind,log_id,tenant_id)
  SELECT 'notify',l.id,l.tenant_id FROM system_notify_message l WHERE l.tenant_id>0
  AND NOT EXISTS (SELECT 1 FROM system_tenant_message_log_owner o WHERE o.log_kind='notify' AND o.log_id=l.id);
  INSERT INTO system_tenant_message_log_owner (log_kind,log_id,tenant_id)
  SELECT 'notify',l.id,u.tenant_id FROM system_notify_message l JOIN system_users u ON u.id=l.user_id AND l.user_type=2
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_message_log_owner o WHERE o.log_kind='notify' AND o.log_id=l.id);
  IF EXISTS (SELECT 1 FROM system_notify_message l LEFT JOIN system_tenant_message_log_owner o
      ON o.log_kind='notify' AND o.log_id=l.id LEFT JOIN system_tenant t ON t.id=o.tenant_id
      WHERE o.tenant_id IS NULL OR t.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_NOTIFY_HISTORICAL_LOG_OWNER_REQUIRED';
  END IF;


  INSERT INTO system_tenant_message_log_owner (log_kind,log_id,tenant_id)
  SELECT 'mail', l.id, u.tenant_id FROM system_mail_log l JOIN system_users u ON u.id=l.user_id AND l.user_type=2
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_message_log_owner o WHERE o.log_kind='mail' AND o.log_id=l.id);
  IF EXISTS (SELECT 1 FROM system_mail_log l LEFT JOIN system_tenant_message_log_owner o
      ON o.log_kind='mail' AND o.log_id=l.id LEFT JOIN system_tenant t ON t.id=o.tenant_id
      WHERE o.tenant_id IS NULL OR t.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_MAIL_HISTORICAL_LOG_OWNER_REQUIRED';
  END IF;

  INSERT INTO system_tenant_message_log_owner (log_kind,log_id,tenant_id)
  SELECT 'sms', l.id, u.tenant_id FROM system_sms_log l JOIN system_users u ON u.id=l.user_id AND l.user_type=2
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_message_log_owner o WHERE o.log_kind='sms' AND o.log_id=l.id);
  IF EXISTS (SELECT 1 FROM system_sms_log l LEFT JOIN system_tenant_message_log_owner o
      ON o.log_kind='sms' AND o.log_id=l.id LEFT JOIN system_tenant t ON t.id=o.tenant_id
      WHERE o.tenant_id IS NULL OR t.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_SMS_HISTORICAL_LOG_OWNER_REQUIRED';
  END IF;

  INSERT INTO system_tenant_message_log_owner (log_kind,log_id,tenant_id)
  SELECT 'code',s.id,s.tenant_id FROM system_sms_code s JOIN system_tenant t ON t.id=s.tenant_id
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_message_log_owner o WHERE o.log_kind='code' AND o.log_id=s.id);
  IF EXISTS (SELECT 1 FROM system_sms_code s LEFT JOIN system_tenant_message_log_owner o
      ON o.log_kind='code' AND o.log_id=s.id LEFT JOIN system_tenant t ON t.id=o.tenant_id
      WHERE o.tenant_id IS NULL OR t.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_SMS_CODE_HISTORICAL_OWNER_REQUIRED';
  END IF;

  IF EXISTS (SELECT 1 FROM system_mail_template s LEFT JOIN system_mail_account p ON p.id=s.account_id WHERE p.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_NOTIFICATION_SOURCE_REFERENCE_MISSING';
  END IF;

  IF EXISTS (SELECT 1 FROM system_sms_template s LEFT JOIN system_sms_channel p ON p.id=s.channel_id WHERE p.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_NOTIFICATION_SOURCE_REFERENCE_MISSING';
  END IF;

  INSERT INTO system_tenant_mail_account (tenant_id,source_id,mail,username,password,host,port,ssl_enable,starttls_enable,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.mail,s.username,s.password,s.host,s.port,s.ssl_enable,s.starttls_enable,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_mail_account s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_mail_account x WHERE x.tenant_id=t.id AND x.source_id=s.id);

  INSERT INTO system_tenant_sms_channel (tenant_id,source_id,signature,code,status,remark,api_key,api_secret,callback_url,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.signature,s.code,s.status,s.remark,s.api_key,s.api_secret,s.callback_url,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_sms_channel s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_sms_channel x WHERE x.tenant_id=t.id AND x.source_id=s.id);

  INSERT INTO system_tenant_mail_template (tenant_id,source_id,name,code,account_id,nickname,title,content,params,status,remark,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.name,s.code,p.id,s.nickname,s.title,s.content,s.params,s.status,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_mail_template s JOIN system_tenant_mail_account p ON p.tenant_id=t.id AND p.source_id=s.account_id
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_mail_template x WHERE x.tenant_id=t.id AND x.source_id=s.id);

  INSERT INTO system_tenant_sms_template (tenant_id,source_id,type,status,code,name,content,params,remark,api_template_id,channel_id,channel_code,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.type,s.status,s.code,s.name,s.content,s.params,s.remark,s.api_template_id,p.id,s.channel_code,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_sms_template s JOIN system_tenant_sms_channel p ON p.tenant_id=t.id AND p.source_id=s.channel_id
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_sms_template x WHERE x.tenant_id=t.id AND x.source_id=s.id);

  INSERT INTO system_tenant_notify_template (tenant_id,source_id,name,code,nickname,content,type,params,status,remark,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.name,s.code,s.nickname,s.content,s.type,s.params,s.status,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_notify_template s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_notify_template x WHERE x.tenant_id=t.id AND x.source_id=s.id);

  INSERT INTO system_tenant_mail_log (tenant_id,id,user_id,user_type,to_mails,cc_mails,bcc_mails,account_id,from_mail,template_id,template_code,template_nickname,template_title,template_content,template_params,send_status,send_time,send_message_id,send_exception,creator,create_time,updater,update_time,deleted)
  SELECT o.tenant_id,s.id,s.user_id,s.user_type,s.to_mails,s.cc_mails,s.bcc_mails,p.id,s.from_mail,q.id,s.template_code,s.template_nickname,s.template_title,s.template_content,s.template_params,s.send_status,s.send_time,s.send_message_id,s.send_exception,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_mail_log s JOIN system_tenant_message_log_owner o ON o.log_kind='mail' AND o.log_id=s.id
  JOIN system_tenant_mail_account p ON p.tenant_id=o.tenant_id AND p.source_id=s.account_id
  JOIN system_tenant_mail_template q ON q.tenant_id=o.tenant_id AND q.source_id=s.template_id
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_mail_log x WHERE x.id=s.id);

  IF (SELECT COUNT(*) FROM system_tenant_mail_log WHERE id IN (SELECT id FROM system_mail_log)) <> (SELECT COUNT(*) FROM system_mail_log) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_MAIL_LOG_REFERENCE_MISSING';
  END IF;

  INSERT INTO system_tenant_sms_log (tenant_id,id,channel_id,channel_code,template_id,template_code,template_type,template_content,template_params,api_template_id,mobile,user_id,user_type,send_status,send_time,api_send_code,api_send_msg,api_request_id,api_serial_no,receive_status,receive_time,api_receive_code,api_receive_msg,creator,create_time,updater,update_time,deleted)
  SELECT o.tenant_id,s.id,p.id,s.channel_code,q.id,s.template_code,s.template_type,s.template_content,s.template_params,s.api_template_id,s.mobile,s.user_id,s.user_type,s.send_status,s.send_time,s.api_send_code,s.api_send_msg,s.api_request_id,s.api_serial_no,s.receive_status,s.receive_time,s.api_receive_code,s.api_receive_msg,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_sms_log s JOIN system_tenant_message_log_owner o ON o.log_kind='sms' AND o.log_id=s.id
  JOIN system_tenant_sms_channel p ON p.tenant_id=o.tenant_id AND p.source_id=s.channel_id
  JOIN system_tenant_sms_template q ON q.tenant_id=o.tenant_id AND q.source_id=s.template_id
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_sms_log x WHERE x.id=s.id);

  IF (SELECT COUNT(*) FROM system_tenant_sms_log WHERE id IN (SELECT id FROM system_sms_log)) <> (SELECT COUNT(*) FROM system_sms_log) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_SMS_LOG_REFERENCE_MISSING';
  END IF;

  IF EXISTS (SELECT 1 FROM system_notify_message s
      JOIN system_tenant_message_log_owner o ON o.log_kind='notify' AND o.log_id=s.id
      JOIN system_tenant_notify_message t ON t.id=s.id
      WHERE t.tenant_id<>o.tenant_id OR NOT(t.user_id<=>s.user_id)
        OR NOT(t.user_type<=>s.user_type) OR NOT(t.template_code<=>s.template_code)
        OR NOT(t.template_content<=>s.template_content)
        OR (t.source_template_id IS NOT NULL AND t.source_template_id<>s.template_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_NOTIFY_HISTORICAL_IDENTITY_CONFLICT';
  END IF;
  INSERT INTO system_tenant_notify_message (id,user_id,user_type,business_key,template_id,source_template_id,template_code,template_nickname,template_content,template_type,template_params,read_status,read_time,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT s.id,s.user_id,s.user_type,s.business_key,p.id,s.template_id,s.template_code,s.template_nickname,s.template_content,s.template_type,s.template_params,s.read_status,s.read_time,s.creator,s.create_time,s.updater,s.update_time,s.deleted,o.tenant_id FROM system_notify_message s JOIN system_tenant_message_log_owner o ON o.log_kind='notify' AND o.log_id=s.id
  LEFT JOIN system_tenant_notify_template p ON p.tenant_id=o.tenant_id AND p.source_id=s.template_id
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_notify_message x WHERE x.id=s.id);
  UPDATE system_tenant_notify_message t JOIN system_notify_message s ON s.id=t.id
    JOIN system_tenant_message_log_owner o ON o.log_kind='notify' AND o.log_id=s.id AND o.tenant_id=t.tenant_id
    SET t.source_template_id=s.template_id,t.update_time=t.update_time WHERE t.source_template_id IS NULL;
  IF (SELECT COUNT(*) FROM system_tenant_notify_message WHERE id IN (SELECT id FROM system_notify_message)) <> (SELECT COUNT(*) FROM system_notify_message) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_NOTIFY_LOG_REFERENCE_MISSING';
  END IF;
  INSERT INTO system_tenant_sms_code (id,mobile,code,create_ip,scene,today_index,used,used_time,used_ip,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT s.id,s.mobile,s.code,s.create_ip,s.scene,s.today_index,s.used,s.used_time,s.used_ip,s.creator,s.create_time,s.updater,s.update_time,s.deleted,o.tenant_id
  FROM system_sms_code s JOIN system_tenant_message_log_owner o ON o.log_kind='code' AND o.log_id=s.id
  WHERE NOT EXISTS (SELECT 1 FROM system_tenant_sms_code x WHERE x.id=s.id);
  COMMIT;
END//
DELIMITER ;
CALL seed_tenant_notification_resources();
DROP PROCEDURE IF EXISTS seed_tenant_notification_resources;
-- Verification history and its original used/expiry state stay with the owning tenant.
