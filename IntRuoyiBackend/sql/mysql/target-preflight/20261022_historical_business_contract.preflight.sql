-- release-target-preflight: migrationId=20261022_historical_business_contract; allowedEnvironments=test,prod,backup
SELECT CASE WHEN
 (SELECT COUNT(*) FROM infra_job WHERE handler_name='mesProNightlyReplanJob' AND deleted=b'0')=1
 AND EXISTS(SELECT 1 FROM infra_job WHERE handler_name='mesProNightlyReplanJob' AND deleted=b'0' AND cron_expression='0 30 2 * * ?' AND status=1)
 AND (SELECT COUNT(*) FROM bpm_business_approval_policy WHERE tenant_id IN(1,122) AND data_domain='FORM_CENTER' AND system_code='FORM_CENTER' AND object_type='FORM_TEMPLATE' AND action_code='UPGRADE' AND object_state='DRAFT' AND status='PUBLISHED' AND deleted=b'0')=2
 AND (SELECT COUNT(DISTINCT tenant_id) FROM bpm_business_approval_policy WHERE tenant_id IN(1,122) AND data_domain='FORM_CENTER' AND system_code='FORM_CENTER' AND object_type='FORM_TEMPLATE' AND action_code='UPGRADE' AND object_state='DRAFT' AND status='PUBLISHED' AND deleted=b'0')=2
 AND NOT EXISTS(SELECT 1 FROM bpm_business_approval_policy WHERE tenant_id IN(1,122) AND data_domain='FORM_CENTER' AND system_code='FORM_CENTER' AND object_type='FORM_TEMPLATE' AND action_code='UPGRADE' AND object_state='DRAFT' AND status='PUBLISHED' AND deleted=b'0' AND (COALESCE(effect_executor_code,'')<>'FORM_TEMPLATE_UPGRADE' OR COALESCE(policy_mode,'') NOT IN('BPM_REQUIRED','DIRECT','SIGNATURE_REQUIRED') OR (policy_mode='BPM_REQUIRED' AND COALESCE(process_definition_key,'')<>'form-template-upgrade-v1')))
 AND NOT EXISTS(SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND (COALESCE(policy_mode,'') NOT IN('BPM_REQUIRED','DIRECT','SIGNATURE_REQUIRED') OR (policy_mode='BPM_REQUIRED' AND COALESCE(process_definition_key,'')='')))
 AND EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog')
 AND (SELECT COUNT(*) FROM system_menu WHERE permission='mdm:product:map-showroom' AND deleted=b'0')<=1
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='release_20261022_business_contract')
 THEN 'TARGET_PREFLIGHT_PASS:20261022_historical_business_contract' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_historical_business_contract:BUSINESS_CONTRACT_REQUIRES_REVIEW' END;
