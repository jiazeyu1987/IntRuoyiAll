import type { DirectoryNode } from './DccFileSelector.vue'
import type { FileCandidate } from './selector-state'
import type { ControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
export interface ReferenceDirectoryTree {
  id: string | number
  projectCodeId: string | number
  name: string
  active: boolean
  children: ReferenceDirectoryTree[]
}

export interface ReferenceRow {
  reference: { id: string; projectId: string; folderId: string; masterId: string; selectedControlledFileId: string; createdBy: string }
  selectedVersion: { tenantId: string; controlledFileId: string; masterId: string; projectId: string; fileName: string; fileNumber: string; versionNo: string; status: string; controlled: boolean; pendingEffect: boolean; executable: boolean;
    canPreview?: boolean; projectName?: string; folderName?: string; projectFolderId?: string | null }
  referenceProjectCount: number
  sourceProjectName: string
}
export interface ReferenceBrowserRow extends ReferenceRow { selectedVersion: FileCandidate }
export interface ReferenceContext { tenantId: string; projectId: string; folderId: string }
export const referenceIdentity = (value: unknown): string => {
  if (typeof value === 'number' && (!Number.isSafeInteger(value) || value < 1)) throw new Error('引用身份发生精度损失')
  if (typeof value !== 'number' && typeof value !== 'string') throw new Error('引用身份缺失')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n) throw new Error('引用身份不合法')
  return id
}
export const mapReferenceRows = (result: unknown, context: ReferenceContext, projectNames: Record<string, string>): ReferenceRow[] => {
  if (!Array.isArray(result)) throw new Error('引用列表响应缺失')
  const tenant = referenceIdentity(context.tenantId), project = referenceIdentity(context.projectId), folder = referenceIdentity(context.folderId)
  const references = new Set<string>(), masters = new Set<string>()
  return result.map(value => {
    const row = record(value), reference = record(row.reference), version = record(row.selectedVersion)
    const id = referenceIdentity(reference.id), masterId = referenceIdentity(reference.masterId), selectedId = referenceIdentity(reference.selectedControlledFileId)
    if (references.has(id) || masters.has(masterId) || referenceIdentity(reference.projectId) !== project || referenceIdentity(reference.folderId) !== folder
      || referenceIdentity(version.tenantId) !== tenant || referenceIdentity(version.masterId) !== masterId || referenceIdentity(version.controlledFileId) !== selectedId) {
      throw new Error('引用返回的目录、租户、所选版本或稳定身份不一致')
    }
    references.add(id); masters.add(masterId)
    const sourceProject = referenceIdentity(version.projectId), status = text(version.status)
    const controlled = boolean(version.controlled), pendingEffect = boolean(version.pendingEffect), executable = boolean(version.executable)
    if (pendingEffect && executable || pendingEffect !== (controlled && status === 'CONTROLLED_PENDING_EFFECTIVE')
      || controlled !== ['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(status) || executable && (!controlled || status !== 'ACTIVE')) {
      throw new Error('引用所选版本状态不一致')
    }
    if (typeof row.referenceProjectCount !== 'number' || !Number.isSafeInteger(row.referenceProjectCount) || row.referenceProjectCount < 1) throw new Error('引用项目数无效')
    return {
      reference: { id, projectId: project, folderId: folder, masterId, selectedControlledFileId: selectedId, createdBy: referenceIdentity(reference.createdBy) },
      selectedVersion: { tenantId: tenant, controlledFileId: selectedId, masterId, projectId: sourceProject, fileName: text(version.fileName),
        fileNumber: text(version.fileNumber), versionNo: text(version.versionNo), status, controlled, pendingEffect, executable },
      referenceProjectCount: row.referenceProjectCount, sourceProjectName: text(projectNames[sourceProject])
    }
  })
}

export const bindReferencePermissions = (row: ReferenceRow, projection: ControlledFileRelationPermissions): ReferenceBrowserRow => {
  const selected = row.selectedVersion
  if (!projection || referenceIdentity(projection.controlledFileId) !== row.reference.selectedControlledFileId
    || referenceIdentity(selected.controlledFileId) !== row.reference.selectedControlledFileId
    || referenceIdentity(projection.tenantId) !== selected.tenantId
    || referenceIdentity(projection.masterId) !== row.reference.masterId
    || referenceIdentity(projection.masterId) !== selected.masterId
    || referenceIdentity(projection.projectId) !== selected.projectId
    || projection.fileName !== selected.fileName || projection.fileNumber !== selected.fileNumber
    || projection.versionNo !== selected.versionNo || projection.status !== selected.status
    || projection.controlled !== selected.controlled || projection.pendingEffect !== selected.pendingEffect
    || projection.executable !== selected.executable || typeof projection.canPreview !== 'boolean'
    || !projection.projectName?.trim() || (projection.projectFolderId != null) !== Boolean(projection.projectFolderName))
    throw new Error('引用权限投影与固定所选版本、租户、项目或文件事实不一致')
  return { ...row, reference: { ...row.reference }, sourceProjectName: projection.projectName,
    selectedVersion: { ...selected, projectName: projection.projectName,
      projectFolderId: projection.projectFolderId, folderName: projection.projectFolderName ?? '未记录',
      canPreview: projection.canPreview } }
}

export const assertReferenceTraceIdentity = (row: ReferenceRow, response: unknown): void => {
  const detail = record(response), selected = row.selectedVersion
  if (referenceIdentity(detail.id) !== row.reference.selectedControlledFileId
    || referenceIdentity(detail.masterId) !== row.reference.masterId
    || referenceIdentity(detail.dccProjectCodeId) !== selected.projectId
    || detail.fileName !== selected.fileName || detail.fileNumber !== selected.fileNumber || detail.versionNo !== selected.versionNo
    || (detail.tenantId != null && referenceIdentity(detail.tenantId) !== selected.tenantId))
    throw new Error('只读追溯响应与固定引用文件或版本不一致')
}
/** Presentation only: official B leader service remains the authority on every write. No role/OWNER bypass. */
export const isEnabledTargetProjectLeader = (actor: { id: unknown; status: unknown }, project: { projectLeaderUserId: unknown }): boolean => {
  if (project.projectLeaderUserId == null || actor.status !== 0) return false
  return referenceIdentity(actor.id) === referenceIdentity(project.projectLeaderUserId)
}
/** Input is B's already validated tree, from getProjectFolders + buildProjectFolderTree. */
export const mapReferenceDirectoryNodes = (tenantId: string, projectId: string, tree: ReferenceDirectoryTree[]): DirectoryNode[] => {
  const tenant = referenceIdentity(tenantId), project = referenceIdentity(projectId)
  const map = (nodes: ReferenceDirectoryTree[]): DirectoryNode[] => nodes.filter(node => node.active).map(node => {
    if (referenceIdentity(node.projectCodeId) !== project) throw new Error('项目目录归属不一致')
    const folder = referenceIdentity(node.id)
    return { key: JSON.stringify([tenant, project, folder]), name: text(node.name), projectId: project, folderId: folder, children: map(node.children) }
  })
  return map(tree)
}
const record = (value: unknown): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('引用对象缺失')
  return value as Record<string, unknown>
}
const text = (value: unknown): string => { if (typeof value !== 'string' || !value.trim()) throw new Error('引用正式展示信息缺失'); return value }
const boolean = (value: unknown): boolean => { if (typeof value !== 'boolean') throw new Error('引用正式状态缺失'); return value }
