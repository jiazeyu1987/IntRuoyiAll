<template>
  <ContentWrap>
    <el-alert
      class="mb-16px"
      title="当前页面仅用于测试租户受控模拟链路：采购订单确认后，可继续执行发料、进度、送收货、检验与对账，但不会伪装成真实 PDA / 仓储 / 质检系统回执。"
      type="warning"
      :closable="false"
    />
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="92px">
      <el-form-item label="委外单号" prop="executionNo">
        <el-input v-model="queryParams.executionNo" clearable class="!w-180px" placeholder="请输入委外单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="采购订单" prop="purchaseOrderNo">
        <el-input
          v-model="queryParams.purchaseOrderNo"
          clearable
          class="!w-190px"
          placeholder="请输入采购订单号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierName">
        <el-input v-model="queryParams.supplierName" clearable class="!w-180px" placeholder="请输入供应商" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="executionStatus">
        <el-select v-model="queryParams.executionStatus" clearable class="!w-170px" placeholder="全部状态">
          <el-option v-for="item in srmOutsourceExecutionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" @click="openCreateDialog" v-hasPermi="['srm:outsource-execution:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新建委外执行
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="委外单号" prop="executionNo" width="170" />
      <el-table-column label="采购订单" prop="sourcePurchaseOrderNo" width="170" />
      <el-table-column label="供应商" prop="supplierName" min-width="160" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.executionStatus)">{{ row.executionStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模拟来源" width="180">
        <template #default="{ row }">
          <el-tag type="warning" effect="plain">{{ row.simulationLabel || '测试租户受控模拟链路' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计划 / 发料" width="130" align="right">
        <template #default="{ row }">{{ row.plannedQuantity || 0 }} / {{ row.issueQuantity || 0 }}</template>
      </el-table-column>
      <el-table-column label="进度" width="150">
        <template #default="{ row }">
          <div>{{ row.progressStage || '-' }}</div>
          <el-progress class="mt-6px" :percentage="Number(row.progressPercent || 0)" :stroke-width="10" />
        </template>
      </el-table-column>
      <el-table-column label="收货 / 合格" width="130" align="right">
        <template #default="{ row }">{{ row.receivedQuantity || 0 }} / {{ row.qualifiedQuantity || 0 }}</template>
      </el-table-column>
      <el-table-column label="对账金额" width="130" align="right">
        <template #default="{ row }">{{ row.reconciliation?.reconciliationAmount ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" :formatter="dateTimeValueFormatter" />
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="warning" :disabled="row.executionStatus !== 'PENDING_ISSUE'" @click="openIssueDialog(row)" v-hasPermi="['srm:outsource-execution:update']">
            发料
          </el-button>
          <el-button link type="success" :disabled="row.executionStatus !== 'DELIVERED'" @click="openInspectDialog(row)" v-hasPermi="['srm:outsource-execution:update']">
            检验
          </el-button>
          <el-button link type="danger" :disabled="row.executionStatus !== 'INSPECTED'" @click="openReconcileDialog(row)" v-hasPermi="['srm:outsource-execution:update']">
            对账
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="createVisible" title="创建委外执行单" width="720px">
    <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
      <el-form-item label="采购订单协同单" prop="purchaseOrderId">
        <el-select v-model="createForm.purchaseOrderId" filterable class="!w-1/1" placeholder="请选择已确认采购订单">
          <el-option
            v-for="item in purchaseOrderOptions"
            :key="item.id"
            :label="`${item.orderNo} | ${item.supplierName}`"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="模拟说明" prop="simulationRemark">
        <el-input v-model="createForm.simulationRemark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitCreate">创建</el-button>
    </template>
  </Dialog>

  <Dialog v-model="issueVisible" title="登记模拟发料" width="520px">
    <el-form ref="issueFormRef" :model="issueForm" :rules="issueRules" label-width="100px">
      <el-form-item label="发料数量" prop="issueQuantity">
        <el-input-number v-model="issueForm.issueQuantity" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
      </el-form-item>
      <el-form-item label="发料说明" prop="issueRemark">
        <el-input v-model="issueForm.issueRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="issueVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitIssue">确认发料</el-button>
    </template>
  </Dialog>

  <Dialog v-model="inspectVisible" title="登记检验结果" width="520px">
    <el-form ref="inspectFormRef" :model="inspectForm" :rules="inspectRules" label-width="100px">
      <el-form-item label="合格数量" prop="qualifiedQuantity">
        <el-input-number v-model="inspectForm.qualifiedQuantity" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
      </el-form-item>
      <el-form-item label="检验说明" prop="inspectRemark">
        <el-input v-model="inspectForm.inspectRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="inspectVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitInspect">确认检验</el-button>
    </template>
  </Dialog>

  <Dialog v-model="reconcileVisible" title="确认对账结果" width="520px">
    <el-form ref="reconcileFormRef" :model="reconcileForm" :rules="reconcileRules" label-width="100px">
      <el-form-item label="对账说明" prop="confirmRemark">
        <el-input v-model="reconcileForm.confirmRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reconcileVisible = false">取消</el-button>
      <el-button type="danger" :loading="actionLoading" @click="submitReconcile">确认对账</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="委外执行详情" width="1100px">
    <template v-if="currentDetail">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="委外单号">{{ currentDetail.executionNo }}</el-descriptions-item>
        <el-descriptions-item label="采购订单">{{ currentDetail.sourcePurchaseOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ currentDetail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ currentDetail.executionStatusLabel }}</el-descriptions-item>
        <el-descriptions-item label="模拟来源">{{ currentDetail.simulationLabel }}</el-descriptions-item>
        <el-descriptions-item label="模拟说明">{{ currentDetail.simulationRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ currentDetail.plannedQuantity || 0 }}</el-descriptions-item>
        <el-descriptions-item label="发料数量">{{ currentDetail.issueQuantity || 0 }}</el-descriptions-item>
        <el-descriptions-item label="单价">{{ currentDetail.unitPrice || 0 }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ currentDetail.progressStage || '-' }} / {{ currentDetail.progressPercent || 0 }}%</el-descriptions-item>
        <el-descriptions-item label="收货数量">{{ currentDetail.receivedQuantity || 0 }}</el-descriptions-item>
        <el-descriptions-item label="合格数量">{{ currentDetail.qualifiedQuantity || 0 }}</el-descriptions-item>
      </el-descriptions>

      <el-card v-if="currentDetail.reconciliation" class="mt-16px" shadow="never">
        <template #header>对账结果</template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="对账单号">{{ currentDetail.reconciliation.reconciliationNo }}</el-descriptions-item>
          <el-descriptions-item label="对账状态">{{ currentDetail.reconciliation.reconciliationStatusLabel }}</el-descriptions-item>
          <el-descriptions-item label="对账金额">{{ currentDetail.reconciliation.reconciliationAmount }}</el-descriptions-item>
          <el-descriptions-item label="差异数量">{{ currentDetail.reconciliation.diffQuantity }}</el-descriptions-item>
          <el-descriptions-item label="差异金额">{{ currentDetail.reconciliation.diffAmount }}</el-descriptions-item>
          <el-descriptions-item label="说明">{{ currentDetail.reconciliation.confirmRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-table class="mt-16px" :data="currentDetail.events" border size="small">
        <el-table-column label="事件单号" prop="eventNo" width="160" />
        <el-table-column label="事件类型" prop="eventTypeLabel" width="140" />
        <el-table-column label="状态流转" min-width="180">
          <template #default="{ row }">{{ row.beforeStatus || '-' }} -> {{ row.afterStatus || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作人" prop="operatorName" width="120" />
        <el-table-column label="事件说明" prop="eventRemark" min-width="220" />
        <el-table-column label="事件时间" prop="eventTime" width="180" :formatter="dateTimeValueFormatter" />
      </el-table>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import { dateTimeValueFormatter } from '@/utils/formatTime'
import {
  SrmOutsourceExecutionApi,
  srmOutsourceExecutionStatusOptions,
  type SrmOutsourceExecutionCreateReqVO,
  type SrmOutsourceExecutionVO
} from '@/api/srm/outsource-execution'
import { SrmPurchaseOrderApi, type SrmPurchaseOrderVO } from '@/api/srm/purchase-order'

defineOptions({ name: 'SrmOutsourceExecution' })

const message = useMessage()
const loading = ref(false)
const actionLoading = ref(false)
const list = ref<SrmOutsourceExecutionVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  executionNo: undefined as string | undefined,
  purchaseOrderNo: undefined as string | undefined,
  supplierName: undefined as string | undefined,
  executionStatus: undefined as string | undefined
})

const purchaseOrderOptions = ref<SrmPurchaseOrderVO[]>([])
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<SrmOutsourceExecutionCreateReqVO>({
  purchaseOrderId: undefined as unknown as number,
  simulationRemark: '测试租户受控模拟链路'
})
const createRules = reactive<FormRules>({
  purchaseOrderId: [{ required: true, message: '请选择采购订单协同单', trigger: 'change' }]
})

const issueVisible = ref(false)
const issueFormRef = ref<FormInstance>()
const issueForm = reactive({
  id: undefined as unknown as number,
  issueQuantity: 0,
  issueRemark: '模拟 PDA 发料'
})
const issueRules = reactive<FormRules>({
  issueQuantity: [{ required: true, message: '请输入发料数量', trigger: 'change' }]
})

const inspectVisible = ref(false)
const inspectFormRef = ref<FormInstance>()
const inspectForm = reactive({
  id: undefined as unknown as number,
  qualifiedQuantity: 0,
  inspectRemark: '模拟检验合格'
})
const inspectRules = reactive<FormRules>({
  qualifiedQuantity: [{ required: true, message: '请输入合格数量', trigger: 'change' }]
})

const reconcileVisible = ref(false)
const reconcileFormRef = ref<FormInstance>()
const reconcileForm = reactive({
  id: undefined as unknown as number,
  confirmRemark: '模拟对账确认'
})
const reconcileRules = reactive<FormRules>({
  confirmRemark: [{ required: true, message: '请输入对账说明', trigger: 'blur' }]
})

const detailVisible = ref(false)
const currentDetail = ref<SrmOutsourceExecutionVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'RECONCILED') return 'success'
  if (status === 'PENDING_ISSUE' || status === 'INSPECTED') return 'warning'
  if (status === 'IN_PRODUCTION' || status === 'DELIVERED') return 'primary'
  return 'info'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmOutsourceExecutionApi.getOutsourceExecutionPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '委外执行列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const loadPurchaseOrders = async () => {
  const data = await SrmPurchaseOrderApi.getPurchaseOrderPage({
    pageNo: 1,
    pageSize: 100,
    orderStatus: 'CONFIRMED'
  })
  purchaseOrderOptions.value = data.list || []
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
  queryFormRef.value?.resetFields()
  handleQuery(true)
}

