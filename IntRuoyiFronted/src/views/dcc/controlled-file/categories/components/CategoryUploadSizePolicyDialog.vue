<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="980px">
    <div v-loading="loading">
      <el-alert
        class="mb-16px"
        title="上传大小策略用于提交前校验源文件和图纸 PDF 大小；缺少有效策略时，受控文件上传会被后端阻断。"
        type="warning"
        :closable="false"
      />

      <div class="policy-toolbar">
        <div>
          <div class="policy-title">{{ currentCategory?.name || '-' }}</div>
          <div class="policy-caption">当前仅维护该文件类别下的上传用途策略。</div>
        </div>
        <el-button
          type="primary"
          plain
          @click="openCreatePolicy"
          v-hasPermi="['dcc:controlled-file:category:manage']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增策略
        </el-button>
      </div>

      <el-table :data="categoryPolicies" empty-text="当前类别尚未配置上传大小策略">
        <el-table-column label="策略编码" min-width="180" prop="policyCode" show-overflow-tooltip />
        <el-table-column label="用途" width="110">
          <template #default="{ row }">
            {{ formatPurpose(row.purpose) }}
          </template>
        </el-table-column>
        <el-table-column label="最大大小" min-width="170">
          <template #default="{ row }">
            <div class="policy-size" :title="formatExactBytes(row.maxBytes)">
              <span class="policy-size__main">{{ formatPolicySize(row.maxBytes) }}</span>
              <span class="policy-size__bytes">{{ formatExactBytes(row.maxBytes) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="120" prop="policyVersion" show-overflow-tooltip />
        <el-table-column label="生效范围" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatEffectiveRange(row.effectiveFrom, row.effectiveTo) }}
          </template>
        </el-table-column>
        <el-table-column label="变更原因" min-width="220" prop="changeReason" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openEditPolicy(row)"
              v-hasPermi="['dcc:controlled-file:category:manage']"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template v-if="formVisible">
        <el-divider />
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="112px">
          <el-form-item label="策略编码" prop="policyCode">
            <el-input v-model="formData.policyCode" placeholder="请输入策略编码" />
          </el-form-item>
          <el-form-item label="用途" prop="purpose">
            <el-select v-model="formData.purpose" class="!w-220px" placeholder="请选择上传用途">
              <el-option
                v-for="item in DCC_UPLOAD_SIZE_POLICY_PURPOSE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="最大大小" prop="maxBytes">
            <el-input-number
              v-model="formData.maxBytes"
              class="!w-260px"
              :min="1"
              :step="1024"
              :precision="0"
            />
            <div class="policy-size-preview" data-testid="dcc-upload-size-policy-readable-preview">
              {{ formatPolicySize(formData.maxBytes) }}（{{ formatExactBytes(formData.maxBytes) }}）
            </div>
          </el-form-item>
          <el-form-item label="启用状态" prop="enabled">
            <el-switch v-model="formData.enabled" />
          </el-form-item>
          <el-form-item label="策略版本" prop="policyVersion">
            <el-input v-model="formData.policyVersion" placeholder="请输入策略版本" />
          </el-form-item>
          <el-form-item label="生效开始">
            <el-date-picker
              v-model="formData.effectiveFrom"
              class="!w-260px"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="立即生效可留空"
            />
          </el-form-item>
          <el-form-item label="生效截止">
            <el-date-picker
              v-model="formData.effectiveTo"
              class="!w-260px"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="长期有效可留空"
            />
          </el-form-item>
          <el-form-item label="变更原因" prop="changeReason">
            <el-input
              v-model="formData.changeReason"
              type="textarea"
              :rows="3"
              placeholder="请输入本次策略配置原因"
            />
          </el-form-item>
        </el-form>
      </template>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button v-if="formVisible" @click="cancelForm">取消编辑</el-button>
      <el-button v-if="formVisible" type="primary" :loading="saving" @click="submitForm">
        保存策略
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import {
  createUploadSizePolicy,
  DCC_UPLOAD_SIZE_POLICY_PURPOSE_OPTIONS,
  getUploadSizePolicyList,
  updateUploadSizePolicy,
  type DccUploadSizePolicyPurpose,
  type DccUploadSizePolicySaveReqVO,
  type DccUploadSizePolicyVO
} from '@/api/dcc/controlledFile/uploadSizePolicies'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'CategoryUploadSizePolicyDialog' })

interface PolicyFormData {
  id?: number
  policyCode: string
  scopeType: 'CATEGORY_PURPOSE'
  categoryId?: number
  purpose: DccUploadSizePolicyPurpose
  maxBytes: number
  enabled: boolean
  policyVersion: string
  effectiveFrom?: string | null
  effectiveTo?: string | null
  changeReason: string
}

