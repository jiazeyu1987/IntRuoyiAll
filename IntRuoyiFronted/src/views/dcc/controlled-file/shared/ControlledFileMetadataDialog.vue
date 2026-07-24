<template>
  <el-dialog
    v-model="dialogVisible"
    title="修改基础信息"
    width="720px"
    destroy-on-close
  >
    <el-alert
      v-if="metadataDialog.inlineError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="metadataDialog.inlineError"
    />
    <el-form label-width="96px">
      <el-form-item label="产品编号" :error="metadataDialog.fieldErrors.productMasterId">
        <el-select
          v-model="metadataForm.productMasterId"
          class="!w-full"
          clearable
          filterable
          remote
          reserve-keyword
          :loading="productOptionsLoading"
          :remote-method="loadProductOptions"
          placeholder="可不选择产品主数据"
          @change="handleProductMasterChange"
        >
          <el-option
            v-for="product in productOptions"
            :key="product.id"
            :label="formatProductOptionLabel(product)"
            :value="product.id"
          />
        </el-select>
        <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
          当前快照：{{ props.file?.productCode || '-' }} / {{ props.file?.productName || '-' }}
        </div>
      </el-form-item>
      <el-form-item label="产品名称">
        <el-input
          v-model="metadataForm.productName"
          disabled
          placeholder="选择产品编号后自动带出"
        />
      </el-form-item>
      <el-form-item label="文件名称" :error="metadataDialog.fieldErrors.fileName">
        <el-input
          v-model="metadataForm.fileName"
          clearable
          maxlength="256"
          placeholder="请输入文件名称"
        />
      </el-form-item>
      <el-form-item label="文件编号" :error="metadataDialog.fieldErrors.fileNumber">
        <el-input
          v-model="metadataForm.fileNumber"
          clearable
          maxlength="64"
          placeholder="请输入文件编号"
        />
      </el-form-item>
      <el-form-item label="DCC基础条目" :error="metadataDialog.fieldErrors.dccProjectCodeId">
        <el-select
          v-model="metadataForm.dccProjectCodeId"
          class="!w-full"
          clearable
          filterable
          remote
          reserve-keyword
          :loading="projectCodeOptionsLoading"
          :remote-method="loadProjectCodeOptions"
          placeholder="请选择 DCC基础条目"
          @change="clearProjectCodeError"
        >
          <el-option
            v-for="projectCode in projectCodeOptions"
            :key="projectCode.id"
            :label="formatProjectCodeOptionLabel(projectCode)"
            :value="projectCode.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="培训要求" :error="metadataDialog.fieldErrors.needTraining">
        <el-radio-group v-model="metadataForm.needTraining">
          <el-radio-button :label="true">需要培训</el-radio-button>
          <el-radio-button :label="false">无需培训</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="文件分类" :error="metadataDialog.fieldErrors.fileTypeTaxonomyId">
        <el-cascader
          v-model="metadataForm.fileTypeTaxonomyId"
          class="!w-full"
          :options="fileTypeTaxonomyOptions"
          :props="taxonomyCascaderProps"
          :disabled="taxonomyLoading"
          clearable
          filterable
          placeholder="请选择五级文件分类路径"
          @change="handleFileTypeTaxonomyChange"
        />
      </el-form-item>
      <el-form-item label="分类路径">
        <div class="metadata-taxonomy-path">
          <el-tag
            v-for="item in taxonomyLevelTags"
            :key="item.label"
            effect="plain"
            size="small"
          >
            {{ item.label }}：{{ item.value || '-' }}
          </el-tag>
        </div>
      </el-form-item>
      <el-form-item label="文件类别" :error="metadataDialog.fieldErrors.categoryId">
        <el-select
          v-model="metadataForm.categoryId"
          class="!w-full"
          filterable
          placeholder="请选择文件类别"
          @change="handleCategoryChange"
        >
          <el-option
            v-for="category in categoryOptions"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="受控目录" :error="metadataDialog.fieldErrors.directoryId">
        <el-select
          v-model="metadataForm.directoryId"
          class="!w-full"
          filterable
          :loading="directoryOptionsLoading"
          placeholder="请选择类别绑定范围内的受控目录"
        >
          <el-option
            v-for="directory in directoryOptions"
            :key="directory.value"
            :label="directory.label"
            :value="directory.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="修改说明">
        <el-input
          v-model="metadataForm.changeReason"
          clearable
          maxlength="512"
          placeholder="可填写本次修正原因"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 4 }"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="hideDialog">取消</el-button>
      <el-button type="primary" :loading="metadataDialog.submitting" @click="submitMetadataDialog">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {
  DCC_PRODUCT_STATUS_ENABLE,
  getDccProductOptions,
  updateControlledFileMetadata,
  type DccControlledFileProductOptionVO,
  type ControlledFileMetadataUpdateReqVO,
  type ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import {
  getDirectoryTree,
  type ControlledFileDirectoryVO
} from '@/api/dcc/controlledFile/directories'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  getFileTypeTaxonomyList,
  type DccFileTypeTaxonomyVO
} from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import { handleTree } from '@/utils/tree'
import { resolveReadSideErrorMessage } from '../detail/presentation'

