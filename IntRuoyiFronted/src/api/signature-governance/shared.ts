export type SignatureGovernanceModuleCode = 'DCC' | 'EDHR' | 'SHOWROOM' | 'INTAUTH'

export type SignatureGovernanceBlockerCode =
  | 'OWNER_MISSING'
  | 'OWNER_REVIEW_MISSING'
  | 'ENDPOINT_MISSING'
  | 'BUCKET_MISSING'
  | 'OBJECT_LOCK_MISSING'
  | 'VERSIONING_MISSING'
  | 'DEFAULT_RETENTION_MISSING'
  | 'RETENTION_VERIFICATION_SOURCE_MISSING'
  | 'RETENTION_MODE_MISSING'
  | 'PERMISSION_MISSING'
  | 'RECOVERY_RUNTIME_MISSING'
  | 'BACKUP_ID_MISSING'
  | 'OBJECT_KEY_MISSING'
  | 'VERSION_ID_MISSING'
  | 'RETAIN_UNTIL_MISSING'
  | 'SHA256_MISSING'
  | 'DCC_EVIDENCE_HASH_MISSING'
  | 'EDHR_ARCHIVE_HASH_MISSING'
  | 'SIGNATURE_HASH_MISSING'
  | 'SOURCE_TYPE_MISMATCH'
  | 'HASH_MISMATCH'
  | 'REPORT_WRITE_FAILED'
  | 'AUDIT_EVENT_MISSING'
  | 'AUDIT_WRITE_FAILED'
  | 'SAMPLE_DCC_SIGNATURE_MISSING'
  | 'SAMPLE_EDHR_ARCHIVE_MISSING'
  | 'QUALITY_APPROVAL_MISSING'
  | 'QA_APPROVAL_MISSING'
  | 'POLICY_SOURCE_MISSING'
  | 'MODULE_ADAPTER_MISSING'
  | 'AUTHORITY_SOURCE_UNCONFIRMED'
  | 'SIGNATURE_AUTH_UNAUTHORIZED'
  | 'SIGNATURE_AUTH_DISABLED'
  | 'SIGNATURE_AUTH_LOCKED'
  | 'ACTION_UNDEFINED'
  | 'REVIEW_OWNER_MISSING'
  | 'REVIEW_SIGNATURE_STRATEGY_MISSING'
  | 'TEST_TENANT_MISSING'

export interface SignatureGovernanceBlocker {
  code: SignatureGovernanceBlockerCode
  message: string
  impact: string
}

export const SIGNATURE_GOVERNANCE_PERMISSIONS = {
  RETENTION_QUERY: 'signature-governance:retention:query',
  RETENTION_MANAGE: 'signature-governance:retention:manage',
  PERIODIC_REVIEW_QUERY: 'signature-governance:periodic-review:query',
  PERIODIC_REVIEW_MANAGE: 'signature-governance:periodic-review:manage',
  CSV_PACKAGE_QUERY: 'signature-governance:csv-package:query',
  CSV_PACKAGE_MANAGE: 'signature-governance:csv-package:manage',
  POLICY_QUERY: 'signature-governance:policy:query',
  POLICY_MANAGE: 'signature-governance:policy:manage'
} as const
