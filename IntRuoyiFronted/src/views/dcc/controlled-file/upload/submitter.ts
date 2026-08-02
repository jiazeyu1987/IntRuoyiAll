import type {
  ControlledFileChangeType,
  ControlledFileSubmitReqVO,
  ControlledFileUploadPreviewContext,
  ControlledFileUploadRespVO,
  UploadPreviewPurpose
} from '@/api/dcc/controlledFile/workflow'

export interface UploadFormDraft {
  categoryId: number | null
  directoryId: number | null
  fileName: string
  fileNumber: string
  productMasterId: null
  productCode: string
  dccProjectCodeId: number | null
  fileTypeTaxonomyId: number | null
  revisionTargetControlledFileId: number | null
  needTraining: boolean
  selectedSignoffUserIds?: number[]
  processType: 'CONTROLLED_FILE' | 'EXTERNAL_REVIEW'
  changeType: ControlledFileChangeType
  versionNo: string
  effectiveDate: string
  remark?: string
}

export interface UploadSelectionCandidate {
  name: string
  type?: string | null
}

export interface UploadSelectionValidation {
  valid: boolean
  message?: string
}

export interface InlinePdfPreview {
  fileName: string
  bytes: Uint8Array
}

export interface UploadSubmitFailureFeedback {
  message: string
  versionFieldError: string
}

export interface UploadSubmitFieldErrors {
  versionNo: string
}

interface UploadSubmitterServiceDeps {
  uploadPreview: (
    file: File,
    purpose: UploadPreviewPurpose,
    context: ControlledFileUploadPreviewContext
  ) => Promise<ControlledFileUploadRespVO>
  submit: (data: ControlledFileSubmitReqVO) => Promise<number>
}

const trimText = (value: string | null | undefined) => value?.trim() ?? ''
const VERSION_ERROR_PATTERN = /(版本|version)/i
const PRODUCT_CODE_PATTERN = /^[A-Za-z0-9]{14}$/
const DRAWING_SOURCE_EXT_PATTERN = /\.(dwg|sldprt|sldasm|slddrw)$/i
const PRODUCT_BOUND_CATEGORY_PREFIXES = ['DCC_FVM_DHF_', 'DCC_FVM_DMR_']
export const EDITABLE_SOURCE_EXTENSIONS = [
  'doc',
  'docx',
  'xls',
  'xlsx',
  'dwg',
  'sldprt',
  'sldasm',
  'slddrw'
] as const
export const EDITABLE_SOURCE_ACCEPT = EDITABLE_SOURCE_EXTENSIONS.map((item) => `.${item}`).join(',')
export const EDITABLE_SOURCE_MESSAGE =
  '仅支持 doc、docx、xls、xlsx、dwg、sldprt、sldasm、slddrw 等可编辑源文件'
const EDITABLE_SOURCE_EXT_PATTERN = /\.(doc|docx|xls|xlsx|dwg|sldprt|sldasm|slddrw)$/i

export const validateSingleUploadFileSelection = (
  files: ReadonlyArray<UploadSelectionCandidate>
): UploadSelectionValidation => {
  if (files.length !== 1) {
    return {
      valid: false,
      message: '只允许上传一个文件'
    }
  }
  return { valid: true }
}

export const validateControlledFileSelection = (
  files: ReadonlyArray<UploadSelectionCandidate>
): UploadSelectionValidation => {
  const singleFileValidation = validateSingleUploadFileSelection(files)
  if (!singleFileValidation.valid) {
    return singleFileValidation
  }
  if (!EDITABLE_SOURCE_EXT_PATTERN.test(trimText(files[0]?.name))) {
    return {
      valid: false,
      message: '仅支持 doc、docx、xls、xlsx、dwg、sldprt、sldasm、slddrw 等可编辑源文件'
    }
  }

  return { valid: true }
}

