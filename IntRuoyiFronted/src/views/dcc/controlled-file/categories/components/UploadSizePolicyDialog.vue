<template>
  <Dialog v-model="dialogVisible" title="上传大小策略" width="1080px">
    <div v-loading="loading">
      <div class="mb-16px flex items-center justify-between gap-12px">
        <el-button
          type="primary"
          plain
          @click="startCreate"
          v-hasPermi="['dcc:controlled-file:category:manage']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增策略
        </el-button>
        <el-button plain :loading="loading" @click="loadPolicies">
          <Icon icon="ep:refresh" class="mr-5px" />
          刷新
        </el-button>
      </div>

      <el-table :data="policies" class="mb-18px" row-key="id">
        <el-table-column label="策略编码" min-width="190" prop="policyCode" show-overflow-tooltip />
        <el-table-column label="范围" width="130">
          <template #default="{ row }">
            {{ getScopeLabel(row.scopeType) }}
          </template>
        </el-table-column>
        <el-table-column label="文件类别" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column label="用途" width="150">
          <template #default="{ row }">
            {{ getPurposeLabel(row.purpose) }}
          </template>
        </el-table-column>
        <el-table-column label="最大大小" width="160">
          <template #default="{ row }">
            <div class="policy-size" :title="formatExactBytes(row.maxBytes)">
              <span class="policy-size__main">{{ formatPolicySize(row.maxBytes) }}</span>
              <span class="policy-size__bytes">{{ formatExactBytes(row.maxBytes) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="170" prop="policyVersion" show-overflow-tooltip />
        <el-table-column label="启用" align="center" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更原因" min-width="220" prop="changeReason" show-overflow-tooltip />
        <el-table-column label="操作" align="center" fixed="right" width="90">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="startEdit(row)"
              v-hasPermi="['dcc:controlled-file:category:manage']"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <div class="policy-form-grid">
          <el-form-item label="策略编码" prop="policyCode">
            <el-input v-model="formData.policyCode" placeholder="请输入策略编码" />
          </el-form-item>
          <el-form-item label="范围" prop="scopeType">
            <el-select v-model="formData.scopeType" class="w-full" @change="handleScopeChange">
              <el-option
                v-for="item in scopeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="needsCategory" label="文件类别" prop="categoryId">
            <el-select
              v-model="formData.categoryId"
              class="w-full"
              filterable
              placeholder="请选择文件类别"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.id"
                :label="`${item.code} / ${item.name}`"
                :value="item.id as number"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="needsPurpose" label="用途" prop="purpose">
            <el-select v-model="formData.purpose" class="w-full" placeholder="请选择上传用途">
              <el-option
                v-for="item in purposeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="最大大小" prop="maxBytes">
            <el-input-number v-model="formData.maxBytes" :min="1" class="!w-full" />
            <div class="policy-size-preview" data-testid="dcc-upload-size-policy-readable-preview">
              {{ formatPolicySize(formData.maxBytes) }}（{{ formatExactBytes(formData.maxBytes) }}）
            </div>
          </el-form-item>
          <el-form-item label="策略版本" prop="policyVersion">
            <el-input v-model="formData.policyVersion" placeholder="请输入策略版本" />
          </el-form-item>
          <el-form-item label="启用状态" prop="enabled">
            <el-switch v-model="formData.enabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
          <el-form-item label="生效时间">
            <el-date-picker
              v-model="formData.effectiveFrom"
              class="w-full"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不填表示立即可用"
            />
          </el-form-item>
          <el-form-item label="失效时间">
            <el-date-picker
              v-model="formData.effectiveTo"
              class="w-full"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不填表示长期有效"
            />
          </el-form-item>
        </div>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input
            v-model="formData.changeReason"
            type="textarea"
            :rows="2"
            placeholder="请输入本次策略调整原因"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button v-if="editingPolicyId" plain @click="startCreate">取消编辑</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        {{ editingPolicyId ? '保存策略' : '创建策略' }}
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import {
  createUploadSizePolicy,
  getUploadSizePolicyList,
  updateUploadSizePolicy,
  type DccUploadSizePolicyScopeType,
  type DccUploadSizePolicyVO
} from '@/api/dcc/controlledFile/uploadSizePolicies'

defineOptions({ name: 'DccUploadSizePolicyDialog' })

interface OptionItem<T extends string> {
  label: string
  value: T
}

interface PolicyFormData {
  policyCode: string
  scopeType: DccUploadSizePolicyScopeType
  categoryId?: number
  purpose?: string
  maxBytes: number
  enabled: boolean
  policyVersion: string
  effectiveFrom?: string
  effectiveTo?: string
  changeReason: string
}

const scopeOptions: OptionItem<DccUploadSizePolicyScopeType>[] = [
  { label: '全局', value: 'GLOBAL' },
  { label: '文件类别', value: 'CATEGORY' },
  { label: '上传用途', value: 'PURPOSE' },
  { label: '类别与用途', value: 'CATEGORY_PURPOSE' }
]

const purposeOptions: OptionItem<string>[] = [
  { label: '源文件', value: 'SOURCE' },
  { label: '图纸 PDF', value: 'DRAWING_PDF' },
  { label: '培训记录', value: 'TRAINING_RECORD' },
  { label: '外来评审输出', value: 'EXTERNAL_REVIEW_OUTPUT' }
]

const message = useMessage()
const dialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const policies = ref<DccUploadSizePolicyVO[]>([])
const categoryOptions = ref<ControlledFileCategoryVO[]>([])
const editingPolicyId = ref<number>()
const formData = reactive<PolicyFormData>({
  policyCode: '',
  scopeType: 'CATEGORY_PURPOSE',
  categoryId: undefined,
  purpose: 'SOURCE',
  maxBytes: 20_000,
  enabled: true,
  policyVersion: '',
  effectiveFrom: undefined,
  effectiveTo: undefined,
  changeReason: ''
})

const formRules = reactive<FormRules>({
  policyCode: [{ required: true, message: '策略编码不能为空', trigger: 'blur' }],
  scopeType: [{ required: true, message: '范围不能为空', trigger: 'change' }],
  maxBytes: [{ required: true, message: '最大大小不能为空', trigger: 'change' }],
  enabled: [{ required: true, message: '启用状态不能为空', trigger: 'change' }],
  policyVersion: [{ required: true, message: '策略版本不能为空', trigger: 'blur' }],
  changeReason: [{ required: true, message: '变更原因不能为空', trigger: 'blur' }]
})

const needsCategory = computed(
  () => formData.scopeType === 'CATEGORY' || formData.scopeType === 'CATEGORY_PURPOSE'
)
const needsPurpose = computed(
  () => formData.scopeType === 'PURPOSE' || formData.scopeType === 'CATEGORY_PURPOSE'
)

const sizeUnits = ['B', 'KB', 'MB', 'GB', 'TB']
const readableSizeFormatter = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 })
const exactBytesFormatter = new Intl.NumberFormat('zh-CN')

