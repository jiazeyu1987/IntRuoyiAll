<template>
  <section class="project-browser" data-testid="dcc-project-browser">
    <aside>
      <div class="search"><el-input v-model="state.projectKeyword" placeholder="项目名称或编码" clearable @keyup.enter="searchProjects" /><el-button :loading="state.projectLoading" @click="searchProjects">查找项目</el-button></div>
      <el-alert v-if="state.projectError" :title="state.projectError" type="error" :closable="false" />
      <el-table :data="state.projects" v-loading="state.projectLoading" highlight-current-row @row-click="selectProject">
        <el-table-column prop="projectName" label="项目" /><el-table-column prop="projectCode" label="编码" />
      </el-table>
      <el-pagination v-model:current-page="state.projectPage" :page-size="20" :total="state.projectsTotal" layout="total, prev, next" @current-change="state.loadProjects()" />
      <h4>{{ state.project ? state.project.projectName : '项目文件夹' }}</h4>
      <div v-if="state.project" data-testid="dcc-project-folder-maintenance">
        <el-button v-hasPermi="['dcc:project-code:update']" :disabled="state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('create')">新增文件夹</el-button>
        <el-button v-hasPermi="['dcc:project-code:update']" :disabled="!state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('update')">编辑文件夹</el-button>
        <el-button v-hasPermi="['dcc:project-code:update']" :disabled="!state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('delete')">删除文件夹</el-button>
      </div>
      <el-alert v-if="state.folderError" :title="state.folderError" type="error" :closable="false" />
      <el-tree :key="state.project?.id" v-loading="state.folderLoading" :data="state.directories" node-key="key" :props="{ label: 'name', children: 'children' }" highlight-current default-expand-all @node-click="selectFolder" />
      <el-empty v-if="state.project && !state.folderLoading && !state.folderError && !state.directories.length" description="项目尚无文件夹" />
    </aside>
    <main>
      <el-tabs :model-value="state.scope" @tab-change="changeScope"><el-tab-pane label="全局文件" name="global" /><el-tab-pane label="项目文件夹" name="directory" /></el-tabs>
      <p v-if="state.scope === 'directory'">{{ state.project?.projectName }} / {{ state.folder?.name || '请选择文件夹' }}</p>
      <div class="version-view">
        <span>版本浏览范围</span>
        <el-select
          v-model="state.versionView"
          data-testid="dcc-project-browser-version-view"
          class="!w-280px"
          @change="state.changeVersionView($event)"
        >
          <el-option-group v-for="group in versionViewGroups" :key="group.name" :label="group.name">
            <el-option v-for="option in group.options" :key="option.value" :value="option.value" :label="option.label" />
          </el-option-group>
        </el-select>
        <span>默认最新受控版本；工作、审批中及历史版本按所选阶段查询。</span>
      </div>
      <div class="search"><el-input v-model="state.keyword" placeholder="文件名称、编号或项目名称" clearable @keyup.enter="state.search(state.scope)" /><el-button :loading="state.loading" @click="state.search(state.scope)">搜索</el-button><el-button v-if="state.folder" @click="operationVisible = true">选择文件操作</el-button></div>
      <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
      <el-table :data="state.rows" v-loading="state.loading" row-key="controlledFileId">
        <el-table-column label="文件名称" min-width="180">
          <template #default="{ row }">
            <span
              data-testid="dcc-project-browser-file-name" class="project-file-name"
              :class="{ 'is-referenced': state.usage[row.masterId]?.referenced === true }">{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fileNumber" label="编号" min-width="110" />
        <el-table-column prop="projectName" label="所属项目" min-width="120" />
        <el-table-column prop="folderName" label="文件夹" min-width="100" />
        <el-table-column prop="versionNo" label="版本" width="100" />
        <el-table-column label="状态" min-width="120"><template #default="{ row }">{{ statusLabel(row.status) }}</template></el-table-column>
        <el-table-column label="被引用情况" min-width="180"><template #default="{ row }"><DccReferenceBadge v-if="state.usage[row.masterId]" :is-reference="false" :project-count="state.usage[row.masterId].referenceProjectCount" :source-project-name="row.projectName" :pending-effect="row.pendingEffect" :obsolete="row.status === 'OBSOLETE'" /><el-button link data-testid="dcc-project-reference-usage" @click="openUsage(row)">使用明细</el-button></template></el-table-column>
        <el-table-column label="操作" width="205"><template #default="{ row }"><el-button link :disabled="!row.canPreview" @click="previewRow(row)">正文</el-button><el-button link data-testid="dcc-project-file-relations" @click="openRelationsRow(row)">关联</el-button><el-button link @click="openOperationRow(row)">操作面板</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="state.pageNo" :page-size="state.pageSize" :total="state.total" layout="total, prev, pager, next" @current-change="state.loadFiles()" />
      <section v-if="state.project && state.folder" class="references">
        <h4>此文件夹引用</h4>
        <el-alert v-if="state.actorError" :title="state.actorError" type="error" :closable="false" />
        <DccProjectReferences
          :key="state.contextKey" :context-key="state.contextKey" :project-id="state.project.id" :folder-id="state.folder.folderId!" :folder-name="state.folder.name" :is-project-leader="state.canReference" :selector-source="selectorSource" :directories="state.directories" :load-page="loadPage" :open-preview="preview"
          :open-reference-preview="previewReference" :open-reference-trace="traceReference"
          @show-usage="openUsage($event.selectedVersion)"
          :load-references="loadReferences" :create-references="createReferences" :cancel-reference="cancelReference"
          @changed="state.referencesChanged()" @saved-read-failure="state.referencesChanged()" />
      </section>
    </main>
    <DccFileSelector v-if="state.folder" v-model="operationVisible" :source="selectorSource" purpose="operation" :directories="state.directories" :selected="[]" :load-page="loadPage" :open-preview="preview" :persist="openOperation" />
    <ProjectFileRelationsDialog v-if="relationsVisible && relationsSource" v-model="relationsVisible" :source="relationsSource" :context-key="listContextKey" />
    <DccReferenceUsageDialog v-if="usageVisible && usageSource" v-model="usageVisible" :source="usageSource" :context-key="listContextKey" :return-to="route.fullPath" />
    <ProjectFolderEditor v-if="state.project" :key="state.project.id" ref="folderEditor" @saved="handleFolderMaintenanceSaved" @deleted="handleFolderMaintenanceSaved" />
  </section>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'
import ProjectFolderEditor from '../basic-data/components/ProjectFolderEditor.vue'
import DccFileSelector from '../relations/DccFileSelector.vue'
import DccProjectReferences from '../relations/DccProjectReferences.vue'
import DccReferenceBadge from '../relations/DccReferenceBadge.vue'
import ProjectFileRelationsDialog from './ProjectFileRelationsDialog.vue'
import DccReferenceUsageDialog from '../relations/DccReferenceUsageDialog.vue'
import type { ReferenceUsageSource } from '../relations/DccReferenceUsageDialog.vue'
import type { DirectoryNode, SelectorSource } from '../relations/DccFileSelector.vue'
import type { FileCandidate, SelectorQuery } from '../relations/selector-state'
import { referenceIdentity, type ReferenceRow } from '../relations/project-reference-contract'
import { buildControlledFileViewerPath, buildControlledFileTraceabilityPath } from '../view/presentation'
import { ProjectBrowserState, PROJECT_BROWSER_VERSION_VIEWS } from './project-browser'
import { getDccControlledFileStatusLabel, type DccControlledFileStatus } from '../shared/lifecycle'
const statusLabel = (status: string) => getDccControlledFileStatusLabel(status as DccControlledFileStatus)
const route = useRoute(), router = useRouter(), user = useUserStore()
const state = reactive(new ProjectBrowserState(referenceIdentity(getVisitTenantId() || getTenantId()), referenceIdentity(user.getUser.id)))
const versionViewGroups = [...new Set(PROJECT_BROWSER_VERSION_VIEWS.map(option => option.group))].map(name => ({
  name, options: PROJECT_BROWSER_VERSION_VIEWS.filter(option => option.group === name)
}))
const operationVisible = ref(false)
const folderEditor = ref<InstanceType<typeof ProjectFolderEditor>>()
const openFolderMaintenance = async (mode: 'create' | 'update' | 'delete') => {
  const project = state.project, folder = state.folder
  if (!project || state.folderLoading || state.folderError || !checkPermi(['dcc:project-code:update'])) return
  if (mode !== 'create' && (!folder?.folderId || folder.projectId !== project.id)) return
  if (!folderEditor.value) { state.folderError = '项目文件夹维护组件尚未加载，请重新选择项目'; return }
  if (mode === 'delete') await folderEditor.value.openDelete(project.id, folder!.folderId!)
  else if (mode === 'update') await folderEditor.value.open(project.id, folder!.folderId!)
  else await folderEditor.value.open(project.id)
}
const handleFolderMaintenanceSaved = async (projectId: number | string) => {
  const current = state.project
  if (current && current.id === String(projectId)) await state.selectProject(current.id)
}
const relationsVisible = ref(false), relationsSource = ref<FileCandidate>()
const usageVisible = ref(false), usageSource = ref<ReferenceUsageSource>()
const listContextKey = computed(() => JSON.stringify([state.contextKey, state.scope, state.keyword, state.versionView, state.pageNo]))
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
const changeScope = (scope: string | number) => state.search(scope === 'directory' ? 'directory' : 'global')
const loadPage = (query: SelectorQuery) => state.loadPage(query)
const loadReferences = (project: string, folder: string) => state.loadReferences(project, folder)
const createReferences = (project: string, folder: string, ids: string[], reason: string) => state.createReferences(project, folder, ids, reason)
const cancelReference = (project: string, folder: string, master: string, id: string, reason: string) => state.cancelReference(project, folder, master, id, reason)
const previewReference = async (row: ReferenceRow, isCurrent: () => boolean) => {
  const selected = await state.readReferenceForAction(row)
  if (!isCurrent()) return
  await preview(selected.selectedVersion)
}
const traceReference = async (row: ReferenceRow, isCurrent: () => boolean) => {
  const selected = await state.readReferenceTrace(row)
  if (!isCurrent()) return
  await router.push(buildControlledFileTraceabilityPath(selected.reference.selectedControlledFileId,
    'browser', route.fullPath, 'trace'))
}
const preview = async (row: FileCandidate) => {
  if (!row.canPreview) throw new Error('没有该文件正文查看权限')
  const opened = window.open(buildControlledFileViewerPath(row.controlledFileId, 'project-browser', route.fullPath), '_blank')
  if (!opened) throw new Error('预览窗口未能打开，请允许此站点打开新窗口')
  opened.opener = null
}
const openOperation = async (rows: FileCandidate[]) => {
  if (rows.length !== 1) throw new Error('请选择一个文件')
  await router.push({ path: `/dcc/controlled-file/detail/${referenceIdentity(rows[0].controlledFileId)}`, query: { management: '1', from: 'project-browser', returnTo: route.fullPath } })
}
const previewRow = async (row: FileCandidate) => {
  try { await preview(row) } catch (error) { state.error = error instanceof Error ? error.message : String(error) }
}
const openOperationRow = async (row: FileCandidate) => {
  try { await openOperation([row]) } catch (error) { state.error = error instanceof Error ? error.message : String(error) }
}
onMounted(() => Promise.all([state.loadProjects(), state.loadActor(), state.loadFiles()]))
</script>
<style scoped>
.project-browser { display: grid; grid-template-columns: minmax(260px, 28%) minmax(0, 1fr); gap: 18px; }
aside, main { min-width: 0; padding: 14px; background: var(--el-bg-color); }
.search { display: flex; gap: 8px; margin-bottom: 12px; }
.references { margin-top: 24px; }
.version-view { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.project-file-name.is-referenced { color: #b85a00; }
@media (max-width: 900px) { .project-browser { grid-template-columns: 1fr; } }
</style>
