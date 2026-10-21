import { readDccBodyReadContextRoute, type DccBodyReadContext } from '@/api/dcc/controlledFile/bodyReadContext'

export interface DetailReadSelection {
  bodyReadContext?: Readonly<DccBodyReadContext>
  expectedVersionNo?: string
  bpmRound?: string
  taskId?: string
  applicationType?: 'UPLOAD' | 'REVISION' | 'OBSOLETE'
  lockRound: boolean
}
const text = (value: unknown): string => {
  if (typeof value !== 'string' || !value || value !== value.trim() || value.length > 64 || /[\u0000-\u001f\u007f]/.test(value))
    throw new Error('详情来源的申请或任务身份无效')
  return value
}
/** Navigation identity only. Access is independently checked by the selected-detail endpoint. */
export const resolveDetailReadSelection = (
  fileId: string | number,
  query: Record<string, unknown>
): DetailReadSelection => {
  const normalized = readDccBodyReadContextRoute(fileId, query)
  const explicit = Object.hasOwn(query, 'bodyReadContext')
  const bodyRound = 'bpmRound' in normalized ? normalized.bpmRound : undefined
  const bodyTask = 'taskId' in normalized ? normalized.taskId : undefined
  const bodyType = 'applicationType' in normalized ? normalized.applicationType : undefined
  const routeRound = Object.hasOwn(query, 'processInstanceId') ? text(query.processInstanceId) : undefined
  const routeTask = Object.hasOwn(query, 'taskId') ? text(query.taskId) : undefined
  const routeType = Object.hasOwn(query, 'applicationType') ? text(query.applicationType) : undefined
  if (routeType && !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(routeType))
    throw new Error('详情来源的申请类型无效')
  if (explicit && (routeRound !== undefined || routeTask !== undefined || routeType !== undefined))
    throw new Error('详情来源与正文的版本、申请或任务上下文冲突')
  const bpmRound = bodyRound ?? routeRound
  const taskId = bodyTask ?? routeTask
  if ((taskId || routeType) && !bpmRound) throw new Error('指定详情申请或任务缺少精确申请轮次')
  return {
    bodyReadContext: explicit ? normalized : undefined,
    expectedVersionNo: normalized.intent === 'DAILY' ? undefined : normalized.expectedVersionNo,
    bpmRound, taskId, applicationType: (bodyType ?? routeType) as DetailReadSelection['applicationType'],
    lockRound: Boolean(bpmRound || taskId)
  }
}
