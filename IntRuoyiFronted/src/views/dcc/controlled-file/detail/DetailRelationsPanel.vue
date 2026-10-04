<template>
  <ContentWrap v-loading="loading" data-testid="dcc-detail-relations">
    <p class="font-600">文件关联</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-tabs v-if="selectedSource" v-model="tab">
      <el-tab-pane label="当前关联（最新受控版本）" name="current">
        <el-alert v-if="directoryError" :title="directoryError" type="error" :closable="false" />
        <p v-if="currentSource">当前关联来源：{{ currentSource.fileName }} · {{ currentSource.versionNo }} · 文件 #{{ currentSource.controlledFileId }}</p>
        <el-alert
          v-if="currentSource && currentSource.controlledFileId !== selectedSource.controlledFileId"
          title="当前关联属于同一文件的另一版本；所选历史版本只读。" type="info" :closable="false" />
        <DccFileRelations
          v-if="currentSource && current" :key="currentSource.contextKey" mode="current"
          :source="currentSource" :directories="directories" :can-edit="canEdit" :model-value="[]"
          :auto-open-editor="props.autoOpenEditor && !autoEditorOpened" @editor-opened="autoEditorOpened = true"
          :load-page="loadPage" :load-current="loadCurrent" :load-history="loadHistory"
          :persist-current="persistCurrent" :open-preview="openPreview" @changed="load" />
      </el-tab-pane>
      <el-tab-pane label="本版本审批关联快照" name="history">
        <DccFileRelations
          mode="history" :source="selectedSource" :directories="[]" :can-edit="false" :model-value="[]"
          :load-page="loadPage" :load-current="loadCurrent" :load-history="loadHistory"
          :persist-current="persistCurrent" :open-preview="openPreview" />
      </el-tab-pane>
    </el-tabs>
    <el-dialog v-model="previewVisible" title="关联文件正文" width="88vw" destroy-on-close>
      <ProtectedPdfViewer v-if="previewId" :controlled-file-id="previewId" title="关联文件正文" />
    </el-dialog>
  </ContentWrap>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { getProjectFolders } from '@/api/dcc/controlledFile/projectAttributes'
