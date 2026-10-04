<template>
  <el-dialog :model-value="modelValue" :title="title" width="1100px" append-to-body @close="close">
    <div class="source" data-testid="current-file">
      当前文件：{{ source.fileName }} · {{ source.fileNumber }} · {{ source.versionNo }}
      <span>{{ source.projectName }} / {{ source.folderName }}</span>
      <el-tag v-if="source.unsubmitted" type="info">尚未提交</el-tag>
    </div>
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" show-icon />
    <div class="workspace">
      <aside>
        <h4>候选项目目录</h4>
        <div class="search"><el-input v-model="navigation.keyword" placeholder="项目名称或编码" clearable @keyup.enter="navigation.searchProjects()" /><el-button :loading="navigation.projectLoading" @click="navigation.searchProjects()">查找项目</el-button></div>
        <el-alert v-if="navigation.projectError" :title="navigation.projectError" type="error" :closable="false" />
        <el-table data-testid="dcc-selector-project-list" :data="navigation.projects" v-loading="navigation.projectLoading" highlight-current-row @row-click="selectProject">
          <el-table-column prop="projectName" label="项目" /><el-table-column prop="projectCode" label="编码" />
        </el-table>
        <el-pagination v-model:current-page="navigation.pageNo" data-testid="dcc-selector-project-pagination" :page-size="navigation.pageSize" :total="navigation.total" layout="total, prev, next" @current-change="navigation.loadProjects()" />
        <h4>{{ navigation.projectName || '请选择候选项目' }}</h4>
        <el-alert v-if="navigation.directoryError" :title="navigation.directoryError" type="error" :closable="false" />
        <el-tree :key="JSON.stringify([source.contextKey, navigation.projectId])" v-loading="navigation.directoryLoading" :data="navigation.directories" node-key="key" :props="{ label: 'name', children: 'children' }" :current-node-key="directoryPath.at(-1)" :default-expanded-keys="directoryPath.slice(0, -1)" highlight-current @node-click="directoryClick" />
        <el-empty v-if="navigation.projectId && !navigation.directoryLoading && !navigation.directoryError && !navigation.directories.length" description="候选项目尚无文件夹" :image-size="50" />
      </aside>
      <section>
        <el-tabs v-model="scope" @tab-change="scopeChanged"><el-tab-pane label="项目目录" name="directory" /><el-tab-pane label="全局搜索" name="global" /></el-tabs>
        <div class="search"><el-input v-model="keyword" placeholder="文件名称、编号或项目名称" clearable @keyup.enter="search" /><el-button :loading="state.loading" @click="search">搜索</el-button></div>
        <p v-if="scope === 'directory' && !folderId">请选择候选项目的逻辑文件夹，或切换全局搜索。</p>
        <el-table :data="state.rows" v-loading="state.loading" row-key="masterId" height="300">
          <el-table-column label="选择" width="64"><template #default="{ row }"><el-checkbox :model-value="state.selected.some(item => item.masterId === row.masterId)" :disabled="state.saving || (purpose === 'relations' && row.masterId === source.masterId) || !row.controlled" @change="value => choose(row, Boolean(value))" /></template></el-table-column>
          <el-table-column prop="fileName" label="名称" min-width="180" /><el-table-column prop="fileNumber" label="编号" width="140" />
          <el-table-column prop="projectName" label="来源项目" min-width="120" /><el-table-column prop="folderName" label="文件夹" min-width="100" />
          <el-table-column prop="versionNo" label="受控版本" width="110" />
          <el-table-column label="状态" width="120"><template #default="{ row }">{{ state.label(row) }}</template></el-table-column>
          <el-table-column label="正文" width="85"><template #default="{ row }"><el-button link :disabled="!row.canPreview" @click="preview(row)">查看</el-button></template></el-table-column>
        </el-table>
        <el-pagination v-model:current-page="pageNo" :page-size="pageSize" :total="state.total" layout="total, prev, pager, next" @current-change="load" />
      </section>
    </div>
    <slot name="confirmation-fields"></slot>
    <div class="selected"><strong>已选 {{ state.selected.length }} 项</strong><el-tag v-for="row in state.selected" :key="row.masterId" :closable="!state.saving" @close="state.remove(row.masterId)">{{ row.fileName }} · {{ row.versionNo }} · {{ state.label(row) }}</el-tag></div>
    <template #footer><el-button :disabled="state.saving" @click="close">取消</el-button><el-button type="primary" :loading="state.saving" @click="confirm">{{ confirmLabel }}</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch, onBeforeUnmount } from 'vue'
