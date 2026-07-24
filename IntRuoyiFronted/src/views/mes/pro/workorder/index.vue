<!-- MES 生产工单列表 -->
<template>
  <doc-alert title="MES 生产工单" url="https://doc.iocoder.cn/mes/pro/work-order/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.pro.workorder.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="workOrderQuickFilterDefinitions"
      :quick-filter-state="workOrderQuickFilter.state"
      :selected-filter-definition="workOrderQuickFilter.selectedDefinition.value"
      :operator-options="workOrderQuickFilter.operatorOptions.value"
      :columns="workOrderColumns"
      :column-saving="workOrderColumnSaving"
      :show-column-settings="false"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="workOrderQuickFilter.updateState"
      @quick-filter-query="workOrderQuickFilter.applyQuickFilter"
      @column-change="saveWorkOrderColumnConfig"
      @pagination="getList"
    >
      <template #extra-filters>
        <el-form-item class="work-order-column-settings">
          <UserTableColumnSettings
            button-label="列筛选"
            :columns="workOrderColumns"
            :saving="workOrderColumnSaving"
            :show-reset="false"
            @change="saveWorkOrderColumnConfig"
          />
        </el-form-item>
      </template>
      <template #actions>
        <el-button
          v-if="isAdminUser"
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:pro-work-order:export']"
          ><Icon icon="ep:download" class="mr-5px" /> 导出</el-button
        >
        <el-button
          v-if="isAdminUser"
          type="warning"
          plain
          @click="handleSyncKingdeeWorkOrders"
          :loading="kingdeeSyncLoading"
          v-hasPermi="['mes:pro-work-order:create']"
          ><Icon icon="ep:refresh" class="mr-5px" /> 增量同步</el-button
        >
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.workorder.main"
          :data="list"
          border
          :max-height="workOrderTableMaxHeight"
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          :default-expand-all="isExpandAll"
          v-if="refreshTable"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          @header-dragend="handleWorkOrderHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
      <el-table-column
        v-if="isWorkOrderColumnVisible('code')"
        label="工单编号"
        prop="code"
        width="340"
        fixed="left"
          v-bind="sortColumnAttrs('code')"
        ><template #default="scope">
          <div class="work-order-key-cell">
            <el-button
              link
              type="primary"
              class="work-order-key-text"
              @click="openForm('detail', scope.row.id)"
              >{{ scope.row.code }}</el-button
            >
          </div></template
        ></el-table-column
      >
      <el-table-column
        v-if="isWorkOrderColumnVisible('productCode')"
        label="产品编码"
        align="center"
        prop="productCode"
        width="260"
          v-bind="sortColumnAttrs('productCode')"
        ><template #default="scope">
          <div class="work-order-key-cell">
            <span class="work-order-key-text">{{ scope.row.productCode }}</span>
          </div></template
        ></el-table-column
      >
      <el-table-column
        v-if="isWorkOrderColumnVisible('productName')"
        label="产品名称"
        align="center"
        prop="productName"
        min-width="340"
          v-bind="sortColumnAttrs('productName')"
        ><template #default="scope">
          <div class="work-order-key-cell">
            <span class="work-order-key-text">{{ scope.row.productName }}</span>
          </div></template
        ></el-table-column
      >
      <el-table-column
        v-if="isWorkOrderColumnVisible('productSpecification')"
        label="规格型号"
        align="center"
        prop="productSpecification"
        min-width="360"
          v-bind="sortColumnAttrs('productSpecification')"
        ><template #default="scope">
          <div class="work-order-key-cell">
            <span class="work-order-key-text">{{ scope.row.productSpecification }}</span>
          </div></template
        ></el-table-column
      >
      <el-table-column
        v-if="isWorkOrderColumnVisible('quantity')"
        label="计划数量"
        align="center"
        prop="quantity"
        width="180"
          v-bind="sortColumnAttrs('quantity')"
        ><template #default="scope">
          <div class="work-order-key-cell work-order-key-cell--number">
            <span class="work-order-key-text">{{ scope.row.quantity }}</span>
          </div></template
        ></el-table-column
      >
      <el-table-column v-if="isWorkOrderColumnVisible('batchCode')" label="批次号" align="center" prop="batchCode" width="160" v-bind="sortColumnAttrs('batchCode')" />
      <el-table-column v-if="isWorkOrderColumnVisible('workshopName')" label="生产车间" align="center" prop="workshopName" :width="getWorkOrderColumnWidthString('workshopName', 120)" v-bind="sortColumnAttrs('workshopName')" />
      <el-table-column
        v-if="isWorkOrderColumnVisible('plannedStartTime')"
        label="计划开工时间"
        align="center"
        prop="plannedStartTime"
        :formatter="dateFormatter2"
        :width="getWorkOrderColumnWidthString('plannedStartTime', 180)"
        v-bind="sortColumnAttrs('plannedStartTime')"
      />
      <el-table-column
        v-if="isWorkOrderColumnVisible('plannedEndTime')"
        label="计划完工时间"
        align="center"
        prop="plannedEndTime"
        :formatter="dateFormatter"
        :width="getWorkOrderColumnWidthString('plannedEndTime', 180)"
        v-bind="sortColumnAttrs('plannedEndTime')"
      />
      <el-table-column v-if="isWorkOrderColumnVisible('businessStatus')" label="业务状态" align="center" prop="businessStatus" :width="getWorkOrderColumnWidthString('businessStatus', 100)" v-bind="sortColumnAttrs('businessStatus')" />
      <el-table-column v-if="isWorkOrderColumnVisible('drawingNumber')" label="图号" align="center" prop="drawingNumber" :width="getWorkOrderColumnWidthString('drawingNumber', 140)" v-bind="sortColumnAttrs('drawingNumber')" />
      <el-table-column v-if="isWorkOrderColumnVisible('auxiliaryCode')" label="备注1助记码" align="center" prop="auxiliaryCode" :width="getWorkOrderColumnWidthString('auxiliaryCode', 140)" v-bind="sortColumnAttrs('auxiliaryCode')" />
      <el-table-column v-if="isWorkOrderColumnVisible('scheduleStatus')" label="排产状态" align="center" prop="scheduleStatus" :width="getWorkOrderColumnWidthString('scheduleStatus', 110)" v-bind="sortColumnAttrs('scheduleStatus')" />
      <el-table-column v-if="isWorkOrderColumnVisible('quantityProduced')" label="已生产数量" align="center" prop="quantityProduced" :width="getWorkOrderColumnWidthString('quantityProduced', 110)" v-bind="sortColumnAttrs('quantityProduced')" />
      <el-table-column v-if="isWorkOrderColumnVisible('status')" label="工单状态" align="center" prop="status" :width="getWorkOrderColumnWidthString('status', 100)"
          v-bind="sortColumnAttrs('status')"
        ><template #default="scope"
          ><dict-tag :type="DICT_TYPE.MES_PRO_WORK_ORDER_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column v-if="isWorkOrderColumnVisible('clientName')" label="客户名称" align="center" prop="clientName" :width="getWorkOrderColumnWidthString('clientName', 120)" v-bind="sortColumnAttrs('clientName')" />
      <el-table-column v-if="isWorkOrderColumnVisible('productionMaterialList')" label="生产用料清单" align="center" prop="productionMaterialList" :min-width="getWorkOrderColumnMinWidthString('productionMaterialList', 220)" v-bind="sortColumnAttrs('productionMaterialList')">
        <template #default="scope">
          <el-link
            v-if="scope.row.productionMaterialListCount > 0"
            type="primary"
            @click="handleOpenProductionMaterialList(scope.row)"
          >
            {{ scope.row.productionMaterialListSummary || `共 ${scope.row.productionMaterialListCount} 张` }}
          </el-link>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="isWorkOrderColumnVisible('createTime')"
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        :width="getWorkOrderColumnWidthString('createTime', 180)"
        v-bind="sortColumnAttrs('createTime')"
      />
      <el-table-column
        v-if="isAdminUser && isWorkOrderColumnVisible('operation')"
        label="操作"
        prop="operation"
        align="center"
        width="220"
        fixed="right"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            :loading="erpCreateLoadingId === scope.row.id"
            @click="handleCreateKingdeeProductionOrder(scope.row)"
            v-hasPermi="['mes:pro-work-order:create-erp']"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 测试添加
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleOpenBatchRecord(scope.row)"
            v-hasPermi="['mes:pro-edhr-batch-execution:create']"
          >
            批记录
          </el-button>
        </template>
      </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <WorkOrderForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { dateFormatter, dateFormatter2 } from '@/utils/formatTime'
