import type {
  ControlledFileChangeType,
  DccEffectiveMode,
  ApprovalMatrixConfirmationSelection,
  ControlledFileSubmitReqVO,
  ControlledFileUploadPreviewContext,
  ControlledFileUploadRespVO,
  UploadPreviewPurpose
} from '@/api/dcc/controlledFile/workflow'
import { validateAttributes } from '../project-attributes/state'
import type { ProjectAttributes } from '../project-attributes/state'
import { normalizeDistributionDepartments, requireUploadApplicationAccount } from './signoff-departments'
import type { UploadApprovalGroup, UploadApplicationAccount, UploadApprovalAccount } from './signoff-departments'
import { requireApprovalMatrixSelection } from './approval-matrix'

export interface UploadFormDraft {
  applicationCustomMatrix?: { approverUserId: string | null; documentControlUserId: string | null } | null
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
  effectiveMode: DccEffectiveMode | null
  effectiveDate: string | null
  remark?: string
  projectAttributes?: ProjectAttributes
  selectedSignoffDepartmentIds?: Array<number | string>
  selectedDistributionDepartmentIds?: Array<number | string>
  approvalMatrixSelection?: ApprovalMatrixConfirmationSelection
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
  distributionDepartments?: Array<{ id: string; name: string }>
  approvers: Array<{ id: string; name: string; deptName?: string }>
  compiler?: UploadApplicationAccount
  supervisor?: UploadApplicationAccount
  documentControl?: UploadApprovalAccount
  approvalRule: string
  approvalGroups?: UploadApprovalGroup[]
  runtimeCapabilityLimitations?: string[]
  editable?: Pick<ControlledFileUploadRespVO, 'fileName'>
  drawingPdf?: Pick<ControlledFileUploadRespVO, 'fileName'>
  attachments?: Array<Pick<ControlledFileUploadRespVO, 'fileName'>>
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
    const compiler = requireUploadApplicationAccount(facts.compiler, '编制人')
    const supervisor = requireUploadApplicationAccount(facts.supervisor, '直属主管')
    rows.push({ label: '项目', value: confirmedText(facts.projectName, '项目') },
      { label: '存储文件夹', value: confirmedText(facts.folderName, '项目文件夹') },
      { label: '文件类型', value: confirmedText(facts.fileTypeName, '文件类型') },
      { label: '编制人', value: `${compiler.name}（${compiler.deptName}）` },
      { label: '直属主管', value: `${supervisor.name}（${supervisor.deptName}）` })
  } else {
    rows.push({ label: '文件类别', value: confirmedText(facts.fileTypeName, '文件类别') })
  }
  rows.push({ label: '完整文件名称（含后缀）', value: confirmedText(preview.fileName, '源文件完整名称') },
    { label: '文件编号', value: confirmedText(draft.fileNumber, '文件编号') },
    { label: draft.processType === 'CONTROLLED_FILE' ? '初始版本号' : '版本号', value: confirmedText(draft.versionNo, '版本号') })
  if (draft.processType === 'EXTERNAL_REVIEW') rows.push({ label: '生效日期', value: confirmedText(draft.effectiveDate ?? undefined, '生效日期') })
  else {
    const effect = buildUploadEffectiveChoice(draft.effectiveMode, draft.effectiveDate, resolveUploadTodayDate())
    rows.push({ label: '生效方式', value: effect.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE' ? '文控审核后立即生效' : effect.effectiveMode === 'DELAYED' ? '延迟生效' : '按预设日期生效（历史申请）' })
    if (effect.effectiveMode === 'PRESET_DATE') rows.push({ label: '预设生效日期', value: effect.effectiveDate! })
    if (effect.effectiveMode === 'DELAYED') rows.push({ label: '延迟生效确认', value: '文控审核及必要培训完成后，由原编制人选择生效时间并电子签名确认' })
    if (effect.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE') rows.push({ label: '生效时间', value: '文控标准化审核完成时记录本轮真实电子签名时间；日常可用和下发须同时满足受控且已生效。' })
  }
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
    const ids = normalizeDistributionDepartments(draft.selectedDistributionDepartmentIds)
    if (!Array.isArray(facts.distributionDepartments) || facts.distributionDepartments.length !== ids.length
      || facts.distributionDepartments.some((department, index) => !department || department.id !== ids[index]))
      throw new Error('提交确认下发部门与本次实际名单不一致')
    rows.push({ label: '下发部门', value: ids.length
      ? facts.distributionDepartments.map(department => confirmedText(department.name, '下发部门名称')).join('、') : '未选择下发部门' },
      { label: '下发范围', value: '受控且已生效后，向所选下发部门的全部人员下发；签收不增加下载、打印或编制权限。' })
  }
  if (!Array.isArray(facts.approvers) || !facts.approvers.length) throw new Error('提交确认缺少批准人')
  rows.push({ label: '批准人', value: facts.approvers.map(account =>
    `${confirmedText(account.name, '批准人名称')}${account.deptName ? `（${account.deptName}）` : ''} · 账号ID ${confirmedText(account.id, '批准人身份')}`).join('；') },
    { label: '批准规则', value: confirmedText(facts.approvalRule, '批准规则') })
  if (draft.approvalMatrixSelection?.approvalSourceMode === 'APPLICATION_CUSTOM') {
    const selection = requireApprovalMatrixSelection(draft.approvalMatrixSelection, { phase: 'SUBMIT', actionType: draft.changeType }, {
      applicationCustomMatrix: draft.applicationCustomMatrix ?? null, selectedSignoffDepartmentIds: draft.selectedSignoffDepartmentIds ?? null
    })
    const config = selection.applicationCustomMatrix!
    if (facts.approvers.length !== 1 || facts.approvers[0].id !== config.approverUserId)
      throw new Error('本次自定义批准人与确认名单不一致')
    const control = facts.documentControl
    if (!control || uploadIdentity(control.id) !== config.documentControlUserId) throw new Error('提交确认缺少本次正式文控审核人')
    rows.push({ label: '审批来源', value: '本次自定义审批配置，仅用于本申请，不修改公共矩阵' },
      { label: '文控审核人', value: `${confirmedText(control.name, '文控审核人名称')}${control.deptName ? `（${control.deptName}）` : ''} · 账号ID ${control.id}` },
      { label: '本次自定义矩阵', value: '直属主管独立审核；各选定部门负责人签名指派后逐部门完成会签，全部通过；由本次所选批准人批准并确认负责人，最后由本次所选文控审核人审核。' })
  }
  if (facts.approvalGroups) for (const group of facts.approvalGroups)
    rows.push({ label: `原批准组 ${group.sourceNodeId}`, value: confirmedText(group.summary, '原批准组规则') })
  if (facts.runtimeCapabilityLimitations?.length) rows.push({ label: '运行能力说明', value: facts.runtimeCapabilityLimitations.join('；') })
  if (draft.processType === 'CONTROLLED_FILE') rows.push({ label: '培训', value: draft.needTraining ? '需要培训' : '不需要培训' })
  rows.push({ label: '可编辑版本', value: facts.editable ? confirmedText(facts.editable.fileName, '可编辑版本名称') : '未上传' })
  rows.push({ label: '图纸 PDF', value: facts.drawingPdf ? confirmedText(facts.drawingPdf.fileName, '图纸 PDF 名称') : '未上传' },
    { label: '普通附件', value: facts.attachments?.length
      ? facts.attachments.map(file => confirmedText(file.fileName, '附件名称')).join('；') : '未上传' },
    { label: '提交备注', value: draft.remark?.trim() || '未填写' })
  return rows
}

interface UploadSubmitterServiceDeps {
  uploadPreview: (
    file: File,
    purpose: UploadPreviewPurpose,
    context: ControlledFileUploadPreviewContext
  ) => Promise<ControlledFileUploadRespVO>
  submit: (data: ControlledFileSubmitReqVO) => Promise<number>
  createWorking?: (data: ControlledFileSubmitReqVO) => Promise<number>
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
  'slddrw',
  'png',
  'txt'
] as const
export const EDITABLE_SOURCE_ACCEPT = EDITABLE_SOURCE_EXTENSIONS.map((item) => `.${item}`).join(',')
export const EDITABLE_SOURCE_MESSAGE =
  '仅支持 doc、docx、xls、xlsx、pdf、dwg、sldprt、sldasm、slddrw、png、txt 等源文件'
const EDITABLE_SOURCE_EXT_PATTERN = /\.(doc|docx|xls|xlsx|pdf|dwg|sldprt|sldasm|slddrw|png|txt)$/i

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

export const validateUploadEffectiveDate = (value: string | null | undefined, today: string): UploadSelectionValidation => {
  const calendarDate = (text: string) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return false
    const [year, month, day] = text.split('-').map(Number)
    if (year < 1) return false
    const date = new Date(0)
    date.setUTCFullYear(year, month - 1, day)
    date.setUTCHours(0, 0, 0, 0)
    return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
  }
  if (!calendarDate(today)) throw new Error('当前日期无效，无法校验预设生效日期')
  if (!value || !calendarDate(value)) return { valid: false, message: '请选择有效的预设生效日期' }
  if (value < today) return { valid: false, message: '预设生效日期不能早于今天，请重新选择' }
  return { valid: true }
}

