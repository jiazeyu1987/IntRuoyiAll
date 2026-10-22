<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
    <el-alert v-if="formError" :title="formError" type="error" :closable="false" class="mb-16px" />
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="124px"
    >
      <el-form-item label="上级类别" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :data="categoryOptions"
          :props="defaultProps"
          check-strictly
          clearable
          default-expand-all
          node-key="id"
          placeholder="请选择上级文件类别"
        />
      </el-form-item>
      <el-form-item label="类别编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入文件类别编码" />
      </el-form-item>
      <el-form-item label="类别名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入文件类别名称" />
      </el-form-item>
      <el-form-item label="审批来源" prop="approvalSourceMode">
        <el-select v-model="formData.approvalSourceMode" data-testid="dcc-category-approval-source" class="!w-100%" :disabled="formLoading">
          <el-option label="公共审批矩阵" value="PUBLIC_MATRIX" />
          <el-option label="本次申请自定义（其他）" value="APPLICATION_CUSTOM" />
        </el-select>
        <div class="mt-1 text-12px text-gray-500">审批来源由此类别明确配置；本次自定义沿用既定主管、会签、批准及文控流程。</div>
      </el-form-item>
      <el-form-item label="阶段">
        <el-input
          :model-value="categoryTaxonomyStageName || '请选择默认文件分类后自动生成'"
          disabled
          placeholder="阶段随默认文件分类自动派生"
        />
        <div class="mt-1 text-12px text-gray-500">阶段随默认文件分类自动派生</div>
      </el-form-item>
      <el-form-item label="默认文件分类" prop="fileTypeTaxonomyId">
        <el-cascader
          v-model="formData.fileTypeTaxonomyId"
          class="!w-100%"
          :options="fileTypeTaxonomyOptions"
          :props="taxonomyCascaderProps"
          :disabled="taxonomyLoading"
          clearable
          filterable
          placeholder="请选择五级文件分类路径"
        />
      </el-form-item>
      <el-form-item label="绑定目录" prop="directoryId">
        <el-tree-select
          v-model="formData.directoryId"
          :data="directoryOptions"
          :props="defaultProps"
          check-strictly
          default-expand-all
          node-key="id"
          placeholder="请选择受控目录"
        />
      </el-form-item>
      <el-form-item label="类别说明" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入类别治理说明、适用范围或业务约束"
        />
      </el-form-item>
      <el-form-item label="来源标识" prop="source">
        <el-input v-model="formData.source" placeholder="例如：IntAuth / 手工维护" />
      </el-form-item>
      <el-form-item label="启用状态" prop="active">
        <el-radio-group v-model="formData.active">
          <el-radio
            v-for="item in ACTIVE_STATUS_OPTIONS"
            :key="String(item.value)"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="要求分发" prop="distributionRequired">
        <el-switch v-model="formData.distributionRequired" />
      </el-form-item>
      <el-form-item label="要求培训" prop="trainingRequired">
        <el-switch v-model="formData.trainingRequired" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-220px" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="formLoading" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import { defaultProps, handleTree } from '@/utils/tree'
