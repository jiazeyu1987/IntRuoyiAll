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
          <div
            class="upload-workbench__column upload-workbench__column--main"
            data-testid="dcc-upload-left-column"
          >
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
          <el-alert
            v-if="projectCodeOptionsError"
            class="mt-8px !w-560px"
            type="warning"
            :closable="false"
            show-icon
            :title="projectCodeOptionsError"
          />
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
            <el-alert
              v-if="fileTypeTaxonomyOptionsError"
              class="mt-8px !w-560px"
              type="warning"
              :closable="false"
              show-icon
              :title="fileTypeTaxonomyOptionsError"
            />
          </div>
        </el-form-item>
        <el-form-item v-if="isExternalReview" label="文件类别" prop="categoryId">
          <div class="w-full">
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
                  {{ categorySelectEmptyText }}
                </div>
              </template>
            </el-select>
            <el-alert
              v-if="categoryPermissionPreflightMessage"
              class="mt-8px !w-560px"
              type="warning"
              :closable="false"
              show-icon
              :title="categoryPermissionPreflightMessage"
            />
          </div>
        </el-form-item>
        <el-form-item v-else label="文件类别" prop="categoryId">
          <div class="w-full">
            <div
              data-testid="dcc-upload-category-leaf-display"
              class="!w-360px rounded-6px border border-[#dbe3ef] bg-[#f8fafc] px-12px py-9px text-13px text-[#172033]"
            >
              {{ selectedFileTypeTaxonomyLeafName || '请先选择文件分类' }}
            </div>
          </div>
        </el-form-item>
        <el-form-item v-if="uploadDirectoryTree" label="提交目录" prop="directoryId">
          <div class="w-full">
            <template v-if="uploadDirectoryTree.leafBinding">
              <div class="rounded-6px border border-[#dbe3ef] bg-[#fafcff] px-12px py-9px text-13px text-[#172033]">
                {{ uploadDirectoryTree.bindingDirectoryPath }}
              </div>
              <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
                {{
                  uploadDirectoryTree.defaultUnclassified
                    ? '当前文件类别未绑定提交目录，系统将自动提交到未分类目录。'
                    : '当前绑定目录已经是最后一层目录，将直接提交到该目录。'
                }}
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
            :trigger-on-focus="canLoadUploadNameOptions"
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
              <el-alert
                v-if="isRequestedVersionDuplicate"
                class="mt-8px"
                :closable="false"
                :title="versionDuplicatePreflightMessage"
                type="error"
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
              未查询到同编号现行版本，将创建新的 master 主档，并按新建规则校验。
            </template>
          </div>
        </el-form-item>
        <el-form-item label="产品编号" prop="productCode">
          <el-input
            v-model="formData.productCode"
            class="!w-420px"
            readonly
            placeholder="选择 DCC 项目后自动生成"
          />
          <div
            v-if="isProductRequiredForSelectedCategory"
            data-testid="dcc-upload-product-code-binding-hint"
            class="mt-6px text-12px"
            :class="productCodeBindingHintClass"
          >
            {{ productCodeBindingHintText }}
          </div>
          <div v-if="selectedProjectCode" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            来源：DCC 项目代码 {{ selectedProjectCode.projectName }} / {{ selectedProjectCode.projectCode || '-' }}
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

          <section class="upload-section upload-section--preflight" data-testid="dcc-upload-preflight-panel">
        <div class="upload-section__title">提交前校验</div>
        <div class="upload-preflight-legend">文件编号/版本 · 分类上传权限 · 审批人链路 · 受控浏览目录 · 浏览权限范围</div>
        <div class="upload-preflight-grid">
          <div
            v-for="check in uploadPreflightChecks"
            :key="check.key"
            class="upload-preflight-card"
            :class="{ 'is-ok': check.ok, 'is-warning': check.warning, 'is-error': !check.ok && !check.warning }"
          >
            <div class="upload-preflight-card__header">
              <span>{{ check.label }}</span>
              <el-tag size="small" :type="check.ok ? 'success' : check.warning ? 'warning' : 'danger'">
                {{ check.status }}
              </el-tag>
            </div>
            <div class="upload-preflight-card__description">{{ check.description }}</div>
          </div>
        </div>
      </section>
          </div>

          <div
            class="upload-workbench__column upload-workbench__column--side"
            data-testid="dcc-upload-right-column"
          >
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
            <el-alert
              v-if="uploadPreviewError"
              data-testid="dcc-upload-preview-error"
              class="mt-12px"
              type="error"
              :closable="false"
              show-icon
              :title="uploadPreviewError"
            />
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
                :onlyoffice-document-url="previewUpload.onlyofficeDocumentUrl"
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
import { formatToDate } from '@/utils/dateUtil'
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
  getControlledFileCurrentVersion,
  getControlledFileUploadRevisionCandidates,
  getControlledFileUploadDirectoryTree,
  getControlledFileUploadNameOptions,
  submitControlledFile,
  uploadControlledFilePreview,
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
  resolveUploadPreviewErrorMessage,
  isDccProductRequiredForCategoryCode,
  validateDrawingPdfUpload,
  validateControlledFileSelection,
  validateSingleUploadFileSelection,
  validateDccProjectProductCode,
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
const uploadDirectoryTree = ref<ControlledFileUploadDirectoryTreeVO>()
const currentVersionInfo = ref<ControlledFileCurrentVersionRespVO>()
const fileList = ref<UploadUserFile[]>([])
const drawingPdfFileList = ref<UploadUserFile[]>([])
const previewUpload = ref<ControlledFileUploadRespVO>()
const drawingPdfUpload = ref<ControlledFileUploadRespVO>()
const previewFileBlob = ref<File | null>(null)
const uploadPreviewError = ref('')
const submitLoading = ref(false)
const uploadPreviewLoading = ref(false)
const uploadDrawingPdfLoading = ref(false)
const uploadNameOptionsLoading = ref(false)
const uploadNameOptionsLoadedKey = ref('')
const currentVersionLookupLoading = ref(false)
const projectCodeOptionsLoading = ref(false)
const fileTypeTaxonomiesLoading = ref(false)
const projectCodeOptionsError = ref('')
const fileTypeTaxonomyOptionsError = ref('')
const categoryOptionsError = ref('')
const selectedHistoryVersion = ref('')
const selectedHistoryFileName = ref('')
const uploadSessionId = createControlledFileUploadSessionId()
const uploadSubmitted = ref(false)
const submitFieldErrors = reactive({
  versionNo: ''
})

