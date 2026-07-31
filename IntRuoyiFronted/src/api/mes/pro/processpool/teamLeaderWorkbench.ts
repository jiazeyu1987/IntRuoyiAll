import request from '@/config/axios'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO,
  ProcessPoolTimelinePageReqVO
} from './index'

export interface ProcessPoolTeamLeaderWorkbenchSummaryVO {
  visibleEventCount: number
  pqcSuccessCount: number
  pqcFailureCount: number
  fifoPendingCount: number
  fifoAllocatedCount: number
  auditCopyPendingCount: number
  auditCopySubmittedCount: number
  modifiedRecordCount: number
}

export interface ProcessPoolTeamLeaderWorkbenchVO {
  total: number
  events: ProcessPoolTimelineEventVO[]
  summary: ProcessPoolTeamLeaderWorkbenchSummaryVO
}

export const getProcessPoolTeamLeaderWorkbenchPage = async (
  params: ProcessPoolTimelinePageReqVO
) => {
  return await request.get<ProcessPoolTeamLeaderWorkbenchVO>({
    url: '/mes/pro/process-pool/team-leader-workbench/page',
    params
  })
}

export const getProcessPoolTeamLeaderWorkbenchDetail = async (id: number) => {
  return await request.get<ProcessPoolTimelineDetailVO>({
    url: '/mes/pro/process-pool/team-leader-workbench/detail',
    params: { id }
  })
}