defineOptions({ name: 'ControlledFileMetadataDialog' })

const props = defineProps<{
  modelValue: boolean
  file?: ControlledFileVO
  categories: ControlledFileCategoryVO[]
  directories: ControlledFileDirectoryVO[]
  loadDirectoriesOnOpen?: boolean
  assignmentId?: number
  readonlyProjectCodeScope?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'saved'): void
}>()

const message = useMessage()

const metadataDialog = reactive({
  submitting: false,
  inlineError: '',
  fieldErrors: {} as Record<string, string>
})

const metadataForm = reactive({
  productMasterId: undefined as number | undefined,
  productName: '',
  dccProjectCodeId: undefined as number | undefined,
  needTraining: false,
  fileTypeTaxonomyId: undefined as number | undefined,
  fileTypeLevel1: '',
  fileTypeLevel2: '',
  fileTypeLevel3: '',
  fileTypeLevel4: '',
  fileTypeLevel5: '',
  fileName: '',
  productCode: '',
  fileNumber: '',
  categoryId: undefined as number | undefined,
  directoryId: undefined as number | undefined,
  changeReason: ''
})

interface DirectoryOption {
  value: number
  label: string
}

const productOptions = ref<DccControlledFileProductOptionVO[]>([])
const productOptionsLoading = ref(false)
const projectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const projectCodeOptionsLoading = ref(false)
const fileTypeTaxonomies = ref<DccFileTypeTaxonomyVO[]>([])
const taxonomyLoading = ref(false)
const dialogDirectories = ref<ControlledFileDirectoryVO[]>([])
const directoryOptionsLoading = ref(false)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const categoryOptions = computed(() =>
  props.categories.filter(
    (item): item is ControlledFileCategoryVO & { id: number } =>
      Boolean(item.active) && item.id !== undefined
  )
)

const taxonomyCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

const fileTypeTaxonomyOptions = computed(
  () => handleTree(fileTypeTaxonomies.value.map((item) => ({ ...item }))) as DccFileTypeTaxonomyVO[]
)

const taxonomyById = computed(() => {
  const result = new Map<number, DccFileTypeTaxonomyVO>()
  fileTypeTaxonomies.value.forEach((item) => {
    if (item.id) {
      result.set(item.id, item)
    }
  })
  return result
})

const resolveTaxonomyPath = (id?: number) => {
  const nodes: DccFileTypeTaxonomyVO[] = []
  const visited = new Set<number>()
  let current = id ? taxonomyById.value.get(id) : undefined
  while (current?.id && !visited.has(current.id)) {
    visited.add(current.id)
    nodes.unshift(current)
    current = current.parentId ? taxonomyById.value.get(current.parentId) : undefined
  }
  return nodes
}

const taxonomyPathNames = computed(() =>
  resolveTaxonomyPath(metadataForm.fileTypeTaxonomyId).map((item) => item.name)
)

