<template>
  <el-dialog :class="{ 'supplemental-selector': supplemental }" :close-on-click-modal="!state.saving" :close-on-press-escape="!state.saving" :show-close="!state.saving" :model-value="modelValue" :title="title" width="1100px" append-to-body @close="close">
    <div class="source" data-testid="current-file">
      当前文件：{{ source.fileName }} · {{ source.fileNumber }} · {{ source.versionNo }}
      <span>{{ source.projectName }} / {{ source.folderName }}</span>
      <el-tag v-if="source.unsubmitted" type="info">尚未提交</el-tag>
    </div>
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" show-icon />
    <div class="workspace">
      <aside>
        <h4>{{ supplemental ? '项目目录' : '候选项目目录' }}</h4>
        <button v-if="supplemental" type="button" class="all-directories" :class="{ active: scope === 'global' }" :aria-pressed="scope === 'global'" @click="allDirectories"><span>全部目录</span><span class="directory-count" v-if="directoryCounts['*'] !== undefined">{{ directoryCounts['*'] }}</span></button>
        <div v-if="purpose !== 'relations'" class="search"><el-input v-model="navigation.keyword" placeholder="项目名称或编码" clearable @keyup.enter="navigation.searchProjects()" /><el-button :loading="navigation.projectLoading" @click="navigation.searchProjects()">查找项目</el-button></div>
        <el-alert v-if="navigation.projectError" :title="navigation.projectError" type="error" :closable="false" />
        <el-table v-if="purpose !== 'relations'" data-testid="dcc-selector-project-list" :data="navigation.projects" v-loading="navigation.projectLoading" highlight-current-row @row-click="selectProject">
          <el-table-column prop="projectName" label="项目" /><el-table-column prop="projectCode" label="编码" />
        </el-table>
        <el-pagination v-if="purpose !== 'relations'" v-model:current-page="navigation.pageNo" data-testid="dcc-selector-project-pagination" :page-size="navigation.pageSize" :total="navigation.total" layout="total, prev, next" @current-change="navigation.loadProjects()" />
        <h4>{{ navigation.projectName || '请选择候选项目' }}</h4>
        <el-alert v-if="navigation.directoryError" :title="navigation.directoryError" type="error" :closable="false" />
        <el-tree :class="{ 'supplemental-directories': supplemental }" :key="JSON.stringify([source.contextKey, navigation.projectId])" v-loading="navigation.directoryLoading" :data="navigation.directories" node-key="key" :props="{ label: 'name', children: 'children' }" :current-node-key="supplemental && scope === 'global' ? null : directoryPath.at(-1)" :default-expanded-keys="directoryPath.slice(0, -1)" :highlight-current="!supplemental || scope === 'directory'" @node-click="directoryClick">
          <template v-if="supplemental" #default="{ data }">
            <span class="directory-node"><span>{{ data.name }}</span><span class="directory-node__meta"><span v-if="state.selected.some(row => row.projectFolderId === data.folderId)" class="selection-dot" aria-label="此目录有本次选择">●</span><span v-if="directoryCounts[data.folderId] !== undefined" class="directory-count" title="此目录直接包含的可见受控文件数">{{ directoryCounts[data.folderId] }}</span></span></span>
          </template>
        </el-tree>
        <el-empty v-if="navigation.projectId && !navigation.directoryLoading && !navigation.directoryError && !navigation.directories.length" description="候选项目尚无文件夹" :image-size="50" />
      </aside>
      <section>
        <el-tabs v-if="!supplemental" v-model="scope" @tab-change="scopeChanged"><el-tab-pane label="项目目录" name="directory" /><el-tab-pane :label="purpose === 'relations' ? '本项目搜索' : '全局搜索'" name="global" /></el-tabs>
        <div class="search"><el-input v-model="keyword" :placeholder="supplemental ? '搜索本项目内的文件名称或编号' : '文件名称、编号或项目名称'" clearable @clear="search" @keyup.enter="search" /><el-button :loading="state.loading" @click="search">搜索</el-button><el-tag v-if="supplemental" type="info">{{ scope === 'global' ? '全部目录' : activeFolderName }}</el-tag></div>
        <p v-if="scope === 'directory' && !folderId">请选择候选项目的逻辑文件夹，或切换{{ purpose === 'relations' ? '本项目搜索' : '全局搜索' }}。</p>
        <el-table :data="state.rows" v-loading="state.loading" :data-user-table-column-explicit="supplemental ? '' : undefined" row-key="masterId" :height="supplemental ? 440 : 300">
          <template v-if="supplemental" #empty>
            <span v-if="!state.loading && !navigation.directoryLoading && !state.error && !navigation.directoryError">{{ queriedKeyword.trim() ? '未找到匹配文件' : '当前目录无可选文件' }}</span>
          </template>
          <el-table-column label="选择" width="64"><template #default="{ row }"><el-checkbox :model-value="state.isLocked(row.masterId) || state.selected.some(item => item.masterId === row.masterId)" :disabled="state.saving || state.isLocked(row.masterId) || (supplemental && !row.canPreview) || (purpose === 'relations' && (row.masterId === source.masterId || row.projectId !== source.projectId)) || !row.controlled || row.status !== 'ACTIVE' || row.pendingEffect || !row.executable" @change="value => choose(row, Boolean(value))" /></template></el-table-column>
          <el-table-column v-if="supplemental" label="文件名称与编号" min-width="280"><template #default="{ row }">
            <div class="candidate-name">{{ row.fileName }} <el-tag v-if="row.masterId === source.masterId" type="warning" size="small">当前文件</el-tag><el-tag v-else-if="state.isLocked(row.masterId)" type="info" size="small">原有关联</el-tag></div>
            <div class="candidate-meta">{{ row.fileNumber }} · {{ row.folderName }}</div>
          </template></el-table-column>
          <template v-else>
            <el-table-column prop="fileName" label="文件名称" min-width="180" /><el-table-column prop="fileNumber" label="文件编号" width="140" />
            <el-table-column prop="projectName" label="来源项目" min-width="120" /><el-table-column prop="folderName" label="文件夹" min-width="100" />
          </template>
          <el-table-column prop="versionNo" label="受控版本" width="110" />
          <el-table-column label="状态" width="120"><template #default="{ row }">{{ state.label(row) }}</template></el-table-column>
          <el-table-column v-if="!supplemental" label="正文" width="85"><template #default="{ row }"><el-button link :disabled="!row.canPreview" @click="preview(row)">查看</el-button></template></el-table-column>
        </el-table>
        <el-pagination v-if="!supplemental || state.total > 0" v-model:current-page="pageNo" :page-size="pageSize" :total="state.total" layout="total, prev, pager, next" @current-change="load" />
      </section>
    </div>
    <slot name="confirmation-fields"></slot>
    <div v-if="!supplemental" class="selected">
      <strong>已选 {{ state.selected.length }} 项</strong>
      <el-tag v-for="row in state.selected" :key="row.masterId" :closable="!state.saving" @close="state.remove(row.masterId)">{{ row.fileName }} · {{ row.versionNo }} · {{ state.label(row) }}</el-tag>
    </div>
    <template #footer>
      <div :class="{ 'selector-footer': supplemental }">
      <div v-if="supplemental" class="selection-summary">
        <div v-if="selectedExpanded" id="dcc-supplemental-selection" class="selected-popover">
          <div class="selected-popover__header"><strong>本次已选补充文件清单</strong><el-button link type="danger" :disabled="state.saving || !state.selected.length" @click="state.clear()">清空本次选择</el-button></div>
          <el-empty v-if="!state.selected.length" description="尚未选择补充文件" :image-size="40" />
          <div v-for="row in state.selected" :key="row.masterId" class="selected-file">
            <div class="selected-file__name">{{ row.fileName }}<div class="candidate-meta">文件编号：{{ row.fileNumber }} · 关联版本：{{ row.versionNo }} · {{ row.folderName }}</div></div>
            <el-button link type="primary" :disabled="state.saving" :aria-label="`移除 ${row.fileName}`" @click="state.remove(row.masterId)">移除</el-button>
          </div>
        </div>
        <el-button :aria-expanded="selectedExpanded" aria-controls="dcc-supplemental-selection" @click="selectedExpanded = !selectedExpanded">已选 {{ state.selected.length }} 项 {{ selectedExpanded ? '⌄' : '⌃' }}</el-button>
      </div>
      <div class="selector-actions"><el-button :disabled="state.saving" @click="close">取消</el-button><el-button type="primary" :loading="state.saving" @click="confirm">{{ confirmLabel }}</el-button></div>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch, onBeforeUnmount } from 'vue'
