-- release-target-preflight: migrationId=20261022_business_approval_form_columns; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='bpm_business_approval_policy' AND column_name='effect_executor_code')
  AND NOT EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='bpm_business_approval_policy'
      AND column_name IN ('form_policy_type','form_slots_json')
      AND NOT (is_nullable='YES' AND column_default IS NULL AND extra=''
        AND ((column_name='form_policy_type' AND data_type='varchar' AND character_maximum_length=32)
          OR (column_name='form_slots_json' AND data_type='longtext'))))
THEN 'TARGET_PREFLIGHT_PASS:20261022_business_approval_form_columns'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_business_approval_form_columns' END;
