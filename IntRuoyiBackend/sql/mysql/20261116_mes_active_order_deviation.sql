-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260924_mes_edhr_deviation_management,20260814_mes_production_release_flow; type=schema; riskLevel=medium
-- Production deviations retain their original active-order identity; completion does not rewrite signed snapshots.
ALTER TABLE mes_pro_edhr_deviation
  ADD COLUMN active_order_id bigint NULL COMMENT '发起时活跃订单ID，轮次不可变' AFTER deviation_code,
  MODIFY COLUMN batch_execution_id bigint NULL COMMENT '发起时正式批记录ID，生产中允许为空',
  MODIFY COLUMN batch_execution_code varchar(64) NULL COMMENT '发起时正式批记录编号快照',
  ADD INDEX idx_deviation_tenant_order_status (tenant_id, active_order_id, status, deleted);
