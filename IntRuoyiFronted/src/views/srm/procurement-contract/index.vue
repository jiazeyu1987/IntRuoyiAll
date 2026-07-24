<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="queryParams.contractNo" clearable class="!w-210px" placeholder="请输入合同编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合同标题" prop="contractTitle">
        <el-input v-model="queryParams.contractTitle" clearable class="!w-220px" placeholder="请输入合同标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" clearable class="!w-150px" placeholder="全部">
          <el-option v-for="item in srmProcurementContractSourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="contractStatus">
        <el-select v-model="queryParams.contractStatus" clearable class="!w-140px" placeholder="全部">
          <el-option v-for="item in srmProcurementContractStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" @click="openCreateDialog()" v-hasPermi="['srm:procurement-contract:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新建合同
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="合同编号" prop="contractNo" width="170" />
      <el-table-column label="合同标题" prop="contractTitle" min-width="190" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.contractStatus)">{{ row.contractStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源类型" prop="sourceTypeLabel" width="120" />
      <el-table-column label="来源单号" prop="sourceNo" width="170" />
      <el-table-column label="供应商" prop="supplierName" min-width="170" />
      <el-table-column label="合同金额" prop="contractAmount" width="120" align="right" />
      <el-table-column label="生效日期" prop="effectiveDate" width="120" />
      <el-table-column label="到期日期" prop="expireDate" width="120" />
      <el-table-column label="操作" width="230" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="warning" :disabled="row.contractStatus !== 'EFFECTIVE'" @click="openCancelDialog(row)" v-hasPermi="['srm:procurement-contract:cancel']">作废</el-button>
          <el-button link type="danger" :disabled="row.contractStatus !== 'EFFECTIVE'" @click="handleDelete(row)" v-hasPermi="['srm:procurement-contract:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="createVisible" title="创建采购合同" width="980px">
    <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="来源类型" prop="sourceType">
            <el-select v-model="createForm.sourceType" class="!w-1/1">
              <el-option v-for="item in srmProcurementContractSourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="来源ID" prop="sourceId">
            <el-input-number v-model="createForm.sourceId" :min="1" class="!w-1/1" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="合同金额" prop="contractAmount">
            <el-input-number v-model="createForm.contractAmount" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="合同标题" prop="contractTitle">
            <el-input v-model="createForm.contractTitle" placeholder="请输入合同标题" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="生效日期" prop="effectiveDate">
            <el-date-picker v-model="createForm.effectiveDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="到期日期" prop="expireDate">
            <el-date-picker v-model="createForm.expireDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">付款约定</el-divider>
      <el-table :data="createForm.payments" border size="small">
        <el-table-column label="阶段" prop="paymentStage" min-width="130">
          <template #default="{ row }"><el-input v-model="row.paymentStage" /></template>
        </el-table-column>
        <el-table-column label="比例" width="130">
          <template #default="{ row }"><el-input-number v-model="row.paymentRatio" :min="0.01" :precision="2" class="!w-1/1" /></template>
        </el-table-column>
        <el-table-column label="金额" width="150">
          <template #default="{ row }"><el-input-number v-model="row.paymentAmount" :min="0.01" :precision="2" class="!w-1/1" /></template>
        </el-table-column>
        <el-table-column label="应付日期" width="180">
          <template #default="{ row }"><el-date-picker v-model="row.dueDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" /></template>
        </el-table-column>
        <el-table-column label="说明" prop="paymentRemark" min-width="160">
          <template #default="{ row }"><el-input v-model="row.paymentRemark" /></template>
        </el-table-column>
      </el-table>
      <el-divider content-position="left">签署信息</el-divider>
      <el-table :data="createForm.signings" border size="small">
        <el-table-column label="签署方" min-width="130">
          <template #default="{ row }"><el-input v-model="row.signingParty" /></template>
        </el-table-column>
        <el-table-column label="签署人" min-width="130">
          <template #default="{ row }"><el-input v-model="row.signerName" /></template>
        </el-table-column>
        <el-table-column label="签署日期" width="180">
          <template #default="{ row }"><el-date-picker v-model="row.signingDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" /></template>
        </el-table-column>
        <el-table-column label="说明" min-width="160">
          <template #default="{ row }"><el-input v-model="row.signingRemark" /></template>
        </el-table-column>
      </el-table>
      <el-divider content-position="left">合同附件</el-divider>
      <el-table :data="createForm.attachments" border size="small">
        <el-table-column label="附件名称" min-width="150">
          <template #default="{ row }"><el-input v-model="row.attachmentName" /></template>
        </el-table-column>
        <el-table-column label="附件地址" min-width="260">
          <template #default="{ row }"><el-input v-model="row.attachmentUrl" /></template>
        </el-table-column>
        <el-table-column label="类型" width="150">
          <template #default="{ row }"><el-input v-model="row.attachmentType" /></template>
        </el-table-column>
      </el-table>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitCreate">保存合同</el-button>
    </template>
  </Dialog>

  <Dialog v-model="cancelVisible" title="作废采购合同" width="620px">
    <el-form ref="cancelFormRef" :model="cancelForm" :rules="cancelRules" label-width="90px">
      <el-form-item label="作废原因" prop="cancelReason">
        <el-input v-model="cancelForm.cancelReason" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="cancelVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitCancel">确认作废</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="采购合同详情" width="980px">
    <el-descriptions v-if="currentContract" :column="3" border>
      <el-descriptions-item label="合同编号">{{ currentContract.contractNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ currentContract.contractStatusLabel }}</el-descriptions-item>
      <el-descriptions-item label="来源">{{ currentContract.sourceTypeLabel }} / {{ currentContract.sourceNo }}</el-descriptions-item>
      <el-descriptions-item label="供应商">{{ currentContract.supplierName }}</el-descriptions-item>
      <el-descriptions-item label="合同金额">{{ currentContract.contractAmount }}</el-descriptions-item>
      <el-descriptions-item label="有效期">{{ currentContract.effectiveDate }} 至 {{ currentContract.expireDate }}</el-descriptions-item>
    </el-descriptions>
    <el-tabs class="mt-16px">
      <el-tab-pane label="付款约定">
        <el-table :data="currentContract?.payments || []" border size="small">
          <el-table-column label="阶段" prop="paymentStage" />
          <el-table-column label="比例" prop="paymentRatio" width="120" align="right" />
          <el-table-column label="金额" prop="paymentAmount" width="130" align="right" />
          <el-table-column label="应付日期" prop="dueDate" width="130" />
          <el-table-column label="说明" prop="paymentRemark" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="签署信息">
        <el-table :data="currentContract?.signings || []" border size="small">
          <el-table-column label="签署方" prop="signingParty" />
          <el-table-column label="签署人" prop="signerName" />
          <el-table-column label="签署日期" prop="signingDate" width="130" />
          <el-table-column label="说明" prop="signingRemark" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="附件">
        <el-table :data="currentContract?.attachments || []" border size="small">
          <el-table-column label="附件名称" prop="attachmentName" />
          <el-table-column label="附件地址" prop="attachmentUrl" min-width="260" />
          <el-table-column label="类型" prop="attachmentType" width="150" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import {
  SrmProcurementContractApi,
  srmProcurementContractSourceTypeOptions,
  srmProcurementContractStatusOptions,
  type SrmProcurementContractVO
} from '@/api/srm/procurement-contract'

