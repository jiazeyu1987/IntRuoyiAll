import request from '@/config/axios'

export interface WorkingTypeStorageRecoveryPreview {
  controlledFileId: string
  masterId: string
  fileNumber: string
  projectId: string
  projectFolderId: string
  categoryId: string
  taxonomyId: string
  currentTypePath: string
  originalProofId: string
  originalProofSha256: string
  observedSha256: string
  sourceSha256: string
  matrixDigest: string
  oldDirectoryId: string
  targetBaseDirectoryId: string
  targetDirectoryId: string | null
  recovered: boolean
  recoveryEvidenceId: string | null
}

export interface WorkingTypeStorageRecoveryCommand {
  expectedOriginalProofId: string
  expectedOriginalProofSha256: string
  expectedObservedSha256: string
  requestId: string
  changeReason: string
  confirmed: true
}

const identity = (id: unknown): string => {
  if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id) || BigInt(id) > 9223372036854775807n)
    throw new Error('类型存储恢复身份无效，请重新核对')
  return id
}
const endpoint = (id: string) => `/dcc/controlled-files/${identity(id)}/working-type-storage-recovery`
const validate = (value: WorkingTypeStorageRecoveryPreview, fileId: string) => {
  if (!value || identity(value.controlledFileId) !== fileId) throw new Error('类型存储恢复返回的文件身份不一致')
  for (const id of [value.masterId, value.projectId, value.projectFolderId, value.categoryId, value.taxonomyId,
    value.originalProofId, value.oldDirectoryId, value.targetBaseDirectoryId]) identity(id)
  if (value.targetDirectoryId !== null) identity(value.targetDirectoryId)
  if (value.recoveryEvidenceId !== null) identity(value.recoveryEvidenceId)
  for (const digest of [value.originalProofSha256, value.observedSha256, value.sourceSha256, value.matrixDigest])
    if (typeof digest !== 'string' || !/^[a-f0-9]{64}$/.test(digest)) throw new Error('类型存储恢复证明摘要无效')
  if (!value.fileNumber?.trim() || !value.currentTypePath?.trim() || typeof value.recovered !== 'boolean'
    || (value.recovered && (!value.targetDirectoryId || !value.recoveryEvidenceId))
    || (!value.recovered && value.recoveryEvidenceId !== null)) throw new Error('类型存储恢复事实不完整')
  return value
}

export const previewWorkingTypeStorageRecovery = async (fileId: string): Promise<WorkingTypeStorageRecoveryPreview> =>
  validate(await request.get({ url: `${endpoint(fileId)}/preview`, ignoreErrorMessage: true }), fileId)

export const confirmWorkingTypeStorageRecovery = async (fileId: string, data: WorkingTypeStorageRecoveryCommand): Promise<WorkingTypeStorageRecoveryPreview> => {
  const result = validate(await request.post({ url: endpoint(fileId), data, ignoreErrorMessage: true }), fileId)
  if (!result.recovered || result.originalProofId !== data.expectedOriginalProofId || result.originalProofSha256 !== data.expectedOriginalProofSha256)
    throw new Error('类型存储恢复回执与原证明不一致，请重新核对')
  return result
}
