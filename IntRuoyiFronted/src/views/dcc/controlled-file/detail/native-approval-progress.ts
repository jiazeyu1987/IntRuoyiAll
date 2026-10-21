import type { ControlledFileApplicationRound, ControlledFileRoundFlowContract, DccApplicationType } from '@/api/dcc/controlledFile/applicationRead'
import type { DccTaskStageProgress } from '../shared/approval'

export interface ApprovalProgressScope {
  fileId: string
  bpmRound: string
  applicationType: DccApplicationType | 'LEGACY'
  flowContract?: ControlledFileRoundFlowContract | null
}
export interface ApprovalProgressTask { id?: string; processInstanceId?: string; taskDefinitionKey?: string; status?: number; children?: ApprovalProgressTask[] }
export type ApprovalProgressStage = Omit<DccTaskStageProgress, 'stageCode'> & { stageCode: string }
export const resolveApprovalProgressScope = (input: {
  fileId: string; bpmRound: string; nativeBpmRound?: string; rounds: ControlledFileApplicationRound[]
  approvalDetail?: { processInstance?: { id?: string; processDefinitionId?: string }; processDefinition?: { id?: string; key?: string } } | null
}): ApprovalProgressScope => {
  if (!input.fileId || !input.bpmRound || !Array.isArray(input.rounds)) throw new Error('审批进度轮次身份缺失')
  const matched = input.rounds.filter(row => String(row.controlledFileId) === input.fileId && row.bpmRound === input.bpmRound)
  if (matched.length === 1 && ['UPLOAD', 'REVISION', 'OBSOLETE'].includes(matched[0].applicationType)) {
    return { fileId: input.fileId, bpmRound: input.bpmRound, applicationType: matched[0].applicationType,
      flowContract: matched[0].flowContract }
  }
  const detail = input.approvalDetail
  if (matched.length === 0 && input.nativeBpmRound === input.bpmRound && detail?.processInstance?.id === input.bpmRound
    && detail.processDefinition?.key === 'dcc-controlled-file-approval' && detail.processDefinition.id
    && detail.processInstance.processDefinitionId === detail.processDefinition.id) {
    return { fileId: input.fileId, bpmRound: input.bpmRound, applicationType: 'LEGACY' }
  }
  throw new Error('当前文件与BPM的审批流程身份未记录或不一致')
}
const flattenTasks = (tasks: ApprovalProgressTask[]): ApprovalProgressTask[] => tasks.flatMap(task => [task, ...flattenTasks(task.children || [])])
export const getNativeApprovalFlowUnavailableReason = (scope?: ApprovalProgressScope): string => {
  if (!scope || scope.applicationType === 'LEGACY') return '本轮原生流程合同未记录'
  const flow = scope.flowContract
  if (!flow) return '本轮流程合同未记录，无法展示审批顺序'
  if (String(flow.controlledFileId) !== scope.fileId || flow.bpmRound !== scope.bpmRound
    || flow.applicationType !== scope.applicationType) throw new Error('本轮流程合同身份不一致')
  if (flow.status === 'NOT_RECORDED') return '本轮流程合同未记录，历史申请证据仍可查阅'
  if (flow.status === 'UNSUPPORTED_DEFINITION') return '本轮实际流程定义不支持，历史申请证据仍可查阅'
  if (flow.status !== 'READY') throw new Error('本轮流程合同状态不支持')
  if (!flow.processDefinitionId || !flow.deploymentId || !/^[a-f0-9]{64}$/.test(flow.definitionDigest))
    throw new Error('本轮流程合同定义身份缺失')
  const obsolete = scope.applicationType === 'OBSOLETE'
  if (obsolete) {
    if (flow.executionContract !== 'PUBLIC_MATRIX_NATIVE_V1' || flow.flowRule !== 'PUBLIC_V5_OBSOLETE_APPROVAL_END'
      || flow.needTraining !== null || !flow.formInstanceId || !flow.formInstanceCode)
      throw new Error('本轮作废流程合同不一致')
  } else {
    if (typeof flow.needTraining !== 'boolean') throw new Error('本轮流程合同培训选择未记录')
    if (!((flow.executionContract === 'PUBLIC_MATRIX_NATIVE_V1' && flow.flowRule === 'PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL')
      || (flow.executionContract === 'PUBLIC_MATRIX_NATIVE_V2' && flow.flowRule === 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM'
        && Number.isSafeInteger(flow.attributeRound) && flow.attributeRound > 0)
      || (flow.executionContract === 'LEGACY_V4_ACTION' && flow.flowRule === 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL')))
      throw new Error('本轮流程合同规则不支持')
  }
  return ''
}

export const getNativeApprovalProcessText = (scope?: ApprovalProgressScope): string => {
  if (scope?.applicationType === 'LEGACY') return '按本轮原四级审批记录展示'
  const reason = getNativeApprovalFlowUnavailableReason(scope)
  if (reason) return reason
  const flow = scope!.flowContract!
  if (scope!.applicationType === 'OBSOLETE') return '直属主管审核 → 部门负责人指派及会签 → 批准，批准后作废并结束流程。'
  if (flow.flowRule === 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL')
    return '会签 → 批准 → 培训（如需） → 文控审核 → 受控 → 文控下发（本轮原流程）。'
  if (flow.flowRule === 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM')
    return '直属主管审核 → 部门负责人指派及会签 → 批准 → 文控审核 → 培训（如需） → 延迟方式由原编制人确认生效时间并签名 → 受控 → 生效后自动下发。'
  return '直属主管审核 → 部门负责人指派及会签 → 批准 → 文控标准化审核 → 培训（如需） → 受控 → 生效后自动下发。'
}

export const buildNativeApprovalProgress = (scope: ApprovalProgressScope, facts: {
  fileId: string; fileBpmRound?: string; needTraining?: boolean; status?: string; controlledTime?: string | null
  activatedTime?: string | null; distributedTime?: string | null; trainingRecordAvailable?: boolean
  effectiveMode?: 'AFTER_STANDARDIZATION_SIGNATURE' | 'DELAYED' | 'PRESET_DATE' | null
}, tasks: ApprovalProgressTask[]): ApprovalProgressStage[] => {
  if (scope.applicationType === 'LEGACY' || scope.fileId !== facts.fileId) throw new Error('原生审批进度上下文无效')
  const unavailable = getNativeApprovalFlowUnavailableReason(scope)
  if (unavailable) throw new Error(unavailable)
  const flow = scope.flowContract!
  const currentFacts = facts.fileBpmRound === scope.bpmRound
  const publicFlow = flow.executionContract === 'PUBLIC_MATRIX_NATIVE_V1' || flow.executionContract === 'PUBLIC_MATRIX_NATIVE_V2'
  const confirmationFlow = flow.executionContract === 'PUBLIC_MATRIX_NATIVE_V2'
  const actualTasks = flattenTasks(tasks)
  if (actualTasks.some(task => !task.id || task.processInstanceId !== scope.bpmRound)) throw new Error('审批任务不属于当前BPM轮次')
  const confirmationTasks=actualTasks.filter(task=>task.taskDefinitionKey==='DELAYED_EFFECTIVE_CONFIRM')
  if(confirmationFlow && currentFacts && !['DELAYED','AFTER_STANDARDIZATION_SIGNATURE'].includes(facts.effectiveMode || ''))
    throw new Error('本次新流程生效方式未记录，不能推断延迟确认分支')
  if(confirmationTasks.length>1 || confirmationTasks.length>0 && (!confirmationFlow || currentFacts && facts.effectiveMode!=='DELAYED'))
    throw new Error('延迟确认任务与本次精确执行合同或生效方式不一致')
  if(confirmationFlow && currentFacts && facts.status==='PENDING_DELAYED_EFFECTIVE_CONFIRM' && confirmationTasks.length!==1)
    throw new Error('延迟确认状态缺少本次原编制人的真实确认任务')
  const stages: Array<[string, string]> = [['MATRIX_REVIEW', '会签'], ['MATRIX_APPROVAL', '批准']]
  if (publicFlow) stages.unshift(['SUPERVISOR_REVIEW', '直属主管审核'])
  if (scope.applicationType !== 'OBSOLETE') {
    if (publicFlow) stages.push(['DOC_CONTROL_REVIEW', '文控标准化审核'])
    if (flow.needTraining) stages.push(['APPLICANT_TRAINING_RECORD', '培训（本次上传人上传线下记录）'])
    if (!publicFlow) stages.push(['DOC_CONTROL_REVIEW', '文控审核'])
    if(confirmationFlow && (currentFacts ? facts.effectiveMode==='DELAYED' : confirmationTasks.length===1))
      stages.push(['DELAYED_EFFECTIVE_CONFIRM','延迟生效确认（原编制人）'])
    stages.push(['CONTROLLED', '受控'], ['DISTRIBUTED', publicFlow ? '生效后自动下发' : '文控下发'])
  }
  return stages.map(([stageCode, stageName], index) => {
    const rows = actualTasks.filter(task => task.taskDefinitionKey === stageCode)
    const trainingNode = stageCode === 'APPLICANT_TRAINING_RECORD'
    const trainingCompleted = currentFacts && trainingNode && facts.trainingRecordAvailable === true
      && ((!publicFlow && facts.status === 'PENDING_DOC_CONTROL_REVIEW')
        || ['PENDING_DELAYED_EFFECTIVE_CONFIRM','READY_TO_PUBLISH', 'FINALIZING', 'FINALIZATION_FAILED', 'CONTROLLED_PENDING_EFFECTIVE', 'ACTIVE'].includes(facts.status || '')
        || Boolean(facts.controlledTime))
    const completedFact = currentFacts && (stageCode === 'CONTROLLED' ? Boolean(facts.controlledTime)
      : stageCode === 'DISTRIBUTED' ? Boolean(facts.distributedTime) : trainingCompleted)
    const factNode = trainingNode || stageCode === 'CONTROLLED' || stageCode === 'DISTRIBUTED'
    const totalCount = factNode ? 1 : rows.length
    const approvedCount = factNode ? Number(completedFact) : rows.filter(task => task.status === 2).length
    const rejectedCount = rows.filter(task => task.status === 3).length
    const runningCount = rows.filter(task => task.status === 1 || task.status === 7).length
    const isCompleted = totalCount > 0 && approvedCount === totalCount
    const isCurrent = runningCount > 0 || (currentFacts && (
      (trainingNode && facts.status === 'PENDING_APPLICANT_TRAINING_RECORD')
      || (!isCompleted && stageCode === 'CONTROLLED' && ['READY_TO_PUBLISH', 'FINALIZING', 'FINALIZATION_FAILED'].includes(facts.status || ''))
      || (!isCompleted && stageCode === 'DISTRIBUTED' && Boolean(facts.controlledTime)
        && (!publicFlow || (facts.status === 'ACTIVE' && Boolean(facts.activatedTime))))))
    return { stageCode, stageName, stageOrder: index + 1, totalCount, approvedCount, rejectedCount, runningCount,
      waitingCount: Math.max(totalCount - approvedCount - rejectedCount - runningCount, 0), completionText: `${approvedCount}/${totalCount}`,
      sameLayerHint: factNode && !currentFacts ? '本轮阶段证据未记录'
        : isCompleted ? '本阶段已完成' : isCurrent ? '当前阶段待处理' : '待进入本阶段',
      isCurrent, isCompleted, isPending: !isCurrent && !isCompleted }
  })
}
export const resolveSignatureDuty = (signature: { taskId?: string; actionType?: string }, tasks: ApprovalProgressTask[], scope?: ApprovalProgressScope): string => {
  if (!scope || !signature.taskId) return '签名职责未记录'
  const matched = flattenTasks(tasks).filter(task => task.id === signature.taskId && task.processInstanceId === scope.bpmRound)
  if (matched.length !== 1) return '签名职责未记录'
  if (scope.applicationType === 'LEGACY') return '四级审批人'
  const key = matched[0].taskDefinitionKey
  if (key === 'MATRIX_REVIEW') {
    if (signature.actionType === 'ASSIGN') return '部门负责人指派'
    return ['APPROVE', 'REJECT'].includes(signature.actionType || '') ? '会签人' : '签名职责未记录'
  }
  if (!['APPROVE', 'REJECT'].includes(signature.actionType || '')) return '签名职责未记录'
  return ({ SUPERVISOR_REVIEW: '直属主管', MATRIX_APPROVAL: '批准人', DOC_CONTROL_REVIEW: '文控审核人', APPLICANT_TRAINING_RECORD: '本次上传人培训记录', DELAYED_EFFECTIVE_CONFIRM: '原编制人延迟生效确认' } as Record<string, string>)[key || ''] || '签名职责未记录'
}

export const resolveRecordedSignatureDuty = (
  signature: { taskId?: string; actionType?: string; meaningCode?: string; processInstanceId?: string | null; versionNo?: string },
  selected?: { processInstanceId?: string | null; versionNo?: string } | null
): string => {
  if (!signature.taskId || !selected?.processInstanceId || !selected.versionNo
    || signature.processInstanceId !== selected.processInstanceId || signature.versionNo !== selected.versionNo)
    return '签名职责未记录'
  const action = signature.actionType
  if (action === 'ASSIGN') return signature.meaningCode === 'MATRIX_REVIEW_ASSIGN' ? '部门负责人指派' : '签名职责未记录'
  if (!['APPROVE', 'REJECT'].includes(action || '')) return '签名职责未记录'
  const stage = ['SUPERVISOR_REVIEW', 'MATRIX_REVIEW', 'MATRIX_APPROVAL', 'DOC_CONTROL_REVIEW', 'DOC_CONTROL_APPROVAL', 'DELAYED_EFFECTIVE_CONFIRM']
    .find(key => signature.meaningCode === `${key}_${action}`)
  return ({ SUPERVISOR_REVIEW: '直属主管', MATRIX_REVIEW: '会签人', MATRIX_APPROVAL: '批准人',
    DOC_CONTROL_REVIEW: '文控审核人', DOC_CONTROL_APPROVAL: '文控批准人', DELAYED_EFFECTIVE_CONFIRM: '原编制人延迟生效确认' } as Record<string, string>)[stage || ''] || '签名职责未记录'
}
