export interface ShowroomCompanyCurrent {
  companyId: number
  revisionId: number | null
  revisionNo: number
  status: string
  fields: Record<string, string>
  companyType: string
  displayName: string
  displayNameEn: string
  coverImage: string
  live: boolean
}

export interface CompanyDraftForm {
  companyType: string
  displayName: string
  displayNameEn: string
  coverImage: string
  fields: Record<string, string>
}

export type CompanyFieldDefinition = {
  key: string
  label: string
  labelEn: string
}

export const companyFieldDefinitions = [
  { key: 'development_history', label: '发展历程', labelEn: 'Development History' },
  { key: 'park_introduction', label: '园区介绍', labelEn: 'Park Introduction' },
  { key: 'incubation_platform', label: '孵化平台', labelEn: 'Incubation Platform' },
  { key: 'subsidiary_overview', label: '子公司概览', labelEn: 'Subsidiary Overview' },
  { key: 'stock_info', label: '上市信息', labelEn: 'Listing Information' },
  {
    key: 'core_manufacturing_capability',
    label: '核心制造能力',
    labelEn: 'Core Manufacturing Capability'
  },
  { key: 'honors_awards', label: '荣誉资质', labelEn: 'Honors and Awards' }
] as const satisfies readonly CompanyFieldDefinition[]

export const resolveCompanyEnglishFieldKey = (fieldKey: string) => `${fieldKey}_en`

const companyStatusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_SUPERVISOR_REVIEW: '主管审核中',
  PENDING_SUPERVISOR_APPROVAL: '主管审核中',
  PENDING_GAOXIN_APPROVAL: '企宣审批中',
  PENDING: '审批中',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  PUBLISHED: '已发布'
}

const expectedCompanyFieldKeys = companyFieldDefinitions.flatMap((item) => [
  item.key,
  resolveCompanyEnglishFieldKey(item.key)
])

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`公司工作台缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`公司工作台缺少数值字段：${fieldName}`)
  }
  return value
}

const expectNullableNumber = (value: unknown, fieldName: string) => {
  if (value === null) {
    return null
  }
  return expectNumber(value, fieldName)
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`公司工作台缺少字符串字段：${fieldName}`)
  }
  return value
}

const expectBoolean = (value: unknown, fieldName: string) => {
  if (typeof value !== 'boolean') {
    throw new Error(`公司工作台缺少布尔字段：${fieldName}`)
  }
  return value
}

const resolveOptionalString = (value: unknown) =>
  value === undefined || value === null ? '' : String(value)

const expectStringMap = (record: Record<string, unknown>) => {
  const normalized: Record<string, string> = {}
  for (const key of expectedCompanyFieldKeys) {
    normalized[key] = resolveOptionalString(record[key])
  }
  return normalized
}

export const normalizeCompanyCurrent = (value: unknown): ShowroomCompanyCurrent => {
  const record = expectRecord(value, 'companyCurrent')
  const fieldsRecord = expectRecord(record.fields, 'fields')
  return {
    companyId: expectNumber(record.companyId, 'companyId'),
    revisionId: expectNullableNumber(record.revisionId, 'revisionId'),
    revisionNo: expectNumber(record.revisionNo, 'revisionNo'),
    status: expectString(record.status, 'status'),
    fields: expectStringMap(fieldsRecord),
    companyType: expectString(record.companyType, 'companyType', true),
    displayName: expectString(record.displayName, 'displayName', true),
    displayNameEn: expectString(record.displayNameEn, 'displayNameEn', true),
    coverImage: resolveOptionalString(fieldsRecord.cover_image),
    live: expectBoolean(record.live, 'live')
  }
}

export const createCompanyDraftForm = (current: ShowroomCompanyCurrent): CompanyDraftForm => {
  return {
    companyType: current.companyType,
    displayName: current.displayName,
    displayNameEn: current.displayNameEn,
    coverImage: current.coverImage,
    fields: { ...current.fields }
  }
}

const normalizeFormValue = (value: string) => value.trim()

export const normalizeCompanyFieldValue = (value: string | undefined) => normalizeFormValue(value || '')

export const buildCompanyDraftPayload = (current: ShowroomCompanyCurrent, form: CompanyDraftForm) => {
  return {
    companyId: current.companyId > 0 ? current.companyId : null,
    companyType: normalizeCompanyFieldValue(form.companyType),
    displayName: normalizeCompanyFieldValue(form.displayName),
    displayNameEn: normalizeCompanyFieldValue(form.displayNameEn),
    fields: Object.fromEntries(
      [
        ...companyFieldDefinitions.flatMap((item) => [
          [item.key, normalizeCompanyFieldValue(form.fields[item.key])],
          [
            resolveCompanyEnglishFieldKey(item.key),
            normalizeCompanyFieldValue(form.fields[resolveCompanyEnglishFieldKey(item.key)])
          ]
        ]),
        ['cover_image', normalizeCompanyFieldValue(form.coverImage)]
      ]
    )
  }
}

export const resolveCompanyChangedFieldCodes = (
  current: ShowroomCompanyCurrent,
  form: CompanyDraftForm
) => {
  return companyFieldDefinitions.flatMap((item) => {
    const changedCodes: string[] = []
    if (
      normalizeCompanyFieldValue(form.fields[item.key]) !==
      normalizeCompanyFieldValue(current.fields[item.key])
    ) {
      changedCodes.push(item.key)
    }
    const englishFieldKey = resolveCompanyEnglishFieldKey(item.key)
    if (
      normalizeCompanyFieldValue(form.fields[englishFieldKey]) !==
      normalizeCompanyFieldValue(current.fields[englishFieldKey])
    ) {
      changedCodes.push(englishFieldKey)
    }
    return changedCodes
  })
}

export const hasCompanyDraftChanges = (current: ShowroomCompanyCurrent, form: CompanyDraftForm) => {
  if (normalizeCompanyFieldValue(form.displayName) !== normalizeCompanyFieldValue(current.displayName)) {
    return true
  }
  if (normalizeCompanyFieldValue(form.displayNameEn) !== normalizeCompanyFieldValue(current.displayNameEn)) {
    return true
  }
  if (normalizeCompanyFieldValue(form.coverImage) !== normalizeCompanyFieldValue(current.coverImage)) {
    return true
  }
  return companyFieldDefinitions.some(
    (item) =>
      normalizeCompanyFieldValue(form.fields[item.key]) !==
        normalizeCompanyFieldValue(current.fields[item.key]) ||
      normalizeCompanyFieldValue(form.fields[resolveCompanyEnglishFieldKey(item.key)]) !==
        normalizeCompanyFieldValue(current.fields[resolveCompanyEnglishFieldKey(item.key)])
  )
}

export const resolveCompanyStatusText = (status: string) => companyStatusTextMap[status] || status

export const resolveCompanyStatusTagType = (status: string) => {
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
