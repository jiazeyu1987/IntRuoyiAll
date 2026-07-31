<template>
  <doc-alert title="【生产】MES工序" url="https://doc.iocoder.cn/mes/pro/process-route/" />

  <ContentWrap>
    <div class="mes-process-page">
      <div class="mes-process-page__toolbar">
        <el-form
          ref="queryFormRef"
          :model="queryParams"
          :inline="true"
          label-width="76px"
          class="mes-process-page__query"
        >
          <el-form-item label="关键词" prop="keyword">
            <el-input
              v-model="queryParams.keyword"
              class="mes-process-page__search"
              clearable
              placeholder="产品 / 工序 / 设备编码 / 批记录工序"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">
              <Icon icon="ep:search" class="mr-5px" /> 搜索
            </el-button>
            <el-button @click="resetQuery">
              <Icon icon="ep:refresh" class="mr-5px" /> 重置
            </el-button>
          </el-form-item>
        </el-form>
        <div class="mes-process-page__hint">
          只读列表：数据来自压力泵工序.xlsx 的二代压力泵工作表，按 Excel 有效工序行原样展示，不多不少。
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        :show-overflow-tooltip="true"
        row-key="rowKey"
        class="mes-process-page__table"
        height="620"
      >
        <el-table-column label="产品名称" prop="productName" min-width="160" fixed="left">
          <template #default="{ row }">{{ formatSourceText(row.productName) }}</template>
        </el-table-column>
        <el-table-column label="设备编码" prop="sourceMachineryCodes" min-width="150">
          <template #default="{ row }">{{ formatSourceText(row.sourceMachineryCodes) }}</template>
        </el-table-column>
        <el-table-column label="工序名称" prop="mesProcessName" min-width="190">
          <template #default="{ row }">{{ formatSourceText(row.mesProcessName) }}</template>
        </el-table-column>
        <el-table-column label="设备名称" prop="sourceMachineryName" min-width="180">
          <template #default="{ row }">{{ formatSourceText(row.sourceMachineryName) }}</template>
        </el-table-column>
        <el-table-column label="设备数量" prop="sourceMachineryQuantity" width="96" align="center">
          <template #default="{ row }">{{ formatSourceText(row.sourceMachineryQuantity) }}</template>
        </el-table-column>
        <el-table-column label="10.5小时日产能" prop="dailyCapacity10_5" width="140" align="right">
          <template #default="{ row }">{{ formatSourceText(row.dailyCapacity10_5) }}</template>
        </el-table-column>
        <el-table-column label="日常工序人力" prop="dailyWorkerQuantity" width="120" align="center">
          <template #default="{ row }">{{ formatSourceText(row.dailyWorkerQuantity) }}</template>
        </el-table-column>
        <el-table-column label="工序编码" prop="mesProcessCode" min-width="110">
          <template #default="{ row }">{{ formatSourceText(row.mesProcessCode) }}</template>
        </el-table-column>
        <el-table-column label="工序单价" prop="processPrice" width="100" align="right">
          <template #default="{ row }">{{ formatSourceText(row.processPrice) }}</template>
        </el-table-column>
        <el-table-column label="工序是否报工" prop="feedbackFlag" width="120" align="center">
          <template #default="{ row }">{{ formatSourceText(row.feedbackFlag) }}</template>
        </el-table-column>
        <el-table-column label="工序是否形成批记录" prop="batchRecordFlag" min-width="160" align="center">
          <template #default="{ row }">{{ formatSourceText(row.batchRecordFlag) }}</template>
        </el-table-column>
        <el-table-column label="批记录工序名称" prop="batchRecordProcessName" min-width="160">
          <template #default="{ row }">{{ formatSourceText(row.batchRecordProcessName) }}</template>
        </el-table-column>
      </el-table>
      <div class="mes-process-page__footer">
        <span>共 {{ total }} 条</span>
        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { MesProcessApi, type MesProcessVO } from '@/api/mes/pro/mes-process'

defineOptions({ name: 'MesProMesProcess' })

const loading = ref(false)
const list = ref<MesProcessVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 50,
  keyword: undefined as string | undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await MesProcessApi.getMesProcessPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const formatSourceText = (value?: string | number | null) => {
  if (value === undefined || value === null) return ''
  return String(value)
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.mes-process-page {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.mes-process-page__toolbar {
  border-bottom: 1px solid #edf1f6;
  padding: 14px 16px 0;
}

.mes-process-page__query {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.mes-process-page__search {
  width: 360px;
}

.mes-process-page__hint {
  padding: 0 0 14px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
}

.mes-process-page__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
  color: #263247;
  font-size: 0.9rem;
  font-weight: 600;
}

.mes-process-page__table :deep(.cell) {
  padding: 7px 10px;
  line-height: 1.28;
}

.mes-process-page__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  border-top: 1px solid #edf1f6;
  padding: 0 14px;
  color: #4b5563;
  font-size: 13px;
}
</style>