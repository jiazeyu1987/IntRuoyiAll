<template>
  <section class="project-browser" data-testid="dcc-project-browser">
    <aside>
      <LibraryRoleAccessHint v-if="libraryScope" :scope="libraryScope" />
      <div v-if="!libraryScope" class="search"><el-input v-model="state.projectKeyword" placeholder="项目名称或编码" clearable @keyup.enter="searchProjects" /><el-button :loading="state.projectLoading" @click="searchProjects">查找项目</el-button></div>
      <el-alert v-if="state.projectError" :title="state.projectError" type="error" :closable="false" />
      <el-table v-if="!libraryScope" :data="state.projects" v-loading="state.projectLoading" highlight-current-row @row-click="selectProject">
        <el-table-column prop="projectName" label="项目" /><el-table-column prop="projectCode" label="编码" />
      </el-table>
      <el-pagination v-if="!libraryScope" v-model:current-page="state.projectPage" :page-size="20" :total="state.projectsTotal" layout="total, prev, next" @current-change="state.loadProjects()" />
      <h4>{{ state.project ? state.project.projectName : '项目文件夹' }}</h4>
      <div v-if="state.project && !libraryScope" data-testid="dcc-project-folder-maintenance">
        <el-button v-hasPermi="['dcc:project-code:update']" :disabled="state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('create')">新增文件夹</el-button>
        <el-button v-hasPermi="['dcc:project-code:update']" :disabled="!state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('update')">编辑文件夹</el-button>
        <el-button v-hasPermi="['dcc:project-code:update']" :disabled="!state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('delete')">删除文件夹</el-button>
      </div>
      <div v-if="state.project && libraryScope && libraryCanEdit" data-testid="dcc-library-folder-maintenance">
        <el-button :disabled="state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('create')">新增文件夹</el-button>
        <el-button :disabled="!state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('update')">编辑文件夹</el-button>
        <el-button :disabled="!state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('delete')">删除文件夹</el-button>
      </div>
      <el-alert v-if="state.folderError" :title="state.folderError" type="error" :closable="false" />
      <el-tree :key="state.project?.id" v-loading="state.folderLoading" :data="state.directories" node-key="key" :props="{ label: 'name', children: 'children' }" highlight-current default-expand-all @node-click="selectFolder" />
      <el-empty v-if="state.project && !state.folderLoading && !state.folderError && !state.directories.length" :description="libraryScope ? '文件库尚无文件夹' : '项目尚无文件夹'" />
    </aside>
    <main>
      <el-tabs :model-value="state.scope" @tab-change="changeScope"><el-tab-pane v-if="!libraryScope" label="全局文件" name="global" /><el-tab-pane :label="libraryScope ? '当前目录' : '当前项目'" name="project" :disabled="!state.project" /><el-tab-pane :label="libraryScope ? '文件夹' : '项目文件夹'" name="directory" /></el-tabs>
      <p v-if="state.scope === 'project'">{{ state.project?.projectName }} / 全部文件夹</p>
      <p v-if="state.scope === 'directory'">{{ state.project?.projectName }} / {{ state.folder?.name || '请选择文件夹' }}</p>
      <div class="search"><el-input v-model="state.keyword" placeholder="文件名称、编号或项目名称" clearable @keyup.enter="state.search(state.scope)" /><el-button :loading="state.loading" @click="state.search(state.scope)">搜索</el-button><el-button v-if="!libraryScope && state.scope === 'directory' && state.folder" @click="operationVisible = true">选择文件操作</el-button></div>
      <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
      <el-alert v-if="state.actorError" :title="state.actorError" type="error" :closable="false" />
      <DccProjectReferences v-if="!libraryScope && state.scope === 'directory' && state.project && state.folder"
          :key="state.contextKey" :context-key="state.contextKey" :selection-context-key="listContextKey" :project-id="state.project.id" :folder-id="state.folder.folderId!" :folder-name="state.folder.name" :is-project-leader="state.canReference" :selector-source="selectorSource" :directories="state.directories" :load-page="loadPage" :open-preview="preview"
          :open-reference-preview="previewReference" :open-reference-trace="traceReference"
          @show-usage="openUsage($event.selectedVersion)"
          :load-references="loadReferences" :create-references="createReferences" :cancel-reference="cancelReference"
          @changed="state.referencesChanged()" @refreshed="state.referencesChanged()" @saved-read-failure="state.referencesChanged()">
        <template #default="{ rows, loading, busy, readingReferenceId, selectedReference, selectReference, readReference }">
          <ProjectFolderFileTable :files="state.rows" :references="rows" :usage="state.usage" :keyword="state.appliedKeyword" :version-view="state.versionView"
            :loading="state.loading || loading || busy" :can-reference="state.canReference" :selected-reference="selectedReference" :reading-reference-id="readingReferenceId"
            @select-reference="selectReference" @read-reference="readReference" @preview="previewRow" @relations="openRelationsRow" @operation="openOperationRow" @usage="openUsage" />
        </template>
      </DccProjectReferences>
      <ProjectFolderFileTable v-else :files="state.rows" :references="[]" :usage="state.usage" :keyword="state.appliedKeyword" :version-view="state.versionView"
        :loading="state.loading" :can-reference="false" :read-only="Boolean(libraryScope)" :can-download="Boolean(libraryScope) && libraryCanEdit" @download="downloadLibraryFile" @preview="previewRow" @relations="openRelationsRow" @operation="openOperationRow" @usage="openUsage" />
      <p v-if="!libraryScope && state.scope === 'directory' && state.folder">当前文件夹引用与文件同表显示；引用固定所选受控版本。分页仅统计本项目文件。</p>
      <el-pagination v-model:current-page="state.pageNo" :page-size="state.pageSize" :total="state.total" layout="total, prev, pager, next" @current-change="state.loadFiles()" />
    </main>
    <DccFileSelector v-if="state.folder" v-model="operationVisible" :source="selectorSource" purpose="operation" :directories="state.directories" :selected="[]" :load-page="loadPage" :open-preview="preview" :persist="openOperation" />
    <ProjectFileRelationsDialog v-if="relationsVisible && relationsSource" v-model="relationsVisible" :source="relationsSource" :context-key="listContextKey" />
    <DccReferenceUsageDialog v-if="usageVisible && usageSource" v-model="usageVisible" :source="usageSource" :context-key="listContextKey" :return-to="route.fullPath" />
    <ProjectFolderEditor v-if="state.project" :key="state.project.id" ref="folderEditor" :library-scope="libraryScope" :library-can-edit="libraryCanEdit" @saved="handleFolderMaintenanceSaved" @deleted="handleFolderMaintenanceSaved" />
  </section>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'
