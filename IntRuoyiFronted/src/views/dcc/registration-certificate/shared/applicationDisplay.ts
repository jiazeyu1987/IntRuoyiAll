import { formatRegistrationCertificateDateTime } from './dateTimeDisplay'

const CERTIFICATE_TYPE_LABELS: Record<string, string> = {
  DOMESTIC: '国内',
  CE: 'CE',
  FDA: 'FDA'
}

// These are request-file review states, separate from request and certificate states.
const FILE_REVIEW_LABELS: Record<string, string> = {
  REQUESTED: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const formatApplicationCertificateType = (value?: string | null) =>
  value ? CERTIFICATE_TYPE_LABELS[value] ?? '未识别证件类型' : '未返回证件类型'

export const formatApplicationFileReview = (value?: string | null) =>
  value ? FILE_REVIEW_LABELS[value] ?? '文件审批状态异常' : '未返回文件审批状态'

export const formatApplicationSubmittedAt = (value?: number | string | null) =>
  formatRegistrationCertificateDateTime(value, '未返回提交时间', '提交时间格式异常')
