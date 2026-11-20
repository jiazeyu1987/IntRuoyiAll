import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import type { DistributionRecipientFact } from '../workflow/distribution-facts'

// The backend uses the same exact file edit scope for EDIT and DISTRIBUTE.
export const canReadControlledDistributionReceipts = (
  file: Pick<ControlledFileVO, 'canEditFile' | 'processInstanceId'> | undefined,
  hasMenuPermission: boolean
) => hasMenuPermission && file?.canEditFile === true && Boolean(file.processInstanceId)

export const getControlledDetailTrainingAttachments = (file: ControlledFileVO) =>
  (file.attachments || []).filter(row => row.attachmentPurpose === 'TRAINING_RECORD'
    && String(row.controlledFileId) === String(file.id))

/** Only exact distribution and recipient identities may replace a summary receipt. */
export const buildControlledDetailDistributionRows = (
  file: ControlledFileVO, receipts: DistributionRecipientFact[] = []
) => {
  const used = new Set<DistributionRecipientFact>()
  const rows = (file.distributionStatuses || []).flatMap(distribution => {
    const recipients = distribution.recipients?.length
      ? distribution.recipients
      : (distribution.recipientUserIds || []).map(userId => ({ id: undefined, userId, readAt: undefined, acknowledgedAt: undefined }))
    return (recipients.length ? recipients : [undefined]).map(recipient => {
      const receipt = recipient?.id == null ? undefined : receipts.find(row =>
        String(row.distributionId) === String(distribution.id)
        && String(row.recipientId) === String(recipient.id)
        && String(row.userId) === String(recipient.userId)
        && String(row.departmentId) === String(distribution.departmentId))
      if (receipt) used.add(receipt)
      // A confirmed recipient receipt takes precedence over the earlier department summary.
      const acknowledgedAt = receipt?.acknowledgedAt ?? recipient?.acknowledgedAt
      return {
        key: `${distribution.id}:${recipient?.id ?? recipient?.userId ?? 'department'}`,
        departmentId: distribution.departmentId as number | string,
        departmentName: receipt?.departmentName,
        userId: recipient?.userId as number | string | undefined,
        userName: receipt?.signerNameSnapshot || receipt?.currentUserName,
        medium: distribution.distributionMedium,
        readAt: receipt?.readAt ?? recipient?.readAt,
        acknowledgedAt,
        status: acknowledgedAt ? 'ACKNOWLEDGED' : recipient ? 'PENDING' : distribution.status,
        recoveredAt: distribution.recoveredAt,
        messageStatus: receipt?.messageStatus,
        errorMessage: receipt?.errorMessage
      }
    })
  })
  for (const receipt of receipts) {
    if (used.has(receipt)) continue
    const distribution = file.distributionStatuses?.find(row => String(row.id) === String(receipt.distributionId))
    rows.push({
      key: `receipt:${receipt.distributionId}:${receipt.recipientId}`,
      departmentId: receipt.departmentId, departmentName: receipt.departmentName,
      userId: receipt.userId, userName: receipt.signerNameSnapshot || receipt.currentUserName,
      medium: distribution?.distributionMedium, readAt: receipt.readAt,
      acknowledgedAt: receipt.acknowledgedAt, status: receipt.acknowledgedAt ? 'ACKNOWLEDGED' : 'PENDING',
      recoveredAt: distribution?.recoveredAt, messageStatus: receipt.messageStatus, errorMessage: receipt.errorMessage
    })
  }
  return rows
}
