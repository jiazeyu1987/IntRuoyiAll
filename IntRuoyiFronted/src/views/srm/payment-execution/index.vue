<template>
  <ContentWrap>
    <el-alert
      class="mb-16px"
      title="付款执行当前基于测试租户受控模拟链路：付款条件来源于真实采购合同，但审批与财务推送状态仅在本地任务范围内模拟留痕。"
      type="warning"
      :closable="false"
    />
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="88px">
      <el-form-item label="付款单号" prop="paymentNo">
        <el-input v-model="queryParams.paymentNo" clearable class="!w-180px" placeholder="请输入付款单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="对账单号" prop="reconciliationNo">
        <el-input
          v-model="queryParams.reconciliationNo"
          clearable
          class="!w-180px"
          placeholder="请输入对账单号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierName">
        <el-input v-model="queryParams.supplierName" clearable class="!w-180px" placeholder="请输入供应商" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="paymentStatus">
        <el-select v-model="queryParams.paymentStatus" clearable class="!w-170px" placeholder="全部状态">
          <el-option v-for="item in srmPaymentExecutionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" @click="openCreateDialog" v-hasPermi="['srm:payment-execution:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新建付款申请
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="付款单号" prop="paymentNo" width="170" />
      <el-table-column label="对账单号" prop="reconciliationNo" width="170" />
      <el-table-column label="合同编号" prop="contractNo" width="170" />
      <el-table-column label="供应商" prop="supplierName" min-width="160" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.paymentStatus)">{{ row.paymentStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款阶段" prop="paymentStage" width="120" />
      <el-table-column label="付款比例" prop="paymentRatio" width="100" align="right" />
      <el-table-column label="申请金额" prop="applyAmount" width="120" align="right" />
      <el-table-column label="应付日期" prop="dueDate" width="120" />
      <el-table-column label="财务回执" prop="pushRemark" min-width="180" />
      <el-table-column label="操作" width="300" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="success" :disabled="row.paymentStatus !== 'DRAFT'" @click="openSubmitDialog(row)" v-hasPermi="['srm:payment-execution:create']">
            提交
          </el-button>
          <el-button link type="warning" :disabled="row.paymentStatus !== 'PENDING_APPROVAL'" @click="openApproveDialog(row)" v-hasPermi="['srm:payment-execution:approve']">
            审批通过
          </el-button>
          <el-button link type="danger" :disabled="row.paymentStatus !== 'PENDING_APPROVAL'" @click="openRejectDialog(row)" v-hasPermi="['srm:payment-execution:approve']">
            驳回
          </el-button>
          <el-button
            link
            type="info"
            :disabled="row.paymentStatus !== 'APPROVED' && row.paymentStatus !== 'PUSH_FAILED'"
            @click="openPushDialog(row)"
            v-hasPermi="['srm:payment-execution:approve']"
          >
            财务回执
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="createVisible" title="创建付款申请" width="760px">
    <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
      <el-form-item label="对账结果" prop="reconciliationId">
        <el-select v-model="createForm.reconciliationId" filterable class="!w-1/1" placeholder="请选择已对账的委外执行">
          <el-option
            v-for="item in reconciliationOptions"
            :key="item.reconciliation?.id"
            :label="`${item.executionNo} | ${item.supplierName} | 对账 ${item.reconciliation?.reconciliationAmount || 0}`"
            :value="item.reconciliation?.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="采购合同" prop="contractId">
        <el-select v-model="createForm.contractId" filterable class="!w-1/1" placeholder="请选择同供应商生效合同">
          <el-option
            v-for="item in contractOptions"
            :key="item.id"
            :label="`${item.contractNo} | ${item.contractTitle}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="付款说明" prop="paymentRemark">
        <el-input v-model="createForm.paymentRemark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitCreate">创建</el-button>
    </template>
  </Dialog>

  <Dialog v-model="submitVisible" title="提交付款申请" width="520px">
    <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="100px">
      <el-form-item label="提交说明" prop="submitRemark">
        <el-input v-model="submitForm.submitRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitSubmit">确认提交</el-button>
    </template>
  </Dialog>

  <Dialog v-model="approveVisible" title="审批通过付款申请" width="520px">
    <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="100px">
      <el-form-item label="审批说明" prop="approveRemark">
        <el-input v-model="approveForm.approveRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="approveVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitApprove">确认审批</el-button>
    </template>
  </Dialog>

  <Dialog v-model="rejectVisible" title="驳回付款申请" width="520px">
    <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="100px">
      <el-form-item label="驳回原因" prop="rejectRemark">
        <el-input v-model="rejectForm.rejectRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectVisible = false">取消</el-button>
      <el-button type="danger" :loading="actionLoading" @click="submitReject">确认驳回</el-button>
    </template>
  </Dialog>

  <Dialog v-model="pushVisible" title="记录财务回执" width="560px">
    <el-form ref="pushFormRef" :model="pushForm" :rules="pushRules" label-width="100px">
      <el-form-item label="回执结果" prop="pushSuccess">
        <el-radio-group v-model="pushForm.pushSuccess">
          <el-radio :label="true">推送成功</el-radio>
          <el-radio :label="false">推送失败</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="回执说明" prop="pushRemark">
        <el-input v-model="pushForm.pushRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pushVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitPush">确认记录</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="付款执行详情" width="1040px">
    <template v-if="currentDetail">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="付款单号">{{ currentDetail.paymentNo }}</el-descriptions-item>
        <el-descriptions-item label="对账单号">{{ currentDetail.reconciliationNo }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ currentDetail.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ currentDetail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentDetail.paymentStatusLabel }}</el-descriptions-item>
        <el-descriptions-item label="模拟来源">{{ currentDetail.simulationLabel }}</el-descriptions-item>
        <el-descriptions-item label="付款条件" :span="3">{{ currentDetail.paymentTermSummary }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ currentDetail.applyAmount }}</el-descriptions-item>
        <el-descriptions-item label="付款比例">{{ currentDetail.paymentRatio }}</el-descriptions-item>
        <el-descriptions-item label="财务回执">{{ currentDetail.pushRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table class="mt-16px" :data="currentDetail.events" border size="small">
        <el-table-column label="事件单号" prop="eventNo" width="160" />
        <el-table-column label="事件类型" prop="eventTypeLabel" width="140" />
        <el-table-column label="状态流转" min-width="180">
          <template #default="{ row }">{{ row.beforeStatus || '-' }} -> {{ row.afterStatus || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作人" prop="operatorName" width="120" />
        <el-table-column label="说明" prop="eventRemark" min-width="220" />
        <el-table-column label="时间" prop="eventTime" width="180" />
      </el-table>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import {
  SrmPaymentExecutionApi,
  srmPaymentExecutionStatusOptions,
  type SrmPaymentExecutionVO
} from '@/api/srm/payment-execution'
import { SrmOutsourceExecutionApi, type SrmOutsourceExecutionVO } from '@/api/srm/outsource-execution'
import { SrmProcurementContractApi, type SrmProcurementContractVO } from '@/api/srm/procurement-contract'

defineOptions({ name: 'SrmPaymentExecution' })

const message = useMessage()
const loading = ref(false)
const actionLoading = ref(false)
const list = ref<SrmPaymentExecutionVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  paymentNo: undefined as string | undefined,
  reconciliationNo: undefined as string | undefined,
  supplierName: undefined as string | undefined,
  paymentStatus: undefined as string | undefined
})

