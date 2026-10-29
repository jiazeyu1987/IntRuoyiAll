<template>
  <section class="project-browser" data-testid="dcc-project-browser">
    <aside>
      <UnifiedListTemplate
        class="unified-list-template--contained project-browser__projects"
        table-key="dcc.projectBrowser.projects"
        pagination-layout="total, prev, next"
        :total="state.projectsTotal"
        v-model:page="state.projectPage"
        v-model:limit="state.projectPageSize"
        @pagination="state.loadProjects()"
      >
        <template #filters>
          <div>
            <el-input
              v-model="state.projectKeyword"
              placeholder="项目名称或编码"
              clearable
              @keyup.enter="searchProjects"
            />
          </div>
        </template>
        <template #actions>
          <el-button :loading="state.projectLoading" @click="searchProjects">查找项目</el-button>
        </template>
        <template #table>
          <el-alert
            v-if="state.projectError"
            :title="state.projectError"
            type="error"
            :closable="false"
          />
          <el-table
            stripe
            border
            :data="state.projects"
            v-loading="state.projectLoading"
            highlight-current-row
            @row-click="selectProject"
          >
            <el-table-column prop="projectName" label="项目" />
            <el-table-column prop="projectCode" label="编码" />
          </el-table>
        </template>
      </UnifiedListTemplate>

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
      <el-tabs :model-value="state.scope" @tab-change="changeScope"><el-tab-pane label="全局文件" name="global" /><el-tab-pane label="当前项目" name="project" :disabled="!state.project" /><el-tab-pane label="项目文件夹" name="directory" /></el-tabs>
      <p v-if="state.scope === 'project'">{{ state.project?.projectName }} / 全部文件夹</p>
      <p v-if="state.scope === 'directory'">{{ state.project?.projectName }} / {{ state.folder?.name || '请选择文件夹' }}</p>
      <UnifiedListTemplate
        class="unified-list-template--contained project-browser__files"
        style="height: auto; flex: 1"
        table-key="dcc.projectBrowser.files"
        :total="state.total"
        v-model:page="state.pageNo"
        v-model:limit="state.pageSize"
        @pagination="state.loadFiles()"
      >
        <template #filters>
          <div>
            <div class="version-view">
              <el-tooltip content="默认最新受控版本；工作、审批中及历史版本按所选阶段查询。"><span>版本浏览范围</span></el-tooltip>
              <el-select
                v-model="state.versionView"
                data-testid="dcc-project-browser-version-view"
                class="!w-280px"
                @change="state.changeVersionView($event)"
              >
                <el-option-group
                  v-for="group in versionViewGroups"
                  :key="group.name"
                  :label="group.name"
                >
                  <el-option
                    v-for="option in group.options"
                    :key="option.value"
                    :value="option.value"
                    :label="option.label"
                  />
                </el-option-group>
              </el-select>

            </div>
            <el-input
              v-model="state.keyword"
              placeholder="文件名称、编号或项目名称"
              clearable
              @keyup.enter="state.search(state.scope)"
            />
          </div>
        </template>
        <template #actions>
          <el-button :loading="state.loading" @click="state.search(state.scope)">搜索</el-button>
          <el-button
            v-if="state.scope === 'directory' && state.folder"
            @click="operationVisible = true"
          >
            选择文件操作
          </el-button>
          <template v-if="state.scope === 'directory' && state.project && state.folder">
            <el-button
              v-if="state.canReference"
              :disabled="
                !projectReferences ||
                projectReferences.loading ||
                projectReferences.busy ||
                projectReferences.savedReadFailure
              "
              @click="projectReferences.openSelector()"
            >
              引用
            </el-button>
            <el-button
              v-if="state.canReference"
              data-testid="dcc-folder-cancel-reference"
              :disabled="
                !projectReferences ||
                projectReferences.loading ||
                projectReferences.busy ||
                projectReferences.savedReadFailure ||
                !projectReferences.selectedReference
              "
              @click="projectReferences.cancelSelectedReference()"
            >
              取消引用
            </el-button>
            <el-button
              data-testid="dcc-folder-refresh-references"
              :disabled="!projectReferences || projectReferences.loading || projectReferences.busy"
              @click="projectReferences.refresh()"
            >
              刷新引用列表
            </el-button>
          </template>
        </template>
        <template #table>
          <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
          <el-alert v-if="state.actorError" :title="state.actorError" type="error" :closable="false" />
          <DccProjectReferences
            ref="projectReferences"
            embedded-toolbar
            v-if="state.scope === 'directory' && state.project && state.folder"
            :key="state.contextKey"
            :context-key="state.contextKey"
            :selection-context-key="listContextKey"
            :project-id="state.project.id"
            :folder-id="state.folder.folderId!"
            :folder-name="state.folder.name"
            :is-project-leader="state.canReference"
            :selector-source="selectorSource"
            :directories="state.directories"
            :load-page="loadPage"
            :open-preview="preview"
            :open-reference-preview="previewReference"
            :open-reference-trace="traceReference"
            @show-usage="openUsage($event.selectedVersion)"
            :load-references="loadReferences"
            :create-references="createReferences"
            :cancel-reference="cancelReference"
            @changed="state.referencesChanged()"
            @refreshed="state.referencesChanged()"
            @saved-read-failure="state.referencesChanged()"
          >
            <template
              #default="{
                rows,
                loading,
                busy,
                readingReferenceId,
                selectedReference,
                selectReference,
                readReference
              }"
            >
              <ProjectFolderFileTable
                :files="state.rows"
                :references="rows"
                :usage="state.usage"
                :keyword="state.appliedKeyword"
                :version-view="state.versionView"
                :loading="state.loading || loading || busy"
                :can-reference="state.canReference"
                :selected-reference="selectedReference"
                :reading-reference-id="readingReferenceId"
                @select-reference="selectReference"
                @read-reference="readReference"
                @preview="previewRow"
                @relations="openRelationsRow"
                @operation="openOperationRow"
                @usage="openUsage"
              />
            </template>
          </DccProjectReferences>
          <ProjectFolderFileTable
            v-else
            :files="state.rows"
            :references="[]"
            :usage="state.usage"
            :keyword="state.appliedKeyword"
            :version-view="state.versionView"
            :loading="state.loading"
            :can-reference="false"
            @preview="previewRow"
            @relations="openRelationsRow"
            @operation="openOperationRow"
            @usage="openUsage"
          />
          <p v-if="state.scope === 'directory' && state.folder">
            当前文件夹引用与文件同表显示；引用固定所选受控版本。分页仅统计本项目文件。
          </p>
        </template>
      </UnifiedListTemplate>

    </main>
    <DccFileSelector v-if="state.folder" v-model="operationVisible" :source="selectorSource" purpose="operation" :directories="state.directories" :selected="[]" :load-page="loadPage" :open-preview="preview" :persist="openOperation" />
    <ProjectFileRelationsDialog v-if="relationsVisible && relationsSource" v-model="relationsVisible" :source="relationsSource" :context-key="listContextKey" />
    <DccReferenceUsageDialog v-if="usageVisible && usageSource" v-model="usageVisible" :source="usageSource" :context-key="listContextKey" :return-to="route.fullPath" />
    <ProjectFolderEditor v-if="state.project" :key="state.project.id" ref="folderEditor" @saved="handleFolderMaintenanceSaved" @deleted="handleFolderMaintenanceSaved" />
  </section>
