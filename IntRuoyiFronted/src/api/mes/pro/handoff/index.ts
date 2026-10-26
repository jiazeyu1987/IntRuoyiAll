import request from '@/config/axios'

const base = '/mes/pro/active-order-handoff'
export interface HandoffTask {
  id: number | string; activeOrderId: number | string; workOrderId: number | string
  routeProcessId?: number | string
  taskType: string; sourceType: string; sourceId: number | string; roundId: number | string
  completedBy?: number | string; completedAt?: string | number; completionSourceId?: number | string
  responsibilitySnapshotJson?: string
  status: string; actionUrl: string; reason: string; createTime: string
}
export interface HandoffReceipt {
  id: number | string; status: 'PENDING' | 'FAILED' | 'SENT'; rowVersion: number
  attemptCount: number; sentAt?: string; lastErrorSummary?: string; systemMessageId?: number | string
}
export const myHandoffs = (): Promise<HandoffTask[]> => request.get({ url: `${base}/my-list` })
export const handoffReceipts = (taskId: number | string): Promise<HandoffReceipt[]> => request.get({ url: `${base}/receipts`, params: { taskId } })
export const retryHandoff = (data: { id: number | string; rowVersion: number; reason: string }) => request.post({ url: `${base}/retry`, data })
export const handoffNavigationContext = (taskId: number | string): Promise<{ task: HandoffTask; current: boolean; processable: boolean; profileLeaderCorrection: boolean }> => request.get({ url: `${base}/navigation-context`, params: { taskId } })