defineOptions({ name: 'SrmProcurementContract' })

const message = useMessage()
const loading = ref(false)
const actionLoading = ref(false)
const list = ref<SrmProcurementContractVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  contractNo: undefined as string | undefined,
  contractTitle: undefined as string | undefined,
  sourceType: undefined as string | undefined,
  contractStatus: undefined as string | undefined
})

const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  sourceType: 'NON_BIDDING',
  sourceId: undefined as number | undefined,
  contractTitle: '',
  contractAmount: 1180,
  currency: 'CNY',
  effectiveDate: '',
  expireDate: '',
  payments: [
    {
      paymentStage: '预付款',
      paymentRatio: 30,
      paymentAmount: 354,
      dueDate: '',
      paymentRemark: '合同签署后支付'
    }
  ],
  signings: [
    {
      signingParty: '采购方',
      signerName: '采购负责人',
      signingDate: '',
      signingRemark: '线下签署'
    }
  ],
  attachments: [
    {
      attachmentName: '合同正文',
      attachmentUrl: 'http://127.0.0.1:9000/yudao/srm/contract/body.pdf',
      attachmentType: 'CONTRACT_FILE'
    }
  ]
})
const createRules = reactive<FormRules>({
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  sourceId: [{ required: true, message: '请输入来源 ID', trigger: 'change' }],
  contractTitle: [{ required: true, message: '请输入合同标题', trigger: 'blur' }],
  contractAmount: [{ required: true, message: '请输入合同金额', trigger: 'change' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  expireDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }]
})

