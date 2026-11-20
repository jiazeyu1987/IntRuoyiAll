import { validateApplicationRoundMappings } from './application-round-context'
import { validateApplicationNodeRecords } from './application-node-records'
import type { ControlledFileApplicationEvidence } from '@/api/dcc/controlledFile/applicationRead'
import type { ControlledFileVO, ControlledFileSignatureSummaryVO } from '@/api/dcc/controlledFile/workflow'
import { buildApprovalRecords, approvalNodeLabel, approvalSignatureStatus } from './approval-records'

export interface ControlledApprovalRow {
  key: string
  item: string
  details: string[]
  actor: string
  time?: string | number | readonly number[] | null
  result: string
  comment?: string | null
  signature: string
}

const lifecycleStages = new Set(['EFFECTIVE', 'CONTROLLED', 'ACTIVATED', 'PUBLISHED', 'DISTRIBUTION', 'DISTRIBUTION_CONFIRM', 'TRAINING'])
const otherSignatureActions = new Set(['DISTRIBUTION_ACK', 'DISTRIBUTION_SIGN', 'ARCHIVE_SEAL'])
const stageNames: Record<string, string> = {
  SUPERVISOR_REVIEW: '直属主管审核', MATRIX_REVIEW: '实际会签人会签', MATRIX_APPROVAL: '批准',
  DOC_CONTROL_REVIEW: '文控审核', DOC_CONTROL_APPROVAL: '文控批准', DOC_CONTROL_STANDARDIZATION: '文控标准化审核',
  DELAYED_EFFECTIVE_CONFIRM: '延迟生效确认'
}
const resultNames: Record<string, string> = {
  APPROVE: '已通过', APPROVED: '已通过', REJECT: '已驳回', REJECTED: '已驳回',
  RETURN: '已退回', RETURNED: '已退回', ASSIGN: '已指派', ASSIGNED: '已指派',
  TRANSFER: '已转办', TRANSFERRED: '已转办', ADD_SIGN: '已加签', SIGN_ADDED: '已加签'
}
const taskResults: Record<string, string> = {
  '-2': '跳过', '-1': '未开始', '0': '待审批', '1': '审批中', '2': '已通过', '3': '审批不通过',
  '4': '已取消', '5': '已退回', '7': '审批通过中'
}
const businessResults = new Set(Object.values(resultNames))
const recordedResult = (result?: string | null, task = false): string => {
  if (!result) return '—'
  return (task ? taskResults[result] : resultNames[result]) || (businessResults.has(result) ? result : '—')
}
const signatureItem = (signature?: ControlledFileSignatureSummaryVO): string => {
  if (['ASSIGN', 'ASSIGNED'].includes(signature?.actionType || '')) return '部门负责人签名指派'
  const meaning = signature?.meaningCode || ''
  const stage = Object.keys(stageNames).find(key => meaning.startsWith(`${key}_`))
  return stage ? stageNames[stage] : '签名（职责未记录）'
}

