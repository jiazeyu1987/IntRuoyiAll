import type { DccRegistrationCertificatePageReqVO } from '@/api/dcc/registrationCertificate'

const DATE_RANGES = [
  ['firstObtainedStart', 'firstObtainedEnd', '首次获证日期'],
  ['approvalStart', 'approvalEnd', '批准日期'],
  ['effectiveStart', 'effectiveEnd', '生效日期'],
  ['expiryStart', 'expiryEnd', '有效期至'],
  ['ceIssueStart', 'ceIssueEnd', 'CE签发日期'],
  ['listingStart', 'listingEnd', '列名日期']
] as const

export const getRegistrationCertificateQueryError = (query: DccRegistrationCertificatePageReqVO) => {
  for (const [start, end, label] of DATE_RANGES) {
    const startDate = query[start]
    const endDate = query[end]
    if (startDate && endDate && startDate > endDate) {
      return `${label}：日期起始不能晚于截止，请修改后重新查询`
    }
  }
  return ''
}
