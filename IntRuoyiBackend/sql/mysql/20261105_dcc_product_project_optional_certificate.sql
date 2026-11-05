-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_explicit_data_relation; type=schema; riskLevel=medium
-- 产品与项目关联可以先建立，注册证建立后再显式补绑；保留租户/产品/项目唯一约束。
-- Rollback: 确认不存在 registration_certificate_id IS NULL 的关系后，才可恢复 NOT NULL。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `dcc_data_relation`
  MODIFY COLUMN `registration_certificate_id` bigint NULL COMMENT '注册证主档 ID，可在建证后补绑';
