import type { TagProps } from 'element-plus'
import type {
  DccRegistrationCertificateLocalDateValue,
  DccRegistrationCertificateReminderFilterState,
  DccRegistrationCertificateReminderVisualState,
  DccRegistrationCertificateStatus
} from '@/api/dcc/registrationCertificate'

export interface RegistrationCertificateStatusMeta {
  label: string
  tagType: TagProps['type']
}

export const REGISTRATION_CERTIFICATE_STATUS_META: Record<
  DccRegistrationCertificateStatus,
  RegistrationCertificateStatusMeta
> = {
  DRAFT: { label: '草稿', tagType: 'info' },
  PENDING_EFFECTIVE: { label: '待生效', tagType: 'warning' },
  CURRENT: { label: '当前有效', tagType: 'success' },
  OLD: { label: '旧证', tagType: 'info' },
  VOIDED: { label: '已作废', tagType: 'danger' }
}

export const REGISTRATION_CERTIFICATE_STATUS_OPTIONS = Object.entries(
  REGISTRATION_CERTIFICATE_STATUS_META
).map(([value, meta]) => ({
  value: value as DccRegistrationCertificateStatus,
  label: meta.label
}))

export const formatRegistrationCertificateStatus = (
  status?: DccRegistrationCertificateStatus | string | null
) => {
  if (!status) return '未返回状态'
  return REGISTRATION_CERTIFICATE_STATUS_META[status as DccRegistrationCertificateStatus]?.label ?? '未识别状态'
}

export const getRegistrationCertificateStatusTagType = (
  status?: DccRegistrationCertificateStatus | string | null
) => REGISTRATION_CERTIFICATE_STATUS_META[status as DccRegistrationCertificateStatus]?.tagType || 'info'

export const formatMissingMarker = (present?: boolean | null) => (present ? '已提供' : '缺失')

export const getMissingMarkerTagType = (present?: boolean | null): TagProps['type'] =>
  present ? 'success' : 'danger'

export const REGISTRATION_CERTIFICATE_REMINDER_FILTER_OPTIONS: Array<{
  value: DccRegistrationCertificateReminderFilterState
  label: string
}> = [
  { value: 'NORMAL', label: '正常' },
  { value: 'T_30', label: '到期前 30 个月' },
  { value: 'T_8', label: '到期前 8 个月' },
  { value: 'T_2', label: '到期前 2 个月' },
  { value: 'T_1', label: '到期前 1 个月' }
]

const REGISTRATION_CERTIFICATE_REMINDER_LABELS: Record<string, string> = {
  NORMAL: '正常',
  NONE: '正常',
  CLEARED: '正常',
  T_30: '到期前 30 个月',
  T_8: '到期前 8 个月',
  T_2: '到期前 2 个月',
  T_1: '到期前 1 个月'
}

const REGISTRATION_CERTIFICATE_WORKFLOW_STATUS_LABELS: Record<string, string> = {
  SUBMITTED: '已提交',
  BPM_BOUND: '审批中',
  RUNNING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  ACTIVE: '有效',
  EXPIRED: '已过期',
  REVOKED: '已撤销'
}

export const formatRegistrationCertificateReminder = (
  state?: DccRegistrationCertificateReminderVisualState | string | null
) => {
  if (!state) return '正常'
  return REGISTRATION_CERTIFICATE_REMINDER_LABELS[state] ?? '提醒状态异常'
}

export const formatRegistrationCertificateWorkflowStatus = (status?: string | null) => {
  if (!status) return '未返回状态'
  return REGISTRATION_CERTIFICATE_WORKFLOW_STATUS_LABELS[status] ?? '未识别状态'
}

export const getRegistrationCertificateReminderTagType = (color?: string | null): TagProps['type'] => {
  if (color === 'BRIGHT') return 'danger'
  if (color === 'LIGHT') return 'warning'
  return 'success'
}

export const displayText = (value?: string | number | boolean | null) => {
  if (value === undefined || value === null || value === '') return '—'
  return String(value)
}

const containsChinese = (value: string) => /[\u4e00-\u9fff]/.test(value)

export const resolveRegistrationCertificateUserMessage = (
  error: unknown,
  fallback: string
) => {
  const record = error as {
    message?: string
    msg?: string
    response?: { data?: { msg?: string; message?: string } }
  }
  const candidates = [
    record?.response?.data?.msg,
    record?.response?.data?.message,
    record?.msg,
    record?.message
  ]
  const businessMessage = candidates.find(
    (candidate): candidate is string => typeof candidate === 'string' && containsChinese(candidate.trim())
  )
  return businessMessage?.trim() || fallback
}

const assertValidLocalDate = (year: number, month: number, day: number) => {
  const daysInMonth = [
    31,
    year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0) ? 29 : 28,
    31,
    30,
    31,
    30,
    31,
    31,
    30,
    31,
    30,
    31
  ]
  if (month < 1 || month > 12 || day < 1 || day > daysInMonth[month - 1]) {
    throw new Error('注册证日期字段格式无效。')
  }
}

export const formatRegistrationCertificateDate = (
  value?: DccRegistrationCertificateLocalDateValue | null
) => {
  if (value === undefined || value === null || value === '') return '—'
  if (typeof value === 'string') return value
  if (!Array.isArray(value) || value.length !== 3 || !value.every(Number.isInteger)) {
    throw new Error('注册证日期字段格式无效。')
  }
  const [year, month, day] = value
  assertValidLocalDate(year, month, day)
  return [
    String(year).padStart(4, '0'),
    String(month).padStart(2, '0'),
    String(day).padStart(2, '0')
  ].join('-')
}

interface EntrustedEnterpriseSnapshot {
  enterpriseName?: unknown
}

export const formatEntrustedEnterpriseNames = (value?: string | null) => {
  if (!value || !value.trim() || value.trim() === '[]') return '—'
  const parsed = JSON.parse(value) as unknown
  if (!Array.isArray(parsed)) {
    throw new Error('注册证受托生产企业数据格式无效。')
  }
  const names = parsed.map((item) => {
    if (!item || typeof item !== 'object') {
      throw new Error('注册证受托生产企业缺少企业名称。')
    }
    const enterpriseName = (item as EntrustedEnterpriseSnapshot).enterpriseName
    if (typeof enterpriseName !== 'string' || !enterpriseName.trim()) {
      throw new Error('注册证受托生产企业缺少企业名称。')
    }
    return enterpriseName.trim()
  })
  return names.length ? names.join('、') : '—'
}
