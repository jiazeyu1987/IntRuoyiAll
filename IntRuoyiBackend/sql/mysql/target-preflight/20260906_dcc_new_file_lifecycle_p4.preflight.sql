-- release-target-preflight: migrationId=20260906_dcc_new_file_lifecycle_p4; allowedEnvironments=test,backup,prod
-- Converts an existing unique valid publish policy to DIRECT; no source is a real error, not an empty successful conversion.
SELECT CASE WHEN

  EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='PUBLISH' AND object_state='READY_TO_PUBLISH' AND status='PUBLISHED')
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='PUBLISH' AND object_state='READY_TO_PUBLISH' AND status='PUBLISHED' GROUP BY tenant_id HAVING COUNT(*)<>1)
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='PUBLISH' AND object_state='READY_TO_PUBLISH' AND status='PUBLISHED'
    AND (COALESCE(effect_executor_code,'')<>'DCC_PUBLISH' OR COALESCE(form_policy_type,'')<>'NONE' OR COALESCE(form_slots_json,'[]')<>'[]'
         OR COALESCE(policy_mode,'') NOT IN ('DIRECT','BPM_REQUIRED','SIGNATURE_REQUIRED')))
THEN 'TARGET_PREFLIGHT_PASS:20260906_dcc_new_file_lifecycle_p4' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260906_dcc_new_file_lifecycle_p4' END;
