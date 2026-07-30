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
              placeholder="产品 / 路线 / 工序 / 设备"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="资源类型" prop="resourceType">
            <el-select
              v-model="queryParams.resourceType"
              class="mes-process-page__select"
              clearable
              placeholder="全部"
            >
              <el-option label="设备" value="MACHINE" />
              <el-option label="人工" value="WORKER" />
              <el-option label="未配置" value="UNCONFIGURED" />
            </el-select>
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
          只读列表：展示一线 MES 工序、设备、工序设置执行工序和批记录工序名称的对应关系。
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
        <el-table-column label="产品名称" prop="productName" min-width="160" fixed="left" />
        <el-table-column label="路线" min-width="190">
          <template #default="{ row }">
            <span class="cell-main">{{ row.routeCode || '-' }}</span>
            <span class="cell-sub">{{ row.routeName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="序号" prop="sort" width="72" align="center" />
        <el-table-column label="MES工序名称" prop="mesProcessName" min-width="150" />
        <el-table-column label="MES工序编码" prop="mesProcessCode" min-width="130" />
        <el-table-column label="执行工序" prop="executionProcessName" min-width="150" />
        <el-table-column label="设备" min-width="210">
          <template #default="{ row }">
            <div v-if="resolveMachineryList(row).length" class="mes-process-page__device-list">
              <span
                v-for="machinery in resolveMachineryList(row)"
                :key="machinery.machineryId || machinery.machineryCode"
                class="mes-process-page__device"
              >
                {{ formatMachinery(machinery) }}
              </span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="设备数量" width="96" align="center">
          <template #default="{ row }">
            {{ row.machineryQuantity ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="10.5小时日产能" width="140" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.dailyCapacity10_5) }}
          </template>
        </el-table-column>
        <el-table-column label="日产人力" width="96" align="center">
          <template #default="{ row }">
            {{ row.dailyWorkerQuantity ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="工序单价" width="100" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.processPrice) }}
          </template>
        </el-table-column>
        <el-table-column label="报工" width="82" align="center">
          <template #default="{ row }">
            {{ row.feedbackEnabled === true ? '是' : row.feedbackEnabled === false ? '否' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="批记录" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="row.batchRecordEnabled ? 'success' : 'info'" effect="light">
              {{ row.batchRecordEnabled ? '是' : '未配置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批记录工序名称" min-width="210">
          <template #default="{ row }">
            <span class="cell-main">{{ row.batchRecordProcessName || '-' }}</span>
            <span class="cell-sub">{{ row.batchRecordReportCode || '-' }}</span>
          </template>
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
import { MesProcessApi, type MesProcessMachineryVO, type MesProcessVO } from '@/api/mes/pro/mes-process'

defineOptions({ name: 'MesProMesProcess' })

const loading = ref(false)
const list = ref<MesProcessVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  resourceType: undefined as string | undefined
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

const resolveMachineryList = (row: MesProcessVO) => row.machineryList || []

const formatMachinery = (machinery: MesProcessMachineryVO) => {
  const code = machinery.machineryCode?.trim()
  const name = machinery.machineryName?.trim()
  if (code && name) return `${code} ${name}`
  return name || code || '-'
}

const formatNumber = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
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
  width: 320px;
}

.mes-process-page__select {
  width: 168px;
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

.mes-process-page__device-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.mes-process-page__device {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
