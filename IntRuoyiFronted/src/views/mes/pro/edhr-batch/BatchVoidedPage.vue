<template>
  <ContentWrap>
    <ActiveOrderDetailLayout v-if="detailVisible" data-edhr-batch-voided-detail>
      <el-button data-edhr-batch-voided-detail-back @click="detailVisible = false">
        返回
      </el-button>
      <ActiveOrderSubmissionDetailPanel
        :detail="orderDetail?.detail"
        :production-material-lists="orderDetail?.productionMaterialLists || []"
        :loading="detailLoading"
        :error="detailError"
        :pqc-release-application-id="detailRow?.applicationId"
        @retry="retryOrderDetail"
      />
    </ActiveOrderDetailLayout>
    <div v-else>
      <EdhrBatchRecordTabs active-tab="voided" />
      <el-form :inline="true" :model="queryParams" class="mt-16px" @submit.prevent="handleQuery">
        <el-form-item label="工单号">
          <el-input v-model="queryParams.workOrderCode" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="queryParams.batchCode" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-table
        v-loading="loading"
        :data="list"
        row-key="applicationId"
        empty-text="暂无已作废放行申请"
        border
      >
        <el-table-column label="工单号" prop="workOrderCode" min-width="180" />
        <el-table-column label="批次号" prop="batchCode" min-width="150" />
        <el-table-column label="申请编号" prop="applicationId" min-width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag type="info">{{ resolveStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="不合格处置" min-width="240">
          <template #default="{ row }">
            <div>{{ resolveDispositionLabel(row.nonconformanceDisposition) }}</div>
            <div v-if="row.nonconformanceReason" class="text-secondary">
              {{ row.nonconformanceReason }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="处置时间" prop="nonconformanceClosedAt" min-width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import ActiveOrderSubmissionDetailPanel from '../processpool/components/ActiveOrderSubmissionDetailPanel.vue'
import ActiveOrderDetailLayout from '../processpool/components/ActiveOrderDetailLayout.vue'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'
import {
  getPqcProductionReleaseOrderDetail,
  getPqcProductionReleasePage,
  PQC_RELEASE_VIEW_VOIDED,
  type MesPqcProductionReleasePageItemRespVO
} from '@/api/mes/pro/productionRelease'

defineOptions({ name: 'MesProEdhrBatchVoided' })

const loading = ref(false)
const loadError = ref('')
const list = ref<MesPqcProductionReleasePageItemRespVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailRow = ref<MesPqcProductionReleasePageItemRespVO>()
const orderDetail = ref<Awaited<ReturnType<typeof getPqcProductionReleaseOrderDetail>>>()
let detailRequestSequence = 0

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  workOrderCode: '',
  batchCode: ''
})

const resolveErrorMessage = (error: unknown) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return '已作废放行申请加载失败，请重试。'
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getPqcProductionReleasePage({
      ...queryParams,
      viewStatus: PQC_RELEASE_VIEW_VOIDED,
      workOrderCode: queryParams.workOrderCode.trim() || undefined,
      batchCode: queryParams.batchCode.trim() || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  return getList()
}

const resetQuery = () => {
  Object.assign(queryParams, { pageNo: 1, pageSize: 10, workOrderCode: '', batchCode: '' })
  return getList()
}

const resolveStatusLabel = (row: MesPqcProductionReleasePageItemRespVO) =>
  row.underReview ? '评审中' : '已作废'

const resolveDispositionLabel = (disposition?: string) => {
  if (disposition === 'rework') return '返工'
  if (disposition === 'concession_release') return '让步放行'
  if (disposition === 'void') return '作废'
  return disposition || '--'
}

const openDetail = async (row: MesPqcProductionReleasePageItemRespVO) => {
  const sequence = ++detailRequestSequence
  detailRow.value = row
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  orderDetail.value = undefined
  try {
    const result = await getPqcProductionReleaseOrderDetail(row.applicationId)
    if (sequence === detailRequestSequence) orderDetail.value = result
  } catch (error) {
    if (sequence === detailRequestSequence) detailError.value = resolveErrorMessage(error)
  } finally {
    if (sequence === detailRequestSequence) detailLoading.value = false
  }
}

const retryOrderDetail = () => {
  if (detailRow.value) void openDetail(detailRow.value)
}

onMounted(getList)
</script>
