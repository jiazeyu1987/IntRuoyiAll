<template>
  <Dialog
    v-model="dialogVisible"
    :title="dialogMode === 'modify' ? '修改归属' : '确认归属'"
    width="min(1280px, 96vw)"
    max-height="78vh"
  >
    <div class="attribution-summary">
      <div class="summary-item">
        <span class="summary-label">导入行工单</span>
        <span class="summary-value">{{ currentRecord?.workOrderCode || '-' }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">导入行工序</span>
        <span class="summary-value">{{ currentRecord?.processCode || '-' }} / {{ currentRecord?.processName || '-' }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">导入行工序数量</span>
        <span class="summary-value quantity-value">{{ formatQuantity(importedFeedbackQuantity) }}</span>
      </div>
    </div>

    <div class="quantity-workbench">
      <div class="quantity-editor">
        <span class="quantity-label">本次工序完成总量</span>
        <el-input-number
          :model-value="allocatedFeedbackQuantity"
          :min="0"
          :precision="0"
          :step="1"
          controls-position="right"
          disabled
          class="!w-220px"
        />
      </div>
      <div class="selected-metrics">
        <div class="metric-item">
          <span>导入行工序数量</span>
          <strong>{{ formatQuantity(importedFeedbackQuantity) }}</strong>
        </div>
        <div class="metric-item">
          <span>工序缓存池</span>
          <strong>{{ formatQuantity(processSurplusPoolQuantity) }}</strong>
        </div>
        <div class="metric-item">
          <span>已分配总量</span>
          <strong>{{ formatQuantity(allocatedFeedbackQuantity) }}</strong>
        </div>
        <div class="metric-item metric-warning">
          <span>剩余待分配</span>
          <strong>{{ formatQuantity(remainingFeedbackQuantity) }}</strong>
        </div>
        <div class="metric-item selected-target">
          <span>已选订单工序</span>
          <strong>
            <template v-if="selectedCandidates.length">
              {{ selectedCandidates.map(formatCandidateLabel).join(' / ') }}
            </template>
            <template v-else>-</template>
          </strong>
        </div>
      </div>
    </div>

    <el-alert
      title="归属会按所选订单工序生成草稿正式报工，提交正式报工后回写排产进度。"
      type="info"
      :closable="false"
      show-icon
      class="mb-12px"
    />

    <div class="candidate-table-shell">
      <div class="table-title">选择订单工序</div>
      <el-table
        v-loading="loading"
        :data="candidates"
        stripe
        row-key="scheduleOrderProcessId"
        max-height="460"
        highlight-current-row
      >
        <el-table-column label="" width="54" align="center" fixed>
          <template #default="scope">
            <el-checkbox
              :model-value="isCandidateSelected(scope.row)"
              @change="(checked) => handleCandidateChecked(scope.row, checked)"
            />
          </template>
        </el-table-column>
        <el-table-column label="订单" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            <div class="primary-cell">{{ scope.row.scheduleOrderCode || scope.row.targetOrderLabel || '-' }}</div>
            <div class="secondary-cell">{{ scope.row.workOrderCode || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="工序" min-width="190" show-overflow-tooltip>
          <template #default="scope">
            <div class="primary-cell">{{ scope.row.processCode || '-' }}</div>
            <div class="secondary-cell">{{ scope.row.processName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="产品" min-width="240" show-overflow-tooltip>
          <template #default="scope">
            <div class="primary-cell">{{ scope.row.targetProductLabel || scope.row.itemName || '-' }}</div>
            <div class="secondary-cell">{{ scope.row.itemCode || '-' }} / {{ scope.row.specification || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="数量" min-width="300">
          <template #default="scope">
            <div class="quantity-grid">
              <span>计划</span>
              <strong>{{ formatQuantity(formatCandidatePlannedQuantity(scope.row.plannedQuantity)) }}</strong>
              <span>已完成</span>
              <strong>{{ formatQuantity(scope.row.reportedQuantity) }}</strong>
              <span>剩余</span>
              <strong>{{ formatQuantity(scope.row.remainingQuantity) }}</strong>
              <span>超出</span>
              <strong class="overproduce-text">
                {{ formatQuantity(resolveOverproduceQuantity(scope.row, importedFeedbackQuantity)) }}
              </strong>
              <span>缓存池</span>
              <strong>{{ formatQuantity(scope.row.surplusPoolQuantity) }}</strong>
              <span>可分配</span>
              <strong class="available-text">{{ formatQuantity(resolveCandidateAvailableQuantity(scope.row)) }}</strong>
              <span>分配</span>
              <el-input-number
                :model-value="getAllocationQuantity(scope.row)"
                :min="0"
                :max="getCandidateMaxQuantity(scope.row)"
                :precision="0"
                :step="1"
                controls-position="right"
                class="quantity-input"
                @update:model-value="(value) => handleAllocationChange(scope.row, value)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="scope">
            <div class="status-stack">
              <el-tag :type="scope.row.exactWorkOrderMatch ? 'success' : 'info'" size="small">
                {{ scope.row.exactWorkOrderMatch ? '精确匹配' : '可归属' }}
              </el-tag>
              <span class="secondary-cell">{{ scope.row.taskCode || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="" width="92" align="center">
          <template #default="scope">
            <el-button
              size="small"
              :disabled="loading || getCandidateFillQuantity(scope.row) <= 0"
              @click="handleFillRowQuantity(scope.row)"
            >
              全部
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
        <el-button :disabled="loading" type="primary" @click="handleConfirm">
          {{ dialogMode === 'modify' ? '保存修改' : '确认归属' }}
        </el-button>
        <el-button :disabled="loading" @click="dialogVisible = false">取消</el-button>
      </template>
  </Dialog>
</template>

<script setup lang="ts">
import {
  ProFeedbackApi,
  type ProFeedbackImportCandidateVO,
  type ProFeedbackImportRecordVO
} from '@/api/mes/pro/feedback'
import {
  formatCandidatePlannedQuantity,
  resolveCurrentOrderFillQuantity
} from './importAttributionQuantity'

defineOptions({ name: 'ImportAttributionDialog' })

const message = useMessage()
const emits = defineEmits(['success'])
const attributionTargetType = {
  currentOrder: 'CURRENT_ORDER',
  externalOtherOrder: 'EXTERNAL_OTHER_ORDER'
} as const

type AttributionSuccessPayload = {
  feedbackId: number
  importRecordId: number
  targetType: 'CURRENT_ORDER' | 'EXTERNAL_OTHER_ORDER'
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  sourceImportFileName?: string
  processLabel: string
  attributionTime: string
}

const dialogVisible = ref(false)
const loading = ref(false)
const dialogMode = ref<'create' | 'modify'>('create')
const currentRecord = ref<ProFeedbackImportRecordVO>()
const candidates = ref<ProFeedbackImportCandidateVO[]>([])
const selectedProcessIds = ref<number[]>([])
const allocationMap = reactive<Record<string, number>>({})
const selectedCandidates = computed(() =>
  candidates.value.filter((item) => selectedProcessIds.value.includes(item.scheduleOrderProcessId))
)
const selectedCandidate = computed(() => selectedCandidates.value[0])
const importedFeedbackQuantity = computed(() => Number(currentRecord.value?.feedbackQuantity ?? 0))
const processSurplusPoolQuantity = computed(() =>
  Number(selectedCandidate.value?.surplusPoolQuantity ?? candidates.value[0]?.surplusPoolQuantity ?? 0)
)
const availableFeedbackQuantity = computed(() => importedFeedbackQuantity.value + processSurplusPoolQuantity.value)
const allocatedFeedbackQuantity = computed(() =>
  selectedCandidates.value.reduce((sum, candidate) => sum + getAllocationQuantity(candidate), 0)
)
const remainingFeedbackQuantity = computed(() =>
  Math.max(0, availableFeedbackQuantity.value - allocatedFeedbackQuantity.value)
)

const open = async (record: ProFeedbackImportRecordVO) => {
  dialogVisible.value = true
  dialogMode.value =
    record.attributionStatus === 'ATTRIBUTED' && record.canModifyAttribution ? 'modify' : 'create'
  currentRecord.value = record
  selectedProcessIds.value = []
  clearAllocationMap()
  candidates.value = []
  await loadCandidates()
}

defineExpose({ open })

const loadCandidates = async () => {
  if (!currentRecord.value) return
  loading.value = true
  try {
    candidates.value = await ProFeedbackApi.getImportRecordCandidates(currentRecord.value.id)
    syncSelectedQuantityFromCandidates()
    if (!selectedProcessIds.value.length && candidates.value.length === 1) {
      handleCandidateChecked(candidates.value[0], true)
    }
  } finally {
    loading.value = false
  }
}

const resolveDefaultProcessFeedbackQuantity = (candidate?: ProFeedbackImportCandidateVO) => {
  const importedQuantity = importedFeedbackQuantity.value
  if (candidate?.externalOtherOrder) return importedQuantity > 0 ? importedQuantity : 0
  return resolveCurrentOrderFillQuantity(candidate)
}

const getCandidateMaxQuantity = (candidate?: ProFeedbackImportCandidateVO) => {
  if (!candidate) {
    return 0
  }
  const currentQuantity = getAllocationQuantity(candidate)
  const remainingPool = Math.max(0, availableFeedbackQuantity.value - (allocatedFeedbackQuantity.value - currentQuantity))
  if (candidate.externalOtherOrder) {
    return remainingPool
  }
  return Math.max(0, remainingPool)
}

const resolveCandidateAvailableQuantity = (candidate?: ProFeedbackImportCandidateVO) =>
  Number(candidate?.availableFeedbackQuantity ?? availableFeedbackQuantity.value)

const resolveOverproduceQuantity = (
  candidate: ProFeedbackImportCandidateVO | undefined,
  feedbackQuantity: number
) => {
  if (candidate?.externalOtherOrder) {
    return Math.max(0, Number(feedbackQuantity.toFixed(6)))
  }
  const remainingQuantity = Number(candidate?.remainingQuantity ?? 0)
  if (!Number.isFinite(remainingQuantity) || remainingQuantity <= 0 || feedbackQuantity <= 0) {
    return 0
  }
  return Math.max(0, Number((feedbackQuantity - remainingQuantity).toFixed(6)))
}

const formatQuantity = (value?: number | string) => {
  if (typeof value === 'string') {
    return value
  }
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value)
}

const formatCandidateLabel = (candidate: ProFeedbackImportCandidateVO) =>
  `${candidate.scheduleOrderCode || candidate.targetOrderLabel || '-'} / ${candidate.processCode || '-'} / ${candidate.processName || '-'}`

const candidateKey = (row: ProFeedbackImportCandidateVO) => String(row.scheduleOrderProcessId)

const clearAllocationMap = () => {
  Object.keys(allocationMap).forEach((key) => {
    delete allocationMap[key]
  })
}

const syncSelectedQuantityFromCandidates = () => {
  selectedProcessIds.value = []
  clearAllocationMap()
  candidates.value.forEach((candidate) => {
    const selectedQuantity = Number(candidate.selectedQuantity ?? 0)
    if (selectedQuantity <= 0) {
      return
    }
    selectedProcessIds.value.push(candidate.scheduleOrderProcessId)
    allocationMap[candidateKey(candidate)] = selectedQuantity
  })
}

const isCandidateSelected = (row: ProFeedbackImportCandidateVO) =>
  selectedProcessIds.value.includes(row.scheduleOrderProcessId)

const getAllocationQuantity = (row: ProFeedbackImportCandidateVO) => allocationMap[candidateKey(row)] ?? 0

const getCandidateFillQuantity = (row: ProFeedbackImportCandidateVO) =>
  Math.max(0, Math.floor(resolveDefaultProcessFeedbackQuantity(row)))

const handleFillRowQuantity = (row: ProFeedbackImportCandidateVO) => {
  const quantity = getCandidateFillQuantity(row)
  if (quantity <= 0) {
    return
  }
  if (!selectedProcessIds.value.includes(row.scheduleOrderProcessId)) {
    selectedProcessIds.value.push(row.scheduleOrderProcessId)
  }
  allocationMap[candidateKey(row)] = quantity
}

const handleAllocationChange = (row: ProFeedbackImportCandidateVO, value: number | undefined) => {
  if (!selectedProcessIds.value.includes(row.scheduleOrderProcessId)) {
    selectedProcessIds.value.push(row.scheduleOrderProcessId)
  }
  allocationMap[candidateKey(row)] = Number(value ?? 0)
}

const handleCandidateChecked = (row: ProFeedbackImportCandidateVO, checked: boolean) => {
  if (checked) {
    if (!selectedProcessIds.value.includes(row.scheduleOrderProcessId)) {
      selectedProcessIds.value.push(row.scheduleOrderProcessId)
    }
    if (getAllocationQuantity(row) <= 0) {
      allocationMap[candidateKey(row)] = resolveDefaultProcessFeedbackQuantity(row)
    }
    return
  }
  selectedProcessIds.value = selectedProcessIds.value.filter((id) => id !== row.scheduleOrderProcessId)
  delete allocationMap[candidateKey(row)]
}

const handleConfirm = async () => {
  if (!currentRecord.value) return
  const allocations = selectedCandidates.value
    .map((row) => ({
      row,
      quantity: Number(getAllocationQuantity(row) || 0)
    }))
    .filter((item) => item.quantity > 0)
  if (!allocations.length) {
    message.error('请选择订单工序')
    return
  }
  const allocationTotal = allocations.reduce((sum, item) => sum + item.quantity, 0)
  if (allocationTotal <= 0) {
    message.error('本次工序完成数量必须大于 0')
    return
  }
  if (allocationTotal > availableFeedbackQuantity.value) {
    message.error('多个订单分配数量之和不能大于导入行工序数量与缓存池数量之和')
    return
  }
  loading.value = true
  try {
    const feedbackId =
      dialogMode.value === 'modify'
        ? await ProFeedbackApi.reattributeImportRecord({
            importRecordId: currentRecord.value.id,
            allocations: allocations.map(({ row, quantity }) => ({
              targetType:
                row.targetType === attributionTargetType.externalOtherOrder
                  ? attributionTargetType.externalOtherOrder
                  : attributionTargetType.currentOrder,
              scheduleOrderId:
                row.targetType === attributionTargetType.currentOrder ? row.scheduleOrderId : undefined,
              scheduleOrderProcessId:
                row.targetType === attributionTargetType.currentOrder ? row.scheduleOrderProcessId : undefined,
              feedbackQuantity: quantity
            }))
          })
        : await ProFeedbackApi.attributeImportRecord({
            importRecordId: currentRecord.value.id,
            allocations: allocations.map(({ row, quantity }) => ({
              targetType:
                row.targetType === attributionTargetType.externalOtherOrder
                  ? attributionTargetType.externalOtherOrder
                  : attributionTargetType.currentOrder,
              scheduleOrderId:
                row.targetType === attributionTargetType.currentOrder ? row.scheduleOrderId : undefined,
              scheduleOrderProcessId:
                row.targetType === attributionTargetType.currentOrder ? row.scheduleOrderProcessId : undefined,
              feedbackQuantity: quantity
            }))
          })
    message.success(dialogMode.value === 'modify' ? '归属修改成功' : '归属成功')
    dialogVisible.value = false
    emits('success', {
      feedbackId,
      importRecordId: currentRecord.value.id,
      targetType: allocations.length > 1 ? 'CURRENT_ORDER' : allocations[0].row.targetType || 'CURRENT_ORDER',
      scheduleOrderId: allocations.length === 1 && allocations[0].row.targetType === attributionTargetType.currentOrder
        ? allocations[0].row.scheduleOrderId
        : undefined,
      scheduleOrderProcessId: allocations.length === 1 && allocations[0].row.targetType === attributionTargetType.currentOrder
        ? allocations[0].row.scheduleOrderProcessId
        : undefined,
      sourceImportFileName: currentRecord.value.sourceFileName,
      processLabel: allocations.map(({ row }) => `${row.processCode || '-'} / ${row.processName || '-'}`).join(' + '),
      attributionTime: new Date().toISOString()
    } satisfies AttributionSuccessPayload)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.attribution-summary {
  display: grid;
  grid-template-columns: minmax(180px, 0.8fr) minmax(320px, 1.4fr) minmax(160px, 0.6fr);
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #f7f9fc;
}

.summary-item,
.metric-item {
  min-width: 0;
}

.summary-label,
.quantity-label,
.metric-item span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.summary-value {
  display: block;
  overflow: hidden;
  color: #263247;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quantity-value,
.metric-item strong {
  color: #172033;
  font-variant-numeric: tabular-nums;
}

.quantity-workbench {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-right: 1px solid #dbe3ef;
  border-bottom: 1px solid #dbe3ef;
  border-left: 1px solid #dbe3ef;
  border-radius: 0 0 8px 8px;
  background: #ffffff;
}

.quantity-editor {
  display: flex;
  gap: 8px;
  align-items: end;
}

.selected-metrics {
  display: grid;
  grid-template-columns: repeat(3, 110px) minmax(280px, 1fr);
  gap: 10px;
  align-items: center;
}

.metric-item {
  padding: 6px 10px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #fafcff;
}

.metric-item strong {
  display: block;
  overflow: hidden;
  font-size: 14px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-warning strong,
.overproduce-text {
  color: #b45309;
}

.available-text {
  color: #1677ff;
}

.selected-target {
  min-width: 0;
}

.candidate-table-shell {
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.table-title {
  padding: 10px 12px;
  border-bottom: 1px solid #e5ebf3;
  background: #f7f9fc;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.candidate-table-shell :deep(.el-table__header th) {
  height: 44px;
  background: #f7f9fc;
  color: #6b7280;
  font-size: 13px;
}

.candidate-table-shell :deep(.el-table__row) {
  height: 58px;
}

.candidate-table-shell :deep(.el-table__cell) {
  padding: 7px 10px;
}

.primary-cell {
  overflow: hidden;
  color: #263247;
  font-size: 14px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.secondary-cell {
  display: block;
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quantity-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 2px 8px;
  color: #6b7280;
  font-size: 12px;
}

.quantity-grid strong {
  color: #263247;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.status-stack {
  display: grid;
  gap: 4px;
  justify-items: center;
}

.candidate-table-shell :deep(.el-button) {
  min-width: 52px;
}

@media (max-width: 1100px) {
  .attribution-summary,
  .quantity-workbench,
  .selected-metrics {
    grid-template-columns: 1fr;
  }

  .quantity-editor {
    align-items: center;
  }
}
</style>
