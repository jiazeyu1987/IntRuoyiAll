<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="76px">
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="queryParams.planNo" clearable class="!w-210px" placeholder="请输入计划编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="计划标题" prop="planTitle">
        <el-input v-model="queryParams.planTitle" clearable class="!w-220px" placeholder="请输入计划标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="planStatus">
        <el-select v-model="queryParams.planStatus" clearable class="!w-150px" placeholder="全部">
          <el-option v-for="item in srmProcurementPlanStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openCreateDialog" v-hasPermi="['srm:procurement-plan:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增计划
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="计划编号" prop="planNo" width="170" />
      <el-table-column label="计划标题" prop="planTitle" min-width="180" />
      <el-table-column label="采购方式" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.procurementMethod === 'TENDER' ? 'warning' : 'info'">{{ row.procurementMethodLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.planStatus)">{{ row.planStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预计金额" prop="expectedAmount" width="120" align="right" />
      <el-table-column label="行数" width="80" align="center">
        <template #default="{ row }">{{ row.lines?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="生成项目" prop="generatedProjectNo" min-width="160" />
      <el-table-column label="提交人" prop="submittedName" width="110" />
      <el-table-column label="审核人" prop="auditName" width="110" />
      <el-table-column label="创建时间" prop="createTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" width="300" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" :disabled="!canSubmit(row)" @click="submitPlan(row)" v-hasPermi="['srm:procurement-plan:submit']">提交</el-button>
          <el-button link type="success" :disabled="row.planStatus !== 'SUBMITTED'" @click="openAuditDialog(row, 'approve')" v-hasPermi="['srm:procurement-plan:audit']">通过</el-button>
          <el-button link type="danger" :disabled="row.planStatus !== 'SUBMITTED'" @click="openAuditDialog(row, 'reject')" v-hasPermi="['srm:procurement-plan:audit']">驳回</el-button>
          <el-dropdown trigger="click" @command="(command) => generateProject(row, command as string)">
            <el-button link type="primary" :disabled="row.planStatus !== 'APPROVED'" v-hasPermi="['srm:procurement-plan:generate']">生成项目</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="NON_BIDDING">非招标项目</el-dropdown-item>
                <el-dropdown-item command="TENDER">招标项目</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="dialogVisible" title="新增采购计划" width="920px">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="92px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="计划标题" prop="planTitle">
            <el-input v-model="formData.planTitle" placeholder="请输入采购计划标题" />
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
          <el-form-item label="预计金额" prop="expectedAmount">
            <el-input-number v-model="formData.expectedAmount" :min="0" :precision="2" class="!w-1/1" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>
      <div class="line-toolbar">
        <span>计划行项目</span>
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
        <el-table-column label="需求日期" width="170">
          <template #default="{ row }"><el-date-picker v-model="row.requiredDate" value-format="YYYY-MM-DD" type="date" class="!w-1/1" /></template>
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

  <Dialog v-model="auditDialogVisible" :title="auditAction === 'approve' ? '通过采购计划' : '驳回采购计划'" width="520px">
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

  <Dialog v-model="detailVisible" title="采购计划详情" width="880px">
    <el-descriptions v-if="currentDetail" :column="3" border>
      <el-descriptions-item label="计划编号">{{ currentDetail.planNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ currentDetail.planStatusLabel }}</el-descriptions-item>
      <el-descriptions-item label="生成项目">{{ currentDetail.generatedProjectNo || '-' }}</el-descriptions-item>
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
import {
  SrmProcurementPlanApi,
  srmProcurementMethodOptions,
  srmProcurementPlanStatusOptions,
  type SrmProcurementPlanVO
} from '@/api/srm/procurement-plan'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SrmProcurementPlan' })

const message = useMessage()
const loading = ref(false)
const list = ref<SrmProcurementPlanVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  planNo: undefined as string | undefined,
  planTitle: undefined as string | undefined,
  planStatus: undefined as string | undefined
})

const defaultLine = () => ({
  materialId: 1,
  materialCode: '',
  materialName: '',
  quantity: 1,
  unit: '件',
  requiredDate: ''
})
const defaultFormData = (): SrmProcurementPlanVO => ({
  planTitle: '',
  procurementMethod: 'NON_BIDDING',
  expectedAmount: 0,
  remark: '',
  lines: [defaultLine()]
})
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<SrmProcurementPlanVO>(defaultFormData())
const formRules = reactive<FormRules>({
  planTitle: [{ required: true, message: '请输入计划标题', trigger: 'blur' }],
  procurementMethod: [{ required: true, message: '请选择采购方式', trigger: 'change' }],
  expectedAmount: [{ required: true, message: '请输入预计金额', trigger: 'change' }]
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
const currentDetail = ref<SrmProcurementPlanVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'APPROVED' || status === 'GENERATED') {
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

const canSubmit = (row: SrmProcurementPlanVO) => row.planStatus === 'DRAFT' || row.planStatus === 'REJECTED'

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmProcurementPlanApi.getProcurementPlanPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购计划列表加载失败，请检查后端接口。'))
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

const openCreateDialog = () => {
  Object.assign(formData, defaultFormData())
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
    await SrmProcurementPlanApi.createProcurementPlan(formData)
    dialogVisible.value = false
    message.success('采购计划已保存')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购计划保存失败，请检查填写内容。'))
    throw error
  } finally {
    formLoading.value = false
  }
}

const submitPlan = async (row: SrmProcurementPlanVO) => {
  if (!row.id) return
  try {
    await SrmProcurementPlanApi.submitProcurementPlan(row.id)
    message.success('采购计划已提交')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购计划提交失败。'))
    throw error
  }
}

const openAuditDialog = (row: SrmProcurementPlanVO, action: 'approve' | 'reject') => {
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
      await SrmProcurementPlanApi.approveProcurementPlan({ id: auditFormData.id, auditRemark: auditFormData.auditRemark })
      message.success('采购计划已通过')
    } else {
      await SrmProcurementPlanApi.rejectProcurementPlan({ id: auditFormData.id, auditRemark: auditFormData.auditRemark })
      message.success('采购计划已驳回')
    }
    auditDialogVisible.value = false
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购计划审核失败。'))
    throw error
  } finally {
    auditLoading.value = false
  }
}

const generateProject = async (row: SrmProcurementPlanVO, projectType: string) => {
  if (!row.id) return
  try {
    const project = await SrmProcurementPlanApi.generateSourcingProject({ id: row.id, projectType })
    message.success(`已生成${project.projectTypeLabel}：${project.projectNo}`)
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '生成寻源项目失败。'))
    throw error
  }
}

const openDetail = async (row: SrmProcurementPlanVO) => {
  if (!row.id) return
  try {
    currentDetail.value = await SrmProcurementPlanApi.getProcurementPlan(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购计划详情加载失败。'))
    throw error
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.line-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 10px;
  color: var(--el-text-color-regular);
  font-weight: 600;
}
</style>