import { DICT_TYPE } from '@/utils/dict'
import download from '@/utils/download'
import { handleTree } from '@/utils/tree'
import {
  ProWorkOrderApi,
  type ProWorkOrderVO
} from '@/api/mes/pro/workorder'
import { MdItemApi, type MdItemVO } from '@/api/mes/md/item'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import WorkOrderForm from './WorkOrderForm.vue'
import { useTreeTableExpand } from '@/utils/treeExpand'
import { checkRole } from '@/utils/permission'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterSuggestion
} from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesProWorkOrder' })
const message = useMessage()
const route = useRoute()
const router = useRouter()
const workOrderTableMaxHeight = 'calc(100vh - 240px)'
const workOrderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '工单编号', width: 340 },
  { key: 'productCode', label: '产品编码', width: 260 },
  { key: 'productName', label: '产品名称', minWidth: 340 },
  { key: 'productSpecification', label: '规格型号', minWidth: 360 },
  { key: 'quantity', label: '计划数量', width: 180 },
  { key: 'batchCode', label: '批次号', width: 160 },
  { key: 'workshopName', label: '生产车间', width: 120 },
  { key: 'plannedStartTime', label: '计划开工时间', width: 180 },
  { key: 'plannedEndTime', label: '计划完工时间', width: 180 },
  { key: 'businessStatus', label: '业务状态', width: 100 },
  { key: 'drawingNumber', label: '图号', width: 140 },
  { key: 'auxiliaryCode', label: '备注1助记码', width: 140 },
  { key: 'scheduleStatus', label: '排产状态', width: 110 },
  { key: 'quantityProduced', label: '已生产数量', width: 110 },
  { key: 'status', label: '工单状态', width: 100 },
  { key: 'clientName', label: '客户名称', width: 120 },
  { key: 'productionMaterialList', label: '生产用料清单', minWidth: 220 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'operation', label: '操作', width: 220, hideable: false, business: false }
]
const {
  columns: workOrderColumns,
  saving: workOrderColumnSaving,
  isColumnVisible: isWorkOrderColumnVisible,
  getColumnWidthString: getWorkOrderColumnWidthString,
  getColumnMinWidthString: getWorkOrderColumnMinWidthString,
  handleHeaderDragend: handleWorkOrderHeaderDragend,
  saveConfig: saveWorkOrderColumnConfig
} = useUserTableColumns('mes.pro.workorder.main', workOrderDefaultColumns)
const isAdminUser = computed(() => checkRole(['admin']))

