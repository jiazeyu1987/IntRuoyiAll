import type { ActiveOrderSignatureEvidence } from '@/api/mes/pro/edhr/activeOrderSignature'

type Verification = ActiveOrderSignatureEvidence['verification']
const dimensions = { CONSISTENT: '一致', MISSING: '未记录', MISMATCH: '不一致' } as const
const statuses = {
  VALID: { label: '本地核验通过', type: 'success' },
  INCOMPLETE: { label: '证据未完整', type: 'warning' },
  MISMATCH: { label: '不一致', type: 'danger' }
} as const
const reasons: Record<string, string> = {
  AUDIT_LINK_MISSING: '创建审计未记录',
  AUDIT_LINK_DUPLICATE: '创建审计关联重复',
  AUDIT_INTEGRITY_FAILED: '创建审计完整性不一致',
  AUDIT_BINDING_MISMATCH: '创建审计绑定不一致',
  TIME_VALUE_MISMATCH: '签署时间值不一致',
  TIME_EVIDENCE_MISSING: '签署时间证据未记录',
  TIME_FORMAT_UNSUPPORTED: '时间证据格式不支持',
  DIGEST_MISMATCH: '摘要不一致',
  DIGEST_MISSING: '摘要未记录',
  IDENTITY_SNAPSHOT_MISSING: '签时姓名快照未记录',
  IDENTITY_BINDING_MISMATCH: '签署身份绑定不一致',
  FORMAT_UNSUPPORTED: '签名证据格式不支持',
  SIGNATURE_FACT_INVALID: '签名事实不完整'
}

export const signatureVerificationDisplay = (verification: Verification) => ({
  ...statuses[verification.verificationStatus],
  digest: dimensions[verification.digestStatus],
  time: dimensions[verification.timeConsistencyStatus],
  audit: dimensions[verification.auditBindingStatus],
  scope: verification.verificationScope === 'LOCAL_DIGEST_TIME_AND_CREATION_AUDIT'
    ? '本地摘要、签署时间值与创建审计' : verification.verificationScope,
  timeSource: verification.timeSource === 'SERVER_CLOCK' ? '服务器时钟' : '未知来源',
  timeTrust: verification.timeTrustStatus === 'NOT_BOUND' ? '未绑定外部可信时间' : verification.timeTrustStatus,
  reasons: verification.reasonCodes.length
    ? verification.reasonCodes.map(code => reasons[code] ? `${reasons[code]}（${code}）` : code).join('；')
    : '无异常原因'
})
