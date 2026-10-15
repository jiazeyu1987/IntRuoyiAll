import type { ControlledFileApplicationEvidence } from '@/api/dcc/controlledFile/applicationRead'

const identity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(String(value)) > 9223372036854775807n) throw new Error('节点记录身份无效')
  return String(value)
}

/** Validates supplied evidence only; authority and historical snapshots remain backend responsibilities. */
export const validateApplicationNodeRecords = (evidence: ControlledFileApplicationEvidence): void => {
  if (!Array.isArray(evidence.nodeRecords)) throw new Error('前序节点记录未返回，请刷新申请证据')
  const ids = new Set<string>(), signatures = new Map(evidence.signatures.map(s => [s.id == null ? '' : identity(s.id), s]))
  for (const record of evidence.nodeRecords) {
    if (!record.recordId || ids.has(record.recordId) || identity(record.controlledFileId) !== identity(evidence.controlledFileId)
      || record.bpmRound !== evidence.bpmRound || !record.taskId || !record.stageCode || !Array.isArray(record.confirmedFields))
      throw new Error('前序节点记录与当前文件、轮次或任务身份不一致')
    ids.add(record.recordId)
    if (record.signatureId != null) {
      const signed = signatures.get(identity(record.signatureId))
      if (!signed || signed.processInstanceId !== record.bpmRound || signed.taskId !== record.taskId)
        throw new Error('前序节点记录与真实电子签名绑定不一致')
    }
    if (record.assignmentTarget) identity(record.assignmentTarget.userId)
  }
}
