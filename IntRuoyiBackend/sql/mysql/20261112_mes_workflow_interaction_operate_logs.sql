-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260615_mes_edhr_tail_four_goals; type=data; riskLevel=medium
-- 将历史失败、取消和拦截记录映射为系统操作日志；保留原审计事件及发生时间。
-- 必须在新版日志写入代码部署前执行，避免迁移与实时写入并发。
-- 原记录未采集的 HTTP/IP/UA 信息保留 NULL，不推断、不填造数据。
INSERT INTO system_operate_log
  (trace_id, user_id, user_type, type, sub_type, biz_id, action, success, extra,
   request_method, request_url, user_ip, user_agent,
   creator, create_time, updater, update_time, deleted, tenant_id)
SELECT e.request_id, e.actor_user_id, 2, '生产业务交互',
       CASE e.result_status WHEN 'CANCELED' THEN '已取消'
                            WHEN 'BLOCKED' THEN '已拦截' WHEN 'FAILED' THEN '失败' END,
       e.id, e.action_name, b'0',
       JSON_SET(e.metadata_json, '$.auditEventId', e.id, '$.resultStatus', e.result_status,
                '$.actorName', e.actor_username),
       JSON_UNQUOTE(JSON_EXTRACT(e.metadata_json, '$.method')), NULL, NULL, NULL,
       e.creator, e.occurred_at, e.updater, e.occurred_at, b'0', e.tenant_id
FROM mes_pro_edhr_operation_audit_event e
WHERE e.object_type = 'WORKFLOW_INTERACTION'
  AND e.result_status IN ('CANCELED', 'BLOCKED', 'FAILED')
  AND e.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM system_operate_log l
    WHERE l.tenant_id = e.tenant_id AND l.type = '生产业务交互' AND l.biz_id = e.id
  )
ORDER BY e.occurred_at, e.id;
