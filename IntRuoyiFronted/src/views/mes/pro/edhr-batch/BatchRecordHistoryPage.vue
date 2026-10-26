<template>
  <ContentWrap class="edhr-record-viewport">
    <div
      class="edhr-history-standard-list"
      data-edhr-batch-history-page
      data-edhr-history-standard-list
    >
      <el-form
        :inline="true"
        :model="queryParams"
        class="edhr-history-standard-list__toolbar edhr-single-line-query"
        @submit.prevent
      >
        <el-form-item label="类型">
          <el-select
            v-model="queryParams.recordType"
            clearable
            placeholder="全部类型"
            class="!w-180px"
            @change="handleTypeChange"
          >
            <el-option
              v-for="item in availableTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="queryParams.recordType"
          label="工单号"
        >
          <el-input
            v-model="queryParams.workOrderCode"
            clearable
            class="!w-190px"
            data-edhr-history-work-order-filter
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item v-if="isBatchType" label="产品名称">
          <el-input
            v-model="queryParams.productName"
            clearable
            class="!w-190px"
            data-edhr-history-product-name-filter
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item
          v-if="queryParams.recordType"
          label="批次号"
        >
          <el-input
            v-model="queryParams.batchCode"
            clearable
            class="!w-190px"
            data-edhr-history-batch-code-filter
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item v-if="queryParams.recordType === 'history'" label="放行时间">
          <el-date-picker
            v-model="queryParams.releaseApprovedTime"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="!w-360px"
            data-edhr-history-release-time-filter
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-edhr-history-query @click="handleQuery">查询</el-button>
          <el-button data-edhr-history-reset @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <el-table
        height="100%"
        v-loading="loading"
        :data="batchList"
        row-key="key"
        border
        stripe
        data-edhr-history-table
        data-edhr-history-batch-list
      >
        <el-table-column prop="recordCode" label="记录编号" min-width="200" show-overflow-tooltip />
        <el-table-column label="批次执行编码" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">
            <span data-edhr-history-batch-item>{{ row.batchExecutionCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="工单号" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span data-edhr-history-work-order-code>{{ row.workOrderCode }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="productName"
          label="产品名称"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column label="批次号" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span data-edhr-history-batch-code>{{ row.batchCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="业务时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.occurredAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag data-edhr-history-batch-status>{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <ReleaseTaskNotificationEntry
              v-if="row.recordType === 'history'"
              v-hasPermi="['mes:pro-edhr-work-task:query']"
              :batch-execution-id="row.batchExecutionId"
              :release-transaction-id="row.releaseTransactionId"
            />
            <el-button
              link
              type="primary"
              data-edhr-history-detail-action
              @click="openActiveOrderDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.recordType === 'history' && reverseTraceAllowed"
              link
              type="primary"
              data-edhr-history-reverse-trace
              @click="openReverseTrace(row)"
            >
              反查
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="loadError ? '加载失败，请重试' : '暂无追溯记录'" />
        </template>
      </el-table>

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

      <Pagination
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getBatchList"
      />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import '@/views/mes/pro/edhr-batch/record-viewport.css'
import ReleaseTaskNotificationEntry from '../production-release/components/ReleaseTaskNotificationEntry.vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BatchReverseTracePanel from './components/BatchReverseTracePanel.vue'
import { canReverseTrace, readReverseTraceState } from './reverseTraceState'
import { hasPermission } from '@/directives/permission/hasPermi'
import { loadHistoryPage } from './historyPagination'
import {
  buildHistorySources,
  historyTypes,
  type HistoryRow,
  type HistoryFilters
} from './historySources'
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const loadError = ref('')
const batchList = ref<HistoryRow[]>([])
const availableTypes = computed(() =>
  historyTypes.filter((item) => hasPermission([item.permission]))
)
const isBatchType = computed(() => ['execution', 'history'].includes(queryParams.recordType))
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
  recordType: '',
  pageNo: 1,
  pageSize: 10,
  workOrderCode: '',
  productName: '',
  batchCode: '',
  releaseApprovedTime: undefined
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

const handleQuery = () => {
  queryParams.pageNo = 1
  void getBatchList()
}

const handleTypeChange = () => {
  queryParams.workOrderCode = ''
  queryParams.productName = ''
  queryParams.batchCode = ''
  queryParams.releaseApprovedTime = undefined
  handleQuery()
}

const resetQuery = () => {
  queryParams.recordType = ''
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.workOrderCode = ''
  queryParams.productName = ''
  queryParams.batchCode = ''
  queryParams.releaseApprovedTime = undefined
  void getBatchList()
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

const openReverseTrace = (batch: HistoryRow) => {
  if (!reverseTraceAllowed.value) return
  if (!batch.batchExecutionId) {
    ElMessage.error('当前历史追溯记录缺少批次执行编号，无法反查。')
    return
  }
  reverseTraceBatchExecutionId.value = String(batch.batchExecutionId)
  reverseTraceRestoreKey.value = undefined
  reverseTraceVisible.value = true
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
    if (route.query.workOrderCode || route.query.batchCode) queryParams.recordType = 'history'
    queryParams.workOrderCode = String(route.query.workOrderCode || '').trim()
    queryParams.batchCode = String(route.query.batchCode || '').trim()
  }
  historyMounted = true
  void getBatchList()
})
</script>

<style scoped>
.edhr-history-standard-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-history-standard-list__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
  border: 1px solid #dbe3ef;

  background: #ffffff;
  padding: 12px 16px 4px;
}

.edhr-history-standard-list :deep(.el-table) {
  width: 100%;
}

@media (min-width: 1181px) {
  .edhr-single-line-query {
    display: flex;
    flex-wrap: nowrap;
    gap: 12px;
    align-items: center;
  }
  .edhr-single-line-query > :deep(.el-form-item) {
    flex: 1 1 0;
    min-width: 0;
    margin-right: 0;
  }
  .edhr-single-line-query > :deep(.el-form-item:last-child) {
    flex: 0 0 auto;
  }
  .edhr-single-line-query :deep(.el-form-item__content) {
    min-width: 0;
    flex-wrap: nowrap;
  }
  .edhr-single-line-query :deep(.el-input),
  .edhr-single-line-query :deep(.el-select),
  .edhr-single-line-query :deep(.el-date-editor) {
    width: 100% !important;
    min-width: 0;
  }
}
</style>
