import type { NotifyMessageVO } from '@/api/system/notify/message'
import type { ReleaseOutcomeRecovery } from '@/api/mes/pro/productionRelease/notification'

const outcomeLabels = {
  PQC_APPROVED: 'PQC生产放行通过',
  PQC_REJECTED: 'PQC生产放行驳回',
  MARKET_APPROVED: '上市放行通过',
  MARKET_REJECTED: '上市放行驳回',
  MARKET_WITHDRAWN: '上市放行撤回'
}

export type InboxMessage = NotifyMessageVO & { kind: 'message'; rowKey: string }
export type InboxRecovery = ReleaseOutcomeRecovery & {
  kind: 'recovery'
  rowKey: string
  templateNickname: string
  templateContent: string
  readStatus: undefined
  createTime: undefined
}
export type InboxRow = InboxMessage | InboxRecovery

export const toInboxMessages = (messages: NotifyMessageVO[]): InboxMessage[] =>
  messages.map((row) => ({ ...row, kind: 'message', rowKey: `message:${row.id}` }))

export const toInboxRecoveries = (rows: ReleaseOutcomeRecovery[]): InboxRecovery[] => {
  if (
    !Array.isArray(rows) ||
    rows.some(
      (row) =>
        !row ||
        !/^[1-9][0-9]*$/.test(row.id) ||
        !/^[1-9][0-9]*$/.test(row.workTaskId) ||
        !row.workOrderCode ||
        !row.batchCode ||
        !Object.hasOwn(outcomeLabels, row.eventType)
    )
  ) {
    throw new Error('放行结果通知身份或内容无效，请核对原通知记录')
  }
  return rows.map((row) => ({
    ...row,
    kind: 'recovery',
    rowKey: `recovery:${row.id}`,
    templateNickname: '放行结果通知',
    templateContent: `${outcomeLabels[row.eventType]}；原生产订单：${row.workOrderCode}；原批号：${row.batchCode}`,
    readStatus: undefined,
    createTime: undefined
  }))
}

// 待补发记录优先显示，已送达消息继续使用服务端分页，避免一次读取全部站内信。
export const loadInboxPage = async (
  pending: InboxRecovery[],
  query: { pageNo: number; pageSize: number; readStatus?: boolean | string },
  loadMessages: (params: typeof query) => Promise<{ list: NotifyMessageVO[]; total: number }>
): Promise<{ list: InboxRow[]; total: number }> => {
  if (query.readStatus === 'pending') {
    return {
      list: pending.slice((query.pageNo - 1) * query.pageSize, query.pageNo * query.pageSize),
      total: pending.length
    }
  }
  const recoveries = query.readStatus === undefined ? pending : []
  const start = (query.pageNo - 1) * query.pageSize
  const prefix = recoveries.slice(start, start + query.pageSize)
  const messageStart = Math.max(0, start - recoveries.length)
  const firstPage = Math.floor(messageStart / query.pageSize) + 1
  const offset = messageStart % query.pageSize
  const data = await loadMessages({ ...query, pageNo: firstPage })
  const needed = query.pageSize - prefix.length
  const messages = data.list.slice(offset, offset + needed)
  if (messages.length < needed && firstPage * query.pageSize < data.total) {
    const next = await loadMessages({ ...query, pageNo: firstPage + 1 })
    messages.push(...next.list.slice(0, needed - messages.length))
  }
  return { list: [...prefix, ...toInboxMessages(messages)], total: recoveries.length + data.total }
}
