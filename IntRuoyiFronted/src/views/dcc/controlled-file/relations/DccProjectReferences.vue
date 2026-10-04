<template>
  <div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-button v-if="isProjectLeader" :disabled="busy || loading || savedReadFailure" @click="openSelector">引用</el-button>
    <el-button v-if="savedReadFailure" :disabled="busy || loading" @click="load">刷新已保存引用</el-button>
    <DccFileSelector v-model="selectorVisible" :source="selectorSource" purpose="reference" :directories="directories" :selected="[]" :load-page="loadPage" :open-preview="openPreview" :persist="createSelected" @confirmed="referencesCreated">
      <template #confirmation-fields><el-input v-model="referenceReason" :disabled="busy" placeholder="引用原因（必填）" type="textarea" /></template>
    </DccFileSelector>
    <el-table :data="rows" v-loading="loading">
      <el-table-column label="文件">
        <template #default="{ row }">
          <span class="reference-file-name is-reference" data-testid="dcc-project-reference-file-name">{{ row.selectedVersion.fileName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="selectedVersion.versionNo" label="所选受控版本" />
      <el-table-column prop="selectedVersion.folderName" label="源文件夹" />
      <el-table-column label="引用"><template #default="{ row }"><DccReferenceBadge :is-reference="true" :project-count="row.referenceProjectCount" :source-project-name="row.sourceProjectName" :obsolete="row.selectedVersion.status === 'OBSOLETE'" :pending-effect="row.selectedVersion.pendingEffect" /><el-button link data-testid="dcc-saved-reference-usage" :disabled="busy || loading" @click="emit('show-usage', row)">使用明细</el-button></template></el-table-column>
      <el-table-column v-if="openReferencePreview || openReferenceTrace" label="只读查看">
        <template #default="{ row }">
          <el-button
            v-if="openReferencePreview" link data-testid="dcc-project-reference-preview"
            :disabled="busy || loading || Boolean(readingReferenceId) || row.selectedVersion.canPreview !== true"
            :title="row.selectedVersion.canPreview === true ? '浏览固定引用版本正文' : '没有固定引用版本正文查看权限'"
            @click="readSavedReference(row, 'preview')">正文浏览</el-button>
          <el-button
            v-if="openReferenceTrace" link data-testid="dcc-project-reference-trace"
            :loading="readingReferenceId === row.reference.id"
            :disabled="busy || loading || Boolean(readingReferenceId)"
            title="按固定引用版本独立校验只读详情权限"
            @click="readSavedReference(row, 'trace')">只读追溯</el-button>
        </template>
      </el-table-column>
      <el-table-column v-if="isProjectLeader" label="操作"><template #default="{ row }"><el-button link :disabled="busy || loading" @click="prepareCancel(row)">取消引用</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="confirmVisible" title="二次确认：取消引用" width="520px" append-to-body>
      <p>{{ pending?.selectedVersion.fileName }} · 来源项目 {{ pending?.sourceProjectName }}</p>
      <p>当前文件夹：{{ folderName }}。确认后移除此文件夹的引用入口。</p>
      <el-input v-model="reason" :disabled="busy" placeholder="取消原因（必填）" type="textarea" />
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <template #footer><el-button :disabled="busy" @click="confirmVisible = false">返回</el-button><el-button type="danger" :loading="busy" :disabled="!reason.trim()" @click="cancelConfirmed">确认取消引用</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import DccReferenceBadge from './DccReferenceBadge.vue'
import DccFileSelector from './DccFileSelector.vue'
import type { SelectorSource, DirectoryNode } from './DccFileSelector.vue'
import type { FileCandidate, SelectorPage, SelectorQuery } from './selector-state'
import type { ReferenceRow } from './project-reference-contract'
const props = defineProps<{ contextKey: string; projectId: string; folderId: string; folderName: string; isProjectLeader: boolean; selectorSource: SelectorSource; directories: DirectoryNode[]; loadPage: (query: SelectorQuery) => Promise<SelectorPage>; openPreview: (row: FileCandidate) => Promise<unknown>;
  openReferencePreview?: (row: ReferenceRow, isCurrent: () => boolean) => Promise<unknown>;
  openReferenceTrace?: (row: ReferenceRow, isCurrent: () => boolean) => Promise<unknown>;
  createReferences: (projectId: string, folderId: string, selectedFileIds: string[], reason: string) => Promise<ReferenceRow[]>; loadReferences: (projectId: string, folderId: string) => Promise<ReferenceRow[]>; cancelReference: (projectId: string, folderId: string, masterId: string, referenceId: string, reason: string) => Promise<number> }>()
const emit = defineEmits<{ 'select-files': []; changed: [projectCount: number, masterId: string]; 'saved-read-failure': []; 'show-usage': [row: ReferenceRow] }>()
const rows = ref<ReferenceRow[]>([]), error = ref(''), loading = ref(false), busy = ref(false), confirmVisible = ref(false), reason = ref(''), pending = ref<ReferenceRow>()
let generation = 0
const readingReferenceId = ref('')
const savedReadFailure = ref(false)
let readSequence = 0
const readSavedReference = async (row: ReferenceRow, action: 'preview' | 'trace') => {
  if (busy.value || loading.value || readingReferenceId.value) return
  const selected = rows.value.find(saved => saved.reference.id === row.reference.id)
  const handler = action === 'preview' ? props.openReferencePreview : props.openReferenceTrace
  if (!selected || !handler || action === 'preview' && selected.selectedVersion.canPreview !== true) return
  const context = props.contextKey, token = generation, sequence = ++readSequence
  const captured: ReferenceRow = JSON.parse(JSON.stringify(selected))
  const current = () => context === props.contextKey && token === generation && sequence === readSequence
    && rows.value.some(saved => saved.reference.id === captured.reference.id
      && saved.reference.selectedControlledFileId === captured.reference.selectedControlledFileId)
  readingReferenceId.value = captured.reference.id; error.value = ''
  try { await handler(captured, current) }
  catch (cause) { if (current()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (current()) readingReferenceId.value = '' }
}
const referenceReason=ref(''),selectorVisible=ref(false)
const openSelector=()=>{if(!props.isProjectLeader || busy.value || loading.value || savedReadFailure.value)return;referenceReason.value='';selectorVisible.value=true;emit('select-files')}
const createSelected=async (selected: FileCandidate[]) => {
  if(!props.isProjectLeader)throw new Error('仅当前项目正式负责人可以引用')
  if(busy.value || loading.value || savedReadFailure.value)throw new Error('请等待当前引用操作或已保存引用刷新完成')
  if(!referenceReason.value.trim())throw new Error('请填写引用原因')
  const context=props.contextKey,project=props.projectId,folder=props.folderId,token=generation
  busy.value=true
  try {
    const created=await props.createReferences(project,folder,selected.map(row=>row.controlledFileId),referenceReason.value)
    if(context!==props.contextKey || token!==generation)throw new Error('引用结果所属上下文已变化，请在原目录核对保存结果')
    const result=new Map(rows.value.map(row=>[row.reference.id,row]))
    created.forEach(row=>result.set(row.reference.id,row));rows.value=Array.from(result.values())
    created.forEach(row=>emit('changed',row.referenceProjectCount,row.reference.masterId))
  } catch (cause) {
    if (token === generation && (cause as { referenceSaved?: unknown })?.referenceSaved === true) {
      selectorVisible.value = false; savedReadFailure.value = true
      error.value = cause instanceof Error ? cause.message : String(cause)
      emit('saved-read-failure')
      return
    }
    throw cause
  } finally { if(token===generation)busy.value=false }
}
const referencesCreated=()=>{selectorVisible.value=false}
const load = async () => {
  const token = ++generation; const project = props.projectId, folder = props.folderId
  loading.value = true; error.value = ''; rows.value = []
  try { const result = await props.loadReferences(project, folder); if (token === generation) { rows.value = result; savedReadFailure.value = false } }
  catch (e) { if (token === generation) error.value = savedReadFailure.value ? `引用已保存，但引用列表读取失败：${String(e)}` : String(e) }
  finally { if (token === generation) loading.value = false }
}
const prepareCancel = (row: ReferenceRow) => { if(!props.isProjectLeader || busy.value || loading.value)return;pending.value = row; reason.value = ''; error.value = ''; confirmVisible.value = true }
const cancelConfirmed = async () => {
  if (!props.isProjectLeader || !pending.value || busy.value || !reason.value.trim()) return
  const token = generation, project = props.projectId, folder = props.folderId, master = pending.value.reference.masterId, referenceId = pending.value.reference.id
  busy.value = true; error.value = ''
  try {
    const count = await props.cancelReference(project, folder, master, referenceId, reason.value)
    if (token !== generation) return
    rows.value = rows.value.filter(row => row.reference.masterId !== master); confirmVisible.value = false; emit('changed', count,master)
  } catch (e) { if (token === generation) error.value = String(e) }
  finally { if (token === generation) busy.value = false }
}
watch(() => props.contextKey, () => { generation++; readSequence++; savedReadFailure.value = false; readingReferenceId.value = ''; busy.value = false; confirmVisible.value = false; selectorVisible.value=false; pending.value = undefined; referenceReason.value=''; void load() }, { immediate: true })
onBeforeUnmount(() => { generation++; readSequence++; readingReferenceId.value = ''; rows.value = [] })
</script>
<style scoped>
.reference-file-name.is-reference { color: #b85a00; }
</style>
