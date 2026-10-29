-- release-target-preflight: migrationId=20260922_dcc_three_workflow_bpmn_seed; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('act_re_procdef','act_re_model','act_re_deployment','act_ge_bytearray','bpm_process_definition_info','system_tenant','bpm_business_approval_policy'))=7)
 AND ((SELECT COUNT(*) FROM system_tenant WHERE id IN (1,122) AND deleted=0 AND status=0)=2)
 AND (NOT EXISTS(SELECT 1 FROM act_re_procdef WHERE KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete') AND TENANT_ID_ IN ('1','122')))
 AND (NOT EXISTS(SELECT 1 FROM act_re_model WHERE KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete') AND TENANT_ID_ IN ('1','122')))
 AND (NOT EXISTS(SELECT 1 FROM act_re_deployment WHERE ID_ REGEXP '^dcc-controlled-file-(upload|revision|obsolete)-deploy-tenant-(1|122)$'))
 AND (NOT EXISTS(SELECT 1 FROM act_ge_bytearray WHERE ID_ REGEXP '^dcc-controlled-file-(upload|revision|obsolete)-bpmn-tenant-(1|122)$'))
 AND (NOT EXISTS(SELECT 1 FROM bpm_process_definition_info WHERE process_definition_id REGEXP '^dcc-controlled-file-(upload|revision|obsolete):1:dcc-three-workflow-tenant-(1|122)$'))
 AND ((SELECT COUNT(*) FROM bpm_business_approval_policy WHERE tenant_id IN (1,122) AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='OBSOLETE' AND object_state='ACTIVE' AND status='PUBLISHED' AND deleted=0)=2)
 THEN 'TARGET_PREFLIGHT_PASS:20260922_dcc_three_workflow_bpmn_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260922_dcc_three_workflow_bpmn_seed:TARGET_PREREQUISITE_OR_CONFLICT' END;