const DEFAULT_MANUAL_VERSION_NO = 'V1.0'
const VERSION_NO_PATTERN = /^V?(\d+)(?:\.\d+)?$/i
const resolveTodayDate = () => formatToDate(new Date())
const resolveNextMajorVersionNo = (currentVersionNo: string | null | undefined) => {
  const matched = (currentVersionNo || '').trim().match(VERSION_NO_PATTERN)
  if (!matched) {
    return ''
  }
  const majorVersion = Number(matched[1])
  if (!Number.isFinite(majorVersion)) {
    return ''
  }
  const nextMajorVersion = majorVersion + 1
  return `V${nextMajorVersion}.0`
}

const resolveProcessTypeByRoute = () =>
  route.path.includes('/external') ? 'EXTERNAL_REVIEW' : 'CONTROLLED_FILE'
const isExternalReview = computed(() => resolveProcessTypeByRoute() === 'EXTERNAL_REVIEW')
const pageTitle = computed(() => (isExternalReview.value ? '外来文件评审' : '受控文件提交'))
const submitButtonText = computed(() => (isExternalReview.value ? '提交评审' : '提交审批'))
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
  versionNo: DEFAULT_MANUAL_VERSION_NO,
  effectiveDate: resolveTodayDate(),
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

const selectedFileTypeTaxonomyLeafName = computed(() => {
  const names = selectedFileTypeTaxonomyPath.value?.names || []
  return names.length ? names[names.length - 1] : ''
})

const isFileTypeTaxonomyDepthValid = computed(
  () => (selectedFileTypeTaxonomyPath.value?.names.length || 0) >= 3
)

