-- release-target-preflight: migrationId=20260830_dcc_registration_certificate_associated_company_backfill; allowedEnvironments=test,backup,prod
WITH owner_references AS (
  SELECT DISTINCT tenant_id, owner_company_id FROM dcc_registration_certificate
  WHERE deleted = b'0' AND status = 'ACTIVE' AND owner_company_id IS NOT NULL AND owner_company_id > 0
), pending_company AS (
  SELECT reference.tenant_id, reference.owner_company_id FROM owner_references reference
  WHERE NOT EXISTS (
    SELECT 1 FROM mdm_enterprise enterprise
    WHERE enterprise.id = reference.owner_company_id AND enterprise.tenant_id = reference.tenant_id
      AND enterprise.deleted = b'0' AND enterprise.type = 'OWNED_COMPANY' AND enterprise.status = 'ENABLE'
  )
)
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_registration_certificate','mdm_enterprise')) = 2
  AND NOT EXISTS (
    SELECT 1 FROM owner_references reference JOIN mdm_enterprise enterprise ON enterprise.id = reference.owner_company_id
    WHERE enterprise.tenant_id <> reference.tenant_id OR enterprise.deleted <> b'0'
      OR enterprise.type <> 'OWNED_COMPANY' OR enterprise.status <> 'ENABLE' OR TRIM(enterprise.name) = ''
  )
  AND NOT EXISTS (
    SELECT 1 FROM pending_company pending JOIN mdm_enterprise enterprise
      ON enterprise.tenant_id = pending.tenant_id
      AND enterprise.enterprise_code = CONCAT('HIST-REG-OWN-', pending.tenant_id, '-', pending.owner_company_id)
      AND enterprise.id <> pending.owner_company_id
  )
  -- The original INSERT uses owner_company_id as a global primary key.
  AND NOT EXISTS (
    SELECT 1 FROM pending_company GROUP BY owner_company_id HAVING COUNT(*) > 1
  )
THEN 'TARGET_PREFLIGHT_PASS:20260830_dcc_registration_certificate_associated_company_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260830_dcc_registration_certificate_associated_company_backfill' END;
