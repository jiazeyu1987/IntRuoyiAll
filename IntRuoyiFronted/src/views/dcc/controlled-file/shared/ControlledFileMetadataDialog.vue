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
      <el-form-item label="DCC 项目" :error="metadataDialog.fieldErrors.dccProjectCodeId">
        <el-select
          v-model="metadataForm.dccProjectCodeId"
          class="!w-full"
          clearable
          filterable
          remote
          reserve-keyword
          :loading="projectCodeOptionsLoading"
          :remote-method="loadProjectCodeOptions"
          placeholder="请选择 DCC 项目"
          @change="handleProjectCodeChange"
        >
          <el-option
            v-for="projectCode in projectCodeOptions"
            :key="projectCode.id"
            :label="formatProjectCodeOptionLabel(projectCode)"
            :value="projectCode.id"
          />
        </el-select>
        <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
          当前快照：{{ props.file?.productCode || '-' }} / {{ props.file?.productName || '-' }}
        </div>
      </el-form-item>
      <el-form-item label="产品编号" :error="metadataDialog.fieldErrors.dccProjectCodeId">
        <el-input
          v-model="metadataForm.productCode"
          disabled
          placeholder="选择 DCC 项目后自动生成"
        />
      </el-form-item>
      <el-form-item label="产品名称">
        <el-input
          v-model="metadataForm.productName"
          disabled
          placeholder="选择 DCC 项目后自动生成"
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
          v-if="selectedCategory?.directoryId"
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
        <el-alert
          v-else-if="metadataForm.categoryId"
          :closable="false"
          show-icon
          type="info"
          title="当前文件类别未绑定受控目录，系统将自动落位到未分类目录。"
        />
        <el-alert
          v-else
          :closable="false"
          show-icon
          type="info"
          title="请选择文件类别后查看目录范围。"
        />
      </el-form-item>
      <div class="metadata-impact-preview" data-testid="dcc-metadata-impact-preview">
        <div class="metadata-impact-preview__title">变更影响预览</div>
        <div class="metadata-impact-preview__grid">
          <div class="metadata-impact-preview__item">
            <span class="metadata-impact-preview__label">当前 DCC 项目</span>
            <span class="metadata-impact-preview__value">{{ currentProjectCodeImpactText }}</span>
          </div>
          <div class="metadata-impact-preview__item">
            <span class="metadata-impact-preview__label">目标 DCC 项目</span>
            <span class="metadata-impact-preview__value">{{ targetProjectCodeImpactText }}</span>
          </div>
          <div class="metadata-impact-preview__item">
            <span class="metadata-impact-preview__label">当前分类路径</span>
            <span class="metadata-impact-preview__value">{{ currentTaxonomyImpactText }}</span>
          </div>
          <div class="metadata-impact-preview__item">
            <span class="metadata-impact-preview__label">目标分类路径</span>
            <span class="metadata-impact-preview__value">{{ targetTaxonomyImpactText }}</span>
          </div>
          <div class="metadata-impact-preview__item">
            <span class="metadata-impact-preview__label">当前受控目录</span>
            <span class="metadata-impact-preview__value">{{ currentDirectoryImpactText }}</span>
          </div>
          <div class="metadata-impact-preview__item">
            <span class="metadata-impact-preview__label">受控浏览目录落位</span>
            <span class="metadata-impact-preview__value">{{ targetDirectoryImpactText }}</span>
          </div>
        </div>
        <div class="metadata-impact-preview__hint">
          保存后将同步更新 DCC 项目代码关联文档、受控浏览目录落位和修正追溯记录。
        </div>
      </div>
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
  updateControlledFileMetadata,
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
const UNCLASSIFIED_DIRECTORY_AUTO_TEXT = '未分类（自动落位）'

const metadataDialog = reactive({
  submitting: false,
  inlineError: '',
  fieldErrors: {} as Record<string, string>
})

