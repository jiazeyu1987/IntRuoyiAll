<template>
  <doc-alert title="【生产】MES工序" url="https://doc.iocoder.cn/mes/pro/process-route/" />

  <!-- 数据源：压力泵工序.xlsx 的二代压力泵工作表，按 Excel 有效工序行原样展示。 -->
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="76px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="产品 / 工序 / 设备编码 / 批记录工序"
          clearable
          @keyup.enter="handleQuery"
          class="!w-360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      row-key="rowKey"
    >
      <el-table-column label="产品名称" align="center" prop="productName" min-width="160" fixed="left">
        <template #default="{ row }">{{ formatSourceText(row.productName) }}</template>
      </el-table-column>
      <el-table-column label="设备编码" align="center" prop="sourceMachineryCodes" min-width="150">
        <template #default="{ row }">{{ formatSourceText(row.sourceMachineryCodes) }}</template>
      </el-table-column>
      <el-table-column label="工序名称" align="center" prop="mesProcessName" min-width="190">
        <template #default="{ row }">{{ formatSourceText(row.mesProcessName) }}</template>
      </el-table-column>
      <el-table-column label="设备名称" align="center" prop="sourceMachineryName" min-width="180">
        <template #default="{ row }">{{ formatSourceText(row.sourceMachineryName) }}</template>
      </el-table-column>
      <el-table-column label="设备数量" align="center" prop="sourceMachineryQuantity" width="96">
        <template #default="{ row }">{{ formatSourceText(row.sourceMachineryQuantity) }}</template>
      </el-table-column>
      <el-table-column label="10.5小时日产能" align="center" prop="dailyCapacity10_5" width="140">
        <template #default="{ row }">{{ formatSourceText(row.dailyCapacity10_5) }}</template>
      </el-table-column>
      <el-table-column label="日常工序人力" align="center" prop="dailyWorkerQuantity" width="120">
        <template #default="{ row }">{{ formatSourceText(row.dailyWorkerQuantity) }}</template>
      </el-table-column>
      <el-table-column label="工序编码" align="center" prop="mesProcessCode" min-width="110">
        <template #default="{ row }">{{ formatSourceText(row.mesProcessCode) }}</template>
      </el-table-column>
      <el-table-column label="工序单价" align="center" prop="processPrice" width="100">
        <template #default="{ row }">{{ formatSourceText(row.processPrice) }}</template>
      </el-table-column>
      <el-table-column label="工序是否报工" align="center" prop="feedbackFlag" width="120">
        <template #default="{ row }">{{ formatSourceText(row.feedbackFlag) }}</template>
      </el-table-column>
      <el-table-column label="工序是否形成批记录" align="center" prop="batchRecordFlag" min-width="160">
        <template #default="{ row }">{{ formatSourceText(row.batchRecordFlag) }}</template>
      </el-table-column>
      <el-table-column label="批记录工序名称" align="center" prop="batchRecordProcessName" min-width="160">
        <template #default="{ row }">{{ formatSourceText(row.batchRecordProcessName) }}</template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { MesProcessApi, type MesProcessVO } from '@/api/mes/pro/mes-process'

defineOptions({ name: 'MesProMesProcess' })

const loading = ref(true)
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

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery(true)
}

const formatSourceText = (value?: string | number | null) => {
  if (value === undefined || value === null) return ''
  return String(value)
}

onMounted(() => {
  getList()
})
</script>
