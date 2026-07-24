import request from '@/config/axios'
import type { EdhrRouteId } from './batchExecution'

export type EdhrSignatureActionType =
  | 'FIELD_CHANGE'
  | 'FORM_REVIEW'
  | 'SUBMIT'
  | 'REVIEW_APPROVE'
  | 'APPROVE'
  | 'REJECT'
  | 'ARCHIVE_SEAL'

export interface EdhrSignaturePageReqVO extends PageParam {
  executionId?: EdhrRouteId
  executionCode?: string
  actionType?: EdhrSignatureActionType
  actorId?: number
  actorName?: string
  processInstanceId?: string
  bpmTaskId?: string
  signedAtStart?: string
  signedAtEnd?: string
}

export interface EdhrSignatureSummaryVO {
  id: number
  executionId: number
  executionCode: string
  actorId: number
  actorName: string
  actorNickname?: string
  actorUsernameSnapshot?: string
  actorNicknameSnapshot?: string
  actorDeptIdSnapshot?: number
  actorDeptNameSnapshot?: string
  actorPostNamesSnapshot?: string
  actorRoleNamesSnapshot?: string
  actionType: EdhrSignatureActionType
  signatureMode: 'PASSWORD'
  passwordVerified: boolean
  comment?: string
  signedAt: string
  selectedSignedAt?: string
  signatureDisplayAt?: string
  signatureTimeMode?: 'SERVER_TIME' | 'USER_SELECTED'
  selectedTimeZone?: string
  selectedTimeReason?: string
  selectedTimePolicyVersion?: string
  selectedTimeAuditHash?: string
  processInstanceId?: string
  bpmTaskId?: string
  bpmTaskDefinitionKey?: string
  taskDefinitionKey?: string
  bpmTaskName?: string
  signatureCellKey?: string
  signatureRowIndex?: number
  signatureColumnIndex?: number
  reviewSourceType?: string
  reviewSourceId?: number
  reviewSourceName?: string
  approvalResult?: 'REVIEW_APPROVE' | 'APPROVE' | 'REJECT'
  reason?: string
  meaningText?: string
  signaturePurpose?: string
  authorizationBasis?: string
  authenticationMethod?: string
  recordVersionSnapshot?: string
  recordHashSnapshot?: string
  clientIpSnapshot?: string
  userAgentSnapshot?: string
  snapshotStatus?: string
}

export const getEdhrExecutionSignaturePage = async (params: EdhrSignaturePageReqVO) => {
  return await request.get<PageResult<EdhrSignatureSummaryVO[]>>({
    url: '/mes/pro/batch-record-execution/signature-page',
    params
  })
}
