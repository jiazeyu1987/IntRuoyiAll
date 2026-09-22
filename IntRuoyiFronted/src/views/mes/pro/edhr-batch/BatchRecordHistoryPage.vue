<template>
  <ContentWrap>
    <div
      class="edhr-history-standard-list"
      data-edhr-batch-history-page
      data-edhr-history-standard-list
    >
      <EdhrBatchRecordTabs active-tab="history" />

      <el-form :inline="true" :model="queryParams" class="edhr-history-standard-list__toolbar" @submit.prevent>
        <el-form-item label="工单号">
          <el-input
            v-model="queryParams.workOrderCode"
            clearable
            class="!w-190px"
            data-edhr-history-work-order-filter
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input
            v-model="queryParams.productName"
            clearable
            class="!w-190px"
            data-edhr-history-product-name-filter
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input
            v-model="queryParams.batchCode"
            clearable
            class="!w-190px"
            data-edhr-history-batch-code-filter
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="放行时间">
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
        v-loading="loading"
        :data="batchList"
        border
        stripe
        data-edhr-history-table
        data-edhr-history-batch-list
      >
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
        <el-table-column prop="productName" label="产品名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="批次号" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span data-edhr-history-batch-code>{{ row.batchCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="放行时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.releaseApprovedAt) }}</template>
        </el-table-column>
        <el-table-column label="放行状态" width="120">
          <template #default="{ row }">
            <el-tag type="success" data-edhr-history-batch-status>{{ formatBatchStatus(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              data-edhr-history-detail-action
              data-edhr-history-active-order-detail
              @click="openActiveOrderDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无已放行批次" />
        </template>
      </el-table>

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
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'
import {
  getEdhrBatchExecutionPage,
  type EdhrBatchExecutionPageReqVO,
  type EdhrBatchExecutionRespVO
} from '@/api/mes/pro/edhr/batchExecution'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const loadError = ref('')
const batchList = ref<EdhrBatchExecutionRespVO[]>([])
const total = ref(0)

const queryParams = reactive<EdhrBatchExecutionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  workOrderCode: '',
  productName: '',
  batchCode: '',
  releasedOnly: true,
  releaseApprovedTime: undefined
})

const formatTime = (value?: string | number | null) => {
  if (value == null || value === '') return '--'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

const formatBatchStatus = (row: EdhrBatchExecutionRespVO) => {
  if (Number(row.status) === 40) return '已归档'
  if (row.releaseStatus === 'RELEASED') return '已放行'
  return row.releaseStatus || '--'
}

const buildQuery = (): EdhrBatchExecutionPageReqVO => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  workOrderCode: queryParams.workOrderCode?.trim() || undefined,
  productName: queryParams.productName?.trim() || undefined,
  batchCode: queryParams.batchCode?.trim() || undefined,
  releasedOnly: true,
  releaseApprovedTime: queryParams.releaseApprovedTime
})

const getBatchList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrBatchExecutionPage(buildQuery())
    batchList.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    batchList.value = []
    total.value = 0
    loadError.value = error instanceof Error ? error.message : '历史追溯列表加载失败。'
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  void getBatchList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.workOrderCode = ''
  queryParams.productName = ''
  queryParams.batchCode = ''
  queryParams.releaseApprovedTime = undefined
  void getBatchList()
}

const openActiveOrderDetail = async (batch: EdhrBatchExecutionRespVO) => {
  if (!batch.id) {
    ElMessage.error('当前历史追溯记录缺少批次执行编号，无法查看详情。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/active-order-detail',
    query: {
      batchExecutionId: String(batch.id),
      from: '/mes/pro/feedback/edhr-batch-history'
    }
  })
}

onMounted(() => {
  queryParams.workOrderCode = String(route.query.workOrderCode || '').trim()
  queryParams.batchCode = String(route.query.batchCode || '').trim()
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
  border-top: 0;
  background: #ffffff;
  padding: 12px 16px 4px;
}

.edhr-history-standard-list :deep(.el-table) {
  width: 100%;
}
</style>
