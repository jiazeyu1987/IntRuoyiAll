<template>
  <doc-alert title="【ERP】金蝶调拨单只读展示" url="https://doc.iocoder.cn/erp/" />

  <ContentWrap>
    <el-alert
      title="该页面仅展示金蝶直接调拨单同步快照，不接入本地库存调拨业务流程。"
      type="info"
      :closable="false"
      show-icon
      class="mb-12px"
    />
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="88px"
    >
      <el-form-item label="调拨单号" prop="sourceBillNo">
        <el-input
          v-model="queryParams.sourceBillNo"
          placeholder="请输入调拨单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="单据状态" prop="documentStatus">
        <el-input
          v-model="queryParams.documentStatus"
          placeholder="请输入金蝶状态"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="调拨方向" prop="transferDirect">
        <el-input
          v-model="queryParams.transferDirect"
          placeholder="请输入调拨方向"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="单据日期" prop="billDate">
        <el-date-picker
          v-model="queryParams.billDate"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-260px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          :loading="syncLoading"
          @click="handleKingdeeSync"
          v-hasPermi="['infra:job:trigger']"
        >
          <Icon icon="ep:refresh-right" class="mr-5px" /> 增量同步
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column type="expand">
        <template #default="{ row }">
          <el-table :data="row.items || []" border size="small">
            <el-table-column label="物料编码" prop="materialNumber" min-width="150" />
            <el-table-column label="物料名称" prop="materialName" min-width="180" />
            <el-table-column label="单位" prop="unitName" width="90" />
            <el-table-column label="调拨数量" prop="quantity" width="120" />
            <el-table-column label="调出仓库" prop="fromWarehouseName" min-width="160" />
            <el-table-column label="调入仓库" prop="toWarehouseName" min-width="160" />
            <el-table-column label="调出仓位" prop="fromStockLocation" min-width="150" />
            <el-table-column label="调入仓位" prop="toStockLocation" min-width="150" />
            <el-table-column label="批号" prop="lotNumber" min-width="130" />
          </el-table>
        </template>
      </el-table-column>
      <el-table-column label="调拨单号" prop="sourceBillNo" min-width="170" />
      <el-table-column label="单据状态" prop="documentStatus" width="110" />
      <el-table-column label="日期" min-width="160">
        <template #default="{ row }">{{ formatDate(row.billDate) || '-' }}</template>
      </el-table-column>
      <el-table-column label="调拨方向" prop="transferDirect" min-width="120" />
      <el-table-column label="物料" prop="materialNames" min-width="220" />
      <el-table-column label="调出仓库" min-width="160">
        <template #default="{ row }">{{ firstItem(row)?.fromWarehouseName || '-' }}</template>
      </el-table-column>
      <el-table-column label="调入仓库" min-width="160">
        <template #default="{ row }">{{ firstItem(row)?.toWarehouseName || '-' }}</template>
      </el-table-column>
      <el-table-column label="ERP修改时间" min-width="170">
        <template #default="{ row }">{{ formatDate(row.sourceModifyTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="最后同步时间" min-width="170">
        <template #default="{ row }">{{ formatDate(row.lastSyncTime) || '-' }}</template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ErpKingdeeStockMoveApi, ErpKingdeeStockMoveVO } from '@/api/erp/stock/kingdeeStockMove'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'ErpKingdeeStockMove' })

const message = useMessage()
const loading = ref(true)
const syncLoading = ref(false)
const list = ref<ErpKingdeeStockMoveVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceBillNo: undefined,
  documentStatus: undefined,
  transferDirect: undefined,
  billDate: []
})

const firstItem = (row: ErpKingdeeStockMoveVO) => row.items?.[0]

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpKingdeeStockMoveApi.getStockMovePage(queryParams)
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

const handleKingdeeSync = async () => {
  syncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSyncJob('kingdeeStockMoveSyncJob')
    message.success('金蝶调拨单增量同步任务已提交')
    await getList()
  } finally {
    syncLoading.value = false
  }
}

onMounted(getList)
</script>