const taxonomyLevelTags = computed(() => {
  const levels = [
    metadataForm.fileTypeLevel1,
    metadataForm.fileTypeLevel2,
    metadataForm.fileTypeLevel3,
    metadataForm.fileTypeLevel4,
    metadataForm.fileTypeLevel5
  ]
  return ['一级', '二级', '三级', '四级', '五级'].map((label, index) => ({
    label,
    value: levels[index]
  }))
})

const effectiveDirectories = computed(() =>
  props.loadDirectoriesOnOpen ? dialogDirectories.value : props.directories
)

const directoryById = computed(() => {
  const result = new Map<number, ControlledFileDirectoryVO>()
  const visit = (items: ControlledFileDirectoryVO[]) => {
    for (const item of items) {
      if (item.id !== undefined) {
        result.set(item.id, item)
      }
      visit(item.children || [])
    }
  }
  visit(effectiveDirectories.value)
  return result
})

const childrenByParentId = computed(() => {
  const result = new Map<number | null, ControlledFileDirectoryVO[]>()
  const visit = (items: ControlledFileDirectoryVO[]) => {
    for (const item of items) {
      const parentKey = item.parentId ?? null
      result.set(parentKey, [...(result.get(parentKey) || []), item])
      visit(item.children || [])
    }
  }
  visit(effectiveDirectories.value)
  return result
})

const collectDirectoryOptions = (
  directoryId: number | null | undefined,
  parentLabel = '',
  result: DirectoryOption[] = []
) => {
  if (directoryId === null || directoryId === undefined) {
    return result
  }
  const directory = directoryById.value.get(directoryId)
  if (!directory?.id) {
    return result
  }
  const label = parentLabel ? `${parentLabel}/${directory.name}` : directory.name
  result.push({ value: directory.id, label })
  for (const child of childrenByParentId.value.get(directory.id) || []) {
    collectDirectoryOptions(child.id, label, result)
  }
  return result
}

const selectedCategory = computed(() =>
  categoryOptions.value.find((category) => category.id === metadataForm.categoryId)
)

const selectedProduct = computed(() =>
  productOptions.value.find((product) => product.id === metadataForm.productMasterId)
)

const directoryOptions = computed(() => {
  const category = selectedCategory.value
  if (!category) {
    return []
  }
  return collectDirectoryOptions(category.directoryId)
})

const trimToUndefined = (value: string) => {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

const trimToNull = (value: string) => {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

const formatProductOptionLabel = (product: DccControlledFileProductOptionVO) =>
  `${product.dccProductCode} · ${product.nameCn} · ${product.productCode}`

const formatProjectCodeOptionLabel = (projectCode: DccProjectCodeRespVO) =>
  [projectCode.projectName, projectCode.projectCode, projectCode.docControlNo].filter(Boolean).join(' / ')

const clearProjectCodeError = () => {
  delete metadataDialog.fieldErrors.dccProjectCodeId
}

const applyTaxonomyPathToLevels = (names: string[]) => {
  metadataForm.fileTypeLevel1 = names[0] || ''
  metadataForm.fileTypeLevel2 = names[1] || ''
  metadataForm.fileTypeLevel3 = names[2] || ''
  metadataForm.fileTypeLevel4 = names[3] || ''
  metadataForm.fileTypeLevel5 = names[4] || ''
}

const handleFileTypeTaxonomyChange = () => {
  applyTaxonomyPathToLevels(taxonomyPathNames.value)
  delete metadataDialog.fieldErrors.fileTypeTaxonomyId
}

const matchCurrentTaxonomyByLevels = () => {
  const currentLevels = [
    metadataForm.fileTypeLevel1,
    metadataForm.fileTypeLevel2,
    metadataForm.fileTypeLevel3,
    metadataForm.fileTypeLevel4,
    metadataForm.fileTypeLevel5
  ].map((item) => item.trim())
  const activeLevelCount = currentLevels.filter(Boolean).length
  if (!activeLevelCount) {
    return undefined
  }
  return fileTypeTaxonomies.value.find((taxonomy) => {
    if (!taxonomy.id) {
      return false
    }
    const names = resolveTaxonomyPath(taxonomy.id).map((item) => item.name)
    if (names.length !== activeLevelCount) {
      return false
    }
    return names.every((name, index) => name === currentLevels[index])
  })?.id
}

const loadFileTypeTaxonomies = async () => {
  taxonomyLoading.value = true
  try {
    fileTypeTaxonomies.value = await getFileTypeTaxonomyList()
    if (!metadataForm.fileTypeTaxonomyId) {
      metadataForm.fileTypeTaxonomyId = matchCurrentTaxonomyByLevels()
    }
    handleFileTypeTaxonomyChange()
  } catch (error) {
    fileTypeTaxonomies.value = []
    metadataDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '文件分类加载失败，请查看错误提示后重试。'
    )
  } finally {
    taxonomyLoading.value = false
  }
}

const loadProductOptions = async (keyword = '') => {
  productOptionsLoading.value = true
  try {
    productOptions.value = await getDccProductOptions({
      status: DCC_PRODUCT_STATUS_ENABLE,
      requireDccProductCode: true,
      keyword: keyword.trim() || undefined
    })
    const currentId = props.file?.productMasterId
    if (currentId && !productOptions.value.some((product) => product.id === currentId)) {
      metadataForm.productMasterId = undefined
      metadataForm.productCode = ''
      metadataForm.productName = ''
    }
  } catch (error) {
    productOptions.value = []
    metadataDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '产品主数据加载失败，请查看错误提示后重试。'
    )
  } finally {
    productOptionsLoading.value = false
  }
}