</template>
<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'
import ProjectFolderEditor from '../basic-data/components/ProjectFolderEditor.vue'
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
import { ProjectBrowserState, PROJECT_BROWSER_VERSION_VIEWS } from './project-browser'
const route = useRoute(), router = useRouter(), user = useUserStore()
const state = reactive(new ProjectBrowserState(referenceIdentity(getVisitTenantId() || getTenantId()), referenceIdentity(user.getUser.id)))
const versionViewGroups = [...new Set(PROJECT_BROWSER_VERSION_VIEWS.map(option => option.group))].map(name => ({
  name, options: PROJECT_BROWSER_VERSION_VIEWS.filter(option => option.group === name)
}))
const projectReferences = ref<InstanceType<typeof DccProjectReferences>>()
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
const listContextKey = computed(() => JSON.stringify([state.contextKey, state.scope, state.appliedKeyword, state.versionView, state.pageNo, state.pageSize]))
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
const openOperationRow = async (row: FileCandidate) => {
  try { await openOperation([row]) } catch (error) { state.error = error instanceof Error ? error.message : String(error) }
}
onMounted(() => Promise.all([state.loadProjects(), state.loadActor(), state.loadFiles()]))
</script>
<style scoped>
.project-browser { height:calc(100dvh - 170px);min-height:0;overflow:hidden;display: grid; grid-template-columns: minmax(260px, 28%) minmax(0, 1fr); gap: 18px; }
aside, main { display:flex;flex-direction:column;min-height:0;min-width: 0; padding: 14px; background: var(--el-bg-color); }
.search { display: flex; gap: 8px; margin-bottom: 12px; }
.version-view { display: flex; align-items: center; flex-wrap: nowrap; gap: 8px; margin-bottom: 0; white-space:nowrap;flex-shrink:0; }
@media (max-width: 900px) { .project-browser { grid-template-columns: 1fr; } }
.project-browser__projects { flex-shrink:0;--unified-list-height:280px; }
aside > .el-tree { flex:1;min-height:0;overflow:auto; }
main > .el-tabs { flex-shrink:0; }
.project-browser__files :deep(.unified-list-template__table-shell > div) { display:flex;flex:1;min-height:0;flex-direction:column; }
.project-browser__files :deep(.el-table) { flex:1;min-height:0;height:100%; }
</style>
