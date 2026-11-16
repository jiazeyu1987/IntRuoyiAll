-- release-target-preflight: migrationId=20260720_dcc_publish_form_policy_seed; allowedEnvironments=test,backup,prod
-- Publish seed must have unique resolvable upload sources; do not let MAX(id) hide ambiguity or let an empty source silently seed nothing.
SELECT CASE WHEN

  (SELECT COUNT(DISTINCT tenant_id) FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='UPLOAD' AND object_state='DRAFT' AND status='PUBLISHED' AND tenant_id IN (1,122) AND COALESCE(process_definition_key,'')<>'')=2
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='UPLOAD' AND object_state='DRAFT' AND status='PUBLISHED' GROUP BY tenant_id HAVING COUNT(*)>1)
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='PUBLISH' AND object_state='READY_TO_PUBLISH' AND status='PUBLISHED' GROUP BY tenant_id HAVING COUNT(*)>1)
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='PUBLISH' AND object_state='READY_TO_PUBLISH' AND status='PUBLISHED'
    AND (COALESCE(policy_mode,'')<>'BPM_REQUIRED' OR COALESCE(process_definition_key,'')=''
         OR COALESCE(effect_executor_code,'')<>'DCC_PUBLISH' OR COALESCE(form_policy_type,'')<>'NONE' OR COALESCE(form_slots_json,'[]')<>'[]'))
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy p WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='UPLOAD' AND object_state='DRAFT' AND status='PUBLISHED'
    AND NOT EXISTS (SELECT 1 FROM act_re_procdef d WHERE d.KEY_=p.process_definition_key AND BINARY d.TENANT_ID_=BINARY CAST(p.tenant_id AS CHAR)))
THEN 'TARGET_PREFLIGHT_PASS:20260720_dcc_publish_form_policy_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260720_dcc_publish_form_policy_seed' END;
