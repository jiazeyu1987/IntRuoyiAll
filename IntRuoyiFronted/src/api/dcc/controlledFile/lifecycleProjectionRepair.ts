import request from '@/config/axios'

export interface LifecycleProjectionRepairPreview {
  fileId: string
  masterId: string | null
  versionRefId: string | null
  tenantId: string
  versionNo: string
  dccStatus: string
  canonicalStatus: string | null
  domainStatus: string | null
  processInstanceId: string | null
  controlledTime: string | null
  activatedTime: string | null
  approvedTime: string | null
  publishedTime: string | null
  effectiveDate: string | null
  publishedFileId: string | null
  stampedFileId: string | null
  signatureIds: string[]
  latestControlledFileId: string | null
  currentActiveControlledFileId: string | null
  sourceFactsHash: string
  preimageHash: string
  canRepair: boolean
  expectedTargetStatus: string | null
  expectedActions: string[]
}
export interface LifecycleProjectionRepairCommand {
  masterId: string
  versionRefId: string
  processInstanceId: string
  expectedCanonicalStatus: 'FINALIZING'
  sourceFactsHash: string
  preimageHash: string
  reason: string
  idempotencyKey: string
}
export interface LifecycleProjectionRepairReceipt {
  fileId: string
  masterId: string
  versionRefId: string
  processInstanceId: string
  status: 'REPAIRED' | 'REPLAY'
  canonicalStatus: string
  domainStatus: string
  sourceFactsHash: string
  preimageHash: string
  idempotencyKey: string
  auditEventId: string
  repairedAt: string
}
const identity = (value: unknown, nullable = false, allowZero = false): string | null => {
  if (nullable && value === null) return null
  if (typeof value !== 'string' || !(allowZero ? /^(0|[1-9]\d*)$/ : /^[1-9]\d*$/).test(value))
    throw new Error('生命周期投影身份必须为正式十进制字符串')
  if (BigInt(value) > 9223372036854775807n) throw new Error('生命周期投影身份超过正式 Long 范围')
  return value
}
const text = (value: unknown, nullable = false): string | null => {
  if (nullable && value === null) return null
  if (typeof value !== 'string' || !value.trim() || value !== value.trim())
    throw new Error('生命周期投影文字事实缺失或格式不正确')
  return value
}
const hash = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[a-f0-9]{64}$/i.test(value))
    throw new Error('生命周期投影摘要格式不正确')
  return value
}
const record = (value: unknown): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('生命周期投影响应不是正式对象')
  return value as Record<string, unknown>
}
const time = (value: unknown, nullable = true): string | null => {
  if (nullable && value === null) return null
  const result = text(value) as string
  if (!/^\d{4}-\d{2}-\d{2}(T| )\d{2}:\d{2}:\d{2}/.test(result) || Number.isNaN(Date.parse(result)))
    throw new Error('生命周期投影时间事实格式不正确')
  return result
}
const endpoint = (id: string) => `/dcc/controlled-file/workflow-lifecycle/${identity(id)}`
export const getLifecycleProjectionRepairPreview = async (fileId: string): Promise<LifecycleProjectionRepairPreview> => {
  const result = record(await request.get({ url: `${endpoint(fileId)}/lifecycle-projection-repair-preview`, ignoreErrorMessage: true }))
  if (identity(result.fileId) !== fileId) throw new Error('生命周期投影文件身份不一致')
  identity(result.tenantId, false, true)
  for (const name of ['masterId', 'versionRefId', 'publishedFileId', 'stampedFileId', 'latestControlledFileId', 'currentActiveControlledFileId']) identity(result[name], true)
  for (const name of ['versionNo', 'dccStatus']) text(result[name])
  for (const name of ['canonicalStatus', 'domainStatus', 'processInstanceId', 'expectedTargetStatus']) text(result[name], true)
  for (const name of ['controlledTime', 'activatedTime', 'approvedTime', 'publishedTime']) time(result[name])
  if (result.effectiveDate !== null && (typeof result.effectiveDate !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(result.effectiveDate))) throw new Error('生命周期投影生效日期格式不正确')
  hash(result.sourceFactsHash); hash(result.preimageHash)
  if (typeof result.canRepair !== 'boolean' || !Array.isArray(result.signatureIds) || !Array.isArray(result.expectedActions)) throw new Error('生命周期投影资格或证据列表未记录')
  result.signatureIds.forEach(id => identity(id)); result.expectedActions.forEach(action => text(action))
  if (result.canRepair && (result.canonicalStatus !== 'FINALIZING' || !result.masterId || !result.versionRefId || !result.processInstanceId || !['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(String(result.expectedTargetStatus)))) throw new Error('生命周期投影维护资格与正式事实不一致')
  return result as unknown as LifecycleProjectionRepairPreview
}
export const repairLifecycleProjection = async (fileId: string, command: LifecycleProjectionRepairCommand): Promise<LifecycleProjectionRepairReceipt> => {
  identity(command.masterId); identity(command.versionRefId); text(command.processInstanceId)
  hash(command.sourceFactsHash); hash(command.preimageHash); text(command.reason); text(command.idempotencyKey)
  if (command.reason.length > 500) throw new Error('生命周期投影修复原因最多500字符')
  if (command.expectedCanonicalStatus !== 'FINALIZING') throw new Error('生命周期投影原状态不正确')
  const data: LifecycleProjectionRepairCommand = {
    masterId: command.masterId, versionRefId: command.versionRefId, processInstanceId: command.processInstanceId,
    expectedCanonicalStatus: 'FINALIZING', sourceFactsHash: command.sourceFactsHash, preimageHash: command.preimageHash,
    reason: command.reason, idempotencyKey: command.idempotencyKey
  }
  const result = record(await request.post({ url: `${endpoint(fileId)}/repair-lifecycle-projection`, data, ignoreErrorMessage: true }))
  for (const name of ['fileId', 'masterId', 'versionRefId', 'auditEventId']) identity(result[name])
  time(result.repairedAt, false)
  text(result.canonicalStatus); text(result.domainStatus)
  if (!['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(String(result.canonicalStatus)) || result.domainStatus !== result.canonicalStatus) throw new Error('生命周期投影维护结果状态不一致')
  if (result.fileId !== fileId || result.masterId !== command.masterId || result.versionRefId !== command.versionRefId || result.processInstanceId !== command.processInstanceId || result.sourceFactsHash !== command.sourceFactsHash || result.preimageHash !== command.preimageHash || result.idempotencyKey !== command.idempotencyKey || !['REPAIRED', 'REPLAY'].includes(String(result.status))) throw new Error('生命周期投影维护结果与原请求不一致')
  return result as unknown as LifecycleProjectionRepairReceipt
}
