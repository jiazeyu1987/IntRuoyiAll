<!-- ERP 即时库存列表 -->
<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      class="-mb-15px"
      label-width="96px"
    >
      <el-form-item label="物料编码" prop="materialNumber">
        <el-input v-model="queryParams.materialNumber" placeholder="请输入物料编码" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料名称" prop="materialName">
        <el-input v-model="queryParams.materialName" placeholder="请输入物料名称" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入仓库" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="批号" prop="lotNumber">
        <el-input v-model="queryParams.lotNumber" placeholder="请输入批号" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="warning"
          plain
          :loading="kingdeeSyncLoading"
          @click="handleIncrementalSync"
          v-hasPermi="['infra:job:trigger']"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> 增量同步
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="物料编码" align="center" prop="materialNumber" width="160" fixed="left" />
      <el-table-column label="物料名称" align="center" prop="materialName" width="180" />
      <el-table-column label="规格型号" align="center" prop="materialSpecification" width="180" />
      <el-table-column label="仓库" align="center" prop="warehouseName" width="180" />
      <el-table-column label="批号" align="center" prop="lotNumber" width="140" />
      <el-table-column label="单位" align="center" prop="unitName" width="90" />
      <el-table-column label="库存数量" align="center" prop="quantity" :formatter="erpCountTableColumnFormatter" width="120" />
      <el-table-column label="库存组织" align="center" prop="stockOrgName" width="180" />
      <el-table-column label="ERP修改时间" align="center" prop="sourceModifyTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="最后同步时间" align="center" prop="lastSyncTime" :formatter="dateFormatter" width="180" />
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
import { isSearchFormInputEmpty } from '@/utils/search'
import { dateFormatter } from '@/utils/formatTime'
import { erpCountTableColumnFormatter } from '@/utils'
import { ErpInventoryListApi, ErpInventoryListVO } from '@/api/erp/production/inventory-list'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'

defineOptions({ name: 'ErpInventoryList' })

const message = useMessage()
const loading = ref(true)
const kingdeeSyncLoading = ref(false)
const list = ref<ErpInventoryListVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  materialNumber: undefined,
  materialName: undefined,
  warehouseName: undefined,
  lotNumber: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpInventoryListApi.getPage(queryParams)
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
  queryFormRef.value.resetFields()
  handleQuery(true)
}

const handleIncrementalSync = async () => {
  kingdeeSyncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSyncJob('kingdeeStockSyncJob')
    message.success('即时库存增量同步任务已提交')
    await getList()
  } finally {
    kingdeeSyncLoading.value = false
  }
}

onActivated(() => {
  getList()
})

onMounted(() => {
  getList()
})
</script>
