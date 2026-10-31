-- release-target-preflight: migrationId=20260714_dcc_distribution_training_menu_retire; allowedEnvironments=test,backup,prod
-- Only the two known DCC entries may be retired; an unrelated identity is a conflict.
SELECT CASE WHEN NOT EXISTS (
  SELECT 1 FROM system_menu WHERE deleted=b'0' AND (
    (id=6808 AND (path IS NULL OR path <> 'controlled-file/distribution'))
    OR (id=6809 AND (path IS NULL OR path <> 'controlled-file/training'))
    OR (path='controlled-file/distribution' AND id<>6808)
    OR (path='controlled-file/training' AND id<>6809)
  )
) THEN 'TARGET_PREFLIGHT_PASS:20260714_dcc_distribution_training_menu_retire'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:dcc-retired-menu-identity-conflict' END;