const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('上传大小策略')
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const formRef = ref()
const currentCategory = ref<ControlledFileCategoryVO>()
const policyList = ref<DccUploadSizePolicyVO[]>([])
const formData = reactive<PolicyFormData>({
  policyCode: '',
  scopeType: 'CATEGORY_PURPOSE',
  categoryId: undefined,
  purpose: 'SOURCE',
  maxBytes: 10 * 1024 * 1024,
  enabled: true,
  policyVersion: 'v1',
  effectiveFrom: undefined,
  effectiveTo: undefined,
  changeReason: ''
})

const formRules = reactive<FormRules>({
  policyCode: [{ required: true, message: '策略编码不能为空', trigger: 'blur' }],
  purpose: [{ required: true, message: '上传用途不能为空', trigger: 'change' }],
  maxBytes: [{ required: true, message: '最大大小不能为空', trigger: 'change' }],
  enabled: [{ required: true, message: '启用状态不能为空', trigger: 'change' }],
  policyVersion: [{ required: true, message: '策略版本不能为空', trigger: 'blur' }],
  changeReason: [{ required: true, message: '变更原因不能为空', trigger: 'blur' }]
})

const categoryPolicies = computed(() => {
  const categoryId = currentCategory.value?.id
  if (!categoryId) {
    return []
  }
  return policyList.value.filter(
    (item) => Number(item.categoryId) === Number(categoryId) && item.scopeType === 'CATEGORY_PURPOSE'
  )
})

const resetForm = () => {
  const category = currentCategory.value
  const timestamp = new Date()
    .toISOString()
    .replace(/[-:.TZ]/g, '')
    .slice(0, 14)
  formData.id = undefined
  formData.policyCode = `DCC-${category?.id || 'CATEGORY'}-SOURCE-${timestamp}`
  formData.scopeType = 'CATEGORY_PURPOSE'
  formData.categoryId = category?.id
  formData.purpose = 'SOURCE'
  formData.maxBytes = 10 * 1024 * 1024
  formData.enabled = true
  formData.policyVersion = 'v1'
  formData.effectiveFrom = undefined
  formData.effectiveTo = undefined
  formData.changeReason = ''
  formRef.value?.clearValidate()
}

const loadPolicies = async () => {
  loading.value = true
  try {
    policyList.value = await getUploadSizePolicyList()
  } finally {
    loading.value = false
  }
}

const openCreatePolicy = () => {
  resetForm()
  formVisible.value = true
}

const openEditPolicy = (row: DccUploadSizePolicyVO) => {
  formData.id = row.id
  formData.policyCode = row.policyCode
  formData.scopeType = 'CATEGORY_PURPOSE'
  formData.categoryId = currentCategory.value?.id
  formData.purpose = row.purpose || 'SOURCE'
  formData.maxBytes = row.maxBytes
  formData.enabled = row.enabled
  formData.policyVersion = row.policyVersion
  formData.effectiveFrom = formatDateTimeValue(row.effectiveFrom, '') || undefined
  formData.effectiveTo = formatDateTimeValue(row.effectiveTo, '') || undefined
  formData.changeReason = row.changeReason
  formVisible.value = true
  formRef.value?.clearValidate()
}

const cancelForm = () => {
  formVisible.value = false
  resetForm()
}

const buildPayload = (): DccUploadSizePolicySaveReqVO => ({
  policyCode: formData.policyCode.trim(),
  scopeType: 'CATEGORY_PURPOSE',
  categoryId: currentCategory.value?.id,
  purpose: formData.purpose,
  maxBytes: Number(formData.maxBytes),
  enabled: formData.enabled,
  policyVersion: formData.policyVersion.trim(),
  effectiveFrom: formData.effectiveFrom || undefined,
  effectiveTo: formData.effectiveTo || undefined,
  changeReason: formData.changeReason.trim()
})

const submitForm = async () => {
  if (!currentCategory.value?.id) {
    message.error('文件类别不能为空')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (formData.id) {
      await updateUploadSizePolicy(formData.id, buildPayload())
    } else {
      await createUploadSizePolicy(buildPayload())
    }
    message.success('DCC 上传大小策略已保存')
    formVisible.value = false
    await loadPolicies()
  } finally {
    saving.value = false
  }
}

const formatPurpose = (purpose?: string | null) =>
  DCC_UPLOAD_SIZE_POLICY_PURPOSE_OPTIONS.find((item) => item.value === purpose)?.label || purpose || '-'

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

const formatEffectiveRange = (from?: number | null, to?: number | null) => {
  const start = from ? formatDateTimeValue(from) : '立即'
  const end = to ? formatDateTimeValue(to) : '长期'
  return `${start} 至 ${end}`
}

const open = async (category: ControlledFileCategoryVO) => {
  currentCategory.value = category
  dialogTitle.value = `上传大小策略 - ${category.name}`
  dialogVisible.value = true
  formVisible.value = false
  resetForm()
  await loadPolicies()
}

defineExpose({ open })
</script>

<style scoped>
.policy-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.policy-title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
}

.policy-caption {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
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

@media (max-width: 720px) {
  .policy-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
