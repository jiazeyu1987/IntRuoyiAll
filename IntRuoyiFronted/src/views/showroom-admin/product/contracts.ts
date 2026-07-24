import type { DeptVO } from '@/api/system/dept'
import type { UserVO } from '@/api/system/user'

export interface ProductDiscussionSummary {
  totalComments: number
  openComments: number
  resolvedComments: number
}

export interface ProductNarrationAvailability {
  narrationVersionId: number
  language: string
  audienceType: string
  status: string
  live: boolean
  audioReady: boolean
}

export interface ShowroomProductAttachment {
  id?: number | null
  assetType: 'image' | 'video' | 'text'
  fileId: number
  url?: string
  originalName: string
  mimeType: string
  size: number
  displayOrder: number
}

export interface ShowroomProductDetail {
  productId: number
  productMasterId: number | null
  productCode: string
  legacyProductCode?: string | null
  currentRevisionId: number
  incomplete: boolean
  live: boolean
  editable: boolean
  revisionId: number
  revisionNo: number
  status: string
  nameCn: string
  nameEn: string
  fields: Record<string, string>
  relatedProductIds: number[]
  discussionSummary: ProductDiscussionSummary
  narrations: ProductNarrationAvailability[]
  attachments: ShowroomProductAttachment[]
}

export interface ProductVersionDiffItem {
  fieldCode: string
  label: string
  oldValue: string
  newValue: string
  operatorId: number
  operatorAction: string
  createdAt: string
}

export interface ProductVersionHistory {
  revisionId: number
  revisionNo: number
  status: string
  diffItems: ProductVersionDiffItem[]
}

export interface ProductDraftForm {
  productMasterId: number | null
  productCode: string
  legacyProductCode: string
  nameCn: string
  nameEn: string
  fields: Record<string, string>
}

export type ProductFieldDefinition = {
  key: string
  label: string
  labelEn: string
  type: 'text' | 'textarea' | 'select'
  translatable: boolean
}

export interface ShowroomCompanyOption {
  id: number
  name: string
  ownerType: 'YINGTAI' | 'SUBSIDIARY'
}

export interface ShowroomApprovalRoutePreview {
  submitterUserId: number
  submitterDeptId: number | null
  supervisorUserId: number | null
  supervisorName: string
  skipSupervisorReview: boolean
}

export interface ProductAssignmentUserOption {
  id: number
  username: string
  nickname: string
}

export const SHOWROOM_PRODUCT_OWNER_LABEL = '瑛泰医疗'
export const SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE = '__PRODUCT_ALL_FIELDS__'
export const SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_LABEL = '整产品资料'
export const SHOWROOM_PRODUCT_BU_OPTIONS = [
  { label: '非血管BU', value: '非血管BU' },
  { label: '外周血管BU', value: '外周血管BU' },
  { label: '结构心BU', value: '结构心BU' },
  { label: '心血管BU', value: '心血管BU' },
  { label: '神经血管BU', value: '神经血管BU' },
  { label: '心脏电生理BU', value: '心脏电生理BU' }
] as const

export const productFieldDefinitions = [
  {
    key: 'owner_company_id',
    label: '所属公司',
    labelEn: 'Owner Company',
    type: 'text',
    translatable: false
  },
  {
    key: 'product_owner_type',
    label: '产品归属/类型',
    labelEn: 'Product Ownership / Type',
    type: 'select',
    translatable: false
  },
  {
    key: 'lifecycle_stage',
    label: '生命周期',
    labelEn: 'Lifecycle Stage',
    type: 'select',
    translatable: false
  },
  {
    key: 'target_market',
    label: '在售国家',
    labelEn: 'Countries on Sale',
    type: 'textarea',
    translatable: true
  },
  {
    key: 'pipeline_layout',
    label: 'BU',
    labelEn: 'BU',
    type: 'select',
    translatable: true
  },
  {
    key: 'indication_content',
    label: '适应症',
    labelEn: 'Indication',
    type: 'textarea',
    translatable: true
  },
  {
    key: 'core_selling_points',
    label: '卖点文案',
    labelEn: 'Selling Points Copy',
    type: 'textarea',
    translatable: true
  },
  {
    key: 'model_specification',
    label: '型号规格',
    labelEn: 'Model Specification',
    type: 'textarea',
    translatable: true
  },
  { key: 'cover_image', label: '封面', labelEn: 'Cover Image', type: 'text', translatable: false },
  {
    key: 'registration_certificate',
    label: '注册证',
    labelEn: 'Registration Certificate',
    type: 'text',
    translatable: true
  },
  {
    key: 'clinical_effect',
    label: '临床效果',
    labelEn: 'Clinical Effect',
    type: 'textarea',
    translatable: true
  },
  { key: 'fim_status', label: 'FIM状态', labelEn: 'FIM Status', type: 'text', translatable: true }
] as const satisfies readonly ProductFieldDefinition[]

