import type { SignatureGovernanceEvidenceDetail } from '@/api/signature-governance/records'

export const createSignatureRecordEvidenceLoader = (
  fetchEvidence: (globalId: string) => Promise<SignatureGovernanceEvidenceDetail>,
  state: { loading: boolean; error: string; selected?: SignatureGovernanceEvidenceDetail }
) => {
  let generation = 0
  const clear = () => {
    generation++
    state.loading = false
    state.error = ''
    state.selected = undefined
  }
  const open = async (globalId: string) => {
    clear()
    const requestGeneration = generation
    state.loading = true
    try {
      if (!/^UNIFIED-[1-9]\d*$/.test(globalId)) throw new Error('统一签名编号无效')
      const result = await fetchEvidence(globalId)
      if (requestGeneration !== generation) return
      if (result.globalId !== globalId || `UNIFIED-${result.evidence.id}` !== globalId ||
          String(result.verification.signatureId) !== String(result.evidence.id)) {
        throw new Error('签名详情身份不一致')
      }
      state.selected = result
    } catch (error) {
      if (requestGeneration !== generation) return
      state.error = error instanceof Error ? error.message : '签名原始证据读取失败'
    } finally {
      if (requestGeneration === generation) state.loading = false
    }
  }
  return { open, clear }
}

export const evidenceDimensionLabel = (value: string) => ({
  CONSISTENT: '一致', MISSING: '未记录', MISMATCH: '不一致',
  DEVELOPMENT_DISABLED: '已停用（本地开发）', NOT_BOUND: '未绑定外部可信时间',
  SERVER_CLOCK: '服务器时钟', UNKNOWN: '未知来源'
}[value] || value)

export const signatureRecordVerificationDisplay = (verification: SignatureGovernanceEvidenceDetail['verification']) => {
  const auditDisabled = verification.auditBindingStatus === 'DEVELOPMENT_DISABLED'
  const reasons: Record<string, string> = {
    GXP_AUDIT_DEVELOPMENT_DISABLED: '本地开发环境已停用创建审计',
    AUDIT_LINK_MISSING: '创建审计未记录', AUDIT_LINK_DUPLICATE: '创建审计关联重复',
    AUDIT_INTEGRITY_FAILED: '创建审计完整性不一致', AUDIT_BINDING_MISMATCH: '创建审计绑定不一致',
    TIME_VALUE_MISMATCH: '签署时间值不一致', TIME_EVIDENCE_MISSING: '签署时间证据未记录',
    DIGEST_MISMATCH: '摘要不一致', DIGEST_MISSING: '摘要未记录', FORMAT_UNSUPPORTED: '签名证据格式不支持'
  }
  return {
    status: verification.verificationStatus === 'VALID'
      ? auditDisabled ? '摘要与时间核验通过（审计已停用）' : '本地核验通过'
      : ({ INCOMPLETE: '证据未完整', MISMATCH: '不一致' }[verification.verificationStatus] || verification.verificationStatus),
    scope: verification.verificationScope === 'LOCAL_DIGEST_TIME_AND_CREATION_AUDIT'
      ? auditDisabled ? '本地摘要与签署时间值（创建审计已停用）' : '本地摘要、签署时间值与创建审计'
      : verification.verificationScope,
    reasons: verification.reasonCodes.map(code => reasons[code] ? `${reasons[code]}（${code}）` : code).join('；') || '无异常原因'
  }
}

export const signatureRecordSignedAt = (value: string | number | undefined) => {
  if (value == null || value === '') return '未记录'
  // LocalDateTime belongs to the application's business zone; never reinterpret it in the browser zone.
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$/.test(value)) {
    return `${value.replace('T', ' ')}（Asia/Shanghai）`
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return `${value}（时间格式无效）`
  return `${new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' }).format(date)}（Asia/Shanghai）`
}
