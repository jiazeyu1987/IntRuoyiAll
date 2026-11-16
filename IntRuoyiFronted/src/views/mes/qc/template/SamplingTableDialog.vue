<template>
  <Dialog v-model="visible" title="GB/T 2828.1 抽样对照表" width="1100px">
    <el-alert
      title="按 GB/T 2828.1-2012 表1查字码，按表2-A返回正常一次名义样本量。"
      description="本查询不包含 AQL、箭头调整及接收/拒收判定。名义样本量达到或超过总数量时应全检。"
      type="info"
      :closable="false"
      show-icon
      class="mb-16px"
    />
    <el-form :inline="true" @submit.prevent="query">
      <el-form-item label="总数量">
        <el-input-number
          v-model="totalQuantity"
          :min="2"
          :max="Number.MAX_SAFE_INTEGER"
          :step="1"
          :precision="0"
          :value-on-clear="null"
          aria-label="抽样查询总数量"
          @change="clearResult"
        />
      </el-form-item>
      <el-form-item label="检验水平">
        <el-select v-model="inspectionLevel" class="!w-160px" @change="clearResult">
          <el-option
            v-for="level in levels"
            :key="level.value"
            :value="level.value"
            :label="level.label"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="querying">查询抽检数量</el-button>
      </el-form-item>
    </el-form>
    <el-descriptions v-if="result" :column="3" border class="mb-16px">
      <el-descriptions-item label="匹配批量">{{ rangeLabel(result) }}</el-descriptions-item>
      <el-descriptions-item label="样本量字码">{{ result.sampleCode }}</el-descriptions-item>
      <el-descriptions-item label="正常一次名义样本量"
        >{{ result.nominalSampleSize }} 件</el-descriptions-item
      >
    </el-descriptions>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-16px" />
    <el-table
      v-loading="loading"
      :data="rows"
      border
      max-height="500"
      :row-class-name="rowClassName"
    >
      <el-table-column label="批量范围" min-width="180">
        <template #default="{ row }">{{ rangeLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="特殊检验水平" align="center">
        <el-table-column
          v-for="level in levels.slice(0, 4)"
          :key="level.value"
          :label="level.label"
          min-width="105"
          align="center"
        >
          <template #default="{ row }">{{ cellLabel(row, level.value) }}</template>
        </el-table-column>
      </el-table-column>
      <el-table-column label="一般检验水平" align="center">
        <el-table-column
          v-for="level in levels.slice(4)"
          :key="level.value"
          :label="level.label"
          min-width="105"
          align="center"
        >
          <template #default="{ row }">{{ cellLabel(row, level.value) }}</template>
        </el-table-column>
      </el-table-column>
    </el-table>
    <p class="mt-12px text-13px text-[var(--el-text-color-secondary)]"
      >单元格格式：字码 / 名义样本量（件）。依据：标准正文第14、15页。</p
    >
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  getSamplingTable,
  lookupSampling,
  type InspectionLevel,
  type SamplingRow,
  type SamplingResult
} from '@/api/mes/qc/sampling'

const levels: { value: InspectionLevel; label: string }[] = [
  { value: 'S1', label: 'S-1' },
  { value: 'S2', label: 'S-2' },
  { value: 'S3', label: 'S-3' },
  { value: 'S4', label: 'S-4' },
  { value: 'I', label: 'I' },
  { value: 'II', label: 'II' },
  { value: 'III', label: 'III' }
]
const visible = ref(false)
const loading = ref(false)
const querying = ref(false)
const totalQuantity = ref<number | null>(null)
const inspectionLevel = ref<InspectionLevel>('II')
const rows = ref<SamplingRow[]>([])
const queryResult = ref<SamplingResult>()
const error = ref('')
// A response for an earlier input must never be displayed as the current lookup result.
const result = computed(() =>
  queryResult.value?.totalQuantity === totalQuantity.value &&
  queryResult.value?.inspectionLevel === inspectionLevel.value
    ? queryResult.value
    : undefined
)
let requestVersion = 0
const clearResult = () => {
  requestVersion++
  queryResult.value = undefined
  error.value = ''
}

const rangeLabel = (row: { minQuantity: number; maxQuantity: number | null }) =>
  row.maxQuantity === null
    ? `${row.minQuantity.toLocaleString()} 及以上`
    : `${row.minQuantity.toLocaleString()}～${row.maxQuantity.toLocaleString()}`
const cellLabel = (row: SamplingRow, level: InspectionLevel) =>
  `${row.samples[level].sampleCode} / ${row.samples[level].nominalSampleSize}`
const rowClassName = ({ row }: { row: SamplingRow }) =>
  result.value?.minQuantity === row.minQuantity ? 'sampling-match-row' : ''

const open = async () => {
  visible.value = true
  clearResult()
  rows.value = []
  loading.value = true
  try {
    rows.value = await getSamplingTable()
  } catch {
    error.value = '抽样对照表加载失败，请关闭后重新打开。'
  } finally {
    loading.value = false
  }
}

const query = async () => {
  clearResult()
  if (
    !Number.isSafeInteger(totalQuantity.value) ||
    totalQuantity.value === null ||
    totalQuantity.value < 2
  ) {
    error.value = '请输入不小于2的整数总数量。'
    return
  }
  const version = requestVersion
  querying.value = true
  try {
    const data = await lookupSampling(totalQuantity.value, inspectionLevel.value)
    if (version === requestVersion) queryResult.value = data
  } catch {
    if (version === requestVersion) error.value = '抽样查询失败，请检查输入或稍后重试。'
  } finally {
    querying.value = false
  }
}
defineExpose({ open })
</script>

<style scoped>
:deep(.sampling-match-row) {
  --el-table-tr-bg-color: var(--el-color-primary-light-9);
}
</style>
