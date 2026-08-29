import type { TagProps } from 'element-plus'
import type {
  DccRegistrationCertificateLocalDateValue,
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
  return REGISTRATION_CERTIFICATE_STATUS_META[status as DccRegistrationCertificateStatus]?.label || status
}

export const getRegistrationCertificateStatusTagType = (
  status?: DccRegistrationCertificateStatus | string | null
) => REGISTRATION_CERTIFICATE_STATUS_META[status as DccRegistrationCertificateStatus]?.tagType || 'info'

export const formatMissingMarker = (present?: boolean | null) => (present ? '已提供' : '缺失')

export const getMissingMarkerTagType = (present?: boolean | null): TagProps['type'] =>
  present ? 'success' : 'danger'

export const formatRegistrationCertificateReminder = (state?: string | null) => {
  if (!state || state === 'NONE' || state === 'CLEARED') return '正常'
  return state.replace('_', '-')
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
