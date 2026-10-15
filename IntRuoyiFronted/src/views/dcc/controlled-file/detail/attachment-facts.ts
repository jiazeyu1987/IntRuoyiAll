import type { ControlledFileAttachmentVO } from '@/api/dcc/controlledFile/workflow'

const storedText = (value?: string | null): string =>
  typeof value === 'string' && value.trim() ? value : '未记录'

export const attachmentPurposeText = (attachment: ControlledFileAttachmentVO): string => {
  if (attachment.attachmentPurpose === 'TRAINING_RECORD') return '培训记录单'
  if (attachment.attachmentPurpose === 'ATTACHMENT') return '普通附件'
  return attachment.attachmentPurpose?.trim() || '用途未记录'
}

export const attachmentUploaderText = (attachment: ControlledFileAttachmentVO): string => {
  const nickname = attachment.uploadedNicknameSnapshot?.trim()
  const username = attachment.uploadedUsernameSnapshot?.trim()
  if (nickname && username) return `${nickname}（${username}）`
  return nickname || username || '未记录'
}

export const attachmentRoundText = (attachment: ControlledFileAttachmentVO): string =>
  storedText(attachment.applicationProcessInstanceId)

export const attachmentUploadTimeText = (attachment: ControlledFileAttachmentVO): string =>
  storedText(attachment.uploadedAt)

const identity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') ||
    (typeof value === 'number' && !Number.isSafeInteger(value)) ||
    !/^[1-9][0-9]*$/.test(String(value)) || BigInt(String(value)) > 9223372036854775807n) {
    throw new Error('附件或对应文件版本身份无效，请重新读取')
  }
  return String(value)
}

export const assertAttachmentBelongsToVersion = (
  attachment: ControlledFileAttachmentVO,
  controlledFileId: string | number
): void => {
  identity(attachment.attachmentId)
  if (identity(attachment.controlledFileId) !== identity(controlledFileId)) {
    throw new Error('附件不属于当前查看版本，请重新读取')
  }
}
