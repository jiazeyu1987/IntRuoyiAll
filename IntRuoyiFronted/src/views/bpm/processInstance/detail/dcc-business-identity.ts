import type { DccBodyReadContext, DccBodyReadApplicationType } from '@/api/dcc/controlledFile/bodyReadContext'

interface DccProcessIdentity {
  id?: unknown
  businessKey?: unknown
  formVariables?: Record<string, unknown>
}

/** Called only after BPM has authorized and returned the real instance/current task. */
export const resolveDccTaskHandlingLocation = (
  instance: DccProcessIdentity | undefined,
  definitionKey: string | undefined,
  todoTask?: { id?: unknown; processInstanceId?: unknown } | null
) => {
  if (!todoTask || !definitionKey || !['dcc-controlled-file-upload', 'dcc-controlled-file-revision'].includes(definitionKey)) return null
  const fileId = resolveDccControlledFileBusinessId(instance, definitionKey)
  const bpm = instance?.id, task = todoTask.id
  if (!fileId || typeof bpm !== 'string' || !bpm || bpm !== bpm.trim() || bpm.length > 64
    || typeof task !== 'string' || !task || task !== task.trim() || task.length > 64
    || todoTask.processInstanceId !== bpm) throw new Error('DCC当前办理任务与真实流程实例身份不一致')
  return { path: `/dcc/controlled-file/detail/${fileId}`,
    query: { handling: 'approval', from: 'approval-center', processInstanceId: bpm, taskId: task } }
}

const nativeDefinitions = new Set([
  'dcc-controlled-file-upload',
  'dcc-controlled-file-revision',
  'dcc-controlled-file-approval'
])
const obsoleteDefinitions = new Set([
  'dcc-controlled-file-obsolete',
  'dcc-controlled-file-obsolete-approval'
])

const exactFileId = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) return ''
  return BigInt(value) <= 9223372036854775807n ? value : ''
}

/** Native file workflows and Form Center actions have different formal business identities. */
export const resolveDccControlledFileBusinessId = (
  instance: DccProcessIdentity | undefined,
  definitionKey: string | undefined
): string => {
  if (!instance || !definitionKey) return ''
  if (nativeDefinitions.has(definitionKey)) return exactFileId(instance.businessKey)
  if (!obsoleteDefinitions.has(definitionKey)
    || typeof instance.businessKey !== 'string'
    || !/^FORM_ACTION:\S+$/.test(instance.businessKey)) return ''
  const facts = instance.formVariables
  if (!facts || facts.systemCode !== 'DCC' || facts.objectType !== 'CONTROLLED_FILE'
    || facts.actionCode !== 'OBSOLETE' || typeof facts.objectVersion !== 'string'
    || !facts.objectVersion || facts.objectVersion !== facts.objectVersion.trim()) return ''
  return exactFileId(facts.objectId)
}

/** Retain the caller's selected task/round. These facts select server checks, never grant permission. */
export const resolveDccApprovalBodyReadContext = (
  instance: DccProcessIdentity | undefined,
  definitionKey: string | undefined,
  file: { id?: unknown; versionNo?: unknown; changeType?: unknown },
  selectedTaskId?: string,
  todoTask?: { id?: unknown; processInstanceId?: unknown } | null
): DccBodyReadContext => {
  const fileId = resolveDccControlledFileBusinessId(instance, definitionKey)
  const bpmRound = instance?.id, expectedVersionNo = file.versionNo
  const exactText = (value: unknown, limit: number): string => {
    if (typeof value !== 'string' || !value || value !== value.trim() || value.length > limit
      || /[\u0000-\u001f\u007f]/.test(value)) throw new Error('审批正文缺少精确任务、版本或轮次身份')
    return value
  }
  if (!fileId || String(file.id) !== fileId || (typeof file.id === 'number' && !Number.isSafeInteger(file.id)))
    throw new Error('审批正文所选文件与真实流程业务对象不一致')
  let applicationType: DccBodyReadApplicationType
  switch (definitionKey) {
    case 'dcc-controlled-file-upload': applicationType = 'UPLOAD'; break
    case 'dcc-controlled-file-revision': applicationType = 'REVISION'; break
    case 'dcc-controlled-file-approval':
      if (file.changeType !== 'NEW' && file.changeType !== 'REVISION') throw new Error('历史审批正文缺少正式申请类型')
      applicationType = file.changeType === 'NEW' ? 'UPLOAD' : 'REVISION'; break
    case 'dcc-controlled-file-obsolete':
    case 'dcc-controlled-file-obsolete-approval': applicationType = 'OBSOLETE'; break
    default: throw new Error('审批正文流程定义不受支持')
  }
  const selected = { controlledFileId: fileId, expectedVersionNo: exactText(expectedVersionNo, 128),
    applicationType, bpmRound: exactText(bpmRound, 64) }
  if (applicationType === 'OBSOLETE' && instance?.formVariables?.objectVersion !== selected.expectedVersionNo)
    throw new Error('作废审批正文版本与冻结对象版本不一致')
  if (todoTask && (todoTask.processInstanceId !== selected.bpmRound || !exactText(todoTask.id, 64)))
    throw new Error('审批正文待办任务与精确流程轮次不一致')
  const taskId = selectedTaskId !== undefined ? exactText(selectedTaskId, 64)
    : todoTask ? exactText(todoTask.id, 64) : undefined
  if (todoTask && taskId !== undefined && taskId === todoTask.id) return { intent: 'APPROVAL_TASK', ...selected, taskId }
  return { intent: 'APPROVAL_HISTORY', ...selected, ...(taskId ? { taskId } : {}) }
}
