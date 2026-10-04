<template>
  <section>
    <slot name="frozen-evidence" ></slot>
    <p>受控版本：{{ versionNo }}；预设生效日期：{{ facts.effectiveDate }}</p>
    <p v-if="view.warning" role="status">{{ view.warning }}</p>
    <p v-if="completed || view.distributionCompleted">已完成下发。接收、签收和纸件回收分别记录。</p>
    <template v-else>
      <div v-for="row in rows" :key="row.key">
        <el-select v-model="row.departmentId" placeholder="接收部门" :disabled="busy || !allowed" @change="loadRowRecipients(row)">
          <el-option v-for="dept in departments" :key="String(dept.id)" :label="dept.name" :value="dept.id" />
        </el-select>
        <el-select v-model="row.distributionMedium" placeholder="下发方式" :disabled="busy || !allowed">
          <el-option label="电子下发" value="PUBLIC_FOLDER" />
          <el-option label="纸件下发" value="PAPER" />
        </el-select>
        <el-select v-model="row.recipientUserIds" multiple placeholder="接收人员" :loading="row.loading" :disabled="busy || row.loading || !allowed">
          <el-option v-for="user in row.options" :key="String(user.id)" :label="user.name" :value="user.id" />
        </el-select>
        <el-button :disabled="busy" @click="removeScope(row.key)">移除部门</el-button>
        <p v-if="row.error" role="alert">{{ row.error }}</p>
      </div>
      <el-button :disabled="busy || !allowed" @click="addScope">添加接收部门</el-button>
      <p v-if="error" role="alert">{{ error }}</p>
      <el-button type="primary" :disabled="busy || !allowed" @click="prepareConfirmation">核对并下发</el-button>
      <div v-if="confirmationVisible" role="dialog" aria-label="确认下发">
        <p>确认向下列部门和人员下发本受控版本？生效日期保持 {{ facts.effectiveDate }}。</p>
        <ul>
          <li v-for="row in rows" :key="row.key">
            {{ departmentName(row.departmentId) }} / {{ row.distributionMedium === 'PAPER' ? '纸件' : '电子' }}：
            {{ recipientNames(row) }}
          </li>
        </ul>
        <p v-if="view.warning">{{ view.warning }}</p>
        <el-button :disabled="busy" @click="cancelConfirmation">取消</el-button>
        <el-button type="primary" :loading="busy" @click="confirmDistribution">确认下发</el-button>
      </div>
    </template>
  </section>
</template>
<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { isWorkflowId, lifecyclePresentation, type LifecycleFacts, type WorkflowId,
  type WorkflowDistributionScope, type WorkflowRecipientOption } from './workflow-actions'

