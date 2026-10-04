<template>
  <section data-testid="dcc-workflow-pending-distribution-list">
    <div>
      <el-checkbox v-model="remindersOnly" :disabled="!canHandle">仅显示到期、逾期及按配置临期事项</el-checkbox>
      <el-button :loading="loading" @click="refresh">刷新待处置列表</el-button>
    </div>
    <p>按预设生效日期从早到晚办理；已下发事项不作为未办理提醒。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon data-testid="dcc-workflow-pending-distribution-error" />
    <el-table v-else v-loading="loading" :data="rows" row-key="id" empty-text="暂无待下发事项">
      <el-table-column prop="fileNumber" label="文件编号" />
      <el-table-column prop="title" label="名称" />
      <el-table-column prop="versionNo" label="受控版本" />
      <el-table-column prop="controlledTime" label="受控日期" />
      <el-table-column prop="effectiveDate" label="预设生效日期" />
      <el-table-column label="提醒">
        <template #default="{ row }">{{ reminderLabel(row.distributionReminderStage) }}</template>
      </el-table-column>
      <el-table-column label="生效状态">
        <template #default="{ row }">{{ row.status === 'CONTROLLED_PENDING_EFFECTIVE' ? '待生效；生效前不得执行' : '已生效' }}</template>
      </el-table-column>
      <el-table-column label="办理">
        <template #default="{ row }"><el-button :disabled="loading || !canHandle" @click="openFile(row)">办理下发</el-button></template>
      </el-table-column>
    </el-table>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { isWorkflowId, type PendingWorkflowDistributionFile } from './workflow-actions'
const props = defineProps<{
  contextKey: string
  canHandle: boolean
  load: (remindersOnly: boolean) => Promise<PendingWorkflowDistributionFile[]>
}>()
const emit = defineEmits<{
  open: [file: PendingWorkflowDistributionFile]
  state: [summary: { total: number | null; loading: boolean; error: string; remindersOnly: boolean }]
}>()
const rows = ref<PendingWorkflowDistributionFile[]>([])
const remindersOnly = ref(false)
const loading = ref(false)
const error = ref('')
let requestSequence = 0
let mounted = true
const labels = { OVERDUE: '逾期未下发', DUE: '到期未下发', UPCOMING: '临期未下发', FUTURE: '后续待下发' }
const reminderLabel = (stage: keyof typeof labels) => labels[stage]
const publishState = (total: number | null) => emit('state', {
  total, loading: loading.value, error: error.value, remindersOnly: remindersOnly.value
})
const refresh = async () => {
  const sequence = ++requestSequence
  const context = props.contextKey
  const filter = remindersOnly.value
  const current = () => mounted && sequence === requestSequence && context === props.contextKey
    && filter === remindersOnly.value && props.canHandle
  if (!props.contextKey.trim() || !props.canHandle) { rows.value = []; loading.value = false; error.value = '当前账号无文控下发办理资格'; publishState(null); return }
  loading.value = true
  error.value = ''
  rows.value = []
  publishState(null)
  try {
    const result = await props.load(filter)
    if (!current()) return
    if (!Array.isArray(result) || result.some(file => !isWorkflowId(file.id) || !file.fileNumber?.trim() || !file.title?.trim() || !file.versionNo?.trim()
      || !file.controlledTime || !/^\d{4}-\d{2}-\d{2}$/.test(file.effectiveDate)
      || !['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(file.status) || !(file.distributionReminderStage in labels)))
      throw new Error('待处置列表返回的受控版本或提醒日期证据不完整')
    const pending = result.filter(file => !file.distributedTime)
    if (new Set(pending.map(file => String(file.id))).size !== pending.length
      || pending.some((file, index) => index > 0 && file.effectiveDate < pending[index - 1].effectiveDate))
      throw new Error('待处置列表身份重复或未按生效日期排序，请刷新后联系文控')
    if (filter && pending.some(file => file.distributionReminderStage === 'FUTURE'))
      throw new Error('提醒列表返回了未进入配置提醒范围的事项，请核对服务端提醒规则')
    rows.value = pending.map(file => ({ ...file, id: String(file.id) }))
  } catch (cause) {
    if (current()) {
      const responseMessage = (cause as { response?: { data?: { msg?: string } }; msg?: string })?.response?.data?.msg
        || (cause as { msg?: string })?.msg
      error.value = typeof responseMessage === 'string' && responseMessage.trim() ? responseMessage
        : cause instanceof Error && cause.message !== 'error' ? cause.message : '待处置列表读取失败，请查看接口错误后重试'
    }
  } finally {
    if (current()) { loading.value = false; publishState(error.value ? null : rows.value.length) }
    else if (mounted && sequence === requestSequence) loading.value = false
  }
}
const openFile = (file: PendingWorkflowDistributionFile) => {
  const selected = rows.value.find(row => String(row.id) === String(file.id))
  if (!props.canHandle || loading.value || error.value || !selected || selected.distributedTime) return
  emit('open', { ...selected })
}
watch(() => [props.contextKey, props.canHandle], refresh, { flush: 'sync' })
watch(remindersOnly, refresh)
onMounted(refresh)
onBeforeUnmount(() => { mounted = false; requestSequence++; rows.value = [] })
defineExpose({ refresh })
</script>
