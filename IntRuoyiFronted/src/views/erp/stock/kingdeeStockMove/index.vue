<template>
  <doc-alert title="【ERP】金蝶调拨单只读展示" url="https://doc.iocoder.cn/erp/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="erp.stock.kingdeeStockMove.main"
      single-line-toolbar
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="stockMoveQuickFilterDefinitions"
      :quick-filter-state="stockMoveQuickFilterState"
      :selected-filter-definition="stockMoveSelectedFilterDefinition"
      :operator-options="stockMoveFilterOperatorOptions"
      :columns="stockMoveColumns"
      :column-saving="stockMoveColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="updateStockMoveQuickFilterState"
      @quick-filter-query="applyStockMoveQuickFilter"
      @column-change="saveStockMoveColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          :loading="syncLoading"
          @click="handleKingdeeSync"
          v-hasPermi="['erp:kingdee-sync:query']"
        >
          <Icon icon="ep:refresh-right" class="mr-5px" /> 增量同步
        </el-button>
      </template>

      <template #table>
        <el-table
          class="stock-move-table stock-move-table--locked"
          v-loading="loading || stockMoveColumnLoading"
          :data="list"
          height="calc(100vh - 304px)"
          border
          stripe
          show-overflow-tooltip
          row-key="id"
          data-user-table-column-explicit
          data-user-table-key="erp.stock.kingdeeStockMove.main"
          @header-dragend="handleStockMoveHeaderDragend"
        >
          <el-table-column type="expand" width="48" fixed="left">
            <template #default="{ row }">
              <div class="stock-move-items">
                <el-table
                  :data="row.items || []"
                  border
                  stripe
                  size="small"
                  empty-text="暂无调拨明细"
                >
                  <el-table-column label="物料编码" prop="materialNumber" min-width="150" />
                  <el-table-column label="物料名称" prop="materialName" min-width="180" />
                  <el-table-column label="单位" prop="unitName" width="90" align="center" />
                  <el-table-column label="调拨数量" prop="quantity" width="120" align="right" />
                  <el-table-column label="调出仓库" prop="fromWarehouseName" min-width="160" />
                  <el-table-column label="调入仓库" prop="toWarehouseName" min-width="160" />
                  <el-table-column label="调出仓位" prop="fromStockLocation" min-width="150" />
                  <el-table-column label="调入仓位" prop="toStockLocation" min-width="150" />
                  <el-table-column label="批号" prop="lotNumber" min-width="130" />
                </el-table>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isStockMoveColumnVisible('sourceBillNo')"
            label="调拨单号"
            prop="sourceBillNo"
            align="center"
            :width="getStockMoveColumnWidthString('sourceBillNo', 190)"
          />
          <el-table-column
            v-if="isStockMoveColumnVisible('documentStatus')"
            label="单据状态"
            prop="documentStatus"
            align="center"
            :width="getStockMoveColumnWidthString('documentStatus', 120)"
          />
          <el-table-column
            v-if="isStockMoveColumnVisible('billDate')"
            label="单据日期"
            prop="billDate"
            align="center"
            :width="getStockMoveColumnWidthString('billDate', 170)"
          >
            <template #default="{ row }">{{ formatDate(row.billDate) || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isStockMoveColumnVisible('transferDirect')"
            label="调拨方向"
            prop="transferDirect"
            align="center"
            :width="getStockMoveColumnWidthString('transferDirect', 130)"
          />
          <el-table-column
            v-if="isStockMoveColumnVisible('materialNames')"
            label="物料"
            prop="materialNames"
            :width="getStockMoveColumnWidthString('materialNames')"
            :min-width="getStockMoveColumnMinWidthString('materialNames', 240)"
          />
          <el-table-column
            v-if="isStockMoveColumnVisible('fromWarehouseName')"
            label="调出仓库"
            prop="fromWarehouseName"
            :width="getStockMoveColumnWidthString('fromWarehouseName', 180)"
          >
            <template #default="{ row }">{{ firstItem(row)?.fromWarehouseName || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isStockMoveColumnVisible('toWarehouseName')"
            label="调入仓库"
            prop="toWarehouseName"
            :width="getStockMoveColumnWidthString('toWarehouseName', 180)"
          >
            <template #default="{ row }">{{ firstItem(row)?.toWarehouseName || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isStockMoveColumnVisible('sourceModifyTime')"
            label="ERP 修改时间"
            prop="sourceModifyTime"
            align="center"
            :width="getStockMoveColumnWidthString('sourceModifyTime', 180)"
          >
            <template #default="{ row }">{{ formatDate(row.sourceModifyTime) || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isStockMoveColumnVisible('lastSyncTime')"
            label="最后同步时间"
            prop="lastSyncTime"
            align="center"
            :width="getStockMoveColumnWidthString('lastSyncTime', 180)"
          >
            <template #default="{ row }">{{ formatDate(row.lastSyncTime) || '-' }}</template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ErpKingdeeStockMoveApi, type ErpKingdeeStockMoveVO } from '@/api/erp/stock/kingdeeStockMove'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'ErpKingdeeStockMove' })

