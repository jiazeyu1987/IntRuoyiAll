-- release-target-preflight: migrationId=20260719_dcc_upload_form_policy_seed; allowedEnvironments=test,backup,prod
-- Dependencies have already created policy/process-info tables. Require both seeded tenants and refuse conflicting/duplicate published upload policies.
SELECT CASE WHEN

  NOT EXISTS (SELECT 1 FROM (SELECT 1 tenant_id UNION ALL SELECT 122) t
    WHERE NOT EXISTS (SELECT 1 FROM act_re_procdef d WHERE d.KEY_='dcc-controlled-file-approval' AND BINARY d.TENANT_ID_=BINARY CAST(t.tenant_id AS CHAR))
       OR NOT EXISTS (SELECT 1 FROM bpm_process_definition_info i JOIN act_re_procdef d ON d.ID_=i.process_definition_id
                      WHERE d.KEY_='dcc-controlled-file-approval' AND BINARY d.TENANT_ID_=BINARY CAST(t.tenant_id AS CHAR) AND i.tenant_id=t.tenant_id AND i.deleted=b'0'))
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='UPLOAD' AND object_state='DRAFT' AND status='PUBLISHED'
    AND (COALESCE(policy_mode,'')<>'BPM_REQUIRED' OR COALESCE(process_definition_key,'')<>'dcc-controlled-file-approval'
         OR COALESCE(effect_executor_code,'')<>'DCC_UPLOAD' OR COALESCE(form_policy_type,'')<>'NONE' OR COALESCE(form_slots_json,'[]')<>'[]'))
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy WHERE deleted=b'0' AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='UPLOAD' AND object_state='DRAFT' AND status='PUBLISHED' GROUP BY tenant_id HAVING COUNT(*)>1)
THEN 'TARGET_PREFLIGHT_PASS:20260719_dcc_upload_form_policy_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260719_dcc_upload_form_policy_seed' END;
