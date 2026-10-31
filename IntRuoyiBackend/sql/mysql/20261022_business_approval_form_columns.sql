-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy; type=schema; riskLevel=low; requiresTargetPreflight=true; applyOrder=10
-- The immutable July 19 schema predates these columns, while July 19-21 seeds
-- already use them. Run immediately after that schema, before ordinary seeds.
-- Match the July 23 unification migration without changing policy data.
DELIMITER $$
CREATE PROCEDURE release_business_approval_form_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='bpm_business_approval_policy' AND column_name='form_policy_type') THEN
    ALTER TABLE bpm_business_approval_policy
      ADD COLUMN form_policy_type varchar(32) DEFAULT NULL COMMENT 'form policy type when the action needs form slots'
      AFTER effect_executor_code;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='bpm_business_approval_policy' AND column_name='form_slots_json') THEN
    ALTER TABLE bpm_business_approval_policy
      ADD COLUMN form_slots_json longtext DEFAULT NULL COMMENT 'form policy slots json'
      AFTER form_policy_type;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='bpm_business_approval_policy'
        AND is_nullable='YES' AND column_default IS NULL AND extra=''
        AND ((column_name='form_policy_type' AND data_type='varchar' AND character_maximum_length=32)
          OR (column_name='form_slots_json' AND data_type='longtext'))) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BUSINESS_APPROVAL_FORM_COLUMN_CONTRACT_MISMATCH';
  END IF;
END$$
DELIMITER ;
CALL release_business_approval_form_columns();
DROP PROCEDURE release_business_approval_form_columns;