/** Only signature identity + task + round can enrich an approval; names and times never establish a link. */
export const buildControlledApprovalRows = (
  file: ControlledFileVO, evidence: ControlledFileApplicationEvidence | undefined,
  round: string | undefined | null, userNames: Map<number, string> = new Map()
): ControlledApprovalRow[] => {
  const summaries = (file.signatureSummaries || []).filter(s => s.processInstanceId === round
    && (s.versionNo == null || s.versionNo === file.versionNo))
  const rows: ControlledApprovalRow[] = []
  if (round === file.processInstanceId && file.submittedTime != null) rows.push({
    key: 'submission', item: '提交申请', details: [], actor: userNames.get(file.requesterId) || '提交人未记录',
    time: file.submittedTime, result: '已提交', signature: '—'
  })
  const source = evidence ? buildApprovalRecords(evidence) : []
  for (const row of source) {
    const summary = summaries.find(s => row.signatureId != null && String(s.id) === String(row.signatureId)
      && s.taskId === row.taskId && s.processInstanceId === row.bpmRound)
    if (lifecycleStages.has(row.stageCode) || otherSignatureActions.has(row.actionType)) continue
    const independent = row.recordId.startsWith('signature:')
    const details = row.confirmedFields
      .filter(field => ['signoffAssignee', 'obligationDepartment'].includes(field.field))
      .map(field => `${({ signoffAssignee: '指派会签人', obligationDepartment: '会签义务部门' })[field.field]}：${field.value || '未记录'}`)
    if (row.assignmentTarget && !row.confirmedFields.some(f => f.field === 'signoffAssignee'))
      details.unshift(`指派会签人：${row.assignmentTarget.nickname || '姓名未记录'}`)
    rows.push({
      key: row.recordId,
      item: independent ? signatureItem(summary) : /^[A-Z][A-Z0-9_]+$/.test(approvalNodeLabel(row))
        ? stageNames[row.stageCode] || '审批环节名称未记录' : approvalNodeLabel(row), details,
      actor: row.actorNicknameSnapshot || row.actorUsernameSnapshot || summary?.actorNicknameSnapshot
        || summary?.actorUsernameSnapshot || (row.actorId == null ? '' : userNames.get(Number(row.actorId))) || '办理人未记录',
      time: row.occurredAt,
      result: independent ? recordedResult(summary?.taskActionResult) : recordedResult(row.result, row.actionType === 'TASK_COMPLETED'),
      comment: row.comment,
      signature: row.signatureId == null ? '—' : approvalSignatureStatus(row.signatureEvidenceStatus || row.signature?.evidenceStatus)
    })
  }
  for (const signature of summaries) {
    if (source.some(row => row.signatureId != null && String(row.signatureId) === String(signature.id)
      && row.taskId === signature.taskId && row.bpmRound === signature.processInstanceId)) continue
    if (otherSignatureActions.has(signature.actionType || '')) continue
    rows.push({ key: `standalone:${signature.id}`, item: signatureItem(signature), details: [],
      actor: signature.actorNicknameSnapshot || signature.actorUsernameSnapshot || userNames.get(signature.actorId) || '签名人未记录',
      time: signature.signedAt, result: recordedResult(signature.taskActionResult), comment: signature.comment,
      signature: approvalSignatureStatus(signature.evidenceStatus) })
  }
  return rows
}

export const controlledApprovalRules = (file: ControlledFileVO, round?: string | null) =>
  round === file.processInstanceId ? [...(file.routeSnapshots || [])]
    .sort((a, b) => (a.stageOrder ?? a.stageNo) - (b.stageOrder ?? b.stageNo))
    .map(snapshot => ({
      name: snapshot.stageName && !/^[A-Z][A-Z0-9_]+$/.test(snapshot.stageName)
        ? snapshot.stageName : stageNames[snapshot.stageCode || ''] || '审批环节名称未记录',
      requirement: snapshot.requireAllApprovals === true || snapshot.approveMethod === 'ALL' ? '全部通过'
        : snapshot.approveRatio != null ? `通过比例 ${snapshot.approveRatio}%`
        : snapshot.approveMethod === 'ANY' ? '任意通过' : '通过要求未记录'
    })) : []



export interface ControlledApprovalGroup {
  key: string
  version: string
  applicationType: string
  applicationLabel: string
  submittedAt?: string | number | readonly number[] | null
  rows: ControlledApprovalRow[]
  error: string
}

interface ApprovalHistoryReaders {
  detail: (id: number | string, context: import('@/api/dcc/controlledFile/bodyReadContext').DccBodyReadContext) => Promise<ControlledFileVO>
  rounds: (id: number | string, context?: import('@/api/dcc/controlledFile/bodyReadContext').DccBodyReadContext) => Promise<import('@/api/dcc/controlledFile/applicationRead').ControlledFileApplicationRound[]>
  evidence: (id: number | string, type: import('@/api/dcc/controlledFile/applicationRead').DccApplicationType, round: string,
    context?: import('@/api/dcc/controlledFile/bodyReadContext').DccBodyReadContext) => Promise<ControlledFileApplicationEvidence>
}

export const approvalApplicationLabel = (type: string, change?: string | null): string =>
  type === 'UPLOAD' ? '初始上传' : type === 'OBSOLETE' ? '作废申请'
    : type === 'REVISION' ? change === 'PARTIAL' ? '局部变更' : change === 'REPLACEMENT' ? '换版变更' : '变更申请'
      : '申请信息未记录'

