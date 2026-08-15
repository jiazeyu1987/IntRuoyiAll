import type {
  DccControlledFileAction,
  DccControlledFileActionProjectionVO
} from '@/api/dcc/controlledFile/workflow'
import {
  resolveControlledActionProjection,
  type ControlledActionProjectionState,
  type ControlledActionProjectionVO
} from '@/api/form-center/actionProjection'

export const DCC_CONTROLLED_FILE_STATUSES = [
  'DRAFT',
  'PENDING_DOC_CONTROL_REVIEW',
  'PENDING_MATRIX_REVIEW',
  'PENDING_MATRIX_APPROVAL',
  'PENDING_DOC_CONTROL_APPROVAL',
  'PENDING_APPLICANT_REWORK',
  'PENDING_APPLICANT_TRAINING_RECORD',
  'READY_TO_PUBLISH',
  'FINALIZING',
  'TRAINING_IN_PROGRESS',
  'PENDING_MANUAL_DISTRIBUTION',
  'ACTIVE',
  'REJECTED',
  'WITHDRAWN',
  'OBSOLETE',
  'SUPERSEDED',
  'FINALIZATION_FAILED'
] as const

export type DccControlledFileStatus = (typeof DCC_CONTROLLED_FILE_STATUSES)[number]

export const DCC_CATEGORY_PERMISSIONS = [
  'VIEW',
  'UPLOAD',
  'DOWNLOAD',
  'PRINT',
  'OBSOLETE',
  'REVIEW',
  'APPROVE',
  'DISTRIBUTE'
] as const

export type DccCategoryPermission = (typeof DCC_CATEGORY_PERMISSIONS)[number]

export const DCC_CONTROLLED_FILE_STAGE_KEYS = [
  'DOC_CONTROL_REVIEW',
  'MATRIX_REVIEW',
  'MATRIX_APPROVAL',
  'DOC_CONTROL_APPROVAL'
] as const

export type DccControlledFileStageKey = (typeof DCC_CONTROLLED_FILE_STAGE_KEYS)[number]
export type DccControlledFileTagType = 'success' | 'primary' | 'warning' | 'danger' | 'info'

export interface DccOption<T extends string> {
  label: string
  value: T
}

export interface DccControlledFileStatusDefinition {
  value: DccControlledFileStatus
  label: string
  tagType: DccControlledFileTagType
  order: number
}

export interface DccControlledFileStageDefinition {
  key: DccControlledFileStageKey
  label: string
  order: number
  pendingStatus: DccPendingControlledFileStatus
  requiredPermission: Extract<DccCategoryPermission, 'REVIEW' | 'APPROVE'>
}

export const DCC_WITHDRAWABLE_CONTROLLED_FILE_STATUSES = [
  'PENDING_DOC_CONTROL_REVIEW',
  'PENDING_MATRIX_REVIEW',
  'PENDING_MATRIX_APPROVAL',
  'PENDING_DOC_CONTROL_APPROVAL',
  'PENDING_APPLICANT_TRAINING_RECORD'
] as const

export type DccPendingControlledFileStatus =
  (typeof DCC_WITHDRAWABLE_CONTROLLED_FILE_STATUSES)[number]

export const DCC_CONTROLLED_FILE_STATUS_DEFINITIONS: readonly DccControlledFileStatusDefinition[] =
  [
    { value: 'DRAFT', label: '草稿', tagType: 'info', order: 1 },
    { value: 'PENDING_DOC_CONTROL_REVIEW', label: '待文控审核', tagType: 'primary', order: 2 },
    { value: 'PENDING_MATRIX_REVIEW', label: '待会签审核', tagType: 'primary', order: 3 },
    { value: 'PENDING_MATRIX_APPROVAL', label: '待会签批准', tagType: 'primary', order: 4 },
    { value: 'PENDING_DOC_CONTROL_APPROVAL', label: '待文控批准', tagType: 'primary', order: 5 },
    { value: 'PENDING_APPLICANT_REWORK', label: '待申请人处理回退', tagType: 'warning', order: 6 },
    { value: 'PENDING_APPLICANT_TRAINING_RECORD', label: '待申请人上传培训记录', tagType: 'warning', order: 7 },
    { value: 'READY_TO_PUBLISH', label: '待发布', tagType: 'warning', order: 8 },
    { value: 'FINALIZING', label: '发布处理中', tagType: 'warning', order: 9 },
    { value: 'TRAINING_IN_PROGRESS', label: '培训中', tagType: 'warning', order: 10 },
    { value: 'PENDING_MANUAL_DISTRIBUTION', label: '待文控下发', tagType: 'primary', order: 11 },
    { value: 'ACTIVE', label: '现行', tagType: 'success', order: 12 },
    { value: 'REJECTED', label: '已驳回', tagType: 'danger', order: 13 },
    { value: 'WITHDRAWN', label: '已撤回', tagType: 'info', order: 14 },
    { value: 'OBSOLETE', label: '已作废', tagType: 'info', order: 15 },
    { value: 'SUPERSEDED', label: '已替代', tagType: 'info', order: 16 },
    { value: 'FINALIZATION_FAILED', label: '发布失败', tagType: 'danger', order: 17 }
  ]

