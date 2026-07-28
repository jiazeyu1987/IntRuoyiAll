<template>
  <ContentWrap>
    <div class="upload-header">
      <div class="text-18px font-600">{{ pageTitle }}</div>
      <el-tag v-if="isExternalReview" type="warning">外来文件评审</el-tag>
    </div>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      class="upload-form"
      label-width="86px"
      v-loading="submitLoading"
    >
      <div class="upload-workbench" data-testid="dcc-upload-single-page-workbench">
        <div class="upload-workbench__grid" data-testid="dcc-upload-single-page-grid">
          <section class="upload-section upload-section--scope" data-testid="dcc-upload-section-scope">
        <div class="upload-section__title">提交范围</div>
        <el-form-item v-if="!isExternalReview" label="DCC项目" prop="dccProjectCodeId">
          <el-select
            v-model="formData.dccProjectCodeId"
            class="!w-460px"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="projectCodeOptionsLoading"
            :remote-method="loadProjectCodeOptions"
            placeholder="请选择 DCC 项目"
            @visible-change="handleProjectCodeOptionsVisibleChange"
            @change="handleProjectCodeChange"
          >
            <el-option
              v-for="project in projectCodeOptions"
              :key="project.id"
              :label="formatProjectCodeOptionLabel(project)"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="文件分类" prop="fileTypeTaxonomyId">
          <div class="w-full">
            <el-cascader
              v-model="formData.fileTypeTaxonomyId"
              class="!w-560px"
              :options="fileTypeTaxonomyOptions"
              :props="fileTypeTaxonomyCascaderProps"
              clearable
              :disabled="fileTypeTaxonomiesLoading"
              filterable
              placeholder="请选择文件分类"
              @change="handleFileTypeTaxonomyChange"
            />
            <div v-if="selectedFileTypeTaxonomyPathLabel" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
              {{ selectedFileTypeTaxonomyPathLabel }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="文件类别" prop="categoryId">
          <el-select
            v-model="formData.categoryId"
            class="!w-360px"
            clearable
            filterable
            placeholder="请选择文件类别"
            @change="handleCategoryChange"
          >
            <el-option
              v-for="item in availableCategories"
              :key="item.id"
              :label="item.name"
              :value="item.id as number"
            />
            <template #empty>
              <div class="px-12px py-8px text-12px text-[var(--el-text-color-secondary)]">
                暂无已绑定提交目录的文件类别
              </div>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item v-if="uploadDirectoryTree" label="提交目录" prop="directoryId">
          <div class="w-full">
            <template v-if="uploadDirectoryTree.leafBinding">
              <div class="rounded-6px border border-[#dbe3ef] bg-[#fafcff] px-12px py-9px text-13px text-[#172033]">
                {{ uploadDirectoryTree.bindingDirectoryPath }}
              </div>
              <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
                当前绑定目录已经是最后一层目录，将直接提交到该目录。
              </div>
            </template>
            <template v-else>
              <el-cascader
                v-model="formData.directoryId"
                class="!w-560px"
                :options="uploadDirectoryTree.children"
                :props="directoryCascaderProps"
                clearable
                filterable
                placeholder="请选择绑定目录下的最后一层子目录"
              />
              <div v-if="selectedUploadDirectoryPath" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
                最终提交路径：{{ selectedUploadDirectoryPath }}
              </div>
              <div v-else class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
                请选择到最后一层叶子目录后再提交。
              </div>
            </template>
          </div>
        </el-form-item>
      </section>

          <section class="upload-section upload-section--file" data-testid="dcc-upload-section-file">
        <div class="upload-section__title">文件信息</div>
        <el-form-item label="文件名称" prop="fileName">
          <el-autocomplete
            v-model="formData.fileName"
            class="!w-420px"
            clearable
            :hide-loading="!uploadNameOptionsLoading"
            :fetch-suggestions="queryUploadNameSuggestions"
            :trigger-on-focus="Boolean(formData.categoryId)"
            placeholder="可选择历史文件名称，或直接输入新名称"
            @select="handleHistoryFileNameSelect"
            @input="handleFileNameInput"
            @clear="handleFileNameClear"
          >
            <template #default="{ item }">
              <div class="flex items-center justify-between gap-12px">
                <span class="truncate">{{ item.value }}</span>
                <span class="text-12px text-[var(--el-text-color-secondary)]">
                  当前版本：{{ item.currentVersionNo || '-' }}
                </span>
              </div>
            </template>
          </el-autocomplete>
          <div v-if="selectedHistoryVersion" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            已选择历史文件名称，将按升版提交；当前版本号 {{ selectedHistoryVersion }}，请按需要修改为更高版本。
          </div>
        </el-form-item>
        <el-form-item label="文件编号" prop="fileNumber">
          <el-input
            v-model="formData.fileNumber"
            class="!w-280px"
            placeholder="例如 SOP-001"
          />
        </el-form-item>
        <el-form-item v-if="formData.fileNumber" label="现行版本">
          <div
            data-testid="dcc-upload-current-version-panel"
            class="w-full rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
          >
            <el-skeleton v-if="currentVersionLookupLoading" :rows="2" animated />
            <template v-else-if="currentVersionInfo?.matched">
              <div class="font-600 text-[var(--el-text-color-primary)]">
                {{ currentVersionInfo.fileName || '-' }} · {{ currentVersionInfo.currentVersionNo || '-' }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                文件编号：{{ currentVersionInfo.fileNumber }}；状态：{{ currentVersionInfo.status || '-' }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                产品：{{ currentVersionInfo.productName || currentVersionInfo.productCode || '-' }}；
                修改中：{{ currentVersionInfo.modifying ? '是' : '否' }}
              </div>
              <el-alert
                v-if="currentVersionProjectionBlockReason"
                class="mt-8px"
                :closable="false"
                :title="currentVersionProjectionBlockReason"
                type="warning"
                show-icon
              />
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                原版本路径：{{ currentVersionInfo.originalFilePath || '-' }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                源文件路径：{{ currentVersionInfo.sourceFilePath || '-' }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                受控文件路径：{{ currentVersionInfo.stampedFilePath || currentVersionInfo.publishedFilePath || '-' }}
              </div>
            </template>
            <template v-else>
              未查询到同编号现行版本，将按新建规则校验。
            </template>
          </div>
        </el-form-item>
        <el-form-item label="产品编号" prop="productMasterId">
          <el-select
            v-model="formData.productMasterId"
            class="!w-420px"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="productOptionsLoading"
            :remote-method="loadProductOptions"
            :placeholder="isProductRequiredForSelectedCategory ? '请选择产品主数据' : '可不选择产品主数据'"
            @visible-change="handleProductOptionsVisibleChange"
            @change="handleProductMasterChange"
          >
            <el-option
              v-for="product in productOptions"
              :key="product.id"
              :label="formatProductOptionLabel(product)"
              :value="product.id"
            />
          </el-select>
          <div v-if="formData.productCode" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            DCC 产品编号：{{ formData.productCode }}
          </div>
          <div v-if="isProductRequiredForSelectedCategory" class="mt-6px text-12px text-[var(--el-color-danger)]">
            DHF/DMR 类别必须选择产品主数据
          </div>
        </el-form-item>
        <el-form-item label="版本号" prop="versionNo" :error="submitFieldErrors.versionNo">
          <el-input v-model="formData.versionNo" class="!w-220px" placeholder="例如 V1.0" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="formData.effectiveDate"
            class="!w-220px"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择生效日期"
          />
        </el-form-item>
        <el-form-item label="提交备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            class="!w-560px"
            placeholder="请输入本次受控文件提交说明"
          />
        </el-form-item>
      </section>

          <section class="upload-section upload-section--approval" data-testid="dcc-upload-section-approval">
        <div class="upload-section__title">审批要求</div>
        <el-form-item label="培训要求" prop="needTraining">
          <el-switch
            v-model="formData.needTraining"
            active-text="需要培训"
            inactive-text="无需培训"
          />
        </el-form-item>
        <el-form-item label="会签人员">
          <UserSelectV2
            v-model="formData.selectedSignoffUserIds"
            class="!w-560px"
            :multiple="true"
            placeholder="请选择会签人员"
          />
        </el-form-item>
      </section>

          <section class="upload-section upload-section--attachment" data-testid="dcc-upload-section-attachment">
        <div class="upload-section__title">附件上传</div>
        <el-form-item label="受控文件" prop="file">
          <div class="w-full">
            <el-upload
              ref="uploadRef"
              action="#"
              accept=".doc,.docx,.xls,.xlsx,.dwg,.sldprt,.sldasm,.slddrw"
              :auto-upload="false"
              :limit="1"
              :file-list="fileList"
              :on-change="handleFileChange"
              :before-remove="handleBeforeFileRemove"
              :on-remove="handleFileRemove"
              :on-exceed="handleFileExceed"
            >
              <el-button type="primary" plain :loading="uploadPreviewLoading">
                <Icon icon="ep:upload" class="mr-5px" />
                选择文件
              </el-button>
              <template #tip>
                <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
                  {{ EDITABLE_SOURCE_MESSAGE }}；图纸源文件需同步上传 PDF。
                </div>
              </template>
            </el-upload>
            <div
              v-if="previewUpload"
              class="mt-12px rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
            >
              <div class="font-600 text-[var(--el-text-color-primary)]">
                预览文件：{{ previewUpload.fileName }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                {{ previewUpload.contentType }} · {{ formatPreviewFileSize(previewUpload.fileSize) }}
              </div>
            </div>
            <div v-if="previewFileBlob && previewUpload" class="upload-preview-panel mt-12px">
              <div class="mb-8px text-15px font-600">提交前预览</div>
              <ProtectedPdfViewer
                :preview-blob="previewFileBlob"
                :preview-kind="previewUpload.previewKind || 'PDF'"
                :onlyoffice-base-url="previewUpload.onlyofficeBaseUrl"
                :preview-unavailable-reason="previewUpload.previewUnavailableReason"
                :title="previewUpload?.fileName || previewFileBlob?.name || '受控文件预览'"
                :watermark="previewUpload?.watermark || null"
              />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="图纸 PDF">
          <div class="w-full">
            <el-upload
              ref="drawingPdfUploadRef"
              action="#"
              accept=".pdf,application/pdf"
              :auto-upload="false"
              :limit="1"
              :file-list="drawingPdfFileList"
              :on-change="handleDrawingPdfChange"
              :before-remove="handleBeforeDrawingPdfRemove"
              :on-remove="handleDrawingPdfRemove"
              :on-exceed="handleDrawingPdfExceed"
            >
              <el-button plain :loading="uploadDrawingPdfLoading">
                <Icon icon="ep:document" class="mr-5px" />
                选择 PDF
              </el-button>
              <template #tip>
                <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
                  源文件为 DWG、SLDPRT、SLDASM、SLDDRW 时必填。
                </div>
              </template>
            </el-upload>
            <div
              v-if="drawingPdfUpload"
              class="mt-12px rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
            >
              <div class="font-600 text-[var(--el-text-color-primary)]">
                图纸 PDF：{{ drawingPdfUpload.fileName }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                {{ drawingPdfUpload.contentType }} · {{ formatPreviewFileSize(drawingPdfUpload.fileSize) }}
              </div>
            </div>
          </div>
        </el-form-item>
      </section>
        </div>

        <section class="upload-submit-bar" data-testid="dcc-upload-section-submit">
          <el-form-item>
            <el-button type="primary" :loading="submitLoading" @click="submitForm">
              <Icon icon="ep:promotion" class="mr-5px" />
              {{ submitButtonText }}
            </el-button>
          </el-form-item>
        </section>
      </div>
    </el-form>
  </ContentWrap>
</template>

<script lang="ts" setup>
import type { FormRules, UploadProps, UploadUserFile } from 'element-plus'
import { handleTree } from '@/utils/tree'
import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import { getFileCategoryList } from '@/api/dcc/controlledFile/fileCategories'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  getFileTypeTaxonomyList,
  type DccFileTypeTaxonomyVO
} from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import {
  cleanupControlledFileUploadSession,
  createControlledFileUploadSessionId,
  DCC_PRODUCT_STATUS_ENABLE,
  getDccProductOptions,
  getControlledFileCurrentVersion,
  getControlledFileUploadRevisionCandidates,
  getControlledFileUploadDirectoryTree,
  getControlledFileUploadNameOptions,
  submitControlledFile,
  uploadControlledFilePreview,
  type DccControlledFileProductOptionVO,
  type ControlledFileCurrentVersionRespVO,
  type ControlledFileVO,
  type ControlledFileUploadDirectoryNodeVO,
  type ControlledFileUploadDirectoryTreeVO,
  type ControlledFileUploadNameOptionVO,
  type ControlledFileUploadRespVO
} from '@/api/dcc/controlledFile/workflow'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import {
  DCC_ACTION_PROJECTION_MISSING_REASON,
  hasDccControlledFileActionProjection,
  resolveDccActionProjectionReadonlyReason
} from '../shared/lifecycle'
import {
  applySubmitFailureFeedback,
  buildSubmitFailureFeedback,
  clearSubmitFieldErrors,
  createUploadSubmitterService,
  EDITABLE_SOURCE_MESSAGE,
  formatPreviewFileSize,
  resolveUploadErrorMessage,
  isDccProductRequiredForCategoryCode,
  validateDrawingPdfUpload,
  validateControlledFileSelection,
  validateSingleUploadFileSelection,
  validateProductMasterSelection,
  type UploadFormDraft
} from './submitter'

defineOptions({ name: 'DccControlledFileUpload' })

const ProtectedPdfViewer = defineAsyncComponent(() => import('../view/index.vue'))

const route = useRoute()
const router = useRouter()
const message = useMessage()

interface UploadNameSuggestionItem {
  value: string
  currentVersionNo?: string | null
  controlledFileId?: number | null
  fileNumber?: string | null
}

const formRef = ref()
const uploadRef = ref()
const drawingPdfUploadRef = ref()
const categories = ref<ControlledFileCategoryVO[]>([])
const projectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const fileTypeTaxonomies = ref<DccFileTypeTaxonomyVO[]>([])
const selectedRevisionCandidate = ref<ControlledFileVO>()
const uploadNameOptions = ref<ControlledFileUploadNameOptionVO[]>([])
const productOptions = ref<DccControlledFileProductOptionVO[]>([])
const uploadDirectoryTree = ref<ControlledFileUploadDirectoryTreeVO>()
const currentVersionInfo = ref<ControlledFileCurrentVersionRespVO>()
const fileList = ref<UploadUserFile[]>([])
const drawingPdfFileList = ref<UploadUserFile[]>([])
const previewUpload = ref<ControlledFileUploadRespVO>()
const drawingPdfUpload = ref<ControlledFileUploadRespVO>()
const previewFileBlob = ref<File | null>(null)
const submitLoading = ref(false)
const uploadPreviewLoading = ref(false)
const uploadDrawingPdfLoading = ref(false)
const uploadNameOptionsLoading = ref(false)
const currentVersionLookupLoading = ref(false)
const productOptionsLoading = ref(false)
const projectCodeOptionsLoading = ref(false)
const fileTypeTaxonomiesLoading = ref(false)
const selectedHistoryVersion = ref('')
const selectedHistoryFileName = ref('')
const uploadSessionId = createControlledFileUploadSessionId()
const uploadSubmitted = ref(false)
const submitFieldErrors = reactive({
  versionNo: ''
})

const resolveProcessTypeByRoute = () =>
  route.path.includes('/external') ? 'EXTERNAL_REVIEW' : 'CONTROLLED_FILE'
const isExternalReview = computed(() => resolveProcessTypeByRoute() === 'EXTERNAL_REVIEW')
const pageTitle = computed(() => (isExternalReview.value ? '外来文件评审' : '受控文件提交'))
const submitButtonText = computed(() => (isExternalReview.value ? '提交评审' : '提交审批'))
const selectedCategory = computed(() =>
  categories.value.find((category) => category.id === formData.categoryId)
)
const selectedProjectCode = computed(() =>
  projectCodeOptions.value.find((project) => project.id === formData.dccProjectCodeId)
)
const categoryDirectoryBindingMessage = '当前文件类别未绑定提交目录，请先在 DCC 文件类别维护目录绑定'
const categoryUploadPermissionMessage = '当前用户没有该文件类别的上传权限，请选择有上传权限的文件类别。'
const availableCategories = computed(() =>
  categories.value.filter(
    (category) => category.active && Boolean(category.directoryId) && category.canUpload !== false
  )
)
const selectedCategoryDirectoryBound = computed(() => Boolean(selectedCategory.value?.directoryId))
const isProductRequiredForSelectedCategory = computed(() =>
  isDccProductRequiredForCategoryCode(selectedCategory.value?.code)
)

const uploadSubmitterService = createUploadSubmitterService({
  uploadPreview: uploadControlledFilePreview,
  submit: submitControlledFile
})

const formData = reactive<UploadFormDraft>({
  categoryId: null,
  directoryId: null,
  fileName: '',
  fileNumber: '',
  productMasterId: null,
  productCode: '',
  dccProjectCodeId: null,
  fileTypeTaxonomyId: null,
  revisionTargetControlledFileId: null,
  needTraining: false,
  selectedSignoffUserIds: [],
  processType: resolveProcessTypeByRoute(),
  changeType: 'NEW',
  versionNo: '',
  effectiveDate: '',
  remark: ''
})

interface FileTypeTaxonomyPathInfo {
  id: number
  names: string[]
}

const activeFileTypeTaxonomyRows = computed(() =>
  fileTypeTaxonomies.value
    .filter((row) => row.id && row.active)
    .map((row) => ({ ...row, children: undefined }))
)

const fileTypeTaxonomyOptions = computed(
  () => handleTree(activeFileTypeTaxonomyRows.value.map((row) => ({ ...row }))) as DccFileTypeTaxonomyVO[]
)

const fileTypeTaxonomyPathMap = computed(() => {
  const pathMap = new Map<number, FileTypeTaxonomyPathInfo>()
  const sortedRows = [...activeFileTypeTaxonomyRows.value].sort(
    (left, right) => (left.levelNo || 0) - (right.levelNo || 0)
  )
  sortedRows.forEach((row) => {
    if (!row.id) {
      return
    }
    const parentId = row.parentId || 0
    const parentPath = parentId > 0 ? pathMap.get(parentId) : undefined
    if (parentId > 0 && !parentPath) {
      return
    }
    pathMap.set(row.id, {
      id: row.id,
      names: [...(parentPath?.names || []), row.name]
    })
  })
  return pathMap
})

const selectedFileTypeTaxonomyPath = computed(() => {
  if (!formData.fileTypeTaxonomyId) {
    return undefined
  }
  return fileTypeTaxonomyPathMap.value.get(Number(formData.fileTypeTaxonomyId))
})

const selectedFileTypeTaxonomyPathLabel = computed(
  () => selectedFileTypeTaxonomyPath.value?.names.join(' / ') || ''
)

const isFileTypeTaxonomyDepthValid = computed(
  () => (selectedFileTypeTaxonomyPath.value?.names.length || 0) >= 3
)

const formatProjectCodeOptionLabel = (project: DccProjectCodeRespVO) =>
  [project.projectName, project.projectCode, project.docControlNo].filter(Boolean).join(' · ')

const formRules = reactive<FormRules>({
  dccProjectCodeId: [{ required: true, message: '请选择 DCC 项目', trigger: 'change' }],
  fileTypeTaxonomyId: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (isExternalReview.value) {
          callback()
          return
        }
        if (!value) {
          callback(new Error('请选择文件分类'))
          return
        }
        const path = fileTypeTaxonomyPathMap.value.get(Number(value))
        if (!path || path.names.length < 3) {
          callback(new Error('请选择至少三级文件分类'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  categoryId: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (!value) {
          callback(new Error('请选择文件类别'))
          return
        }
        const category = categories.value.find((item) => item.id === Number(value))
        if (!category?.directoryId) {
          callback(new Error(categoryDirectoryBindingMessage))
          return
        }
        if (category.canUpload === false) {
          callback(new Error(categoryUploadPermissionMessage))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  directoryId: [{ required: true, message: '请选择最终提交目录', trigger: 'change' }],
  fileName: [{ required: true, message: '请输入文件名称', trigger: 'blur' }],
  fileNumber: [{ required: true, message: '请输入文件编号', trigger: 'blur' }],
  versionNo: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
})

const directoryCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: false,
  checkStrictly: false
} as const

const fileTypeTaxonomyCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: false,
  checkStrictly: true
} as const

const currentVersionProjectionBlockReason = computed(() => {
  const currentVersion = currentVersionInfo.value
  if (!currentVersion?.matched) {
    return ''
  }
  if (!hasDccControlledFileActionProjection(currentVersion)) {
    return DCC_ACTION_PROJECTION_MISSING_REASON
  }
  if (currentVersion.actionProjection?.actionLocked) {
    return resolveDccActionProjectionReadonlyReason(currentVersion)
  }
  return ''
})

let currentVersionLookupTimer: ReturnType<typeof setTimeout> | undefined
let currentVersionLookupSeq = 0
let revisionTargetLookupSeq = 0
let productAutofillSeq = 0

const clearCurrentVersionInfo = () => {
  currentVersionInfo.value = undefined
}

const resetUploadNameLinkage = (clearVersionNo: boolean) => {
  selectedHistoryFileName.value = ''
  selectedHistoryVersion.value = ''
  formData.changeType = 'NEW'
  clearRevisionTargetSelection()
  if (clearVersionNo) {
    formData.versionNo = ''
  }
}

const resetUploadNameContext = (clearFileName: boolean) => {
  uploadNameOptions.value = []
  uploadNameOptionsLoading.value = false
  if (clearFileName) {
    formData.fileName = ''
  }
  resetUploadNameLinkage(true)
  clearCurrentVersionInfo()
}

const resetUploadDirectoryContext = () => {
  uploadDirectoryTree.value = undefined
  formData.directoryId = null
}

const resetSelectedPreview = () => {
  previewUpload.value = undefined
  fileList.value = []
  previewFileBlob.value = null
}

const resetDrawingPdfUpload = () => {
  drawingPdfUpload.value = undefined
  drawingPdfFileList.value = []
}

const hasTemporaryUploadState = () =>
  Boolean(previewUpload.value?.uploadTicket || drawingPdfUpload.value?.uploadTicket)

const resolveCurrentUploadCleanupRequestId = () =>
  previewUpload.value?.requestId || drawingPdfUpload.value?.requestId

const cleanupCurrentUploadSession = async (showSuccess = false) => {
  if (uploadSubmitted.value || !hasTemporaryUploadState()) {
    return true
  }
  try {
    const status = await cleanupControlledFileUploadSession(
      uploadSessionId,
      resolveCurrentUploadCleanupRequestId()
    )
    if (showSuccess && (status.cleanedCount ?? 0) > 0) {
      message.success('已清理本次上传临时文件')
    }
    return true
  } catch (error) {
    message.error(resolveUploadErrorMessage(error, '临时文件清理失败，请处理后再继续'))
    return false
  }
}

const buildUploadPreviewContext = () => {
  if (!formData.categoryId) {
    throw new Error('请先选择文件类别')
  }
  return {
    categoryId: formData.categoryId,
    sessionId: uploadSessionId
  }
}

const loadProjectCodeOptions = async (keyword = '') => {
  projectCodeOptionsLoading.value = true
  try {
    const page = await getProjectCodePage({
      pageNo: 1,
      pageSize: 50,
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      keyword: keyword.trim() || undefined
    })
    projectCodeOptions.value = page.list || []
  } catch (error) {
    projectCodeOptions.value = []
    message.error(resolveUploadErrorMessage(error, 'DCC 项目加载失败，请查看错误提示后重试'))
  } finally {
    projectCodeOptionsLoading.value = false
  }
}

const handleProjectCodeOptionsVisibleChange = async (visible: boolean) => {
  if (!visible || projectCodeOptions.value.length || projectCodeOptionsLoading.value) {
    return
  }
  await loadProjectCodeOptions()
}

const loadFileTypeTaxonomies = async () => {
  fileTypeTaxonomiesLoading.value = true
  try {
    fileTypeTaxonomies.value = await getFileTypeTaxonomyList()
  } catch (error) {
    fileTypeTaxonomies.value = []
    message.error(resolveUploadErrorMessage(error, '文件分类加载失败，请查看错误提示后重试'))
  } finally {
    fileTypeTaxonomiesLoading.value = false
  }
}

const clearRevisionTargetSelection = () => {
  selectedRevisionCandidate.value = undefined
  formData.revisionTargetControlledFileId = null
}

const normalizeHistoryFileName = (value?: string | null) => (value || '').trim()

const applyResolvedRevisionTarget = (row: ControlledFileVO) => {
  selectedRevisionCandidate.value = row
  formData.revisionTargetControlledFileId = row.id
  if (row.fileNumber) {
    formData.fileNumber = row.fileNumber
  }
  formData.productMasterId = row.productMasterId ?? null
  formData.productCode = row.productCode || ''
}

const applyUploadNameOptionRevisionTarget = (item: UploadNameSuggestionItem) => {
  if (!item.controlledFileId) {
    return false
  }
  selectedRevisionCandidate.value = undefined
  formData.revisionTargetControlledFileId = item.controlledFileId
  if (item.fileNumber) {
    formData.fileNumber = item.fileNumber
  }
  return true
}

const resolveHistoryRevisionTarget = async (fileName: string) => {
  const normalizedFileName = normalizeHistoryFileName(fileName)
  const requestSeq = ++revisionTargetLookupSeq
  clearRevisionTargetSelection()
  if (
    !normalizedFileName ||
    !formData.dccProjectCodeId ||
    !formData.fileTypeTaxonomyId ||
    !isFileTypeTaxonomyDepthValid.value
  ) {
    return
  }
  try {
    const page = await getControlledFileUploadRevisionCandidates({
      dccProjectCodeId: formData.dccProjectCodeId,
      fileTypeTaxonomyId: formData.fileTypeTaxonomyId,
      keyword: normalizedFileName,
      pageNo: 1,
      pageSize: 20
    })
    if (
      requestSeq !== revisionTargetLookupSeq ||
      formData.changeType !== 'REVISION' ||
      normalizeHistoryFileName(formData.fileName) !== normalizedFileName ||
      selectedHistoryFileName.value !== normalizedFileName
    ) {
      return
    }
    const exactMatch = (page.list || []).find(
      (row) => normalizeHistoryFileName(row.fileName || row.title) === normalizedFileName
    )
    if (exactMatch?.id) {
      applyResolvedRevisionTarget(exactMatch)
    }
  } catch (error) {
    if (requestSeq === revisionTargetLookupSeq) {
      clearRevisionTargetSelection()
    }
    message.error(resolveUploadErrorMessage(error, '历史文件升版目标解析失败，请查看错误提示后重试'))
  }
}

const handleProjectCodeChange = async () => {
  applyProductMasterSelection(undefined)
  resetUploadNameLinkage(Boolean(selectedHistoryFileName.value || selectedHistoryVersion.value))
  await tryAutofillProductFromSelectedProject()
}

const handleFileTypeTaxonomyChange = async () => {
  resetUploadNameLinkage(Boolean(selectedHistoryFileName.value || selectedHistoryVersion.value))
  await formRef.value?.validateField?.('fileTypeTaxonomyId').catch(() => undefined)
}

const formatProductOptionLabel = (product: DccControlledFileProductOptionVO) =>
  `${product.dccProductCode} · ${product.nameCn} · ${product.productCode}`

const applyProductMasterSelection = (product: DccControlledFileProductOptionVO | undefined) => {
  formData.productMasterId = product?.id ?? null
  formData.productCode = product?.dccProductCode || ''
}

const normalizeProductAutofillKeyword = (value?: string | null) => value?.trim() || ''

const resolveProjectProductAutofillKeywords = (project: DccProjectCodeRespVO | undefined) =>
  Array.from(
    new Set(
      [project?.projectName, project?.projectCode, project?.docControlNo]
        .map(normalizeProductAutofillKeyword)
        .filter(Boolean)
    )
  )

const uniqueProductOptionsById = (options: ReadonlyArray<DccControlledFileProductOptionVO>) =>
  Array.from(new Map(options.map((option) => [option.id, option])).values())

const tryAutofillProductFromSelectedProject = async () => {
  if (!isProductRequiredForSelectedCategory.value || formData.productMasterId) {
    return
  }
  const keywords = resolveProjectProductAutofillKeywords(selectedProjectCode.value)
  if (!keywords.length) {
    return
  }
  const requestSeq = ++productAutofillSeq
  productOptionsLoading.value = true
  try {
    const keywordResults = await Promise.all(
      keywords.map((keyword) =>
        getDccProductOptions({
          status: DCC_PRODUCT_STATUS_ENABLE,
          requireDccProductCode: true,
          keyword
        })
      )
    )
    if (requestSeq !== productAutofillSeq || formData.productMasterId || !isProductRequiredForSelectedCategory.value) {
      return
    }
    const matchingProducts = uniqueProductOptionsById(keywordResults.flat())
    productOptions.value = uniqueProductOptionsById([...matchingProducts, ...productOptions.value])
    if (matchingProducts.length === 1) {
      applyProductMasterSelection(matchingProducts[0])
      return
    }
    message.warning('未能根据 DCC 项目唯一匹配产品主数据，请手动选择产品主数据')
  } catch (error) {
    if (requestSeq === productAutofillSeq) {
      message.error(resolveUploadErrorMessage(error, '产品主数据自动带出失败，请查看错误提示后手动选择'))
    }
  } finally {
    if (requestSeq === productAutofillSeq) {
      productOptionsLoading.value = false
    }
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
  } catch (error) {
    productOptions.value = []
    message.error(resolveUploadErrorMessage(error, '产品主数据加载失败，请查看错误提示后重试'))
  } finally {
    productOptionsLoading.value = false
  }
}

const handleProductMasterChange = (productId: number | undefined) => {
  const product = productOptions.value.find((item) => item.id === productId)
  applyProductMasterSelection(product)
}

const handleProductOptionsVisibleChange = async (visible: boolean) => {
  if (!visible || productOptions.value.length || productOptionsLoading.value) {
    return
  }
  await loadProductOptions()
}

const loadBaseData = async () => {
  const [categoryList] = await Promise.all([
    getFileCategoryList(),
    loadFileTypeTaxonomies(),
    loadProjectCodeOptions()
  ])
  categories.value = categoryList.filter((item) => item.active)
}

const loadUploadNameOptions = async (categoryId: number) => {
  uploadNameOptionsLoading.value = true
  try {
    uploadNameOptions.value = await getControlledFileUploadNameOptions(categoryId)
  } catch (error) {
    uploadNameOptions.value = []
    message.error(resolveUploadErrorMessage(error, '历史文件名称加载失败，请查看错误提示后重试'))
  } finally {
    uploadNameOptionsLoading.value = false
  }
}

const loadUploadDirectoryTree = async (categoryId: number) => {
  try {
    const tree = await getControlledFileUploadDirectoryTree(categoryId)
    uploadDirectoryTree.value = tree
    formData.directoryId = tree.leafBinding ? tree.bindingDirectoryId : null
  } catch (error) {
    uploadDirectoryTree.value = undefined
    formData.directoryId = null
    message.error(resolveUploadErrorMessage(error, '上传目录加载失败，请查看错误提示后重试'))
  }
}

const buildUploadDirectoryPathMap = (tree: ControlledFileUploadDirectoryTreeVO | undefined) => {
  const pathMap = new Map<number, string>()
  if (!tree) {
    return pathMap
  }
  const walk = (nodes: ControlledFileUploadDirectoryNodeVO[], parentPath: string) => {
    nodes.forEach((node) => {
      const currentPath = `${parentPath}/${node.name}`
      pathMap.set(node.id, currentPath)
      if (node.children?.length) {
        walk(node.children, currentPath)
      }
    })
  }
  if (tree.leafBinding) {
    pathMap.set(tree.bindingDirectoryId, tree.bindingDirectoryPath)
  }
  walk(tree.children || [], tree.bindingDirectoryPath)
  return pathMap
}

const uploadDirectoryPathMap = computed(() => buildUploadDirectoryPathMap(uploadDirectoryTree.value))

const selectedUploadDirectoryPath = computed(() => {
  if (!formData.directoryId) {
    return ''
  }
  return uploadDirectoryPathMap.value.get(formData.directoryId) || ''
})

const loadCurrentVersionByFileNumber = async () => {
  const fileNumber = formData.fileNumber.trim()
  const requestSeq = ++currentVersionLookupSeq
  if (!fileNumber) {
    clearCurrentVersionInfo()
    return
  }
  currentVersionLookupLoading.value = true
  try {
    const info = await getControlledFileCurrentVersion(fileNumber)
    if (requestSeq !== currentVersionLookupSeq) {
      return
    }
    currentVersionInfo.value = info
    if (info.matched) {
      if (!formData.fileName && info.fileName) {
        formData.fileName = info.fileName
      }
      if (info.productMasterId) {
        formData.productMasterId = info.productMasterId
      }
      if (info.productCode) {
        formData.productCode = info.productCode
      }
    }
  } catch (error) {
    if (requestSeq === currentVersionLookupSeq) {
      clearCurrentVersionInfo()
    }
    message.error(resolveUploadErrorMessage(error, '现行版本信息查询失败，请查看错误提示后重试'))
  } finally {
    if (requestSeq === currentVersionLookupSeq) {
      currentVersionLookupLoading.value = false
    }
  }
}

const queryUploadNameSuggestions = (
  queryString: string,
  callback: (items: UploadNameSuggestionItem[]) => void
) => {
  if (!formData.categoryId) {
    callback([])
    return
  }
  const keyword = queryString.trim().toLowerCase()
  const suggestions = uploadNameOptions.value
    .filter((item) => !keyword || item.fileName.toLowerCase().includes(keyword))
    .map((item) => ({
      value: item.fileName,
      currentVersionNo: item.currentVersionNo,
      controlledFileId: item.controlledFileId,
      fileNumber: item.fileNumber
    }))
  callback(suggestions)
}

const handleCategoryChange = async () => {
  if (!(await cleanupCurrentUploadSession())) {
    return
  }
  clearSubmitFieldErrors(submitFieldErrors)
  clearCurrentVersionInfo()
  resetUploadNameContext(true)
  resetUploadDirectoryContext()
  resetSelectedPreview()
  resetDrawingPdfUpload()
  if (formData.categoryId) {
    if (!selectedCategoryDirectoryBound.value) {
      message.warning(categoryDirectoryBindingMessage)
      return
    }
    await Promise.all([
      loadUploadNameOptions(formData.categoryId),
      loadUploadDirectoryTree(formData.categoryId)
    ])
    await tryAutofillProductFromSelectedProject()
  }
}

const handleHistoryFileNameSelect = async (item: UploadNameSuggestionItem) => {
  selectedHistoryFileName.value = item.value
  selectedHistoryVersion.value = item.currentVersionNo?.trim() || ''
  formData.fileName = item.value
  formData.changeType = 'REVISION'
  formData.versionNo = selectedHistoryVersion.value
  clearSubmitFieldErrors(submitFieldErrors)
  if (!applyUploadNameOptionRevisionTarget(item)) {
    await resolveHistoryRevisionTarget(item.value)
  }
}

const handleFileNameInput = (value: string) => {
  const normalized = value.trim()
  if (!normalized) {
    resetUploadNameLinkage(Boolean(selectedHistoryFileName.value || selectedHistoryVersion.value))
    return
  }
  if (!selectedHistoryFileName.value || normalized !== selectedHistoryFileName.value) {
    resetUploadNameLinkage(Boolean(selectedHistoryFileName.value || selectedHistoryVersion.value))
  }
}

const handleFileNameClear = () => {
  formData.fileName = ''
  resetUploadNameLinkage(Boolean(selectedHistoryFileName.value || selectedHistoryVersion.value))
  clearSubmitFieldErrors(submitFieldErrors)
}

const handleFileExceed: UploadProps['onExceed'] = () => {
  message.error('只允许上传一个文件')
}

const handleFileChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  const validation = validateControlledFileSelection(
    uploadFiles.map((item) => ({
      name: item.name,
      type: item.raw?.type
    }))
  )

  if (!validation.valid) {
    message.error(validation.message || '文件校验失败')
    uploadRef.value?.clearFiles()
    resetSelectedPreview()
    return
  }
  if (!file.raw) {
    return
  }

  clearSubmitFieldErrors(submitFieldErrors)
  fileList.value = uploadFiles.slice(-1)
  previewUpload.value = undefined
  previewFileBlob.value = file.raw as File

  uploadPreviewLoading.value = true
  try {
    previewUpload.value = await uploadSubmitterService.uploadPreview(
      file.raw as File,
      'SOURCE',
      buildUploadPreviewContext()
    )
  } catch (error) {
    previewUpload.value = undefined
    previewFileBlob.value = null
    message.error(resolveUploadErrorMessage(error, '文件预览上传失败，请查看错误提示后重试'))
  } finally {
    uploadPreviewLoading.value = false
  }
}

const handleBeforeFileRemove: UploadProps['beforeRemove'] = async () => {
  return await cleanupCurrentUploadSession(true)
}

const handleFileRemove: UploadProps['onRemove'] = () => {
  resetSelectedPreview()
  resetDrawingPdfUpload()
  clearSubmitFieldErrors(submitFieldErrors)
}

const handleDrawingPdfExceed: UploadProps['onExceed'] = () => {
  message.error('只允许上传一个 PDF 文件')
}

const handleDrawingPdfChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  const validation = validateSingleUploadFileSelection(
    uploadFiles.map((item) => ({
      name: item.name,
      type: item.raw?.type
    }))
  )

  if (!validation.valid) {
    message.error(validation.message || '图纸 PDF 校验失败')
    drawingPdfUploadRef.value?.clearFiles()
    resetDrawingPdfUpload()
    return
  }
  if (!file.raw) {
    return
  }
  const isPdf = file.raw.type === 'application/pdf' || /\.pdf$/i.test(file.name)
  if (!isPdf) {
    message.error('图纸文件必须上传 PDF 格式')
    drawingPdfUploadRef.value?.clearFiles()
    resetDrawingPdfUpload()
    return
  }

  drawingPdfFileList.value = uploadFiles.slice(-1)
  drawingPdfUpload.value = undefined
  uploadDrawingPdfLoading.value = true
  try {
    drawingPdfUpload.value = await uploadSubmitterService.uploadPreview(
      file.raw as File,
      'DRAWING_PDF',
      buildUploadPreviewContext()
    )
  } catch (error) {
    drawingPdfUpload.value = undefined
    message.error(resolveUploadErrorMessage(error, '图纸 PDF 上传失败，请查看错误提示后重试'))
  } finally {
    uploadDrawingPdfLoading.value = false
  }
}

const handleBeforeDrawingPdfRemove: UploadProps['beforeRemove'] = async () => {
  if (!previewUpload.value) {
    return await cleanupCurrentUploadSession(true)
  }
  return true
}

const handleDrawingPdfRemove: UploadProps['onRemove'] = () => {
  resetDrawingPdfUpload()
}

const submitForm = async () => {
  clearSubmitFieldErrors(submitFieldErrors)
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (currentVersionProjectionBlockReason.value) {
    message.warning(currentVersionProjectionBlockReason.value)
    return
  }
  if (currentVersionInfo.value?.modifying) {
    message.warning('同编号文件已有未完成流程，当前不可重复提交')
    return
  }
  if (!isExternalReview.value && formData.changeType === 'REVISION' && !formData.revisionTargetControlledFileId) {
    message.warning('请选择历史文件名称后再升版')
    return
  }
  if (!uploadDirectoryTree.value) {
    message.warning('请先选择文件类别并完成目录加载')
    return
  }
  if (!formData.directoryId) {
    message.warning(
      uploadDirectoryTree.value.leafBinding
        ? '当前绑定目录尚未就绪，请刷新后重试'
        : '请选择绑定目录下的最后一层子目录'
    )
    return
  }
  if (!previewUpload.value) {
    message.warning('请先选择并完成文件预览上传')
    return
  }
  const productMasterValidation = validateProductMasterSelection(
    formData.productMasterId,
    formData.productCode,
    isProductRequiredForSelectedCategory.value
  )
  if (!productMasterValidation.valid) {
    message.warning(productMasterValidation.message || '产品主数据校验失败')
    return
  }
  const drawingPdfValidation = validateDrawingPdfUpload(previewUpload.value, drawingPdfUpload.value)
  if (!drawingPdfValidation.valid) {
    message.warning(drawingPdfValidation.message || '图纸 PDF 校验失败')
    return
  }

  submitLoading.value = true
  try {
    await uploadSubmitterService.submit(formData, previewUpload.value, drawingPdfUpload.value)
    uploadSubmitted.value = true
    message.success(isExternalReview.value ? '外来文件评审已提交审批' : '受控文件已提交审批')
    await router.push({ name: 'DccControlledFileBrowser' })
  } catch (error) {
    const feedback = buildSubmitFailureFeedback(error, '受控文件提交失败，请查看错误提示后重试')
    applySubmitFailureFeedback(submitFieldErrors, feedback)
    message.error(feedback.message)
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => formData.versionNo,
  () => {
    if (submitFieldErrors.versionNo) {
      clearSubmitFieldErrors(submitFieldErrors)
    }
  }
)

watch(
  () => formData.fileNumber,
  () => {
    clearSubmitFieldErrors(submitFieldErrors)
    if (
      selectedRevisionCandidate.value &&
      formData.fileNumber.trim() !== (selectedRevisionCandidate.value.fileNumber || '').trim()
    ) {
      clearRevisionTargetSelection()
    }
    if (currentVersionLookupTimer) {
      clearTimeout(currentVersionLookupTimer)
    }
    currentVersionLookupTimer = setTimeout(loadCurrentVersionByFileNumber, 300)
  }
)

watch(
  () => formData.changeType,
  (changeType) => {
    if (changeType !== 'REVISION') {
      clearRevisionTargetSelection()
    }
  }
)

watch(
  () => route.fullPath,
  () => {
    formData.processType = resolveProcessTypeByRoute()
  }
)

onMounted(() => {
  loadBaseData()
})

onBeforeRouteLeave(async () => {
  if (await cleanupCurrentUploadSession()) {
    return true
  }
  return false
})

onBeforeUnmount(() => {
  if (currentVersionLookupTimer) {
    clearTimeout(currentVersionLookupTimer)
  }
  previewFileBlob.value = null
})
</script>

<style scoped>
.upload-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.upload-form {
  min-width: 0;
}

.upload-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.upload-workbench__grid {
  display: grid;
  grid-template-areas:
    'scope approval'
    'file attachment';
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px 16px;
  align-items: start;
  min-width: 0;
}

.upload-section {
  min-width: 0;
  padding: 14px 16px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.upload-section--scope {
  grid-area: scope;
}

.upload-section--file {
  grid-area: file;
}

.upload-section--approval {
  grid-area: approval;
}

.upload-section--attachment {
  grid-area: attachment;
}

.upload-section__title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.upload-submit-bar {
  display: flex;
  justify-content: flex-end;
  padding: 10px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.upload-preview-panel {
  max-height: 420px;
  overflow: auto;
}

.upload-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.upload-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.upload-form :deep(.el-form-item__label) {
  min-height: 32px;
  color: #263247;
  font-size: 13px;
  line-height: 32px;
  padding-right: 10px;
}

.upload-form :deep(.el-form-item__content) {
  min-width: 0;
}

.upload-form :deep(.el-select),
.upload-form :deep(.el-cascader),
.upload-form :deep(.el-autocomplete),
.upload-form :deep(.el-input),
.upload-form :deep(.el-textarea),
.upload-form :deep(.el-date-editor) {
  width: 100% !important;
  max-width: 100%;
}

.upload-form :deep(.el-input__wrapper),
.upload-form :deep(.el-select__wrapper),
.upload-form :deep(.el-textarea__inner) {
  min-height: 32px;
  border-radius: 6px;
}

.upload-submit-bar :deep(.el-form-item),
.upload-submit-bar :deep(.el-form-item__content) {
  margin: 0 !important;
}

@media (max-width: 1280px) {
  .upload-workbench__grid {
    grid-template-areas:
      'scope'
      'file'
      'approval'
      'attachment';
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
