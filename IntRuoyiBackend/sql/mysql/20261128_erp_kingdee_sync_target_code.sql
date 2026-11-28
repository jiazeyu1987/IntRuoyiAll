-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_erp_kingdee_sync_runtime; type=schema; riskLevel=low
-- 按编号同步的成功、失败和运行中记录均保留目标编号；历史记录不推断、不补造。
ALTER TABLE `erp_kingdee_sync_run`
    ADD COLUMN `target_code` varchar(100) NULL COMMENT '按编号同步的目标编号' AFTER `trigger_type`,
    ADD INDEX `idx_erp_sync_run_target` (`tenant_id`, `sync_type`, `target_code`, `deleted`);
-- 回滚：先回退应用，再删除 idx_erp_sync_run_target 索引和 target_code 列。
