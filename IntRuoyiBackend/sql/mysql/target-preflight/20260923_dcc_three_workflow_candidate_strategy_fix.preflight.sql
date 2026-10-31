-- release-target-preflight: migrationId=20260923_dcc_three_workflow_candidate_strategy_fix; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('act_re_procdef','act_re_model','act_re_deployment','act_ge_bytearray','bpm_process_definition_info','system_tenant','bpm_business_approval_policy'))=7)
 AND ((SELECT COUNT(*) FROM system_tenant WHERE id IN (1,122) AND deleted=0 AND status=0)=2)
 AND ((SELECT COUNT(*) FROM act_re_procdef p JOIN act_re_model m ON m.ID_=CONCAT(p.KEY_,'-model-tenant-',p.TENANT_ID_) AND m.TENANT_ID_=p.TENANT_ID_ JOIN act_ge_bytearray b ON b.ID_=m.EDITOR_SOURCE_VALUE_ID_ AND b.DEPLOYMENT_ID_=p.DEPLOYMENT_ID_ JOIN bpm_process_definition_info i ON i.process_definition_id=p.ID_ AND i.tenant_id=CAST(p.TENANT_ID_ AS UNSIGNED) AND i.deleted=0 WHERE p.KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete') AND p.TENANT_ID_ IN ('1','122') AND p.VERSION_=1 AND p.ID_=CONCAT(p.KEY_,':1:dcc-three-workflow-tenant-',p.TENANT_ID_) AND p.DERIVED_FROM_ IS NULL AND p.DERIVED_FROM_ROOT_ IS NULL AND p.DERIVED_VERSION_=0 AND b.BYTES_ IS NOT NULL)=6)
 AND (NOT EXISTS(SELECT 1 FROM act_re_procdef WHERE KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete') AND TENANT_ID_ IN ('1','122') AND VERSION_>=2))
 AND (NOT EXISTS(SELECT 1 FROM act_re_model WHERE ID_ REGEXP '^dcc-controlled-file-(upload|revision|obsolete)-model-v2-tenant-(1|122)$'))
 AND (NOT EXISTS(SELECT 1 FROM act_re_deployment WHERE ID_ REGEXP '^dcc-controlled-file-(upload|revision|obsolete)-deploy-v2-tenant-(1|122)$'))
 AND (NOT EXISTS(SELECT 1 FROM act_ge_bytearray WHERE ID_ REGEXP '^dcc-controlled-file-(upload|revision|obsolete)-bpmn-v2-tenant-(1|122)$'))
 THEN 'TARGET_PREFLIGHT_PASS:20260923_dcc_three_workflow_candidate_strategy_fix' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260923_dcc_three_workflow_candidate_strategy_fix:TARGET_PREREQUISITE_OR_CONFLICT' END;
