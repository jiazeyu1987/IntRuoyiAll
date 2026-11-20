<template>
  <section class="project-browser" data-testid="dcc-project-browser" data-list-viewport="embedded" data-user-table-column-explicit-scope>
    <aside>
      <LibraryRoleAccessHint v-if="libraryScope" :scope="libraryScope" />
      <h4 v-if="!libraryScope">项目</h4>
      <UnifiedListTemplate v-if="!libraryScope"
        @query="searchProjects"
        :server-filter-reset="resetProjects"
        :query-loading="state.projectLoading"
        class="project-browser__projects"
        :class="{ 'filters-expanded': projectFiltersExpanded }"
        table-key="dcc.projectBrowser.projects"
        pagination-layout="total, prev, next"
        :total="state.projectsTotal"
        v-model:page="state.projectPage"
        v-model:limit="state.projectPageSize"
        @pagination="state.loadProjects()"
        server-filter
        :server-filter-query="() => state.loadProjects()"
        :query-model="state.projectQuery"
        :columns="projectFilterColumns"
        :show-column-settings="false"



      >
        <template #filters>
          <div>
            <el-input
              v-model="state.projectKeyword"
              placeholder="项目名称或编号"
              clearable
              @keyup.enter="searchProjects"
            />
            <el-button class="project-browser__filter-toggle" text :aria-expanded="projectFiltersExpanded" @click="projectFiltersExpanded = !projectFiltersExpanded">
              筛选 {{ projectFiltersExpanded ? '⌃' : '⌄' }}
            </el-button>
          </div>
        </template>

        <template #table="{ resolveRows }">
          <el-alert
            v-if="state.projectError"
            :title="state.projectError"
            type="error"
            :closable="false"
          />
          <div ref="projectList" class="project-browser__project-list" :style="{ height: `${projectListHeight}px` }" v-loading="state.projectLoading" @scroll="scheduleProjectScroll" @scrollend="settleProjectScroll()">
            <button
              v-for="project in resolveRows(state.projects)"
              :key="project.id"
              type="button"
              class="project-browser__project"
              :class="{ 'is-selected': state.project?.id === project.id }"
              :aria-pressed="state.project?.id === project.id"
              @click="selectProject(project)"
            >
              <strong>{{ project.projectName }}</strong>
              <span>{{ project.projectCode }}</span>
            </button>
            <el-empty v-if="!state.projectLoading && !state.projectError && !state.projects.length" description="暂无项目" :image-size="50" />
          </div>
        </template>
      </UnifiedListTemplate>

      <h4>{{ libraryScope ? state.project?.projectName || '文件夹' : '项目目录' }}</h4>
      <div v-if="state.project && !libraryScope && canMaintainProjectFolders" class="project-browser__folder-actions" data-testid="dcc-project-folder-maintenance">
        <el-button :disabled="state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('create')">新增文件夹</el-button>
        <el-button :disabled="state.scope !== 'directory' || !state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('update')">编辑文件夹</el-button>
        <el-button :disabled="state.scope !== 'directory' || !state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('delete')">删除文件夹</el-button>
      </div>
      <div v-if="state.project && libraryScope && libraryCanEdit" class="project-browser__folder-actions" data-testid="dcc-library-folder-maintenance">
        <el-button :disabled="state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('create')">新增文件夹</el-button>
        <el-button :disabled="state.scope !== 'directory' || !state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('update')">编辑文件夹</el-button>
        <el-button :disabled="state.scope !== 'directory' || !state.folder || state.folderLoading || Boolean(state.folderError)" @click="openFolderMaintenance('delete')">删除文件夹</el-button>
      </div>
      <el-alert v-if="state.folderError" :title="state.folderError" type="error" :closable="false" />
      <el-tree :key="state.project?.id" v-loading="state.folderLoading" :data="directoryTree" :current-node-key="currentDirectoryKey" :indent="18" node-key="key" :props="{ label: 'name', children: 'children' }" highlight-current default-expand-all @node-click="selectFolder" />
      <el-empty v-if="state.project && !state.folderLoading && !state.folderError && !state.directories.length" :description="libraryScope ? '文件库尚无文件夹' : '项目尚无文件夹'" />
    </aside>
    <main>
      <el-tabs :model-value="state.scope" @tab-change="changeScope"><el-tab-pane v-if="!libraryScope" label="全局文件" name="global" /><el-tab-pane :label="libraryScope ? '当前目录' : '当前项目'" name="project" :disabled="!state.project" /><el-tab-pane :label="libraryScope ? '文件夹' : '项目文件夹'" name="directory" :disabled="!state.folder" /></el-tabs>
      <p class="project-browser__path" v-if="state.scope === 'project'">{{ state.project?.projectName }} / 全部文件夹</p>
      <p class="project-browser__path" v-if="state.scope === 'directory'">{{ state.project?.projectName }} / {{ state.folder?.name || '请选择文件夹' }}</p>
      <UnifiedListTemplate
        @query="state.search(state.scope)"
        :server-filter-reset="resetFiles"
        :query-loading="state.loading"
        class="project-browser__files"
        :class="{ 'filters-expanded': fileFiltersExpanded }"
        table-key="dcc.projectBrowser.files"
        :total="state.total"
        v-model:page="state.pageNo"
        v-model:limit="state.pageSize"
        @pagination="state.loadFiles()"
        server-filter
        :server-filter-query="() => state.loadFiles()"
        :query-model="state"


        :columns="fileFilterColumns"
        :column-filter-definitions="fileFilterDefinitions"
        :show-column-settings="false"

      >
        <template #filters>
          <div class="project-browser__file-search">
            <el-input
              v-model="state.keyword"
              placeholder="文件名称、编号或项目名称"
              clearable
              @keyup.enter="state.search(state.scope)"
            />
            <div class="version-view">
              <el-tooltip content="默认最新受控版本；工作、审批中及历史版本按所选阶段查询。"><span>版本浏览范围</span></el-tooltip>
              <el-select
                v-model="state.versionView"
                data-testid="dcc-project-browser-version-view"
                @change="state.changeVersionView($event)"
              >
                <el-option-group v-for="group in versionViewGroups" :key="group.name" :label="group.name">
                  <el-option v-for="option in group.options" :key="option.value" :value="option.value" :label="option.label" />
                </el-option-group>
              </el-select>
            </div>
            <el-button text :aria-expanded="fileFiltersExpanded" @click="fileFiltersExpanded = !fileFiltersExpanded">
              筛选 {{ fileFiltersExpanded ? '⌃' : '⌄' }}
            </el-button>
          </div>
        </template>
        <template #actions>

          <el-button
            v-if="!libraryScope && state.scope === 'directory' && state.folder"
            @click="operationVisible = true"
          >
            选择文件操作
          </el-button>
          <template v-if="!libraryScope && state.scope === 'directory' && state.project && state.folder">
            <el-button
              v-if="state.canReference"
              :disabled="
                !projectReferences ||
                projectReferences.loading ||
                projectReferences.busy ||
                projectReferences.savedReadFailure
              "
              @click="requireProjectReferences().openSelector()"
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
              @click="requireProjectReferences().cancelSelectedReference()"
            >
              取消引用
            </el-button>
            <el-button
              data-testid="dcc-folder-refresh-references"
              :disabled="!projectReferences || projectReferences.loading || projectReferences.busy"
              @click="requireProjectReferences().refresh()"
            >
              刷新引用列表
            </el-button>
          </template>
        </template>
        <template #table="{ resolveRows }">
          <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
          <el-alert v-if="state.actorError" :title="state.actorError" type="error" :closable="false" />
          <DccProjectReferences
            ref="projectReferences"
            embedded-toolbar
            v-if="!libraryScope && state.scope === 'directory' && state.project && state.folder"
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
            :page-rows="state.references"
            :create-references="createReferences"
            :cancel-reference="cancelReference"
            @changed="state.referencesChanged()"
            @refreshed="state.referencesChanged()"
            @saved-read-failure="state.referencesChanged()"
          >
            <template
              #default="{
                loading,
                busy,
                readingReferenceId,
                selectedReference,
                selectReference,
                readReference
              }"
            >
              <ProjectFolderFileTable
                :resolve-rows="resolveRows"
                :files="state.rows"
                :references="state.references"
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
            :read-only="Boolean(libraryScope)" :can-manage="libraryScope ? canManageLibraryFile : undefined" :can-download="Boolean(libraryScope) && libraryCanEdit" @download="downloadLibraryFile"
            :resolve-rows="resolveRows"
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
        </template>
      </UnifiedListTemplate>

    </main>
    <DccFileSelector v-if="state.folder" v-model="operationVisible" :source="selectorSource" purpose="operation" :directories="state.directories" :selected="[]" :load-page="loadPage" :open-preview="preview" :persist="openOperation" />
    <ProjectFileRelationsDialog v-if="relationsVisible && relationsSource" v-model="relationsVisible" :source="relationsSource" :context-key="listContextKey" />
    <DccReferenceUsageDialog v-if="usageVisible && usageSource" v-model="usageVisible" :source="usageSource" :context-key="listContextKey" :return-to="route.fullPath" />
    <ProjectFolderEditor v-if="state.project" :key="state.project.id" ref="folderEditor" :project-is-leader="state.canMaintainFolders" :library-scope="libraryScope" :library-can-edit="libraryCanEdit" @saved="handleFolderMaintenanceSaved" @deleted="handleFolderMaintenanceSaved" />
  </section>
