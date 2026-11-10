-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261030_dcc_registration_certificate_regulatory_facts; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS dcc_registration_handling_config (
 tenant_id BIGINT NOT NULL PRIMARY KEY, responsible_user_id BIGINT NOT NULL,
 project_upload_days INT NOT NULL, renewal_days INT NOT NULL,
 configured_by BIGINT NOT NULL, configured_at DATETIME NOT NULL,
 CHECK (project_upload_days BETWEEN 1 AND 3650), CHECK (renewal_days BETWEEN 1 AND 3650)
);
CREATE TABLE IF NOT EXISTS dcc_registration_handling_task (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL,
 source_key VARCHAR(96) NOT NULL, task_kind VARCHAR(16) NOT NULL,
 project_code_id BIGINT NULL, certificate_id BIGINT NULL, source_version_id BIGINT NULL,
 title VARCHAR(512) NOT NULL, responsible_user_id BIGINT NOT NULL, assigned_user_id BIGINT NOT NULL,
 due_at DATETIME NOT NULL, status VARCHAR(16) NOT NULL, created_at DATETIME NOT NULL,
 last_request_id BIGINT NULL, actual_handler_id BIGINT NULL, result VARCHAR(32) NULL, completed_at DATETIME NULL,
 UNIQUE KEY uk_reg_handling_source (tenant_id, source_key),
 KEY idx_reg_handling_actor (tenant_id, assigned_user_id, status)
);
CREATE TABLE IF NOT EXISTS dcc_registration_handling_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, task_id BIGINT NOT NULL,
 event_type VARCHAR(32) NOT NULL, actor_id BIGINT NOT NULL, assigned_user_id BIGINT NULL,
 due_at DATETIME NULL, request_id BIGINT NULL, reason VARCHAR(1024) NOT NULL, created_at DATETIME NOT NULL,
 KEY idx_reg_handling_events (tenant_id, task_id, id)
);
CREATE TABLE IF NOT EXISTS dcc_registration_handling_attempt (
 tenant_id BIGINT NOT NULL, request_id BIGINT NOT NULL, task_id BIGINT NOT NULL,
 handler_id BIGINT NOT NULL, submitted_at DATETIME NOT NULL, result VARCHAR(32) NULL, decided_at DATETIME NULL,
 PRIMARY KEY (tenant_id, request_id), KEY idx_reg_handling_attempt_task (tenant_id, task_id)
);
INSERT INTO system_notify_template
(id,name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM system_notify_template), '注册证办理任务', 'DCC_REGISTRATION_HANDLING_TASK',2,'注册证管理',
       '请办理《{title}》，办理期限：{dueAt}。请在消息详情查看任务并上传证件或授权办理。',
       '["title","dueAt","handlingTaskId"]',0,'项目新建上传及失效换证任务；负责人和期限由业务显式配置','1',NOW(),'1',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='DCC_REGISTRATION_HANDLING_TASK');
