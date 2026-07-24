import request from '@/config/axios'
import type { EdhrRouteId, EdhrSignatureTimeReqVO } from './batchExecution'

export type EdhrFieldAuditSignatureAction = 'FIELD_CHANGE'

export type EdhrFieldAuditActionType = 'FIELD_CHANGE' | 'BASELINE_ANCHOR'

export type EdhrFieldValueType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATETIME'
  | 'SIGNATURE'
  | 'JSON'
  | 'NULL'

export type EdhrFieldTypedJsonValue =
  | string
  | number
  | boolean
  | null
  | Record<string, unknown>
  | unknown[]

export type EdhrFieldChangeReasonCategory =
  | 'CORRECTION'
  | 'PROCESS_OBSERVATION'
  | 'CALCULATION_FIX'
  | 'OPERATOR_ENTRY'
  | 'OTHER'

export type EdhrHashVerificationStatus =
  | 'VALID'
  | 'CHAIN_BROKEN'
  | 'SIGNATURE_MISMATCH'
  | 'SOURCE_MISSING'
  | 'CONCURRENCY_CONFLICT'

export type EdhrFieldAuditExportFormat = 'XLSX'

export type EdhrFieldResponsibilityEvidenceStatus = 'COMPLETE' | 'EVIDENCE_MISSING' | 'BLOCKED'

export type EdhrFieldResponsibilityValueOrigin =
  | 'HUMAN'
  | 'SYSTEM_BASELINE'
  | 'EMPTY_UNTOUCHED'
  | 'UNKNOWN'

export type EdhrFieldResponsibilityContextWarning = 'VERSION_CONTEXT_MISSING'

export type EdhrFieldResponsibilityReasonCode =
  | 'EXECUTION_SNAPSHOT_MISSING'
  | 'FIELD_DEFINITION_MISSING'
  | 'BASELINE_MISSING'
  | 'FIELD_AUDIT_MISSING'
  | 'SIGNATURE_MISSING'
  | 'SIGNATURE_INVALID'
  | 'CHAIN_INVALID'
  | 'CURRENT_VALUE_MISMATCH'
  | 'FIELD_IDENTITY_AMBIGUOUS'
  | 'CROSS_TENANT_ASSOCIATION'
  | 'CROSS_EXECUTION_ASSOCIATION'

export type EdhrFieldResponsibilityExportFormat = 'XLSX'

export interface EdhrHashVerificationVO {
  status: EdhrHashVerificationStatus
  calculatedHeadHash: string
  storedHeadHash: string
  checkedBatchCount: number
  checkedItemCount: number
  brokenBatchId?: string
  brokenItemId?: string
  failedReason?: string
  checkedAt: string
}

export interface EdhrFieldChangeItemReqVO {
  fieldPath: string
  fieldKey: string
  rowIndex: number
  columnIndex: number
  valueType: EdhrFieldValueType
  newValueJson: EdhrFieldTypedJsonValue
  newValueDisplay: string
  expectedOldValueJson?: EdhrFieldTypedJsonValue
  expectedOldValueHash?: string
}

export interface EdhrFieldAttachmentChangeReqVO {
  workTaskId: EdhrRouteId
  fieldPath: string
  fieldKey: string
  rowIndex: number
  columnIndex: number
  attachmentType: 'FILE' | 'IMAGE'
  attachmentAction: 'ADD' | 'REPLACE' | 'VOID'
  attachmentGroupKey?: string
  uploadToken?: string
  fileId?: number
  storageConfigId?: number
  storagePath?: string
  fileUrl?: string
  fileName?: string
  contentType?: string
  fileSize?: number
  sha256?: string
  storageRetentionJson?: string
  storageRetentionHash?: string
  expectedPreviousAttachmentHash?: string
}

export interface EdhrFieldAuditSignatureReqVO {
  password: string
  signatureTime?: EdhrSignatureTimeReqVO
}

