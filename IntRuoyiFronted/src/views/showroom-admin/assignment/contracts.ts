import {
  productFieldDefinitions,
  SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE,
  SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_LABEL
} from '@/views/showroom-admin/product/contracts'

export interface ShowroomAssignmentRecord {
  assignmentId: number
  targetType: string
  targetId: number
  fieldCode: string
  assigneeUserId: number
  assignedBy: number
  status: string
  notifyMessageId: number | null
  notifyTemplateCode: string | null
  notifyContent: string | null
  currentDraftValue: string | null
  lastSavedRevisionId: number | null
  lastChangeRequestId: number | null
  latestChangeRequestStatus: string | null
}

export interface AssignmentTargetOption {
  label: string
  value: number
  sourceRevisionId: number | null
}

export interface AssignmentUserOption {
  id: number
  nickname: string
  deptId: number
}

const companyFieldDefinitions = [
  { key: 'development_history', label: '发展历程' },
  { key: 'park_introduction', label: '园区介绍' },
  { key: 'incubation_platform', label: '孵化平台' },
  { key: 'subsidiary_overview', label: '子公司概览' },
  { key: 'stock_info', label: '股权信息' },
  { key: 'core_manufacturing_capability', label: '核心制造能力' },
  { key: 'honors_awards', label: '荣誉奖项' }
] as const

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`指派工作台缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`指派工作台缺少字符串字段：${fieldName}`)
  }
  return value
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`指派工作台缺少数值字段：${fieldName}`)
  }
  return value
}

const optionalNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  return expectNumber(value, 'optionalNumber')
}

const getRevisionId = (record: Record<string, unknown>) => {
  const revision = record.revision && typeof record.revision === 'object' ? (record.revision as Record<string, unknown>) : null
  return revision?.revisionId === undefined || revision?.revisionId === null
    ? null
    : expectNumber(revision.revisionId, 'revision.revisionId')
}

export const normalizeAssignmentRecord = (value: unknown): ShowroomAssignmentRecord => {
  const record = expectRecord(value, 'assignment')
  return {
    assignmentId: expectNumber(record.assignmentId, 'assignmentId'),
    targetType: expectString(record.targetType, 'targetType'),
    targetId: expectNumber(record.targetId, 'targetId'),
    fieldCode: expectString(record.fieldCode, 'fieldCode'),
    assigneeUserId: expectNumber(record.assigneeUserId, 'assigneeUserId'),
    assignedBy: expectNumber(record.assignedBy, 'assignedBy'),
    status: expectString(record.status, 'status'),
    notifyMessageId: optionalNumber(record.notifyMessageId),
    notifyTemplateCode: record.notifyTemplateCode === undefined || record.notifyTemplateCode === null
      ? null
      : String(record.notifyTemplateCode),
    notifyContent: record.notifyContent === undefined || record.notifyContent === null
      ? null
      : String(record.notifyContent),
    currentDraftValue: record.currentDraftValue === undefined || record.currentDraftValue === null
      ? null
      : String(record.currentDraftValue),
    lastSavedRevisionId: optionalNumber(record.lastSavedRevisionId),
    lastChangeRequestId: optionalNumber(record.lastChangeRequestId),
    latestChangeRequestStatus: record.latestChangeRequestStatus === undefined || record.latestChangeRequestStatus === null
      ? null
      : String(record.latestChangeRequestStatus)
  }
}

export const normalizeAssignmentPage = (value: unknown): ShowroomAssignmentRecord[] => {
  if (!Array.isArray(value)) {
    throw new Error('指派工作台缺少数组字段：assignmentPage')
  }
  return value.map((item) => normalizeAssignmentRecord(item))
}

const statusTextMap: Record<string, string> = {
  OPEN: '待处理',
  DRAFT: '草稿',
  PENDING: '待提交',
  PENDING_SUPERVISOR_REVIEW: '主管审核中',
  PENDING_SUPERVISOR_APPROVAL: '主管审核中',
  PENDING_GAOXIN_APPROVAL: '企宣审批中',
  COMPLETED: '已完成',
  APPROVED: '已批准',
  REJECTED: '已驳回'
}

export const resolveAssignmentStatusText = (status: string) => statusTextMap[status] || status

export const resolveAssignmentStatusTagType = (status: string) => {
  if (status === 'COMPLETED' || status === 'APPROVED') {
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

export const resolveTargetTypeText = (targetType: string) => {
  if (targetType === 'COMPANY') {
    return '公司'
  }
  if (targetType === 'PRODUCT') {
    return '产品'
  }
  return targetType
}

export const resolveFieldLabel = (targetType: string, fieldCode: string) => {
  if (targetType === 'COMPANY') {
    const found = companyFieldDefinitions.find((item) => item.key === fieldCode)
    return found?.label || fieldCode
  }
  if (fieldCode === SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE) {
    return SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_LABEL
  }
  const found = productFieldDefinitions.find((item) => item.key === fieldCode)
  return found?.label || fieldCode
}

export const buildTargetOptions = (
  companyCurrent: Record<string, unknown> | null,
  products: unknown[]
): Record<'COMPANY' | 'PRODUCT', AssignmentTargetOption[]> => {
  const productOptions = Array.isArray(products)
    ? products
        .map((item) => {
          const record = expectRecord(item, 'products')
          const revision = record.revision && typeof record.revision === 'object'
            ? (record.revision as Record<string, unknown>)
            : {}
          return {
            label: `${expectString(record.productCode, 'productCode')} · ${
              revision.nameCn ? String(revision.nameCn) : '未命名'
            }`,
            value: expectNumber(record.productId, 'productId'),
            sourceRevisionId: getRevisionId(record)
          }
        })
    : []
  const companyOptions = companyCurrent
    ? [
        {
          label: `${companyCurrent.displayName ? String(companyCurrent.displayName) : '公司'} · ${
            companyCurrent.revisionNo ?? '未发布'
          }`,
          value: expectNumber(companyCurrent.companyId, 'companyId'),
          sourceRevisionId:
            companyCurrent.revisionId === undefined || companyCurrent.revisionId === null
              ? null
              : expectNumber(companyCurrent.revisionId, 'companyCurrent.revisionId')
        }
      ]
    : []
  return {
    COMPANY: companyOptions,
    PRODUCT: productOptions
  }
}

export const buildFieldOptions = (targetType: string) => {
  if (targetType === 'COMPANY') {
    return companyFieldDefinitions.map((item) => ({ label: item.label, value: item.key }))
  }
  return productFieldDefinitions.map((item) => ({ label: item.label, value: item.key }))
}
