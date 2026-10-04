import type { ControlledFileApplicationRound } from '@/api/dcc/controlledFile/applicationRead'

export interface ApplicationApprovalRead {
  contextKey: string
  processInstanceId?: string
  taskId?: string
  error?: string
}
const exactText = (value: unknown, label: string): string => {
  if (typeof value !== 'string' || !value || value !== value.trim()) throw new Error(`${label}身份无效`)
  return value
}
const fileIdentity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') ||
      (typeof value === 'number' && !Number.isSafeInteger(value))) throw new Error('申请文件身份缺失或精度损失')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n) throw new Error('申请文件身份无效')
  return id
}
/** Read context only; this does not confer approval, signature or evidence access rights. */
export const validateApplicationApprovalRead = (
  processInstanceId: unknown,
  taskId: unknown,
  tasks: unknown
): { processInstanceId: string; taskId: string } => {
  const bpm = exactText(processInstanceId, '办理轮次')
  const requestedTask = taskId === '' || taskId == null ? '' : exactText(taskId, '办理任务')
  if (!Array.isArray(tasks) || !tasks.length) throw new Error('办理轮次缺少真实任务读取证据')
  const taskIds = new Set<string>()
  const visit = (rows: unknown[]) => rows.forEach(value => {
    if (!value || typeof value !== 'object') throw new Error('办理任务响应不完整')
    const task = value as { id?: unknown; processInstanceId?: unknown; children?: unknown }
    if (task.processInstanceId !== bpm) throw new Error('办理任务与请求流程轮次不一致')
    taskIds.add(exactText(task.id, '办理任务'))
    if (task.children != null) {
      if (!Array.isArray(task.children)) throw new Error('办理子任务响应不完整')
      visit(task.children)
    }
  })
  visit(tasks)
  if (requestedTask && !taskIds.has(requestedTask)) throw new Error('指定办理任务不属于本次真实流程轮次')
  return { processInstanceId: bpm, taskId: requestedTask }
}
export const resolveApplicationRoundSelection = (input: {
  fileId: unknown
  requestedFileId?: unknown
  nativeBpmRound?: unknown
  requestedBpmRound?: unknown
  requestedTaskId?: unknown
  contextKey: string
  readContext?: ApplicationApprovalRead
}): { primaryBpmRound: string | null; blockedReason: string; lockPrimaryRound: boolean } => {
  try {
    const fileId = fileIdentity(input.fileId)
    if (input.requestedFileId != null && fileIdentity(input.requestedFileId) !== fileId)
      return { primaryBpmRound: null, blockedReason: '正在核验当前所选文件身份', lockPrimaryRound: true }
    const hasBpm = input.requestedBpmRound != null && input.requestedBpmRound !== ''
    const hasTask = input.requestedTaskId != null && input.requestedTaskId !== ''
    const native = input.nativeBpmRound == null || input.nativeBpmRound === '' ? null : exactText(input.nativeBpmRound, '本版本BPM')
    if (!hasBpm && !hasTask) return { primaryBpmRound: native, blockedReason: '', lockPrimaryRound: false }
    const requested = hasBpm ? exactText(input.requestedBpmRound, '办理轮次') : native
    if (!requested) throw new Error('指定办理任务缺少实际流程轮次')
    const taskId = hasTask ? exactText(input.requestedTaskId, '办理任务') : ''
    const read = input.readContext
    if (!read || read.contextKey !== input.contextKey) return { primaryBpmRound: null, blockedReason: '正在核验实际办理轮次', lockPrimaryRound: true }
    if (read.error) throw new Error(read.error)
    if (read.processInstanceId !== requested || read.taskId !== taskId) throw new Error('已读取办理任务与当前轮次上下文不一致')
    return { primaryBpmRound: requested, blockedReason: '', lockPrimaryRound: true }
  } catch (cause) {
    return { primaryBpmRound: null, blockedReason: cause instanceof Error ? cause.message : String(cause), lockPrimaryRound: true }
  }
}
/** Mapping values have already passed the API/BPM guard; verify selection identity again at the component boundary. */
export const validateApplicationRoundMappings = (fileId: unknown, rows: unknown): ControlledFileApplicationRound[] => {
  const id = fileIdentity(fileId)
  if (!Array.isArray(rows)) throw new Error('正式申请轮次映射缺失')
  const keys = new Set<string>()
  return rows.map(value => {
    if (!value || typeof value !== 'object') throw new Error('正式申请轮次映射缺失')
    const row = value as ControlledFileApplicationRound
    if (fileIdentity(row.controlledFileId) !== id || !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(row.applicationType) ||
        !Number.isSafeInteger(row.attributeRound) || row.attributeRound < 1) throw new Error('正式申请映射与所选文件身份不一致')
    const bpm = exactText(row.bpmRound, '申请轮次')
    if (keys.has(bpm)) throw new Error('同一BPM对应重复或多个申请轮次映射')
    keys.add(bpm)
    return { ...row, controlledFileId: id }
  })
}