export const resolveUploadTodayDate = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

export const buildUploadEffectiveChoice = (mode: unknown, date: string | null | undefined, today: string, executionContract?: string | null): {
  effectiveMode: DccEffectiveMode; effectiveDate: string | null
} => {
  if (mode === 'DELAYED') {
    if (executionContract != null && executionContract !== 'PUBLIC_MATRIX_NATIVE_V2') throw new Error('延迟生效确认仅支持本次正式新流程，历史申请不能隐式升级')
    if (date !== null) throw new Error('延迟生效在申请阶段不填写具体生效时间或预设日期')
    return { effectiveMode: mode, effectiveDate: null }
  }
  if (mode === 'AFTER_STANDARDIZATION_SIGNATURE') {
    if (date !== null) throw new Error('文控标准化后生效时不能填写预设日期')
    return { effectiveMode: mode, effectiveDate: null }
  }
  if (mode === 'PRESET_DATE') {
    if (executionContract === 'PUBLIC_MATRIX_NATIVE_V2') throw new Error('新流程不能使用历史预设日期方式')
    const result = validateUploadEffectiveDate(date, today)
    if (!result.valid) throw new Error(result.message)
    return { effectiveMode: mode, effectiveDate: date as string }
  }
  throw new Error('请选择生效方式')
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
  return normalizeKnownUploadErrorMessage(resolveNestedUploadErrorText(error) || fallback, fallback, processType)
}