export const DCC_CONTROLLED_FILE_STAGE_DEFINITIONS: readonly DccControlledFileStageDefinition[] = [
  {
    key: 'DOC_CONTROL_REVIEW',
    label: '文控审核',
    order: 1,
    pendingStatus: 'PENDING_DOC_CONTROL_REVIEW',
    requiredPermission: 'REVIEW'
  },
  {
    key: 'MATRIX_REVIEW',
    label: '会签审核',
    order: 2,
    pendingStatus: 'PENDING_MATRIX_REVIEW',
    requiredPermission: 'REVIEW'
  },
  {
    key: 'MATRIX_APPROVAL',
    label: '会签批准',
    order: 3,
    pendingStatus: 'PENDING_MATRIX_APPROVAL',
    requiredPermission: 'APPROVE'
  },
  {
    key: 'DOC_CONTROL_APPROVAL',
    label: '文控批准',
    order: 4,
    pendingStatus: 'PENDING_DOC_CONTROL_APPROVAL',
    requiredPermission: 'APPROVE'
  }
]

export const DCC_CONTROLLED_FILE_STATUS_OPTIONS: ReadonlyArray<DccOption<DccControlledFileStatus>> =
  DCC_CONTROLLED_FILE_STATUS_DEFINITIONS.map(({ label, value }) => ({ label, value }))

export const DCC_CATEGORY_PERMISSION_OPTIONS: ReadonlyArray<DccOption<DccCategoryPermission>> = [
  { label: '查看', value: 'VIEW' },
  { label: '上传', value: 'UPLOAD' },
  { label: '下载', value: 'DOWNLOAD' },
  { label: '打印', value: 'PRINT' },
  { label: '作废', value: 'OBSOLETE' },
  { label: '审核', value: 'REVIEW' },
  { label: '批准', value: 'APPROVE' },
  { label: '分发', value: 'DISTRIBUTE' }
]

export const DCC_CONTROLLED_FILE_STAGE_OPTIONS: ReadonlyArray<DccOption<DccControlledFileStageKey>> =
  DCC_CONTROLLED_FILE_STAGE_DEFINITIONS.map(({ key, label }) => ({ label, value: key }))

const statusDefinitionMap = new Map(
  DCC_CONTROLLED_FILE_STATUS_DEFINITIONS.map((definition) => [definition.value, definition] as const)
)

const stageDefinitionByStatusMap = new Map(
  DCC_CONTROLLED_FILE_STAGE_DEFINITIONS.map((definition) => [definition.pendingStatus, definition] as const)
)

const permissionLabelMap = new Map(
  DCC_CATEGORY_PERMISSION_OPTIONS.map((option) => [option.value, option.label] as const)
)

export const getDccControlledFileStatusDefinition = (status: DccControlledFileStatus | undefined) => {
  return status ? statusDefinitionMap.get(status) : undefined
}

export const getDccControlledFileStatusLabel = (status: DccControlledFileStatus | undefined) => {
  return getDccControlledFileStatusDefinition(status)?.label ?? '-'
}

export const getDccControlledFileStatusTagType = (status: DccControlledFileStatus | undefined) => {
  return getDccControlledFileStatusDefinition(status)?.tagType ?? 'info'
}

export const isDccControlledFileWithdrawableStatus = (
  status: DccControlledFileStatus | undefined
): status is DccPendingControlledFileStatus => {
  return !!status && DCC_WITHDRAWABLE_CONTROLLED_FILE_STATUSES.includes(status as DccPendingControlledFileStatus)
}

