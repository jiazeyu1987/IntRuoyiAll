import type {
  SignatureGovernanceRecordPageReqVO,
  SignatureGovernanceRecordSourceCode
} from '@/api/signature-governance/records'

export interface SignatureRecordFilters {
  pageNo: number
  pageSize: number
  recordScope?: string
  source?: SignatureGovernanceRecordSourceCode
  signatureId?: string
  keyword?: string
  signerKeyword?: string
  actionCode?: string
  signedAt?: string[]
}

export function buildSignatureRecordQuery(filters: SignatureRecordFilters) {
  const text = (value?: string) => value?.trim() || undefined
  const params: SignatureGovernanceRecordPageReqVO = {
    pageNo: filters.pageNo,
    pageSize: filters.pageSize,
    sourceCodes: filters.source ? [filters.source] : undefined,
    signatureId: text(filters.signatureId),
    keyword: text(filters.keyword),
    signerKeyword: text(filters.signerKeyword),
    actionCode: text(filters.actionCode)
  }
  if (filters.signedAt?.length === 2) {
    params.signedAt = filters.signedAt.map((value, index) =>
      /^\d{4}-\d{2}-\d{2}$/.test(value)
        ? `${value} ${index === 0 ? '00:00:00' : '23:59:59'}`
        : value
    )
  }
  return { params, mine: filters.recordScope === 'MY_SIGNATURES' }
}
