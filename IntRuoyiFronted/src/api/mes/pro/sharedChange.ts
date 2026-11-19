import request from '@/config/axios'

export type SharedSubject = 'PROCESS' | 'CRAFT' | 'DEVICE'
export type SharedAction = 'UPDATE' | 'REPLACE'
export interface SharedChangeRequest {
  subjectType: SharedSubject
  action: SharedAction
  sourceId: number
  targetId?: number
  expectedFingerprint: string
  requestId: string
  reason: string
  impactToken?: string
  process?: Record<string, unknown>
  processContents?: import('@/api/mes/pro/process/content').ProProcessContentVO[]
  device?: Record<string, unknown>
  craft?: Record<string, unknown>
  nodes: Array<{ routeId: number; routeProcessId: number; deviceCraftId?: number; withoutParameters?: boolean; crafts: Array<{ craftId: number; deviceIds: number[] }> }>
}
export interface SharedImpact {
  before: Record<string, unknown>
  after: Record<string, unknown>
  routes: Array<{ routeId: number; routeName: string; routeCode: string; versionNo: string; nextVersionNo: string; nodes: Array<{ routeProcessId: number; processId: number; processName: string; workshopName?: string; affectedDeviceIds?: number[]; productionConfig?: any }> }>
  blockers: string[]
  pendingSelectionCount: number
  impactToken: string
}
export const SharedChangeApi = {
  options: (subjectType: SharedSubject, sourceId: number) => request.get({ url: `/mes/pro/shared-change/${subjectType}/options`, params: { sourceId } }),
  crafts: (subjectType: SharedSubject, processId: number) => request.get({ url: `/mes/pro/shared-change/${subjectType}/crafts`, params: { processId } }),
  source: (subjectType: SharedSubject, id: number | string) => request.get({ url: `/mes/pro/shared-change/${subjectType}/source`, params: { id } }),
  preview: (subjectType: SharedSubject, data: SharedChangeRequest): Promise<SharedImpact> => request.post({ url: `/mes/pro/shared-change/${subjectType}/preview`, data }),
  submit: (subjectType: SharedSubject, data: SharedChangeRequest) => request.post({ url: `/mes/pro/shared-change/${subjectType}/submit`, data })
}
