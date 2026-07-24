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
      <el-form-item label="风险等级" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" clearable class="!w-160px" placeholder="全部">
          <el-option
            v-for="item in srmSupplierRiskLevelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="riskStatus">
        <el-select v-model="queryParams.riskStatus" clearable class="!w-160px" placeholder="全部">
          <el-option
            v-for="item in srmSupplierRiskStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openCreateDialog()"
          v-hasPermi="['srm:supplier-risk:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增风险
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="risk-overview">
      <div class="risk-card">
        <span class="risk-card__label">风险总数</span>
        <strong class="risk-card__value">{{ total }}</strong>
      </div>
      <div class="risk-card risk-card--danger">
        <span class="risk-card__label">未处理高风险</span>
        <strong class="risk-card__value">{{ openHighRiskCount }}</strong>
      </div>
      <div class="risk-card">
        <span class="risk-card__label">处理中断来源</span>
        <strong class="risk-card__value">{{ distinctSourceCount }}</strong>
      </div>
      <div class="risk-card risk-card--success">
        <span class="risk-card__label">已处理</span>
        <strong class="risk-card__value">{{ resolvedCount }}</strong>
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
      <el-table-column label="风险等级" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveRiskLevelType(row.riskLevel)">
            {{ row.riskLevelLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.riskStatus === 'RESOLVED' ? 'success' : 'danger'">
            {{ row.riskStatusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源类型" prop="sourceTypeLabel" width="120" />
      <el-table-column label="来源编号" prop="sourceId" width="110" />
      <el-table-column label="来源编码" prop="sourceCode" min-width="140" />
      <el-table-column label="来源名称" prop="sourceName" min-width="180" />
      <el-table-column label="风险描述" prop="riskDescription" min-width="220" />
      <el-table-column label="风险备注" prop="riskRemark" min-width="180" />
      <el-table-column label="上报人" prop="reportedName" width="110" />
      <el-table-column label="上报时间" prop="reportedTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="处理人" prop="resolvedName" width="110" />
      <el-table-column label="处理时间" prop="resolvedTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="处理说明" prop="resolutionRemark" min-width="180" />
      <el-table-column label="操作" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.riskStatus === 'RESOLVED'"
            @click="openResolveDialog(row)"
            v-hasPermi="['srm:supplier-risk:resolve']"
          >
            处理
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

  <Dialog v-model="dialogVisible" title="新增供应商风险" width="840px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="98px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
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
            >
              <el-option
                v-for="item in supplierOptions"
                :key="item.id"
                :label="`${item.name} (#${item.id})`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="准入档案编号" prop="supplierAccessId">
            <el-input-number
              v-model="formData.supplierAccessId"
              :min="1"
              controls-position="right"
              class="!w-1/1"
              placeholder="可选"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="风险等级" prop="riskLevel">
            <el-select v-model="formData.riskLevel" class="!w-1/1" placeholder="请选择风险等级">
              <el-option
                v-for="item in srmSupplierRiskLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源类型" prop="sourceType">
            <el-select v-model="formData.sourceType" class="!w-1/1" placeholder="请选择来源类型">
              <el-option
                v-for="item in srmSupplierRiskSourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源编号" prop="sourceId">
            <el-input-number
              v-model="formData.sourceId"
              :min="1"
              controls-position="right"
              class="!w-1/1"
              placeholder="可选"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源编码" prop="sourceCode">
            <el-input v-model="formData.sourceCode" placeholder="例如 ACCESS-122-001" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="来源名称" prop="sourceName">
            <el-input v-model="formData.sourceName" placeholder="请输入来源名称，便于页面追溯" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="风险描述" prop="riskDescription">
            <el-input
              v-model="formData.riskDescription"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="请填写阻断原因，例如资质文件过期、黑名单命中等"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="风险备注" prop="riskRemark">
            <el-input
              v-model="formData.riskRemark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="可填写补充动作、责任人或时限"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
    </template>
  </Dialog>

  <Dialog v-model="resolveDialogVisible" title="处理供应商风险" width="560px">
    <el-form
      ref="resolveFormRef"
      v-loading="resolveLoading"
      :model="resolveFormData"
      :rules="resolveFormRules"
      label-width="92px"
    >
      <el-form-item label="供应商">
        <el-input :model-value="resolveFormData.supplierName" disabled />
      </el-form-item>
      <el-form-item label="风险描述">
        <el-input :model-value="resolveFormData.riskDescription" type="textarea" :rows="3" disabled />
      </el-form-item>
      <el-form-item label="处理说明" prop="resolutionRemark">
        <el-input
          v-model="resolveFormData.resolutionRemark"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请填写处理动作、复核结论和恢复准入依据"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="resolveDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="resolveLoading" @click="submitResolve">提交</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import { SrmSupplierAccessApi, type SrmSupplierReferenceVO } from '@/api/srm/supplier-access'
import {
  SrmSupplierRiskApi,
  srmSupplierRiskLevelOptions,
  srmSupplierRiskSourceTypeOptions,
  srmSupplierRiskStatusOptions,
  type SrmSupplierRiskVO
} from '@/api/srm/supplier-risk'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SrmSupplierRisk' })

const message = useMessage()

const loading = ref(false)
const list = ref<SrmSupplierRiskVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  supplierName: undefined as string | undefined,
  riskLevel: undefined as string | undefined,
  riskStatus: undefined as string | undefined
})

const supplierOptions = ref<SrmSupplierReferenceVO[]>([])
const supplierOptionsLoading = ref(false)

const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()

const resolveDialogVisible = ref(false)
const resolveLoading = ref(false)
const resolveFormRef = ref<FormInstance>()

const defaultFormData = (): SrmSupplierRiskVO => ({
  supplierId: undefined as unknown as number,
  supplierAccessId: undefined,
  riskLevel: 'HIGH',
  sourceType: 'ACCESS_REQUEST',
  sourceId: undefined,
  sourceCode: '',
  sourceName: '',
  riskDescription: '',
  riskRemark: ''
})
const formData = reactive<SrmSupplierRiskVO>(defaultFormData())
const formRules = reactive<FormRules>({
  supplierId: [{ required: true, message: '请选择 ERP 供应商', trigger: 'change' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  riskDescription: [{ required: true, message: '请填写风险描述', trigger: 'blur' }]
})

const resolveFormData = reactive({
  id: undefined as number | undefined,
  supplierName: '',
  riskDescription: '',
  resolutionRemark: ''
})
const resolveFormRules = reactive<FormRules>({
  resolutionRemark: [{ required: true, message: '请填写处理说明', trigger: 'blur' }]
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const openHighRiskCount = computed(
  () =>
    list.value.filter((item) => item.riskStatus === 'OPEN' && item.riskLevel === 'HIGH').length
)
const resolvedCount = computed(
  () => list.value.filter((item) => item.riskStatus === 'RESOLVED').length
)
const distinctSourceCount = computed(
  () =>
    new Set(
      list.value
        .filter((item) => item.riskStatus === 'OPEN')
        .map((item) => `${item.sourceType || ''}-${item.sourceId || ''}-${item.sourceCode || ''}`)
    ).size
)

const resolveRiskLevelType = (riskLevel?: string) => {
  if (riskLevel === 'HIGH') {
    return 'danger'
  }
  if (riskLevel === 'MEDIUM') {
    return 'warning'
  }
  return 'info'
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

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmSupplierRiskApi.getSupplierRiskPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商风险列表加载失败，请检查后端接口。'))
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

const openCreateDialog = async () => {
  resetFormData()
  await loadReferenceSuppliers('')
  dialogVisible.value = true
}

const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    await SrmSupplierRiskApi.createSupplierRisk(formData)
    dialogVisible.value = false
    message.success('供应商风险记录已新增')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商风险保存失败，请检查填写内容和后端接口。'))
    throw error
  } finally {
    formLoading.value = false
  }
}

const openResolveDialog = (row: SrmSupplierRiskVO) => {
  if (!row.id) {
    message.error('当前风险记录缺少编号，无法处理。')
    return
  }
  Object.assign(resolveFormData, {
    id: row.id,
    supplierName: row.supplierName || '',
    riskDescription: row.riskDescription || '',
    resolutionRemark: ''
  })
  resolveDialogVisible.value = true
}

const submitResolve = async () => {
  await resolveFormRef.value?.validate()
  if (!resolveFormData.id) {
    message.error('风险记录编号缺失，无法继续。')
    return
  }
  resolveLoading.value = true
  try {
    await SrmSupplierRiskApi.resolveSupplierRisk({
      id: resolveFormData.id,
      resolutionRemark: resolveFormData.resolutionRemark
    })
    resolveDialogVisible.value = false
    message.success('供应商风险已处理')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商风险处理失败，请检查后端接口。'))
    throw error
  } finally {
    resolveLoading.value = false
  }
}

onMounted(() => {
  loadReferenceSuppliers('')
  getList()
})
</script>

<style scoped lang="scss">
.risk-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.risk-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: linear-gradient(135deg, #ffffff 0%, #f7f9fc 100%);
}

.risk-card--danger {
  background: linear-gradient(135deg, #fff3f3 0%, #ffe9e9 100%);
}

.risk-card--success {
  background: linear-gradient(135deg, #f0fbf5 0%, #e5f7ec 100%);
}

.risk-card__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.risk-card__value {
  font-size: 24px;
  line-height: 1;
  color: var(--el-text-color-primary);
}

@media (max-width: 992px) {
  .risk-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .risk-overview {
    grid-template-columns: 1fr;
  }
}
</style>
