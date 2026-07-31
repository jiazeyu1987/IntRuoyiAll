import request from '@/config/axios'

export interface ProcessPoolReviewCopyFieldMappingVO {
  fieldCode: string
  fieldName: string
  lowerLimit: number | string
  upperLimit: number | string
  valueType?: string
  affectsAllocation?: boolean
  allocationField?: string
  sourceQuantityFragmentId?: number
  templateFieldMetadataJson?: string
}

export interface ProcessPoolReviewCopyGenerateSubmitReqVO {
  eventId: number
  reviewerUserId: number
  reviewerSignatureId: number
  reviewerSignatureUserId: number
  reviewerSignatureSnapshot: string
  fieldMappings: ProcessPoolReviewCopyFieldMappingVO[]
}

export interface ProcessPoolReviewCopyGenerateFromRulesReqVO {
  eventId: number
  reviewerUserId: number
  reviewerSignatureId: number
  reviewerSignatureUserId: number
  reviewerSignatureSnapshot: string
}

export const generateSubmitProcessPoolReviewCopy = async (
  data: ProcessPoolReviewCopyGenerateSubmitReqVO
) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/review-copy/generate-submit',
    data
  })
}

export const generateSubmitProcessPoolReviewCopyFromRules = async (
  data: ProcessPoolReviewCopyGenerateFromRulesReqVO
) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/review-copy/generate-submit-from-rules',
    data
  })
}