const reconciliationOptions = ref<SrmOutsourceExecutionVO[]>([])
const contractOptions = ref<SrmProcurementContractVO[]>([])
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  reconciliationId: undefined as unknown as number,
  contractId: undefined as unknown as number,
  paymentRemark: '测试租户受控模拟链路'
})
const createRules = reactive<FormRules>({
  reconciliationId: [{ required: true, message: '请选择对账结果', trigger: 'change' }],
  contractId: [{ required: true, message: '请选择采购合同', trigger: 'change' }]
})

const submitVisible = ref(false)
const submitFormRef = ref<FormInstance>()
const submitForm = reactive({
  id: undefined as unknown as number,
  submitRemark: '提交付款申请'
})
const submitRules = reactive<FormRules>({
  submitRemark: [{ required: true, message: '请输入提交说明', trigger: 'blur' }]
})

const approveVisible = ref(false)
const approveFormRef = ref<FormInstance>()
const approveForm = reactive({
  id: undefined as unknown as number,
  approveRemark: '审批通过'
})
const approveRules = reactive<FormRules>({
  approveRemark: [{ required: true, message: '请输入审批说明', trigger: 'blur' }]
})

const rejectVisible = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({
  id: undefined as unknown as number,
  rejectRemark: ''
})
const rejectRules = reactive<FormRules>({
  rejectRemark: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }]
})

