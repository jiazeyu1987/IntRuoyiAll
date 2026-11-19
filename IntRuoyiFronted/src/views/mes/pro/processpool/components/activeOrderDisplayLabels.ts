const verificationLabels: Record<string, string> = {
  VALID: '核验通过',
  INCOMPLETE: '证据不完整',
  MISMATCH: '核验不一致'
}

const dispositionLabels: Record<string, string> = {
  concession_release: '让步放行',
  rework: '返工',
  void: '作废',
  pending_review: '待评审',
  closed: '已关闭'
}

const documentStatusLabels: Record<string, string> = {
  Z: '暂存', A: '创建', B: '审核中', C: '已审核', D: '重新审核'
}

const requireLabel = (labels: Record<string, string>, value: string | undefined, field: string) => {
  if (!value) return '未记录'
  if (labels[value]) return labels[value]
  if (/[\u4e00-\u9fff]/.test(value)) return value
  throw new Error(`${field}缺少中文描述：${value}`)
}

export const verificationStatusLabel = (value?: string) =>
  requireLabel(verificationLabels, value, '核验状态')

export const nonconformanceDispositionLabel = (value?: string) =>
  requireLabel(dispositionLabels, value, '不合格评审处置')

export const pickDocumentStatusLabel = (value?: string) =>
  requireLabel(documentStatusLabels, value, '领料单状态')
