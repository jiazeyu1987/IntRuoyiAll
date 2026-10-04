import { referenceIdentity } from './project-reference-contract'

export interface ReferenceUsageRow {
  referenceId: string; projectId: string; projectName: string; folderId: string; folderName: string
  masterId: string; selectedControlledFileId: string; fileNumber: string; fileName: string; versionNo: string
  status: string; controlled: boolean; pendingEffect: boolean; executable: boolean; canPreview: boolean
}
export interface ReferenceUsagePage {
  tenantId: string; sourceControlledFileId: string; masterId: string
  referenceProjectCount: number; visibleReferenceProjectCount: number; total: number
  detailsRestricted: boolean; list: ReferenceUsageRow[]
}
export interface ReferenceUsageContext { tenantId: string; sourceControlledFileId: string; masterId: string }
const record = (value: unknown): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('引用使用明细响应缺失')
  return value as Record<string, unknown>
}
const count = (value: unknown): number => {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw new Error('引用使用明细计数不合法')
  return value
}
const text = (value: unknown): string => {
  if (typeof value !== 'string' || !value.trim()) throw new Error('引用使用明细正式名称或版本缺失')
  return value
}
const flag = (value: unknown): boolean => {
  if (typeof value !== 'boolean') throw new Error('引用使用明细权限或生命周期事实缺失')
  return value
}
export const validateReferenceUsagePage = (value: unknown, expected: ReferenceUsageContext,
  pageNo: number, pageSize: number): ReferenceUsagePage => {
  if (!Number.isSafeInteger(pageNo) || pageNo < 1 || !Number.isSafeInteger(pageSize) || pageSize < 1 || pageSize > 200)
    throw new Error('引用使用明细分页无效')
  const input = record(value), tenantId = referenceIdentity(input.tenantId)
  const sourceControlledFileId = referenceIdentity(input.sourceControlledFileId), masterId = referenceIdentity(input.masterId)
  if (tenantId !== referenceIdentity(expected.tenantId) || sourceControlledFileId !== referenceIdentity(expected.sourceControlledFileId)
    || masterId !== referenceIdentity(expected.masterId)) throw new Error('引用使用明细与所选文件、租户或Master不一致')
  const referenceProjectCount = count(input.referenceProjectCount), visibleReferenceProjectCount = count(input.visibleReferenceProjectCount)
  const total = count(input.total), detailsRestricted = flag(input.detailsRestricted)
  const offset = (pageNo - 1) * pageSize
  if (!Number.isSafeInteger(offset) || visibleReferenceProjectCount > referenceProjectCount || visibleReferenceProjectCount > total
    || (visibleReferenceProjectCount === 0) !== (total === 0) || detailsRestricted !== (visibleReferenceProjectCount < referenceProjectCount)
    || !Array.isArray(input.list) || input.list.length !== Math.min(pageSize, Math.max(total - offset, 0)))
    throw new Error('引用使用明细授权计数与服务器分页不一致')
  const ids = new Set<string>(), folders = new Set<string>()
  const list = input.list.map(value => {
    const row = record(value), referenceId = referenceIdentity(row.referenceId), projectId = referenceIdentity(row.projectId)
    const folderId = referenceIdentity(row.folderId), selectedControlledFileId = referenceIdentity(row.selectedControlledFileId)
    const folder = JSON.stringify([projectId, folderId])
    const status = text(row.status), controlled = flag(row.controlled), pendingEffect = flag(row.pendingEffect)
    const executable = flag(row.executable), canPreview = flag(row.canPreview)
    if (referenceIdentity(row.masterId) !== masterId || ids.has(referenceId) || folders.has(folder)
      || controlled !== ['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(status)
      || pendingEffect !== (status === 'CONTROLLED_PENDING_EFFECTIVE') || pendingEffect && executable
      || executable && (!controlled || status !== 'ACTIVE')) throw new Error('引用使用明细固定版本、身份或状态不一致')
    ids.add(referenceId); folders.add(folder)
    return { referenceId, projectId, folderId, selectedControlledFileId, masterId, status, controlled, pendingEffect, executable, canPreview,
      projectName: text(row.projectName), folderName: text(row.folderName), fileNumber: text(row.fileNumber), fileName: text(row.fileName), versionNo: text(row.versionNo) }
  })
  if (new Set(list.map(row => row.projectId)).size > visibleReferenceProjectCount)
    throw new Error('本页引用项目与授权项目计数不一致')
  return { tenantId, sourceControlledFileId, masterId, referenceProjectCount, visibleReferenceProjectCount, total, detailsRestricted, list }
}