const selectedCategory = computed(() =>
  categories.value.find((category) => category.id === formData.categoryId)
)
const selectedProjectCode = computed(() =>
  projectCodeOptions.value.find((project) => project.id === formData.dccProjectCodeId)
)
const categoryDirectoryBindingMessage = '当前文件类别未绑定提交目录，系统将自动落位到未分类目录。'
const categoryUploadPermissionMessage = '当前用户没有该文件类别的上传权限，请联系文控管理员补齐该类别 UPLOAD 权限。'
const categorySelectEmptyText = computed(() => {
  return '当前没有可上传文件类别'
})
const selectedFileTypeTaxonomyBoundCategories = computed(() => {
  if (isExternalReview.value) {
    return categories.value
  }
  if (!formData.fileTypeTaxonomyId || !isFileTypeTaxonomyDepthValid.value) {
    return []
  }
  return categories.value.filter(
    (category) =>
      category.active &&
      category.fileTypeTaxonomyId === Number(formData.fileTypeTaxonomyId)
  )
})
const availableCategories = computed(() =>
  selectedFileTypeTaxonomyBoundCategories.value.filter((category) => {
    if (!category.active || category.canUpload === false) {
      return false
    }
    return true
  })
)
const selectedFileTypeTaxonomyAutoCategory = computed(() =>
  !isExternalReview.value && availableCategories.value.length === 1
    ? availableCategories.value[0]
    : undefined
)
const categoryPermissionPreflightMessage = computed(() => {
  if (categoryOptionsError.value) {
    return categoryOptionsError.value
  }
  if (isExternalReview.value) {
    if (!categories.value.length || !availableCategories.value.length) {
      return '当前没有可上传文件类别：请确认分类已启用，并授予当前账号文件类别 UPLOAD 权限。'
    }
    return ''
  }
  if (!formData.fileTypeTaxonomyId) {
    return '请先选择文件分类，文件类别将自动显示所选分类的最后一级。'
  }
  if (!isFileTypeTaxonomyDepthValid.value) {
    return '请先选择至少三级文件分类，文件类别将自动取最后一级。'
  }
  if (!selectedFileTypeTaxonomyBoundCategories.value.length) {
    return '当前文件分类暂无可上传文件类别，请联系文控管理员配置该叶子节点的唯一正式 DCC 类别。'
  }
  if (selectedFileTypeTaxonomyBoundCategories.value.length > 1) {
    return '当前文件分类叶子节点绑定了多个正式 DCC 类别，请联系文控管理员保留唯一启用类别后再提交。'
  }
  const boundCategory = selectedFileTypeTaxonomyBoundCategories.value[0]
  if (boundCategory.canUpload === false) {
    return categoryUploadPermissionMessage
  }
  if (!boundCategory.directoryId) {
    return categoryDirectoryBindingMessage
  }
  return ''
})
const isProductRequiredForSelectedCategory = computed(() =>
  isDccProductRequiredForCategoryCode(selectedCategory.value?.code)
)
const isRequiredProjectCodeBound = computed(() => Boolean(formData.productCode.trim()))
const productCodeBindingHintText = computed(() => {
  if (isRequiredProjectCodeBound.value) {
    return `已自动绑定 DCC 项目代码：${formData.productCode.trim()}`
  }
  return 'DHF/DMR 类别必须选择包含项目代码的 DCC 项目'
})
const productCodeBindingHintClass = computed(() =>
  isRequiredProjectCodeBound.value
    ? 'text-[var(--el-color-success)]'
    : 'text-[var(--el-color-danger)]'
)

const uploadSubmitterService = createUploadSubmitterService({
  uploadPreview: uploadControlledFilePreview,
  submit: submitControlledFile
})

