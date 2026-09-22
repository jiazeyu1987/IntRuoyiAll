<template>
  <ContentWrap>
    <EdhrBatchRecordTabs active-tab="voided" />
    <el-form :inline="true" :model="queryParams" class="mt-16px" @submit.prevent="handleQuery">
      <el-form-item label="工单号">
        <el-input v-model="queryParams.workOrderCode" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="产品名称">
        <el-input v-model="queryParams.productName" clearable @keyup.enter="handleQuery" />
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
    <el-table v-loading="loading" :data="list" row-key="id" empty-text="暂无已作废批次" border>
      <el-table-column prop="batchExecutionCode" label="批次执行编号" min-width="180" />
      <el-table-column prop="workOrderCode" label="工单号" min-width="160" />
      <el-table-column prop="productName" label="产品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="batchCode" label="批次号" min-width="160" />
      <el-table-column label="状态" width="100">
        <template #default><el-tag type="info">已作废</el-tag></template>
      </el-table-column>
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
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'
import {
  EDHR_BATCH_STATUS_VOIDED,
  getEdhrBatchExecutionPage,
  type EdhrBatchExecutionRespVO
} from '@/api/mes/pro/edhr/batchExecution'

defineOptions({ name: 'MesProEdhrBatchVoided' })
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrBatchExecutionRespVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  workOrderCode: '',
  productName: '',
  batchCode: ''
})

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrBatchExecutionPage({
      ...queryParams,
      status: EDHR_BATCH_STATUS_VOIDED,
      completedTraceOnly: true
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = error instanceof Error ? error.message : '作废批次加载失败，请重试。'
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  return getList()
}
const resetQuery = () => {
  Object.assign(queryParams, {
    pageNo: 1,
    pageSize: 10,
    workOrderCode: '',
    productName: '',
    batchCode: ''
  })
  return getList()
}
const openDetail = async (row: EdhrBatchExecutionRespVO) => {
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/active-order-detail',
    query: {
      batchExecutionId: String(row.id),
      from: '/mes/pro/feedback/edhr-batch-voided'
    }
  })
}
onMounted(getList)
</script>
