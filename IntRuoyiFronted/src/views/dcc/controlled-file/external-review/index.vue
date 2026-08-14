<template>
  <ContentWrap>
    <div class="external-review-header">
      <div class="text-18px font-600">外来文件评审</div>
    </div>
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="112px" v-loading="submitLoading">
      <section
        class="external-review-section external-review-section--first"
        data-testid="dcc-external-review-section-review"
      >
        <div class="external-review-section__title">评审信息</div>
        <el-form-item label="外来来源" prop="externalSource">
          <el-input v-model="formData.externalSource" class="!w-420px" placeholder="例如 客户来图、供应商资料" />
        </el-form-item>
        <el-form-item label="外来归属" prop="externalOwner">
          <el-input v-model="formData.externalOwner" class="!w-420px" placeholder="请输入外来责任方" />
        </el-form-item>
        <el-form-item label="评审原因" prop="reviewReason">
          <el-input
            v-model="formData.reviewReason"
            class="!w-560px"
            type="textarea"
            :rows="3"
            placeholder="请输入本次外来文件评审原因"
          />
        </el-form-item>
        <el-form-item label="参与人" prop="participantUserIds">
          <UserSelectV2
            v-model="formData.participantUserIds"
            class="!w-560px"
            :multiple="true"
            placeholder="请选择外来文件评审参与人"
          />
        </el-form-item>
      </section>

      <section class="external-review-section" data-testid="dcc-external-review-section-file">
        <div class="external-review-section__title">文件信息</div>
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
          </el-select>
        </el-form-item>
        <el-form-item v-if="uploadDirectoryTree" label="提交目录" prop="directoryId">
          <template v-if="uploadDirectoryTree.leafBinding">
            <div class="rounded-6px border border-[#dbe3ef] bg-[#fafcff] px-12px py-9px text-13px">
              {{ uploadDirectoryTree.bindingDirectoryPath }}
            </div>
          </template>
          <el-cascader
            v-else
            v-model="formData.directoryId"
            class="!w-560px"
            :options="uploadDirectoryTree.children"
            :props="directoryCascaderProps"
            clearable
            filterable
            placeholder="请选择最终提交目录"
          />
        </el-form-item>
        <el-form-item label="文件名称" prop="fileName">
          <el-input v-model="formData.fileName" class="!w-420px" placeholder="请输入外来文件名称" />
        </el-form-item>
        <el-form-item label="文件编号" prop="fileNumber">
          <el-input v-model="formData.fileNumber" class="!w-280px" placeholder="例如 EXT-001" />
        </el-form-item>
        <el-form-item label="DCC 项目" prop="dccProjectCodeId">
          <el-select
            v-model="formData.dccProjectCodeId"
            class="!w-420px"
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
        </el-form-item>
        <el-form-item label="产品编号" prop="productCode">
          <el-input
            v-model="formData.productCode"
            class="!w-420px"
            readonly
            placeholder="选择 DCC 项目后自动生成"
          />
          <div v-if="selectedProjectCode" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            来源：DCC 项目代码 {{ selectedProjectCode.projectName }} / {{ selectedProjectCode.projectCode || '-' }}
          </div>
        </el-form-item>
        <el-form-item label="版本号" prop="versionNo">
          <el-input v-model="formData.versionNo" class="!w-220px" placeholder="例如 V1.0" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="formData.effectiveDate"
            class="!w-220px"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
          />
        </el-form-item>
        <el-form-item label="提交备注">
          <el-input v-model="formData.remark" class="!w-560px" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </section>

      <section class="external-review-section" data-testid="dcc-external-review-section-upload">
        <div class="external-review-section__title">附件上传</div>
        <el-form-item label="外来文件" prop="file">
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
            >
              <el-button type="primary" plain :loading="uploadPreviewLoading">
                <Icon icon="ep:upload" class="mr-5px" />
                选择外来文件
              </el-button>
              <template #tip>
                <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
                  {{ EDITABLE_SOURCE_MESSAGE }}；图纸源文件需同步上传 PDF。
                </div>
              </template>
            </el-upload>
            <div v-if="previewUpload" class="mt-10px text-13px text-[var(--el-text-color-secondary)]">
              {{ previewUpload.fileName }} · {{ formatPreviewFileSize(previewUpload.fileSize) }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="图纸 PDF">
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
          >
            <el-button plain :loading="uploadDrawingPdfLoading">
              <Icon icon="ep:document" class="mr-5px" />
              选择 PDF
            </el-button>
          </el-upload>
          <div v-if="drawingPdfUpload" class="mt-10px text-13px text-[var(--el-text-color-secondary)]">
            {{ drawingPdfUpload.fileName }}
          </div>
        </el-form-item>
      </section>

      <section class="external-review-section" data-testid="dcc-external-review-section-submit">
        <div class="external-review-section__title">提交操作</div>
        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">
            <Icon icon="ep:promotion" class="mr-5px" />
            提交评审
          </el-button>
        </el-form-item>
      </section>
    </el-form>
  </ContentWrap>
</template>

<script lang="ts" setup>
import type { FormRules, UploadProps, UploadUserFile } from 'element-plus'
import { getFileCategoryList, type ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import {
  cleanupControlledFileUploadSession,
  createControlledFileUploadSessionId,
  EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY,
  getControlledFileUploadDirectoryTree,
  submitExternalFileReview,
  uploadControlledFilePreview,
  type ControlledFileUploadDirectoryTreeVO,
  type ControlledFileUploadRespVO
} from '@/api/dcc/controlledFile/workflow'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import {
  EDITABLE_SOURCE_MESSAGE,
  formatPreviewFileSize,
  resolveUploadErrorMessage,
  validateControlledFileSelection,
  validateDrawingPdfUpload,
  validateSingleUploadFileSelection
} from '../upload/submitter'

defineOptions({ name: 'DccExternalFileReview' })

const router = useRouter()
const message = useMessage()

const formRef = ref()
const uploadRef = ref()
const drawingPdfUploadRef = ref()
const categories = ref<ControlledFileCategoryVO[]>([])
const projectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const uploadDirectoryTree = ref<ControlledFileUploadDirectoryTreeVO>()
const fileList = ref<UploadUserFile[]>([])
const drawingPdfFileList = ref<UploadUserFile[]>([])
const previewUpload = ref<ControlledFileUploadRespVO>()
const drawingPdfUpload = ref<ControlledFileUploadRespVO>()
const submitLoading = ref(false)
const uploadPreviewLoading = ref(false)
const uploadDrawingPdfLoading = ref(false)
const projectCodeOptionsLoading = ref(false)
const uploadSessionId = createControlledFileUploadSessionId()
const uploadSubmitted = ref(false)

const formData = reactive({
  categoryId: null as number | null,
  directoryId: null as number | null,
  externalSource: '',
  externalOwner: '',
  reviewReason: '',
  participantUserIds: [] as number[],
  fileName: '',
  fileNumber: '',
  dccProjectCodeId: null as number | null,
  productCode: '',
  versionNo: '',
  effectiveDate: '',
  remark: ''
})

const availableCategories = computed(() =>
  categories.value.filter((category) => category.active)
)

const formRules = reactive<FormRules>({
  categoryId: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (!value) {
          callback(new Error('请选择文件类别'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  directoryId: [{ required: true, message: '请选择最终提交目录', trigger: 'change' }],
  externalSource: [{ required: true, message: '请输入外来来源', trigger: 'blur' }],
  externalOwner: [{ required: true, message: '请输入外来归属', trigger: 'blur' }],
  reviewReason: [{ required: true, message: '请输入评审原因', trigger: 'blur' }],
  participantUserIds: [{ required: true, type: 'array', min: 1, message: '请选择参与人', trigger: 'change' }],
  fileName: [{ required: true, message: '请输入文件名称', trigger: 'blur' }],
  fileNumber: [{ required: true, message: '请输入文件编号', trigger: 'blur' }],
  dccProjectCodeId: [{ required: true, message: '请选择 DCC 项目', trigger: 'change' }],
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

const loadBaseData = async () => {
  categories.value = (await getFileCategoryList()).filter((item) => item.active)
  await loadProjectCodeOptions()
}

const selectedProjectCode = computed(() =>
  projectCodeOptions.value.find((projectCode) => projectCode.id === formData.dccProjectCodeId)
)

const formatProjectCodeOptionLabel = (projectCode: DccProjectCodeRespVO) =>
  [projectCode.projectName, projectCode.projectCode, projectCode.docControlNo].filter(Boolean).join(' / ')

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
  } catch (error) {
    projectCodeOptions.value = []
    message.error(resolveUploadErrorMessage(error, 'DCC 项目加载失败，请查看错误提示后重试'))
  } finally {
    projectCodeOptionsLoading.value = false
  }
}

const applyDccProjectCodeProductNumber = () => {
  formData.productCode = selectedProjectCode.value?.projectCode?.trim() || ''
}

const handleProjectCodeChange = () => {
  applyDccProjectCodeProductNumber()
}

const resetSelectedPreview = () => {
  previewUpload.value = undefined
  fileList.value = []
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

const handleCategoryChange = async () => {
  if (!(await cleanupCurrentUploadSession())) {
    return
  }
  uploadDirectoryTree.value = undefined
  formData.directoryId = null
  resetSelectedPreview()
  resetDrawingPdfUpload()
  if (!formData.categoryId) {
    return
  }
  try {
    const tree = await getControlledFileUploadDirectoryTree(formData.categoryId)
    uploadDirectoryTree.value = tree
    formData.directoryId = tree.leafBinding ? tree.bindingDirectoryId : null
  } catch (error) {
    message.error(resolveUploadErrorMessage(error, '上传目录加载失败，请查看错误提示后重试'))
  }
}

const handleFileChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  const validation = validateControlledFileSelection(
    uploadFiles.map((item) => ({ name: item.name, type: item.raw?.type }))
  )
  if (!validation.valid || !file.raw) {
    message.error(validation.message || '文件校验失败')
    uploadRef.value?.clearFiles()
    resetSelectedPreview()
    return
  }
  fileList.value = uploadFiles.slice(-1)
  uploadPreviewLoading.value = true
  try {
    previewUpload.value = await uploadControlledFilePreview(
      file.raw as File,
      'SOURCE',
      buildUploadPreviewContext()
    )
  } catch (error) {
    resetSelectedPreview()
    message.error(resolveUploadErrorMessage(error, '文件上传失败，请查看错误提示后重试'))
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
}

const handleDrawingPdfChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  const validation = validateSingleUploadFileSelection(
    uploadFiles.map((item) => ({ name: item.name, type: item.raw?.type }))
  )
  const isPdf = file.raw?.type === 'application/pdf' || /\.pdf$/i.test(file.name)
  if (!validation.valid || !file.raw || !isPdf) {
    message.error(validation.message || '图纸文件必须上传 PDF 格式')
    drawingPdfUploadRef.value?.clearFiles()
    resetDrawingPdfUpload()
    return
  }
  drawingPdfFileList.value = uploadFiles.slice(-1)
  uploadDrawingPdfLoading.value = true
  try {
    drawingPdfUpload.value = await uploadControlledFilePreview(
      file.raw as File,
      'DRAWING_PDF',
      buildUploadPreviewContext()
    )
  } catch (error) {
    resetDrawingPdfUpload()
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
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!previewUpload.value) {
    message.warning('请先选择并完成外来文件上传')
    return
  }
  if (!formData.productCode.trim()) {
    message.warning('请选择包含项目代码的 DCC 项目')
    return
  }
  const drawingPdfValidation = validateDrawingPdfUpload(previewUpload.value, drawingPdfUpload.value)
  if (!drawingPdfValidation.valid) {
    message.warning(drawingPdfValidation.message || '图纸 PDF 校验失败')
    return
  }
  submitLoading.value = true
  try {
    await submitExternalFileReview({
      categoryId: formData.categoryId as number,
      directoryId: formData.directoryId as number,
      sessionId: previewUpload.value.sessionId,
      originalUploadTicket: previewUpload.value.uploadTicket,
      sourceUploadTicket: previewUpload.value.uploadTicket,
      sourceFileName: previewUpload.value.fileName,
      drawingPdfUploadTicket: drawingPdfUpload.value?.uploadTicket,
      fileName: formData.fileName.trim(),
      fileNumber: formData.fileNumber.trim(),
      productMasterId: null,
      productCode: formData.productCode.trim() || undefined,
      dccProjectCodeId: formData.dccProjectCodeId,
      needTraining: false,
      processType: 'EXTERNAL_REVIEW',
      changeType: 'NEW',
      selectedSignoffUserIds: formData.participantUserIds,
      participantUserIds: formData.participantUserIds,
      externalSource: formData.externalSource.trim(),
      externalOwner: formData.externalOwner.trim(),
      reviewReason: formData.reviewReason.trim(),
      versionNo: formData.versionNo.trim(),
      effectiveDate: formData.effectiveDate,
      remark:
        `${EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY} ${formData.remark}`.trim() || undefined
    })
    uploadSubmitted.value = true
    message.success('外来文件评审已提交')
    await router.push({ name: 'DccControlledFileBrowser' })
  } catch (error) {
    message.error(resolveUploadErrorMessage(error, '外来文件评审提交失败，请查看错误提示后重试'))
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadBaseData()
})

onBeforeRouteLeave(async () => {
  if (await cleanupCurrentUploadSession()) {
    return true
  }
  return false
})
</script>

<style scoped>
.external-review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.external-review-section {
  padding-top: 18px;
  margin-top: 18px;
  border-top: 1px solid #e5ebf3;
}

.external-review-section--first {
  padding-top: 0;
  margin-top: 0;
  border-top: 0;
}

.external-review-section__title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}
</style>