export const validateProductCode = (value: string | undefined): UploadSelectionValidation => {
  const productCode = trimText(value)
  if (!productCode) {
    return {
      valid: false,
      message: '请输入产品编号'
    }
  }
  if (!PRODUCT_CODE_PATTERN.test(productCode)) {
    return {
      valid: false,
      message: '产品编号必须为 14 位字母或数字'
    }
  }
  return { valid: true }
}

export const validateDccProjectProductCode = (
  productCode: string | undefined,
  productRequired = false
): UploadSelectionValidation => {
  if (productRequired && !trimText(productCode)) {
    return {
      valid: false,
      message: '请选择包含项目代码的 DCC 项目'
    }
  }
  return { valid: true }
}

export const isDccProductRequiredForCategoryCode = (categoryCode?: string | null) => {
  const normalizedCategoryCode = trimText(categoryCode).toUpperCase()
  return PRODUCT_BOUND_CATEGORY_PREFIXES.some((prefix) => normalizedCategoryCode.startsWith(prefix))
}

export const isDrawingSourceFile = (fileName: string | null | undefined) =>
  DRAWING_SOURCE_EXT_PATTERN.test(trimText(fileName))

export const validateDrawingPdfUpload = (
  previewFile: ControlledFileUploadRespVO | undefined,
  drawingPdfUpload: ControlledFileUploadRespVO | undefined
): UploadSelectionValidation => {
  if (isDrawingSourceFile(previewFile?.fileName) && !drawingPdfUpload?.uploadTicket) {
    return {
      valid: false,
      message: '图纸源文件必须同步上传 PDF 格式文件'
    }
  }
  return { valid: true }
}

export const buildInlinePdfPreview = async (
  file: Blob & Pick<File, 'name'>
): Promise<InlinePdfPreview> => ({
  fileName: file.name,
  bytes: new Uint8Array(await file.arrayBuffer())
})

export const formatPreviewFileSize = (fileSize: number | null | undefined) => {
  if (!fileSize || fileSize < 0) {
    return '-'
  }
  if (fileSize < 1024) {
    return `${fileSize} B`
  }
  if (fileSize < 1024 * 1024) {
    return `${(fileSize / 1024).toFixed(1)} KB`
  }
  return `${(fileSize / 1024 / 1024).toFixed(2)} MB`
}

export const resolveUploadErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

const readErrorField = (source: unknown, key: string): unknown => {
  if (!source || typeof source !== 'object') {
    return undefined
  }
  return (source as Record<string, unknown>)[key]
}

const resolveNestedUploadErrorText = (error: unknown): string => {
  const candidates = [
    error,
    readErrorField(error, 'response'),
    readErrorField(readErrorField(error, 'response'), 'data'),
    readErrorField(error, 'data')
  ]
  for (const candidate of candidates) {
    if (!candidate) {
      continue
    }
    if (typeof candidate === 'string' && candidate.trim() && candidate !== 'error') {
      return candidate.trim()
    }
    if (candidate instanceof Error && candidate.message && candidate.message !== 'error') {
      return candidate.message.trim()
    }
    for (const key of ['msg', 'message', 'error', 'detail']) {
      const value = readErrorField(candidate, key)
      if (typeof value === 'string' && value.trim() && value !== 'error') {
        return value.trim()
      }
    }
  }
  return ''
}

const appendUploadPreviewErrorDetail = (message: string, detail: string) => {
  const normalizedDetail = detail.trim()
  if (!normalizedDetail || normalizedDetail === message || normalizedDetail.includes(message)) {
    return message
  }
  return `${message} 原始错误：${normalizedDetail}`
}

