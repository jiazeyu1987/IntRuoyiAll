-- release-target-preflight: migrationId=20260803_dcc_unclassified_upload_directory_seed; allowedEnvironments=test,backup,prod
-- Existing inactive same-code directory prevents insertion and the migration would fail afterward; reject before writes.
SELECT CASE WHEN

  NOT EXISTS (SELECT 1 FROM dcc_file_directory WHERE deleted=0 AND code='UNCLASSIFIED'
    GROUP BY tenant_id HAVING COUNT(*)>1)
  AND NOT EXISTS (SELECT 1 FROM dcc_file_directory WHERE deleted=0 AND code='UNCLASSIFIED' AND (active<>1 OR active IS NULL))

THEN 'TARGET_PREFLIGHT_PASS:20260803_dcc_unclassified_upload_directory_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260803_dcc_unclassified_upload_directory_seed' END;
