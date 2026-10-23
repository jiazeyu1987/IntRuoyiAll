SELECT 'column',TABLE_NAME,COLUMN_NAME,COLUMN_DEFAULT,COLLATION_NAME,IS_NULLABLE,GENERATION_EXPRESSION
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'dcc_%'
ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT 'process',ID_,KEY_,VERSION_,TENANT_ID_,SUSPENSION_STATE_,DEPLOYMENT_ID_,RESOURCE_NAME_
FROM act_re_procdef WHERE KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete') ORDER BY TENANT_ID_,KEY_,VERSION_;
SELECT 'model',ID_,KEY_,TENANT_ID_,VERSION_,DEPLOYMENT_ID_,EDITOR_SOURCE_VALUE_ID_
FROM act_re_model WHERE KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete') ORDER BY TENANT_ID_,KEY_,VERSION_;
SELECT 'definition-info',process_definition_id,model_id,tenant_id,deleted,form_type,form_custom_create_path,form_custom_view_path
FROM bpm_process_definition_info WHERE process_definition_id LIKE 'dcc-controlled-file-%' ORDER BY tenant_id,process_definition_id;
SELECT 'bpmn-hash',ID_,NAME_,DEPLOYMENT_ID_,OCTET_LENGTH(BYTES_),SHA2(BYTES_,256)
FROM act_ge_bytearray WHERE ID_ LIKE 'dcc-controlled-file-%' ORDER BY ID_;
SELECT 'policy',id,tenant_id,object_type,action_code,object_state,policy_mode,process_definition_key,effect_executor_code,status,deleted
FROM bpm_business_approval_policy WHERE BINARY data_domain=BINARY 'DCC' AND BINARY system_code=BINARY 'DCC' ORDER BY tenant_id,id;
SELECT 'template',id,code,type,status,deleted,SHA2(content,256),params
FROM system_notify_template WHERE code LIKE 'dcc_%' ORDER BY code,id;
SELECT 'job',id,name,status,handler_name,cron_expression,retry_count,retry_interval,monitor_timeout
FROM infra_job WHERE handler_name='dccControlledFileActivationJob' AND deleted=b'0';
SELECT 'route-null-action',COUNT(*) FROM dcc_category_approval_route WHERE action_type IS NULL OR action_type='';
SELECT 'transaction-count',COUNT(*) FROM information_schema.innodb_trx;