import { FileSelectorState } from './selector-state'
import { SelectorProjectDirectoryState } from './selector-project-directory'
import type { DccDiscoveredProject } from '@/api/dcc/controlledFile/projectDiscovery'
import type { FileCandidate, SelectorContext, SelectorPage, SelectorQuery } from './selector-state'
export interface SelectorSource extends SelectorContext { controlledFileId?: string; fileName: string; fileNumber: string; versionNo: string; projectId: string; folderId?: string; projectName: string; folderName: string; unsubmitted?: boolean }
export interface LockedRelation { masterId: string; fileName?: string; fileNumber?: string; versionNo?: string }
export interface DirectoryNode { key: string; name: string; projectId: string; folderId?: string; children?: DirectoryNode[] }
const props = defineProps<{ modelValue: boolean; source: SelectorSource; purpose: 'relations' | 'reference' | 'operation'; supplemental?: boolean; lockedRelations?: LockedRelation[]; directories: DirectoryNode[]; selected: FileCandidate[]; loadPage: (query: SelectorQuery) => Promise<SelectorPage>; persist: (rows: FileCandidate[]) => Promise<unknown>; openPreview: (row: FileCandidate) => Promise<unknown> }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; confirmed: [rows: FileCandidate[]] }>()
const selectionContext = (): SelectorContext => ({ ...props.source,
  relationProjectId: props.purpose === 'relations' ? props.source.projectId : undefined,
  selectionMode: props.purpose === 'operation' ? 'single' : 'multiple',
  lockedMasterIds: props.supplemental ? props.lockedRelations?.map(row => row.masterId) : undefined, requirePreview: props.supplemental,
  forbidSelfRelation: props.purpose === 'relations', allowEmptySelection: props.purpose === 'relations' })
