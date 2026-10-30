<template>
  <ContentWrap class="production-sync-list erp-viewport-list">
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
      :load-filter-rows="loadColumnFilterRows0"
      @filtered-rows="list = $event"
      :list-rows="list"
      @column-filter-reset="getList"
    >
      <template #actions>
        <RecentFullSyncButton sync-type="PRODUCTION_REPLENISHMENT_LIST" />
      </template>

      <template #table="{ resolveRows }">
        <el-table
          class="replenishment-list-table replenishment-list-table--locked"
          v-loading="loading || replenishmentListColumnLoading"
          :data="resolveRows(list)"
          border
          stripe
          show-overflow-tooltip
          row-key="id"
          data-user-table-column-explicit
          data-user-table-key="erp.production.replenishmentList.main"
          @header-dragend="handleReplenishmentListHeaderDragend"
         height="100%">
          <el-table-column type="expand" width="48" fixed="left">
            <template #default="{ row }">
              <div class="replenishment-list-items">
                <ErpLocalList
                  table-key="erp.production.replenishmentList.details"
                  :rows="row.items || []"
                  :default-columns="detailColumns"
                  :filter-definitions="detailFilters"
                >
                  <template
                    #default="{ rows, isColumnVisible, getColumnWidthString, handleHeaderDragend }"
                  >
                    <el-table
                      :data="rows"
                      border
                      stripe
                      size="small"
                      empty-text="暂无补料明细"
                      data-user-table-column-explicit
                      data-user-table-key="erp.production.replenishmentList.details"
                      @header-dragend="handleHeaderDragend"
                    >
                      <el-table-column
                        v-if="isColumnVisible('materialNumber')"
                        column-key="materialNumber"
                        label="物料编码"
                        prop="materialNumber"
                        :width="getColumnWidthString('materialNumber', 150)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('materialName')"
                        column-key="materialName"
                        label="物料名称"
                        prop="materialName"
                        :width="getColumnWidthString('materialName', 180)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('materialSpecification')"
                        column-key="materialSpecification"
                        label="规格型号"
                        prop="materialSpecification"
                        :width="getColumnWidthString('materialSpecification', 180)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('unitName')"
                        column-key="unitName"
                        label="单位"
                        prop="unitName"
                        align="center"
                        :width="getColumnWidthString('unitName', 90)"
                      />
                      <el-table-column
                        :formatter="erpListIntegerColumnFormatter"
                        v-if="isColumnVisible('requestedQuantity')"
                        column-key="requestedQuantity"
                        label="申请数量"
                        prop="requestedQuantity"
                        align="right"
                        :width="getColumnWidthString('requestedQuantity', 120)"
                      />
                      <el-table-column
                        :formatter="erpListIntegerColumnFormatter"
                        v-if="isColumnVisible('actualQuantity')"
                        column-key="actualQuantity"
                        label="实发数量"
                        prop="actualQuantity"
                        align="right"
                        :width="getColumnWidthString('actualQuantity', 120)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('warehouseName')"
                        column-key="warehouseName"
                        label="仓库"
                        prop="warehouseName"
                        :width="getColumnWidthString('warehouseName', 160)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('stockLocationName')"
                        column-key="stockLocationName"
                        label="仓位"
                        prop="stockLocationName"
                        :width="getColumnWidthString('stockLocationName', 150)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('stockStatusName')"
                        column-key="stockStatusName"
                        label="库存状态"
                        prop="stockStatusName"
                        :width="getColumnWidthString('stockStatusName', 130)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('lotNumber')"
                        column-key="lotNumber"
                        label="批号"
                        prop="lotNumber"
                        :width="getColumnWidthString('lotNumber', 130)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('productionOrderNo')"
                        column-key="productionOrderNo"
                        label="生产订单编号"
                        prop="productionOrderNo"
                        :width="getColumnWidthString('productionOrderNo', 170)"
                      />
                      <el-table-column
                        v-if="isColumnVisible('workshopName')"
                        column-key="workshopName"
                        label="车间"
                        prop="workshopName"
                        :width="getColumnWidthString('workshopName', 150)"
                      />
                    </el-table>
                  </template>
                </ErpLocalList>
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
import { loadAllListPages } from '@/hooks/web/useListColumnFilter'

