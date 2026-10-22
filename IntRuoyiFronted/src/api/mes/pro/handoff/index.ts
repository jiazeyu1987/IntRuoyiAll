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
export interface AssignmentOption { id: number; label: string }
export interface QaAssignmentRule {
  id: number; candidateSourceType: 'USER' | 'ROLE_GROUP'; candidateSourceId: number
  candidateLabel: string; enabled: boolean; remark: string
}
export const myHandoffs = (): Promise<HandoffTask[]> => request.get({ url: `${base}/my-list` })
export const handoffReceipts = (taskId: number | string): Promise<HandoffReceipt[]> => request.get({ url: `${base}/receipts`, params: { taskId } })
export const retryHandoff = (data: { id: number | string; rowVersion: number; reason: string }) => request.post({ url: `${base}/retry`, data })
export const qaRouteOptions = (): Promise<AssignmentOption[]> => request.get({ url: `${base}/qa-route-options` })
export const qaUserOptions = (keyword: string): Promise<AssignmentOption[]> => request.get({ url: `${base}/qa-user-options`, params: { keyword } })
export const qaRoleOptions = (): Promise<AssignmentOption[]> => request.get({ url: `${base}/qa-role-options` })
export const qaAssignment = (routeId: number): Promise<QaAssignmentRule | null> => request.get({ url: `${base}/qa-assignment`, params: { routeId } })
export const saveQaAssignment = (data: { routeId: number; candidateSourceType: string; candidateSourceId: number; enabled: boolean; reason: string; expectedRuleId: number | null }): Promise<QaAssignmentRule> => request.post({ url: `${base}/qa-assignment`, data })

export interface PqcAssignmentRule extends QaAssignmentRule {
  readonly candidateUserSnapshot: string
  readonly handlerLeaderUserIds: number[]
}
export const pqcRouteOptions = (): Promise<AssignmentOption[]> => request.get({ url: `${base}/pqc-route-options` })
export const pqcUserOptions = (keyword: string): Promise<AssignmentOption[]> => request.get({ url: `${base}/pqc-user-options`, params: { keyword } })
export const pqcRoleOptions = (): Promise<AssignmentOption[]> => request.get({ url: `${base}/pqc-role-options` })
export const pqcAssignment = (routeId: number): Promise<PqcAssignmentRule | null> => request.get({ url: `${base}/pqc-assignment`, params: { routeId } })
export const savePqcAssignment = (data: { routeId: number; candidateSourceType: 'USER' | 'ROLE_GROUP'; candidateSourceId: number; enabled: boolean; reason: string; expectedRuleId: number | null }): Promise<PqcAssignmentRule> => request.post({ url: `${base}/pqc-assignment`, data })

export const handoffNavigationContext = (taskId: number | string): Promise<{ task: HandoffTask; current: boolean; processable: boolean; profileLeaderCorrection: boolean }> => request.get({ url: `${base}/navigation-context`, params: { taskId } })
