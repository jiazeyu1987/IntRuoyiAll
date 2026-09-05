<template>
  <ContentWrap>
    <UnifiedListTemplate
      table-key="erp.production.replenishmentList.main"
      single-line-toolbar
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="replenishmentListQuickFilterDefinitions"
      :quick-filter-state="replenishmentListQuickFilterState"
      :selected-filter-definition="replenishmentListSelectedFilterDefinition"
      :operator-options="replenishmentListFilterOperatorOptions"
      :columns="replenishmentListColumns"
      :column-saving="replenishmentListColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="updateReplenishmentListQuickFilterState"
      @quick-filter-query="applyReplenishmentListQuickFilter"
      @column-change="saveReplenishmentListColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          :loading="syncLoading"
          @click="handleKingdeeSync"
          v-hasPermi="['erp:kingdee-sync:query']"
        >
          <Icon icon="ep:refresh-right" class="mr-5px" />增量同步
        </el-button>
      </template>

      <template #table>
        <el-table
          class="replenishment-list-table replenishment-list-table--locked"
          v-loading="loading || replenishmentListColumnLoading"
          :data="list"
          border
          stripe
          show-overflow-tooltip
          row-key="id"
          height="calc(100vh - 304px)"
          data-user-table-column-explicit
          data-user-table-key="erp.production.replenishmentList.main"
          @header-dragend="handleReplenishmentListHeaderDragend"
        >
          <el-table-column type="expand" width="48" fixed="left">
            <template #default="{ row }">
              <div class="replenishment-list-items">
                <el-table
                  :data="row.items || []"
                  border
                  stripe
                  size="small"
                  empty-text="暂无补料明细"
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
          <el-table-column
            v-if="isReplenishmentListColumnVisible('sourceBillNo')"
            label="生产补料单号"
            prop="sourceBillNo"
            fixed="left"
            :width="getReplenishmentListColumnWidthString('sourceBillNo', 190)"
          />
          <el-table-column
            v-if="isReplenishmentListColumnVisible('documentStatus')"
            label="单据状态"
            prop="documentStatus"
            align="center"
            :width="getReplenishmentListColumnWidthString('documentStatus', 110)"
          />
          <el-table-column
            v-if="isReplenishmentListColumnVisible('billDate')"
            label="单据日期"
            prop="billDate"
            align="center"
            :width="getReplenishmentListColumnWidthString('billDate', 170)"
          >
            <template #default="{ row }">{{ formatDate(row.billDate) || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isReplenishmentListColumnVisible('productionOrderNos')"
            label="生产订单编号"
            prop="productionOrderNos"
            :width="getReplenishmentListColumnWidthString('productionOrderNos')"
            :min-width="getReplenishmentListColumnMinWidthString('productionOrderNos', 190)"
          />
          <el-table-column
            v-if="isReplenishmentListColumnVisible('materialNames')"
            label="物料"
            prop="materialNames"
            :width="getReplenishmentListColumnWidthString('materialNames')"
            :min-width="getReplenishmentListColumnMinWidthString('materialNames', 240)"
          />
          <el-table-column
            v-if="isReplenishmentListColumnVisible('stockOrgName')"
            label="库存组织"
            prop="stockOrgName"
            :width="getReplenishmentListColumnWidthString('stockOrgName', 170)"
          />
          <el-table-column
            v-if="isReplenishmentListColumnVisible('productionOrgName')"
            label="生产组织"
            prop="productionOrgName"
            :width="getReplenishmentListColumnWidthString('productionOrgName', 170)"
          />
          <el-table-column
            v-if="isReplenishmentListColumnVisible('departmentName')"
            label="补料部门"
            prop="departmentName"
            :width="getReplenishmentListColumnWidthString('departmentName', 150)"
          />
          <el-table-column
            v-if="isReplenishmentListColumnVisible('sourceModifyTime')"
            label="ERP 修改时间"
            prop="sourceModifyTime"
            align="center"
            :width="getReplenishmentListColumnWidthString('sourceModifyTime', 180)"
          >
            <template #default="{ row }">{{ formatDate(row.sourceModifyTime) || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isReplenishmentListColumnVisible('lastSyncTime')"
            label="最后同步时间"
            prop="lastSyncTime"
            align="center"
            :width="getReplenishmentListColumnWidthString('lastSyncTime', 180)"
          >
            <template #default="{ row }">{{ formatDate(row.lastSyncTime) || '-' }}</template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  ErpProductionReplenishmentListApi,
  type ErpProductionReplenishmentListVO
} from '@/api/erp/production/replenishment-list'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'ErpProductionReplenishmentList' })

