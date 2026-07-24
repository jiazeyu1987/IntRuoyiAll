export type ControlledActionEffectStatus = 'APPLIED' | 'FAILED_PENDING' | 'EFFECT_FAILED_PENDING' | string

export interface ControlledActionProjectionVO {
  actionCode?: string
  actionLabel?: string
  allowed?: boolean
  permissionGranted?: boolean
  permissionCode?: string
  locked?: boolean
  pending?: boolean
  pendingInstanceId?: number | string
  pendingStatus?: string
  pendingApplicantUserId?: number
  pendingBpmTaskId?: number | string
  withdrawable?: boolean
  approvable?: boolean
  approvalTaskId?: number | string
  effectStatus?: ControlledActionEffectStatus
  effectFailureReason?: string
  blockerCode?: string
  blockerReason?: string
  disabledReason?: string
  lockReason?: string
  reason?: string
  message?: string
}

export interface ControlledActionProjectionState {
  actionLabel: string
  allowed: boolean
  disabled: boolean
  pending: boolean
  withdrawable: boolean
  approvable: boolean
  effectFailedPending: boolean
  projectionMissing: boolean
  blockerCode: string
  blockerMessage: string
  projection?: ControlledActionProjectionVO
}

const DEFAULT_ACTION_LABEL = '受控动作'
const EFFECT_FAILED_PENDING_STATUSES = new Set(['EFFECT_FAILED_PENDING', 'FAILED_PENDING'])

const normalizeActionLabel = (projection?: ControlledActionProjectionVO | null, actionLabel?: string) => {
  return actionLabel || projection?.actionLabel || projection?.actionCode || DEFAULT_ACTION_LABEL
}

const firstVisibleReason = (...reasons: Array<unknown>) => {
  for (const reason of reasons) {
    if (typeof reason === 'string' && reason.trim()) {
      return reason.trim()
    }
  }
  return ''
}

const normalizeEffectStatus = (projection: ControlledActionProjectionVO) => {
  return String(projection.effectStatus || '')
}

export const buildProjectionMissingState = (
  actionLabel = DEFAULT_ACTION_LABEL
): ControlledActionProjectionState => ({
  actionLabel,
  allowed: false,
  disabled: true,
  pending: false,
  withdrawable: false,
  approvable: false,
  effectFailedPending: false,
  projectionMissing: true,
  blockerCode: 'ACTION_PROJECTION_MISSING',
  blockerMessage: `${actionLabel}动作投影缺失，已阻止操作，请刷新后重试或联系管理员确认后端投影。`
})

export const resolveControlledActionProjection = (
  projection?: ControlledActionProjectionVO | null,
  actionLabel?: string
): ControlledActionProjectionState => {
  const label = normalizeActionLabel(projection, actionLabel)
  if (!projection) {
    return buildProjectionMissingState(label)
  }

  const effectFailedPending = EFFECT_FAILED_PENDING_STATUSES.has(normalizeEffectStatus(projection))
  const pending = projection.pending === true || Boolean(projection.pendingInstanceId)
  const permissionDenied = projection.permissionGranted === false
  const backendDenied = projection.allowed === false
  const locked = projection.locked === true
  const allowed =
    projection.allowed === true &&
    projection.permissionGranted !== false &&
    !locked &&
    !pending &&
    !effectFailedPending

  const blockerMessage = allowed
    ? ''
    : firstVisibleReason(
        effectFailedPending && (projection.effectFailureReason || '生效失败待处理，请先处理失败原因后再继续。'),
        pending && '已有审批中的申请，请先处理待审批、撤回或等待审批结束。',
        locked && (projection.lockReason || '当前对象已被审批中动作锁定。'),
        permissionDenied && (projection.disabledReason || projection.blockerReason || '当前账号没有该动作权限。'),
        backendDenied && (projection.disabledReason || projection.blockerReason),
        projection.disabledReason,
        projection.blockerReason,
        projection.reason,
        projection.message,
        `${label}当前不可操作。`
      )

  return {
    actionLabel: label,
    allowed,
    disabled: !allowed,
    pending,
    withdrawable: projection.withdrawable === true,
    approvable: projection.approvable === true,
    effectFailedPending,
    projectionMissing: false,
    blockerCode: allowed
      ? ''
      : projection.blockerCode ||
        (effectFailedPending
          ? 'EFFECT_FAILED_PENDING'
          : pending
            ? 'PENDING_APPROVAL_ACTION_LOCK'
            : locked
                ? 'PENDING_APPROVAL_ACTION_LOCK'
                : permissionDenied
                  ? 'ACTION_PERMISSION_DENIED'
                  : 'ACTION_PROJECTION_BLOCKED'),
    blockerMessage,
    projection
  }
}

export const assertProjectionAvailable = (
  projection?: ControlledActionProjectionVO | null,
  actionLabel?: string
) => {
  const state = resolveControlledActionProjection(projection, actionLabel)
  if (state.projectionMissing || !state.allowed) {
    const error = new Error(state.blockerMessage)
    Object.assign(error, {
      code: state.blockerCode,
      projectionState: state,
      projectionMissing: state.projectionMissing
    })
    throw error
  }
  return state
}

export const resolveProjectionErrorMessage = (
  error: unknown,
  fallbackActionLabel = DEFAULT_ACTION_LABEL
) => {
  const state = (error as { projectionState?: ControlledActionProjectionState })?.projectionState
  if (state?.blockerMessage) {
    return state.blockerMessage
  }

  const response = (error as any)?.response?.data
  return firstVisibleReason(
    response?.msg,
    response?.message,
    response?.code,
    (error as Error)?.message,
    `${fallbackActionLabel}处理失败，请查看后端返回错误。`
  )
}