type WorkOrderTreeRow = ProWorkOrderVO & {
  children?: WorkOrderTreeRow[]
}

const loading = ref(true) // 列表的加载中
const list = ref<WorkOrderTreeRow[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  productNameKeyword: undefined as string | undefined,
  productCodeKeyword: undefined as string | undefined,
  requestDate: undefined as string[] | undefined,
  quickFilter: undefined as any
})
const exportLoading = ref(false) // 导出的加载中
const kingdeeSyncLoading = ref(false) // 金蝶同步加载中
const erpCreateLoadingId = ref<number | null>(null) // 行级创建 ERP 订单加载中
const openedRouteDetailId = ref('')
const formRef = ref<any>()
const { isExpandAll, refreshTable } = useTreeTableExpand(true)
const getList = async () => {
  loading.value = true
  try {
    const data = await ProWorkOrderApi.getWorkOrderPage(queryParams)
    list.value = handleTree(data.list, 'id', 'parentId') as WorkOrderTreeRow[]
    total.value = data.total
  } finally {
    loading.value = false
  }
}
type ProductSuggestion = MdItemVO & {
  value: string
  [key: string]: unknown
}
const toProductSuggestion = (item: MdItemVO, value: string): ProductSuggestion => ({
  ...item,
  value
})
const queryProductNameSuggestions = async (
  queryString: string,
  callback: (items: TableQuickFilterSuggestion[]) => void
) => {
  const keyword = queryString.trim()
  if (!keyword) {
    callback([])
    return
  }
  const data = await MdItemApi.getItemPage({
    pageNo: 1,
    pageSize: 20,
    name: keyword
  })
  callback((data.list ?? []).map((item) => toProductSuggestion(item, item.name)))
}
const queryProductCodeSuggestions = async (
  queryString: string,
  callback: (items: TableQuickFilterSuggestion[]) => void
) => {
  const keyword = queryString.trim()
  if (!keyword) {
    callback([])
    return
  }
  const data = await MdItemApi.getItemPage({
    pageNo: 1,
    pageSize: 20,
    code: keyword
  })
  callback((data.list ?? []).map((item) => toProductSuggestion(item, item.code)))
}
const workOrderQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'code', label: '工单编号', type: 'text', placeholder: '请输入工单编号' },
  {
    key: 'productName',
    label: '产品名称',
    type: 'autocomplete',
    placeholder: '请输入产品名称',
    fetchSuggestions: queryProductNameSuggestions
  },
  {
    key: 'productCode',
    label: '产品编码',
    type: 'autocomplete',
    placeholder: '请输入产品编码',
    fetchSuggestions: queryProductCodeSuggestions
  },
  { key: 'productSpecification', label: '规格型号', type: 'text', placeholder: '请输入规格型号' },
  { key: 'requestDate', label: '需求日期', type: 'dateRange' }
]
const workOrderQuickFilter = useTableQuickFilter(
  'mes.pro.workorder.main',
  workOrderQuickFilterDefinitions,
  queryParams,
  getList
)
const resetQuery = () => {
  workOrderQuickFilter.resetQuickFilter()
}

