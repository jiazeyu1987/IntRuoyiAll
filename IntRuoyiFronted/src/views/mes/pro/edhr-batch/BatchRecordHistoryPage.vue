<template>
  <ContentWrap class="edhr-record-viewport">
    <div
      class="edhr-history-standard-list"
      data-edhr-batch-history-page
      data-edhr-history-standard-list
    >
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <UnifiedListTemplate
        table-key="mes.pro.edhr-batch-history"
        single-line-toolbar
        :query-model="queryParams"
        :filter-definitions="filterDefinitions"
        :quick-filter-state="quickFilter.state"
        :selected-filter-definition="quickFilter.selectedDefinition.value"
        :operator-options="quickFilter.operatorOptions.value"
        :columns="columns"
        :column-saving="columnSaving"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:quick-filter-state="updateQuickFilterState"
        @quick-filter-query="quickFilter.applyQuickFilter"
        @column-change="saveColumnConfig"
        @pagination="getBatchList"
        :load-filter-rows="loadColumnFilterRows0"
        @filtered-rows="batchList = $event"
        :list-rows="batchList"
        @column-filter-reset="getBatchList"
      >
        <template #actions><BatchIntegrityCheck /></template>
        <template #table="{ resolveRows }">
          <el-table
            v-loading="loading || columnLoading"
            :data="resolveRows(batchList)"
            height="100%"
            row-key="key"
            border
            stripe
            show-overflow-tooltip
            data-edhr-history-table
            data-edhr-history-batch-list
            data-user-table-column-explicit
            data-user-table-key="mes.pro.edhr-batch-history"
            @header-dragend="handleHeaderDragend"
          >
            <el-table-column
              v-for="column in visibleColumns"
              :key="column.key"
              :column-key="column.key"
              :prop="column.key"
              :label="column.label"
              :width="getColumnWidthString(column.key, column.width)"
              :fixed="column.key === 'operation' ? 'right' : false"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  v-if="column.key === 'operation'"
                  link
                  type="primary"
                  data-edhr-history-detail-action
                  @click="openActiveOrderDetail(row)"
                  >详情</el-button
                >
                <el-tag v-else-if="column.key === 'statusLabel'" data-edhr-history-batch-status>{{
                  row.statusLabel
                }}</el-tag>
                <span
                  v-else-if="
                    ['startedAt', 'pqcReleasedAt', 'releaseApprovedAt'].includes(column.key)
                  "
                  >{{ formatTime(row[column.key]) }}</span
                >
                <span v-else>{{ row[column.key] }}</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="loadError ? '加载失败，请重试' : '暂无追溯记录'" />
            </template>
          </el-table>
        </template>
      </UnifiedListTemplate>
      <el-alert
        v-if="reverseTraceNotice"
        :title="reverseTraceNotice"
        type="info"
        :closable="false"
        show-icon
      />
      <BatchReverseTracePanel
        v-model:visible="reverseTraceVisible"
        :anchor-batch-execution-id="reverseTraceBatchExecutionId"
        :restore-key="reverseTraceRestoreKey"
        :history-list-state="queryParams"
      />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { loadAllListPages } from '@/hooks/web/useListColumnFilter'

import '@/views/mes/pro/edhr-batch/record-viewport.css'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterState
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import { useRoute, useRouter } from 'vue-router'

import BatchIntegrityCheck from './components/BatchIntegrityCheck.vue'
import BatchReverseTracePanel from './components/BatchReverseTracePanel.vue'
import { canReverseTrace, readReverseTraceState } from './reverseTraceState'
import { hasPermission } from '@/directives/permission/hasPermi'
import { loadHistoryPage } from './historyPagination'
import { buildHistorySources, type HistoryRow, type HistoryFilters } from './historySources'
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const loadError = ref('')
const batchList = ref<HistoryRow[]>([])
const total = ref(0)
let listRequestSequence = 0
const reverseTraceVisible = ref(false)
const reverseTraceBatchExecutionId = ref('')
const reverseTraceAllowed = computed(canReverseTrace)
const reverseTraceRestoreKey = ref<string>()
const reverseTraceNotice = computed(() =>
  route.query.reverseTraceExpired === '1'
    ? '原反查状态已丢失、过期或身份权限已变化，请选择原批次重新查询。'
    : ''
)
let historyMounted = false

const queryParams = reactive<HistoryFilters & { pageNo: number; pageSize: number }>({
  pageNo: 1,
  pageSize: 20,
  workOrderCode: '',
  productName: '',
  batchCode: '',
  releaseApprovedTime: undefined,
  tracePerson: '',
  traceEquipment: '',
  traceMaterial: '',
  traceMaterialLot: ''
})

