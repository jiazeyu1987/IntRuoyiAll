import type {
  ApprovalTaskCapability,
  ApprovalTaskSummaryVO,
  ApprovalTaskTimelineEntryVO,
  ApprovalTaskViewType
} from '@/api/approval-center'

type EdhrReadFacts = ApprovalTaskSummaryVO | ApprovalTaskTimelineEntryVO
type DisplayType = 'success' | 'danger' | 'warning' | 'info'
export const APPROVAL_CAPABILITIES_DEFAULT_WIDTH = 180

const recorded = (value?: string | number) =>
  value === undefined || value === null || String(value).trim() === '' ? '未记录' : String(value)

export const edhrReadDisplay = (facts: EdhrReadFacts) => {
  const taskLabels: Record<string, string> = {
    CREATED: '交接已创建', WAIT: '待处理', RUNNING: '处理中', COMPLETED: '任务已完成',
    CANCELLED: '已取消', OVERDUE: '已逾期'
  }
  const taskLabel = taskLabels[facts.taskOutcome || '']
    || `任务状态未识别（${recorded(facts.sourceStatus)}）`
  const decision = facts.decisionState
  let decisionLabel = '决定未知'
  let decisionType: DisplayType = 'info'
  if (decision === 'APPROVE' && facts.approvalResult === 'APPROVE') {
    decisionLabel = '审批通过'
    decisionType = 'success'
  } else if (decision === 'REJECT' && facts.approvalResult === 'REJECT') {
    decisionLabel = '审批驳回'
    decisionType = 'danger'
  } else if (decision === 'UNSUPPORTED') {
    decisionLabel = `决定未识别（${recorded(facts.decisionSourceStatus)}）`
  }
  return {
    taskLabel,
    sourceStatus: recorded(facts.sourceStatus),
    businessStatus: recorded(facts.businessStatus),
    decisionLabel,
    decisionType,
    decisionSourceStatus: recorded(facts.decisionSourceStatus),
    // Preserve the formally stored opinion, including line breaks and punctuation.
    opinion: recorded(facts.approvalRemark),
    sourceRemark: facts.sourceRemark,
    terminationReason: facts.terminationReason,
    actorName: '姓名未记录',
    actorIdentity: facts.decisionActorUserId == null ? '未记录' : `系统用户 ID：${facts.decisionActorUserId}`,
    completionActorIdentity: facts.taskCompletedByUserId == null ? '未记录' : `系统用户 ID：${facts.taskCompletedByUserId}`,
    creationActorIdentity: facts.taskOutcome === 'CREATED' && 'actorUserId' in facts && facts.actorUserId != null
      ? `系统用户 ID：${facts.actorUserId}` : '未记录',
    creationAt: facts.taskOutcome === 'CREATED' && 'actedAt' in facts ? facts.actedAt : undefined,
    taskCreatedAt: facts.taskCreatedAt,
    taskCompletedAt: facts.taskCompletedAt,
    decisionAt: facts.decisionAt,
    evidenceId: recorded(facts.evidenceId),
    evidenceType: recorded(facts.evidenceType),
    domainReferenceId: 'domainReferenceId' in facts ? recorded(facts.domainReferenceId) : '未记录'
  }
}

export const bpmMissingPermissionReason = (row: ApprovalTaskSummaryVO) => {
  const domainNavigation = row.moduleCode === 'BPM'
    || (row.moduleCode === 'EDHR' && ['EDHR_WORK_TASK', 'ACTIVE_ORDER_HANDOFF'].includes(row.sourceTaskType || ''))
  if (!domainNavigation) return undefined
  const contractMissing = row.decisionDetailQuery?.navigationContractMissing
  if (contractMissing) return String(contractMissing)
  const missing = row.decisionDetailQuery?.requiredPermissionsMissing
  if (!missing) return undefined
  const labels: Record<string, string> = {
    'mes:pro-batch-record-execution:approve': '批记录审批',
    'mes:pro-batch-record-execution:track': '批记录跟踪查询',
    'mes:pro-batch-record-execution:signature-query': '批记录签名查询',
    'mes:pro-batch-record-execution:query': '批记录查询',
    'mes:pro-edhr-batch-execution:query': '批次执行查询',
    'mes:pro-edhr-work-task:query': '工作任务查询',
    'mes:pro-production-release:query': '生产放行查询',
    'mes:pro-feedback:query': '一线生产与PQC查询',
    'mes:pro-feedback:create': '一线生产与PQC填写',
    'mes:pro-process-pool-team-leader:query': '组长工作台查询',
    'mes:pro-edhr-nonconformance-review:query': '不合格评审查询'
  }
  return `领域办理缺少权限：${String(missing).split(',').map(code => labels[code] || code).join('、')}`
}

export const canProcessBpmInModule = (row: ApprovalTaskSummaryVO, view: ApprovalTaskViewType) =>
  view === 'TODO' && row.moduleCode === 'BPM' && !row.businessDeleted
  && Boolean(row.availableActions?.includes('PROCESS_IN_MODULE'))
  && Boolean(row.decisionDetailRoute?.trim())

export const bpmModuleDecisionLocation = (row: ApprovalTaskSummaryVO, view: ApprovalTaskViewType) =>
  canProcessBpmInModule(row, view)
    ? { path: row.decisionDetailRoute!, query: { ...row.decisionDetailQuery } }
    : undefined

export const compactApprovalCapabilities = (row: ApprovalTaskSummaryVO) => {
  const capabilities = [...new Set(row.capabilities || [])]
  const priority: ApprovalTaskCapability[] = ['TIMELINE']
  const main = priority.filter((capability) => capabilities.includes(capability))
  const others = capabilities.filter((capability) => !priority.includes(capability))
  return { visible: [...main, ...others.slice(0, 2)], overflow: others.slice(2) }
}
