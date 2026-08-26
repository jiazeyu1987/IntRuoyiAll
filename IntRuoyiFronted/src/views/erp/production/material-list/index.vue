<!-- ERP 生产用料清单列表 -->
<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      class="-mb-15px"
      label-width="96px"
    >
      <el-form-item label="单据编号" prop="sourceBillNo">
        <el-input
          v-model="queryParams.sourceBillNo"
          placeholder="请输入单据编号"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品编码" prop="productCode">
        <el-input
          v-model="queryParams.productCode"
          placeholder="请输入产品编码"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产订单" prop="productionOrderNo">
        <el-input
          v-model="queryParams.productionOrderNo"
          placeholder="请输入生产订单编号"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="子项编码" prop="childMaterialCode">
        <el-input
          v-model="queryParams.childMaterialCode"
          placeholder="请输入子项物料编码"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="子项名称" prop="childMaterialName">
        <el-input
          v-model="queryParams.childMaterialName"
          placeholder="请输入子项物料名称"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
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
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      class="erp-production-material-list__table"
    >
      <el-table-column label="单据编号" align="center" prop="sourceBillNo" width="220" fixed="left">
        <template #default="{ row }">
          <el-link
            type="primary"
            class="erp-production-material-list__bill-link"
            @click="openDetailDialog(row)"
          >
            {{ row.sourceBillNo }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="子项数量" align="center" prop="lineCount" width="120" />
      <el-table-column label="生产工单" align="center" prop="productionOrderSummary" min-width="220">
        <template #default="{ row }">
          <el-link
            v-if="row.productionOrderCount === 1 && row.productionOrderSummary"
            type="primary"
            @click="handleOpenGroupWorkOrder(row)"
          >
            {{ row.productionOrderSummary }}
          </el-link>
          <span v-else>{{ row.productionOrderSummary || '无' }}</span>
          <span v-if="row.productionOrderCount > 1">（{{ row.productionOrderCount }}）</span>
        </template>
      </el-table-column>
      <el-table-column
        label="ERP修改时间"
        align="center"
        prop="sourceModifyTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column
        label="最后同步时间"
        align="center"
        prop="lastSyncTime"
        :formatter="dateFormatter"
        width="180"
      />
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <el-dialog
    v-model="detailDialogVisible"
    :title="detailDialogTitle"
    width="1080px"
    destroy-on-close
    class="erp-production-material-list__dialog"
  >
    <el-table
      v-loading="detailLoading"
      :data="detailList"
      :stripe="true"
      :show-overflow-tooltip="true"
      max-height="520"
      class="erp-production-material-list__detail-table"
    >
      <el-table-column
        label="子项物料编码"
        align="center"
        prop="childMaterialCode"
        min-width="180"
        fixed="left"
      />
      <el-table-column
        label="子项物料名称"
        align="center"
        prop="childMaterialName"
        min-width="200"
      />
      <el-table-column
        label="规格型号"
        align="center"
        prop="childMaterialSpecification"
        min-width="180"
      />
      <el-table-column
        label="子项类型"
        align="center"
        prop="childMaterialType"
        min-width="120"
      />
      <el-table-column
        label="分子"
        align="center"
        prop="numerator"
        :formatter="erpCountTableColumnFormatter"
        width="110"
      />
      <el-table-column
        label="分母"
        align="center"
        prop="denominator"
        :formatter="erpCountTableColumnFormatter"
        width="110"
      />
      <el-table-column
        label="子项单位"
        align="center"
        prop="childUnitName"
        min-width="120"
      />
      <el-table-column label="对应生产订单" align="center" min-width="180">
        <template #default="{ row }">
          <el-link v-if="row.workOrderId" type="primary" @click="handleOpenWorkOrder(row)">
            {{ row.workOrderCode || row.productionOrderNo }}
          </el-link>
          <span v-else>{{ row.productionOrderNo || '无' }}</span>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { dateFormatter } from '@/utils/formatTime'
import { erpCountTableColumnFormatter } from '@/utils'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import {
  ErpProductionMaterialListApi,
  ErpProductionMaterialListDetailVO,
  ErpProductionMaterialListGroupVO
} from '@/api/erp/production/material-list'

/** ERP 生产用料清单列表 */
defineOptions({ name: 'ErpProductionMaterialList' })

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(true)
const kingdeeSyncLoading = ref(false)
const detailLoading = ref(false)
const list = ref<ErpProductionMaterialListGroupVO[]>([])
const detailList = ref<ErpProductionMaterialListDetailVO[]>([])
const total = ref(0)
const detailDialogVisible = ref(false)
const currentSourceBillNo = ref('')
const detailDialogTitle = computed(() =>
  currentSourceBillNo.value ? `单据明细 - ${currentSourceBillNo.value}` : '单据明细'
)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceBillNo: undefined as string | undefined,
  productCode: undefined as string | undefined,
  productionOrderNo: undefined as string | undefined,
  childMaterialCode: undefined as string | undefined,
  childMaterialName: undefined as string | undefined
})
const queryFormRef = ref()

const applyRouteQuery = () => {
  queryParams.productionOrderNo =
    typeof route.query.productionOrderNo === 'string' ? route.query.productionOrderNo : undefined
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ErpProductionMaterialListApi.getGroupPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleOpenWorkOrder = async (row: ErpProductionMaterialListDetailVO) => {
  if (!row.workOrderId) {
    return
  }
  await router.push({
    path: '/mes/pro/work-order',
    query: { code: row.workOrderCode, openId: row.workOrderId }
  })
}

const handleOpenGroupWorkOrder = async (row: ErpProductionMaterialListGroupVO) => {
  if (row.productionOrderCount !== 1 || !row.productionOrderSummary) {
    return
  }
  await router.push({
    path: '/mes/pro/work-order',
    query: { code: row.productionOrderSummary }
  })
}

/** 打开明细弹窗 */
const openDetailDialog = async (row: ErpProductionMaterialListGroupVO) => {
  currentSourceBillNo.value = row.sourceBillNo
  detailDialogVisible.value = true
  detailLoading.value = true
  detailList.value = []
  try {
    detailList.value = await ErpProductionMaterialListApi.getDetailList(row.sourceBillNo)
  } catch (error) {
    detailDialogVisible.value = false
    currentSourceBillNo.value = ''
    message.error((error as Error)?.message || '加载单据明细失败')
  } finally {
    detailLoading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery(true)
}

const handleIncrementalSync = async () => {
  kingdeeSyncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSync('PRODUCTION_MATERIAL_LIST')
    message.success('生产用料清单增量同步任务已提交')
    await getList()
  } finally {
    kingdeeSyncLoading.value = false
  }
}

watch(detailDialogVisible, (visible) => {
  if (visible) {
    return
  }
  currentSourceBillNo.value = ''
  detailList.value = []
  detailLoading.value = false
})

onActivated(() => {
  applyRouteQuery()
  getList()
})

onMounted(() => {
  applyRouteQuery()
  getList()
})
</script>

<style scoped lang="scss">
.erp-production-material-list__table {
  :deep(.el-link) {
    font-weight: 600;
  }
}

.erp-production-material-list__bill-link {
  padding: 0;
}

.erp-production-material-list__detail-table {
  :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }
}
</style>