const loadProjectCodeOptions = async (keyword = '') => {
  projectCodeOptionsLoading.value = true
  try {
    const data = await getProjectCodePage({
      pageNo: 1,
      pageSize: 50,
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      keyword: keyword.trim() || undefined
    })
    projectCodeOptions.value = data.list
    const currentId = props.file?.dccProjectCodeId
    if (currentId && !projectCodeOptions.value.some((projectCode) => projectCode.id === currentId)) {
      metadataForm.dccProjectCodeId = currentId
    }
  } catch (error) {
    projectCodeOptions.value = []
    metadataDialog.inlineError = resolveReadSideErrorMessage(
      error,
      'DCC基础条目加载失败，请查看错误提示后重试。'
    )
  } finally {
    projectCodeOptionsLoading.value = false
  }
}

const loadDialogDirectories = async () => {
  if (!props.loadDirectoriesOnOpen) {
    return
  }
  directoryOptionsLoading.value = true
  try {
    dialogDirectories.value = await getDirectoryTree()
  } catch (error) {
    dialogDirectories.value = []
    metadataDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '受控目录加载失败，请查看错误提示后重试。'
    )
  } finally {
    directoryOptionsLoading.value = false
  }
}

const handleProductMasterChange = (productId: number | undefined) => {
  const product = productOptions.value.find((item) => item.id === productId)
  metadataForm.productCode = product?.dccProductCode || ''
  metadataForm.productName = product?.nameCn || ''
  delete metadataDialog.fieldErrors.productMasterId
}

const resetMetadataDialog = () => {
  metadataDialog.submitting = false
  metadataDialog.inlineError = ''
  metadataDialog.fieldErrors = {}
  metadataForm.productMasterId = props.file?.productMasterId || undefined
  metadataForm.productName = props.file?.productName || ''
  metadataForm.dccProjectCodeId = props.file?.dccProjectCodeId || undefined
  metadataForm.needTraining = Boolean(props.file?.needTraining)
  metadataForm.fileTypeTaxonomyId = props.file?.fileTypeTaxonomyId || undefined
  metadataForm.fileTypeLevel1 = props.file?.fileTypeLevel1 || ''
  metadataForm.fileTypeLevel2 = props.file?.fileTypeLevel2 || ''
  metadataForm.fileTypeLevel3 = props.file?.fileTypeLevel3 || ''
  metadataForm.fileTypeLevel4 = props.file?.fileTypeLevel4 || ''
  metadataForm.fileTypeLevel5 = props.file?.fileTypeLevel5 || ''
  metadataForm.fileName = props.file?.fileName || props.file?.title || ''
  metadataForm.productCode = props.file?.productCode || ''
  metadataForm.fileNumber = props.file?.fileNumber || ''
  metadataForm.categoryId = props.file?.categoryId
  metadataForm.directoryId = props.file?.directoryId
  metadataForm.changeReason = ''
}

