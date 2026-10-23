export interface DccDistributionReadIdentity {
  controlledFileId: string
  distributionId: string
  recipientId: string
  versionNo: string
  approvalProcessInstanceId: string
}

const exactId = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value) || BigInt(value) > 9223372036854775807n)
    throw new Error('下发通知缺少精确文件或收件义务身份')
  return value
}
const exactText = (value: unknown, limit: number): string => {
  if (typeof value !== 'string' || !value.trim() || value !== value.trim() || value.length > limit)
    throw new Error('下发通知缺少正式版本或申请轮次')
  return value
}
export const readDccDistributionNotification = (params: Record<string, unknown>): DccDistributionReadIdentity => ({
  controlledFileId: exactId(params.controlledFileId), distributionId: exactId(params.distributionId),
  recipientId: exactId(params.recipientId), versionNo: exactText(params.version, 128),
  approvalProcessInstanceId: exactText(params.approvalProcessInstanceId, 64)
})
export const readDccDistributionRoute = (fileId: unknown, query: Record<string, unknown>): DccDistributionReadIdentity => {
  if (query.mode !== 'distribution-read' || query.viewer !== '1' || query.from !== 'notification')
    throw new Error('下发查阅入口身份不完整')
  return readDccDistributionNotification({ controlledFileId: fileId, distributionId: query.distributionId,
    recipientId: query.recipientId, version: query.version, approvalProcessInstanceId: query.approvalProcessInstanceId })
}
export const dccDistributionReadLocation = (identity: DccDistributionReadIdentity) => ({
  name: 'DccControlledFileDetail', params: { id: identity.controlledFileId },
  query: { viewer: '1', from: 'notification', mode: 'distribution-read', distributionId: identity.distributionId,
    recipientId: identity.recipientId, version: identity.versionNo, approvalProcessInstanceId: identity.approvalProcessInstanceId }
})
