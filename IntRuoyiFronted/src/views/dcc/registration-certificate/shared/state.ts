import type { TagProps } from 'element-plus'
import type { DccRegistrationCertificateStatus } from '@/api/dcc/registrationCertificate'

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

export const displayText = (value?: string | number | boolean | null) => {
  if (value === undefined || value === null || value === '') return '—'
  return String(value)
}
