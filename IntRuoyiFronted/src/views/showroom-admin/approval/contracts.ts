export interface ShowroomApprovalItem {
  fieldCode: string
  oldValueJson: string
  newValueJson: string
  label: string
  oldValue: string
  newValue: string
  approvalStatus: string
  approvedBy: number | null
  approvedAt: string | null
  comment: string | null
}

export interface ShowroomChangeRequestRecord {
  changeRequestId: number
  targetType: string
  targetId: number
  targetRevisionId: number
  moduleCode: string | null
  requestType: string
  submissionSource: string
  status: string
  processInstanceId: string | null
  submittedBy: number
  submitterDeptId: number | null
  submittedAt: string | null
  supervisorUserId: number | null
  supervisorDeptId: number | null
  supervisorActionAt: string | null
  gaoxinUserId: number | null
  gaoxinActionAt: string | null
  rejectionReason: string | null
  sourceAssignmentId: number | null
  items: ShowroomApprovalItem[]
}

export interface ShowroomApprovalPreview {
  targetType: string
  targetId: number
  liveRevisionId: number | null
  targetRevisionId: number
  liveFields: Record<string, string>
  targetFields: Record<string, string>
  rows: ShowroomApprovalPreviewRow[]
}

export interface ShowroomApprovalPreviewRow {
  fieldCode: string
  label: string
  liveValue: string
  targetValue: string
}

export interface ShowroomVersionAuditRecord {
  targetType: string
  targetId: number
  revisionId: number
  fieldCode: string
  oldValueJson: string
  newValueJson: string
  operatorId: number
  operatorAction: string
  createdAt: string | null
}

export interface ShowroomApprovalSignatureRecord {
  id: number
  changeRequestId: number
  approvalStage: string
  actionType: string
  actorId: number
  signatureMode: string
  passwordVerified: boolean | null
  comment: string | null
  signedAt: string | null
}

