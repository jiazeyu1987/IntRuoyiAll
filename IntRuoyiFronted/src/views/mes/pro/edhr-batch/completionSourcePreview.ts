import type {
  EdhrBatchExecutionReviewFormViewModel,
  EdhrBatchExecutionTaskPreviewRespVO
} from '@/api/mes/pro/edhr/batchExecution'

/** Backend projects only the verified source audit's cells for an unsubmitted form. */
export const resolveCompletionSourceDraftPreview = (
  preview?: EdhrBatchExecutionTaskPreviewRespVO
): EdhrBatchExecutionReviewFormViewModel | undefined => {
  const evidence = preview?.completionSourceMaterialization
  const hash = /^[a-f0-9]{64}$/
  if (
    evidence?.status !== 'APPLIED' ||
    evidence.executionStatus !== 0 ||
    preview?.executionCreated !== true ||
    !preview.formViewModel ||
    !evidence.receiptId ||
    !evidence.auditBatchId ||
    !evidence.executionId ||
    evidence.executionId !== String(preview.executionId) ||
    !hash.test(evidence.sourceSnapshotHash || '') ||
    !hash.test(evidence.mappingHash || '') ||
    !hash.test(evidence.fieldAuditHeadHash || '') ||
    !Number.isInteger(evidence.appliedFieldCount) ||
    Number(evidence.appliedFieldCount) <= 0
  ) {
    return undefined
  }
  return { ...preview.formViewModel, remark: undefined, signatureCellMarkers: [] }
}