const cancelVisible = ref(false)
const cancelFormRef = ref<FormInstance>()
const cancelForm = reactive({
  id: undefined as number | undefined,
  cancelReason: ''
})
const cancelRules = reactive<FormRules>({
  cancelReason: [{ required: true, message: '请输入作废原因', trigger: 'blur' }]
})

const detailVisible = ref(false)
const currentContract = ref<SrmProcurementContractVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'EFFECTIVE') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmProcurementContractApi.getContractPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购合同列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
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

const openCreateDialog = (sourceType?: string, sourceId?: number) => {
  createForm.sourceType = sourceType || 'NON_BIDDING'
  createForm.sourceId = sourceId
  createForm.contractTitle = '采购合同'
  const today = new Date()
  const nextMonth = new Date(today)
  nextMonth.setMonth(today.getMonth() + 6)
  createForm.effectiveDate = today.toISOString().slice(0, 10)
  createForm.expireDate = nextMonth.toISOString().slice(0, 10)
  createForm.payments[0].dueDate = createForm.effectiveDate
  createForm.signings[0].signingDate = createForm.effectiveDate
  createVisible.value = true
}

const submitCreate = async () => {
  await createFormRef.value?.validate()
  if (!createForm.sourceId) return
  if (createForm.expireDate < createForm.effectiveDate) {
    throw new Error('合同到期日期不能早于生效日期。')
  }
  actionLoading.value = true
  try {
    await SrmProcurementContractApi.createContract({
      sourceType: createForm.sourceType,
      sourceId: createForm.sourceId,
      contractTitle: createForm.contractTitle,
      contractAmount: createForm.contractAmount,
      currency: createForm.currency,
      effectiveDate: createForm.effectiveDate,
      expireDate: createForm.expireDate,
      payments: createForm.payments,
      signings: createForm.signings,
      attachments: createForm.attachments
    })
    createVisible.value = false
    message.success('采购合同已创建并回写来源项目')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购合同创建失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openCancelDialog = (row: SrmProcurementContractVO) => {
  cancelForm.id = row.id
  cancelForm.cancelReason = '合同作废，释放来源项目'
  cancelVisible.value = true
}

const submitCancel = async () => {
  await cancelFormRef.value?.validate()
  if (!cancelForm.id) return
  actionLoading.value = true
  try {
    await SrmProcurementContractApi.cancelContract({
      id: cancelForm.id,
      cancelReason: cancelForm.cancelReason
    })
    cancelVisible.value = false
    message.success('采购合同已作废，来源项目已恢复可建合同状态')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购合同作废失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const handleDelete = async (row: SrmProcurementContractVO) => {
  actionLoading.value = true
  try {
    await SrmProcurementContractApi.deleteContract(row.id)
    message.success('采购合同已删除，来源项目已恢复可建合同状态')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购合同删除失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openDetail = async (row: SrmProcurementContractVO) => {
  try {
    currentContract.value = await SrmProcurementContractApi.getContract(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购合同详情加载失败。'))
    throw error
  }
}

onMounted(() => {
  getList()
})
</script>
