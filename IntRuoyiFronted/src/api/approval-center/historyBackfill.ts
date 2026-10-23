import request from '@/config/axios'

export interface HistoryFinding {
  processId: string
  status: 'BLOCKED' | 'INSERT' | 'EXISTING'
  reason: string
  authority: null | {
    authorityType: string
    authorityId: string
    authorityVersion: string
    writerCode: string
    projectionCode: string
    expectedBusinessKey: string
  }
  sourceProof: string | null
  candidates: string[]
}

export interface HistoryReport {
  source: string
  batchId: string
  tenantId: string
  status: 'BLOCKED' | 'READY' | 'APPLIED'
  inspectionDigest: string
  inventoryDigest: string
  runtimeTasks: number
  historicTasks: number
  initiatedInstances: number
  copies: number
  findings: HistoryFinding[]
  migratedAt: string | null
}

export const inspectApprovalHistory = (batchId: string) =>
  request.get<HistoryReport>({
    url: '/approval-center/history-backfill/inspect',
    // Full tenant history inspection exceeded the normal 30-second page-query budget.
    timeout: 120000,
    params: { batchId }
  })

export const executeApprovalHistory = (batchId: string, inspectionDigest: string) =>
  request.post<HistoryReport>({
    url: '/approval-center/history-backfill/execute',
    timeout: 120000,
    data: { batchId, inspectionDigest, confirmation: 'EXECUTE_HISTORY_BACKFILL' }
  })