const handleCategoryChange = () => {
  metadataForm.directoryId = undefined
  const categoryTaxonomyId = selectedCategory.value?.fileTypeTaxonomyId
  metadataForm.fileTypeTaxonomyId = categoryTaxonomyId || undefined
  handleFileTypeTaxonomyChange()
  delete metadataDialog.fieldErrors.categoryId
  delete metadataDialog.fieldErrors.directoryId
}

const validateMetadataDialog = () => {
  const errors: Record<string, string> = {}
  const fileName = metadataForm.fileName.trim()
  if (!fileName) {
    errors.fileName = '请输入文件名称'
  }
  if (metadataForm.productMasterId && !selectedProduct.value?.dccProductCode) {
    errors.productMasterId = '请选择启用且包含 DCC 产品编号的产品主数据'
  }
  if (metadataForm.needTraining === undefined || metadataForm.needTraining === null) {
    errors.needTraining = '请选择培训要求'
  }
  if (!metadataForm.fileTypeTaxonomyId) {
    errors.fileTypeTaxonomyId = '请选择文件分类'
  }
  if (!metadataForm.categoryId) {
    errors.categoryId = '请选择文件类别'
  }
  if (metadataForm.categoryId && !selectedCategory.value?.directoryId) {
    errors.directoryId = '当前类别未绑定受控目录'
  } else if (!metadataForm.directoryId) {
    errors.directoryId = '请选择受控目录'
  } else if (!directoryOptions.value.some((item) => item.value === metadataForm.directoryId)) {
    errors.directoryId = '请选择类别绑定范围内的受控目录'
  }
  metadataDialog.fieldErrors = errors
  metadataDialog.inlineError = Object.values(errors)[0] || ''
  return Object.keys(errors).length === 0
}

const buildMetadataPayload = (): ControlledFileMetadataUpdateReqVO => ({
  assignmentId: props.assignmentId,
  changeReason: trimToUndefined(metadataForm.changeReason),
  productMasterId: metadataForm.productMasterId,
  productName: trimToUndefined(selectedProduct.value?.nameCn || metadataForm.productName),
  dccProjectCodeId: metadataForm.dccProjectCodeId || null,
  needTraining: metadataForm.needTraining,
  fileTypeTaxonomyId: metadataForm.fileTypeTaxonomyId || null,
  fileTypeLevel1: trimToNull(metadataForm.fileTypeLevel1),
  fileTypeLevel2: trimToNull(metadataForm.fileTypeLevel2),
  fileTypeLevel3: trimToNull(metadataForm.fileTypeLevel3),
  fileTypeLevel4: trimToNull(metadataForm.fileTypeLevel4),
  fileTypeLevel5: trimToNull(metadataForm.fileTypeLevel5),
  fileName: metadataForm.fileName.trim(),
  productCode: trimToUndefined(selectedProduct.value?.dccProductCode || metadataForm.productCode),
  fileNumber: metadataForm.fileNumber.trim(),
  categoryId: metadataForm.categoryId as number,
  directoryId: metadataForm.directoryId as number
})

const submitMetadataDialog = async () => {
  if (!props.file?.id || !validateMetadataDialog()) {
    return
  }
  metadataDialog.submitting = true
  metadataDialog.inlineError = ''
  try {
    await updateControlledFileMetadata(props.file.id, buildMetadataPayload())
    message.success('基础信息已更新')
    emit('saved')
    dialogVisible.value = false
  } catch (error) {
    metadataDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '基础信息保存失败，请查看错误提示后重试。'
    )
  } finally {
    metadataDialog.submitting = false
  }
}

const hideDialog = () => {
  dialogVisible.value = false
}

watch(
  () => [props.modelValue, props.file?.id],
  async () => {
    if (props.modelValue) {
      resetMetadataDialog()
      await Promise.all([
        loadProductOptions(),
        loadProjectCodeOptions(),
        loadDialogDirectories(),
        loadFileTypeTaxonomies()
      ])
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.metadata-taxonomy-path {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 24px;
  align-items: center;
}
</style>
