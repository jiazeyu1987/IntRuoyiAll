<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="76px"
    >
      <el-form-item label="供应商" prop="supplierName">
        <el-input
          v-model="queryParams.supplierName"
          clearable
          class="!w-220px"
          placeholder="请输入供应商名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="准入状态" prop="accessStatus">
        <el-select
          v-model="queryParams.accessStatus"
          clearable
          class="!w-160px"
          placeholder="全部"
        >
          <el-option
            v-for="item in srmSupplierAccessStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="启停状态" prop="enabled">
        <el-select v-model="queryParams.enabled" clearable class="!w-160px" placeholder="全部">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['srm:supplier-access:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增准入
        </el-button>
        <el-button
          type="warning"
          plain
          @click="openCheckDialog()"
          v-hasPermi="['srm:supplier-access:check']"
        >
          <Icon icon="ep:checked" class="mr-5px" /> 资格校验
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="supplier-access-overview">
      <div class="overview-card">
        <span class="overview-label">总档案</span>
        <strong class="overview-value">{{ total }}</strong>
      </div>
      <div class="overview-card">
        <span class="overview-label">已通过</span>
        <strong class="overview-value">{{ approvedCount }}</strong>
      </div>
      <div class="overview-card">
        <span class="overview-label">待审核</span>
        <strong class="overview-value">{{ pendingCount }}</strong>
      </div>
      <div class="overview-card overview-card--alert">
        <span class="overview-label">高风险阻断</span>
        <strong class="overview-value">{{ blockedByRiskCount }}</strong>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      row-key="id"
    >
      <el-table-column label="供应商" prop="supplierName" min-width="180" />
      <el-table-column label="门户联系人" prop="portalContactName" width="140" />
      <el-table-column label="联系电话" prop="portalContactPhone" width="150" />
      <el-table-column label="资质到期日" prop="qualificationExpireDate" width="130" />
      <el-table-column label="资质状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveQualificationTagType(row.qualificationStatusLabel)">
            {{ row.qualificationStatusLabel || '未登记' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="样品测试" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStageTagType(row.sampleTestStatus)">
            {{ row.sampleTestStatusLabel || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="小批试用" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStageTagType(row.trialOrderStatus)">
            {{ row.trialOrderStatusLabel || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="准入状态" min-width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveAccessTagType(row.accessStatus)">
            {{ row.accessStatusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启停状态" width="110" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            :loading="enableLoadingId === row.id"
            :disabled="row.accessStatus !== 'APPROVED'"
            @change="handleEnableChange(row)"
            v-hasPermi="['srm:supplier-access:enable']"
          />
        </template>
      </el-table-column>
      <el-table-column label="未处理高风险" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.openHighRiskCount ? 'danger' : 'success'">
            {{ row.openHighRiskCount || 0 }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="资格概览" min-width="140">
        <template #default="{ row }">
          <span :class="['eligibility-chip', `eligibility-chip--${resolveEligibilityTone(row)}`]">
            {{ row.eligibilitySummary || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="阶段概览" prop="onboardingStageSummary" min-width="140" />
      <el-table-column label="准入备注" prop="accessRemark" min-width="220" />
      <el-table-column label="提交人" prop="submittedName" width="110" />
      <el-table-column label="提交时间" prop="submittedTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="审核人" prop="auditName" width="110" />
      <el-table-column label="审核时间" prop="auditTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openForm('update', row)"
            v-hasPermi="['srm:supplier-access:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="openProfile(row)"
            v-hasPermi="['srm:supplier-profile:query']"
          >
            档案
          </el-button>
          <el-button
            link
            type="success"
            :disabled="!canApproveSample(row)"
            @click="openAuditDialog('approveSample', row)"
            v-hasPermi="['srm:supplier-access:audit']"
          >
            样品通过
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="!canRejectSample(row)"
            @click="openAuditDialog('rejectSample', row)"
            v-hasPermi="['srm:supplier-access:audit']"
          >
            样品驳回
          </el-button>
          <el-button
            link
            type="success"
            :disabled="!canApproveTrial(row)"
            @click="openAuditDialog('approveTrial', row)"
            v-hasPermi="['srm:supplier-access:audit']"
          >
            试用通过
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="!canRejectTrial(row)"
            @click="openAuditDialog('rejectTrial', row)"
            v-hasPermi="['srm:supplier-access:audit']"
          >
            试用驳回
          </el-button>
          <el-button
            link
            type="success"
            :disabled="row.accessStatus === 'APPROVED'"
            @click="openAuditDialog('approve', row)"
            v-hasPermi="['srm:supplier-access:audit']"
          >
            通过
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.accessStatus === 'REJECTED'"
            @click="openAuditDialog('reject', row)"
            v-hasPermi="['srm:supplier-access:audit']"
          >
            驳回
          </el-button>
          <el-button
            link
            type="warning"
            @click="openCheckDialog(row)"
            v-hasPermi="['srm:supplier-access:check']"
          >
            校验
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(row)"
            v-hasPermi="['srm:supplier-access:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <Dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="96px"
    >
      <el-form-item label="ERP供应商" prop="supplierId">
        <el-select
          v-model="formData.supplierId"
          filterable
          remote
          reserve-keyword
          class="!w-1/1"
          placeholder="请输入供应商名称检索"
          :remote-method="handleSupplierSearch"
          :loading="supplierOptionsLoading"
          :disabled="formType === 'update'"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="`${item.name} (#${item.id})`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="联系人" prop="portalContactName">
            <el-input
              v-model="formData.portalContactName"
              maxlength="64"
              placeholder="请输入门户联系人"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="portalContactPhone">
            <el-input
              v-model="formData.portalContactPhone"
              maxlength="32"
              placeholder="请输入联系人手机号或座机"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="资质到期日" prop="qualificationExpireDate">
        <el-date-picker
          v-model="formData.qualificationExpireDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择资质到期日"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="准入备注" prop="accessRemark">
        <el-input
          v-model="formData.accessRemark"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请填写准入背景、材料结论或限制说明"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
    </template>
  </Dialog>

  <Dialog v-model="auditDialogVisible" :title="auditDialogTitle" width="520px">
    <el-form
      ref="auditFormRef"
      v-loading="auditLoading"
      :model="auditFormData"
      :rules="auditFormRules"
      label-width="92px"
    >
      <el-form-item label="供应商">
        <el-input :model-value="auditFormData.supplierName" disabled />
      </el-form-item>
      <el-form-item label="审核备注" prop="auditRemark">
        <el-input
          v-model="auditFormData.auditRemark"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请填写审核意见，驳回时建议明确阻断原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="auditDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="auditLoading" @click="submitAudit">提交</el-button>
    </template>
  </Dialog>

  <Dialog v-model="checkDialogVisible" title="供应商资格校验" width="760px">
    <div class="check-panel">
      <el-form :inline="true" :model="checkFormData" label-width="76px">
        <el-form-item label="ERP供应商">
          <el-select
            v-model="checkFormData.supplierId"
            filterable
            remote
            reserve-keyword
            class="!w-320px"
            placeholder="请输入供应商名称检索"
            :remote-method="handleSupplierSearch"
            :loading="supplierOptionsLoading"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="`${item.name} (#${item.id})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="checkLoading" @click="submitEligibilityCheck">
            执行校验
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="eligibilityResult" class="eligibility-panel">
        <div class="eligibility-panel__header">
          <div>
            <div class="eligibility-title">
              {{ eligibilityResult.supplierName || '未命名供应商' }}
            </div>
            <div class="eligibility-subtitle">
              ERP 供应商编号 #{{ eligibilityResult.supplierId }}
            </div>
          </div>
          <el-tag :type="eligibilityResult.eligible ? 'success' : 'danger'" size="large">
            {{ eligibilityResult.eligible ? '校验通过' : '已阻断' }}
          </el-tag>
        </div>
        <div class="eligibility-grid">
          <div class="eligibility-item">
            <span class="eligibility-item__label">准入状态</span>
            <span>{{ eligibilityResult.accessStatusLabel || '-' }}</span>
          </div>
          <div class="eligibility-item">
            <span class="eligibility-item__label">启停状态</span>
            <span>
              {{
                eligibilityResult.enabled === false
                  ? '停用'
                  : eligibilityResult.enabled === true
                    ? '启用'
                    : '-'
              }}
            </span>
          </div>
          <div class="eligibility-item">
            <span class="eligibility-item__label">未处理高风险</span>
            <span>{{ eligibilityResult.openHighRiskCount || 0 }}</span>
          </div>
          <div class="eligibility-item">
            <span class="eligibility-item__label">校验时间</span>
            <span>{{ eligibilityResult.checkedTime || '-' }}</span>
          </div>
        </div>
        <el-alert
          v-if="eligibilityResult.blockedReason"
          type="error"
          :closable="false"
          :title="eligibilityResult.blockedReason"
          class="mt-16px"
        />
        <div v-if="eligibilityResult.openHighRiskSources?.length" class="mt-16px">
          <div class="eligibility-item__label">风险来源</div>
          <ul class="risk-source-list">
            <li v-for="item in eligibilityResult.openHighRiskSources" :key="item">
              {{ item }}
            </li>
          </ul>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="checkDialogVisible = false">关闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  SrmSupplierAccessApi,
  srmSupplierAccessStatusOptions,
  type SrmSupplierAccessVO,
  type SrmSupplierEligibilityVO,
  type SrmSupplierReferenceVO
} from '@/api/srm/supplier-access'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SrmSupplierAccess' })

const message = useMessage()
const router = useRouter()

const loading = ref(false)
const list = ref<SrmSupplierAccessVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  supplierName: undefined as string | undefined,
  accessStatus: undefined as string | undefined,
  enabled: undefined as boolean | undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formType = ref<'create' | 'update'>('create')
const formLoading = ref(false)
const formRef = ref<FormInstance>()

const auditDialogVisible = ref(false)
const auditDialogTitle = ref('')
const auditAction = ref<'approve' | 'reject' | 'approveSample' | 'rejectSample' | 'approveTrial' | 'rejectTrial'>('approve')
const auditLoading = ref(false)
const auditFormRef = ref<FormInstance>()

const checkDialogVisible = ref(false)
const checkLoading = ref(false)
const eligibilityResult = ref<SrmSupplierEligibilityVO>()

const supplierOptions = ref<SrmSupplierReferenceVO[]>([])
const supplierOptionsLoading = ref(false)
const enableLoadingId = ref<number>()

const defaultFormData = (): SrmSupplierAccessVO => ({
  supplierId: undefined as unknown as number,
  portalContactName: '',
  portalContactPhone: '',
  qualificationExpireDate: '',
  accessRemark: ''
})
const formData = reactive<SrmSupplierAccessVO>(defaultFormData())
const formRules = reactive<FormRules>({
  supplierId: [{ required: true, message: '请选择 ERP 供应商', trigger: 'change' }],
  portalContactName: [{ required: true, message: '请输入门户联系人', trigger: 'blur' }],
  portalContactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  qualificationExpireDate: [{ required: true, message: '请选择资质到期日', trigger: 'change' }]
})

const auditFormData = reactive({
  id: undefined as number | undefined,
  supplierName: '',
  auditRemark: ''
})
const auditFormRules = reactive<FormRules>({
  auditRemark: [{ required: true, message: '请填写审核备注', trigger: 'blur' }]
})

const checkFormData = reactive({
  supplierId: undefined as number | undefined
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const approvedCount = computed(
  () => list.value.filter((item) => item.accessStatus === 'APPROVED').length
)
const pendingCount = computed(
  () => list.value.filter((item) => item.accessStatus === 'PENDING').length
)
const blockedByRiskCount = computed(
  () => list.value.filter((item) => Number(item.openHighRiskCount || 0) > 0).length
)

const resolveAccessTagType = (status?: string) => {
  if (status === 'APPROVED') {
    return 'success'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  return 'warning'
}

const resolveStageTagType = (status?: string) => {
  if (status === 'PASSED') {
    return 'success'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  if (status === 'PENDING') {
    return 'warning'
  }
  return 'info'
}

const resolveQualificationTagType = (status?: string) => {
  if (status === '有效') {
    return 'success'
  }
  if (status === '已过期') {
    return 'danger'
  }
  if (status === '待更新') {
    return 'warning'
  }
  return 'info'
}

const resolveEligibilityTone = (row: SrmSupplierAccessVO) => {
  if (Number(row.openHighRiskCount || 0) > 0) {
    return 'danger'
  }
  if (row.sampleTestStatus === 'REJECTED' || row.trialOrderStatus === 'REJECTED') {
    return 'danger'
  }
  if (row.sampleTestStatus === 'PENDING' || row.trialOrderStatus === 'PENDING') {
    return 'warning'
  }
  if (row.qualificationStatusLabel === '已过期') {
    return 'danger'
  }
  if (row.qualificationStatusLabel === '待更新') {
    return 'warning'
  }
  if (row.accessStatus === 'APPROVED' && row.enabled !== false) {
    return 'success'
  }
  if (row.enabled === false) {
    return 'muted'
  }
  return 'warning'
}

const canApproveSample = (row: SrmSupplierAccessVO) =>
  row.accessStatus === 'PENDING' && row.sampleTestStatus !== 'PASSED'

const canRejectSample = (row: SrmSupplierAccessVO) =>
  row.accessStatus === 'PENDING' && row.sampleTestStatus !== 'REJECTED'

const canApproveTrial = (row: SrmSupplierAccessVO) =>
  row.accessStatus === 'PENDING' &&
  row.sampleTestStatus === 'PASSED' &&
  row.trialOrderStatus !== 'PASSED'

const canRejectTrial = (row: SrmSupplierAccessVO) =>
  row.accessStatus === 'PENDING' &&
  row.sampleTestStatus === 'PASSED' &&
  row.trialOrderStatus !== 'REJECTED'

const openProfile = (row: SrmSupplierAccessVO) => {
  if (!row.supplierId) {
    message.error('???????????????????')
    return
  }
  router.push({
    path: '/srm/supplier/profile',
    query: { supplierId: String(row.supplierId) }
  })
}

const resetFormData = () => {
  Object.assign(formData, defaultFormData())
}

const upsertSupplierOptions = (items: SrmSupplierReferenceVO[]) => {
  const next = new Map<number, SrmSupplierReferenceVO>()
  supplierOptions.value.forEach((item) => next.set(item.id, item))
  items.forEach((item) => next.set(item.id, item))
  supplierOptions.value = Array.from(next.values())
}

const loadReferenceSuppliers = async (keyword?: string) => {
  supplierOptionsLoading.value = true
  try {
    const data = await SrmSupplierAccessApi.getReferenceSuppliers(keyword)
    upsertSupplierOptions(data || [])
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商引用列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    supplierOptionsLoading.value = false
  }
}

const handleSupplierSearch = (keyword: string) => {
  loadReferenceSuppliers(keyword)
}

const ensureSupplierOption = (row: SrmSupplierAccessVO) => {
  if (row.supplierId && row.supplierName) {
    upsertSupplierOptions([{ id: row.supplierId, name: row.supplierName }])
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmSupplierAccessApi.getSupplierAccessPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商准入列表加载失败，请检查后端接口。'))
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

const openForm = async (type: 'create' | 'update', row?: SrmSupplierAccessVO) => {
  formType.value = type
  dialogTitle.value = type === 'create' ? '新增供应商准入' : '编辑供应商准入'
  resetFormData()
  await loadReferenceSuppliers('')
  if (type === 'update' && row) {
    ensureSupplierOption(row)
    Object.assign(formData, {
      id: row.id,
      supplierId: row.supplierId,
      portalContactName: row.portalContactName || '',
      portalContactPhone: row.portalContactPhone || '',
      qualificationExpireDate: row.qualificationExpireDate || '',
      accessRemark: row.accessRemark || ''
    })
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await SrmSupplierAccessApi.createSupplierAccess(formData)
      message.success('供应商准入档案已新增')
    } else {
      await SrmSupplierAccessApi.updateSupplierAccess(formData)
      message.success('供应商准入档案已更新')
    }
    dialogVisible.value = false
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商准入保存失败，请检查填写内容和后端接口。'))
    throw error
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (row: SrmSupplierAccessVO) => {
  if (!row.id) {
    message.error('当前准入档案缺少编号，无法删除。')
    return
  }
  formLoading.value = true
  try {
    await SrmSupplierAccessApi.deleteSupplierAccess(row.id)
    message.success('供应商准入档案已删除')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商准入删除失败，请检查后端接口。'))
    throw error
  } finally {
    formLoading.value = false
  }
}

const openAuditDialog = (
  action: 'approve' | 'reject' | 'approveSample' | 'rejectSample' | 'approveTrial' | 'rejectTrial',
  row: SrmSupplierAccessVO
) => {
  if (!row.id) {
    message.error('当前准入档案缺少编号，无法提交审核。')
    return
  }
  auditAction.value = action
  auditDialogTitle.value =
    action === 'approve'
      ? '通过准入审核'
      : action === 'reject'
        ? '驳回准入审核'
        : action === 'approveSample'
          ? '通过样品测试'
          : action === 'rejectSample'
            ? '驳回样品测试'
            : action === 'approveTrial'
              ? '通过小批试用'
              : '驳回小批试用'
  Object.assign(auditFormData, {
    id: row.id,
    supplierName: row.supplierName || '',
    auditRemark: ''
  })
  auditDialogVisible.value = true
}

const submitAudit = async () => {
  await auditFormRef.value?.validate()
  if (!auditFormData.id) {
    message.error('审核档案编号缺失，无法继续。')
    return
  }
  auditLoading.value = true
  try {
    const payload = { id: auditFormData.id, auditRemark: auditFormData.auditRemark }
    if (auditAction.value === 'approve') {
      await SrmSupplierAccessApi.approveSupplierAccess(payload)
      message.success('供应商准入已通过')
    } else if (auditAction.value === 'reject') {
      await SrmSupplierAccessApi.rejectSupplierAccess(payload)
      message.success('供应商准入已驳回')
    } else if (auditAction.value === 'approveSample') {
      await SrmSupplierAccessApi.approveSampleTest(payload)
      message.success('样品测试已通过')
    } else if (auditAction.value === 'rejectSample') {
      await SrmSupplierAccessApi.rejectSampleTest(payload)
      message.success('样品测试已驳回')
    } else if (auditAction.value === 'approveTrial') {
      await SrmSupplierAccessApi.approveTrialOrder(payload)
      message.success('小批试用已通过')
    } else {
      await SrmSupplierAccessApi.rejectTrialOrder(payload)
      message.success('小批试用已驳回')
    }
    auditDialogVisible.value = false
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商准入审核失败，请检查后端接口。'))
    throw error
  } finally {
    auditLoading.value = false
  }
}

const handleEnableChange = async (row: SrmSupplierAccessVO) => {
  if (!row.id) {
    message.error('供应商准入档案缺少编号，无法更新启停状态。')
    row.enabled = !row.enabled
    return
  }
  const nextEnabled = Boolean(row.enabled)
  enableLoadingId.value = row.id
  try {
    await SrmSupplierAccessApi.enableSupplierAccess({
      id: row.id,
      enabled: nextEnabled,
      operationRemark: nextEnabled ? '页面恢复供应商准入' : '页面停用供应商准入'
    })
    message.success(nextEnabled ? '供应商准入已启用' : '供应商准入已停用')
    await getList()
  } catch (error) {
    row.enabled = !nextEnabled
    message.error(resolveErrorMessage(error, '供应商准入启停失败，请检查后端接口。'))
    throw error
  } finally {
    enableLoadingId.value = undefined
  }
}

const openCheckDialog = async (row?: SrmSupplierAccessVO) => {
  eligibilityResult.value = undefined
  await loadReferenceSuppliers('')
  if (row?.supplierId) {
    ensureSupplierOption(row)
    checkFormData.supplierId = row.supplierId
  } else {
    checkFormData.supplierId = undefined
  }
  checkDialogVisible.value = true
}

const submitEligibilityCheck = async () => {
  if (!checkFormData.supplierId) {
    message.error('请选择要校验的 ERP 供应商。')
    return
  }
  checkLoading.value = true
  try {
    eligibilityResult.value = await SrmSupplierAccessApi.checkSupplierEligibility(
      checkFormData.supplierId
    )
    message.success(eligibilityResult.value.eligible ? '资格校验通过' : '资格校验已阻断')
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商资格校验失败，请检查后端接口。'))
    throw error
  } finally {
    checkLoading.value = false
  }
}

onMounted(() => {
  loadReferenceSuppliers('')
  getList()
})
</script>

<style scoped lang="scss">
.supplier-access-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.overview-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f7f9fc 100%);
}

.overview-card--alert {
  background: linear-gradient(135deg, #fff5f5 0%, #fff0f0 100%);
}

.overview-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.overview-value {
  font-size: 24px;
  line-height: 1;
  color: var(--el-text-color-primary);
}

.eligibility-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.eligibility-chip--success {
  color: #16794d;
  background: #e8f7ee;
}

.eligibility-chip--warning {
  color: #8a5a00;
  background: #fff3d6;
}

.eligibility-chip--danger {
  color: #a12c2c;
  background: #fde8e8;
}

.eligibility-chip--muted {
  color: #596273;
  background: #eef1f5;
}

.check-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.eligibility-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  padding: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.eligibility-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.eligibility-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.eligibility-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.eligibility-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.eligibility-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
}

.eligibility-item__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.risk-source-list {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
}

@media (max-width: 992px) {
  .supplier-access-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .eligibility-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .supplier-access-overview {
    grid-template-columns: 1fr;
  }

  .eligibility-panel__header {
    flex-direction: column;
  }
}
</style>