import ProjectFolderEditor from '../basic-data/components/ProjectFolderEditor.vue'
import LibraryRoleAccessHint from './LibraryRoleAccessHint.vue'
import DccFileSelector from '../relations/DccFileSelector.vue'
import DccProjectReferences from '../relations/DccProjectReferences.vue'
import ProjectFolderFileTable from './ProjectFolderFileTable.vue'
import ProjectFileRelationsDialog from './ProjectFileRelationsDialog.vue'
import DccReferenceUsageDialog from '../relations/DccReferenceUsageDialog.vue'
import type { ReferenceUsageSource } from '../relations/DccReferenceUsageDialog.vue'
import type { DirectoryNode, SelectorSource } from '../relations/DccFileSelector.vue'
import type { FileCandidate, SelectorQuery } from '../relations/selector-state'
import { referenceIdentity, type ReferenceRow } from '../relations/project-reference-contract'
import { buildControlledFileViewerPath, buildControlledFileTraceabilityPath } from '../view/presentation'
import { serializeDccBodyReadContext } from '@/api/dcc/controlledFile/bodyReadContext'
import { ProjectBrowserState } from './project-browser'
import request from '@/config/axios'
import { triggerControlledFileVariantDownload } from '@/api/dcc/controlledFile/workflow'
const props=defineProps<{libraryScope?: 'QMS' | 'GENERAL'}>()
const libraryCanEdit=ref(false)
const route = useRoute(), router = useRouter(), user = useUserStore()
const state = reactive(new ProjectBrowserState(referenceIdentity(getVisitTenantId() || getTenantId()), referenceIdentity(user.getUser.id)))
const operationVisible = ref(false)
const folderEditor = ref<InstanceType<typeof ProjectFolderEditor>>()
const openFolderMaintenance = async (mode: 'create' | 'update' | 'delete') => {
  const project = state.project, folder = state.folder
  if (!project || state.folderLoading || state.folderError || !(props.libraryScope ? libraryCanEdit.value : checkPermi(['dcc:project-code:update']))) return
  if (mode !== 'create' && (!folder?.folderId || folder.projectId !== project.id)) return
  if (!folderEditor.value) { state.folderError = '文件夹维护组件尚未加载，请重新打开当前目录'; return }
  if (mode === 'delete') await folderEditor.value.openDelete(project.id, folder!.folderId!)
  else if (mode === 'update') await folderEditor.value.open(project.id, folder!.folderId!)
  else if (props.libraryScope) await folderEditor.value.open(project.id, undefined, folder?.folderId || '0')
  else await folderEditor.value.open(project.id)
}
const handleFolderMaintenanceSaved = async (projectId: number | string) => {
  const current = state.project
  if (!current || current.id !== String(projectId)) return
  if (props.libraryScope) await state.selectLibrary(current)
  else await state.selectProject(current.id)
}
const relationsVisible = ref(false), relationsSource = ref<FileCandidate>()
const usageVisible = ref(false), usageSource = ref<ReferenceUsageSource>()
const listContextKey = computed(() => JSON.stringify([state.contextKey, state.scope, state.appliedKeyword, state.versionView, state.pageNo]))
const openRelationsRow = (row: FileCandidate) => {
  if (state.loading || !state.rows.some(file => file.controlledFileId === row.controlledFileId)) return
  relationsSource.value = JSON.parse(JSON.stringify(row)); relationsVisible.value = true
}
const openUsage = (source: ReferenceUsageSource) => {
  if (state.loading) return
  usageSource.value = JSON.parse(JSON.stringify(source)); usageVisible.value = true
}
watch(() => [listContextKey.value, state.loading], () => { relationsVisible.value = false; relationsSource.value = undefined }, { flush: 'sync' })
watch(() => [listContextKey.value, state.loading], () => { usageVisible.value = false; usageSource.value = undefined }, { flush: 'sync' })
const selectorSource = computed<SelectorSource>(() => ({
  contextKey: state.contextKey, tenantId: state.tenantId, projectId: state.project!.id,
  folderId: state.folder!.folderId, projectName: state.project!.projectName, folderName: state.folder!.name,
  fileName: '请选择文件', fileNumber: '', versionNo: ''
}))
const searchProjects = () => { state.projectPage = 1; return state.loadProjects() }
const selectProject = (project: { id: string }) => { operationVisible.value = false; return state.selectProject(project.id) }
const selectFolder = (node: DirectoryNode) => { operationVisible.value = false; return state.selectFolder(node) }
const changeScope = (scope: string | number) => {
  if (scope !== 'global' && scope !== 'project' && scope !== 'directory') throw new Error('请选择正式项目浏览范围')
  return state.search(scope)
}
const loadPage = (query: SelectorQuery) => state.loadPage(query)
const loadReferences = (project: string, folder: string) => state.loadReferences(project, folder)
const createReferences = (project: string, folder: string, ids: string[], reason: string) => state.createReferences(project, folder, ids, reason)
const cancelReference = (project: string, folder: string, master: string, id: string, reason: string) => state.cancelReference(project, folder, master, id, reason)
const previewReference = async (row: ReferenceRow, isCurrent: () => boolean) => {
  const selected = await state.readReferenceForAction(row)
  if (!isCurrent()) return
  await preview(selected.selectedVersion, serializeDccBodyReadContext(selected.reference.selectedControlledFileId, {
    intent: 'VERSION_HISTORY', controlledFileId: selected.reference.selectedControlledFileId,
    expectedVersionNo: selected.selectedVersion.versionNo
  }))
}
const traceReference = async (row: ReferenceRow, isCurrent: () => boolean) => {
  const selected = await state.readReferenceTrace(row)
  if (!isCurrent()) return
  await router.push(buildControlledFileTraceabilityPath(selected.reference.selectedControlledFileId,
    'browser', route.fullPath, 'trace'))
}
const preview = async (row: FileCandidate, bodyReadContext?: string) => {
  if (!row.canPreview) throw new Error('没有该文件正文查看权限')
  const opened = window.open(buildControlledFileViewerPath(row.controlledFileId, 'project-browser', route.fullPath, bodyReadContext), '_blank')
  if (!opened) throw new Error('预览窗口未能打开，请允许此站点打开新窗口')
  opened.opener = null
}
const openOperation = async (rows: FileCandidate[]) => {
  if (rows.length !== 1) throw new Error('请选择一个文件')
  await router.push({ path: `/dcc/controlled-file/detail/${referenceIdentity(rows[0].controlledFileId)}`, query: { management: '1', from: 'project-browser', returnTo: route.fullPath } })
}
const previewRow = async (row: FileCandidate) => {
  try {
    const bodyReadContext = row.status === 'SUPERSEDED' || row.status === 'OBSOLETE'
      ? serializeDccBodyReadContext(row.controlledFileId, {
        intent: 'VERSION_HISTORY', controlledFileId: row.controlledFileId,
        expectedVersionNo: row.versionNo
      })
      : undefined
    await preview(row, bodyReadContext)
  } catch (error) { state.error = error instanceof Error ? error.message : String(error) }
}
const downloadLibraryFile=async(row:FileCandidate)=>{try {await triggerControlledFileVariantDownload(row.controlledFileId,'editable')} catch(e:any){state.error=e.message||'下载失败'}}
const openOperationRow = async (row: FileCandidate) => {
  try { await openOperation([row]) } catch (error) { state.error = error instanceof Error ? error.message : String(error) }
}
onMounted(async () => {
  if (!props.libraryScope) { await Promise.all([state.loadProjects(), state.loadActor(), state.loadFiles()]); return }
  try {
    const library=await request.get({url:'/dcc/project-codes/library',params:{scope:props.libraryScope}})
    if (!library || !library.id || library.libraryScope!==props.libraryScope || typeof library.canEdit!=='boolean')
      throw new Error('独立文件库身份或权限响应不完整')
    libraryCanEdit.value=library.canEdit
    await state.loadActor();await state.selectLibrary({...library,id:String(library.id)})
  } catch(e:any) {state.projectError=e.message || '文件库加载失败'}
})
</script>
<style scoped>
.project-browser {
  --el-color-primary: #008f86;
  --el-color-primary-light-3: #4db1aa;
  --el-color-primary-light-5: #80c7c2;
  --el-color-primary-light-7: #b3ddd9;
  --el-color-primary-light-8: #cce9e7;
  --el-color-primary-light-9: #e6f5f2;
  --el-color-primary-dark-2: #00726b;
  display: grid;
  grid-template-columns: minmax(244px, 24%) minmax(0, 1fr);
  gap: 8px;
}
aside, main {
  min-width: 0;
  padding: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}