import RecentFullSyncButton from '@/views/erp/components/RecentFullSyncButton.vue'
import '@/views/erp/components/viewport-list.css'

import { erpListIntegerColumnFormatter } from '@/views/erp/utils/integerDisplay'
import ErpLocalList from '@/views/erp/components/ErpLocalList.vue'

import {
  ErpProductionReplenishmentListApi,
  type ErpProductionReplenishmentListVO
} from '@/api/erp/production/replenishment-list'
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
import { useRoute } from 'vue-router'

const detailColumns: UserTableColumnDefinition[] = [
  {
    key: 'materialNumber',
    label: '物料编码',
    width: 150,
    sortable: false
  },
  {
    key: 'materialName',
    label: '物料名称',
    width: 180,
    sortable: false
  },
  {
    key: 'materialSpecification',
    label: '规格型号',
    width: 180,
    sortable: false
  },
  {
    key: 'unitName',
    label: '单位',
    width: 90,
    sortable: false
  },
  {
    key: 'requestedQuantity',
    label: '申请数量',
    width: 120,
    sortable: false
  },
  {
    key: 'actualQuantity',
    label: '实发数量',
    width: 120,
    sortable: false
  },
  {
    key: 'warehouseName',
    label: '仓库',
    width: 160,
    sortable: false
  },
  {
    key: 'stockLocationName',
    label: '仓位',
    width: 150,
    sortable: false
  },
  {
    key: 'stockStatusName',
    label: '库存状态',
    width: 130,
    sortable: false
  },
  {
    key: 'lotNumber',
    label: '批号',
    width: 130,
    sortable: false
  },
  {
    key: 'productionOrderNo',
    label: '生产订单编号',
    width: 170,
    sortable: false
  },
  {
    key: 'workshopName',
    label: '车间',
    width: 150,
    sortable: false
  }
]
const detailFilters: TableQuickFilterDefinition[] = [
  {
    key: 'materialNumber',
    label: '物料编码',
    type: 'text',
    queryParamKey: 'materialNumber',
    operators: ['contains']
  },
  {
    key: 'materialName',
    label: '物料名称',
    type: 'text',
    queryParamKey: 'materialName',
    operators: ['contains']
  },
  {
    key: 'materialSpecification',
    label: '规格型号',
    type: 'text',
    queryParamKey: 'materialSpecification',
    operators: ['contains']
  }
]
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

const route = useRoute()
const loading = ref(true)
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

const applyRouteQuery = () => {
  if (route.query.sourceBillNo) {
    queryParams.sourceBillNo = String(route.query.sourceBillNo)
  }
}


onMounted(() => {
  applyRouteQuery()
  getList()
})

const loadColumnFilterRows0 = async () => {
  const params = { ...queryParams }
  const rows = await loadAllListPages<any>((pagination) =>
    ErpProductionReplenishmentListApi.getPage({ ...params, ...pagination })
  )
  return rows
}
</script>

<style scoped>
.production-sync-list {
  height: calc(100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding));
  min-height: 320px;
  margin-bottom: 0;
}

.production-sync-list :deep(> .el-card__body),
.production-sync-list :deep(.el-card__body > .unified-list-template) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.production-sync-list :deep(.el-card__body > .unified-list-template > .unified-list-template__table-shell) {
  flex: 1;
  min-height: 0;
}

.production-sync-list :deep(.el-card__body > .unified-list-template > .el-form),
.production-sync-list :deep(.el-card__body > .unified-list-template > .el-pagination) {
  flex-shrink: 0;
}

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
