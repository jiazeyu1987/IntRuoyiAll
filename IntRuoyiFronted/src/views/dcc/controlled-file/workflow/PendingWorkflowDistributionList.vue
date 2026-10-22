<template>
  <section data-testid="dcc-workflow-pending-distribution-list">
    <div>
      <el-button :loading="loading" @click="refresh">刷新待处置列表</el-button>
    </div>
    <p>受控且已生效后自动下发；仅查看已生效但尚未建立下发义务的事项。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon data-testid="dcc-workflow-pending-distribution-error" />
    <el-table v-else v-loading="loading" :data="rows" row-key="id" empty-text="暂无待下发事项">
      <el-table-column prop="fileNumber" label="文件编号" />
      <el-table-column prop="title" label="名称" />
      <el-table-column prop="versionNo" label="受控版本" />
      <el-table-column prop="controlledTime" label="受控日期" />
      <el-table-column label="生效方式"><template #default="{ row }">{{ effectModeName(row) }}</template></el-table-column>
      <el-table-column label="预设生效日期"><template #default="{ row }">{{ presetDate(row) }}</template></el-table-column>
      <el-table-column label="实际生效时间"><template #default="{ row }">{{ formatDateTimeValue(row.effectiveTime, '尚未记录') }}</template></el-table-column>
      <el-table-column prop="activatedTime" label="日常启用时间" />
      <el-table-column label="生效状态">
        <template #default>已生效</template>
      </el-table-column>
      <el-table-column label="办理">
        <template #default="{ row }"><el-button :disabled="loading || !canHandle" @click="openFile(row)">查看下发结果</el-button></template>
      </el-table-column>
    </el-table>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { isWorkflowId, type PendingWorkflowDistributionFile } from './workflow-actions'
import { formatDateTimeValue } from '@/utils/formatTime'
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
const remindersOnly = false
const loading = ref(false)
const error = ref('')
let requestSequence = 0
let mounted = true
const publishState = (total: number | null) => emit('state', {
  total, loading: loading.value, error: error.value, remindersOnly
})
const refresh = async () => {
  const sequence = ++requestSequence
  const context = props.contextKey
  const filter = false
  const current = () => mounted && sequence === requestSequence && context === props.contextKey
    && props.canHandle
  if (!props.contextKey.trim() || !props.canHandle) { rows.value = []; loading.value = false; error.value = '当前账号无文控下发办理资格'; publishState(null); return }
  loading.value = true
  error.value = ''
  rows.value = []
  publishState(null)
  try {
    const result = await props.load(filter)
    if (!current()) return
    if (!Array.isArray(result) || result.some(file => !isWorkflowId(file.id) || !file.fileNumber?.trim() || !file.title?.trim() || !file.versionNo?.trim()
      || !file.controlledTime
      || file.effectiveMode !== null && file.effectiveMode !== undefined && !['PRESET_DATE','AFTER_STANDARDIZATION_SIGNATURE'].includes(file.effectiveMode)
      || file.effectiveMode === 'PRESET_DATE' && Boolean(file.effectiveDate) && !/^\d{4}-\d{2}-\d{2}$/.test(file.effectiveDate as string)
      || file.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE' && file.effectiveDate !== null && file.effectiveDate !== undefined
      || !['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(file.status)))
      throw new Error('待处置列表返回的受控版本或生效事实不完整')
    const pending = result.filter(file => file.status === 'ACTIVE' && Boolean(file.activatedTime) && !file.distributedTime)
    if (new Set(pending.map(file => String(file.id))).size !== pending.length)
      throw new Error('待处置列表版本身份重复，请刷新后联系文控')
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
const effectModeName = (file: PendingWorkflowDistributionFile) => file.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE'
  ? '文控标准化签名后生效' : file.effectiveMode === 'PRESET_DATE' ? '按预设日期生效' : '生效方式未记录'
const presetDate = (file: PendingWorkflowDistributionFile) => file.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE'
  ? '不适用，无预设日期' : file.effectiveMode === 'PRESET_DATE' ? file.effectiveDate || '尚未记录' : '未记录'
onMounted(refresh)
onBeforeUnmount(() => { mounted = false; requestSequence++; rows.value = [] })
defineExpose({ refresh })
</script>
