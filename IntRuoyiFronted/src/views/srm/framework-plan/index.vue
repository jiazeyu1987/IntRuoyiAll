<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="76px">
      <el-form-item label="计划编号" prop="frameworkPlanNo">
        <el-input v-model="queryParams.frameworkPlanNo" clearable class="!w-210px" placeholder="请输入框架计划编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierName">
        <el-input v-model="queryParams.supplierName" clearable class="!w-220px" placeholder="请输入供应商名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="planStatus">
        <el-select v-model="queryParams.planStatus" clearable class="!w-150px" placeholder="全部">
          <el-option v-for="item in srmFrameworkPlanStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openCreateDialog" v-hasPermi="['srm:framework-plan:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增框架计划
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="计划编号" prop="frameworkPlanNo" width="170" />
      <el-table-column label="计划标题" prop="planTitle" min-width="180" />
      <el-table-column label="供应商" prop="supplierName" min-width="180" />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.planStatus)">{{ row.planStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预算金额" prop="budgetAmount" width="120" align="right" />
      <el-table-column label="有效期" min-width="210">
        <template #default="{ row }">{{ row.validStartDate }} 至 {{ row.validEndDate }}</template>
      </el-table-column>
      <el-table-column label="协议编号" prop="agreementNo" min-width="160" />
      <el-table-column label="审核人" prop="auditName" width="110" />
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" :disabled="!canSubmit(row)" @click="submitPlan(row)" v-hasPermi="['srm:framework-plan:submit']">提交</el-button>
          <el-button link type="success" :disabled="row.planStatus !== 'SUBMITTED'" @click="openAuditDialog(row, 'approve')" v-hasPermi="['srm:framework-plan:audit']">通过</el-button>
          <el-button link type="danger" :disabled="row.planStatus !== 'SUBMITTED'" @click="openAuditDialog(row, 'reject')" v-hasPermi="['srm:framework-plan:audit']">驳回</el-button>
          <el-button link type="primary" :disabled="row.planStatus !== 'APPROVED'" @click="createAgreement(row)" v-hasPermi="['srm:framework-plan:agreement']">生成协议</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <ContentWrap>
    <div class="agreement-header">
      <span>框架协议</span>
      <el-button link type="primary" @click="getAgreementList"><Icon icon="ep:refresh" class="mr-5px" /> 刷新协议</el-button>
    </div>
    <el-table v-loading="agreementLoading" :data="agreementList" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="协议编号" prop="agreementNo" width="170" />
      <el-table-column label="来源计划" prop="frameworkPlanNo" width="170" />
      <el-table-column label="供应商" prop="supplierName" min-width="180" />
      <el-table-column label="采购方式" prop="procurementMethodLabel" width="110" />
      <el-table-column label="预算金额" prop="budgetAmount" width="120" align="right" />
      <el-table-column label="状态" prop="agreementStatusLabel" width="110" align="center" />
      <el-table-column label="物料行数" width="90" align="center">
        <template #default="{ row }">{{ row.lines?.length || 0 }}</template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <Dialog v-model="dialogVisible" title="新增框架计划" width="940px">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="98px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="计划标题" prop="planTitle">
            <el-input v-model="formData.planTitle" placeholder="请输入框架计划标题" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="合格供应商" prop="supplierId">
            <el-select
              v-model="formData.supplierId"
              filterable
              remote
              reserve-keyword
              class="!w-1/1"
              placeholder="请输入供应商名称检索"
              :remote-method="loadSupplierOptions"
              :loading="supplierLoading"
            >
              <el-option v-for="item in supplierOptions" :key="item.id" :label="`${item.name} (#${item.id})`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="采购方式" prop="procurementMethod">
            <el-select v-model="formData.procurementMethod" class="!w-1/1">
              <el-option v-for="item in srmProcurementMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="预算金额" prop="budgetAmount">
            <el-input-number v-model="formData.budgetAmount" :min="0" :precision="2" class="!w-1/1" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="开始日期" prop="validStartDate">
            <el-date-picker v-model="formData.validStartDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="结束日期" prop="validEndDate">
            <el-date-picker v-model="formData.validEndDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" />
          </el-form-item>
        </el-col>
      </el-row>
      <div class="line-toolbar">
        <span>框架物料行</span>
        <el-button link type="primary" @click="addLine"><Icon icon="ep:plus" class="mr-5px" /> 添加行</el-button>
      </div>
      <el-table :data="formData.lines" border size="small">
        <el-table-column label="物料编码" min-width="130">
          <template #default="{ row }"><el-input v-model="row.materialCode" placeholder="物料编码" /></template>
        </el-table-column>
        <el-table-column label="物料名称" min-width="160">
          <template #default="{ row }"><el-input v-model="row.materialName" placeholder="物料名称" /></template>
        </el-table-column>
        <el-table-column label="物料ID" width="120">
          <template #default="{ row }"><el-input-number v-model="row.materialId" :min="1" class="!w-1/1" controls-position="right" /></template>
        </el-table-column>
        <el-table-column label="数量" width="130">
          <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" /></template>
        </el-table-column>
        <el-table-column label="单位" width="100">
          <template #default="{ row }"><el-input v-model="row.unit" placeholder="单位" /></template>
        </el-table-column>
        <el-table-column label="预算" width="130">
          <template #default="{ row }"><el-input-number v-model="row.budgetAmount" :min="0" :precision="2" class="!w-1/1" controls-position="right" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="formData.lines.length === 1" @click="removeLine($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
    </template>
  </Dialog>

  <Dialog v-model="auditDialogVisible" :title="auditAction === 'approve' ? '通过框架计划' : '驳回框架计划'" width="520px">
    <el-form ref="auditFormRef" :model="auditFormData" :rules="auditRules" label-width="86px">
      <el-form-item label="审核意见" prop="auditRemark">
        <el-input v-model="auditFormData.auditRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="auditDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="auditLoading" @click="submitAudit">提交</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="框架计划详情" width="880px">
    <el-descriptions v-if="currentDetail" :column="3" border>
      <el-descriptions-item label="计划编号">{{ currentDetail.frameworkPlanNo }}</el-descriptions-item>
      <el-descriptions-item label="供应商">{{ currentDetail.supplierName }}</el-descriptions-item>
      <el-descriptions-item label="协议编号">{{ currentDetail.agreementNo || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-table class="mt-16px" :data="currentDetail?.approvalRecords || []" border size="small">
      <el-table-column label="动作" prop="actionLabel" width="90" />
      <el-table-column label="操作人" prop="operatorName" width="120" />
      <el-table-column label="时间" prop="operationTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="意见" prop="remark" />
    </el-table>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import { SrmSupplierAccessApi, type SrmSupplierReferenceVO } from '@/api/srm/supplier-access'
import {
  SrmFrameworkPlanApi,
  srmFrameworkPlanStatusOptions,
  type SrmFrameworkAgreementVO,
  type SrmFrameworkPlanVO
} from '@/api/srm/framework-plan'
import { srmProcurementMethodOptions } from '@/api/srm/procurement-plan'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SrmFrameworkPlan' })

const message = useMessage()
const loading = ref(false)
const list = ref<SrmFrameworkPlanVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  frameworkPlanNo: undefined as string | undefined,
  supplierName: undefined as string | undefined,
  planStatus: undefined as string | undefined
})