const STOCK_MOVE_TABLE_KEY = 'erp.stock.kingdeeStockMove.main'

const stockMoveDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sourceBillNo', label: '调拨单号', width: 190, sortable: false },
  { key: 'documentStatus', label: '单据状态', width: 120, sortable: false },
  { key: 'billDate', label: '单据日期', width: 170, sortable: false },
  { key: 'transferDirect', label: '调拨方向', width: 130, sortable: false },
  { key: 'materialNames', label: '物料', minWidth: 240, sortable: false },
  { key: 'fromWarehouseName', label: '调出仓库', width: 180, sortable: false },
  { key: 'toWarehouseName', label: '调入仓库', width: 180, sortable: false },
  { key: 'sourceModifyTime', label: 'ERP 修改时间', width: 180, sortable: false },
  { key: 'lastSyncTime', label: '最后同步时间', width: 180, sortable: false }
]

const stockMoveQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'sourceBillNo',
    label: '调拨单号',
    type: 'text',
    queryParamKey: 'sourceBillNo',
    placeholder: '请输入调拨单号'
  },
  {
    key: 'documentStatus',
    label: '单据状态',
    type: 'text',
    queryParamKey: 'documentStatus',
    placeholder: '请输入金蝶状态'
  },
  {
    key: 'transferDirect',
    label: '调拨方向',
    type: 'text',
    queryParamKey: 'transferDirect',
    placeholder: '请输入调拨方向'
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
const list = ref<ErpKingdeeStockMoveVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceBillNo: undefined as string | undefined,
  documentStatus: undefined as string | undefined,
  transferDirect: undefined as string | undefined,
  billDate: undefined as string[] | undefined
})

const {
  loading: stockMoveColumnLoading,
  saving: stockMoveColumnSaving,
  columns: stockMoveColumns,
  isColumnVisible: isStockMoveColumnVisible,
  getColumnWidthString: getStockMoveColumnWidthString,
  getColumnMinWidthString: getStockMoveColumnMinWidthString,
  handleHeaderDragend: handleStockMoveHeaderDragend,
  saveConfig: saveStockMoveColumnConfig
} = useUserTableColumns(STOCK_MOVE_TABLE_KEY, stockMoveDefaultColumns)

const {
  state: stockMoveQuickFilterState,
  selectedDefinition: stockMoveSelectedFilterDefinition,
  operatorOptions: stockMoveFilterOperatorOptions,
  applyQuickFilter: applyStockMoveQuickFilter,
  updateState: updateStockMoveQuickFilterState
} = useTableQuickFilter(
  STOCK_MOVE_TABLE_KEY,
  stockMoveQuickFilterDefinitions,
  queryParams,
  getList
)

const firstItem = (row: ErpKingdeeStockMoveVO) => row.items?.[0]

const getBillDateRequestRange = () => {
  if (!queryParams.billDate || queryParams.billDate.length !== 2) return undefined
  return [`${queryParams.billDate[0]} 00:00:00`, `${queryParams.billDate[1]} 23:59:59`]
}

async function getList() {
  loading.value = true
  try {
    const requestParams = {
      ...queryParams,
      billDate: getBillDateRequestRange()
    }
    const data = await ErpKingdeeStockMoveApi.getStockMovePage(requestParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleKingdeeSync = async () => {
  syncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSync('STOCK_MOVE')
    message.success('金蝶调拨单增量同步任务已提交')
    await getList()
  } finally {
    syncLoading.value = false
  }
}

onMounted(getList)
</script>

<style scoped>
.stock-move-table {
  width: 100%;
}

.stock-move-table--locked :deep(.el-table__header-wrapper),
.stock-move-table--locked :deep(.el-table__fixed-header-wrapper) {
  z-index: 5;
}

.stock-move-table--locked :deep(.el-scrollbar__bar.is-horizontal) {
  z-index: 6;
}

.stock-move-items {
  padding: 8px 12px;
  background: #f7f9fc;
}
</style>