export interface ShowroomApprovalDetailRecord {
  changeRequest: ShowroomChangeRequestRecord
  fieldDiffs: ShowroomApprovalItem[]
  targetPreview: ShowroomApprovalPreview
  versionDiffs: ShowroomVersionAuditRecord[]
  signatureRecords: ShowroomApprovalSignatureRecord[]
}

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`审批中心缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`审批中心缺少数值字段：${fieldName}`)
  }
  return value
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`审批中心缺少字符串字段：${fieldName}`)
  }
  return value
}

const optionalString = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  return String(value)
}

const optionalNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`审批中心字段类型错误：${value}`)
  }
  return value
}

const expectStringMap = (value: unknown, fieldName: string) => {
  const record = expectRecord(value, fieldName)
  const normalized: Record<string, string> = {}
  for (const [key, entry] of Object.entries(record)) {
    normalized[key] = entry === undefined || entry === null ? '' : String(entry)
  }
  return normalized
}

const parseDisplayValue = (value: string) => {
  if (!value) {
    return ''
  }
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed) && parsed.value !== undefined) {
      return parsed.value === null ? '' : String(parsed.value)
    }
  } catch {
    return value
  }
  return value
}

const normalizeApprovalItem = (value: unknown, index: number): ShowroomApprovalItem => {
  const record = expectRecord(value, `items[${index}]`)
  const fieldCode = expectString(record.fieldCode, `items[${index}].fieldCode`)
  const oldValueJson = expectString(record.oldValueJson ?? '', `items[${index}].oldValueJson`, true)
  const newValueJson = expectString(record.newValueJson ?? '', `items[${index}].newValueJson`, true)
  return {
    fieldCode,
    oldValueJson,
    newValueJson,
    label: optionalString(record.label) || fieldCode,
    oldValue: optionalString(record.oldValue) ?? parseDisplayValue(oldValueJson),
    newValue: optionalString(record.newValue) ?? parseDisplayValue(newValueJson),
    approvalStatus: expectString(record.approvalStatus, `items[${index}].approvalStatus`),
    approvedBy: optionalNumber(record.approvedBy),
    approvedAt: optionalString(record.approvedAt),
    comment: optionalString(record.comment)
  }
}

const expectPreviewRows = (value: unknown): ShowroomApprovalPreviewRow[] => {
  if (!Array.isArray(value)) {
    throw new Error('审批中心缺少数组字段：targetPreview.rows')
  }
  return value.map((item, index) => {
    const record = expectRecord(item, `targetPreview.rows[${index}]`)
    return {
      fieldCode: expectString(record.fieldCode, `targetPreview.rows[${index}].fieldCode`),
      label: expectString(record.label, `targetPreview.rows[${index}].label`),
      liveValue: expectString(record.liveValue ?? '', `targetPreview.rows[${index}].liveValue`, true),
      targetValue: expectString(
        record.targetValue ?? '',
        `targetPreview.rows[${index}].targetValue`,
        true
      )
    }
  })
}

export const normalizeChangeRequestRecord = (value: unknown): ShowroomChangeRequestRecord => {
  const record = expectRecord(value, 'changeRequest')
  const rawItems = Array.isArray(record.items) ? record.items : []
  return {
    changeRequestId: expectNumber(record.changeRequestId, 'changeRequestId'),
    targetType: expectString(record.targetType, 'targetType'),
    targetId: expectNumber(record.targetId, 'targetId'),
    targetRevisionId: expectNumber(record.targetRevisionId, 'targetRevisionId'),
    moduleCode: optionalString(record.moduleCode),
    requestType: expectString(record.requestType, 'requestType'),
    submissionSource: expectString(record.submissionSource, 'submissionSource'),
    status: expectString(record.status, 'status'),
    processInstanceId: optionalString(record.processInstanceId),
    submittedBy: expectNumber(record.submittedBy, 'submittedBy'),
    submitterDeptId: optionalNumber(record.submitterDeptId),
    submittedAt: optionalString(record.submittedAt),
    supervisorUserId: optionalNumber(record.supervisorUserId),
    supervisorDeptId: optionalNumber(record.supervisorDeptId),
    supervisorActionAt: optionalString(record.supervisorActionAt),
    gaoxinUserId: optionalNumber(record.gaoxinUserId),
    gaoxinActionAt: optionalString(record.gaoxinActionAt),
    rejectionReason: optionalString(record.rejectionReason),
    sourceAssignmentId: optionalNumber(record.sourceAssignmentId),
    items: rawItems.map(normalizeApprovalItem)
  }
}

export const normalizeApprovalPage = (value: unknown): ShowroomChangeRequestRecord[] => {
  if (!Array.isArray(value)) {
    throw new Error('审批中心缺少数组字段：approvalPage')
  }
  return value.map((item) => normalizeChangeRequestRecord(item))
}

export const normalizeApprovalDetail = (value: unknown): ShowroomApprovalDetailRecord => {
  const record = expectRecord(value, 'approvalDetail')
  const fieldDiffs = Array.isArray(record.fieldDiffs)
    ? record.fieldDiffs.map(normalizeApprovalItem)
    : []
  const versionDiffs = Array.isArray(record.versionDiffs)
    ? record.versionDiffs.map((item, index) => {
        const audit = expectRecord(item, `versionDiffs[${index}]`)
        return {
          targetType: expectString(audit.targetType, `versionDiffs[${index}].targetType`),
          targetId: expectNumber(audit.targetId, `versionDiffs[${index}].targetId`),
          revisionId: expectNumber(audit.revisionId, `versionDiffs[${index}].revisionId`),
          fieldCode: expectString(audit.fieldCode, `versionDiffs[${index}].fieldCode`),
          oldValueJson: expectString(
            audit.oldValueJson ?? '',
            `versionDiffs[${index}].oldValueJson`,
            true
          ),
          newValueJson: expectString(
            audit.newValueJson ?? '',
            `versionDiffs[${index}].newValueJson`,
            true
          ),
          operatorId: expectNumber(audit.operatorId, `versionDiffs[${index}].operatorId`),
          operatorAction: expectString(
            audit.operatorAction,
            `versionDiffs[${index}].operatorAction`
          ),
          createdAt: optionalString(audit.createdAt)
        }
      })
    : []
  const signatureRecords = Array.isArray(record.signatureRecords)
    ? record.signatureRecords.map((item, index) => {
        const signature = expectRecord(item, `signatureRecords[${index}]`)
        return {
          id: expectNumber(signature.id, `signatureRecords[${index}].id`),
          changeRequestId: expectNumber(
            signature.changeRequestId,
            `signatureRecords[${index}].changeRequestId`
          ),
          approvalStage: expectString(
            signature.approvalStage,
            `signatureRecords[${index}].approvalStage`
          ),
          actionType: expectString(signature.actionType, `signatureRecords[${index}].actionType`),
          actorId: expectNumber(signature.actorId, `signatureRecords[${index}].actorId`),
          signatureMode: expectString(
            signature.signatureMode,
            `signatureRecords[${index}].signatureMode`
          ),
          passwordVerified:
            signature.passwordVerified === undefined || signature.passwordVerified === null
              ? null
              : Boolean(signature.passwordVerified),
          comment: optionalString(signature.comment),
          signedAt: optionalString(signature.signedAt)
        }
      })
    : []
  const preview = expectRecord(record.targetPreview, 'targetPreview')
  const liveFields = expectStringMap(preview.liveFields ?? {}, 'targetPreview.liveFields')
  const targetFields = expectStringMap(preview.targetFields ?? {}, 'targetPreview.targetFields')
  const previewRows = Array.isArray(preview.rows)
    ? expectPreviewRows(preview.rows)
    : Array.from(new Set([...Object.keys(liveFields), ...Object.keys(targetFields)])).map((fieldCode) => ({
        fieldCode,
        label: fieldCode,
        liveValue: liveFields[fieldCode] || '',
        targetValue: targetFields[fieldCode] || ''
      }))
  return {
    changeRequest: normalizeChangeRequestRecord(record.changeRequest),
    fieldDiffs,
    targetPreview: {
      targetType: expectString(preview.targetType, 'targetPreview.targetType'),
      targetId: expectNumber(preview.targetId, 'targetPreview.targetId'),
      liveRevisionId:
        preview.liveRevisionId === null || preview.liveRevisionId === undefined
          ? null
          : expectNumber(preview.liveRevisionId, 'targetPreview.liveRevisionId'),
      targetRevisionId: expectNumber(preview.targetRevisionId, 'targetPreview.targetRevisionId'),
      liveFields,
      targetFields,
      rows: previewRows
    },
    versionDiffs,
    signatureRecords
  }
}

const statusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '审批中',
  PENDING_SUPERVISOR_REVIEW: '主管审核中',
  PENDING_SUPERVISOR_APPROVAL: '主管审核中',
  PENDING_GAOXIN_APPROVAL: '企宣审批中',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  PUBLISHED: '已发布'
}

export const resolveApprovalStatusText = (status: string) => statusTextMap[status] || status

export const resolveApprovalStatusTagType = (status: string) => {
  if (status === 'APPROVED' || status === 'PUBLISHED') {
    return 'success'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  if (status.startsWith('PENDING')) {
    return 'warning'
  }
  return 'info'
}

export const resolveTargetTypeText = (targetType: string) => {
  if (targetType === 'COMPANY') {
    return '公司'
  }
  if (targetType === 'HALL') {
    return '展柜'
  }
  if (targetType === 'PRODUCT') {
    return '产品'
  }
  return targetType
}