const agreementLoading = ref(false)
const agreementList = ref<SrmFrameworkAgreementVO[]>([])

const supplierOptions = ref<SrmSupplierReferenceVO[]>([])
const supplierLoading = ref(false)
const defaultLine = () => ({
  materialId: 1,
  materialCode: '',
  materialName: '',
  quantity: 1,
  unit: '件',
  budgetAmount: 0
})
const defaultFormData = (): SrmFrameworkPlanVO => ({
  planTitle: '',
  supplierId: undefined as unknown as number,
  procurementMethod: 'NON_BIDDING',
  budgetAmount: 0,
  validStartDate: '',
  validEndDate: '',
  lines: [defaultLine()]
})

const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<SrmFrameworkPlanVO>(defaultFormData())
const formRules = reactive<FormRules>({
  planTitle: [{ required: true, message: '请输入框架计划标题', trigger: 'blur' }],
  supplierId: [{ required: true, message: '请选择合格供应商', trigger: 'change' }],
  budgetAmount: [{ required: true, message: '请输入预算金额', trigger: 'change' }],
  validStartDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  validEndDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
})

const auditDialogVisible = ref(false)
const auditLoading = ref(false)
const auditAction = ref<'approve' | 'reject'>('approve')
const auditFormRef = ref<FormInstance>()
const auditFormData = reactive({ id: undefined as number | undefined, auditRemark: '' })
const auditRules = computed<FormRules>(() => ({
  auditRemark: auditAction.value === 'reject' ? [{ required: true, message: '请填写驳回意见', trigger: 'blur' }] : []
}))

