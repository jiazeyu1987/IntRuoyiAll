<template>
  <doc-alert title="【ERP】金蝶同步运行" url="https://doc.iocoder.cn/erp/" />

  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="82px"
    >
      <el-form-item label="同步类型" prop="syncType">
        <el-select v-model="queryParams.syncType" clearable class="!w-220px">
          <el-option
            v-for="item in syncTypes"
            :key="item.type"
            :label="item.label"
            :value="item.type"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="运行状态" prop="status">
        <el-select v-model="queryParams.status" clearable class="!w-180px">
          <el-option label="运行中" :value="10" />
          <el-option label="成功" :value="20" />
          <el-option label="失败" :value="30" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="getList" v-hasPermi="['erp:kingdee-sync:query']">
          <Icon icon="ep:refresh-right" class="mr-5px" /> 刷新
        </el-button>
        <el-button type="primary" @click="openProductionOrderDialog" v-hasPermi="['erp:kingdee-sync:query']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增ERP工单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

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
            <el-input v-model="productionOrderForm.materialNumber" placeholder="请输入有工艺路线的物料编码" />
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
            <el-input v-model="productionOrderForm.sourceBillNo" placeholder="请输入来源单号，可为空" />
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
      <el-button
        type="primary"
        :loading="productionOrderSubmitting"
        @click="submitProductionOrder"
      >
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
        {{ lastProductionOrderClosure.syncSubmitted ? '同步任务已提交，等待生成确认' : '待触发生产工单同步' }}
      </el-descriptions-item>
      <el-descriptions-item label="同步处理器">
        {{ lastProductionOrderClosure.syncHandlerName || 'kingdeeProductionOrderSyncJob' }}
      </el-descriptions-item>
      <el-descriptions-item label="下一步责任">
        A2 在 MES 生产工单与排产工单池确认入池
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="最近成功水位仅表示同步窗口水位，不代表当前 ERP 单据已生成 MES 工单。"
      type="info"
      :closable="false"
      show-icon
      class="mb-12px"
    />
    <el-table v-loading="watermarkLoading" :data="syncStatusRows" :stripe="true">
      <el-table-column label="同步对象" min-width="150">
        <template #default="{ row }">{{ row.label }}</template>
      </el-table-column>
      <el-table-column label="处理器" min-width="240" prop="handlerName" />
      <el-table-column label="最近成功水位" min-width="180">
        <template #default="{ row }">
          {{ formatDate(row.lastSuccessTime) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="调度" width="210">
        <template #default="{ row }">
          <el-button link type="primary" @click="openJobPage(row.handlerName)">任务配置</el-button>
          <el-button link type="primary" @click="openJobLogPage(row.handlerName)">执行日志</el-button>
        </template>
      </el-table-column>
      <el-table-column label="手动补跑" width="130">
        <template #default="{ row }">
          <el-button
            link
            type="warning"
            :loading="runningHandlerName === row.handlerName"
            @click="runSyncJob(row)"
            v-hasPermi="['infra:job:trigger']"
          >
            增量同步
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" width="90" prop="id" />
      <el-table-column label="同步类型" min-width="150">
        <template #default="{ row }">{{ getSyncTypeLabel(row.syncType) }}</template>
      </el-table-column>
      <el-table-column label="触发" width="100" prop="triggerType" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="窗口开始" min-width="170">
        <template #default="{ row }">{{ formatDate(row.windowStartTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="窗口结束" min-width="170">
        <template #default="{ row }">{{ formatDate(row.windowEndTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="新增" width="80" prop="createdCount" />
      <el-table-column label="更新" width="80" prop="updatedCount" />
      <el-table-column label="跳过" width="80" prop="skippedCount" />
      <el-table-column label="失败" width="80" prop="failedCount" />
      <el-table-column label="失败原因" min-width="220" prop="failureMessage" />
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getRunList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import type {
  ErpKingdeeProductionOrderCreateReqVO,
  ErpKingdeeProductionOrderCreateRespVO,
  ErpKingdeeSyncRunVO
} from '@/api/erp/sync'
import { formatDate } from '@/utils/formatTime'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({ name: 'ErpKingdeeSync' })

const message = useMessage()
const { push } = useRouter()

const syncTypes = [
  { type: 'PRODUCT', label: 'ERP 商品', handlerName: 'kingdeeProductItemSyncJob' },
  { type: 'STOCK', label: 'ERP 库存', handlerName: 'kingdeeStockSyncJob' },
  { type: 'STOCK_MOVE', label: '金蝶调拨单', handlerName: 'kingdeeStockMoveSyncJob' },
  { type: 'PURCHASE_ORDER', label: '采购订单', handlerName: 'kingdeePurchaseOrderSyncJob' },
  { type: 'SALE_ORDER', label: '销售订单', handlerName: 'kingdeeSaleOrderSyncJob' },
  { type: 'PRODUCTION_ORDER', label: '生产工单', handlerName: 'kingdeeProductionOrderSyncJob' },
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
const runningHandlerName = ref('')
const queryFormRef = ref()
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

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getRunList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery(true)
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

const runSyncJob = async (row: { handlerName: string; label: string }) => {
  runningHandlerName.value = row.handlerName
  try {
    await ErpKingdeeSyncApi.runIncrementalSyncJob(row.handlerName)
    if (row.handlerName === 'kingdeeProductionOrderSyncJob' && lastProductionOrderClosure.value) {
      lastProductionOrderClosure.value = {
        ...lastProductionOrderClosure.value,
        syncSubmitted: true,
        syncHandlerName: row.handlerName,
        confirmedAt: new Date().toISOString()
      }
    }
    message.success(`${row.label} 增量同步任务已提交`)
    await getList()
  } finally {
    runningHandlerName.value = ''
  }
}

onMounted(getList)
</script>
