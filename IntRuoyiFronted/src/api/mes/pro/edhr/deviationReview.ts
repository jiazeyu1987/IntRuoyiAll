import request from '@/config/axios'

export type DeviationReviewNode = 'PREPARER' | 'VERIFIER' | 'DEPARTMENT_OWNER' | 'QA' | 'QUALITY_OWNER' | 'MANAGEMENT_REP'

export interface DeviationReviewerOption {
  id: number | string
  username: string
  nickname: string
}

export const getDeviationReviewerCandidates = (node: string) =>
  request.get<DeviationReviewerOption[]>({
    url: '/mes/pro/edhr-deviation-review/reviewer-candidates', params: { node }, ignoreErrorMessage: true
  })

export interface DeviationReviewReturnRequest {
  deviationId: number
  expectedContentVersion: number
  expectedContentHash: string
  node: string
  reason: string
}

export const returnDeviationToInvestigation = (data: DeviationReviewReturnRequest) =>
  request.post({ url: '/mes/pro/edhr-deviation-review/return', data, ignoreErrorMessage: true })