export const productAdvancedFieldKeys = [
  'registration_certificate',
  'clinical_effect',
  'fim_status'
] as const

export const resolveProductEnglishFieldKey = (fieldKey: string) => `${fieldKey}_en`

export const productBasicFieldDefinitions = productFieldDefinitions.filter(
  (definition) => !productAdvancedFieldKeys.includes(definition.key as (typeof productAdvancedFieldKeys)[number])
)

export const productAdvancedFieldDefinitions = productFieldDefinitions.filter((definition) =>
  productAdvancedFieldKeys.includes(definition.key as (typeof productAdvancedFieldKeys)[number])
)

export const productTranslatableFieldDefinitions = productFieldDefinitions.filter(
  (definition) => definition.translatable
)

export const productEnglishFieldDefinitions = [
  {
    key: 'target_market_en',
    sourceKey: 'target_market',
    label: '在售国家(英文)',
    labelEn: 'Countries on Sale'
  },
  {
    key: 'pipeline_layout_en',
    sourceKey: 'pipeline_layout',
    label: 'BU',
    labelEn: 'BU'
  },
  {
    key: 'indication_content_en',
    sourceKey: 'indication_content',
    label: '适应症(英文)',
    labelEn: 'Indication'
  },
  {
    key: 'core_selling_points_en',
    sourceKey: 'core_selling_points',
    label: '卖点文案(英文)',
    labelEn: 'Selling Points Copy'
  },
  {
    key: 'model_specification_en',
    sourceKey: 'model_specification',
    label: '型号规格(英文)',
    labelEn: 'Model Specification'
  },
  {
    key: 'registration_certificate_en',
    sourceKey: 'registration_certificate',
    label: '注册证(英文)',
    labelEn: 'Registration Certificate'
  },
  {
    key: 'clinical_effect_en',
    sourceKey: 'clinical_effect',
    label: '临床效果(英文)',
    labelEn: 'Clinical Effect'
  },
  {
    key: 'fim_status_en',
    sourceKey: 'fim_status',
    label: 'FIM状态(英文)',
    labelEn: 'FIM Status'
  }
] as const

const showroomOwnerDeptNameCandidates = [
  '瑛泰医疗',
  '上海瑛泰医疗器械股份有限公司',
  '上海英泰医疗器械股份有限公司',
  '英泰医疗'
] as const

const statusTextMap: Record<string, string> = {
  IN_FILLING: '指派中',
  DRAFT: '草稿',
  PENDING_SUPERVISOR_REVIEW: '主管审核中',
  PENDING_SUPERVISOR_APPROVAL: '主管审核中',
  PENDING_GAOXIN_APPROVAL: '企宣审批中',
  PENDING: '审批中',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  PUBLISHED: '已发布'
}

const requiredFieldKeys = [
  ...productFieldDefinitions.map((item) => item.key),
  ...productTranslatableFieldDefinitions.map((item) => resolveProductEnglishFieldKey(item.key))
]