const normalizeSizeValue = (bytes?: number | null) => {
  const value = Number(bytes)
  return Number.isFinite(value) && value >= 0 ? value : undefined
}

const formatPolicySize = (bytes?: number | null) => {
  const value = normalizeSizeValue(bytes)
  if (value === undefined) {
    return '-'
  }
  let normalizedValue = value
  let unitIndex = 0
  while (normalizedValue >= 1024 && unitIndex < sizeUnits.length - 1) {
    normalizedValue /= 1024
    unitIndex += 1
  }
  return `${readableSizeFormatter.format(normalizedValue)} ${sizeUnits[unitIndex]}`
}

const formatExactBytes = (bytes?: number | null) => {
  const value = normalizeSizeValue(bytes)
  return value === undefined ? '-' : `${exactBytesFormatter.format(Math.trunc(value))} 字节`
}

const open = async (payload: { categories: ControlledFileCategoryVO[] }) => {
  categoryOptions.value = payload.categories
  dialogVisible.value = true
  startCreate()
  await loadPolicies()
}

defineExpose({ open })

const loadPolicies = async () => {
  loading.value = true
  try {
    policies.value = await getUploadSizePolicyList()
  } finally {
    loading.value = false
  }
}

const startCreate = () => {
  editingPolicyId.value = undefined
  formData.policyCode = ''
  formData.scopeType = 'CATEGORY_PURPOSE'
  formData.categoryId = undefined
  formData.purpose = 'SOURCE'
  formData.maxBytes = 20_000
  formData.enabled = true
  formData.policyVersion = ''
  formData.effectiveFrom = undefined
  formData.effectiveTo = undefined
  formData.changeReason = ''
  formRef.value?.clearValidate()
}

