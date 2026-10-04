export type WorkflowId = string | number
import type { ArrangementCommand } from '../relations/arrangement-form'
export type { ArrangementCommand } from '../relations/arrangement-form'

export interface WorkflowDistributionScope {
  departmentId: WorkflowId
  distributionMedium: 'PUBLIC_FOLDER' | 'PAPER'
  recipientUserIds: WorkflowId[]
}

export interface WorkflowRecipientOption {
  id: WorkflowId
  name: string
}

export interface PendingWorkflowDistributionFile {
  id: WorkflowId
  fileNumber: string
  title?: string
  versionNo: string
  effectiveDate: string
  controlledTime: string
  activatedTime?: string
  distributedTime?: string
  status: 'ACTIVE' | 'CONTROLLED_PENDING_EFFECTIVE'
  distributionReminderStage: 'OVERDUE' | 'DUE' | 'UPCOMING' | 'FUTURE'
}

export interface SignoffAssignment {
  taskId: string
  assigneeUserId: WorkflowId
  password: string
  reason: string
  relationArrangements?: ArrangementCommand[]
}

export const isWorkflowId = (id: WorkflowId): boolean => typeof id === 'number'
  ? Number.isSafeInteger(id) && id > 0
  : /^[1-9]\d*$/.test(id) && BigInt(id) <= 9223372036854775807n

export const trainingUploadSession = (fileId: WorkflowId, processInstanceId: string, clientSessionId: string): string => {
  if (!isWorkflowId(fileId) || !processInstanceId || processInstanceId !== processInstanceId.trim()
    || processInstanceId.length > 64 || !clientSessionId?.trim() || clientSessionId !== clientSessionId.trim()) {
    throw new Error('培训上传需要当前文件、真实流程轮次及本次会话')
  }
  const session = `dcc-training:${fileId}:${processInstanceId.length}:${processInstanceId}:${clientSessionId}`
  if (session.length > 128) throw new Error('培训上传会话超过正式长度限制，请缩短本次会话标识')
  return session
}

export const submitSignoffAssignment = async (
  request: SignoffAssignment,
  save: (request: SignoffAssignment) => Promise<boolean>
): Promise<{ success: boolean; error?: string }> => {
  if (!request.taskId.trim() || !isWorkflowId(request.assigneeUserId)) return { success: false, error: '请选择本部门会签人，可以选择本人' }
  if (!request.password.trim()) return { success: false, error: '请输入当前账号密码完成指派签名' }
  if (!request.reason.trim()) return { success: false, error: '请填写指派意见' }
  const arrangements = request.relationArrangements || []
  const masters = new Set<string>()
  for (const row of arrangements) {
    if (!row || !isWorkflowId(row.relatedMasterId) || !isWorkflowId(row.assigneeUserId)
      || !/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(row.dueAt)
      || masters.has(String(row.relatedMasterId))) return { success: false, error: '请核对整改安排的关联文件、责任人和期限' }
    masters.add(String(row.relatedMasterId))
  }
  try {
    const saved = await save({ ...request, reason: request.reason.trim(), relationArrangements: arrangements.map(row => ({ ...row })) })
    if (saved !== true) return { success: false, error: '指派未返回成功保存结果，请核对正式指派事实' }
    return { success: true }
  } catch (error) {
    return { success: false, error: error instanceof Error ? error.message : '指派签名失败，请核对签名授权及本部门办理资格' }
  }
}

export interface LifecycleFacts {
  controlledTime?: string
  effectiveDate: string
  activatedTime?: string
  distributedTime?: string
  status: string
}

export const lifecyclePresentation = (facts: LifecycleFacts) => ({
  controlled: Boolean(facts.controlledTime),
  executable: facts.status === 'ACTIVE' && Boolean(facts.activatedTime),
  distributionCompleted: Boolean(facts.distributedTime),
  canDistribute: Boolean(facts.controlledTime) && !facts.distributedTime
    && ['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(facts.status),
  warning: facts.status === 'CONTROLLED_PENDING_EFFECTIVE'
    ? `${facts.effectiveDate} 生效，生效前不得执行` : '',
  reworkHint: facts.status === 'REJECTED' ? '查看签名驳回原因，修改后重新提交；新一轮须重新指派、会签和批准' : ''
})
