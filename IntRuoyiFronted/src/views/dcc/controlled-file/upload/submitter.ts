import type {
  ControlledFileChangeType,
  ControlledFileSubmitReqVO,
  ControlledFileUploadPreviewContext,
  ControlledFileUploadRespVO,
  UploadPreviewPurpose
} from '@/api/dcc/controlledFile/workflow'
import { validateAttributes } from '../project-attributes/state'
import type { ProjectAttributes } from '../project-attributes/state'

export interface UploadFormDraft {
  categoryId: number | null
  directoryId: number | null
  fileName: string
  fileNumber: string
  productMasterId: number | null
  productCode: string
  dccProjectCodeId: number | string | null
  projectFolderId?: string | null
  projectFolderChangeReason?: string
  fileTypeTaxonomyId: number | string | null
  revisionTargetControlledFileId: number | null
  revisionSourceControlledFileId: number | null
  relatedControlledFileIds: string[]
  needTraining: boolean
  processType: 'CONTROLLED_FILE' | 'EXTERNAL_REVIEW'
  changeType: ControlledFileChangeType
  versionNo?: string
  effectiveDate: string
  remark?: string
  projectAttributes?: ProjectAttributes
  selectedSignoffDepartmentIds?: Array<number | string>
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
  versionNo?: string
}

export interface UploadConfirmationRow { label: string; value: string }
export interface UploadConfirmationFacts {
  projectName?: string
  folderName?: string
  fileTypeName?: string
  relatedFiles: Array<{ fileName: string; versionNo: string; pendingEffect?: boolean }>
  signoffDepartments?: Array<{ id: string; name: string }>
  approvers: Array<{ id: string; name: string; deptName?: string }>
  approvalRule: string
}

const confirmedText = (value: string | undefined, field: string): string => {
  if (typeof value !== 'string' || !value.trim()) throw new Error(`提交确认缺少${field}`)
  return value
}

