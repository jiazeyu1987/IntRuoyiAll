-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260920_mes_edhr_nonconformance_review_active_order_fact; type=schema; riskLevel=medium
-- eDHR 不合格评审编号按租户持续递增；月份仅展示，不按月重置流水。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_nonconformance_review_counter` (
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `current_serial` bigint NOT NULL DEFAULT -1 COMMENT '当前流水，首次分配从0开始',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR不合格评审租户编号计数器';