const openForm = (type: string, id?: number, parentRow?: any) => {
  formRef.value?.open(type, id, parentRow)
}
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await ProWorkOrderApi.exportWorkOrder(queryParams)
    download.excel(data, 'work-orders.xls')
  } finally {
    exportLoading.value = false
  }
}
const loadListFromRoute = async () => {
  queryParams.code = typeof route.query.code === 'string' ? route.query.code : undefined
  queryParams.pageNo = 1
  await getList()
  const openId = typeof route.query.openId === 'string' ? route.query.openId : ''
  if (!openId) {
    openedRouteDetailId.value = ''
    return
  }
  if (openedRouteDetailId.value === openId) return
  openedRouteDetailId.value = openId
  openForm('detail', Number(openId))
}
const handleSyncKingdeeWorkOrders = async () => {
  kingdeeSyncLoading.value = true
  try {
    await ErpKingdeeSyncApi.runIncrementalSyncJob('kingdeeProductionOrderSyncJob')
    message.success('生产工单增量同步任务已提交')
    await getList()
  } finally {
    kingdeeSyncLoading.value = false
  }
}
const handleCreateKingdeeProductionOrder = async (row: WorkOrderTreeRow) => {
  await message.confirm(`确认根据生产工单 ${row.code} 创建 ERP 测试生产订单？`)
  erpCreateLoadingId.value = row.id
  try {
    const result = await ProWorkOrderApi.createKingdeeProductionOrder(row.id)
    message.success(`ERP 测试生产订单已创建，单据编号：${result.erpBillNo}`)
    await getList()
  } finally {
    erpCreateLoadingId.value = null
  }
}
const handleOpenProductionMaterialList = async (row: WorkOrderTreeRow) => {
  await router.push({
    path: '/erp/production/material-list',
    query: { productionOrderNo: row.code }
  })
}
const handleOpenBatchRecord = async (row: WorkOrderTreeRow) => {
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution',
    query: {
      prefillWorkOrderCode: row.code
    }
  })
}
watch(
  () => [route.query.code, route.query.openId],
  async () => {
    await loadListFromRoute()
  }
)
onMounted(loadListFromRoute)
</script>

<style scoped>
.work-order-product-suggestion {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.4;
}
.work-order-product-suggestion__primary {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.work-order-product-suggestion__secondary {
  flex: none;
  color: #4b5563;
  font-size: 12px;
}
:global(.work-order-product-suggestion-popper) {
  width: 320px !important;
  max-width: 320px;
}
:global(.work-order-product-suggestion-popper .el-autocomplete-suggestion__wrap) {
  max-width: 320px;
}
.work-order-key-cell {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 4px;
}
.work-order-key-text {
  flex: 1 1 auto;
  min-width: 0;
  max-width: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.work-order-key-cell--number {
  justify-content: center;
  font-variant-numeric: tabular-nums;
}
</style>
