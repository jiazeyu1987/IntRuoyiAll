import type { FrontlinePqcProcessVO, FrontlinePqcInspectionSubmitReqVO } from './index'
import { getOwnReturnDetail, listOwnReturns, resubmitOwnPqc, type OwnReturnDetail } from '../processpool/frontlineReturnCorrection'

// Keep the persisted submission and review history; project only the owner's rejected task as editable.
export const restoreReturnedPqcProcesses = async (processes: FrontlinePqcProcessVO[], activeOrderId: number) => {
  const returned = (await listOwnReturns('PQC')).filter(row => row.activeOrderId === activeOrderId)
  await Promise.all(returned.map(async row => {
    const matches = processes.flatMap(process => process.pqcTaskOptions)
      .filter(option => option.submittedEventId === row.eventId)
    if (matches.length !== 1 || matches[0].taskStatus !== 'SUBMITTED') {
      throw new Error('退回检验记录与原PQC任务不一致，请刷新订单。')
    }
    const detail = await getOwnReturnDetail(row, 'PQC')
    if (!detail.pqc || ['eventId', 'activeOrderId', 'rejectedReviewId', 'expectedRevisionId', 'returnTaskId']
      .some(key => detail.row[key] !== row[key])) {
      throw new Error('退回检验记录缺少原检验数据或轮次已变化，请刷新订单。')
    }
    matches[0].returnedSubmission = detail
    matches[0].taskStatus = 'PENDING'
  }))
  for (const process of processes) {
    const statuses = new Set(process.pqcTaskOptions.map(task => task.taskStatus))
    process.taskSummary = {
      ...process.taskSummary,
      state: statuses.size === 1 ? process.pqcTaskOptions[0].taskStatus : statuses.size ? 'MIXED' : 'NOT_CREATED',
      pendingCount: process.pqcTaskOptions.filter(task => task.taskStatus === 'PENDING').length,
      submittedCount: process.pqcTaskOptions.filter(task => task.taskStatus === 'SUBMITTED').length,
      confirmedCount: process.pqcTaskOptions.filter(task => task.taskStatus === 'CONFIRMED').length
    }
  }
}

export const submitReturnedPqcInspection = (returned: OwnReturnDetail, payload: FrontlinePqcInspectionSubmitReqVO,
  options?: Parameters<typeof resubmitOwnPqc>[1]) => resubmitOwnPqc({
  activeOrderId: returned.row.activeOrderId,
  rejectedReviewId: returned.row.rejectedReviewId,
  expectedRevisionId: returned.row.expectedRevisionId,
  correction: {
    eventId: returned.row.eventId,
    actualInspectionQuantity: payload.actualInspectionQuantity,
    scrapQuantity: payload.scrapQuantity,
    itemResults: payload.itemResults,
    changeReason: 'PQC组长退回后重新签名提交',
    signaturePassword: payload.signaturePassword
  }
}, options)
