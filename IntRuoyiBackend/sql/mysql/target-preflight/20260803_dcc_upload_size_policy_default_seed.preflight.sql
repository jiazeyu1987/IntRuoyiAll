-- release-target-preflight: migrationId=20260803_dcc_upload_size_policy_default_seed; allowedEnvironments=test,backup,prod
-- A valid custom GLOBAL/PURPOSE policy is allowed. Block same-code disabled/expired/deleted rows that prevent insertion without providing effective coverage.
WITH seed AS (SELECT 'SOURCE' COLLATE utf8mb4_unicode_ci purpose,'DCC_UPLOAD_DEFAULT_SOURCE_V1' COLLATE utf8mb4_unicode_ci policy_code UNION ALL SELECT 'DRAWING_PDF' COLLATE utf8mb4_unicode_ci purpose,'DCC_UPLOAD_DEFAULT_DRAWING_PDF_V1' COLLATE utf8mb4_unicode_ci policy_code UNION ALL SELECT 'TRAINING_RECORD' COLLATE utf8mb4_unicode_ci purpose,'DCC_UPLOAD_DEFAULT_TRAINING_RECORD_V1' COLLATE utf8mb4_unicode_ci policy_code UNION ALL SELECT 'EXTERNAL_REVIEW_OUTPUT' COLLATE utf8mb4_unicode_ci purpose,'DCC_UPLOAD_DEFAULT_EXTERNAL_REVIEW_OUTPUT_V1' COLLATE utf8mb4_unicode_ci policy_code), tenants AS (SELECT DISTINCT tenant_id FROM dcc_file_category WHERE deleted=0 AND active=1)
SELECT CASE WHEN

  NOT EXISTS (SELECT 1 FROM tenants t CROSS JOIN seed s
    WHERE NOT EXISTS (SELECT 1 FROM dcc_controlled_file_upload_policy e
      WHERE e.tenant_id=t.tenant_id AND e.deleted=0 AND e.enabled=b'1' AND e.max_bytes>0
        AND (e.effective_from IS NULL OR e.effective_from<=NOW()) AND (e.effective_to IS NULL OR e.effective_to>NOW())
        AND (e.scope_type='GLOBAL' OR (e.scope_type='PURPOSE' AND e.purpose=s.purpose)))
    AND EXISTS (SELECT 1 FROM dcc_controlled_file_upload_policy collision WHERE collision.tenant_id=t.tenant_id
      AND collision.policy_code=s.policy_code))
THEN 'TARGET_PREFLIGHT_PASS:20260803_dcc_upload_size_policy_default_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260803_dcc_upload_size_policy_default_seed' END;