import { getControlledFileRelationPermissions, loadDccSelectorPage } from '@/api/dcc/controlledFile/applicationRead'
import type { ControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import { listCurrentRelations, listHistoricalRelations, replaceCurrentRelations } from '@/api/dcc/controlledFile/relations'
import { buildProjectFolderTree } from '../basic-data/components/project-folder-tree'
import { mapReferenceDirectoryNodes } from '../relations/project-reference-contract'
import DccFileRelations from '../relations/DccFileRelations.vue'
import type { SelectorSource, DirectoryNode } from '../relations/DccFileSelector.vue'
import type { FileCandidate, SelectorQuery } from '../relations/selector-state'
import type { CurrentRelationContext, RelationSaveCommand } from '../relations/relation-editor-state'
import ProtectedPdfViewer from '../view/index.vue'
import { detailIdentity, assertRelationSource, historicalRelationCandidate } from './relation-contract'
import type { DetailRelationFile, HistoricalRelationSnapshot } from './relation-contract'
const props = defineProps<{ file: DetailRelationFile; allowEdit: boolean; autoOpenEditor?: boolean }>()
const loading = ref(false), error = ref(''), directoryError = ref(''), tab = ref('current')
const current = ref<CurrentRelationContext>(), selectedSource = ref<SelectorSource>(), currentSource = ref<SelectorSource>()
const directories = ref<DirectoryNode[]>([]), canEdit = ref(false), previewId = ref('')
const autoEditorOpened = ref(false)
let generation = 0
const previewVisible = computed({ get: () => Boolean(previewId.value), set: value => { if (!value) previewId.value = '' } })
const tenant = () => detailIdentity(getVisitTenantId() ?? getTenantId())
const assertMetadata = (metadata: ControlledFileRelationPermissions) => {
  if (metadata.tenantId !== tenant()) throw new Error('关联名称投影租户不一致')
  return metadata
}
const identityFile = (metadata: ControlledFileRelationPermissions): DetailRelationFile => ({
  id: metadata.controlledFileId, masterId: metadata.masterId, dccProjectCodeId: metadata.projectId,
  projectFolderId: metadata.projectFolderId, versionNo: metadata.versionNo, title: metadata.fileName, fileNumber: metadata.fileNumber
})
const source = (metadata: ControlledFileRelationPermissions): SelectorSource => ({
  contextKey: JSON.stringify([tenant(), metadata.masterId, metadata.controlledFileId, generation]),
  tenantId: metadata.tenantId, controlledFileId: metadata.controlledFileId, masterId: metadata.masterId,
  projectId: metadata.projectId, folderId: metadata.projectFolderId ?? undefined,
  projectName: metadata.projectName, folderName: metadata.projectFolderName || '未记录',
  fileName: metadata.fileName, fileNumber: metadata.fileNumber, versionNo: metadata.versionNo
})
type CurrentRow = Omit<FileCandidate, 'projectName' | 'folderName' | 'canPreview'>
const currentCandidate = async (row: CurrentRow): Promise<FileCandidate> => {
  const id = detailIdentity(row.controlledFileId), metadata = assertMetadata(await getControlledFileRelationPermissions(id))
  if (detailIdentity(row.tenantId) !== tenant() || detailIdentity(row.masterId) !== metadata.masterId || detailIdentity(row.projectId) !== metadata.projectId ||
      row.versionNo !== metadata.versionNo || row.fileNumber !== metadata.fileNumber || row.fileName !== metadata.fileName || row.status !== metadata.status ||
      row.controlled !== metadata.controlled || row.pendingEffect !== metadata.pendingEffect || row.executable !== metadata.executable)
    throw new Error('当前关联的正式版本、稳定身份或生命周期事实不一致')
  return { ...row, tenantId: metadata.tenantId, controlledFileId: id, masterId: metadata.masterId, projectId: metadata.projectId,
    projectName: metadata.projectName, projectFolderId: metadata.projectFolderId, folderName: metadata.projectFolderName || '未记录', canPreview: metadata.canPreview }
}
const load = async () => {
  const token = ++generation, file = props.file, selectedId = detailIdentity(file.id)
  loading.value = true; error.value = ''; directoryError.value = ''; directories.value = []; current.value = undefined; currentSource.value = undefined; canEdit.value = false; selectedSource.value = undefined; previewId.value = ''
  try {
    const selected = assertMetadata(await getControlledFileRelationPermissions(selectedId))
    assertRelationSource(file, identityFile(selected), selectedId)
    if (token !== generation) return
    // Establish historical context independently; current relation-set failure must not erase it.
    selectedSource.value = source(selected)
    const result = await listCurrentRelations(selectedId) as { sourceControlledFileId: string; rowVersion: string; files: CurrentRow[] }
    if (!result || !Array.isArray(result.files) || !/^(0|[1-9][0-9]*)$/.test(result.rowVersion)) throw new Error('当前关联上下文缺失')
    const actualId = detailIdentity(result.sourceControlledFileId)
    const actual = actualId === selectedId ? selected : assertMetadata(await getControlledFileRelationPermissions(actualId))
    const selectedIsCurrent = assertRelationSource(file, identityFile(actual), actualId)
    const files = await Promise.all(result.files.map(currentCandidate))
    if (new Set(files.map(row => row.masterId)).size !== files.length || files.some(row => row.masterId === selected.masterId)) throw new Error('当前关联包含重复或自身关系')
    if (token !== generation) return
    currentSource.value = source(actual)
    current.value = { sourceControlledFileId: actualId, rowVersion: result.rowVersion, files }
    if (props.allowEdit && selectedIsCurrent && actual.canEdit) {
      try {
        const folders = await getProjectFolders(actual.projectId)
        if (token !== generation) return
        if (actual.projectFolderId != null && !folders.some(row => detailIdentity(row.id) === actual.projectFolderId)) throw new Error('当前源文件的正式文件夹不存在，无法编辑关联')
        directories.value = [{ key: `project:${actual.projectId}`, name: actual.projectName, projectId: actual.projectId,
          children: mapReferenceDirectoryNodes(tenant(), actual.projectId, buildProjectFolderTree(actual.projectId, folders)) }]
        canEdit.value = true
      } catch (cause) { if (token === generation) directoryError.value = cause instanceof Error ? cause.message : String(cause) }
    }
  } catch (cause) { if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === generation) loading.value = false }
}
const loadCurrent = async (id: string) => {
  if (!current.value || id !== current.value.sourceControlledFileId) throw new Error('当前关联来源已变化，请刷新')
  return current.value
}
const loadHistory = async (id: string): Promise<FileCandidate[]> => {
  if (id !== detailIdentity(props.file.id)) throw new Error('历史关联必须绑定所选版本')
  const snapshots = await listHistoricalRelations(id) as HistoricalRelationSnapshot[]
  if (!Array.isArray(snapshots)) throw new Error('历史关联响应缺失')
  return Promise.all(snapshots.map(async snapshot => {
    const targetId = detailIdentity(snapshot.controlledFileId)
    const target = assertMetadata(await getControlledFileRelationPermissions(targetId))
    if (target.controlledFileId !== targetId || target.masterId !== detailIdentity(snapshot.masterId)) throw new Error('历史关联所选正文身份不一致')
    return historicalRelationCandidate(snapshot, { tenantId: target.tenantId, controlledFileId: targetId, masterId: target.masterId,
      projectId: target.projectId, projectName: target.projectName, folderName: '历史未记录', canPreview: target.canPreview })
  }))
}
const loadPage = (query: SelectorQuery) => loadDccSelectorPage(tenant(), query.folderId == null ? { keyword: query.keyword, pageNo: query.pageNo, pageSize: query.pageSize } : query)
const persistCurrent = async (id: string, command: RelationSaveCommand) => {
  if (!canEdit.value || !current.value || id !== current.value.sourceControlledFileId || id !== detailIdentity(props.file.id)) throw new Error('所选版本关联只读或当前来源已变化')
  return replaceCurrentRelations(id, command.selectedFileIds, command.expectedMasterIds, command.expectedVersion, command.idempotencyKey, command.reason)
}
const openPreview = async (row: FileCandidate) => { if (!row.canPreview) throw new Error('无此关联版本正文权限'); previewId.value = detailIdentity(row.controlledFileId) }
watch(() => [String(props.file.id), props.allowEdit], load, { immediate: true })
watch(() => String(props.file.id), () => { autoEditorOpened.value = false }, { flush: 'sync' })
onBeforeUnmount(() => { generation++ })
</script>
