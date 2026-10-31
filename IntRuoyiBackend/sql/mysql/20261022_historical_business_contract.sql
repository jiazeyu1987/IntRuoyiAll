-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260723_unify_form_action_policy_into_business_approval_policy,20260710_dcc_product_catalog_database,20260613_mes_nightly_replan_job_2am,20260607_product_master_data; type=data; riskLevel=low; requiresTargetPreflight=true
-- Read-only business decision: retain 02:30 (02:00 was not requested), existing
-- catalog records and existing MDM menu grants. Deleted seed INSERT is not revocation.
-- FORM_TEMPLATE policies are already correct in the three observed baselines.
DELIMITER $$
CREATE PROCEDURE release_20261022_business_contract()
BEGIN
 IF (SELECT COUNT(*) FROM infra_job WHERE handler_name='mesProNightlyReplanJob' AND deleted=b'0')<>1
 OR NOT EXISTS(SELECT 1 FROM infra_job WHERE handler_name='mesProNightlyReplanJob' AND deleted=b'0' AND cron_expression='0 30 2 * * ?' AND status=1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='NIGHTLY_JOB_IDENTITY_OR_RETAINED_0230_CONFLICT';
 END IF;
 IF (SELECT COUNT(*) FROM bpm_business_approval_policy WHERE tenant_id IN (1,122) AND object_type='FORM_TEMPLATE' AND action_code='UPGRADE' AND object_state='DRAFT' AND data_domain='FORM_CENTER' AND system_code='FORM_CENTER' AND status='PUBLISHED' AND deleted=b'0')<>2
 OR (SELECT COUNT(DISTINCT tenant_id) FROM bpm_business_approval_policy WHERE tenant_id IN(1,122) AND data_domain='FORM_CENTER' AND system_code='FORM_CENTER' AND object_type='FORM_TEMPLATE' AND action_code='UPGRADE' AND object_state='DRAFT' AND status='PUBLISHED' AND deleted=b'0')<>2
 OR EXISTS(SELECT 1 FROM bpm_business_approval_policy WHERE tenant_id IN(1,122) AND object_type='FORM_TEMPLATE' AND action_code='UPGRADE' AND object_state='DRAFT' AND data_domain='FORM_CENTER' AND system_code='FORM_CENTER' AND status='PUBLISHED' AND deleted=b'0' AND (COALESCE(effect_executor_code,'')<>'FORM_TEMPLATE_UPGRADE' OR COALESCE(policy_mode,'') NOT IN('BPM_REQUIRED','DIRECT','SIGNATURE_REQUIRED') OR (policy_mode='BPM_REQUIRED' AND COALESCE(process_definition_key,'')<>'form-template-upgrade-v1'))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='FORM_TEMPLATE_POLICY_REQUIRES_EXPLICIT_BUSINESS_REVIEW';
 END IF;
 IF EXISTS(SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND (COALESCE(policy_mode,'') NOT IN('BPM_REQUIRED','DIRECT','SIGNATURE_REQUIRED') OR (policy_mode='BPM_REQUIRED' AND COALESCE(process_definition_key,'')=''))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BUSINESS_APPROVAL_POLICY_MODE_OR_PROCESS_KEY_CONFLICT';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_EXISTING_PRODUCT_CATALOG_MISSING';
 END IF;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission='mdm:product:map-showroom' AND deleted=b'0')>1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='MDM_MAP_SHOWROOM_PERMISSION_IDENTITY_CONFLICT';
 END IF;
END$$
DELIMITER ;
CALL release_20261022_business_contract();
DROP PROCEDURE release_20261022_business_contract;