/** Enumerate only the authorized logical-file history; each read independently enforces existing access rules. */
export const loadControlledApprovalGroups = async (
  file: ControlledFileVO, readers: ApprovalHistoryReaders, userNames?: Map<number, string>,
  initialContext?: import('@/api/dcc/controlledFile/bodyReadContext').DccBodyReadContext
): Promise<ControlledApprovalGroup[]> => {
  const groups: ControlledApprovalGroup[] = []
  const versions = new Map<string, { id: number | string; versionNo: string }>()
  versions.set(String(file.id), { id: file.id, versionNo: file.versionNo })
  for (const version of file.versionHistory || []) {
    if (!version.versionNo) continue
    versions.set(String(version.id), { id: version.id, versionNo: version.versionNo })
  }
  for (const version of versions.values()) {
    let detail: ControlledFileVO
    // Enumerating all applications is a separate permission-checked read, never a reuse of one round's grant.
    const context = String(version.id) === String(file.id)
      ? initialContext && !('bpmRound' in initialContext) ? initialContext : undefined : {
      intent: 'VERSION_HISTORY' as const, controlledFileId: String(version.id), expectedVersionNo: version.versionNo
    }
    try {
      detail = String(version.id) === String(file.id) ? file : await readers.detail(version.id, context!)
      if (String(detail.id) !== String(version.id) || detail.versionNo !== version.versionNo
        || !file.masterId || String(detail.masterId) !== String(file.masterId)) throw new Error('版本身份不一致')
    } catch {
      groups.push({ key: `version:${version.id}`, version: version.versionNo, applicationType: '',
        applicationLabel: '申请信息未读取', rows: [], error: '该版本审批资料读取失败或当前无查阅权限，请重试。' })
      continue
    }
    let mappings: import('@/api/dcc/controlledFile/applicationRead').ControlledFileApplicationRound[] = []
    let mappingError = ''
    try {
      mappings = validateApplicationRoundMappings(detail.id, await readers.rounds(detail.id, context))
    } catch { mappingError = '本版本申请关联读取失败，以下仅列已授权的独立记录。' }
    const mappedRounds = new Set<string>()
    for (const mapping of mappings) {
      mappedRounds.add(mapping.bpmRound)
      const group: ControlledApprovalGroup = {
        key: JSON.stringify([String(detail.id), mapping.applicationType, mapping.bpmRound, mapping.attributeRound]),
        version: detail.versionNo, applicationType: mapping.applicationType,
        applicationLabel: approvalApplicationLabel(mapping.applicationType, mapping.revisionChangeType),
        submittedAt: mapping.applicationTime, rows: [], error: ''
      }
      // Keep independently authorized version signatures even when the association/evidence request fails.
      const selectedDetail = detail
      let evidence: ControlledFileApplicationEvidence | undefined
      try {
        // Evidence authorizes its own explicit application; reading its body is not a prerequisite.
        const result = await readers.evidence(detail.id, mapping.applicationType, mapping.bpmRound, context)
        if (String(result.controlledFileId) !== String(detail.id) || result.versionNo !== detail.versionNo
          || result.applicationType !== mapping.applicationType || result.bpmRound !== mapping.bpmRound
          || (result.attributeRound != null && result.attributeRound !== mapping.attributeRound)) throw new Error('申请证据身份不一致')
        validateApplicationNodeRecords(result)
        evidence = result
      } catch { group.error = '本次申请关联读取失败，以下保留已授权的独立记录。' }
      group.rows = buildControlledApprovalRows({ ...selectedDetail, submittedTime: undefined }, evidence, mapping.bpmRound, userNames)
      if (mapping.applicationTime != null) group.rows.unshift({ key: 'submission', item: '提交申请', details: [],
        actor: mapping.bpmRound === selectedDetail.processInstanceId ? userNames?.get(selectedDetail.requesterId) || '提交人未记录' : '提交人未记录',
        time: mapping.applicationTime, result: '已提交', signature: '—' })
      groups.push(group)
    }
    const independentRounds = new Set((detail.signatureSummaries || []).map(signature => signature.processInstanceId).filter(Boolean))
    if (detail.processInstanceId) independentRounds.add(detail.processInstanceId)
    for (const round of independentRounds) {
      if (!round || mappedRounds.has(round)) continue
      groups.push({ key: JSON.stringify([String(detail.id), 'unmapped', round]), version: detail.versionNo,
        applicationType: '', applicationLabel: '申请信息未记录',
        submittedAt: round === detail.processInstanceId ? detail.submittedTime : undefined,
        rows: buildControlledApprovalRows(detail, undefined, round, userNames),
        error: mappingError || '未查到本组独立记录的正式申请关联。' })
    }
    if (!mappings.length && !independentRounds.size && mappingError) groups.push({ key: `mapping:${detail.id}`,
      version: detail.versionNo, applicationType: '', applicationLabel: '申请信息未读取', rows: [], error: mappingError })
  }
  return groups
}

export const filterControlledApprovalGroups = (groups: ControlledApprovalGroup[], version: string, type: string) =>
  groups.filter(group => (!version || group.version === version) && (!type || group.applicationType === type || group.applicationLabel === type))