const formatTime = (value?: string | number | null) => {
  if (value == null || value === '') return '--'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

const getBatchList = async () => {
  const sequence = ++listRequestSequence
  loading.value = true
  loadError.value = ''
  try {
    const data = await loadHistoryPage(
      buildHistorySources({ ...queryParams }, hasPermission),
      queryParams.pageNo,
      queryParams.pageSize
    )
    if (sequence !== listRequestSequence) return
    batchList.value = data.list
    total.value = data.total
  } catch (error) {
    if (sequence !== listRequestSequence) return
    batchList.value = []
    total.value = 0
    loadError.value = error instanceof Error ? error.message : '历史追溯列表加载失败。'
  } finally {
    if (sequence === listRequestSequence) loading.value = false
  }
}

const filterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'workOrderCode',
    label: '工单号',
    type: 'text',
    queryParamKey: 'workOrderCode',
    operators: ['contains']
  },
  {
    key: 'productName',
    label: '产品名称',
    type: 'text',
    queryParamKey: 'productName',
    operators: ['contains']
  },
  {
    key: 'batchCode',
    label: '生产批号',
    type: 'text',
    queryParamKey: 'batchCode',
    operators: ['contains']
  },
  {
    key: 'tracePerson',
    label: '人员',
    placeholder: '姓名或工号/账号',
    type: 'text',
    queryParamKey: 'tracePerson',
    operators: ['contains']
  },
  {
    key: 'traceEquipment',
    label: '设备',
    placeholder: '设备编码或名称',
    type: 'text',
    queryParamKey: 'traceEquipment',
    operators: ['contains']
  },
  {
    key: 'traceMaterial',
    label: '物料',
    placeholder: '物料编码或名称',
    type: 'text',
    queryParamKey: 'traceMaterial',
    operators: ['contains']
  },
  {
    key: 'traceMaterialLot',
    label: '物料批号',
    type: 'text',
    queryParamKey: 'traceMaterialLot',
    operators: ['contains']
  },
  {
    key: 'releaseApprovedTime',
    label: '上市放行时间',
    type: 'dateRange',
    queryParamKey: 'releaseApprovedTime',
    operators: ['between']
  }
]
const defaultColumns: UserTableColumnDefinition[] = [
  { key: 'recordCode', label: '记录编号', width: 200 },
  { key: 'batchExecutionCode', label: '批次执行编码', width: 210 },
  { key: 'workOrderCode', label: '工单号', width: 220 },
  { key: 'productName', label: '产品名称', width: 200 },
  { key: 'batchCode', label: '生产批号', width: 210 },
  { key: 'startedAt', label: '开始时间', width: 180 },
  { key: 'pqcReleasedAt', label: 'PQC放行时间', width: 180 },
  { key: 'releaseApprovedAt', label: '上市放行时间', width: 180 },
  { key: 'statusLabel', label: '状态', width: 100 },
  { key: 'operation', label: '操作', width: 90 }
]
const {
  columns,
  loading: columnLoading,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns('mes.pro.edhr-batch-history', defaultColumns)
const visibleColumns = computed(() => columns.value.filter((column) => isColumnVisible(column.key)))
const quickFilter = useTableQuickFilter(
  'mes.pro.edhr-batch-history',
  filterDefinitions,
  queryParams,
  getBatchList
)
const updateQuickFilterState = async (next: Partial<TableQuickFilterState>) => {
  const previousCount = quickFilter.state.conditions?.length ?? 0
  quickFilter.updateState(next)
  if (next.conditions && next.conditions.length < previousCount)
    await quickFilter.applyQuickFilter()
}

const openActiveOrderDetail = async (batch: HistoryRow) => {
  if (!batch.batchExecutionId) {
    ElMessage.error('当前历史追溯记录缺少批次执行编号，无法查看详情。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/active-order-detail',
    query: {
      batchExecutionId: String(batch.batchExecutionId),
      from: '/mes/pro/feedback/edhr-batch-history'
    }
  })
}

watch(
  () => [route.query.reverseTraceRestore, reverseTraceAllowed.value],
  () => {
    reverseTraceVisible.value = false
    const key = route.query.reverseTraceRestore
    if (typeof key !== 'string') return
    if (!reverseTraceAllowed.value) {
      loadError.value = '反查权限已变化，请重新查询。'
      return
    }
    const saved = readReverseTraceState(key)
    if (saved?.historyListState) {
      Object.assign(queryParams, saved.historyListState)
      if (historyMounted) void getBatchList()
    }
    const anchor = saved?.anchorBatchExecutionId || route.query.reverseTraceAnchor
    if (typeof anchor !== 'string' || !/^[1-9]\d*$/.test(anchor)) {
      loadError.value = '原反查状态已丢失，请重新查询。'
      return
    }
    reverseTraceBatchExecutionId.value = anchor
    reverseTraceRestoreKey.value = key
    reverseTraceVisible.value = true
  },
  { immediate: true }
)

onMounted(() => {
  if (!readReverseTraceState(route.query.reverseTraceRestore)?.historyListState) {
    queryParams.workOrderCode = String(route.query.workOrderCode || '').trim()
    queryParams.batchCode = String(route.query.batchCode || '').trim()
  }
  historyMounted = true
  void getBatchList()
})

const loadColumnFilterRows0 = async () => {
  const sources = buildHistorySources({ ...queryParams }, hasPermission)
  return loadAllListPages((pagination) =>
    loadHistoryPage(sources, pagination.pageNo, pagination.pageSize)
  )
}
</script>