export const resolveUploadPreviewErrorMessage = (error: unknown, fallback: string) => {
  const rawMessage = resolveNestedUploadErrorText(error) || resolveUploadErrorMessage(error, fallback)
  const normalized = rawMessage.toLowerCase()
  if (
    /minio|object\s*storage|对象存储|文件存储|bucket|s3|oss|putobject|getobject|connection refused|econnrefused|9000/.test(
      normalized
    )
  ) {
    return appendUploadPreviewErrorDetail(
      '文件存储服务不可用：请联系平台/运维检查 MinIO 或对象存储服务，本次预览不会继续。',
      rawMessage
    )
  }
  if (/unsupported|format|extension|mime|content\s*type|文件格式|格式不支持|不支持的文件|扩展名/.test(normalized)) {
    return appendUploadPreviewErrorDetail(
      '文件格式不受支持：请按页面允许的 Office、图纸源文件或 PDF 格式重新选择文件。',
      rawMessage
    )
  }
  if (/403|forbidden|permission|access denied|canupload|无权限|没有该操作权限|没有文件类别上传权限|不可访问/.test(normalized)) {
    return appendUploadPreviewErrorDetail(
      '没有文件类别上传权限：请联系文控管理员补齐该分类的 UPLOAD 权限后再上传。',
      rawMessage
    )
  }
  if (/duplicate|exists|already exists|唯一|重复|已存在|file\s*number|文件编号/.test(normalized)) {
    return appendUploadPreviewErrorDetail(
      '文件编号已存在：请先调整文件编号或改走升版流程，再提交审批。',
      rawMessage
    )
  }
  return rawMessage || fallback
}

export const buildSubmitFailureFeedback = (
  error: unknown,
  fallback: string
): UploadSubmitFailureFeedback => {
  const message = resolveUploadErrorMessage(error, fallback)
  return {
    message,
    versionFieldError: VERSION_ERROR_PATTERN.test(message) ? message : ''
  }
}

export const applySubmitFailureFeedback = (
  fieldErrors: UploadSubmitFieldErrors,
  feedback: UploadSubmitFailureFeedback
) => {
  fieldErrors.versionNo = feedback.versionFieldError
}

export const clearSubmitFieldErrors = (fieldErrors: UploadSubmitFieldErrors) => {
  fieldErrors.versionNo = ''
}

export const buildSubmitPayload = (
  draft: UploadFormDraft,
  previewFile: ControlledFileUploadRespVO,
  drawingPdfUpload?: ControlledFileUploadRespVO
): ControlledFileSubmitReqVO => ({
  categoryId: draft.categoryId as number,
  directoryId: draft.directoryId as number,
  sessionId: previewFile.sessionId,
  originalUploadTicket: previewFile.uploadTicket,
  sourceUploadTicket: previewFile.uploadTicket,
  sourceFileName: previewFile.fileName,
  drawingPdfUploadTicket: drawingPdfUpload?.uploadTicket,
  fileName: trimText(draft.fileName),
  fileNumber: trimText(draft.fileNumber),
  productMasterId: null,
  productCode: trimText(draft.productCode) || undefined,
  dccProjectCodeId: draft.dccProjectCodeId ?? undefined,
  fileTypeTaxonomyId: draft.fileTypeTaxonomyId ?? undefined,
  revisionTargetControlledFileId: draft.revisionTargetControlledFileId ?? undefined,
  needTraining: Boolean(draft.needTraining),
  selectedSignoffUserIds: draft.selectedSignoffUserIds ?? [],
  processType: draft.processType,
  changeType: draft.changeType,
  versionNo: trimText(draft.versionNo),
  effectiveDate: draft.effectiveDate,
  remark: trimText(draft.remark) || undefined
})

export const createUploadSubmitterService = (deps: UploadSubmitterServiceDeps) => {
  return {
    async uploadPreview(file: File, purpose: UploadPreviewPurpose, context: ControlledFileUploadPreviewContext) {
      return await deps.uploadPreview(file, purpose, context)
    },
    async submit(
      draft: UploadFormDraft,
      previewFile: ControlledFileUploadRespVO,
      drawingPdfUpload?: ControlledFileUploadRespVO
    ) {
      return await deps.submit(buildSubmitPayload(draft, previewFile, drawingPdfUpload))
    }
  }
}
