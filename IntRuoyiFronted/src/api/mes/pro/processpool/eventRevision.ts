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

export const updateProcessPoolOriginalRecord = async (data: ProcessPoolEventRevisionUpdateReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/event-revision/update-original',
    data
  })
}
