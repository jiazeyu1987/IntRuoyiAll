<!-- ERP 物料清单列表 -->
<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      class="-mb-15px"
      label-width="96px"
    >
      <el-form-item label="BOM编号" prop="bomNumber">
        <el-input v-model="queryParams.bomNumber" placeholder="请输入BOM编号" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="父项编码" prop="parentMaterialCode">
        <el-input v-model="queryParams.parentMaterialCode" placeholder="请输入父项物料编码" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="父项名称" prop="parentMaterialName">
        <el-input v-model="queryParams.parentMaterialName" placeholder="请输入父项物料名称" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="子项编码" prop="childMaterialCode">
        <el-input v-model="queryParams.childMaterialCode" placeholder="请输入子项物料编码" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="子项名称" prop="childMaterialName">
        <el-input v-model="queryParams.childMaterialName" placeholder="请输入子项物料名称" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="warning"
          plain
          :loading="kingdeeSyncLoading"
          @click="handleIncrementalSync"
          v-hasPermi="['erp:kingdee-sync:query']"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> 增量同步
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="父项物料编码" align="center" prop="parentMaterialCode" width="160" fixed="left" />
      <el-table-column label="父项物料名称" align="center" prop="parentMaterialName" width="180" />
      <el-table-column label="BOM编号" align="center" prop="bomNumber" width="150" />
      <el-table-column label="BOM类型" align="center" prop="bomType" width="110" />
      <el-table-column label="审核状态" align="center" prop="documentStatus" width="110" />
      <el-table-column label="行号" align="center" prop="lineNo" width="80" />
      <el-table-column label="子项物料编码" align="center" prop="childMaterialCode" width="160" />
      <el-table-column label="子项物料名称" align="center" prop="childMaterialName" width="180" />
      <el-table-column label="规格型号" align="center" prop="childMaterialSpecification" width="200" />
      <el-table-column label="单位" align="center" prop="childUnitName" width="90" />
      <el-table-column label="分子" align="center" prop="numerator" :formatter="erpCountTableColumnFormatter" width="100" />
      <el-table-column label="分母" align="center" prop="denominator" :formatter="erpCountTableColumnFormatter" width="100" />
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
import { ErpBomListApi, ErpBomListVO } from '@/api/erp/production/bom-list'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'

defineOptions({ name: 'ErpBomList' })

const message = useMessage()
const loading = ref(true)
const kingdeeSyncLoading = ref(false)
const list = ref<ErpBomListVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  bomNumber: undefined,
  parentMaterialCode: undefined,
  parentMaterialName: undefined,
  childMaterialCode: undefined,
  childMaterialName: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpBomListApi.getPage(queryParams)
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
    await ErpKingdeeSyncApi.runIncrementalSync('BOM')
    message.success('产品 BOM 增量同步任务已提交')
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
