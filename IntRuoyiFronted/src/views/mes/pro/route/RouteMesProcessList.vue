<template>
  <section class="route-mes-process-list">
    <div class="route-mes-process-list__toolbar">
      <div class="route-mes-process-list__title">
        <strong>MES 工序映射</strong>
        <span>一线报工看到的工序、工序设置主数据和设备关系</span>
      </div>
      <el-input
        v-model="keyword"
        class="route-mes-process-list__search"
        clearable
        placeholder="搜索工序 / 设备 / 批记录"
      />
    </div>
    <el-alert
      class="route-mes-process-list__hint"
      type="info"
      :closable="false"
      show-icon
      title="该列表复用当前路线工序和工作站设备配置；维护工序请进入工序设置，维护设备请进入工作站详情。"
    />
    <el-table
      v-loading="loading || submitting"
      :data="filteredRows"
      :show-overflow-tooltip="true"
      class="route-mes-process-list__table"
      row-key="rowKey"
      height="600"
    >
      <el-table-column label="序号" prop="sort" width="76" align="center" fixed="left" />
      <el-table-column label="MES 工序名称" min-width="170" fixed="left">
        <template #default="{ row }">
          <span class="cell-main">{{ row.mesProcessName || '-' }}</span>
          <span class="cell-sub">路线工序 ID：{{ row.routeProcessId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="工序设置工序" min-width="190">
        <template #default="{ row }">
          <span class="cell-main">{{ row.processCode || '-' }}</span>
          <span class="cell-sub">{{ row.processName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="设备编码" prop="machineryCode" min-width="145" />
      <el-table-column label="设备名称" prop="machineryName" min-width="170" />
      <el-table-column label="设备数量" prop="quantity" width="96" align="center" />
      <el-table-column label="单台产能/h" width="120" align="right">
        <template #default="{ row }">
          {{ formatNumber(row.machineryStandardHourlyCapacity) }}
        </template>
      </el-table-column>
      <el-table-column label="设备总产能/h" width="130" align="right">
        <template #default="{ row }">
          {{ formatNumber(row.machineryHourlyCapacityTotal) }}
        </template>
      </el-table-column>
      <el-table-column label="工序班次产能" width="130" align="right">
        <template #default="{ row }">
          {{ formatNumber(row.processShiftCapacityTotal) }}
        </template>
      </el-table-column>
      <el-table-column label="批记录工序名称" min-width="210">
        <template #default="{ row }">
          <span class="cell-main">{{ row.batchRecordReportName || '-' }}</span>
          <span class="cell-sub">{{ row.batchRecordReportCode || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="资源状态" width="112" align="center">
        <template #default="{ row }">
          <el-tag :type="row.hasMachinery ? 'success' : 'warning'" effect="light">
            {{ row.hasMachinery ? '已关联设备' : '未关联设备' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import {
  ProRouteProcessApi,
  type ProRouteProcessMachineryVO,
  type ProRouteProcessVO
} from '@/api/mes/pro/route/process'

defineOptions({ name: 'RouteMesProcessList' })

const props = withDefaults(
  defineProps<{
    routeId: number
    formType?: string
    submitting?: boolean
  }>(),
  {
    formType: 'detail',
    submitting: false
  }
)

type RouteMesProcessMappingRow = {
  rowKey: string
  routeProcessId?: number
  sort?: number
  mesProcessName?: string
  processCode?: string
  processName?: string
  machineryCode?: string
  machineryName?: string
  quantity?: number | string
  machineryStandardHourlyCapacity?: number
  machineryHourlyCapacityTotal?: number
  processShiftCapacityTotal?: number
  batchRecordReportCode?: string
  batchRecordReportName?: string
  hasMachinery: boolean
}

const loading = ref(false)
const keyword = ref('')
const rows = ref<RouteMesProcessMappingRow[]>([])
const submitting = computed(() => props.submitting)

const filteredRows = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return rows.value
  return rows.value.filter((row) =>
    [
      row.mesProcessName,
      row.processCode,
      row.processName,
      row.machineryCode,
      row.machineryName,
      row.batchRecordReportCode,
      row.batchRecordReportName
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text))
  )
})

const getList = async () => {
  loading.value = true
  try {
    const data = await ProRouteProcessApi.getRouteProcessListByRoute(props.routeId)
    rows.value = buildMappingRows(data || [])
  } finally {
    loading.value = false
  }
}

const buildMappingRows = (processes: ProRouteProcessVO[]) =>
  processes.flatMap((process) => {
    const machineryList = process.machineryList || []
    if (machineryList.length === 0) {
      return [buildMappingRow(process)]
    }
    return machineryList.map((machinery, index) => buildMappingRow(process, machinery, index))
  })

const buildMappingRow = (
  process: ProRouteProcessVO,
  machinery?: ProRouteProcessMachineryVO,
  machineryIndex = 0
): RouteMesProcessMappingRow => {
  const routeProcessId = process.id
  return {
    rowKey: `${routeProcessId || process.processId || 'process'}:${machinery?.machineryId || machineryIndex}`,
    routeProcessId,
    sort: process.sort,
    mesProcessName: process.processName,
    processCode: process.processCode,
    processName: process.processName,
    machineryCode: machinery?.machineryCode || '-',
    machineryName: machinery?.machineryName || '-',
    quantity: machinery?.quantity ?? '-',
    machineryStandardHourlyCapacity: machinery?.machineryStandardHourlyCapacity,
    machineryHourlyCapacityTotal: machinery?.machineryHourlyCapacityTotal,
    processShiftCapacityTotal: process.processShiftCapacityTotal,
    batchRecordReportCode: process.batchRecordReportCode,
    batchRecordReportName: process.batchRecordReportName,
    hasMachinery: Boolean(machinery?.machineryId || machinery?.machineryCode)
  }
}

const formatNumber = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

onMounted(() => {
  getList()
})

watch(
  () => props.routeId,
  () => {
    keyword.value = ''
    getList()
  }
)
</script>

<style scoped>
.route-mes-process-list {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.route-mes-process-list__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #edf1f6;
  padding: 14px 16px;
}

.route-mes-process-list__title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.route-mes-process-list__title strong {
  color: #172033;
  font-size: 15px;
}

.route-mes-process-list__title span {
  color: #6b7280;
  font-size: 13px;
}

.route-mes-process-list__search {
  width: 320px;
}

.route-mes-process-list__hint {
  border-width: 0 0 1px;
  border-radius: 0;
}

.route-mes-process-list__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
  color: #263247;
  font-size: 0.9rem;
  font-weight: 600;
}

.route-mes-process-list__table :deep(.cell) {
  padding: 7px 10px;
  line-height: 1.28;
}

.cell-main,
.cell-sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-main {
  color: #172033;
  font-weight: 600;
}

.cell-sub {
  margin-top: 3px;
  color: #4b5563;
  font-size: 12px;
}
</style>