import {
  createFileCategory,
  updateFileCategory,
  requireCategoryApprovalSourceMode,
  type DccApprovalSourceMode,
  type ControlledFileCategorySaveReqVO,
  type ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import type { ControlledFileDirectoryVO } from '@/api/dcc/controlledFile/directories'
import {
  getFileTypeTaxonomyList,
  type DccFileTypeTaxonomyVO
} from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import { ACTIVE_STATUS_OPTIONS } from '../../shared/options'
import {
  buildDccFileTypeTaxonomyStageNameMap,
  resolveDccFileTypeTaxonomyStageName
} from '../../shared/file-type-taxonomy-stage'

defineOptions({ name: 'DccControlledFileCategoryForm' })

interface CategoryTreeNode extends ControlledFileCategoryVO {
  children?: CategoryTreeNode[]
}

interface CategoryFormData {
  id?: number
  parentId?: number | null
  code: string
  name: string
  approvalSourceMode: DccApprovalSourceMode | null
  fileTypeTaxonomyId?: number | null
  directoryId?: number | null
  active: boolean
  sort: number
  source?: string
  remark?: string
  description?: string
  distributionRequired: boolean
  trainingRequired: boolean
}

const message = useMessage()
const { t } = useI18n()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formError = ref('')
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const categoryOptions = ref<CategoryTreeNode[]>([])
const directoryOptions = ref<ControlledFileDirectoryVO[]>([])
const fileTypeTaxonomyOptions = ref<DccFileTypeTaxonomyVO[]>([])
const fileTypeTaxonomies = ref<DccFileTypeTaxonomyVO[]>([])
const taxonomyLoading = ref(false)
const formData = ref<CategoryFormData>({
  parentId: undefined,
  code: '',
  name: '',
  approvalSourceMode: 'PUBLIC_MATRIX',
  fileTypeTaxonomyId: undefined,
  directoryId: undefined,
  active: true,
  sort: 0,
  source: '',
  remark: '',
  description: '',
  distributionRequired: true,
  trainingRequired: true
})

const formRules = reactive<FormRules>({
  code: [{ required: true, message: '类别编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '类别名称不能为空', trigger: 'blur' }],
  approvalSourceMode: [{ required: true, message: '请明确文件类别审批来源', trigger: 'change' }],
  fileTypeTaxonomyId: [{ required: true, message: '默认文件分类不能为空', trigger: 'change' }],
  directoryId: [{ required: true, message: '绑定目录不能为空', trigger: 'change' }],
  active: [{ required: true, message: '启用状态不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }]
})

const taxonomyCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}
const categoryTaxonomyStageNameMap = computed(() =>
  buildDccFileTypeTaxonomyStageNameMap(fileTypeTaxonomies.value)
)
const categoryTaxonomyStageName = computed(
  () =>
    resolveDccFileTypeTaxonomyStageName(
      { fileTypeTaxonomyId: formData.value.fileTypeTaxonomyId },
      categoryTaxonomyStageNameMap.value
    ) || ''
)

const emit = defineEmits<{
  success: []
}>()

const open = (
  type: 'create' | 'update',
  payload: {
    row?: ControlledFileCategoryVO
    categories: CategoryTreeNode[]
    directories: ControlledFileDirectoryVO[]
  }
) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增文件类别' : '编辑文件类别'
  formType.value = type
  categoryOptions.value = payload.categories.filter((item) => item.id !== payload.row?.id)
  directoryOptions.value = payload.directories
  resetForm()
  void loadFileTypeTaxonomies()
  if (payload.row) {
    formData.value = {
      id: payload.row.id,
      parentId: payload.row.parentId,
      code: payload.row.code,
      name: payload.row.name,
      approvalSourceMode: null,
      fileTypeTaxonomyId: payload.row.fileTypeTaxonomyId,
      directoryId: payload.row.directoryId,
      active: payload.row.active,
      sort: payload.row.sort,
      source: payload.row.source,
      remark: payload.row.remark,
      description: payload.row.description,
      distributionRequired: payload.row.distributionRequired ?? true,
      trainingRequired: payload.row.trainingRequired ?? true
    }
    try { formData.value.approvalSourceMode = requireCategoryApprovalSourceMode(payload.row.approvalSourceMode) }
    catch (cause) { formError.value = cause instanceof Error ? cause.message : String(cause) }
  }
}

defineExpose({ open })

const resetForm = () => {
  formError.value = ''
  formData.value = {
    parentId: undefined,
    code: '',
    name: '',
    approvalSourceMode: 'PUBLIC_MATRIX',
    fileTypeTaxonomyId: undefined,
    directoryId: undefined,
    active: true,
    sort: 0,
    source: '',
    remark: '',
    description: '',
    distributionRequired: true,
    trainingRequired: true
  }
  formRef.value?.resetFields()
}

const submitForm = async () => {
  if (formLoading.value) return
  formError.value = ''
  let approvalSourceMode: DccApprovalSourceMode
  try { approvalSourceMode = requireCategoryApprovalSourceMode(formData.value.approvalSourceMode) }
  catch (cause) { formError.value = cause instanceof Error ? cause.message : String(cause); return }
  let valid = false
  try {
    valid = await formRef.value?.validate()
  } catch {
    formError.value = '请修正标出的必填信息后保存'
    return
  }
  if (!valid) {
    return
  }
  const payload: ControlledFileCategorySaveReqVO = {
    id: formData.value.id,
    parentId: formData.value.parentId,
    code: formData.value.code.trim(),
    name: formData.value.name.trim(),
    approvalSourceMode,
    lifecycleStage: '' as ControlledFileCategoryVO['lifecycleStage'],
    fileTypeTaxonomyId: formData.value.fileTypeTaxonomyId ?? null,
    active: formData.value.active,
    sort: formData.value.sort,
    source: formData.value.source?.trim() || undefined,
    remark: formData.value.remark?.trim() || undefined,
    description: formData.value.description?.trim() || undefined,
    distributionRequired: formData.value.distributionRequired,
    trainingRequired: formData.value.trainingRequired,
    directoryBinding: {
      directoryId: formData.value.directoryId as number,
      active: true
    }
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      const categoryId = await createFileCategory(payload)
      if (!Number.isSafeInteger(categoryId) || !categoryId || categoryId <= 0) {
        throw new Error('新增文件类别未返回有效身份，请核对已保存记录')
      }
      formData.value.id = categoryId
      formType.value = 'update'
    } else if (formData.value.id) {
      await updateFileCategory(formData.value.id, payload)
    } else {
      throw new Error('文件类别身份缺失，不能保存')
    }
    message.success(t('common.saveSuccess'))
    dialogVisible.value = false
    emit('success')
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error)
    formError.value = reason
  } finally {
    formLoading.value = false
  }
}

const loadFileTypeTaxonomies = async () => {
  taxonomyLoading.value = true
  try {
    const rows = await getFileTypeTaxonomyList()
    fileTypeTaxonomies.value = rows
    fileTypeTaxonomyOptions.value = handleTree(
      rows.map((item) => ({ ...item }))
    ) as DccFileTypeTaxonomyVO[]
  } finally {
    taxonomyLoading.value = false
  }
}
</script>
