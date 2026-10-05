import request from '@/config/axios'
import type {
  ProcessPoolProductionReportCorrectionReqVO,
  ProcessPoolPqcInspectionCorrectionReqVO
} from './eventRevision'

export type ReturnLeaderType = 'PRODUCTION' | 'PQC'
export interface OwnReturnRow {
  returnTaskId: string
  eventId: number
  activeOrderId: number
  rejectedReviewId: number
  expectedRevisionId: number
  workOrderCode: string
  formName: string
  rejectionReason: string
  rejectedAt: string
}
export interface OwnReturnPqcItem {
  itemCode: string
  itemName: string
  resultType: string
  standardText: string
  lowerLimit?: number
  upperLimit?: number
  unit?: string
  precision?: number
  selectedEquipmentId?: number
  selectedEquipmentNumber?: string
  sampleValues: string[]
}
export interface OwnReturnDetail {
  row: OwnReturnRow
  production?: Pick<
    ProcessPoolProductionReportCorrectionReqVO,
    'outputQuantity' | 'materialDetails' | 'lossDetails' | 'deviceParameterReadings'
  >
  pqc?: { actualInspectionQuantity: number; scrapQuantity: number; items: OwnReturnPqcItem[] }
}
export interface OwnReturnCorrectionResult {
  eventId: number
  revisionId: number
  changes: { fieldName: string; beforeValue: string; afterValue: string }[]
}
export const listOwnReturns = (leaderType: ReturnLeaderType) =>
  request.get<OwnReturnRow[]>({
    url: '/mes/pro/frontline-return-correction/list',
    params: { leaderType }
  })
export const getOwnReturnDetail = (row: OwnReturnRow, leaderType: ReturnLeaderType) =>
  request.get<OwnReturnDetail>({
    url: '/mes/pro/frontline-return-correction/detail',
    params: {
      eventId: row.eventId,
      activeOrderId: row.activeOrderId,
      rejectedReviewId: row.rejectedReviewId,
      expectedRevisionId: row.expectedRevisionId,
      leaderType
    }
  })
type OwnReturnContext = Pick<
  OwnReturnRow,
  'activeOrderId' | 'rejectedReviewId' | 'expectedRevisionId'
>
export const resubmitOwnProduction = (
  data: OwnReturnContext & { correction: ProcessPoolProductionReportCorrectionReqVO }
) =>
  request.post<OwnReturnCorrectionResult>({
    url: '/mes/pro/frontline-return-correction/production/resubmit',
    data
  })
export const resubmitOwnPqc = (
  data: OwnReturnContext & { correction: ProcessPoolPqcInspectionCorrectionReqVO }
) =>
  request.post<OwnReturnCorrectionResult>({
    url: '/mes/pro/frontline-return-correction/pqc/resubmit',
    data
  })
