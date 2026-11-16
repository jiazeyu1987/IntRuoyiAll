-- release-migration: allowedEnvironments=test,backup,prod; type=schema; riskLevel=low
-- Existing and newly created tenants explicitly start with sequence enforcement disabled.
ALTER TABLE system_tenant ADD COLUMN frontline_process_sequence_enabled bit(1) NOT NULL DEFAULT b'0'
  COMMENT '一线生产提交按冻结工艺前置工序生产100%校验';