export const getDccControlledFileStageByStatus = (status: DccControlledFileStatus | undefined) => {
  if (status === 'PENDING_APPLICANT_TRAINING_RECORD') {
    return getDccControlledFileStageByKey('DOC_CONTROL_APPROVAL')
  }
  return status ? stageDefinitionByStatusMap.get(status as DccPendingControlledFileStatus) : undefined
}

export const getDccControlledFileStageByKey = (key: DccControlledFileStageKey | undefined) => {
  return key ? DCC_CONTROLLED_FILE_STAGE_DEFINITIONS.find((definition) => definition.key === key) : undefined
}

export const getDccCategoryPermissionLabel = (permission: DccCategoryPermission | undefined) => {
  return permission ? permissionLabelMap.get(permission) ?? '-' : '-'
}

export const hasDccCategoryPermission = (
  permissions: ReadonlyArray<DccCategoryPermission> | undefined,
  permission: DccCategoryPermission
) => {
  return permissions?.includes(permission) ?? false
}

export const hasAnyDccCategoryPermission = (
  permissions: ReadonlyArray<DccCategoryPermission> | undefined,
  requiredPermissions: ReadonlyArray<DccCategoryPermission>
) => {
  return requiredPermissions.some((permission) => hasDccCategoryPermission(permissions, permission))
}

export const DCC_ACTION_PROJECTION_MISSING_REASON =
  '后端未返回 actionProjection，当前操作区已进入只读保护。'

export interface DccActionProjectionReadable {
  actionProjection?: DccControlledFileActionProjectionVO | null
}

export const hasDccControlledFileActionProjection = (
  source: DccActionProjectionReadable | null | undefined
) => {
  const projection = source?.actionProjection
  return !!projection && Array.isArray(projection.allowedActions)
}

export const getDccControlledFileAllowedActions = (
  source: DccActionProjectionReadable | null | undefined
): ReadonlyArray<DccControlledFileAction> => {
  const projection = source?.actionProjection
  if (!projection || !Array.isArray(projection.allowedActions)) {
    return []
  }
  return projection.allowedActions
}

export const isDccControlledFileActionAllowed = (
  source: DccActionProjectionReadable | null | undefined,
  action: DccControlledFileAction
) => {
  return resolveDccControlledFileActionState(source, action).allowed
}

export const isDccControlledFileActionUnlocked = (
  source: DccActionProjectionReadable | null | undefined
) => hasDccControlledFileActionProjection(source) && source?.actionProjection?.actionLocked === false

export const resolveDccActionProjectionReadonlyReason = (
  source: DccActionProjectionReadable | null | undefined,
  defaultReason = '后端动作投影未放行当前操作。'
) => {
  if (!hasDccControlledFileActionProjection(source)) {
    return DCC_ACTION_PROJECTION_MISSING_REASON
  }
  const reason = String(source?.actionProjection?.actionLockReason || '').trim()
  return reason || defaultReason
}

export const mapDccControlledFileProjection = (
  source: DccActionProjectionReadable | null | undefined,
  action: DccControlledFileAction,
  actionLabel?: string
): ControlledActionProjectionVO | null => {
  if (!hasDccControlledFileActionProjection(source)) {
    return null
  }
  const projection = source?.actionProjection
  const allowed = getDccControlledFileAllowedActions(source).includes(action)
  const lockedForAction = projection?.actionLocked === true && !allowed
  return {
    actionCode: action,
    actionLabel: actionLabel || action,
    allowed,
    permissionGranted: allowed,
    locked: lockedForAction,
    pending: lockedForAction,
    pendingInstanceId: lockedForAction ? projection?.pendingRequestId ?? undefined : undefined,
    withdrawable: action === 'WITHDRAW' && projection?.canWithdraw === true,
    blockerCode: lockedForAction ? 'PENDING_APPROVAL_ACTION_LOCK' : allowed ? '' : 'ACTION_PROJECTION_BLOCKED',
    blockerReason: lockedForAction
      ? projection?.actionLockReason || '已有审批中的申请，请先撤回、审批或等待结束。'
      : projection?.actionLockReason || undefined,
    lockReason: lockedForAction ? projection?.actionLockReason || undefined : undefined
  }
}

export const resolveDccControlledFileActionState = (
  source: DccActionProjectionReadable | null | undefined,
  action: DccControlledFileAction,
  actionLabel?: string
): ControlledActionProjectionState => {
  return resolveControlledActionProjection(
    mapDccControlledFileProjection(source, action, actionLabel),
    actionLabel || action
  )
}
