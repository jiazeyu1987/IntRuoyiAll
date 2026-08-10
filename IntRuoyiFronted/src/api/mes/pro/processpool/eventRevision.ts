import request from '@/config/axios'

export type ProcessPoolFragmentOriginalField =
  | 'OUTPUT_QUANTITY'
  | 'QUALITY_STATUS'
  | 'ALLOCATABLE_STATUS'
  | 'REMARK'

export interface ProcessPoolEventRevisionFieldChangeVO {
  fieldCode: string
  fieldName: string
  beforeValue?: string
  afterValue?: string
  affectsQuantityFragment: boolean
  sourceQuantityFragmentId?: number
  originalField: ProcessPoolFragmentOriginalField
}

export interface ProcessPoolEventRevisionUpdateReqVO {
  eventId: number
  afterPayload: string
  changeReason: string
  revisionSignatureId: number
  revisionSignatureUserId: number
  revisionSignatureSnapshot: string
  modifiedByUserId: number
  changedFields: ProcessPoolEventRevisionFieldChangeVO[]
}

export interface ProcessPoolProductionReportCorrectionLossDetailReqVO {
  reasonId: number
  quantity: number
}

export interface ProcessPoolProductionReportCorrectionParameterReqVO {
  deviceId: number
  parameterCode: string
  value: number
}

export interface ProcessPoolProductionReportCorrectionReqVO {
  eventId: number
  outputQuantity: number
  lossDetails: ProcessPoolProductionReportCorrectionLossDetailReqVO[]
  deviceParameterReadings: ProcessPoolProductionReportCorrectionParameterReqVO[]
  changeReason: string
  signaturePassword: string
}

export interface ProcessPoolPqcInspectionCorrectionItemReqVO {
  itemCode: string
  selectedEquipmentId?: number
  selectedEquipmentNumber?: string
  sampleValues: string[]
}

export interface ProcessPoolPqcInspectionCorrectionReqVO {
  eventId: number
  actualInspectionQuantity: number
  scrapQuantity: number
  nonconformanceDescription?: string
  itemResults: ProcessPoolPqcInspectionCorrectionItemReqVO[]
  changeReason: string
  signaturePassword: string
}

export interface ProcessPoolProductionReportRevisionLogChangeVO {
  fieldName: string
  beforeValue: string
  afterValue: string
}

export interface ProcessPoolProductionReportRevisionLogVO {
  modifiedByName: string
  modifiedAt: string
  changeReason: string
  signatureConfirmed: boolean
  changes: ProcessPoolProductionReportRevisionLogChangeVO[]
}

export const updateProcessPoolOriginalRecord = async (data: ProcessPoolEventRevisionUpdateReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/event-revision/update-original',
    data
  })
}

export const correctProcessPoolProductionReport = async (
  data: ProcessPoolProductionReportCorrectionReqVO
) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/event-revision/correct-production-report',
    data
  })
}

export const correctProcessPoolPqcInspection = async (
  data: ProcessPoolPqcInspectionCorrectionReqVO
) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/event-revision/correct-pqc-inspection',
    data
  })
}

export const getProcessPoolProductionReportRevisionLogs = async (eventId: number) => {
  return await request.get<ProcessPoolProductionReportRevisionLogVO[]>({
    url: '/mes/pro/process-pool/event-revision/production-report-logs',
    params: { eventId }
  })
}
