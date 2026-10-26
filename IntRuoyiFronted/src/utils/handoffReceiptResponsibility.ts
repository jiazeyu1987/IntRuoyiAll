import type { HandoffTask } from '@/api/mes/pro/handoff'

const id = (value: unknown): string => {
  const text = typeof value === 'string' ? value : typeof value === 'number' && Number.isSafeInteger(value) ? String(value) : ''
  if (!/^[1-9][0-9]*$/.test(text) || BigInt(text) > 9223372036854775807n) throw new Error('原任务责任快照账号或业务编号无效')
  return text
}

/** The task creation snapshot remains authoritative when route roles change. */
export const readHandoffReceiptCandidates = (task: HandoffTask): string[] => {
  if (!task.responsibilitySnapshotJson) throw new Error('原任务缺少正式责任候选快照')
  const snapshot = JSON.parse(task.responsibilitySnapshotJson)
  if (!snapshot || snapshot.identityDomain !== 'SYSTEM_USER' || snapshot.sourceType !== task.sourceType
    || ['activeOrderId', 'sourceId', 'roundId'].some(key => id(snapshot[key]) !== id(task[key as keyof HandoffTask]))) {
    throw new Error('原任务责任候选快照与所选任务身份不一致')
  }
  if (!Array.isArray(snapshot.userIds) || !snapshot.userIds.length) throw new Error('原任务责任候选快照没有正式账号')
  const candidates = snapshot.userIds.map(id)
  if (new Set(candidates).size !== candidates.length) throw new Error('原任务责任候选账号重复')
  return candidates
}