export interface EdhrFieldChangeSaveReqVO {
  executionId: EdhrRouteId
  workTaskId: EdhrRouteId
  idempotencyKey: string
  baseCellValuesHash: string
  baseFieldAuditRevision: number
  baseFieldAuditHeadHash: string
  reasonCategory: EdhrFieldChangeReasonCategory
  reasonText: string
  changes: EdhrFieldChangeItemReqVO[]
  attachmentChanges?: EdhrFieldAttachmentChangeReqVO[]
  signature: EdhrFieldAuditSignatureReqVO
}

export interface EdhrFieldChangeSaveRespVO {
  executionId: number
  fieldAuditRevision: number
  fieldAuditHeadHash: string
  cellValuesHash: string
  auditBatchId: string
  signatureId: number
  hashVerification: EdhrHashVerificationVO
  changedAt: string
}

export interface EdhrFieldAuditSignatureVO {
  signatureId: number
  signatureMode: 'PASSWORD'
  actorId: number
  actorName: string
  actorNickname?: string
  signedAt: string
  selectedSignedAt?: string
  signatureDisplayAt?: string
  signatureTimeMode?: string
  selectedTimeZone?: string
  selectedTimeReason?: string
  selectedTimePolicyVersion?: string
  selectedTimeAuditHash?: string
  passwordVerified: boolean
  signatureChallengeHash: string
  fieldAuditRevision: number
  fieldAuditHeadHash: string
  cellValuesHash: string
}

export interface EdhrFieldAuditEntryVO {
  id: string
  auditBatchId: string
  executionId: number
  executionCode: string
  fieldAuditRevision: number
  fieldKey: string
  fieldPath: string
  fieldLabel: string
  rowIndex: number
  columnIndex: number
  component: string
  valueType: EdhrFieldValueType
  oldValueJson: EdhrFieldTypedJsonValue
  oldValueDisplay: string
  oldValueHash: string
  newValueJson: EdhrFieldTypedJsonValue
  newValueDisplay: string
  newValueHash: string
  reasonCategory: EdhrFieldChangeReasonCategory
  reasonText: string
  actorId: number
  actorName: string
  signatureId?: number
  previousHash: string
  auditHash: string
  changedAt: string
  hashVerification: EdhrHashVerificationVO
}

export interface EdhrFieldAuditPageReqVO extends PageParam {
  executionId?: EdhrRouteId
  auditBatchId?: string
  fieldKey?: string
  fieldPath?: string
  actorId?: number
  actorName?: string
  reasonCategory?: EdhrFieldChangeReasonCategory
  reasonKeyword?: string
  changedAtStart?: string
  changedAtEnd?: string
}

export interface EdhrFieldAuditPageRespVO {
  list: EdhrFieldAuditEntryVO[]
  total: number
}

export interface EdhrFieldAuditBatchVO {
  id: string
  executionId?: number
  executionCode?: string
  beforeFieldAuditRevision: number
  afterFieldAuditRevision: number
  baseFieldAuditHeadHash: string
  previousHeadHash: string
  newHeadHash: string
  baseCellValuesHash: string
  beforeCellValuesHash: string
  afterCellValuesHash: string
  signatureChallengeHash: string
  signatureProjectionHash: string
  signatureId?: number
  changedAt?: string
}

export interface EdhrFieldAuditDetailReqVO {
  executionId: EdhrRouteId
  auditBatchId?: string
  auditItemId?: string
}

export interface EdhrFieldAuditDetailRespVO {
  executionId: number
  executionCode: string
  auditBatch: EdhrFieldAuditBatchVO
  items: EdhrFieldAuditEntryVO[]
  signature?: EdhrFieldAuditSignatureVO
  hashVerification: EdhrHashVerificationVO
}

export interface EdhrFieldAuditVerifyReqVO {
  executionId: EdhrRouteId
  fromFieldAuditRevision?: number
  toFieldAuditRevision?: number
  expectedFieldAuditHeadHash?: string
  expectedCellValuesHash?: string
  includeBrokenItem?: boolean
}