const detailVisible = ref(false)
const currentDetail = ref<SrmFrameworkPlanVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'APPROVED' || status === 'AGREEMENT_CREATED') {
    return 'success'
  }
  if (status === 'SUBMITTED') {
    return 'warning'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  return 'info'
}

const canSubmit = (row: SrmFrameworkPlanVO) => row.planStatus === 'DRAFT' || row.planStatus === 'REJECTED'

const loadSupplierOptions = async (keyword?: string) => {
  supplierLoading.value = true
  try {
    supplierOptions.value = await SrmSupplierAccessApi.getReferenceSuppliers(keyword)
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商引用列表加载失败，请检查准入数据。'))
    throw error
  } finally {
    supplierLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmFrameworkPlanApi.getFrameworkPlanPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '框架计划列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const getAgreementList = async () => {
  agreementLoading.value = true
  try {
    const data = await SrmFrameworkPlanApi.getAgreementPage({ pageNo: 1, pageSize: 20 })
    agreementList.value = data.list || []
  } catch (error) {
    message.error(resolveErrorMessage(error, '框架协议列表加载失败。'))
    throw error
  } finally {
    agreementLoading.value = false
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

const openCreateDialog = async () => {
  Object.assign(formData, defaultFormData())
  await loadSupplierOptions('')
  dialogVisible.value = true
}

const addLine = () => {
  formData.lines.push(defaultLine())
}

const removeLine = (index: number) => {
  formData.lines.splice(index, 1)
}

const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    await SrmFrameworkPlanApi.createFrameworkPlan(formData)
    dialogVisible.value = false
    message.success('框架计划已保存')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '框架计划保存失败，请确认供应商已准入且无高风险。'))
    throw error
  } finally {
    formLoading.value = false
  }
}

const submitPlan = async (row: SrmFrameworkPlanVO) => {
  if (!row.id) return
  try {
    await SrmFrameworkPlanApi.submitFrameworkPlan(row.id)
    message.success('框架计划已提交')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '框架计划提交失败。'))
    throw error
  }
}

const openAuditDialog = (row: SrmFrameworkPlanVO, action: 'approve' | 'reject') => {
  auditAction.value = action
  auditFormData.id = row.id
  auditFormData.auditRemark = ''
  auditDialogVisible.value = true
}

const submitAudit = async () => {
  await auditFormRef.value?.validate()
  if (!auditFormData.id) return
  auditLoading.value = true
  try {
    if (auditAction.value === 'approve') {
      await SrmFrameworkPlanApi.approveFrameworkPlan({ id: auditFormData.id, auditRemark: auditFormData.auditRemark })
      message.success('框架计划已通过')
    } else {
      await SrmFrameworkPlanApi.rejectFrameworkPlan({ id: auditFormData.id, auditRemark: auditFormData.auditRemark })
      message.success('框架计划已驳回')
    }
    auditDialogVisible.value = false
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '框架计划审核失败。'))
    throw error
  } finally {
    auditLoading.value = false
  }
}

const createAgreement = async (row: SrmFrameworkPlanVO) => {
  if (!row.id) return
  try {
    const agreement = await SrmFrameworkPlanApi.createAgreement(row.id)
    message.success(`已生成框架协议：${agreement.agreementNo}`)
    await getList()
    await getAgreementList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '生成框架协议失败。'))
    throw error
  }
}

const openDetail = async (row: SrmFrameworkPlanVO) => {
  if (!row.id) return
  try {
    currentDetail.value = await SrmFrameworkPlanApi.getFrameworkPlan(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '框架计划详情加载失败。'))
    throw error
  }
}

onMounted(() => {
  getList()
  getAgreementList()
})
</script>

<style scoped lang="scss">
.agreement-header,
.line-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 10px;
  color: var(--el-text-color-regular);
  font-weight: 600;
}
</style>
