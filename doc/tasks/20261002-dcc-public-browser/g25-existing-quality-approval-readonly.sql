-- SELECT ONLY, prepared for Root; not executed by this child.
-- Registry has no signature FK. Joined signature rows are exact-hash/version metadata candidates only,
-- never an inferred QA approval. A canonical signature hash can differ from raw policy SHA.
-- Zero registry rows means no existing exact candidate/admin registration was found, not authorization to invent one.
SELECT JSON_OBJECT(
  'kind','existing_exact_policy_approval_registration',
  'database',DATABASE(),'serverUuid',@@server_uuid,
  'tenantId',CAST(p.tenant_id AS CHAR),'approvalId',CAST(p.id AS CHAR),
  'policyVersion',p.policy_version,'policyHash',p.policy_hash,
  'approvedBy',CAST(p.approved_by AS CHAR),'account',u.username,
  'approvedAt',CAST(p.approved_at AS CHAR),'approvalReference',p.approval_reference,
  'coverageReportHash',p.coverage_report_hash,
  'expectedCoverageHashMatches',BINARY p.coverage_report_hash=BINARY '319fc04a677dcb8670b420c9791bf5e4584d6708e0e765e58dbb637d9ab6b558',
  'signatureCandidateKind',IF(s.id IS NULL,'NO_EXACT_HASH_SIGNATURE_METADATA','EXACT_HASH_VERSION_METADATA_CANDIDATE_NOT_APPROVAL_PROOF'),
  'signatureId',CAST(s.id AS CHAR),'signatureActorId',CAST(s.actor_id AS CHAR),
  'signatureModule',s.module_code,'signatureAction',s.action_code,
  'signatureSubjectType',s.subject_type,'signatureSubjectId',s.subject_id,
  'signatureSubjectVersion',s.subject_version,'signedAt',CAST(s.signed_at AS CHAR),
  'signatureContentHash',s.content_hash,'signatureAfterContentHash',s.after_content_hash,
  'signatureVerificationStatus',s.verification_status,'signatureEvidenceHash',s.evidence_hash
) AS fact
FROM gxp_audit_policy_version p
JOIN system_users u ON u.id=p.approved_by AND u.tenant_id=p.tenant_id
  AND u.status=0 AND u.deleted=b'0' AND BINARY u.username=BINARY 'admin'
LEFT JOIN system_electronic_signature s ON s.tenant_id=p.tenant_id AND s.actor_id=p.approved_by
  AND s.deleted=b'0' AND BINARY s.subject_version=BINARY p.policy_version
  AND (BINARY s.content_hash=BINARY p.policy_hash OR BINARY s.after_content_hash=BINARY p.policy_hash)
WHERE DATABASE()='ruoyi-vue-pro' AND p.tenant_id=1
  AND BINARY p.policy_version=BINARY '2026-10-dcc-integration-01'
  AND BINARY p.policy_hash=BINARY '776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df'
  AND (SELECT COUNT(*) FROM system_users a WHERE a.tenant_id=1 AND a.status=0
    AND a.deleted=b'0' AND BINARY a.username=BINARY 'admin')=1
ORDER BY p.id,s.id;