export interface EdhrFieldAuditVerifyRespVO {
  hashVerification: EdhrHashVerificationVO
  verifiedCount: number
  firstFailedAuditItemId?: string
  fieldAuditRevision?: number
  fieldAuditHeadHash?: string
  cellValuesHash?: string
}

export interface EdhrFieldAuditExportReqVO extends Omit<EdhrFieldAuditPageReqVO, 'executionId'> {
  executionId: EdhrRouteId
  format: EdhrFieldAuditExportFormat
}

export interface EdhrFieldAuditExportRespVO {
  fileName: string
  contentType: string
  fileSize: number
  sha256: string
  executionId: number
  recordCount: number
  fieldAuditRevision: number
  fieldAuditHeadHash: string
  cellValuesHash: string
  hashVerification: EdhrHashVerificationVO
  generatedAt: string
  content: string | number[]
}

export interface EdhrFieldResponsibilitySummaryReqVO {
  executionId: EdhrRouteId
  pageNo: number
  pageSize: number
  fieldKeyword?: string
  evidenceStatus?: EdhrFieldResponsibilityEvidenceStatus
  valueOrigin?: EdhrFieldResponsibilityValueOrigin
  actorId?: number
}

export interface EdhrFieldResponsibilitySummaryRespVO {
  executionId: number
  executionCode: string
  batchRecordDefinitionId: number
  batchRecordVersionId?: number
  batchRecordReportId?: string
  fieldAuditRevision: number
  fieldAuditHeadHash: string
  cellValuesHash: string
  overallEvidenceStatus: EdhrFieldResponsibilityEvidenceStatus
  overallReasonCodes: EdhrFieldResponsibilityReasonCode[]
  contextWarnings: EdhrFieldResponsibilityContextWarning[]
  total: number
  list: EdhrFieldResponsibilityItemRespVO[]
}

export interface EdhrFieldResponsibilityItemRespVO {
  fieldPath: string
  fieldKey: string
  fieldLabel: string
  rowIndex: number
  columnIndex: number
  component: string
  valueType: string
  currentValueJson: string
  currentValueDisplay: string
  currentValueHash: string
  valueOrigin: EdhrFieldResponsibilityValueOrigin
  firstHumanActorId?: number
  firstHumanActorName?: string
  firstHumanChangedAt?: string
  currentValueActorId?: number
  currentValueActorName?: string
  currentValueChangedAt?: string
  evidenceStatus: EdhrFieldResponsibilityEvidenceStatus
  reasonCodes: EdhrFieldResponsibilityReasonCode[]
  historyCount: number
  latestAuditItemId?: string
}

export interface EdhrFieldResponsibilityHistoryReqVO {
  executionId: EdhrRouteId
  fieldPath: string
  fieldKey: string
  rowIndex: number
  columnIndex: number
  pageSize: number
  cursorFieldAuditRevision?: number
  cursorAuditItemId?: string
}

export interface EdhrFieldResponsibilityHistoryRespVO {
  executionId: number
  fieldPath: string
  fieldKey: string
  rowIndex: number
  columnIndex: number
  list: EdhrFieldResponsibilityHistoryItemRespVO[]
  hasMore: boolean
  nextCursorFieldAuditRevision?: number
  nextCursorAuditItemId?: string
}

export interface EdhrFieldResponsibilityHistoryItemRespVO {
  auditItemId: string
  auditBatchId: string
  fieldAuditRevision: number
  oldValueJson: string
  oldValueDisplay: string
  oldValueHash: string
  newValueJson: string
  newValueDisplay: string
  newValueHash: string
  reasonCategory: string
  reasonText: string
  actorId?: number
  actorName?: string
  changedAt: string
  signatureId?: number
  signatureActorUsernameSnapshot?: string
  signatureActorNicknameSnapshot?: string
  signatureDisplayAt?: string
  signatureProjectionHash?: string
  previousHash: string
  auditHash: string
  evidenceStatus: EdhrFieldResponsibilityEvidenceStatus
  reasonCodes: EdhrFieldResponsibilityReasonCode[]
}