const REPLENISHMENT_LIST_TABLE_KEY = 'erp.production.replenishmentList.main'

const replenishmentListDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sourceBillNo', label: '生产补料单号', width: 190, sortable: false },
  { key: 'documentStatus', label: '单据状态', width: 110, sortable: false },
  { key: 'billDate', label: '单据日期', width: 170, sortable: false },
  { key: 'productionOrderNos', label: '生产订单编号', minWidth: 190, sortable: false },
  { key: 'materialNames', label: '物料', minWidth: 240, sortable: false },
  { key: 'stockOrgName', label: '库存组织', width: 170, sortable: false },
  { key: 'productionOrgName', label: '生产组织', width: 170, sortable: false },
  { key: 'departmentName', label: '补料部门', width: 150, sortable: false },
  { key: 'sourceModifyTime', label: 'ERP 修改时间', width: 180, sortable: false },
  { key: 'lastSyncTime', label: '最后同步时间', width: 180, sortable: false }
]

const replenishmentListQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'sourceBillNo',
    label: '生产补料单号',
    type: 'text',
    queryParamKey: 'sourceBillNo',
    placeholder: '请输入生产补料单号'
  },
  {
    key: 'documentStatus',
    label: '单据状态',
    type: 'text',
    queryParamKey: 'documentStatus',
    placeholder: '请输入金蝶状态'
  },
  {
    key: 'productionOrderNo',
    label: '生产订单',
    type: 'text',
    queryParamKey: 'productionOrderNo',
    placeholder: '请输入生产订单编号'
  },
  {
    key: 'stockOrgName',
    label: '库存组织',
    type: 'text',
    queryParamKey: 'stockOrgName',
    placeholder: '请输入库存组织'
  },
  {
    key: 'productionOrgName',
    label: '生产组织',
    type: 'text',
    queryParamKey: 'productionOrgName',
    placeholder: '请输入生产组织'
  },
  {
    key: 'billDate',
    label: '单据日期',
    type: 'dateRange',
    queryParamKey: 'billDate'
  }
]

const message = useMessage()
const loading = ref(true)
const syncLoading = ref(false)
const list = ref<ErpProductionReplenishmentListVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceBillNo: undefined as string | undefined,
  documentStatus: undefined as string | undefined,
  productionOrderNo: undefined as string | undefined,
  stockOrgName: undefined as string | undefined,
  productionOrgName: undefined as string | undefined,
  billDate: undefined as string[] | undefined
})

const {
  loading: replenishmentListColumnLoading,
  saving: replenishmentListColumnSaving,
  columns: replenishmentListColumns,
  isColumnVisible: isReplenishmentListColumnVisible,
  getColumnWidthString: getReplenishmentListColumnWidthString,
  getColumnMinWidthString: getReplenishmentListColumnMinWidthString,
  handleHeaderDragend: handleReplenishmentListHeaderDragend,
  saveConfig: saveReplenishmentListColumnConfig
} = useUserTableColumns(REPLENISHMENT_LIST_TABLE_KEY, replenishmentListDefaultColumns)

async function getList() {
  loading.value = true
  try {
    const data = await ErpProductionReplenishmentListApi.getPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const {
  state: replenishmentListQuickFilterState,
  selectedDefinition: replenishmentListSelectedFilterDefinition,
  operatorOptions: replenishmentListFilterOperatorOptions,
  applyQuickFilter: applyReplenishmentListQuickFilter,
  updateState: updateReplenishmentListQuickFilterState
} = useTableQuickFilter(
  REPLENISHMENT_LIST_TABLE_KEY,
  replenishmentListQuickFilterDefinitions,
  queryParams,
  getList
)

const handleKingdeeSync = async () => {
  syncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSync('PRODUCTION_REPLENISHMENT_LIST')
    message.success('生产补料单列表增量同步任务已提交')
    await getList()
  } finally {
    syncLoading.value = false
  }
}

onMounted(getList)
</script>

<style scoped>
.replenishment-list-table {
  width: 100%;
}

.replenishment-list-table--locked :deep(.el-table__header-wrapper),
.replenishment-list-table--locked :deep(.el-table__fixed-header-wrapper) {
  z-index: 5;
}

.replenishment-list-table--locked :deep(.el-scrollbar__bar.is-horizontal) {
  z-index: 6;
}

.replenishment-list-items {
  padding: 8px 12px;
  background: #f7f9fc;
}
</style>
