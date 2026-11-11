import type { ControlledFileApplicationEvidence, ControlledFileApplicationNodeRecord } from '@/api/dcc/controlledFile/applicationRead'
import { validateApplicationNodeRecords } from './application-node-records'

export const approvalSignatureStatus = (status?: string | null): string => ({
  VALID: '有效', INVALID: '校验失败', PENDING_VERIFY: '待校验', HISTORICAL_UNBOUND: '历史未绑定',
  MISSING: '签名缺失', IDENTITY_MISMATCH: '签名身份不一致', UNRECORDED: '状态未记录'
})[status || ''] || '状态未记录'

export const approvalResultLabel = (row: Pick<ControlledFileApplicationNodeRecord, 'result' | 'actionType'>): string => {
  if (row.actionType === 'TASK_COMPLETED') return ({
    '-2': '跳过', '-1': '未开始', '0': '待审批', '1': '审批中', '2': '审批通过', '3': '审批不通过', '4': '已取消',
    '5': '已退回', '7': '审批通过中'
  })[row.result || ''] || '结果未记录'
  return ({ ASSIGN: '已指派', ASSIGNED: '已指派', APPROVE: '已通过', APPROVED: '已通过', REJECT: '已驳回',
    REJECTED: '已驳回', RETURN: '已退回', RETURNED: '已退回', TRANSFER: '已转办', TRANSFERRED: '已转办',
    ADD_SIGN: '已加签', SIGN_ADDED: '已加签' })[row.actionType] || '结果未记录'
}

export const buildApprovalRecords = (evidence: ControlledFileApplicationEvidence) => {
  validateApplicationNodeRecords(evidence)
  const rows = evidence.nodeRecords.map(row => ({
    ...row,
    signature: evidence.signatures.find(signature => String(signature.id) === String(row.signatureId)),
    relationStages: (evidence.relationEvidence?.stages || []).filter(stage =>
      String(row.signatureId) === stage.snapshot.header.signatureId && row.taskId === stage.snapshot.header.taskId)
  }))
  // A historical signature can exist even when its node projection was not recorded.
  for (const signature of evidence.signatures) {
    if (rows.some(row => String(row.signatureId) === String(signature.id))) continue
    rows.push({ recordId: `signature:${signature.id}`, controlledFileId: evidence.controlledFileId,
      bpmRound: evidence.bpmRound, taskId: signature.taskId || '', stageCode: '', nodeName: '电子签名',
      actionType: signature.actionType || '', actorNicknameSnapshot: signature.actorNicknameSnapshot,
      actorDeptNameSnapshot: signature.actorDeptNameSnapshot, occurredAt: signature.signedAt,
      signatureId: signature.id, signatureEvidenceStatus: signature.evidenceStatus,
      comment: signature.comment, confirmedFields: [], signature,
      relationStages: (evidence.relationEvidence?.stages || []).filter(stage => stage.snapshot.header.signatureId === String(signature.id)
        && stage.snapshot.header.taskId === signature.taskId) })
  }
  return rows
}