import { FileSelectorState } from './selector-state'
import { SelectorProjectDirectoryState } from './selector-project-directory'
import type { DccDiscoveredProject } from '@/api/dcc/controlledFile/projectDiscovery'
import type { FileCandidate, SelectorContext, SelectorPage, SelectorQuery } from './selector-state'
export interface SelectorSource extends SelectorContext { controlledFileId?: string; fileName: string; fileNumber: string; versionNo: string; projectId: string; folderId?: string; projectName: string; folderName: string; unsubmitted?: boolean }
export interface DirectoryNode { key: string; name: string; projectId: string; folderId?: string; children?: DirectoryNode[] }
const props = defineProps<{ modelValue: boolean; source: SelectorSource; purpose: 'relations' | 'reference' | 'operation'; directories: DirectoryNode[]; selected: FileCandidate[]; loadPage: (query: SelectorQuery) => Promise<SelectorPage>; persist: (rows: FileCandidate[]) => Promise<unknown>; openPreview: (row: FileCandidate) => Promise<unknown> }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; confirmed: [rows: FileCandidate[]] }>()
const state = reactive(new FileSelectorState(props.source))
let navigation = reactive(new SelectorProjectDirectoryState(props.source.tenantId))
const invalidateFilePage = () => { const token = state.begin({ keyword: keyword.value, pageNo: pageNo.value, pageSize }); state.resolve(token, { list: [], total: 0 }) }
const title = computed(() => ({ relations: '文件选择器 · 关联文件', reference: '文件选择器 · 引用文件', operation: '文件选择器 · 文件操作' })[props.purpose])
const confirmLabel = computed(() => ({ relations: '确认关联', reference: '确认引用', operation: '打开操作面板' })[props.purpose])
const scope = ref('directory'), keyword = ref(''), pageNo = ref(1), pageSize = 20
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
  const token = state.begin({ keyword: keyword.value, pageNo: pageNo.value, pageSize,
    projectId: scope.value === 'directory' ? projectId.value : undefined,
    folderId: scope.value === 'directory' ? folderId.value : undefined })
  try { state.resolve(token, await props.loadPage(token.query)) } catch (error) { state.reject(token, error) }
}
const search = () => { pageNo.value = 1; return load() }
const scopeChanged = () => search()
const selectProject = async (project: DccDiscoveredProject) => {
  if (!props.modelValue) return
  scope.value = 'directory'; projectId.value = project.id; folderId.value = undefined; pageNo.value = 1; invalidateFilePage()
  await navigation.selectProject(project)
}
const directoryClick = (node: DirectoryNode) => {
  if (!props.modelValue) return
  if (!node.folderId) return selectProject({ id: node.projectId, projectName: node.name } as DccDiscoveredProject)
  try { navigation.selectFolder(node); scope.value = 'directory'; projectId.value = navigation.projectId!; folderId.value = navigation.folderId; return search() }
  catch (cause) { navigation.directoryError = cause instanceof Error ? cause.message : String(cause) }
}
const choose = (row: FileCandidate, checked: boolean) => { try { checked ? state.select(row) : state.remove(row.masterId) } catch (error) { state.error = String(error) } }
const preview = async (row: FileCandidate) => {
  const context = state.context.contextKey, generation=state.contextGeneration
  try { state.assertPreview(row); await props.openPreview({ ...row }) } catch (error) { if (context === state.context.contextKey && generation===state.contextGeneration) state.error = String(error) }
}
const close = () => { navigation.close(); state.reset(props.source, []); emit('update:modelValue', false) }
const confirm = async () => { if (await state.save(props.persist, true)) { emit('confirmed', state.selected.map(row => ({ ...row }))); close() } }
watch(() => [props.modelValue, props.source.contextKey, props.purpose] as const, async ([open]) => {
  navigation.close()
  if (navigation.tenantId !== props.source.tenantId) navigation = reactive(new SelectorProjectDirectoryState(props.source.tenantId))
  state.reset({ ...props.source, selectionMode: props.purpose === 'operation' ? 'single' : 'multiple', forbidSelfRelation: props.purpose === 'relations', allowEmptySelection: props.purpose === 'relations' }, open ? props.selected : []); keyword.value = ''; pageNo.value = 1; scope.value = 'directory'
  projectId.value = props.source.projectId; folderId.value = props.source.folderId
  if (open) {
    const context = props.source.contextKey, generation = state.contextGeneration
    const initialNavigationCurrent = await navigation.open(props.source)
    if (!props.modelValue || context !== props.source.contextKey || generation !== state.contextGeneration) return
    if (!initialNavigationCurrent) return
    projectId.value = navigation.projectId!; folderId.value = navigation.folderId
    if (folderId.value) await load()
  }
}, { immediate: true })
onBeforeUnmount(() => { navigation.close(); state.reset(props.source, []) })
</script>
<style scoped>
.source { padding: 12px; background: var(--el-fill-color-light); margin-bottom: 12px; display: flex; gap: 12px; flex-wrap: wrap; }
.workspace { display: grid; grid-template-columns: 240px minmax(0, 1fr); gap: 18px; }
aside { border-right: 1px solid var(--el-border-color); max-height: 440px; overflow: auto; }
.search { display: flex; gap: 8px; margin-bottom: 12px; }
.selected { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 16px; }
</style>
