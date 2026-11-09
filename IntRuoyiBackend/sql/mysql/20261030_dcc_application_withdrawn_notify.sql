-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_notify_template_seed; type=seed; riskLevel=low
INSERT INTO system_notify_template
(id, name, code, type, nickname, content, params, status, remark, creator, create_time, updater, update_time, deleted)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM system_notify_template),
       'DCC申请撤回通知', 'dcc_controlled_file_withdrawn', 2, 'DCC系统',
       '受控文件流程《{processInstanceName}》已撤回，原因：{reason}。可在消息详情重新发起。',
       '["processInstanceName","reason","detailUrl"]', 0, '受控文件失败申请重新发起入口', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code = 'dcc_controlled_file_withdrawn');