export const buildShowroomCompanyOptions = (depts: DeptVO[]): ShowroomCompanyOption[] => {
  const topLevelCompanies = depts.filter((dept) => dept.parentId === 0)
  const ownerDept = showroomOwnerDeptNameCandidates
    .map((name) => topLevelCompanies.find((dept) => dept.name === name))
    .find((dept) => Boolean(dept))

  if (!ownerDept) {
    return []
  }

  return [
    {
      id: ownerDept.id,
      name: SHOWROOM_PRODUCT_OWNER_LABEL,
      ownerType: 'YINGTAI'
    }
  ]
}

export const resolveShowroomApprovalRoutePreview = (
  submitterUserId: number | null | undefined,
  submitterDeptId: number | null | undefined,
  depts: DeptVO[],
  users: UserVO[]
): ShowroomApprovalRoutePreview => {
  const buildSkipPreview = (resolvedDeptId: number | null, supervisorName: string) => ({
    submitterUserId: submitterUserId as number,
    submitterDeptId: resolvedDeptId,
    supervisorUserId: null,
    supervisorName,
    skipSupervisorReview: true
  })
  if (!submitterUserId) {
    throw new Error('当前登录用户缺失，无法提交产品审批')
  }
  if (!submitterDeptId) {
    return buildSkipPreview(null, '已跳过主管，直接进入企宣审批')
  }
  const dept = depts.find((item) => item.id === submitterDeptId)
  if (!dept) {
    return buildSkipPreview(submitterDeptId, '未解析到主管，直接进入企宣审批')
  }
  if (!dept.leaderUserId) {
    return buildSkipPreview(submitterDeptId, '未解析到主管，直接进入企宣审批')
  }
  const supervisor = users.find((item) => item.id === dept.leaderUserId)
  if (!supervisor) {
    return buildSkipPreview(submitterDeptId, '未解析到主管，直接进入企宣审批')
  }
  return {
    submitterUserId,
    submitterDeptId,
    supervisorUserId: supervisor.id,
    supervisorName: supervisor.nickname,
    skipSupervisorReview: false
  }
}

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`产品详情缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`产品详情缺少字符串字段：${fieldName}`)
  }
  return value
}

const expectNullableString = (value: unknown, fieldName: string) => {
  if (value === undefined || value === null) {
    return ''
  }
  return expectString(value, fieldName, true)
}

const expectHistoryTimestampString = (value: unknown, fieldName: string) => {
  if (value === undefined || value === null) {
    return ''
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }
  return expectString(value, fieldName, true)
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`产品详情缺少数值字段：${fieldName}`)
  }
  return value
}

const expectNullableNumber = (value: unknown, fieldName: string) => {
  if (value === null) {
    return null
  }
  return expectNumber(value, fieldName)
}

const expectBoolean = (value: unknown, fieldName: string) => {
  if (typeof value !== 'boolean') {
    throw new Error(`产品详情缺少布尔字段：${fieldName}`)
  }
  return value
}

const expectNumberArray = (value: unknown, fieldName: string) => {
  if (!Array.isArray(value)) {
    throw new Error(`产品详情缺少数组字段：${fieldName}`)
  }
  return value.map((item, index) => expectNumber(item, `${fieldName}[${index}]`))
}

const expectStringMap = (value: unknown, fieldName: string) => {
  const record = expectRecord(value, fieldName)
  const normalized: Record<string, string> = {}
  for (const key of requiredFieldKeys) {
    normalized[key] = record[key] === undefined || record[key] === null ? '' : String(record[key])
  }
  return normalized
}

const expectDiscussionSummary = (value: unknown): ProductDiscussionSummary => {
  const record = expectRecord(value, 'discussionSummary')
  return {
    totalComments: expectNumber(record.totalComments, 'discussionSummary.totalComments'),
    openComments: expectNumber(record.openComments, 'discussionSummary.openComments'),
    resolvedComments: expectNumber(record.resolvedComments, 'discussionSummary.resolvedComments')
  }
}

const expectNarrations = (value: unknown): ProductNarrationAvailability[] => {
  if (!Array.isArray(value)) {
    throw new Error('产品详情缺少数组字段：narrations')
  }
  return value.map((item, index) => {
    const record = expectRecord(item, `narrations[${index}]`)
    return {
      narrationVersionId: expectNumber(record.narrationVersionId, `narrations[${index}].narrationVersionId`),
      language: expectString(record.language, `narrations[${index}].language`),
      audienceType: expectString(record.audienceType, `narrations[${index}].audienceType`),
      status: expectString(record.status, `narrations[${index}].status`),
      live: expectBoolean(record.live, `narrations[${index}].live`),
      audioReady: expectBoolean(record.audioReady, `narrations[${index}].audioReady`)
    }
  })
}

const expectProductAttachments = (value: unknown): ShowroomProductAttachment[] => {
  if (!Array.isArray(value)) {
    throw new Error('产品详情缺少数组字段：attachments')
  }
  return value.map((item, index) => {
    const record = expectRecord(item, `attachments[${index}]`)
    const assetType = expectString(record.assetType, `attachments[${index}].assetType`)
    if (!['image', 'video', 'text'].includes(assetType)) {
      throw new Error(`产品附件类型不支持：attachments[${index}].assetType`)
    }
    return {
      id: record.id === null || record.id === undefined ? null : expectNumber(record.id, `attachments[${index}].id`),
      assetType: assetType as ShowroomProductAttachment['assetType'],
      fileId: expectNumber(record.fileId, `attachments[${index}].fileId`),
      url: expectNullableString(record.url, `attachments[${index}].url`),
      originalName: expectString(record.originalName, `attachments[${index}].originalName`),
      mimeType: expectString(record.mimeType, `attachments[${index}].mimeType`),
      size: expectNumber(record.size, `attachments[${index}].size`),
      displayOrder: expectNumber(record.displayOrder, `attachments[${index}].displayOrder`)
    }
  })
}

export const normalizeProductDetail = (value: unknown): ShowroomProductDetail => {
  const record = expectRecord(value, 'productDetail')
  return {
    productId: expectNumber(record.productId, 'productId'),
    productMasterId: expectNullableNumber(record.productMasterId, 'productMasterId'),
    productCode: expectString(record.productCode, 'productCode'),
    legacyProductCode: expectNullableString(record.legacyProductCode, 'legacyProductCode'),
    currentRevisionId: expectNumber(record.currentRevisionId, 'currentRevisionId'),
    incomplete: expectBoolean(record.incomplete, 'incomplete'),
    live: expectBoolean(record.live, 'live'),
    editable: expectBoolean(record.editable, 'editable'),
    revisionId: expectNumber(record.revisionId, 'revisionId'),
    revisionNo: expectNumber(record.revisionNo, 'revisionNo'),
    status: expectString(record.status, 'status'),
    nameCn: expectString(record.nameCn, 'nameCn'),
    nameEn: expectString(record.nameEn, 'nameEn'),
    fields: expectStringMap(record.fields, 'fields'),
    relatedProductIds: expectNumberArray(record.relatedProductIds, 'relatedProductIds'),
    discussionSummary: expectDiscussionSummary(record.discussionSummary),
    narrations: expectNarrations(record.narrations),
    attachments: expectProductAttachments(record.attachments)
  }
}

export const createProductDraftForm = (detail: ShowroomProductDetail): ProductDraftForm => {
  return {
    productMasterId: detail.productMasterId,
    productCode: detail.productCode,
    legacyProductCode: detail.legacyProductCode || '',
    nameCn: detail.nameCn,
    nameEn: detail.nameEn,
    fields: { ...detail.fields }
  }
}

const normalizedFormValue = (value: string) => value.trim()

export const buildProductDraftPayload = (
  detail: ShowroomProductDetail,
  form: ProductDraftForm
) => {
  const payload = {
    productId: detail.productId,
    productMasterId: form.productMasterId ?? undefined,
    productCode: normalizedFormValue(form.productCode),
    legacyProductCode: normalizedFormValue(form.legacyProductCode),
    nameCn: normalizedFormValue(form.nameCn),
    nameEn: normalizedFormValue(form.nameEn),
    fields: Object.fromEntries(
      productFieldDefinitions.flatMap((item) => {
        const entries: Array<[string, string]> = [
          [item.key, normalizedFormValue(form.fields[item.key] || '')]
        ]
        if (item.translatable) {
          entries.push([
            resolveProductEnglishFieldKey(item.key),
            normalizedFormValue(form.fields[resolveProductEnglishFieldKey(item.key)] || '')
          ])
        }
        return entries
      })
    )
  }
  if (!payload.productCode || !payload.nameCn || !payload.nameEn) {
    throw new Error('产品编码、中文名称、英文名称为必填项')
  }
  return payload
}

export const resolveChangedFieldCodes = (
  detail: ShowroomProductDetail,
  form: ProductDraftForm
) => {
  const changedFieldCodes: string[] = []
  if (normalizedFormValue(form.nameCn) !== detail.nameCn) {
    changedFieldCodes.push('name_cn')
  }
  if (normalizedFormValue(form.nameEn) !== detail.nameEn) {
    changedFieldCodes.push('name_en')
  }
  if (normalizedFormValue(form.legacyProductCode) !== (detail.legacyProductCode || '')) {
    changedFieldCodes.push('legacy_product_code')
  }
  for (const definition of productFieldDefinitions) {
    const previousValue = detail.fields[definition.key] || ''
    const nextValue = normalizedFormValue(form.fields[definition.key] || '')
    if (previousValue !== nextValue) {
      changedFieldCodes.push(definition.key)
    }
    if (definition.translatable) {
      const englishFieldKey = resolveProductEnglishFieldKey(definition.key)
      const previousEnglishValue = detail.fields[englishFieldKey] || ''
      const nextEnglishValue = normalizedFormValue(form.fields[englishFieldKey] || '')
      if (previousEnglishValue !== nextEnglishValue) {
        changedFieldCodes.push(englishFieldKey)
      }
    }
  }
  return changedFieldCodes
}

const expectDiffItems = (value: unknown): ProductVersionDiffItem[] => {
  if (!Array.isArray(value)) {
    throw new Error('产品历史缺少数组字段：diffItems')
  }
  return value.map((item, index) => {
    const record = expectRecord(item, `diffItems[${index}]`)
    return {
      fieldCode: expectString(record.fieldCode, `diffItems[${index}].fieldCode`),
      label: expectString(record.label, `diffItems[${index}].label`),
      oldValue: expectNullableString(record.oldValue, `diffItems[${index}].oldValue`),
      newValue: expectNullableString(record.newValue, `diffItems[${index}].newValue`),
      operatorId: expectNumber(record.operatorId, `diffItems[${index}].operatorId`),
      operatorAction: expectString(record.operatorAction, `diffItems[${index}].operatorAction`),
      createdAt: expectHistoryTimestampString(record.createdAt, `diffItems[${index}].createdAt`)
    }
  })
}

export const normalizeProductHistory = (value: unknown): ProductVersionHistory[] => {
  if (!Array.isArray(value)) {
    throw new Error('产品历史缺少数组字段：history')
  }
  return value.map((item, index) => {
    const record = expectRecord(item, `history[${index}]`)
    return {
      revisionId: expectNumber(record.revisionId, `history[${index}].revisionId`),
      revisionNo: expectNumber(record.revisionNo, `history[${index}].revisionNo`),
      status: expectString(record.status, `history[${index}].status`),
      diffItems: expectDiffItems(record.diffItems)
    }
  })
}

export const resolveProductStatusText = (status: string) => {
  return statusTextMap[status] || status
}

export const resolveProductStatusTagType = (status: string) => {
  if (status === 'PUBLISHED' || status === 'APPROVED') {
    return 'success'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  if (status.startsWith('PENDING')) {
    return 'warning'
  }
  return 'info'
}

export const resolveProductOwnerTypeText = (value: string) => {
  return value === 'SUBSIDIARY' ? '子公司产品' : '盈泰产品'
}

export const resolveLifecycleText = (value: string) => {
  return value === 'R_AND_D' ? '研发中' : '已注册'
}

export const resolveNarrationStatusText = (value: ProductNarrationAvailability) => {
  return `${value.language} / ${value.audienceType} / ${resolveProductStatusText(value.status)}`
}
