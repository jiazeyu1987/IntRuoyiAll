import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import type { ControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import { detailIdentity } from './relation-contract'
import type { ReferenceUsageSummary } from '../relations/reference-usage'

interface BrowseReaders {
  getPermissions: (id: string) => Promise<ControlledFileRelationPermissions>
  getUsage: (id: string) => Promise<ReferenceUsageSummary>
}
export interface BrowseMetadata {
  selected: ControlledFileRelationPermissions
  latest?: ControlledFileRelationPermissions
  referenceProjectCount: number
}

// Read the current controlled identity separately; never replace the selected body or its evidence.
export const loadBrowseMetadata = async (
  file: ControlledFileVO,
  tenantId: string,
  readers: BrowseReaders
): Promise<BrowseMetadata> => {
  const id = detailIdentity(file.id),
    tenant = detailIdentity(tenantId)
  const selected = await readers.getPermissions(id)
  if (
    selected.controlledFileId !== id ||
    selected.tenantId !== tenant ||
    selected.masterId !== detailIdentity(file.masterId) ||
    selected.projectId !== detailIdentity(file.dccProjectCodeId) ||
    selected.versionNo !== file.versionNo ||
    selected.projectFolderId !==
      (file.projectFolderId == null ? null : detailIdentity(file.projectFolderId))
  )
    throw new Error('浏览基础信息与所选文件、项目文件夹或租户不一致')
  if (typeof selected.hasCurrentControlledSource !== 'boolean')
    throw new Error('当前受控版本事实缺失')
  let latest: ControlledFileRelationPermissions | undefined
  if (selected.hasCurrentControlledSource) {
    if (selected.latestControlledFileId == null)
      throw new Error('最新受控版本不可见或正式版本身份未提供')
    const latestId = detailIdentity(selected.latestControlledFileId)
    latest = latestId === id ? selected : await readers.getPermissions(latestId)
    if (
      latest.controlledFileId !== latestId ||
      latest.tenantId !== tenant ||
      latest.masterId !== selected.masterId
    )
      throw new Error('最新受控版本身份与所选文件不一致')
    if (latest.controlled !== true) throw new Error('最新受控版本的正式受控状态不一致')
  }
  const usage = await readers.getUsage(id)
  if (
    detailIdentity(usage.tenantId) !== tenant ||
    detailIdentity(usage.sourceControlledFileId) !== id ||
    detailIdentity(usage.masterId) !== selected.masterId ||
    !Number.isSafeInteger(usage.referenceProjectCount) ||
    usage.referenceProjectCount < 0 ||
    usage.referenced !== usage.referenceProjectCount > 0
  )
    throw new Error('浏览引用项目计数与所选文件不一致')
  return { selected, latest, referenceProjectCount: usage.referenceProjectCount }
}