export const buildUploadConfirmationSummary = (
  draft: UploadFormDraft, preview: Pick<ControlledFileUploadRespVO, 'fileName'>, facts: UploadConfirmationFacts
): UploadConfirmationRow[] => {
  const rows: UploadConfirmationRow[] = []
  if (draft.processType === 'CONTROLLED_FILE') {
    rows.push({ label: '项目', value: confirmedText(facts.projectName, '项目') },
      { label: '存储文件夹', value: confirmedText(facts.folderName, '项目文件夹') },
      { label: '文件类型', value: confirmedText(facts.fileTypeName, '文件类型') })
  } else {
    rows.push({ label: '文件类别', value: confirmedText(facts.fileTypeName, '文件类别') })
  }
  rows.push({ label: '完整文件名称（含后缀）', value: confirmedText(preview.fileName, '源文件完整名称') },
    { label: '文件编号', value: confirmedText(draft.fileNumber, '文件编号') },
    { label: draft.processType === 'CONTROLLED_FILE' ? '初始版本号' : '版本号', value: confirmedText(draft.versionNo, '版本号') },
    { label: '预设生效日期', value: confirmedText(draft.effectiveDate, '预设生效日期') })
  if (draft.processType === 'CONTROLLED_FILE') {
    const actual = validateAttributes(draft.projectAttributes as ProjectAttributes)
    const choices: Record<string, string> = { Y: '是', N: '否', NA: '不适用' }
    rows.push({ label: '目标市场', value: actual.targetMarkets.map(market =>
      market === 'OTHER' ? `其他：${actual.otherMarket}` : market === 'NA' ? '不适用' : market).join('、') },
    { label: '注册／生产身份', value: `注册人：${choices[actual.licenseHolder!]}；生产方：${choices[actual.actualManufacturer!]}` },
    { label: '文件转移', value: `文件转移：${choices[actual.documentTransfer!]}${actual.documentTransfer === 'Y' ? `；转移至：${actual.transferTo}` : ''}` })
    if (!Array.isArray(facts.relatedFiles)) throw new Error('提交确认缺少关联选择')
    rows.push({ label: '关联文件', value: facts.relatedFiles.length ? facts.relatedFiles.map(file =>
      `${confirmedText(file.fileName, '关联文件名称')} · ${confirmedText(file.versionNo, '关联受控版本')}${file.pendingEffect ? ' · 待生效' : ''}`).join('；') : '未关联' })
    if (!Array.isArray(facts.signoffDepartments) || !facts.signoffDepartments.length) throw new Error('提交确认缺少会签部门')
    rows.push({ label: '会签部门', value: facts.signoffDepartments.map(department => confirmedText(department.name, '会签部门名称')).join('、') })
  }
  if (!Array.isArray(facts.approvers) || !facts.approvers.length) throw new Error('提交确认缺少批准人')
  rows.push({ label: '批准人', value: facts.approvers.map(account =>
    `${confirmedText(account.name, '批准人名称')}${account.deptName ? `（${account.deptName}）` : ''} · 账号ID ${confirmedText(account.id, '批准人身份')}`).join('；') },
    { label: '批准规则', value: confirmedText(facts.approvalRule, '批准规则') })
  if (draft.processType === 'CONTROLLED_FILE') rows.push({ label: '培训', value: draft.needTraining ? '需要培训' : '不需要培训' })
  return rows
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
const FILE_VERSION_FIELD_ERROR_PATTERN = /(版本|version|文件编号|file\s*number|编号|logical\s*document\s*chain)/i
const FILE_NUMBER_CHAIN_CONFLICT_RAW_MESSAGE =
  'Controlled file number conflicts with the existing logical document chain'
const FILE_NUMBER_CHAIN_CONFLICT_MESSAGE =
  '该文件编号存在版本链冲突，当前不可提交，请选择正确历史文件或联系管理员处理。'
const VERSION_INVALID_ERROR_CODE = 'CONTROLLED_FILE_VERSION_INVALID'
const VERSION_INVALID_MESSAGE = '版本号格式不正确，请使用 V1.0、V2.0 或 1.0 这类数字版本。'
const CONTROLLED_INITIAL_VERSION_INVALID_MESSAGE =
  '初始版本号格式不正确，请使用 A/1、B/1 等“修订版/1”格式。'
const PRODUCT_CODE_PATTERN = /^[A-Za-z0-9]{14}$/
const DRAWING_SOURCE_EXT_PATTERN = /\.(dwg|sldprt|sldasm|slddrw)$/i
const PRODUCT_BOUND_CATEGORY_PREFIXES = ['DCC_FVM_DHF_', 'DCC_FVM_DMR_']

export const isFileNumberChainConflictMessage = (message: string | null | undefined) => {
  const normalizedMessage = trimText(message).toLowerCase()
  return Boolean(
    normalizedMessage &&
      (normalizedMessage === FILE_NUMBER_CHAIN_CONFLICT_MESSAGE.toLowerCase() ||
        normalizedMessage.includes(FILE_NUMBER_CHAIN_CONFLICT_RAW_MESSAGE.toLowerCase()) ||
        normalizedMessage.includes('controlled_file_file_number_conflict') ||
        normalizedMessage.includes('logical document chain'))
  )
}

export const EDITABLE_SOURCE_EXTENSIONS = [
  'doc',
  'docx',
  'xls',
  'xlsx',
  'pdf',
  'dwg',
  'sldprt',
  'sldasm',
  'slddrw'
] as const
export const EDITABLE_SOURCE_ACCEPT = EDITABLE_SOURCE_EXTENSIONS.map((item) => `.${item}`).join(',')
export const EDITABLE_SOURCE_MESSAGE =
  '仅支持 doc、docx、xls、xlsx、pdf、dwg、sldprt、sldasm、slddrw 等源文件'
const EDITABLE_SOURCE_EXT_PATTERN = /\.(doc|docx|xls|xlsx|pdf|dwg|sldprt|sldasm|slddrw)$/i

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
      message: EDITABLE_SOURCE_MESSAGE
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

const normalizeKnownUploadErrorMessage = (
  message: string,
  fallback: string,
  processType?: UploadFormDraft['processType']
) => {
  const rawMessage = trimText(message)
  if (!rawMessage || rawMessage === 'error') {
    return fallback
  }
  const normalizedMessage = rawMessage.toLowerCase()
  if (isFileNumberChainConflictMessage(rawMessage)) {
    return FILE_NUMBER_CHAIN_CONFLICT_MESSAGE
  }
  if (
    normalizedMessage.includes(VERSION_INVALID_ERROR_CODE.toLowerCase()) ||
    normalizedMessage.includes('controlled file version format is invalid') ||
    /version\s*format\s*is\s*invalid|invalid\s*version|版本号.*(无效|非法|格式)/i.test(rawMessage)
  ) {
    return processType === 'CONTROLLED_FILE'
      ? CONTROLLED_INITIAL_VERSION_INVALID_MESSAGE
      : VERSION_INVALID_MESSAGE
  }
  return rawMessage
}

export const resolveUploadErrorMessage = (
  error: unknown,
  fallback: string,
  processType?: UploadFormDraft['processType']
) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return normalizeKnownUploadErrorMessage(error.message, fallback, processType)
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return normalizeKnownUploadErrorMessage(error, fallback, processType)
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
  if (/403|forbidden|permission|access denied|无权限|没有该操作权限|不可访问/.test(normalized)) {
    return appendUploadPreviewErrorDetail(
      '当前账号缺少受控文件提交权限：请联系管理员补齐受控文件提交权限后再上传。',
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
  fallback: string,
  processType?: UploadFormDraft['processType']
): UploadSubmitFailureFeedback => {
  const message = normalizeKnownUploadErrorMessage(
    resolveNestedUploadErrorText(error) || resolveUploadErrorMessage(error, fallback, processType),
    fallback,
    processType
  )
  return {
    message,
    versionFieldError: FILE_VERSION_FIELD_ERROR_PATTERN.test(message) ? message : ''
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

const uploadIdentity = (value: number | string | null | undefined): string => {
  if (value == null || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(String(value)) > 9223372036854775807n) {
    throw new Error('请选择有效的项目文件夹或关联文件')
  }
  return String(value)
}

const placementReason = (value?: string): string => {
  const reason = value?.trim()
  if (!reason || reason.length > 500) throw new Error('请填写500字以内的文件夹登记说明')
  return reason
}

export const buildSubmitPayload = (
  draft: UploadFormDraft,
  previewFile: ControlledFileUploadRespVO,
  drawingPdfUpload?: ControlledFileUploadRespVO,
  attachmentUploads: ControlledFileUploadRespVO[] = []
): ControlledFileSubmitReqVO => ({
  categoryId: draft.categoryId as number,
  directoryId: draft.directoryId as number,
  sessionId: previewFile.sessionId,
  idempotencyKey: previewFile.sessionId,
  originalUploadTicket: previewFile.uploadTicket,
  sourceUploadTicket: previewFile.uploadTicket,
  sourceFileName: previewFile.fileName,
  drawingPdfUploadTicket: drawingPdfUpload?.uploadTicket,
  attachmentUploadTickets: attachmentUploads.map((upload) => ({
    uploadTicket: upload.uploadTicket,
    sessionId: upload.sessionId
  })),
  fileName: trimText(draft.fileName),
  fileNumber: trimText(draft.fileNumber),
  productMasterId: null,
  productCode: trimText(draft.productCode) || undefined,
  dccProjectCodeId: draft.dccProjectCodeId ?? undefined,
  projectFolderId: draft.processType === 'EXTERNAL_REVIEW' ? undefined : uploadIdentity(draft.projectFolderId),
  projectFolderChangeReason: draft.processType === 'EXTERNAL_REVIEW' ? undefined : placementReason(draft.projectFolderChangeReason),
  fileTypeTaxonomyId: draft.fileTypeTaxonomyId ?? undefined,
  revisionTargetControlledFileId: draft.revisionTargetControlledFileId ?? undefined,
  revisionSourceControlledFileId: draft.revisionSourceControlledFileId ?? undefined,
  relatedControlledFileIds: (draft.relatedControlledFileIds ?? []).map(uploadIdentity),
  projectAttributes: draft.processType === 'EXTERNAL_REVIEW' ? undefined
    : JSON.parse(JSON.stringify(validateAttributes(draft.projectAttributes as ProjectAttributes))),
  selectedSignoffDepartmentIds: draft.selectedSignoffDepartmentIds === undefined ? undefined : [...draft.selectedSignoffDepartmentIds],
  needTraining: Boolean(draft.needTraining),
  processType: draft.processType,
  changeType: draft.changeType,
  versionNo: trimText(draft.versionNo) || undefined,
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
      drawingPdfUpload?: ControlledFileUploadRespVO,
      attachmentUploads: ControlledFileUploadRespVO[] = []
    ) {
      return await deps.submit(buildSubmitPayload(draft, previewFile, drawingPdfUpload, attachmentUploads))
    }
  }
}
