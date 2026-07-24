export interface VersionDiffItem {
  fieldCode: string
  label: string
  oldValue: string
  newValue: string
  operatorId: number
  operatorAction: string
  createdAt: string
}

export interface CompanyVersionDiffItem extends VersionDiffItem {}

export interface CompanyVersionHistory {
  revisionId: number
  revisionNo: number
  status: string
  diffItems: VersionDiffItem[]
}

export type VersionBrowserScope = 'COMPANY' | 'PRODUCT' | 'NARRATION' | 'PREVIEW_ASSET'

export type VersionBrowserContentTargetType = 'COMPANY' | 'PRODUCT' | 'HALL'

export interface VersionHistoryRecord {
  revisionId: number
  revisionNo: number
  status: string
  diffItems: VersionDiffItem[]
  targetType: 'COMPANY' | 'PRODUCT'
  targetId: number
  targetLabel: string
}

export interface VersionPreviewSnapshot {
  targetType: VersionBrowserContentTargetType
  targetId: number
  title: string
  description: string
  previewImageUrl: string
}

const companyHistoryStatusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_SUPERVISOR_REVIEW: '主管审核中',
  PENDING_SUPERVISOR_APPROVAL: '主管审核中',
  PENDING_GAOXIN_APPROVAL: '企宣审批中',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  PUBLISHED: '已发布'
}

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`公司历史缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`公司历史缺少数值字段：${fieldName}`)
  }
  return value
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`公司历史缺少字符串字段：${fieldName}`)
  }
  return value
}

const expectNullableString = (value: unknown, fieldName: string) => {
  if (value === null || value === undefined) {
    return ''
  }
  return expectString(value, fieldName, true)
}

const expectStringish = (value: unknown, fieldName: string) => {
  if (typeof value === 'string') {
    return value
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }
  throw new Error(`公司历史缺少字符串字段：${fieldName}`)
}

const expectDiffItems = (value: unknown): VersionDiffItem[] => {
  if (!Array.isArray(value)) {
    throw new Error('公司历史缺少数组字段：diffItems')
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
      createdAt: expectStringish(record.createdAt, `diffItems[${index}].createdAt`)
    }
  })
}

export const normalizeCompanyHistory = (value: unknown): CompanyVersionHistory[] => {
  if (!Array.isArray(value)) {
    throw new Error('公司历史缺少数组字段：history')
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

export const resolveCompanyHistoryStatusText = (status: string) => {
  return companyHistoryStatusTextMap[status] || status
}

export const resolveCompanyHistoryStatusTagType = (status: string) => {
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

export const formatHistoryDiffValue = (value: string) => {
  if (!value) {
    return '空'
  }
  try {
    const parsed = JSON.parse(value)
    if (parsed && typeof parsed === 'object' && 'value' in parsed) {
      return String((parsed as { value: unknown }).value ?? '')
    }
  } catch {
    return value
  }
  return value
}
