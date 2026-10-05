import type { ControlledFileApplicationRound, DccApplicationType } from '@/api/dcc/controlledFile/applicationRead'
import type { DccTaskStageProgress } from '../shared/approval'

export interface ApprovalProgressScope { fileId: string; bpmRound: string; applicationType: DccApplicationType | 'LEGACY' }
export interface ApprovalProgressTask { id?: string; processInstanceId?: string; taskDefinitionKey?: string; status?: number; children?: ApprovalProgressTask[] }
export type ApprovalProgressStage = Omit<DccTaskStageProgress, 'stageCode'> & { stageCode: string }
export const resolveApprovalProgressScope = (input: {
  fileId: string; bpmRound: string; nativeBpmRound?: string; rounds: ControlledFileApplicationRound[]
  approvalDetail?: { processInstance?: { id?: string; processDefinitionId?: string }; processDefinition?: { id?: string; key?: string } } | null
}): ApprovalProgressScope => {
  if (!input.fileId || !input.bpmRound || !Array.isArray(input.rounds)) throw new Error('审批进度轮次身份缺失')
  const matched = input.rounds.filter(row => String(row.controlledFileId) === input.fileId && row.bpmRound === input.bpmRound)
  if (matched.length === 1 && ['UPLOAD', 'REVISION', 'OBSOLETE'].includes(matched[0].applicationType)) {
    return { fileId: input.fileId, bpmRound: input.bpmRound, applicationType: matched[0].applicationType }
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
export const buildNativeApprovalProgress = (scope: ApprovalProgressScope, facts: {
  fileId: string; needTraining?: boolean; status?: string; controlledTime?: string | null; distributedTime?: string | null; trainingRecordAvailable?: boolean
}, tasks: ApprovalProgressTask[]): ApprovalProgressStage[] => {
  if (scope.applicationType === 'LEGACY' || scope.fileId !== facts.fileId) throw new Error('原生审批进度上下文无效')
  const actualTasks = flattenTasks(tasks)
  if (actualTasks.some(task => !task.id || task.processInstanceId !== scope.bpmRound)) throw new Error('审批任务不属于当前BPM轮次')
  const stages: Array<[string, string]> = [['MATRIX_REVIEW', '会签'], ['MATRIX_APPROVAL', '批准']]
  if (scope.applicationType !== 'OBSOLETE') {
    if (typeof facts.needTraining !== 'boolean') throw new Error('本次申请培训选择事实未记录')
    if (facts.needTraining) stages.push(['APPLICANT_TRAINING_RECORD', '培训（文控上传线下记录）'])
    stages.push(['DOC_CONTROL_REVIEW', '文控审核'], ['CONTROLLED', '受控'], ['DISTRIBUTED', '文控下发'])
  }
  return stages.map(([stageCode, stageName], index) => {
    const rows = actualTasks.filter(task => task.taskDefinitionKey === stageCode)
    const trainingNode = stageCode === 'APPLICANT_TRAINING_RECORD'
    const trainingCompleted = trainingNode && facts.trainingRecordAvailable === true
      && (['PENDING_DOC_CONTROL_REVIEW', 'READY_TO_PUBLISH', 'FINALIZING', 'FINALIZATION_FAILED', 'CONTROLLED_PENDING_EFFECTIVE', 'ACTIVE'].includes(facts.status || '') || Boolean(facts.controlledTime))
    const completedFact = stageCode === 'CONTROLLED' ? Boolean(facts.controlledTime) : stageCode === 'DISTRIBUTED' ? Boolean(facts.distributedTime) : trainingCompleted
    const factNode = trainingNode || stageCode === 'CONTROLLED' || stageCode === 'DISTRIBUTED'
    const totalCount = factNode ? 1 : rows.length
    const approvedCount = factNode ? Number(completedFact) : rows.filter(task => task.status === 2).length
    const rejectedCount = rows.filter(task => task.status === 3).length
    const runningCount = rows.filter(task => task.status === 1 || task.status === 7).length
    const isCompleted = totalCount > 0 && approvedCount === totalCount
    const isCurrent = runningCount > 0 || (trainingNode && facts.status === 'PENDING_APPLICANT_TRAINING_RECORD')
      || (!isCompleted && stageCode === 'CONTROLLED' && ['READY_TO_PUBLISH', 'FINALIZING', 'FINALIZATION_FAILED'].includes(facts.status || ''))
      || (!isCompleted && stageCode === 'DISTRIBUTED' && Boolean(facts.controlledTime))
    return { stageCode, stageName, stageOrder: index + 1, totalCount, approvedCount, rejectedCount, runningCount,
      waitingCount: Math.max(totalCount - approvedCount - rejectedCount - runningCount, 0), completionText: `${approvedCount}/${totalCount}`,
      sameLayerHint: isCompleted ? '本阶段已完成' : isCurrent ? '当前阶段待处理' : '待进入本阶段', isCurrent, isCompleted, isPending: !isCurrent && !isCompleted }
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
  return ({ MATRIX_APPROVAL: '批准人', DOC_CONTROL_REVIEW: '文控审核人', APPLICANT_TRAINING_RECORD: '文控培训记录' } as Record<string, string>)[key || ''] || '签名职责未记录'
}