const canLoadUploadNameOptions = computed(
  () =>
    Boolean(formData.dccProjectCodeId) &&
    Boolean(formData.fileTypeTaxonomyId) &&
    isFileTypeTaxonomyDepthValid.value
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
          callback(new Error(isExternalReview.value ? '请选择文件类别' : categoryPermissionPreflightMessage.value || '文件分类尚未自动匹配可上传文件类别'))
          return
        }
        const category = categories.value.find((item) => item.id === Number(value))
        if (!isExternalReview.value && category?.fileTypeTaxonomyId !== Number(formData.fileTypeTaxonomyId)) {
          callback(new Error('文件类别必须来自当前文件分类叶子节点，请重新选择文件分类'))
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

const clearCurrentVersionInfo = () => {
  currentVersionInfo.value = undefined
}

const ensureEffectiveDateDefault = () => {
  if (!formData.effectiveDate) {
    formData.effectiveDate = resolveTodayDate()
  }
}

const resetUploadNameLinkage = (clearVersionNo: boolean) => {
  selectedHistoryFileName.value = ''
  selectedHistoryVersion.value = ''
  formData.changeType = 'NEW'
  clearRevisionTargetSelection()
  if (clearVersionNo) {
    formData.versionNo = DEFAULT_MANUAL_VERSION_NO
  }
  ensureEffectiveDateDefault()
}

const resetUploadNameContext = (clearFileName: boolean) => {
  uploadNameOptions.value = []
  uploadNameOptionsLoading.value = false
  uploadNameOptionsLoadedKey.value = ''
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
  uploadPreviewError.value = ''
}

const resetDrawingPdfUpload = () => {
  drawingPdfUpload.value = undefined
  drawingPdfFileList.value = []
}

const resetCategorySelectionForFileTypeTaxonomyChange = () => {
  formData.categoryId = null
  resetUploadDirectoryContext()
  resetSelectedPreview()
  resetDrawingPdfUpload()
  clearSubmitFieldErrors(submitFieldErrors)
}

const syncAutoCategoryFromSelectedFileTypeTaxonomy = async () => {
  if (isExternalReview.value) {
    return
  }
  formData.categoryId = selectedFileTypeTaxonomyAutoCategory.value?.id || null
  if (!formData.categoryId) {
    return
  }
  applyDccProjectCodeProductNumber()
  await loadUploadDirectoryTree(formData.categoryId)
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
    throw new Error(isExternalReview.value ? '请先选择文件类别' : categoryPermissionPreflightMessage.value || '文件分类尚未自动匹配可上传文件类别')
  }
  return {
    categoryId: formData.categoryId,
    sessionId: uploadSessionId
  }
}

