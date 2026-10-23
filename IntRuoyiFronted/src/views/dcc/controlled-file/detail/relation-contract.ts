import type { FileCandidate } from '../relations/selector-state'
import type {
  ControlledFileApplicationEvidence,
  DccApplicationType,
  RelationRoundEvidence,
  RelationStageSelection
} from '@/api/dcc/controlledFile/applicationRead'
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
  if (
    (typeof value !== 'number' && typeof value !== 'string') ||
    (typeof value === 'number' && !Number.isSafeInteger(value))
  )
    throw new Error('详情身份缺失或精度损失')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n)
    throw new Error('详情身份无效')
  return id
}
export const assertRelationSource = (
  selected: DetailRelationFile,
  actual: DetailRelationFile,
  sourceId: string
): boolean => {
  if (
    detailIdentity(actual.id) !== detailIdentity(sourceId) ||
    detailIdentity(selected.id) !== detailIdentity(sourceId) ||
    detailIdentity(actual.masterId) !== detailIdentity(selected.masterId) ||
    detailIdentity(actual.dccProjectCodeId) !== detailIdentity(selected.dccProjectCodeId)
  )
    throw new Error('当前关联来源的稳定文件身份不一致')
  if (!selected.versionNo || actual.versionNo !== selected.versionNo)
    throw new Error('关联来源与所选冻结版本不一致')
  return true
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
type Navigation = Pick<
  FileCandidate,
  | 'tenantId'
  | 'controlledFileId'
  | 'masterId'
  | 'projectId'
  | 'projectName'
  | 'folderName'
  | 'canPreview'
>
export type DetailRelationRow = FileCandidate & {
  relationId?: string
  projectCodeId: string
  origin?: string
  supplemental?: boolean
}
/** Only current permission/navigation comes from exact target metadata; business fields stay frozen. */
export const historicalRelationCandidate = (
  snapshot: HistoricalRelationSnapshot,
  navigation: Navigation
): FileCandidate & { relationId: string; projectCodeId: string } => {
  detailIdentity(snapshot.relationId)
  detailIdentity(snapshot.projectCodeId)
  if (
    detailIdentity(snapshot.controlledFileId) !== navigation.controlledFileId ||
    detailIdentity(snapshot.masterId) !== navigation.masterId ||
    !snapshot.fileName ||
    !snapshot.fileNumber ||
    !snapshot.versionNo
  )
    throw new Error('历史关联快照身份或展示事实缺失')
  return {
    ...snapshot,
    ...navigation,
    relationId: detailIdentity(snapshot.relationId),
    projectCodeId: detailIdentity(snapshot.projectCodeId),
    fileName: snapshot.fileName,
    fileNumber: snapshot.fileNumber,
    versionNo: snapshot.versionNo,
    status: snapshot.status || 'HISTORICAL_SNAPSHOT',
    controlled: false,
    pendingEffect: false,
    executable: false
  }
}

export interface DetailRelationApplicationSelection {
  controlledFileId: DetailIdentity
  applicationType: DccApplicationType
  bpmRound: string
  attributeRound: number
}
export const assertRelationApplicationSelection = (
  file: DetailRelationFile,
  selection: DetailRelationApplicationSelection
): void => {
  if (
    detailIdentity(selection.controlledFileId) !== detailIdentity(file.id) ||
    !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(selection.applicationType) ||
    typeof selection.bpmRound !== 'string' ||
    !selection.bpmRound.trim() ||
    selection.bpmRound !== selection.bpmRound.trim() ||
    !Number.isSafeInteger(selection.attributeRound) ||
    selection.attributeRound < 1
  )
    throw new Error('关联必须绑定所选文件的正式申请轮次')
}
export const assertRelationApplicationEvidence = (
  file: DetailRelationFile,
  selection: DetailRelationApplicationSelection,
  tenantId: string,
  evidence: ControlledFileApplicationEvidence
): RelationRoundEvidence | null => {
  assertRelationApplicationSelection(file, selection)
  if (
    !evidence ||
    detailIdentity(evidence.controlledFileId) !== detailIdentity(file.id) ||
    evidence.versionNo !== file.versionNo ||
    evidence.applicationType !== selection.applicationType ||
    evidence.bpmRound !== selection.bpmRound ||
    evidence.attributeRound !== selection.attributeRound
  )
    throw new Error('关联申请证据与所选版本或正式申请轮次不一致')
  const relations = evidence.relationEvidence
  if (relations === undefined) throw new Error('关联申请证据响应缺少正式记录状态')
  if (relations === null) return null
  if (
    detailIdentity(relations.tenantId) !== detailIdentity(tenantId) ||
    detailIdentity(relations.fileId) !== detailIdentity(file.id) ||
    detailIdentity(relations.masterId) !== detailIdentity(file.masterId) ||
    detailIdentity(relations.projectId) !== detailIdentity(file.dccProjectCodeId) ||
    relations.applicationType !== selection.applicationType ||
    relations.bpmRound !== selection.bpmRound ||
    relations.attributeRound !== selection.attributeRound ||
    typeof relations.recorded !== 'boolean' ||
    !Array.isArray(relations.originalUploadSelections) ||
    !Array.isArray(relations.completeSelections) ||
    !Array.isArray(relations.stages)
  )
    throw new Error('关联阶段证据与所选租户、版本或申请轮次不一致')
  return relations
}
export const applicationRelationCandidate = (
  selection: RelationStageSelection,
  navigation: Navigation
): DetailRelationRow => {
  if (
    detailIdentity(selection.selectedControlledFileId) !== navigation.controlledFileId ||
    detailIdentity(selection.relatedMasterId) !== navigation.masterId ||
    detailIdentity(selection.projectId) !== navigation.projectId ||
    !selection.fileName ||
    !selection.fileNumber ||
    !selection.versionNo ||
    !['UPLOAD', 'SUPPLEMENT'].includes(selection.origin) ||
    typeof selection.supplemental !== 'boolean'
  )
    throw new Error('申请关联冻结成员身份或事实缺失')
  return {
    ...navigation,
    fileName: selection.fileName,
    fileNumber: selection.fileNumber,
    versionNo: selection.versionNo,
    projectCodeId: detailIdentity(selection.projectId),
    origin: selection.origin,
    supplemental: selection.supplemental,
    status: 'HISTORICAL_SNAPSHOT',
    controlled: false,
    pendingEffect: false,
    executable: false
  }
}
