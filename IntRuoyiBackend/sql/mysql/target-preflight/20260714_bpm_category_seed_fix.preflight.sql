-- release-target-preflight: migrationId=20260714_bpm_category_seed_fix; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
NOT EXISTS (
    SELECT tenant_id,code FROM bpm_category WHERE deleted=b'0' AND code IN ('OA','BATCH_RECORD')
    GROUP BY tenant_id,code HAVING COUNT(*)>1
  )
  AND NOT EXISTS (
    SELECT 1 FROM bpm_category c LEFT JOIN system_tenant t ON t.id=c.tenant_id
    WHERE c.deleted=b'0' AND c.code IN ('OA','BATCH_RECORD') AND (t.id IS NULL OR t.deleted<>b'0')
  )
THEN 'TARGET_PREFLIGHT_PASS:20260714_bpm_category_seed_fix'
ELSE 'TARGET_PREFLIGHT_BLOCKED:review-required-before-data-migration' END;