</template>
<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { completeProjectWindow } from './project-list-scroll'
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
import request from '@/config/axios'
import { triggerControlledFileVariantDownload } from '@/api/dcc/controlledFile/workflow'
const props=defineProps<{libraryScope?: 'QMS' | 'GENERAL'}>()
const libraryCanEdit=ref(false)
import { ProjectBrowserState, PROJECT_BROWSER_VERSION_VIEWS } from './project-browser'

import type { ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'
import { getDccControlledFileStatusLabel, type DccControlledFileStatus } from '../shared/lifecycle'
const projectFilterColumns = [
  { key: 'projectName', label: '项目', visible: true },
  { key: 'projectCode', label: '编码', visible: true }
]
const fileFilterColumns = [
  { key: 'file.fileName', label: '文件名称', visible: true },
  { key: 'file.fileNumber', label: '编号', visible: true },
  { key: 'file.projectName', label: '所属项目', visible: true },
  { key: 'file.folderName', label: '来源文件夹', visible: true },
  { key: 'file.versionNo', label: '版本', visible: true },
  { key: 'file.status', label: '状态', visible: true },
  { key: 'projectCount', label: '引用情况', visible: true }
]
const fileFilterDefinitions: ListColumnFilterDefinition[] = [
  { key: 'file.projectName', label: '所属项目', type: 'text', getValue: row => row.reference?.sourceProjectName || row.file.projectName },
  { key: 'file.status', label: '状态', type: 'text', getValue: row => getDccControlledFileStatusLabel(row.file.status as DccControlledFileStatus) },
  { key: 'projectCount', label: '引用情况', type: 'text', getValue: row => `${row.reference ? '引用 ' : ''}${row.projectCount} 个项目` }
]
const route = useRoute(), router = useRouter(), user = useUserStore()
const state = reactive(new ProjectBrowserState(referenceIdentity(getVisitTenantId() || getTenantId()), referenceIdentity(user.getUser.id)))
const versionViewGroups = [
  ...new Set(PROJECT_BROWSER_VERSION_VIEWS.map((option) => option.group))
].map((name) => ({
  name,
  options: PROJECT_BROWSER_VERSION_VIEWS.filter((option) => option.group === name)
}))
const projectReferences = ref<InstanceType<typeof DccProjectReferences>>()
const requireProjectReferences = () => {
  const references = projectReferences.value
  if (!references) throw new Error('引用列表尚未就绪，请等待加载完成')
  return references
}
const projectList = ref<HTMLElement>()
const projectListHeight = ref(280)
let projectScrollTimer: ReturnType<typeof setTimeout> | undefined
let projectListObserver: ResizeObserver | undefined
const settleProjectScroll = (top = projectList.value?.scrollTop || 0) => {
  clearTimeout(projectScrollTimer)
  const list = projectList.value
  if (!list) return
  const rows = Array.from(list.querySelectorAll<HTMLElement>('.project-browser__project'))
  const bounds = rows.map(row => ({ top: row.offsetTop, height: row.getBoundingClientRect().height }))
  const window = completeProjectWindow(bounds, top)
  projectListHeight.value = window.height
  nextTick(() => { if (projectList.value === list) list.scrollTop = window.top })
}
const scheduleProjectScroll = () => {
  clearTimeout(projectScrollTimer)
  projectScrollTimer = setTimeout(() => settleProjectScroll(), 140)
}
watch(() => state.projects, () => nextTick(() => settleProjectScroll(0)), { flush: 'post' })
onMounted(() => {
  if (!projectList.value) return
  let width = 0
  projectListObserver = new ResizeObserver(([entry]) => {
    if (entry.contentRect.width === width) return
    width = entry.contentRect.width
    settleProjectScroll()
  })
  projectListObserver.observe(projectList.value)
})
onBeforeUnmount(() => { clearTimeout(projectScrollTimer); projectListObserver?.disconnect() })
const projectFiltersExpanded = ref(false)
const fileFiltersExpanded = ref(false)
const directoryRootKey = computed(() => `project:${state.project?.id}:all`)
const directoryTree = computed<DirectoryNode[]>(() => !props.libraryScope && state.project
  ? [{ key: directoryRootKey.value, name: '全部文件夹', projectId: state.project.id, children: state.directories }]
  : state.directories)
const currentDirectoryKey = computed(() => state.scope === 'project' && !props.libraryScope
  ? directoryRootKey.value : state.scope === 'directory' ? state.folder?.key : '')
const operationVisible = ref(false)
const folderEditor = ref<InstanceType<typeof ProjectFolderEditor>>()
const canMaintainProjectFolders = computed(() => state.canMaintainFolders || checkPermi(['dcc:project-code:update']))
const openFolderMaintenance = async (mode: 'create' | 'update' | 'delete') => {
  const project = state.project,
    folder = state.folder
  if (
    !project ||
    state.folderLoading ||
    state.folderError ||
    !(props.libraryScope ? libraryCanEdit.value : canMaintainProjectFolders.value)
  )
    return
  if (mode !== 'create' && (state.scope !== 'directory' || !folder?.folderId || folder.projectId !== project.id)) return
  if (!folderEditor.value) {
    state.folderError = '项目文件夹维护组件尚未加载，请重新选择项目'
    return
  }
  if (mode === 'delete') await folderEditor.value.openDelete(project.id, folder!.folderId!)
  else if (mode === 'update') await folderEditor.value.open(project.id, folder!.folderId!)
  else if (props.libraryScope) await folderEditor.value.open(project.id, undefined, folder?.folderId || '0')
  else await folderEditor.value.open(project.id)
}
const handleFolderMaintenanceSaved = async (projectId: number | string) => {
  const current = state.project
  if (!current || current.id !== String(projectId)) return
  if (props.libraryScope) await state.selectLibrary(current)
  else await selectProject(current)
}
const relationsVisible = ref(false),
  relationsSource = ref<FileCandidate>()
const usageVisible = ref(false),
  usageSource = ref<ReferenceUsageSource>()
const listContextKey = computed(() =>
  JSON.stringify([
    state.contextKey,
    state.scope,
    state.appliedKeyword,
    state.versionView,
    state.pageNo,
    state.pageSize
  ])
)
const openRelationsRow = (row: FileCandidate) => {
  if (state.loading || !state.rows.some((file) => file.controlledFileId === row.controlledFileId))
    return
  relationsSource.value = JSON.parse(JSON.stringify(row))
  relationsVisible.value = true
}
const openUsage = (source: ReferenceUsageSource) => {
  if (state.loading) return
  usageSource.value = JSON.parse(JSON.stringify(source))
  usageVisible.value = true
}
watch(
  () => [listContextKey.value, state.loading],
  () => {
    relationsVisible.value = false
    relationsSource.value = undefined
  },
  { flush: 'sync' }
)
watch(
  () => [listContextKey.value, state.loading],
  () => {
    usageVisible.value = false
    usageSource.value = undefined
  },
  { flush: 'sync' }
)
const selectorSource = computed<SelectorSource>(() => ({
  contextKey: state.contextKey,
  tenantId: state.tenantId,
  projectId: state.project!.id,
  folderId: state.folder!.folderId,
  projectName: state.project!.projectName,
  folderName: state.folder!.name,
  fileName: '请选择文件',
  fileNumber: '',
  versionNo: ''
}))
const searchProjects = () => {
  state.projectPage = 1
  return state.loadProjects()
}
const resetProjects = () => {
  state.projectKeyword = ''
}

const resetFiles = () => {
  state.keyword = ''
  state.versionView = 'LATEST_CONTROLLED'
}

const selectProject = async (project: { id: string }) => {
  operationVisible.value = false
  await state.selectProject(project.id)
  if (state.project?.id === project.id && !state.folderError) await state.search('project')
}
const selectFolder = (node: DirectoryNode) => {
  operationVisible.value = false
  if (!props.libraryScope && node.key === directoryRootKey.value && node.projectId === state.project?.id)
    return state.search('project')
  return state.selectFolder(node)
}
const changeScope = (scope: string | number) => {
  if (scope !== 'global' && scope !== 'project' && scope !== 'directory')
    throw new Error('请选择正式项目浏览范围')
  return state.search(scope)
}
const loadPage = (query: SelectorQuery) => state.loadPage(query)
const loadReferences = (project: string, folder: string) => state.loadReferences(project, folder)
const createReferences = (project: string, folder: string, ids: string[], reason: string) =>
  state.createReferences(project, folder, ids, reason)
const cancelReference = (
  project: string,
  folder: string,
  master: string,
  id: string,
  reason: string
) => state.cancelReference(project, folder, master, id, reason)
const previewReference = async (row: ReferenceRow, isCurrent: () => boolean) => {
  const selected = await state.readReferenceForAction(row)
  if (!isCurrent()) return
  await preview(
    selected.selectedVersion,
    serializeDccBodyReadContext(selected.reference.selectedControlledFileId, {
      intent: 'VERSION_HISTORY',
      controlledFileId: selected.reference.selectedControlledFileId,
      expectedVersionNo: selected.selectedVersion.versionNo
    })
  )
}
const traceReference = async (row: ReferenceRow, isCurrent: () => boolean) => {
  const selected = await state.readReferenceTrace(row)
  if (!isCurrent()) return
  await router.push(
    buildControlledFileTraceabilityPath(
      selected.reference.selectedControlledFileId,
      'browser',
      route.fullPath,
      'trace'
    )
  )
}
const preview = async (row: FileCandidate, bodyReadContext?: string) => {
  if (!row.canPreview) throw new Error('没有该文件正文查看权限')
  const opened = window.open(
    buildControlledFileViewerPath(
      row.controlledFileId,
      'project-browser',
      route.fullPath,
      bodyReadContext
    ),
    '_blank'
  )
  if (!opened) throw new Error('预览窗口未能打开，请允许此站点打开新窗口')
  opened.opener = null
}
const openOperation = async (rows: FileCandidate[]) => {
  if (rows.length !== 1) throw new Error('请选择一个文件')
  if (props.libraryScope && !canManageLibraryFile(rows[0])) throw new Error('当前账号或文件状态不允许变更，请刷新后核对权限')
  await router.push({ path: `/dcc/controlled-file/detail/${referenceIdentity(rows[0].controlledFileId)}`, query: { management: '1', from: 'project-browser', returnTo: route.fullPath } })
}
const canManageLibraryFile = (file: FileCandidate) => libraryCanEdit.value && file.canEditFile === true
  && ['WORKING', 'ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(file.status)
const previewRow = async (row: FileCandidate) => {
  try {
    const bodyReadContext =
      row.status === 'SUPERSEDED' || row.status === 'OBSOLETE'
        ? serializeDccBodyReadContext(row.controlledFileId, {
            intent: 'VERSION_HISTORY',
            controlledFileId: row.controlledFileId,
            expectedVersionNo: row.versionNo
          })
        : undefined
    await preview(row, bodyReadContext)
  } catch (error) {
    state.error = error instanceof Error ? error.message : String(error)
  }
}
const downloadLibraryFile=async(row:FileCandidate)=>{try {await triggerControlledFileVariantDownload(row.controlledFileId,'editable')} catch(e:any){state.error=e.message||'下载失败'}}
const openOperationRow = async (row: FileCandidate) => {
  try {
    await openOperation([row])
  } catch (error) {
    state.error = error instanceof Error ? error.message : String(error)
  }
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
.project-browser { height:calc(100dvh - 170px); min-height:0; overflow:hidden; display:grid; grid-template-columns:minmax(260px, 18%) minmax(0, 1fr); border:1px solid var(--el-border-color-light); border-radius:6px; }
aside, main { display:flex; flex-direction:column; min-height:0; min-width:0; padding:14px; background:var(--el-bg-color); overflow:auto; }
aside { border-right:1px solid var(--el-border-color-light); }
h4 { margin:4px 0 12px; font-size:16px; }
.project-browser__projects { flex:none; margin-bottom:16px; }
.project-browser__project-list { position:relative; overflow-y:auto; overflow-x:hidden; overflow-anchor:none; scrollbar-gutter:stable; outline:1px solid var(--el-border-color-light); border-radius:4px; }
.project-browser__project { display:flex; flex-direction:column; gap:4px; width:100%; padding:10px 12px; border:0; border-left:4px solid transparent; background:transparent; text-align:left; color:var(--el-text-color-primary); cursor:pointer; font:inherit; }
.project-browser__project strong, .project-browser__project span { max-width:100%; overflow-wrap:anywhere; }
.project-browser__project span { color:var(--el-text-color-regular); font-size:13px; }
.project-browser__project:hover { background:var(--el-fill-color-light); }
.project-browser__project.is-selected { background:var(--el-color-primary-light-9); border-left-color:var(--el-color-primary); }
.project-browser__folder-actions { display:flex; gap:4px; margin-bottom:12px; }
.project-browser__folder-actions .el-button { flex:1; min-width:0; margin:0; padding:8px 4px; font-size:12px; }
aside > .el-tree { flex:none; min-height:100px; }
aside :deep(.el-tree-node__content) { height:34px; border-radius:4px; }
aside :deep(.el-tree-node.is-current > .el-tree-node__content) { background:var(--el-color-primary-light-9); color:var(--el-color-primary); }
main > .el-tabs { flex-shrink:0; }
.project-browser__path { margin:4px 0 16px; }
.project-browser__files { flex:none; }
.project-browser__projects :deep(.unified-list-template__query-form.unified-list-template__custom-query),
.project-browser__files :deep(.unified-list-template__query-form.unified-list-template__custom-query) { display:flex; flex-direction:column; align-items:stretch; gap:10px; margin-bottom:14px; }
.project-browser__projects :deep(.unified-list-template__filter-panel) { display:grid; grid-template-columns:minmax(0, 1fr) auto; align-items:center; }
.project-browser__projects :deep(.unified-list-template__filters),
.project-browser__projects :deep(.unified-list-template__filters > div),
.project-browser__projects :deep(.list-filter-actions) { display:contents; }
.project-browser__projects :deep(.list-filter-actions .el-button:first-child) { grid-column:2; grid-row:1; }
.project-browser__projects :deep(.list-filter-actions .el-button:last-child) { grid-column:2; grid-row:2; margin:0; }
.project-browser__filter-toggle { grid-column:1; grid-row:2; justify-self:start; }
.project-browser__projects :deep(.table-multi-filter) { grid-column:1 / -1; }
.project-browser__projects:not(.filters-expanded) :deep(.table-multi-filter),
.project-browser__files:not(.filters-expanded) :deep(.table-multi-filter) { display:none; }
.project-browser__projects :deep(.unified-list-template__toolbar:empty),
.project-browser__files :deep(.unified-list-template__toolbar:empty) { display:none; }
.project-browser__files :deep(.unified-list-template__filter-panel) { display:grid; grid-template-columns:minmax(0, 1fr) auto; align-items:center; width:100%; }
.project-browser__projects :deep(.unified-list-template__custom-query > .unified-list-template__filter-panel),
.project-browser__files :deep(.unified-list-template__custom-query > .unified-list-template__filter-panel) { flex:none; }
.project-browser__files :deep(.unified-list-template__filters) { overflow:visible; }
.project-browser__files :deep(.table-multi-filter) { grid-column:1 / -1; grid-row:2; }
.project-browser__files :deep(.list-filter-actions) { grid-column:2; grid-row:1; flex-wrap:nowrap; }
.project-browser__files :deep(.unified-list-template__filters > .project-browser__file-search) { display:flex; flex-wrap:nowrap; gap:12px; }
.project-browser__files :deep(.project-browser__file-search > .el-input) { width:auto !important; min-width:0; flex:1 1 220px; }
.version-view { display:flex; align-items:center; gap:8px; white-space:nowrap; }
.project-browser__files :deep(.version-view .el-select) { width:210px !important; }
.project-browser__files :deep(.unified-list-template__custom-query > .unified-list-template__toolbar) { display:flex; flex-wrap:wrap; gap:8px; width:100%; max-width:100%; }
.project-browser__files :deep(.dcc-project-references__toolbar) { display:none; }
.project-browser__files :deep(.unified-list-template__table-shell) { flex:none; max-height:58dvh; overflow:auto; }
.project-browser__files :deep(.el-table) { height:auto; overflow:visible; }
.project-browser__files :deep(.el-table__header-wrapper) { position:sticky; top:0; z-index:2; }
.project-browser__files :deep(.el-table__inner-wrapper) { overflow:visible; }
.project-browser :deep(.el-pagination) { margin:12px 0 0; flex-wrap:wrap; gap:4px; }
@media (max-width:1450px) {
  .project-browser__files :deep(.unified-list-template__filter-panel) { grid-template-columns:minmax(0, 1fr); }
  .project-browser__files :deep(.list-filter-actions) { grid-column:1; grid-row:3; }
  .project-browser__files :deep(.unified-list-template__filters > .project-browser__file-search) { flex-wrap:wrap; }
  .project-browser__files :deep(.project-browser__file-search > .el-input) { min-width:180px; }
}
@media (max-width:900px) { .project-browser { grid-template-columns:1fr; height:auto; overflow:visible; } aside { border-right:0; border-bottom:1px solid var(--el-border-color-light); } }
</style>