const readErrorField = (source: unknown, key: string): unknown => {
  if (!source || typeof source !== 'object') {
    return undefined
  }
  return (source as Record<string, unknown>)[key]
}

const resolveNestedUploadErrorText = (error: unknown): string => {
  const responseMessage = readErrorField(readErrorField(readErrorField(error, 'response'), 'data'), 'msg')
  if (typeof responseMessage === 'string' && responseMessage.trim() && responseMessage !== 'error') {
    return responseMessage.trim()
  }
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
  const businessCode = readErrorField(readErrorField(readErrorField(error, 'response'), 'data'), 'code')
  if (businessCode === 1080000348 || businessCode === '1080000348'
    || rawMessage === '文件名称已存在，请先走作废或者升版路线') {
    return '文件名称已被占用；受控文件及20年保留期内的作废文件继续占用原名。请引用已有文件，或在既有文件链中办理升版。'
  }
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

const placementReason = (value?: string): string | undefined => {
  const reason = value?.trim()
  if (reason && reason.length > 500) throw new Error('文件夹登记说明不能超过500字')
  return reason || undefined
}

export const validateUploadTickets = (
  preview: ControlledFileUploadRespVO,
  drawingPdf?: ControlledFileUploadRespVO,
  attachments: ControlledFileUploadRespVO[] = [], editableFile?: ControlledFileUploadRespVO
) => {
  const identities = new Set<string>()
  for (const upload of [preview, ...(editableFile ? [editableFile] : []), ...(drawingPdf ? [drawingPdf] : []), ...attachments]) {
    if (!upload || !trimText(upload.uploadTicket) || !trimText(upload.sessionId) || !trimText(upload.fileName))
      throw new Error('上传票据或文件身份不完整，请重新上传')
    if (upload.expireTime != null && (!Number.isFinite(upload.expireTime) || upload.expireTime <= Date.now()))
      throw new Error('上传票据已过期，请重新上传')
    if (identities.has(upload.uploadTicket)) throw new Error('上传票据不能重复绑定正文或附件')
    identities.add(upload.uploadTicket)
  }
  if (editableFile && editableFile.sessionId !== preview.sessionId)
    throw new Error('可编辑版本与只读版本上传会话不一致，请重新上传')
  if (drawingPdf && drawingPdf.sessionId !== preview.sessionId)
    throw new Error('图纸 PDF 与正文上传会话不一致，请重新上传')
}

export const validateUploadPreviewResponse = (upload: ControlledFileUploadRespVO, selectedFileName: string, requirePreviewKind: boolean) => {
  validateUploadTickets(upload)
  if (upload.fileName !== selectedFileName) throw new Error('上传原件完整名称与本次选择文件不一致，请重新上传')
  if (requirePreviewKind && !['PDF', 'IMAGE', 'TEXT', 'OFFICE', 'VIDEO', 'AUDIO', 'DOWNLOAD_ONLY'].includes(upload.previewKind || ''))
    throw new Error('上传响应缺少有效的正式预览类型，请重新上传')
}

const buildUploadPayload = (
  draft: UploadFormDraft,
  previewFile: ControlledFileUploadRespVO,
  drawingPdfUpload: ControlledFileUploadRespVO | undefined,
  attachmentUploads: ControlledFileUploadRespVO[],
  creatingDraft: boolean,
  editableFile?: ControlledFileUploadRespVO
): ControlledFileSubmitReqVO => {
  validateUploadTickets(previewFile, drawingPdfUpload, attachmentUploads, editableFile)
  const matrix = draft.processType === 'EXTERNAL_REVIEW' ? undefined : requireApprovalMatrixSelection(draft.approvalMatrixSelection,
    { phase: creatingDraft ? 'SAVE' : 'SUBMIT', actionType: draft.changeType }, {
      applicationCustomMatrix: draft.applicationCustomMatrix ?? null, selectedSignoffDepartmentIds: draft.selectedSignoffDepartmentIds ?? null
    })
  if (draft.processType !== 'EXTERNAL_REVIEW' && matrix?.approvalSourceMode !== 'APPLICATION_CUSTOM' && draft.applicationCustomMatrix != null)
    throw new Error('当前公共来源不能携带其他类型的自定义账号配置')
  if (creatingDraft && draft.effectiveMode === null && draft.effectiveDate !== null)
    throw new Error('未选择生效方式的草稿不能填写预设日期')
  const effective = draft.processType === 'EXTERNAL_REVIEW'
    ? { effectiveDate: confirmedText(draft.effectiveDate ?? undefined, '生效日期') }
    : creatingDraft && draft.effectiveMode === null ? { effectiveDate: null }
    : creatingDraft && draft.effectiveMode === 'PRESET_DATE' && draft.effectiveDate === null
      ? { effectiveMode: draft.effectiveMode, effectiveDate: null }
    : buildUploadEffectiveChoice(draft.effectiveMode, draft.effectiveDate, resolveUploadTodayDate(), draft.approvalMatrixSelection?.executionContract)
  return {
  ...matrix,
  categoryId: draft.categoryId as number,
  ...(draft.processType === 'CONTROLLED_FILE' && draft.changeType === 'NEW'
    ? {} : { directoryId: draft.directoryId as number }),
  sessionId: previewFile.sessionId,
  idempotencyKey: previewFile.sessionId,
  originalUploadTicket: draft.processType === 'EXTERNAL_REVIEW' ? previewFile.uploadTicket : undefined,
  sourceUploadTicket: draft.processType === 'EXTERNAL_REVIEW' ? previewFile.uploadTicket : undefined,
  readOnlyUploadTicket: draft.processType === 'CONTROLLED_FILE' ? previewFile.uploadTicket : undefined,
  editableUploadTicket: editableFile?.uploadTicket,
  sourceFileName: editableFile?.fileName ?? previewFile.fileName,
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
  selectedDistributionDepartmentIds: draft.processType === 'EXTERNAL_REVIEW' || (creatingDraft && draft.selectedDistributionDepartmentIds === undefined)
    ? undefined : normalizeDistributionDepartments(draft.selectedDistributionDepartmentIds),
  needTraining: Boolean(draft.needTraining),
  processType: draft.processType,
  changeType: draft.changeType,
  versionNo: trimText(draft.versionNo) || undefined,
  ...effective,
  remark: trimText(draft.remark) || undefined
  }
}

export const buildSubmitPayload = (draft: UploadFormDraft, preview: ControlledFileUploadRespVO,
  drawingPdf?: ControlledFileUploadRespVO, attachments: ControlledFileUploadRespVO[] = [], editableFile?: ControlledFileUploadRespVO): ControlledFileSubmitReqVO =>
  buildUploadPayload(draft, preview, drawingPdf, attachments, false, editableFile)

export const buildWorkingDraftPayload = (draft: UploadFormDraft, preview: ControlledFileUploadRespVO,
  drawingPdf?: ControlledFileUploadRespVO, attachments: ControlledFileUploadRespVO[] = [], editableFile?: ControlledFileUploadRespVO): ControlledFileSubmitReqVO =>
  buildUploadPayload(draft, preview, drawingPdf, attachments, true, editableFile)

export const createUploadSubmitterService = (deps: UploadSubmitterServiceDeps) => {
  return {
    async uploadPreview(file: File, purpose: UploadPreviewPurpose, context: ControlledFileUploadPreviewContext) {
      return await deps.uploadPreview(file, purpose, context)
    },
    async submit(
      draft: UploadFormDraft,
      previewFile: ControlledFileUploadRespVO,
      drawingPdfUpload?: ControlledFileUploadRespVO,
      attachmentUploads: ControlledFileUploadRespVO[] = [],
      editableFile?: ControlledFileUploadRespVO
    ) {
      return await deps.submit(buildSubmitPayload(draft, previewFile, drawingPdfUpload, attachmentUploads, editableFile))
    },
    async saveDraft(draft: UploadFormDraft, preview: ControlledFileUploadRespVO,
      drawingPdf?: ControlledFileUploadRespVO, attachments: ControlledFileUploadRespVO[] = [], editableFile?: ControlledFileUploadRespVO) {
      if (!deps.createWorking) throw new Error('正式工作申请保存服务未接入')
      return await deps.createWorking(buildWorkingDraftPayload(draft, preview, drawingPdf, attachments, editableFile))
    }
  }
}