const startEdit = (row: DccUploadSizePolicyVO) => {
  editingPolicyId.value = row.id
  formData.policyCode = row.policyCode
  formData.scopeType = row.scopeType
  formData.categoryId = row.categoryId || undefined
  formData.purpose = row.purpose || undefined
  formData.maxBytes = row.maxBytes
  formData.enabled = row.enabled
  formData.policyVersion = row.policyVersion
  formData.effectiveFrom = formatDateTimeValue(row.effectiveFrom)
  formData.effectiveTo = formatDateTimeValue(row.effectiveTo)
  formData.changeReason = row.changeReason
  formRef.value?.clearValidate()
}

const formatDateTimeValue = (value?: string | number | null) => {
  if (!value) {
    return undefined
  }
  if (typeof value === 'string' && !/^\d+$/.test(value)) {
    return value.includes('T') ? value.replace('T', ' ').slice(0, 19) : value.slice(0, 19)
  }
  const timestamp = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(timestamp)) {
    return undefined
  }
  const date = new Date(timestamp)
  const pad = (input: number) => String(input).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const handleScopeChange = () => {
  if (!needsCategory.value) {
    formData.categoryId = undefined
  }
  if (!needsPurpose.value) {
    formData.purpose = undefined
  } else if (!formData.purpose) {
    formData.purpose = 'SOURCE'
  }
}

const validateScope = () => {
  if (needsCategory.value && !formData.categoryId) {
    message.warning('请选择文件类别')
    return false
  }
  if (needsPurpose.value && !formData.purpose) {
    message.warning('请选择上传用途')
    return false
  }
  return true
}

const buildPayload = (): DccUploadSizePolicyVO => ({
  policyCode: formData.policyCode.trim(),
  scopeType: formData.scopeType,
  categoryId: needsCategory.value ? formData.categoryId : null,
  purpose: needsPurpose.value ? formData.purpose : null,
  maxBytes: formData.maxBytes,
  enabled: formData.enabled,
  policyVersion: formData.policyVersion.trim(),
  effectiveFrom: formData.effectiveFrom || null,
  effectiveTo: formData.effectiveTo || null,
  changeReason: formData.changeReason.trim()
})

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid || !validateScope()) {
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingPolicyId.value) {
      await updateUploadSizePolicy(editingPolicyId.value, payload)
      message.success('上传大小策略已保存')
    } else {
      const id = await createUploadSizePolicy(payload)
      editingPolicyId.value = id
      message.success('上传大小策略已创建')
    }
    await loadPolicies()
  } finally {
    saving.value = false
  }
}

const getScopeLabel = (scopeType?: string) =>
  scopeOptions.find((item) => item.value === scopeType)?.label || scopeType || '-'

const getPurposeLabel = (purpose?: string | null) =>
  purposeOptions.find((item) => item.value === purpose)?.label || purpose || '-'

const resolveCategoryName = (categoryId?: number | null) => {
  if (!categoryId) {
    return '-'
  }
  const category = categoryOptions.value.find((item) => item.id === categoryId)
  return category ? `${category.code} / ${category.name}` : String(categoryId)
}
</script>

<style scoped>
.policy-form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 16px;
}

.policy-size {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.3;
}

.policy-size__main {
  color: #172033;
  font-weight: 600;
}

.policy-size__bytes,
.policy-size-preview {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.policy-size-preview {
  margin-top: 6px;
  width: 100%;
}
</style>
