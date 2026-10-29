-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_mes_edhr_final_archive_work_task,20261005_mes_active_order_handoff; type=schema; riskLevel=medium
-- ROUTE_DEADLINE stores time commitments only; it must not invent an assignee.
ALTER TABLE mes_pro_edhr_work_task_assignment_rule MODIFY COLUMN assignee_user_id bigint NULL COMMENT '责任规则指定用户；独立时限规则不指定人员';
ALTER TABLE mes_pro_edhr_work_task MODIFY COLUMN assignee_user_id bigint NULL COMMENT '已指定或受理用户；仅候选责任池时为空';
ALTER TABLE mes_active_order_handoff_task
  ADD COLUMN due_time datetime NULL COMMENT '首次正式派单时按显式规则冻结的期限',
  ADD COLUMN overdue_notified_at datetime NULL COMMENT '本任务超期提醒已发送时间',
  ADD INDEX idx_handoff_due (tenant_id,status,overdue_notified_at,due_time);
-- No default deadlines or retrospective deadline backfill: formal owners configure only agreed SLAs.
