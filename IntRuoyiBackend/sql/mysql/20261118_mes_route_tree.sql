-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261118_mes_route_batch_change_approval,20261118_mes_route_batch_change_bpm_seed; type=schema; riskLevel=medium
-- 仅添加树形配置管理归属，不改动历史版本与生产记录。
SET @route_tree_parent = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pro_route' AND column_name='parent_id'), 'SELECT 1', 'ALTER TABLE mes_pro_route ADD COLUMN parent_id BIGINT NULL, ADD INDEX idx_route_tree_parent (tenant_id,parent_id,deleted)');
PREPARE tree_parent_stmt FROM @route_tree_parent;
EXECUTE tree_parent_stmt;
DEALLOCATE PREPARE tree_parent_stmt;
SET @route_tree_kind = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pro_route' AND column_name='node_type'), 'SELECT 1', 'ALTER TABLE mes_pro_route ADD COLUMN node_type VARCHAR(16) NOT NULL DEFAULT ''FLOW''');
PREPARE tree_kind_stmt FROM @route_tree_kind;
EXECUTE tree_kind_stmt;
DEALLOCATE PREPARE tree_kind_stmt;

INSERT INTO bpm_business_approval_policy
 (tenant_id,data_domain,system_code,object_type,action_code,object_state,policy_mode,
  process_definition_key,effect_executor_code,status,remark,creator,create_time,updater,update_time,deleted)
SELECT p.tenant_id,'MES','MES','ROUTE_TREE_CHANGE','PUBLISH','PREPARING','BPM_REQUIRED',
 'mes-route-batch-change-approval-v1','MES_ROUTE_BATCH_CHANGE_PUBLISH','PUBLISHED',
 '树形工艺父节点统一修改，所有下属流程一次审核整批发布','1',NOW(),'1',NOW(),b'0'
FROM bpm_business_approval_policy p
WHERE p.deleted=b'0' AND p.status='PUBLISHED' AND p.data_domain='MES' AND p.system_code='MES'
 AND p.object_type='ROUTE_BATCH_CHANGE' AND p.action_code='PUBLISH' AND p.object_state='PREPARING'
 AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy x WHERE x.tenant_id=p.tenant_id
  AND x.deleted=b'0' AND x.system_code='MES' AND x.data_domain='MES' AND x.object_type='ROUTE_TREE_CHANGE'
  AND x.action_code='PUBLISH' AND x.object_state='PREPARING');
