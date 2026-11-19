import { formatBusinessDateTime } from '@/utils/businessTime'

// Registration APIs return epoch milliseconds; date-only certificate fields use their own formatter.
export const formatRegistrationCertificateDateTime = (
  value?: number | string | null,
  emptyText = '--',
  invalidText = '时间格式异常'
) => formatBusinessDateTime(value, emptyText, invalidText)

export const registrationCertificateDateTimeFormatter = (
  _row: unknown,
  _column: unknown,
  value?: number | string | null
) => formatRegistrationCertificateDateTime(value, '')
