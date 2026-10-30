<!-- ERP 生产用料清单列表 -->
<template>
  <ContentWrap class="erp-viewport-list">
    <UnifiedListTemplate
      ref="codeSyncListTemplate"
      table-key="erp.production.material-list.main"
      single-line-toolbar
      :query-model="queryParams"
      :filter-definitions="filterDefinitions"
      :quick-filter-state="quickFilter.state"
      :selected-filter-definition="quickFilter.selectedDefinition.value"
      :operator-options="quickFilter.operatorOptions.value"
      :columns="columns"
      :column-saving="columnSaving"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="quickFilter.updateState"
      @quick-filter-query="quickFilter.applyQuickFilter"
      @column-change="saveColumnConfig"
      @pagination="getList"
      :load-filter-rows="loadColumnFilterRows0"
      @filtered-rows="list = $event"
      :list-rows="list"
      @column-filter-reset="getList"
      :column-filter-definitions="listColumnFilterDefinitions0"
    >
      <template #actions>
        <SyncByCodeButton sync-type="PRODUCTION_MATERIAL_LIST" code-label="单据编号" :on-synced="handleCodeSynced" />
        <el-button v-if="queryParams.syncCode" plain @click="clearCodeSyncFilter">显示全部</el-button>
        <RecentFullSyncButton sync-type="PRODUCTION_MATERIAL_LIST" />
      </template>
      <template #table="{ resolveRows }">
        <el-table
          v-loading="loading || columnLoading"
          :data="resolveRows(list)"
          :stripe="true"
          :show-overflow-tooltip="true"
          class="erp-production-material-list__table"
          border
          row-key="id"
          data-user-table-column-explicit
          data-user-table-key="erp.production.material-list.main"
          @header-dragend="handleHeaderDragend"
         height="100%">
          <el-table-column
            v-if="isColumnVisible('sourceBillNo')"
            column-key="sourceBillNo"
            label="单据编号"
            align="center"
            prop="sourceBillNo"
            fixed="left"
            :width="getColumnWidthString('sourceBillNo', 220)"
          >
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
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isColumnVisible('lineCount')"
            column-key="lineCount"
            label="子项数量"
            align="center"
            prop="lineCount"
            :width="getColumnWidthString('lineCount', 120)"
          />
          <el-table-column
            v-if="isColumnVisible('productionOrderSummary')"
            column-key="productionOrderSummary"
            label="生产工单"
            align="center"
            prop="productionOrderSummary"
            :min-width="getColumnWidthString('productionOrderSummary', 220)"
          >
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
            v-if="isColumnVisible('sourceModifyTime')"
            column-key="sourceModifyTime"
            label="ERP修改时间"
            align="center"
            prop="sourceModifyTime"
            :formatter="dateFormatter"
            :width="getColumnWidthString('sourceModifyTime', 180)"
          />
          <el-table-column
            v-if="isColumnVisible('lastSyncTime')"
            column-key="lastSyncTime"
            label="最后同步时间"
            align="center"
            prop="lastSyncTime"
            :formatter="dateFormatter"
            :width="getColumnWidthString('lastSyncTime', 180)"
          />
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <el-dialog
    v-model="detailDialogVisible"
    :title="detailDialogTitle"
    width="1080px"
    destroy-on-close
    class="erp-production-material-list__dialog erp-list-detail-dialog"
  >
    <ErpLocalList
      table-key="erp.production.material-list.details"
      :rows="detailList"
      :default-columns="detailColumns"
      :filter-definitions="detailFilters"
    >
      <template
        #default="{
          rows,
          isColumnVisible: isDetailColumnVisible,
          getColumnWidthString: getDetailColumnWidthString,
          handleHeaderDragend: handleDetailHeaderDragend
        }"
      >
        <el-table
          v-loading="detailLoading"
          :data="rows"
          :stripe="true"
          :show-overflow-tooltip="true"
          height="100%"
          class="erp-production-material-list__detail-table"
          data-user-table-column-explicit
          data-user-table-key="erp.production.material-list.details"
          @header-dragend="handleDetailHeaderDragend"
        >
          <el-table-column
            v-if="isDetailColumnVisible('childMaterialCode')"
            column-key="childMaterialCode"
            label="子项物料编码"
            align="center"
            prop="childMaterialCode"
            fixed="left"
            :width="getDetailColumnWidthString('childMaterialCode', 180)"
          />
          <el-table-column
            v-if="isDetailColumnVisible('childMaterialName')"
            column-key="childMaterialName"
            label="子项物料名称"
            align="center"
            prop="childMaterialName"
            :width="getDetailColumnWidthString('childMaterialName', 200)"
          />
          <el-table-column
            v-if="isDetailColumnVisible('childMaterialSpecification')"
            column-key="childMaterialSpecification"
            label="规格型号"
            align="center"
            prop="childMaterialSpecification"
            :width="getDetailColumnWidthString('childMaterialSpecification', 180)"
          />
          <el-table-column
            v-if="isDetailColumnVisible('childMaterialType')"
            column-key="childMaterialType"
            label="子项类型"
            align="center"
            prop="childMaterialType"
            :width="getDetailColumnWidthString('childMaterialType', 120)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isDetailColumnVisible('numerator')"
            column-key="numerator"
            label="分子"
            align="center"
            prop="numerator"
            :width="getDetailColumnWidthString('numerator', 110)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isDetailColumnVisible('denominator')"
            column-key="denominator"
            label="分母"
            align="center"
            prop="denominator"
            :width="getDetailColumnWidthString('denominator', 110)"
          />
          <el-table-column
            v-if="isDetailColumnVisible('childUnitName')"
            column-key="childUnitName"
            label="子项单位"
            align="center"
            prop="childUnitName"
            :width="getDetailColumnWidthString('childUnitName', 120)"
          />
          <el-table-column
            v-if="isDetailColumnVisible('drawingNumber')"
            column-key="drawingNumber"
            label="图号"
            align="center"
            prop="drawingNumber"
            :width="getDetailColumnWidthString('drawingNumber', 140)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isDetailColumnVisible('requiredQuantity')"
            column-key="requiredQuantity"
            label="应发数量"
            align="center"
            prop="requiredQuantity"
            :width="getDetailColumnWidthString('requiredQuantity', 120)"
          />
          <el-table-column
            v-if="isDetailColumnVisible('demandTime')"
            column-key="demandTime"
            label="需求日期"
            align="center"
            prop="demandTime"
            :formatter="dateFormatter"
            :width="getDetailColumnWidthString('demandTime', 180)"
          />
          <el-table-column
            v-if="isDetailColumnVisible('issueMethod')"
            column-key="issueMethod"
            label="发料方式"
            align="center"
            prop="issueMethod"
            :width="getDetailColumnWidthString('issueMethod', 120)"
          >
            <template #default="{ row }">
              {{ String(row.issueMethod) === '1' ? '直接领料' : row.issueMethod || '' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDetailColumnVisible('productionOrderLink')"
            column-key="productionOrderLink"
            label="对应生产订单"
            align="center"
            :width="getDetailColumnWidthString('productionOrderLink', 180)"
          >
            <template #default="{ row }">
              <el-link v-if="row.workOrderId" type="primary" @click="handleOpenWorkOrder(row)">
                {{ row.workOrderCode || row.productionOrderNo }}
              </el-link>
              <span v-else>{{ row.productionOrderNo || '无' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </ErpLocalList>
  </el-dialog>
</template>
<script setup lang="ts">
import SyncByCodeButton from '@/views/erp/components/SyncByCodeButton.vue'
import { useErpCodeSyncFilter } from '@/views/erp/components/useErpCodeSyncFilter'

import { loadAllListPages, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import RecentFullSyncButton from '@/views/erp/components/RecentFullSyncButton.vue'
import '@/views/erp/components/viewport-list.css'

import { erpListIntegerColumnFormatter } from '@/views/erp/utils/integerDisplay'
import ErpLocalList from '@/views/erp/components/ErpLocalList.vue'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import { dateFormatter } from '@/utils/formatTime'
import {
  ErpProductionMaterialListApi,
  ErpProductionMaterialListDetailVO,
  ErpProductionMaterialListGroupVO
} from '@/api/erp/production/material-list'

/** ERP 生产用料清单列表 */

const detailColumns: UserTableColumnDefinition[] = [
  {
    key: 'childMaterialCode',
    label: '子项物料编码',
    width: 180,
    sortable: false
  },
  {
    key: 'childMaterialName',
    label: '子项物料名称',
    width: 200,
    sortable: false
  },
  {
    key: 'childMaterialSpecification',
    label: '规格型号',
    width: 180,
    sortable: false
  },
  {
    key: 'childMaterialType',
    label: '子项类型',
    width: 120,
    sortable: false
  },
  {
    key: 'numerator',
    label: '分子',
    width: 110,
    sortable: false
  },
  {
    key: 'denominator',
    label: '分母',
    width: 110,
    sortable: false
  },
  {
    key: 'childUnitName',
    label: '子项单位',
    width: 120,
    sortable: false
  },
  {
    key: 'drawingNumber',
    label: '图号',
    width: 140,
    sortable: false
  },
  {
    key: 'requiredQuantity',
    label: '应发数量',
    width: 120,
    sortable: false
  },
  {
    key: 'demandTime',
    label: '需求日期',
    width: 180,
    sortable: false
  },
  {
    key: 'issueMethod',
    label: '发料方式',
    width: 120,
    sortable: false
  },
  {
    key: 'productionOrderLink',
    label: '对应生产订单',
    width: 180,
    sortable: false
  }
]
const detailFilters: TableQuickFilterDefinition[] = [
  {
    key: 'childMaterialCode',
    label: '子项物料编码',
    type: 'text',
    queryParamKey: 'childMaterialCode',
    operators: ['contains']
  },
  {
    key: 'childMaterialName',
    label: '子项物料名称',
    type: 'text',
    queryParamKey: 'childMaterialName',
    operators: ['contains']
  },
  {
    key: 'childMaterialSpecification',
    label: '规格型号',
    type: 'text',
    queryParamKey: 'childMaterialSpecification',
    operators: ['contains']
  }
]
defineOptions({ name: 'ErpProductionMaterialList' })

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(true)
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
  syncCode: undefined as string | undefined,
  pageNo: 1,
  pageSize: 10,
  sourceBillNo: undefined as string | undefined,
  productCode: undefined as string | undefined,
  productionOrderNo: undefined as string | undefined,
  childMaterialCode: undefined as string | undefined,
  childMaterialName: undefined as string | undefined
})

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

/** 重置按钮操作 */


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

const filterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'sourceBillNo',
    label: '单据编号',
    type: 'text',
    queryParamKey: 'sourceBillNo',
    operators: ['contains']
  },
  {
    key: 'productCode',
    label: '产品编码',
    type: 'text',
    queryParamKey: 'productCode',
    operators: ['contains']
  },
  {
    key: 'productionOrderNo',
    label: '生产订单',
    type: 'text',
    queryParamKey: 'productionOrderNo',
    operators: ['contains']
  },
  {
    key: 'childMaterialCode',
    label: '子项编码',
    type: 'text',
    queryParamKey: 'childMaterialCode',
    operators: ['contains']
  },
  {
    key: 'childMaterialName',
    label: '子项名称',
    type: 'text',
    queryParamKey: 'childMaterialName',
    operators: ['contains']
  }
]
const defaultColumns: UserTableColumnDefinition[] = [
  {
    key: 'sourceBillNo',
    label: '单据编号',
    sortable: false,
    width: 220
  },
  {
    key: 'lineCount',
    label: '子项数量',
    sortable: false,
    width: 120
  },
  {
    key: 'productionOrderSummary',
    label: '生产工单',
    sortable: false,
    minWidth: 220
  },
  {
    key: 'sourceModifyTime',
    label: 'ERP修改时间',
    sortable: false,
    width: 180
  },
  {
    key: 'lastSyncTime',
    label: '最后同步时间',
    sortable: false,
    width: 180
  }
]
const {
  columns,
  loading: columnLoading,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns('erp.production.material-list.main', defaultColumns)
const quickFilter = useTableQuickFilter(
  'erp.production.material-list.main',
  filterDefinitions,
  queryParams,
  getList
)

onMounted(() => {
  applyRouteQuery()
  getList()
})

const loadColumnFilterRows0 = async () => {
  const params = { ...queryParams }
  const rows = await loadAllListPages<any>((pagination) =>
    ErpProductionMaterialListApi.getGroupPage({ ...params, ...pagination })
  )
  return rows
}

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'productionOrderSummary',
    label: '生产工单',
    type: 'text',
    getValue: (row) =>
      [
        row.productionOrderSummary,
        row.productionOrderSummary || '无',
        row.productionOrderCount
      ].join(' ')
  }
])

const codeSyncListTemplate = ref<InstanceType<typeof UnifiedListTemplate>>()
const { handleCodeSynced, clearCodeSyncFilter } = useErpCodeSyncFilter(
  queryParams, quickFilter.state, getList, () => codeSyncListTemplate.value!.resetColumnFilters()
)
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
