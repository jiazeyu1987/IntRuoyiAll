<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      label-width="94px"
      class="-mb-15px"
    >
      <el-form-item label="生产领料单号" prop="sourceBillNo">
        <el-input
          v-model="queryParams.sourceBillNo"
          placeholder="请输入生产领料单号"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单据状态" prop="documentStatus">
        <el-input
          v-model="queryParams.documentStatus"
          placeholder="请输入金蝶状态"
          clearable
          class="!w-180px"
          @keyup.enter="handleQuery"
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
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />查询
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" />重置
        </el-button>
        <el-button
          type="primary"
          :loading="syncLoading"
          @click="handleKingdeeSync"
          v-hasPermi="['infra:job:trigger']"
        >
          <Icon icon="ep:refresh-right" class="mr-5px" />增量同步
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      border
      stripe
      show-overflow-tooltip
      row-key="id"
      height="calc(100vh - 304px)"
    >
      <el-table-column type="expand" width="48" fixed="left">
        <template #default="{ row }">
          <div class="pick-list-items">
            <el-table
              :data="row.items || []"
              border
              stripe
              size="small"
              empty-text="暂无领料明细"
            >
              <el-table-column label="物料编码" prop="materialNumber" min-width="150" />
              <el-table-column label="物料名称" prop="materialName" min-width="180" />
              <el-table-column label="规格型号" prop="materialSpecification" min-width="180" />
              <el-table-column label="单位" prop="unitName" width="90" align="center" />
              <el-table-column label="申请数量" prop="requestedQuantity" width="120" align="right" />
              <el-table-column label="实发数量" prop="actualQuantity" width="120" align="right" />
              <el-table-column label="仓库" prop="warehouseName" min-width="160" />
              <el-table-column label="仓位" prop="stockLocationName" min-width="150" />
              <el-table-column label="库存状态" prop="stockStatusName" min-width="130" />
              <el-table-column label="批号" prop="lotNumber" min-width="130" />
              <el-table-column label="生产订单编号" prop="productionOrderNo" min-width="170" />
              <el-table-column label="车间" prop="workshopName" min-width="150" />
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="生产领料单号" prop="sourceBillNo" min-width="190" fixed="left" />
      <el-table-column label="单据状态" prop="documentStatus" width="110" align="center" />
      <el-table-column label="单据日期" prop="billDate" min-width="170" align="center">
        <template #default="{ row }">{{ formatDate(row.billDate) || '-' }}</template>
      </el-table-column>
      <el-table-column label="生产订单编号" prop="productionOrderNos" min-width="190" />
      <el-table-column label="物料" prop="materialNames" min-width="240" />
      <el-table-column label="库存组织" prop="stockOrgName" min-width="170" />
      <el-table-column label="生产组织" prop="productionOrgName" min-width="170" />
      <el-table-column label="领料部门" prop="departmentName" min-width="150" />
      <el-table-column label="ERP 修改时间" prop="sourceModifyTime" min-width="180" align="center">
        <template #default="{ row }">{{ formatDate(row.sourceModifyTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="最后同步时间" prop="lastSyncTime" min-width="180" align="center">
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
import {
  ErpProductionPickListApi,
  type ErpProductionPickListVO
} from '@/api/erp/production/pick-list'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'ErpProductionPickList' })

const message = useMessage()
const loading = ref(true)
const syncLoading = ref(false)
const list = ref<ErpProductionPickListVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceBillNo: undefined as string | undefined,
  documentStatus: undefined as string | undefined,
  billDate: undefined as string[] | undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpProductionPickListApi.getPage(queryParams)
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
    await ErpKingdeeSyncApi.runIncrementalSyncJob('kingdeeProductionPickListSyncJob')
    message.success('生产领料单列表增量同步任务已提交')
    await getList()
  } finally {
    syncLoading.value = false
  }
}

onMounted(getList)
</script>

<style scoped>
.pick-list-items {
  padding: 8px 12px;
  background: #f7f9fc;
}
</style>
