-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_mes_smart_scheduling_t1_schema; type=schema; riskLevel=medium
-- 产品绑定、生产组长和工序配置共享整份版本快照，不能受 TEXT 的 65535 字节限制。
-- 仅扩容存储；保留原字符集、排序规则、可空性及注释，不改写 JSON、hash 或版本状态。
-- 执行前评估表大小及 DDL 锁等待；不自动缩回 TEXT，已有大快照会超出其容量。
ALTER TABLE `mes_pro_route_version`
  MODIFY COLUMN `route_snapshot_json` LONGTEXT
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '路线版本快照';