const props = defineProps<{
  fileId: WorkflowId
  processInstanceId: string
  versionNo: string
  facts: LifecycleFacts
  canDistribute: boolean
  departments: Array<{ id: WorkflowId; name: string }>
  loadRecipients: (fileId: WorkflowId, departmentId: WorkflowId) => Promise<WorkflowRecipientOption[]>
  save: (fileId: WorkflowId, scopes: WorkflowDistributionScope[]) => Promise<boolean>
}>()
const emit = defineEmits<{ saved: [context: { fileId: WorkflowId; processInstanceId: string; versionNo: string }] }>()
interface ScopeRow {
  key: number
  departmentId: WorkflowId
  distributionMedium: 'PUBLIC_FOLDER' | 'PAPER'
  recipientUserIds: WorkflowId[]
  options: WorkflowRecipientOption[]
  loading: boolean
  error: string
}
const rows = ref<ScopeRow[]>([])
const busy = ref(false)
const error = ref('')
const completed = ref(false)
const confirmationVisible = ref(false)
let confirmationSnapshot: { context: string; scopes: WorkflowDistributionScope[] } | undefined
let contextGeneration = 0
let mounted = true
const rowRequests = new Map<number, number>()
let rowKey = 0
const contextKey = () => JSON.stringify([String(props.fileId), props.processInstanceId, props.versionNo, props.facts.effectiveDate])
const view = computed(() => lifecyclePresentation(props.facts))
const allowed = computed(() => props.canDistribute && view.value.canDistribute && !completed.value)
watch(contextKey, () => {
  contextGeneration++
  rowRequests.clear()
  confirmationSnapshot = undefined
  rows.value = []
  busy.value = false
  error.value = ''
  completed.value = false
  confirmationVisible.value = false
}, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; contextGeneration++; rowRequests.clear(); confirmationSnapshot = undefined })
const addScope = () => {
  if (!allowed.value || busy.value) return
  rows.value.push({ key: ++rowKey, departmentId: '', distributionMedium: 'PUBLIC_FOLDER', recipientUserIds: [],
    options: [], loading: false, error: '' })
}
const removeScope = (key: number) => {
  if (!busy.value) { rowRequests.delete(key); rows.value = rows.value.filter(row => row.key !== key) }
}
const loadRowRecipients = async (row: ScopeRow) => {
  if (busy.value || !allowed.value) return
  const generation = contextGeneration
  const fileId = props.fileId
  const departmentId = row.departmentId
  const requestId = (rowRequests.get(row.key) || 0) + 1
  rowRequests.set(row.key, requestId)
  const current = () => mounted && generation === contextGeneration && rowRequests.get(row.key) === requestId
    && String(row.departmentId) === String(departmentId) && rows.value.includes(row)
  row.options = []
  row.recipientUserIds = []
  row.error = ''
  if (!isWorkflowId(row.departmentId)) return
  row.loading = true
  try {
    const options = await props.loadRecipients(fileId, departmentId)
    if (!current()) return
    if (!Array.isArray(options) || options.some(user => !isWorkflowId(user.id) || !user.name?.trim())
      || new Set(options.map(user => String(user.id))).size !== options.length) throw new Error('接收人目录身份缺失或重复')
    row.options = options
  } catch (cause) {
    if (current()) row.error = cause instanceof Error ? cause.message : '接收人目录读取失败'
  } finally { if (current()) row.loading = false }
}
const readScopes = (): WorkflowDistributionScope[] => {
  if (!allowed.value || !isWorkflowId(props.fileId) || !props.processInstanceId?.trim() || !props.versionNo?.trim())
    throw new Error('当前文件或办理资格不允许下发，请刷新后核对')
  if (!rows.value.length) throw new Error('请添加接收部门并核对人员和方式')
  const selected = new Set<string>()
  return rows.value.map(row => {
    if (!isWorkflowId(row.departmentId) || !props.departments.some(dept => String(dept.id) === String(row.departmentId))
      || selected.has(String(row.departmentId))) throw new Error('接收部门无效或重复')
    selected.add(String(row.departmentId))
    if (row.loading || row.error) throw new Error('接收人目录未就绪，请重新读取')
    if (new Set(row.recipientUserIds.map(String)).size !== row.recipientUserIds.length
      || row.recipientUserIds.some(id => !isWorkflowId(id) || !row.options.some(option => String(option.id) === String(id))))
      throw new Error('接收人员已失效或不属于所选部门')
    if (!['PAPER', 'PUBLIC_FOLDER'].includes(row.distributionMedium)) throw new Error('请选择正式下发方式')
    if (row.distributionMedium === 'PUBLIC_FOLDER' && !row.recipientUserIds.length) throw new Error('电子下发请选择接收人')
    return { departmentId: String(row.departmentId), distributionMedium: row.distributionMedium,
      recipientUserIds: row.recipientUserIds.map(String).sort() }
  }).sort((left, right) => String(left.departmentId).localeCompare(String(right.departmentId)))
}
const prepareConfirmation = () => {
  if (busy.value) return
  error.value = ''
  try { confirmationSnapshot = { context: contextKey(), scopes: readScopes() }; confirmationVisible.value = true }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '下发信息校验失败'; confirmationVisible.value = false }
}
const cancelConfirmation = () => { if (!busy.value) { confirmationVisible.value = false; confirmationSnapshot = undefined } }
const confirmDistribution = async () => {
  if (busy.value || !confirmationVisible.value) return
  const generation = contextGeneration
  const context = { fileId: props.fileId, processInstanceId: props.processInstanceId, versionNo: props.versionNo }
  try {
    const scopes = readScopes()
    if (!confirmationSnapshot || confirmationSnapshot.context !== contextKey()
      || JSON.stringify(confirmationSnapshot.scopes) !== JSON.stringify(scopes)) {
      confirmationVisible.value = false
      confirmationSnapshot = undefined
      throw new Error('下发信息已变更，请重新确认')
    }
    busy.value = true
    const saved = await props.save(context.fileId, scopes)
    if (!mounted || generation !== contextGeneration) return
    if (saved !== true) throw new Error('下发保存未确认成功，请刷新核对后重试')
    completed.value = true
    confirmationVisible.value = false
    error.value = ''
    emit('saved', context)
  } catch (cause) { if (mounted && generation === contextGeneration) error.value = cause instanceof Error ? cause.message : '下发保存失败' }
  finally { if (mounted && generation === contextGeneration) busy.value = false }
}
const departmentName = (id: WorkflowId) => props.departments.find(dept => String(dept.id) === String(id))?.name || '部门未记录'
const recipientNames = (row: ScopeRow) => row.recipientUserIds.map(id => row.options.find(option => String(option.id) === String(id))?.name || '人员未记录').join('、') || '纸件接收另行登记'
</script>
