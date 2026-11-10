-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260831_dcc_registration_certificate_change_approval_mvp,20260901_dcc_registration_certificate_business_event_notify_template; type=schema; riskLevel=medium
-- Acknowledgment is separate from reading the existing site message; no recipients or grants are inferred.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_registration_certificate_change_confirmation (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  certificate_id bigint NOT NULL,
  version_id bigint NOT NULL,
  event_key varchar(160) NOT NULL,
  recipient_user_id bigint NOT NULL,
  notify_message_id bigint NOT NULL,
  sent_at datetime NOT NULL,
  confirmed_at datetime DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_change_recipient (tenant_id,event_key,recipient_user_id),
  UNIQUE KEY uk_change_message (tenant_id,notify_message_id),
  KEY idx_change_certificate (tenant_id,certificate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册证变更发送及本人确认记录';
