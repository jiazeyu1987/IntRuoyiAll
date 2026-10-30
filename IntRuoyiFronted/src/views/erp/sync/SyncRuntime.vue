<template>
  <div class="erp-runtime-panel">
    <el-tabs v-model="activeList" class="erp-list-tabs">
      <el-tab-pane label="运行记录" name="history" />
      <el-tab-pane label="成功水位" name="watermarks" />
    </el-tabs>
  <Dialog title="新增ERP工单" v-model="productionOrderDialogVisible" width="720px">
    <el-form
      ref="productionOrderFormRef"
      :model="productionOrderForm"
      :rules="productionOrderRules"
      label-width="116px"
      v-loading="productionOrderSubmitting"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="ERP工单号" prop="billNo">
            <el-input v-model="productionOrderForm.billNo" placeholder="请输入ERP生产工单号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="物料编码" prop="materialNumber">
            <el-input
              v-model="productionOrderForm.materialNumber"
              placeholder="请输入有工艺路线的物料编码"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位编码" prop="unitNumber">
            <el-input v-model="productionOrderForm.unitNumber" placeholder="请输入ERP单位编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生产数量" prop="quantity">
            <el-input-number
              v-model="productionOrderForm.quantity"
              :min="0.0001"
              :precision="4"
              controls-position="right"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计划开始" prop="plannedStartDate">
            <el-date-picker
              v-model="productionOrderForm.plannedStartDate"
              type="datetime"
              value-format="x"
              placeholder="请选择计划开始时间"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计划完成" prop="plannedFinishDate">
            <el-date-picker
              v-model="productionOrderForm.plannedFinishDate"
              type="datetime"
              value-format="x"
              placeholder="请选择计划完成时间"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源单号" prop="sourceBillNo">
            <el-input
              v-model="productionOrderForm.sourceBillNo"
              placeholder="请输入来源单号，可为空"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="批次号" prop="batchNumber">
            <el-input v-model="productionOrderForm.batchNumber" placeholder="请输入批次号" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="productionOrderDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="productionOrderSubmitting" @click="submitProductionOrder">
        创建并提交ERP工单
      </el-button>
    </template>
  </Dialog>

  <ContentWrap v-if="lastProductionOrderClosure" class="erp-production-order-closure">
    <el-alert
      title="ERP已保存并提交；MES 工单生成需要等待生产工单同步任务完成。"
      type="success"
      :closable="false"
      show-icon
      class="mb-12px"
    />
    <el-descriptions :column="3" border>
      <el-descriptions-item label="ERP工单号">
        {{ lastProductionOrderClosure.erpBillNo }}
      </el-descriptions-item>
      <el-descriptions-item label="ERP FID">
        {{ lastProductionOrderClosure.erpFid }}
      </el-descriptions-item>
      <el-descriptions-item label="保存/提交">
        {{ lastProductionOrderClosure.saved ? '已保存' : '未保存' }} /
        {{ lastProductionOrderClosure.submitted ? '已提交' : '未提交' }}
      </el-descriptions-item>
      <el-descriptions-item label="MES工单生成状态">
        {{
          lastProductionOrderClosure.syncSubmitted
            ? '同步任务已提交，等待生成确认'
            : '待触发生产工单同步'
        }}
      </el-descriptions-item>
      <el-descriptions-item label="同步处理器">
        {{ lastProductionOrderClosure.syncHandlerName || 'kingdeeProductionOrderSyncJob' }}
      </el-descriptions-item>
      <el-descriptions-item label="下一步责任">
        A2 在 MES 生产工单与排产工单池确认入池
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap v-show="activeList === 'watermarks'" class="erp-runtime-summary">
    <el-alert
      title="最近成功水位仅表示同步窗口水位，不代表当前 ERP 单据已生成 MES 工单。"
      type="info"
      :closable="false"
      show-icon
      class="mb-12px"
    />
    <ErpLocalList
      table-key="erp.sync.watermarks"
      :rows="syncStatusRows"
      :default-columns="watermarkColumns"
      :filter-definitions="watermarkFilters"
    >
      <template #default="{ rows, isColumnVisible, getColumnWidthString, handleHeaderDragend }">
        <el-table
          v-loading="watermarkLoading"
          :data="rows"
          :stripe="true"
          data-user-table-column-explicit
          data-user-table-key="erp.sync.watermarks"
          @header-dragend="handleHeaderDragend"
         height="100%">
          <el-table-column
            v-if="isColumnVisible('label')"
            column-key="label"
            label="同步对象"
            :min-width="getColumnWidthString('label', 150)"
          >
            <template #default="{ row }">{{ row.label }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('handlerName')"
            column-key="handlerName"
            label="处理器"
            prop="handlerName"
            :min-width="getColumnWidthString('handlerName', 240)"
          />
          <el-table-column
            v-if="isColumnVisible('lastSuccessTime')"
            column-key="lastSuccessTime"
            label="最近成功水位"
            :min-width="getColumnWidthString('lastSuccessTime', 180)"
          >
            <template #default="{ row }">
              {{ formatDate(row.lastSuccessTime) || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="false"
            column-key="schedule"
            label="调度"
            :width="getColumnWidthString('schedule', 210)"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openJobPage(row.handlerName)"
                >任务配置</el-button
              >
              <el-button link type="primary" @click="openJobLogPage(row.handlerName)"
                >执行日志</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </template>
    </ErpLocalList>
  </ContentWrap>

  <ContentWrap v-show="activeList === 'history'" class="erp-runtime-history">
    <UnifiedListTemplate
      table-key="erp.sync.runs"
      single-line-toolbar
      :query-model="queryParams"
      :filter-definitions="runFilters"
      :quick-filter-state="runFilter.state"
      :selected-filter-definition="runFilter.selectedDefinition.value"
      :operator-options="runFilter.operatorOptions.value"
      :columns="runColumns"
      :column-saving="runColumnSaving"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="runFilter.updateState"
      @quick-filter-query="runFilter.applyQuickFilter"
      @column-change="saveRunColumns"
      @pagination="getRunList"
      :load-filter-rows="loadColumnFilterRows0"
      @filtered-rows="list = $event"
      :list-rows="list"
      @column-filter-reset="getRunList"
      :column-filter-definitions="listColumnFilterDefinitions0"
    >
      <template #actions>
        <el-button type="primary" plain @click="getList" v-hasPermi="['erp:kingdee-sync:query']">
          <Icon icon="ep:refresh-right" class="mr-5px" /> 刷新
        </el-button>
        <el-button
          v-show="false"
          type="primary"
          @click="openProductionOrderDialog"
          v-hasPermi="['erp:kingdee-sync:query']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增ERP工单
        </el-button>
      </template>
      <template #table="{ resolveRows }"
        ><el-table
          v-loading="loading"
          :data="resolveRows(list)"
          :stripe="true"
          :show-overflow-tooltip="true"
          border
          row-key="id"
          data-user-table-column-explicit
          data-user-table-key="erp.sync.runs"
          @header-dragend="handleRunHeaderDragend"
         height="100%">
          <el-table-column
            v-if="isRunColumnVisible('id')"
            column-key="id"
            label="编号"
            prop="id"
            :width="getRunColumnWidthString('id', 90)"
          />
          <el-table-column
            v-if="isRunColumnVisible('syncType')"
            column-key="syncType"
            label="同步类型"
            :width="getRunColumnWidthString('syncType', 150)"
          >
            <template #default="{ row }">{{ getSyncTypeLabel(row.syncType) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isRunColumnVisible('triggerType')"
            column-key="triggerType"
            label="触发"
            prop="triggerType"
            :width="getRunColumnWidthString('triggerType', 100)"
          />
          <el-table-column
            v-if="isRunColumnVisible('status')"
            column-key="status"
            label="状态"
            :width="getRunColumnWidthString('status', 100)"
          >
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRunColumnVisible('windowStartTime')"
            column-key="windowStartTime"
            label="窗口开始"
            :width="getRunColumnWidthString('windowStartTime', 170)"
          >
            <template #default="{ row }">{{ formatDate(row.windowStartTime) || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isRunColumnVisible('windowEndTime')"
            column-key="windowEndTime"
            label="窗口结束"
            :width="getRunColumnWidthString('windowEndTime', 170)"
          >
            <template #default="{ row }">{{ formatDate(row.windowEndTime) || '-' }}</template>
          </el-table-column>
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isRunColumnVisible('createdCount')"
            column-key="createdCount"
            label="新增"
            prop="createdCount"
            :width="getRunColumnWidthString('createdCount', 80)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isRunColumnVisible('updatedCount')"
            column-key="updatedCount"
            label="更新"
            prop="updatedCount"
            :width="getRunColumnWidthString('updatedCount', 80)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isRunColumnVisible('skippedCount')"
            column-key="skippedCount"
            label="跳过"
            prop="skippedCount"
            :width="getRunColumnWidthString('skippedCount', 80)"
          />
          <el-table-column
            :formatter="erpListIntegerColumnFormatter"
            v-if="isRunColumnVisible('failedCount')"
            column-key="failedCount"
            label="失败"
            prop="failedCount"
            :width="getRunColumnWidthString('failedCount', 80)"
          />
          <el-table-column
            v-if="isRunColumnVisible('failureMessage')"
            column-key="failureMessage"
            label="失败原因"
            prop="failureMessage"
            :width="getRunColumnWidthString('failureMessage', 220)"
          /> </el-table
      ></template>
    </UnifiedListTemplate>
  </ContentWrap>
  </div>
</template>
<script setup lang="ts">
import { loadAllListPages, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

const activeList = ref('history')
import { erpListIntegerColumnFormatter } from '@/views/erp/utils/integerDisplay'
import ErpLocalList from '@/views/erp/components/ErpLocalList.vue'
import type { UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import type { TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

const watermarkColumns: UserTableColumnDefinition[] = [
  {
    "key": "label",
    "label": "同步对象",
    "width": 150,
    "sortable": false
  },
  {
    "key": "handlerName",
    "label": "处理器",
    "width": 240,
    "sortable": false
  },
  {
    "key": "lastSuccessTime",
    "label": "最近成功水位",
    "width": 180,
    "sortable": false
  },
]
const watermarkFilters: TableQuickFilterDefinition[] = [
  { key: 'label', label: '同步对象', type: 'text', queryParamKey: 'label', operators: ['contains'] },
  {
    "key": "handlerName",
    "label": "处理器",
    "type": "text",
    "queryParamKey": "handlerName",
    "operators": [
      "contains"
    ]
  }
]

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns } from '@/hooks/web/useUserTableColumns'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import type {
  ErpKingdeeProductionOrderCreateReqVO,
  ErpKingdeeProductionOrderCreateRespVO,
  ErpKingdeeSyncRunVO
} from '@/api/erp/sync'
import { formatDate } from '@/utils/formatTime'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({ name: 'ErpSyncRuntime' })

const message = useMessage()
const { push } = useRouter()

const syncTypes = [
  { type: 'PRODUCT', label: '物料列表', handlerName: 'kingdeeProductItemSyncJob' },
  { type: 'STOCK', label: 'ERP 库存', handlerName: 'kingdeeStockSyncJob' },
  { type: 'STOCK_MOVE', label: '金蝶调拨单', handlerName: 'kingdeeStockMoveSyncJob' },
  { type: 'PURCHASE_ORDER', label: '采购订单', handlerName: 'kingdeePurchaseOrderSyncJob' },
  { type: 'SALE_ORDER', label: '销售订单', handlerName: 'kingdeeSaleOrderSyncJob' },
  { type: 'PRODUCTION_ORDER', label: '生产工单', handlerName: 'kingdeeProductionOrderSyncJob' },
  {
    type: 'PRODUCTION_PICK_LIST',
    label: '生产领料单列表',
    handlerName: 'kingdeeProductionPickListSyncJob'
  },
  {
    type: 'PRODUCTION_REPLENISHMENT_LIST',
    label: '生产补料单列表',
    handlerName: 'kingdeeProductionReplenishmentListSyncJob'
  },
  {
    type: 'PRODUCTION_MATERIAL_LIST',
    label: '生产用料清单',
    handlerName: 'kingdeeProductionMaterialListSyncJob'
  },
  { type: 'BOM', label: '产品 BOM', handlerName: 'kingdeeBomSyncJob' }
]

const loading = ref(false)
const watermarkLoading = ref(false)
const list = ref<ErpKingdeeSyncRunVO[]>([])
const total = ref(0)
const watermarks = ref<Record<string, string | undefined>>({})
const lastProductionOrderClosure = ref<{
  erpBillNo: string
  erpFid: string
  saved: boolean
  submitted: boolean
  syncSubmitted: boolean
  syncHandlerName: string
  confirmedAt: string
} | null>(null)
const productionOrderDialogVisible = ref(false)
const productionOrderSubmitting = ref(false)
const productionOrderFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  syncType: undefined,
  status: undefined
})
const productionOrderForm = reactive<ErpKingdeeProductionOrderCreateReqVO>({
  billNo: '',
  materialNumber: '',
  unitNumber: '',
  quantity: 1,
  plannedStartDate: Date.now() + 12 * 60 * 60 * 1000,
  plannedFinishDate: Date.now() + 36 * 60 * 60 * 1000,
  sourceBillNo: '',
  batchNumber: ''
})
const productionOrderRules = reactive<FormRules>({
  billNo: [{ required: true, message: '请输入ERP生产工单号', trigger: 'blur' }],
  materialNumber: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  unitNumber: [{ required: true, message: '请输入单位编码', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入生产数量', trigger: 'blur' }],
  plannedStartDate: [{ required: true, message: '请选择计划开始时间', trigger: 'change' }],
  plannedFinishDate: [{ required: true, message: '请选择计划完成时间', trigger: 'change' }],
  batchNumber: [{ required: true, message: '请输入批次号', trigger: 'blur' }]
})

const syncStatusRows = computed(() =>
  syncTypes.map((item) => ({
    ...item,
    lastSuccessTime: watermarks.value[item.type]
  }))
)

const getSyncTypeLabel = (syncType: string) =>
  syncTypes.find((item) => item.type === syncType)?.label || syncType

const getStatusLabel = (status: number) => {
  if (status === 10) return '运行中'
  if (status === 20) return '成功'
  if (status === 30) return '失败'
  return String(status)
}

const getStatusTagType = (status: number) => {
  if (status === 20) return 'success'
  if (status === 30) return 'danger'
  return 'primary'
}

const getWatermarks = async () => {
  watermarkLoading.value = true
  try {
    const data = await ErpKingdeeSyncApi.getWatermarkList()
    watermarks.value = data.reduce<Record<string, string | undefined>>((acc, item) => {
      acc[item.syncType] = item.lastSuccessTime
      return acc
    }, {})
  } finally {
    watermarkLoading.value = false
  }
}

const getRunList = async () => {
  loading.value = true
  try {
    const data = await ErpKingdeeSyncApi.getRunPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const getList = async () => {
  await Promise.all([getWatermarks(), getRunList()])
}


const resetProductionOrderForm = () => {
  productionOrderForm.billNo = ''
  productionOrderForm.materialNumber = ''
  productionOrderForm.unitNumber = ''
  productionOrderForm.quantity = 1
  productionOrderForm.plannedStartDate = Date.now() + 12 * 60 * 60 * 1000
  productionOrderForm.plannedFinishDate = Date.now() + 36 * 60 * 60 * 1000
  productionOrderForm.sourceBillNo = ''
  productionOrderForm.batchNumber = ''
}

const openProductionOrderDialog = () => {
  resetProductionOrderForm()
  productionOrderDialogVisible.value = true
}

const submitProductionOrder = async () => {
  await productionOrderFormRef.value?.validate()
  const plannedStartDate = Number(productionOrderForm.plannedStartDate)
  const plannedFinishDate = Number(productionOrderForm.plannedFinishDate)
  if (plannedFinishDate < plannedStartDate) {
    message.error('计划完成时间不能早于计划开始时间')
    return
  }
  productionOrderSubmitting.value = true
  try {
    const result: ErpKingdeeProductionOrderCreateRespVO = await ErpKingdeeSyncApi.createProductionOrder({
      ...productionOrderForm,
      plannedStartDate,
      plannedFinishDate
    })
    lastProductionOrderClosure.value = {
      erpBillNo: result.erpBillNo,
      erpFid: result.erpFid,
      saved: result.saved,
      submitted: result.submitted,
      syncSubmitted: false,
      syncHandlerName: 'kingdeeProductionOrderSyncJob',
      confirmedAt: new Date().toISOString()
    }
    message.success(`ERP已保存并提交：${result.erpBillNo}`)
    productionOrderDialogVisible.value = false
    await getList()
  } finally {
    productionOrderSubmitting.value = false
  }
}

const openJobPage = (handlerName: string) => {
  push({ name: 'InfraJob', query: { handlerName } })
}

const openJobLogPage = (handlerName: string) => {
  push({ name: 'InfraJobLog', query: { handlerName } })
}


const runFilters: TableQuickFilterDefinition[] = [
  { key: 'syncType', label: '同步类型', type: 'select', queryParamKey: 'syncType', options: syncTypes.map(item => ({label:item.label,value:item.type})) },
  { key: 'status', label: '运行状态', type: 'select', queryParamKey: 'status', options: [{label:'运行中',value:10},{label:'成功',value:20},{label:'失败',value:30}] }
]
const runDefaultColumns: UserTableColumnDefinition[] = [
  {
    "key": "id",
    "label": "编号",
    "width": 90,
    "sortable": false
  },
  {
    "key": "syncType",
    "label": "同步类型",
    "width": 150,
    "sortable": false
  },
  {
    "key": "triggerType",
    "label": "触发",
    "width": 100,
    "sortable": false
  },
  {
    "key": "status",
    "label": "状态",
    "width": 100,
    "sortable": false
  },
  {
    "key": "windowStartTime",
    "label": "窗口开始",
    "width": 170,
    "sortable": false
  },
  {
    "key": "windowEndTime",
    "label": "窗口结束",
    "width": 170,
    "sortable": false
  },
  {
    "key": "createdCount",
    "label": "新增",
    "width": 80,
    "sortable": false
  },
  {
    "key": "updatedCount",
    "label": "更新",
    "width": 80,
    "sortable": false
  },
  {
    "key": "skippedCount",
    "label": "跳过",
    "width": 80,
    "sortable": false
  },
  {
    "key": "failedCount",
    "label": "失败",
    "width": 80,
    "sortable": false
  },
  {
    "key": "failureMessage",
    "label": "失败原因",
    "width": 220,
    "sortable": false
  }
]
const { columns: runColumns, saving: runColumnSaving, isColumnVisible: isRunColumnVisible,
  getColumnWidthString: getRunColumnWidthString, handleHeaderDragend: handleRunHeaderDragend,
  saveConfig: saveRunColumns } = useUserTableColumns('erp.sync.runs', runDefaultColumns)
const runFilter = useTableQuickFilter('erp.sync.runs', runFilters, queryParams, getRunList)

onMounted(getList)

const loadColumnFilterRows0 = async () => {
  const params = { ...queryParams }
  const rows = await loadAllListPages<any>((pagination) =>
    ErpKingdeeSyncApi.getRunPage({ ...params, ...pagination })
  )
  return rows
}

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'syncType',
    label: '同步类型',
    type: 'text',
    getValue: (row) => [getSyncTypeLabel(row.syncType)].join(' ')
  },
  {
    key: 'status',
    label: '状态',
    type: 'text',
    getValue: (row) => [getStatusLabel(row.status)].join(' ')
  }
])
</script>