export interface EdhrFieldResponsibilityExportReqVO {
  executionId: EdhrRouteId
  format?: EdhrFieldResponsibilityExportFormat
}

export interface EdhrFieldResponsibilityExportRespVO {
  fileName: string
  format: EdhrFieldResponsibilityExportFormat
  contentType: string
  contentBase64: string
  sha256: string
  recordCount: number
  fieldAuditRevision: number
  fieldAuditHeadHash: string
  cellValuesHash: string
  evidenceStatus: EdhrFieldResponsibilityEvidenceStatus
  reasonCodes: EdhrFieldResponsibilityReasonCode[]
  contextWarnings: EdhrFieldResponsibilityContextWarning[]
  generatedAt: string
}

export const EDHR_FIELD_CHANGE_REASON_OPTIONS: Array<{
  label: string
  value: EdhrFieldChangeReasonCategory
}> = [
  { label: '纠正录入', value: 'CORRECTION' },
  { label: '过程观察', value: 'PROCESS_OBSERVATION' },
  { label: '计算修正', value: 'CALCULATION_FIX' },
  { label: '操作录入', value: 'OPERATOR_ENTRY' },
  { label: '其他', value: 'OTHER' }
]

export const EDHR_HASH_STATUS_LABEL_MAP: Record<EdhrHashVerificationStatus, string> = {
  VALID: '校验通过',
  CHAIN_BROKEN: '链断裂',
  SIGNATURE_MISMATCH: '签名不匹配',
  SOURCE_MISSING: '源数据缺失',
  CONCURRENCY_CONFLICT: '并发冲突'
}

export const EDHR_HASH_STATUS_TAG_TYPE_MAP: Record<EdhrHashVerificationStatus, string> = {
  VALID: 'success',
  CHAIN_BROKEN: 'danger',
  SIGNATURE_MISMATCH: 'danger',
  SOURCE_MISSING: 'danger',
  CONCURRENCY_CONFLICT: 'warning'
}

export const saveEdhrFieldChanges = async (data: EdhrFieldChangeSaveReqVO) => {
  return await request.put<EdhrFieldChangeSaveRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/save-changes',
    data
  })
}

export const getEdhrFieldAuditPage = async (params: EdhrFieldAuditPageReqVO) => {
  return await request.get<EdhrFieldAuditPageRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/page',
    params
  })
}

export const getEdhrFieldAuditDetail = async (params: EdhrFieldAuditDetailReqVO) => {
  return await request.get<EdhrFieldAuditDetailRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/detail',
    params
  })
}

export const verifyEdhrFieldAuditChain = async (data: EdhrFieldAuditVerifyReqVO) => {
  return await request.post<EdhrFieldAuditVerifyRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/verify-chain',
    data
  })
}

export const exportEdhrFieldAudit = async (params: EdhrFieldAuditExportReqVO) => {
  return await request.get<EdhrFieldAuditExportRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/export',
    params
  })
}

export const getEdhrFieldResponsibilitySummary = async (
  params: EdhrFieldResponsibilitySummaryReqVO
) => {
  return await request.get<EdhrFieldResponsibilitySummaryRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/responsibility-summary',
    params
  })
}

export const getEdhrFieldResponsibilityHistory = async (
  params: EdhrFieldResponsibilityHistoryReqVO
) => {
  return await request.get<EdhrFieldResponsibilityHistoryRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/responsibility-history',
    params
  })
}

export const exportEdhrFieldResponsibility = async (
  params: EdhrFieldResponsibilityExportReqVO
) => {
  return await request.get<EdhrFieldResponsibilityExportRespVO>({
    url: '/mes/pro/batch-record-execution/field-audit/responsibility-export',
    params
  })
}
