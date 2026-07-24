-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=low
-- Expand API access log operation names so legitimate OpenAPI summaries can be persisted without truncation.
ALTER TABLE `infra_api_access_log`
    MODIFY COLUMN `operate_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作名';