const pushVisible = ref(false)
const pushFormRef = ref<FormInstance>()
const pushForm = reactive({
  id: undefined as unknown as number,
  pushSuccess: false,
  pushRemark: '模拟财务回执'
})
const pushRules = reactive<FormRules>({
  pushSuccess: [{ required: true, message: '请选择回执结果', trigger: 'change' }],
  pushRemark: [{ required: true, message: '请输入回执说明', trigger: 'blur' }]
})

const detailVisible = ref(false)
const currentDetail = ref<SrmPaymentExecutionVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'PUSH_SUCCESS') return 'success'
  if (status === 'PUSH_FAILED' || status === 'REJECTED') return 'danger'
  if (status === 'PENDING_APPROVAL' || status === 'APPROVED') return 'warning'
  return 'info'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmPaymentExecutionApi.getPaymentExecutionPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '付款执行列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const loadReconciliations = async () => {
  const data = await SrmOutsourceExecutionApi.getOutsourceExecutionPage({
    pageNo: 1,
    pageSize: 100,
    executionStatus: 'RECONCILED'
  })
  reconciliationOptions.value = data.list || []
}

const loadContracts = async (supplierId?: number) => {
  if (!supplierId) {
    contractOptions.value = []
    return
  }
  const data = await SrmProcurementContractApi.getContractPage({
    pageNo: 1,
    pageSize: 100,
    supplierId,
    contractStatus: 'EFFECTIVE'
  })
  contractOptions.value = data.list || []
}

watch(
  () => createForm.reconciliationId,
  async (value) => {
    const selected = reconciliationOptions.value.find((item) => item.reconciliation?.id === value)
    createForm.contractId = undefined as unknown as number
    await loadContracts(selected?.supplierId)
  }
)

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
  await loadReconciliations()
  createForm.reconciliationId = undefined as unknown as number
  createForm.contractId = undefined as unknown as number
  createForm.paymentRemark = '测试租户受控模拟链路'
  contractOptions.value = []
  createVisible.value = true
}

const submitCreate = async () => {
  await createFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmPaymentExecutionApi.createFromReconciliation(createForm)
    createVisible.value = false
    message.success('付款申请已创建')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '付款申请创建失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openSubmitDialog = (row: SrmPaymentExecutionVO) => {
  submitForm.id = row.id!
  submitForm.submitRemark = '提交付款申请'
  submitVisible.value = true
}

const submitSubmit = async () => {
  await submitFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmPaymentExecutionApi.submit(submitForm)
    submitVisible.value = false
    message.success('付款申请已提交审批')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '付款申请提交失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openApproveDialog = (row: SrmPaymentExecutionVO) => {
  approveForm.id = row.id!
  approveForm.approveRemark = '审批通过'
  approveVisible.value = true
}

const submitApprove = async () => {
  await approveFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmPaymentExecutionApi.approve(approveForm)
    approveVisible.value = false
    message.success('付款申请已审批通过')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '付款申请审批失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openRejectDialog = (row: SrmPaymentExecutionVO) => {
  rejectForm.id = row.id!
  rejectForm.rejectRemark = ''
  rejectVisible.value = true
}

const submitReject = async () => {
  await rejectFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmPaymentExecutionApi.reject(rejectForm)
    rejectVisible.value = false
    message.success('付款申请已驳回')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '付款申请驳回失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openPushDialog = (row: SrmPaymentExecutionVO) => {
  pushForm.id = row.id!
  pushForm.pushSuccess = row.paymentStatus === 'APPROVED'
  pushForm.pushRemark = row.pushRemark || '模拟财务回执'
  pushVisible.value = true
}

const submitPush = async () => {
  await pushFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmPaymentExecutionApi.financePush(pushForm)
    pushVisible.value = false
    message.success('财务回执已记录')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '财务回执记录失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openDetail = async (row: SrmPaymentExecutionVO) => {
  if (!row.id) return
  try {
    currentDetail.value = await SrmPaymentExecutionApi.getPaymentExecution(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '付款执行详情加载失败。'))
    throw error
  }
}

onMounted(() => {
  getList()
})
</script>
