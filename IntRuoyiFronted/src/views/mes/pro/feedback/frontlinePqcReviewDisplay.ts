import type { FrontlinePqcTaskOptionVO } from '@/api/mes/pro/feedback'
export const formatPqcReviewTime = (time: string | number) => {
  const date = new Date(time)
  if (!Number.isFinite(date.getTime())) throw new Error(`PQC复核时间无效：${time}`)
  return date.toLocaleString('zh-CN', { hour12: false })
}

export const formatPqcReviewStatus = (status: 'APPROVED' | 'REJECTED'): string => {
  switch (status) {
    case 'APPROVED': return '复核通过'
    case 'REJECTED': return '退回修改'
    default: throw new Error(`未知PQC复核状态：${status}`)
  }
}

// A formal submission and an approved review are separate business facts.
export const getPqcReviewLabel = (task?: FrontlinePqcTaskOptionVO): string => {
  if (!task) return '未选择检验任务'
  if (task.returnedSubmission) return '退回待修改'
  switch (task.taskStatus) {
    case 'PENDING': return '待检验提交'
    case 'SUBMITTED':
    case 'CONFIRMED':
      switch (task.currentReviewStatus) {
        case 'PENDING': return '待复核'
        case 'APPROVED': return '复核通过'
        case 'REJECTED': return '退回待修改'
        case undefined: return '复核状态缺失，请刷新订单'
        default: throw new Error(`未知PQC当前复核状态：${task.currentReviewStatus}`)
      }
    case 'CANCELLED': return '已撤销'
    default: throw new Error(`未知PQC任务状态：${task.taskStatus}`)
  }
}

export const getPqcReviewCounts = (tasks: FrontlinePqcTaskOptionVO[]) => ({
  pending: tasks.filter(task => ['SUBMITTED', 'CONFIRMED'].includes(task.taskStatus) && task.currentReviewStatus === 'PENDING' && !task.returnedSubmission).length,
  confirmed: tasks.filter(task => task.taskStatus !== 'CANCELLED' && task.currentReviewStatus === 'APPROVED' && !task.returnedSubmission).length,
  returned: tasks.filter(task => task.taskStatus !== 'CANCELLED' && (task.currentReviewStatus === 'REJECTED' || Boolean(task.returnedSubmission))).length,
  unavailable: tasks.filter(task => ['SUBMITTED', 'CONFIRMED'].includes(task.taskStatus) && !task.currentReviewStatus && !task.returnedSubmission).length
})