.search { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.search :deep(.el-input) { flex: 1 1 140px; min-width: 0; }
.search :deep(.el-button),
[data-testid="dcc-project-folder-maintenance"] :deep(.el-button) { margin-left: 0; }
[data-testid="dcc-project-folder-maintenance"],
[data-testid="dcc-library-folder-maintenance"] { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.version-view { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.version-select { width: 220px; max-width: 100%; }
.version-view > span:last-child, main > p { font-size: 12px; color: var(--el-text-color-secondary); }
main > p, h4 { margin: 8px 0; overflow-wrap: anywhere; }
h4 { font-size: 14px; }
.project-browser :deep(.el-input__wrapper),
.project-browser :deep(.el-select__wrapper) { min-height: 32px; }
.project-browser :deep(.el-table) { --el-table-header-bg-color: #f5f8f9; font-size: 13px; }
.project-browser :deep(.el-table__cell) { padding: 6px 0; }
.project-browser :deep(.el-tabs__header) { margin-bottom: 8px; }
.project-browser :deep(.el-pagination) { flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.project-browser :deep(.el-alert) { margin-bottom: 8px; }
.project-browser :deep(.el-alert__content),
.project-browser :deep(.el-alert__title) { min-width: 0; overflow-wrap: anywhere; }
@media (max-width: 900px) {
  .project-browser { grid-template-columns: minmax(0, 1fr); }
  .project-browser > aside { max-height: 280px; overflow: auto; }
  .project-browser :deep(.el-pagination) { overflow-x: auto; max-width: 100%; }
}
</style>