const metadataForm = reactive({
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

const selectedProjectCode = computed(() =>
  projectCodeOptions.value.find((projectCode) => projectCode.id === metadataForm.dccProjectCodeId)
)

const directoryOptions = computed(() => {
  const category = selectedCategory.value
  if (!category) {
    return []
  }
  return collectDirectoryOptions(category.directoryId)
})

const selectedCategoryUsesUnclassifiedDirectory = computed(() =>
  Boolean(metadataForm.categoryId && selectedCategory.value && !selectedCategory.value.directoryId)
)

const trimToUndefined = (value: string) => {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

const trimToNull = (value: string) => {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

const formatProjectCodeOptionLabel = (projectCode: DccProjectCodeRespVO) =>
  [projectCode.projectName, projectCode.projectCode, projectCode.docControlNo].filter(Boolean).join(' / ')

const formatImpactPath = (items: Array<string | null | undefined>) => {
  const parts = items.map((item) => item?.trim()).filter((item): item is string => Boolean(item))
  return parts.length ? parts.join(' / ') : '-'
}

const resolveDirectoryPathById = (directoryId?: number) => {
  if (!directoryId) {
    return '-'
  }
  const nodes: string[] = []
  const visited = new Set<number>()
  let current = directoryById.value.get(directoryId)
  while (current?.id && !visited.has(current.id)) {
    visited.add(current.id)
    nodes.unshift(current.name)
    current = current.parentId ? directoryById.value.get(current.parentId) : undefined
  }
  return nodes.length ? nodes.join('/') : '-'
}

const currentProjectCodeImpactText = computed(() =>
  formatImpactPath([props.file?.productName, props.file?.productCode]) ||
  (props.file?.dccProjectCodeId ? `项目#${props.file.dccProjectCodeId}` : '-')
)

const targetProjectCodeImpactText = computed(() => {
  if (selectedProjectCode.value) {
    return formatProjectCodeOptionLabel(selectedProjectCode.value)
  }
  return formatImpactPath([metadataForm.productName, metadataForm.productCode])
})

const currentTaxonomyImpactText = computed(() =>
  formatImpactPath([
    props.file?.fileTypeLevel1,
    props.file?.fileTypeLevel2,
    props.file?.fileTypeLevel3,
    props.file?.fileTypeLevel4,
    props.file?.fileTypeLevel5
  ])
)

const targetTaxonomyImpactText = computed(() =>
  taxonomyPathNames.value.length
    ? taxonomyPathNames.value.join(' / ')
    : formatImpactPath([
        metadataForm.fileTypeLevel1,
        metadataForm.fileTypeLevel2,
        metadataForm.fileTypeLevel3,
        metadataForm.fileTypeLevel4,
        metadataForm.fileTypeLevel5
      ])
)

const selectedDirectoryOption = computed(() =>
  directoryOptions.value.find((item) => item.value === metadataForm.directoryId)
)

const currentDirectoryImpactText = computed(() => resolveDirectoryPathById(props.file?.directoryId))
const targetDirectoryImpactText = computed(() =>
  selectedCategoryUsesUnclassifiedDirectory.value
    ? UNCLASSIFIED_DIRECTORY_AUTO_TEXT
    : selectedDirectoryOption.value?.label || resolveDirectoryPathById(metadataForm.directoryId)
)

const applyDccProjectCodeProductNumber = () => {
  metadataForm.productCode = selectedProjectCode.value?.projectCode?.trim() || ''
  metadataForm.productName = selectedProjectCode.value?.projectName?.trim() || ''
}

const handleProjectCodeChange = () => {
  applyDccProjectCodeProductNumber()
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
      'DCC 项目加载失败，请查看错误提示后重试。'
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

const resetMetadataDialog = () => {
  metadataDialog.submitting = false
  metadataDialog.inlineError = ''
  metadataDialog.fieldErrors = {}
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
  if (!metadataForm.dccProjectCodeId || !metadataForm.productCode.trim()) {
    errors.dccProjectCodeId = '请选择包含项目代码的 DCC 项目'
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
  if (!selectedCategoryUsesUnclassifiedDirectory.value && !metadataForm.directoryId) {
    errors.directoryId = '请选择受控目录'
  } else if (
    !selectedCategoryUsesUnclassifiedDirectory.value &&
    !directoryOptions.value.some((item) => item.value === metadataForm.directoryId)
  ) {
    errors.directoryId = '请选择类别绑定范围内的受控目录'
  }
  metadataDialog.fieldErrors = errors
  metadataDialog.inlineError = Object.values(errors)[0] || ''
  return Object.keys(errors).length === 0
}

const buildMetadataPayload = (): ControlledFileMetadataUpdateReqVO => ({
  assignmentId: props.assignmentId,
  changeReason: trimToUndefined(metadataForm.changeReason),
  productMasterId: null,
  productName: trimToUndefined(selectedProjectCode.value?.projectName || metadataForm.productName),
  dccProjectCodeId: metadataForm.dccProjectCodeId || null,
  needTraining: metadataForm.needTraining,
  fileTypeTaxonomyId: metadataForm.fileTypeTaxonomyId || null,
  fileTypeLevel1: trimToNull(metadataForm.fileTypeLevel1),
  fileTypeLevel2: trimToNull(metadataForm.fileTypeLevel2),
  fileTypeLevel3: trimToNull(metadataForm.fileTypeLevel3),
  fileTypeLevel4: trimToNull(metadataForm.fileTypeLevel4),
  fileTypeLevel5: trimToNull(metadataForm.fileTypeLevel5),
  fileName: metadataForm.fileName.trim(),
  productCode: trimToUndefined(selectedProjectCode.value?.projectCode || metadataForm.productCode),
  fileNumber: metadataForm.fileNumber.trim(),
  categoryId: metadataForm.categoryId as number,
  directoryId: selectedCategoryUsesUnclassifiedDirectory.value ? null : metadataForm.directoryId || null
})

const resolveMetadataPermissionErrorMessage = (error: unknown, defaultMessage: string) => {
  const message = resolveReadSideErrorMessage(error, defaultMessage)
  if (
    message.includes('Only doc control can update controlled file metadata') ||
    message.includes('CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED') ||
    message.includes('1080000124')
  ) {
    return '当前账号未被后端识别为文控角色（doc_control）。请确认已分配文控角色并重新登录；如果刚调整过权限，请刷新 user_role_ids 权限缓存后重试。'
  }
  return message
}

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
    metadataDialog.inlineError = resolveMetadataPermissionErrorMessage(
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

.metadata-impact-preview {
  display: grid;
  gap: 10px;
  margin: 0 0 18px 96px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.metadata-impact-preview__title {
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.metadata-impact-preview__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.metadata-impact-preview__item {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.metadata-impact-preview__label {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.metadata-impact-preview__value {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metadata-impact-preview__hint {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

@media (max-width: 720px) {
  .metadata-impact-preview {
    margin-left: 0;
  }

  .metadata-impact-preview__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
