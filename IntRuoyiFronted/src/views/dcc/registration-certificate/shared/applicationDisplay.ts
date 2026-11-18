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

const businessTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23'
})

export const formatApplicationSubmittedAt = (value?: number | string | null) => {
  if (value === undefined || value === null || value === '') return '未返回提交时间'
  if (typeof value === 'string' && !/^\d+$/.test(value)) return '提交时间格式异常'
  const date = new Date(Number(value))
  if (Number.isNaN(date.getTime())) return '提交时间格式异常'
  const parts = Object.fromEntries(businessTimeFormatter.formatToParts(date).map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
}
