// 仅展示层映射：不改变提交给治理接口的枚举或签名证据。
const labels: Record<string, string> = {
  READY: '已就绪', BLOCKED: '有阻断', CLEAR: '无阻断', VALID: '有效',
  APPROVED: '已批准', EFFECTIVE: '已生效', SUBMITTED: '已提交', DRAFT: '草稿', IN_REVIEW: '审核中', REJECTED: '已驳回',
  GO: '允许发布', COLLECTED: '已采集', SIGNED: '已签署', CLOSED: '已关闭',
  ALLOWED: '允许', RECORDED: '已记录', PASSED: '已通过', PASS: '通过', FAILED: '失败',
  GOVERNANCE: '治理模式', COMPLIANCE: '合规模式',
  DCC: '文控', EDHR: '电子批记录', SHOWROOM: '展示', INTAUTH: '内部授权',
  SIGNATURE_PERMISSION_EXCEPTION: '签名权限异常', SIGNATURE_LOCK_EXCEPTION: '签名锁定异常',
  SIGNATURE_FAILURE_RECORDED: '已记录签名失败', ABNORMAL_SIGNATURE_EVIDENCE: '签名证据异常',
  HASH_MISMATCH: '校验码不一致', HISTORICAL_UNBOUND: '历史记录未绑定', POLICY_EXCEPTION: '策略异常',
  APPROVE: '审批通过', REJECT: '审批驳回', SIGN: '签署',
  URS: '用户需求', FRS: '功能需求', RISK_ASSESSMENT: '风险评估', IQ: '安装确认',
  OQ: '运行确认', PQ: '性能确认', TRACE_MATRIX: '追溯矩阵',
  ELECTRONIC_SIGNATURE_SOP: '电子签名操作规程', EVIDENCE_INDEX: '证据索引'
}
export const governanceLabel = (value: unknown): string => {
  const text = String(value ?? '').trim()
  return labels[text] || text || '未提供'
}
export const governanceStatusLabel = (value: unknown): string => {
  const text = String(value ?? '').trim()
  return labels[text] || (/^[A-Z][A-Z0-9_]*$/.test(text) ? '未识别状态，请查看技术详情' : text || '等待来源')
}

// 表单默认状态不是来源事实；只有已评估或已读取正式来源时才作结论。
export const csvChangeStatusLabel = (formStatus: string, sourceStatus: string, evaluated: boolean): string =>
  evaluated ? governanceStatusLabel(formStatus) : sourceStatus ? governanceStatusLabel(sourceStatus) : '待核验'