const loadProjectCodeOptions = async (keyword = '') => {
  projectCodeOptionsLoading.value = true
  projectCodeOptionsError.value = ''
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
    const errorMessage = resolveUploadErrorMessage(error, 'DCC 项目候选加载失败，请确认项目代码权限或联系文控管理员。')
    projectCodeOptionsError.value = errorMessage.includes('DCC 项目候选加载失败')
      ? errorMessage
      : `DCC 项目候选加载失败：${errorMessage}`
    message.error(projectCodeOptionsError.value)
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
  fileTypeTaxonomyOptionsError.value = ''
  try {
    fileTypeTaxonomies.value = await getFileTypeTaxonomyList()
  } catch (error) {
    fileTypeTaxonomies.value = []
    const errorMessage = resolveUploadErrorMessage(error, '文件分类候选加载失败，请确认文件类型权限或联系文控管理员。')
    fileTypeTaxonomyOptionsError.value = errorMessage.includes('文件分类候选加载失败')
      ? errorMessage
      : `文件分类候选加载失败：${errorMessage}`
    message.error(fileTypeTaxonomyOptionsError.value)
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

const applyDccProjectCodeProductNumber = () => {
  formData.productMasterId = null
  formData.productCode = selectedProjectCode.value?.projectCode?.trim() || ''
}

const loadBaseData = async () => {
  categoryOptionsError.value = ''
  try {
    const [categoryList] = await Promise.all([
      getFileCategoryList(),
      loadFileTypeTaxonomies(),
      loadProjectCodeOptions()
    ])
    categories.value = categoryList.filter((item) => item.active)
  } catch (error) {
    categories.value = []
    const errorMessage = resolveUploadErrorMessage(error, '文件类别候选加载失败，请确认分类权限或联系文控管理员。')
    categoryOptionsError.value = errorMessage.includes('文件类别候选加载失败')
      ? errorMessage
      : `文件类别候选加载失败：${errorMessage}`
    message.error(categoryOptionsError.value)
  }
}

const buildUploadNameOptionsKey = () => {
  if (!canLoadUploadNameOptions.value || !formData.dccProjectCodeId || !formData.fileTypeTaxonomyId) {
    return ''
  }
  return `${formData.dccProjectCodeId}:${formData.fileTypeTaxonomyId}`
}

const loadUploadNameOptions = async (dccProjectCodeId: number, fileTypeTaxonomyId: number) => {
  const requestKey = `${dccProjectCodeId}:${fileTypeTaxonomyId}`
  uploadNameOptionsLoading.value = true
  try {
    const options = await getControlledFileUploadNameOptions({
      dccProjectCodeId,
      fileTypeTaxonomyId
    })
    if (buildUploadNameOptionsKey() !== requestKey) {
      return
    }
    uploadNameOptions.value = options
    uploadNameOptionsLoadedKey.value = requestKey
  } catch (error) {
    if (buildUploadNameOptionsKey() === requestKey) {
      uploadNameOptions.value = []
      uploadNameOptionsLoadedKey.value = ''
      message.error(resolveUploadErrorMessage(error, '历史文件名称加载失败，请查看错误提示后重试'))
    }
  } finally {
    if (buildUploadNameOptionsKey() === requestKey) {
      uploadNameOptionsLoading.value = false
    }
  }
}

const ensureUploadNameOptionsLoaded = async () => {
  const optionsKey = buildUploadNameOptionsKey()
  if (!optionsKey) {
    return
  }
  if (uploadNameOptionsLoadedKey.value === optionsKey) {
    return
  }
  if (!canLoadUploadNameOptions.value || !formData.dccProjectCodeId || !formData.fileTypeTaxonomyId) {
    return
  }
  await loadUploadNameOptions(formData.dccProjectCodeId, formData.fileTypeTaxonomyId)
}

const handleProjectCodeChange = async () => {
  applyDccProjectCodeProductNumber()
  resetUploadNameContext(true)
}

const handleFileTypeTaxonomyChange = async () => {
  resetUploadNameContext(true)
  resetCategorySelectionForFileTypeTaxonomyChange()
  await formRef.value?.validateField?.('fileTypeTaxonomyId').catch(() => undefined)
  await syncAutoCategoryFromSelectedFileTypeTaxonomy()
  await formRef.value?.validateField?.('categoryId').catch(() => undefined)
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
const controlledBrowserPermissionScopeText = computed(() => {
  const categoryName = selectedCategory.value?.name || '未选择文件类别'
  const directoryPath = selectedUploadDirectoryPath.value || '未选择受控浏览目录'
  const projectCode = selectedProjectCode.value?.projectCode || formData.productCode || '未选择项目代码'
  return `浏览权限范围：分类 ${categoryName}；目录 ${directoryPath}；项目代码 ${projectCode}；发布后按正式 VIEW 权限矩阵控制可见人员。`
})
interface UploadPreflightCheck {
  key: string
  label: string
  status: string
  description: string
  ok: boolean
  warning?: boolean
}

const normalizePreflightVersionNo = (value?: string | null) => String(value || '').trim().toUpperCase()

const isRequestedVersionDuplicate = computed(() => {
  const currentVersionNo = normalizePreflightVersionNo(currentVersionInfo.value?.currentVersionNo)
  const requestedVersionNo = normalizePreflightVersionNo(formData.versionNo)
  return Boolean(currentVersionInfo.value?.matched && currentVersionNo && requestedVersionNo && currentVersionNo === requestedVersionNo)
})
const versionDuplicatePreflightMessage = computed(() => {
  if (!isRequestedVersionDuplicate.value) {
    return ''
  }
  if (normalizePreflightVersionNo(formData.versionNo) === 'V1.0') {
    return '文件编号已存在，不能重复创建 V1.0 原版，请改用升版流程或更换文件编号。'
  }
  return `文件编号 ${formData.fileNumber.trim()} 的版本 ${formData.versionNo.trim()} 已存在，请调整升版版本号。`
})

const approvalChainPreflightText = computed(() => {
  const approvalPositionIds = selectedCategory.value?.approvalPositionIds || []
  const signoffPositionIds = selectedCategory.value?.signoffPositionIds || []
  if (!selectedCategory.value) {
    return isExternalReview.value ? '请选择文件类别后检查审批人链路' : '请先完成文件分类的文件类别自动匹配后检查审批人链路'
  }
  if (!approvalPositionIds.length || !signoffPositionIds.length) {
    return `审批岗位 ${approvalPositionIds.length} 个，会签/签核岗位 ${signoffPositionIds.length} 个，请先补齐分类审批链路`
  }
  return `审批岗位 ${approvalPositionIds.length} 个，会签/签核岗位 ${signoffPositionIds.length} 个，审批人链路已具备`
})

const uploadPreflightChecks = computed<UploadPreflightCheck[]>(() => {
  const categoryCanUpload = selectedCategory.value?.canUpload !== false
  const approvalPositionIds = selectedCategory.value?.approvalPositionIds || []
  const signoffPositionIds = selectedCategory.value?.signoffPositionIds || []
  const hasApprovalChain = Boolean(selectedCategory.value && approvalPositionIds.length && signoffPositionIds.length)
  const hasDirectoryLanding = Boolean(selectedUploadDirectoryPath.value)
  const versionReady = Boolean(formData.fileNumber.trim() && formData.versionNo.trim())
  const versionDescription = currentVersionLookupLoading.value
    ? '正在校验文件编号和版本，请等待结果后再提交。'
    : isRequestedVersionDuplicate.value
      ? versionDuplicatePreflightMessage.value
      : currentVersionProjectionBlockReason.value
        ? currentVersionProjectionBlockReason.value
        : currentVersionInfo.value?.modifying
          ? '同编号文件已有未完成流程，当前不可重复提交。'
          : currentVersionInfo.value?.matched
            ? `现行版本 ${currentVersionInfo.value.currentVersionNo || '-'}，本次提交版本 ${formData.versionNo || '-'}。`
            : versionReady
              ? '未发现同编号现行版本，将按新建编号继续校验。'
              : '请输入文件编号和版本号后检查是否重复。'

  return [
    {
      key: 'file-version',
      label: '文件编号/版本',
      status: currentVersionLookupLoading.value
        ? '检查中'
        : isRequestedVersionDuplicate.value || currentVersionProjectionBlockReason.value || currentVersionInfo.value?.modifying
          ? '需处理'
          : versionReady
            ? '可提交'
            : '待填写',
      description: versionDescription,
      ok: versionReady && !currentVersionLookupLoading.value && !isRequestedVersionDuplicate.value && !currentVersionProjectionBlockReason.value && !currentVersionInfo.value?.modifying,
      warning: !versionReady || currentVersionLookupLoading.value
    },
    {
      key: 'category-upload',
      label: '分类上传权限',
      status: selectedCategory.value ? (categoryCanUpload ? '可上传' : '无权限') : (isExternalReview.value ? '待选择' : '待匹配'),
      description: selectedCategory.value
        ? categoryCanUpload
          ? `当前分类 ${selectedCategory.value.name} 允许上传。`
          : categoryUploadPermissionMessage
        : isExternalReview.value ? '请选择文件类别后检查当前账号是否有上传权限。' : '选择文件分类后将自动匹配正式文件类别并检查当前账号上传权限。',
      ok: Boolean(selectedCategory.value && categoryCanUpload),
      warning: !selectedCategory.value
    },
    {
      key: 'approval-chain',
      label: '审批人链路',
      status: selectedCategory.value ? (hasApprovalChain ? '已配置' : '不完整') : (isExternalReview.value ? '待选择' : '待匹配'),
      description: approvalChainPreflightText.value,
      ok: hasApprovalChain,
      warning: !selectedCategory.value
    },
    {
      key: 'controlled-browser-directory',
      label: '受控浏览目录',
      status: hasDirectoryLanding ? '可落位' : '待落位',
      description: hasDirectoryLanding
        ? `最终受控浏览目录：${selectedUploadDirectoryPath.value}`
        : '请选择最终提交目录，确保发布后可以落位到受控浏览目录。',
      ok: hasDirectoryLanding,
      warning: !formData.categoryId
    },
    {
      key: 'controlled-browser-permission-scope',
      label: '浏览权限范围',
      status: selectedCategory.value && hasDirectoryLanding ? '按矩阵生效' : '待确认',
      description: controlledBrowserPermissionScopeText.value,
      ok: Boolean(selectedCategory.value && hasDirectoryLanding),
      warning: !selectedCategory.value || !hasDirectoryLanding
    }
  ]
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
      applyDccProjectCodeProductNumber()
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

const queryUploadNameSuggestions = async (
  queryString: string,
  callback: (items: UploadNameSuggestionItem[]) => void
) => {
  if (!canLoadUploadNameOptions.value) {
    callback([])
    return
  }
  await ensureUploadNameOptionsLoaded()
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
  resetUploadDirectoryContext()
  resetSelectedPreview()
  resetDrawingPdfUpload()
  if (formData.categoryId) {
    applyDccProjectCodeProductNumber()
    await loadUploadDirectoryTree(formData.categoryId)
  }
}

const handleHistoryFileNameSelect = async (item: UploadNameSuggestionItem) => {
  selectedHistoryFileName.value = item.value
  selectedHistoryVersion.value = item.currentVersionNo?.trim() || ''
  formData.fileName = item.value
  formData.changeType = 'REVISION'
  formData.versionNo = resolveNextMajorVersionNo(selectedHistoryVersion.value)
  ensureEffectiveDateDefault()
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
    const errorMessage = validation.message || '文件校验失败'
    uploadRef.value?.clearFiles()
    resetSelectedPreview()
    uploadPreviewError.value = errorMessage
    message.error(errorMessage)
    return
  }
  if (!file.raw) {
    return
  }

  clearSubmitFieldErrors(submitFieldErrors)
  uploadPreviewError.value = ''
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
    uploadPreviewError.value = resolveUploadPreviewErrorMessage(error, '文件预览上传失败，请查看错误提示后重试')
    message.error(uploadPreviewError.value)
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
    const errorMessage = validation.message || '图纸 PDF 校验失败'
    drawingPdfUploadRef.value?.clearFiles()
    resetDrawingPdfUpload()
    uploadPreviewError.value = errorMessage
    message.error(errorMessage)
    return
  }
  if (!file.raw) {
    return
  }
  const isPdf = file.raw.type === 'application/pdf' || /\.pdf$/i.test(file.name)
  if (!isPdf) {
    const errorMessage = '文件格式不受支持：图纸文件必须上传 PDF 格式'
    drawingPdfUploadRef.value?.clearFiles()
    resetDrawingPdfUpload()
    uploadPreviewError.value = errorMessage
    message.error(errorMessage)
    return
  }

  uploadPreviewError.value = ''
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
    uploadPreviewError.value = resolveUploadPreviewErrorMessage(error, '图纸 PDF 上传失败，请查看错误提示后重试')
    message.error(uploadPreviewError.value)
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
  if (isRequestedVersionDuplicate.value) {
    submitFieldErrors.versionNo = versionDuplicatePreflightMessage.value
    message.warning(versionDuplicatePreflightMessage.value)
    return
  }
  if (!isExternalReview.value && formData.changeType === 'REVISION' && !formData.revisionTargetControlledFileId) {
    message.warning('请选择历史文件名称后再升版')
    return
  }
  if (!uploadDirectoryTree.value) {
    message.warning(isExternalReview.value ? '请先选择文件类别并完成目录加载' : categoryPermissionPreflightMessage.value || '请先选择文件分类并完成目录加载')
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
  const productCodeValidation = validateDccProjectProductCode(
    formData.productCode,
    isProductRequiredForSelectedCategory.value
  )
  if (!productCodeValidation.valid) {
    message.warning(productCodeValidation.message || '产品编号校验失败')
    return
  }
  const drawingPdfValidation = validateDrawingPdfUpload(previewUpload.value, drawingPdfUpload.value)
  if (!drawingPdfValidation.valid) {
    message.warning(drawingPdfValidation.message || '图纸 PDF 校验失败')
    return
  }

  submitLoading.value = true
  try {
    await uploadSubmitterService.submit(
      { ...formData, productMasterId: null },
      previewUpload.value,
      drawingPdfUpload.value
    )
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
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
}

.upload-workbench__column {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.upload-section {
  min-width: 0;
  padding: 14px 16px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
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

.upload-preflight-legend {
  margin-bottom: 10px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.upload-preflight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.upload-preflight-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #f1b8b8;
  border-radius: 8px;
  background: #fffafa;
}

.upload-preflight-card.is-ok {
  border-color: #b7dfc7;
  background: #f7fcf8;
}

.upload-preflight-card.is-warning {
  border-color: #f0d49a;
  background: #fffaf0;
}

.upload-preflight-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.upload-preflight-card__description {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
  word-break: break-word;
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
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
  .upload-preflight-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