const openCreateDialog = async () => {
  await loadPurchaseOrders()
  createForm.purchaseOrderId = undefined as unknown as number
  createForm.simulationRemark = '测试租户受控模拟链路'
  createVisible.value = true
}

const submitCreate = async () => {
  await createFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmOutsourceExecutionApi.createFromPurchaseOrder(createForm)
    createVisible.value = false
    message.success('委外执行单已创建')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '委外执行单创建失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openIssueDialog = (row: SrmOutsourceExecutionVO) => {
  issueForm.id = row.id!
  issueForm.issueQuantity = Number(row.plannedQuantity || 0)
  issueForm.issueRemark = '模拟 PDA 发料'
  issueVisible.value = true
}

const submitIssue = async () => {
  await issueFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmOutsourceExecutionApi.issue(issueForm)
    issueVisible.value = false
    message.success('模拟发料已登记')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '模拟发料登记失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openInspectDialog = (row: SrmOutsourceExecutionVO) => {
  inspectForm.id = row.id!
  inspectForm.qualifiedQuantity = Number(row.receivedQuantity || 0)
  inspectForm.inspectRemark = '模拟检验合格'
  inspectVisible.value = true
}

const submitInspect = async () => {
  await inspectFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmOutsourceExecutionApi.inspect(inspectForm)
    inspectVisible.value = false
    message.success('检验结果已登记')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '检验结果登记失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openReconcileDialog = (row: SrmOutsourceExecutionVO) => {
  reconcileForm.id = row.id!
  reconcileForm.confirmRemark = '模拟对账确认'
  reconcileVisible.value = true
}

const submitReconcile = async () => {
  await reconcileFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmOutsourceExecutionApi.reconcile(reconcileForm)
    reconcileVisible.value = false
    message.success('对账结果已确认')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '对账确认失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openDetail = async (row: SrmOutsourceExecutionVO) => {
  if (!row.id) return
  try {
    currentDetail.value = await SrmOutsourceExecutionApi.getOutsourceExecution(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '委外执行详情加载失败。'))
    throw error
  }
}

onMounted(() => {
  getList()
})
</script>
