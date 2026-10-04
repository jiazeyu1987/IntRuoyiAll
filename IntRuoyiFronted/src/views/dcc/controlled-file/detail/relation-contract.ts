import type { FileCandidate } from '../relations/selector-state'
export type DetailIdentity = string | number
export interface DetailRelationFile {
  id: DetailIdentity
  masterId?: DetailIdentity | null
  dccProjectCodeId?: DetailIdentity | null
  projectFolderId?: DetailIdentity | null
  versionNo: string
  title: string
  fileName?: string
  sourceOriginalFileName?: string | null
  fileNumber?: string
}
export const detailIdentity = (value: unknown): string => {
  if ((typeof value !== 'number' && typeof value !== 'string') || (typeof value === 'number' && !Number.isSafeInteger(value))) throw new Error('详情身份缺失或精度损失')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n) throw new Error('详情身份无效')
  return id
}
export const assertRelationSource = (selected: DetailRelationFile, actual: DetailRelationFile, sourceId: string): boolean => {
  if (detailIdentity(actual.id) !== detailIdentity(sourceId) || detailIdentity(actual.masterId) !== detailIdentity(selected.masterId) ||
      detailIdentity(actual.dccProjectCodeId) !== detailIdentity(selected.dccProjectCodeId)) throw new Error('当前关联来源的稳定文件身份不一致')
  return detailIdentity(selected.id) === sourceId
}
export interface HistoricalRelationSnapshot {
  relationId: DetailIdentity
  controlledFileId: DetailIdentity
  masterId: DetailIdentity
  projectCodeId: DetailIdentity
  fileName: string
  fileNumber: string
  versionNo: string
  status?: string | null
}
type Navigation = Pick<FileCandidate, 'tenantId' | 'controlledFileId' | 'masterId' | 'projectId' | 'projectName' | 'folderName' | 'canPreview'>
/** Only current permission/navigation comes from exact target metadata; business fields stay frozen. */
export const historicalRelationCandidate = (snapshot: HistoricalRelationSnapshot, navigation: Navigation): FileCandidate & { relationId: string; projectCodeId: string } => {
  detailIdentity(snapshot.relationId); detailIdentity(snapshot.projectCodeId)
  if (detailIdentity(snapshot.controlledFileId) !== navigation.controlledFileId || detailIdentity(snapshot.masterId) !== navigation.masterId ||
      !snapshot.fileName || !snapshot.fileNumber || !snapshot.versionNo) throw new Error('历史关联快照身份或展示事实缺失')
  return { ...snapshot, ...navigation, relationId: detailIdentity(snapshot.relationId), projectCodeId: detailIdentity(snapshot.projectCodeId),
    fileName: snapshot.fileName, fileNumber: snapshot.fileNumber, versionNo: snapshot.versionNo,
    status: snapshot.status || 'HISTORICAL_SNAPSHOT', controlled: false, pendingEffect: false, executable: false }
}
