export interface ShowroomCompanyCurrentRecord {
  companyId: number
  revisionId: number | null
  revisionNo: number
  status: string
  fields: Record<string, string>
  companyType: string | null
  displayName: string | null
  live: boolean
}

export interface ShowroomNarrationVersionRecord {
  id: number
  key: {
    targetType: 'COMPANY' | 'PRODUCT' | 'HALL' | 'AWARD'
    targetId: number
    audienceType: 'PUBLIC'
    language: 'ZH' | 'EN'
  }
  sourceRevisionId: number
  versionNo: number
  scriptText: string
  audioFileId: number | null
  audioUrl: string | null
  audioDurationSeconds: number | null
  voice: string | null
  generationStatus: string
  status: string
  generatedByAi: boolean
  generatedAt: string | null
  publishedAt: string | null
  live: boolean
}

export interface NarrationTargetOption {
  label: string
  value: number
  sourceRevisionId: number | null
}

export interface PreviewAssetState {
  title: string
  description: string
  previewImageUrl: string
}

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`讲解工作台缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`讲解工作台缺少数值字段：${fieldName}`)
  }
  return value
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`讲解工作台缺少字符串字段：${fieldName}`)
  }
  return value
}

const optionalNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  return expectNumber(value, 'optionalNumber')
}

const optionalString = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  return String(value)
}

const expectStringMap = (value: unknown, fieldName: string) => {
  const record = expectRecord(value, fieldName)
  return Object.fromEntries(
    Object.entries(record).map(([key, entry]) => [key, entry === undefined || entry === null ? '' : String(entry)])
  )
}

export const normalizeCompanyCurrent = (value: unknown): ShowroomCompanyCurrentRecord => {
  const record = expectRecord(value, 'companyCurrent')
  return {
    companyId: expectNumber(record.companyId, 'companyId'),
    revisionId: optionalNumber(record.revisionId),
    revisionNo: typeof record.revisionNo === 'number' ? record.revisionNo : 0,
    status: typeof record.status === 'string' ? record.status : 'DRAFT',
    fields: expectStringMap(record.fields ?? {}, 'fields'),
    companyType: optionalString(record.companyType),
    displayName: optionalString(record.displayName),
    live: typeof record.live === 'boolean' ? record.live : false
  }
}

const getProductSourceRevisionId = (record: Record<string, unknown>) => {
  const revision = record.revision && typeof record.revision === 'object'
    ? (record.revision as Record<string, unknown>)
    : null
  if (revision?.revisionId !== undefined && revision.revisionId !== null) {
    return expectNumber(revision.revisionId, 'revision.revisionId')
  }
  if (record.currentRevisionId !== undefined && record.currentRevisionId !== null) {
    return expectNumber(record.currentRevisionId, 'currentRevisionId')
  }
  return null
}

export const buildTargetOptions = (
  companyCurrent: Record<string, unknown> | null,
  halls: unknown[],
  products: unknown[]
): Record<'COMPANY' | 'HALL' | 'PRODUCT', NarrationTargetOption[]> => {
  const companyOptions = companyCurrent
    ? [
        {
          value: expectNumber(companyCurrent.companyId, 'companyId'),
          label: `${companyCurrent.displayName ? String(companyCurrent.displayName) : '公司'} · ${
            companyCurrent.revisionNo ?? '未发布'
          }`,
          sourceRevisionId:
            companyCurrent.revisionId === undefined || companyCurrent.revisionId === null
              ? null
              : expectNumber(companyCurrent.revisionId, 'revisionId')
        }
      ]
    : []
  const hallOptions = Array.isArray(halls)
    ? halls.map((item) => {
        const record = expectRecord(item, 'halls')
        return {
          value: expectNumber(record.hallId, 'hallId'),
          label: `${expectString(record.hallCode, 'hallCode')} · ${expectString(record.name, 'name')}`,
          sourceRevisionId: null
        }
      })
    : []
  const productOptions = Array.isArray(products)
    ? products.map((item) => {
        const record = expectRecord(item, 'products')
        const revision =
          record.revision && typeof record.revision === 'object'
            ? (record.revision as Record<string, unknown>)
            : {}
        return {
          value: expectNumber(record.productId, 'productId'),
          label: `${expectString(record.productCode, 'productCode')} · ${
            revision.nameCn ? String(revision.nameCn) : '未命名'
          }`,
          sourceRevisionId: getProductSourceRevisionId(record)
        }
      })
    : []
  return {
    COMPANY: companyOptions,
    HALL: hallOptions,
    PRODUCT: productOptions
  }
}

export const normalizeNarrationVersion = (value: unknown): ShowroomNarrationVersionRecord => {
  const record = expectRecord(value, 'narrationVersion')
  const key = expectRecord(record.key, 'key')
  return {
    id: expectNumber(record.id, 'id'),
    key: {
      targetType: expectString(key.targetType, 'key.targetType') as ShowroomNarrationVersionRecord['key']['targetType'],
      targetId: expectNumber(key.targetId, 'key.targetId'),
      audienceType: expectString(key.audienceType, 'key.audienceType') as 'PUBLIC',
      language: expectString(key.language, 'key.language') as 'ZH' | 'EN'
    },
    sourceRevisionId: expectNumber(record.sourceRevisionId, 'sourceRevisionId'),
    versionNo: expectNumber(record.versionNo, 'versionNo'),
    scriptText: expectString(record.scriptText, 'scriptText', true),
    audioFileId: optionalNumber(record.audioFileId),
    audioUrl: optionalString(record.audioUrl),
    audioDurationSeconds: optionalNumber(record.audioDurationSeconds),
    voice: optionalString(record.voice),
    generationStatus: expectString(record.generationStatus, 'generationStatus'),
    status: expectString(record.status, 'status'),
    generatedByAi: typeof record.generatedByAi === 'boolean' ? record.generatedByAi : false,
    generatedAt: optionalString(record.generatedAt),
    publishedAt: optionalString(record.publishedAt),
    live: typeof record.live === 'boolean' ? record.live : false
  }
}

const statusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_SUPERVISOR_REVIEW: '主管审核中',
  PENDING_GAOXIN_APPROVAL: '企宣审批中',
  APPROVED: '已批准待发布',
  PUBLISHED: '已发布',
  REJECTED: '已驳回'
}

const generationTextMap: Record<string, string> = {
  NOT_GENERATED: '未生成',
  SCRIPT_GENERATED: '已生成讲解稿',
  AUDIO_GENERATED: '已生成音频',
  FAILED: '生成失败'
}

export const resolveNarrationStatusText = (status: string) => statusTextMap[status] || status

export const resolveNarrationStatusTagType = (status: string) => {
  if (status === 'PUBLISHED') {
    return 'success'
  }
  if (status === 'APPROVED') {
    return 'warning'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  if (status.startsWith('PENDING')) {
    return 'warning'
  }
  return 'info'
}

export const resolveGenerationStatusText = (status: string) => generationTextMap[status] || status

export const resolveGenerationStatusTagType = (status: string) => {
  if (status === 'AUDIO_GENERATED') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  if (status === 'SCRIPT_GENERATED') {
    return 'warning'
  }
  return 'info'
}

export const resolveTargetTypeText = (targetType: string) => {
  if (targetType === 'COMPANY') {
    return '公司'
  }
  if (targetType === 'HALL') {
    return '展柜'
  }
  if (targetType === 'PRODUCT') {
    return '产品'
  }
  return targetType
}

export const resolveNarrationAudioUrl = (version: ShowroomNarrationVersionRecord | null) => {
  if (!version?.audioFileId) {
    return ''
  }
  if (!version.audioUrl) {
    throw new Error(
      `讲解音频缺少真实 audioUrl：targetType=${version.key.targetType}, targetId=${version.key.targetId}, language=${version.key.language}, fileId=${version.audioFileId}`
    )
  }
  return version.audioUrl
}