const state = reactive(new FileSelectorState(selectionContext()))
let navigation = reactive(new SelectorProjectDirectoryState(props.source.tenantId))
const invalidateFilePage = () => { const token = state.begin({ keyword: keyword.value, pageNo: pageNo.value, pageSize }); state.resolve(token, { list: [], total: 0 }) }
const title = computed(() => props.supplemental ? '补充关联文件' : ({ relations: '文件选择器 · 关联文件', reference: '文件选择器 · 引用文件', operation: '文件选择器 · 文件操作' })[props.purpose])
const confirmLabel = computed(() => ({ relations: '确认关联', reference: '确认引用', operation: '打开操作面板' })[props.purpose])
const scope = ref('directory'), keyword = ref(''), pageNo = ref(1), pageSize = 20
const queriedKeyword = ref('')
const selectedExpanded = ref(false), directoryCounts = ref<Record<string, number>>({})
const activeFolderName = computed(() => {
  const find = (nodes: DirectoryNode[]): string | undefined => { for (const node of nodes) { if (node.folderId === folderId.value) return node.name; const name = find(node.children || []); if (name) return name } }
  return find(navigation.directories) || '项目目录'
})
const projectId = ref(props.source.projectId), folderId = ref(props.source.folderId)
const findDirectoryPath=(nodes:DirectoryNode[],parents:string[]):string[]=>{
  for(const node of nodes){
    const path=[...parents,node.key]
    if(node.projectId===projectId.value&&node.folderId===folderId.value)return path
    if(node.children){const found=findDirectoryPath(node.children,path);if(found.length)return found}
  }
  return []
}
const directoryPath=computed(()=>findDirectoryPath(navigation.directories,[]))
const load = async () => {
  if (!props.modelValue) return
  if (scope.value === 'directory' && (!projectId.value || !folderId.value)) { invalidateFilePage(); return }
  queriedKeyword.value = keyword.value
  const token = state.begin({ keyword: keyword.value, pageNo: pageNo.value, pageSize,
    projectId: props.purpose === 'relations' ? props.source.projectId : scope.value === 'directory' ? projectId.value : undefined,
    folderId: scope.value === 'directory' ? folderId.value : undefined })
  try {
    const page = await props.loadPage(token.query)
    if (state.resolve(token, page) && props.supplemental && !token.query.keyword.trim()) directoryCounts.value[token.query.folderId || '*'] = page.total
  } catch (error) { state.reject(token, error) }
}
const search = () => { if (props.supplemental && keyword.value.trim()) scope.value = 'global'; pageNo.value = 1; return load() }
const scopeChanged = () => search()
const allDirectories = () => { scope.value = 'global'; keyword.value = ''; return search() }
const selectProject = async (project: DccDiscoveredProject) => {
  if (!props.modelValue) return
  if (props.purpose === 'relations' && project.id !== props.source.projectId) { navigation.directoryError = '关联只可选择本项目文件'; return }
  scope.value = 'directory'; projectId.value = project.id; folderId.value = undefined; pageNo.value = 1; invalidateFilePage()
  await navigation.selectProject(project)
}
const directoryClick = (node: DirectoryNode) => {
  if (!props.modelValue) return
  if (!node.folderId) return selectProject({ id: node.projectId, projectName: node.name } as DccDiscoveredProject)
  try { navigation.selectFolder(node); if (props.supplemental) keyword.value = ''; scope.value = 'directory'; projectId.value = navigation.projectId!; folderId.value = navigation.folderId; return search() }
  catch (cause) { navigation.directoryError = cause instanceof Error ? cause.message : String(cause) }
}
const choose = (row: FileCandidate, checked: boolean) => { try { checked ? state.select(row) : state.remove(row.masterId) } catch (error) { state.error = String(error) } }
const preview = async (row: FileCandidate) => {
  const context = state.context.contextKey, generation=state.contextGeneration
  try { state.assertPreview(row); await props.openPreview({ ...row }) } catch (error) { if (context === state.context.contextKey && generation===state.contextGeneration) state.error = String(error) }
}
const close = () => { if (state.saving) return; navigation.close(); state.reset(selectionContext(), []); emit('update:modelValue', false) }
const confirm = async () => { if (await state.save(props.persist, true)) { emit('confirmed', state.selected.map(row => ({ ...row }))); close() } }
watch(() => [props.modelValue, props.source.contextKey, props.purpose] as const, async ([open]) => {
  navigation.close()
  if (navigation.tenantId !== props.source.tenantId) navigation = reactive(new SelectorProjectDirectoryState(props.source.tenantId))
  try { state.reset(selectionContext(), open ? props.selected.filter(row => !props.supplemental || !props.lockedRelations?.some(locked => locked.masterId === row.masterId)) : []) }
  catch (cause) { state.error = cause instanceof Error ? cause.message : String(cause); return }
  queriedKeyword.value = ''; keyword.value = ''; pageNo.value = 1; scope.value = props.supplemental ? 'global' : 'directory'; selectedExpanded.value = false; directoryCounts.value = {}
  projectId.value = props.source.projectId; folderId.value = props.source.folderId
  if (open) {
    const context = props.source.contextKey, generation = state.contextGeneration
    const initialNavigationCurrent = await navigation.open(props.source, props.purpose !== 'relations')
    if (!props.modelValue || context !== props.source.contextKey || generation !== state.contextGeneration) return
    if (!initialNavigationCurrent) return
    projectId.value = navigation.projectId!; folderId.value = navigation.folderId
    if (props.supplemental || folderId.value) await load()
  }
}, { immediate: true })
onBeforeUnmount(() => { navigation.close(); state.reset(selectionContext(), []) })
</script>
<style scoped>
.source { padding: 12px; background: var(--el-fill-color-light); margin-bottom: 12px; display: flex; gap: 12px; flex-wrap: wrap; }
.workspace { display: grid; grid-template-columns: 240px minmax(0, 1fr); gap: 18px; }
aside { border-right: 1px solid var(--el-border-color); max-height: 440px; overflow: auto; }
.search { display: flex; gap: 8px; margin-bottom: 12px; }
.selected { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 16px; }
.selected .el-tag { height: auto; min-height: 24px; white-space: normal; overflow-wrap: anywhere; }
.selected-file { box-sizing: border-box; width: 100%; display: flex; flex-wrap: wrap; align-items: center; gap: 12px 24px; padding: 12px; border: 1px solid var(--el-border-color); border-radius: 4px; overflow-wrap: anywhere; }
.selected-file__name { flex: 1; min-width: 180px; }
.selected-file .el-button { margin-left: auto; }
.all-directories { box-sizing: border-box; width: 100%; height: 26px; padding: 0 12px 0 24px; border: 0; border-radius: 0; background: transparent; color: var(--el-tree-text-color, var(--el-text-color-regular)); font: inherit; text-align: left; cursor: pointer; display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.all-directories:hover { background: var(--el-tree-node-hover-bg-color, var(--el-fill-color-light)); }
.all-directories:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: -2px; }
.all-directories.active { color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.directory-node { box-sizing: border-box; display: flex; justify-content: space-between; align-items: center; width: 100%; min-width: 0; padding-right: 12px; gap: 8px; }
.directory-node > span:first-child { min-width: 0; flex: 1; text-align: left; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.directory-node__meta { display: flex; flex-shrink: 0; gap: 6px; align-items: center; }
.supplemental-directories.el-tree--highlight-current :deep(.el-tree-node.is-current > .el-tree-node__content) { color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.directory-count { border-radius: 10px; padding: 0 5px; background: var(--el-fill-color); font-size: 12px; }
.selection-dot { color: var(--el-color-primary); font-size: 10px; }
.candidate-name { overflow-wrap: anywhere; }
.candidate-meta { color: var(--el-text-color-secondary); font-size: 12px; overflow-wrap: anywhere; }
.selector-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.selector-actions { display: inline-flex; align-items: center; flex-shrink: 0; }
.selection-summary { display: inline-flex; align-items: center; position: relative; text-align: left; }
.selected-popover { position: absolute; bottom: calc(100% + 12px); left: 0; width: 420px; max-width: calc(100vw - 64px); max-height: 300px; overflow: auto; background: var(--el-bg-color-overlay); border: 1px solid var(--el-border-color); border-radius: 10px; box-shadow: var(--el-box-shadow-light); z-index: 2; }
.selected-popover__header { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; border-bottom: 1px solid var(--el-border-color); }
.selected-popover .selected-file { border: 0; border-radius: 0; border-bottom: 1px solid var(--el-border-color-lighter); gap: 8px; }
.supplemental-selector .workspace { gap: 16px; }
@media (max-width: 768px) {
  .workspace { grid-template-columns: 180px minmax(0, 1fr); }
}
</style>
