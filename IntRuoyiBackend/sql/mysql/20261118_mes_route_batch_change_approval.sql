-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261021_mes_craft_library,20260721_mes_route_version_publish_business_approval_policy_seed; type=schema; riskLevel=medium
-- 新类别默认关闭；现有审批请求继续使用创建时的策略及原执行器。
SET @batch_scope_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pro_craft_change' AND column_name='approval_scope'), 'SELECT 1', 'ALTER TABLE mes_pro_craft_change ADD COLUMN approval_scope VARCHAR(16) NOT NULL DEFAULT ''ROUTE''');
PREPARE batch_scope_stmt FROM @batch_scope_ddl;
EXECUTE batch_scope_stmt;
DEALLOCATE PREPARE batch_scope_stmt;
SET @batch_request_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pro_craft_change' AND column_name='approval_request_id'), 'SELECT 1', 'ALTER TABLE mes_pro_craft_change ADD COLUMN approval_request_id BIGINT NULL');
PREPARE batch_request_stmt FROM @batch_request_ddl;
EXECUTE batch_request_stmt;
DEALLOCATE PREPARE batch_request_stmt;
SET @batch_process_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pro_craft_change' AND column_name='approval_process_instance_id'), 'SELECT 1', 'ALTER TABLE mes_pro_craft_change ADD COLUMN approval_process_instance_id VARCHAR(64) NULL');
PREPARE batch_process_stmt FROM @batch_process_ddl;
EXECUTE batch_process_stmt;
DEALLOCATE PREPARE batch_process_stmt;

INSERT INTO bpm_business_approval_policy
 (tenant_id,data_domain,system_code,object_type,action_code,object_state,policy_mode,
  process_definition_key,effect_executor_code,status,remark,creator,create_time,updater,update_time,deleted)
SELECT t.id,'MES','MES','ROUTE_BATCH_CHANGE','PUBLISH','PREPARING','DIRECT',
 'mes-route-batch-change-approval-v1','MES_ROUTE_BATCH_CHANGE_PUBLISH','PUBLISHED',
 '共享工序、工艺、设备变更按整单审批；开关仅影响后续提交','1',NOW(),'1',NOW(),b'0'
FROM system_tenant t WHERE t.deleted=b'0' AND t.status=0 AND NOT EXISTS
 (SELECT 1 FROM bpm_business_approval_policy p WHERE p.tenant_id=t.id AND p.deleted=b'0'
  AND p.data_domain='MES' AND p.system_code='MES' AND p.object_type='ROUTE_BATCH_CHANGE'
  AND p.action_code='PUBLISH' AND p.object_state='PREPARING');
