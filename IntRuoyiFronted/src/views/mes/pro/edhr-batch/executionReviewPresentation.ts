import type { EdhrBatchExecutionReviewExecutionRespVO } from '@/api/mes/pro/edhr/batchExecution'

export const resolveExecutionStatusText = (status?: number | null) => {
  if (status === 4) return '填写完成'
  if (status === 3) return '已批准'
  if (status === 2) return '已提交'
  if (status === 1) return '填写中'
  if (status === 0) return '草稿'
  return status == null ? '未知' : `状态 ${status}`
}

export const resolveExecutionStatusTagType = (status?: number | null) => {
  if (status === 4) return 'success'
  if (status === 3) return 'success'
  if (status === 2) return 'warning'
  if (status === 1) return 'primary'
  if (status === 0) return 'info'
  return 'danger'
}

export const resolveExecutionSummaryItems = (
  execution: EdhrBatchExecutionReviewExecutionRespVO
) => [
  {
    label: '字段审计',
    value: `${execution.fieldAuditSummary?.batchCount || 0} 次`,
    type: 'success'
  },
  {
    label: '字段变更',
    value: `${execution.signatureSummary?.fieldChangeCount || 0} 次`,
    type: 'success'
  },
  {
    label: '历史表单复核',
    value: `${execution.signatureSummary?.formReviewCount || 0} 次`,
    type: 'success'
  },
  {
    label: '提交签名',
    value: `${execution.signatureSummary?.submitCount || 0} 次`,
    type: 'success'
  },
  {
    label: '放行批准签名',
    value: `${execution.signatureSummary?.approveCount || 0} 次`,
    type: 'success'
  },
  {
    label: '追溯状态',
    value: execution.domainTraceSummary?.status || '--',
    type: 'info'
  }
]
