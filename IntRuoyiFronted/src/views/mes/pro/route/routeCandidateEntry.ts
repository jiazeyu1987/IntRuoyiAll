import { ProRouteApi, type ProRouteVO, type ProRouteVersionVO } from '@/api/mes/pro/route'

const DRAFT_ROUTE_VERSION_STATUS = 'DRAFT'
const PENDING_APPROVAL_ROUTE_VERSION_STATUS = 'PENDING_APPROVAL'
const READY_TO_PUBLISH_ROUTE_VERSION_STATUS = 'READY_TO_PUBLISH'
const OPEN_ROUTE_VERSION_STATUSES = [
  DRAFT_ROUTE_VERSION_STATUS,
  PENDING_APPROVAL_ROUTE_VERSION_STATUS,
  READY_TO_PUBLISH_ROUTE_VERSION_STATUS
]

type RouteCandidateConfirm = (message: string, title: string) => Promise<unknown> | unknown
type RouteCandidateSuccess = (message: string) => void
type RouteCandidateEditQuery = Record<string, string | string[] | undefined>

export type EnsureSameSourceDraftCandidateOptions = {
  routeId: number
  actionName: string
  changeReason: string
  confirm?: RouteCandidateConfirm
  success?: RouteCandidateSuccess
  existingConfirmMessage?: string
  existingConfirmTitle?: string
  createConfirmMessage?: string
  createConfirmTitle?: string
  existingSuccessMessage?: string
  createdSuccessMessage?: string
}

export type EnsureSameSourceDraftCandidateResult = {
  candidate: ProRouteVersionVO
  created: boolean
}

const formatRouteLabel = (routeInfo: ProRouteVO) =>
  routeInfo.name || routeInfo.code || (routeInfo.id ? String(routeInfo.id) : '未知路线')

export class RouteMultipleDraftCandidatesError extends Error {
  routeInfo: ProRouteVO
  draftCandidates: ProRouteVersionVO[]

  constructor(actionName: string, routeInfo: ProRouteVO, draftCandidates: ProRouteVersionVO[]) {
    super(
      `${actionName}失败：路线「${formatRouteLabel(routeInfo)}」存在多个草稿候选版本，请在候选版本工作区选择。`
    )
    this.name = 'RouteMultipleDraftCandidatesError'
    this.routeInfo = routeInfo
    this.draftCandidates = draftCandidates
  }
}

export const isRouteMultipleDraftCandidateError = (
  error: unknown
): error is RouteMultipleDraftCandidatesError =>
  error instanceof RouteMultipleDraftCandidatesError ||
  (error instanceof Error && error.name === 'RouteMultipleDraftCandidatesError')

export const isRouteCandidateConfirmCancel = (error: unknown) => {
  if (error === 'cancel' || error === 'close' || error === 'canceled') return true
  return error instanceof Error && /cancel|close/i.test(error.message)
}

const confirmRouteCandidateAction = async (
  confirm: RouteCandidateConfirm | undefined,
  message: string | undefined,
  title: string | undefined
) => {
  if (!confirm || !message) return true
  try {
    await confirm(message, title || '确认')
    return true
  } catch (error) {
    if (isRouteCandidateConfirmCancel(error)) return false
    throw error
  }
}

const requireRouteForCandidateEntry = (routeInfo: ProRouteVO, actionName: string) => {
  if (!routeInfo?.id) {
    throw new Error(`${actionName}失败：缺少路线编号，无法进入候选版本。`)
  }
  if (!routeInfo.activeRouteVersionId) {
    throw new Error(`${actionName}失败：当前路线缺少激活版本，无法进入候选版本。`)
  }
}

const requireDraftCandidateVersion = (
  candidate: ProRouteVersionVO | undefined,
  actionName: string
) => {
  if (!candidate?.id || !candidate.versionNo || candidate.lifecycleStatus !== DRAFT_ROUTE_VERSION_STATUS) {
    throw new Error(`${actionName}失败：候选版本未返回可编辑草稿版本。`)
  }
  return candidate
}

export const resolveSameSourceDraftCandidateForProductionConfig = (
  routeInfo: ProRouteVO,
  routeVersions: ProRouteVersionVO[],
  actionName: string
) => {
  requireRouteForCandidateEntry(routeInfo, actionName)
  if (!Array.isArray(routeVersions)) {
    throw new Error(`${actionName}失败：路线版本列表返回异常，无法判断候选版本。`)
  }
  const openCandidates = routeVersions.filter(
    (version) =>
      !version.active &&
      OPEN_ROUTE_VERSION_STATUSES.includes(String(version.lifecycleStatus))
  )
  const draftCandidates = openCandidates.filter(
    (version) => version.lifecycleStatus === DRAFT_ROUTE_VERSION_STATUS
  )
  if (draftCandidates.length > 1) {
    throw new RouteMultipleDraftCandidatesError(actionName, routeInfo, draftCandidates)
  }
  const blockingCandidate = openCandidates.find((version) =>
    [PENDING_APPROVAL_ROUTE_VERSION_STATUS, READY_TO_PUBLISH_ROUTE_VERSION_STATUS].includes(
      String(version.lifecycleStatus)
    )
  )
  if (blockingCandidate) {
    throw new Error(
      `${actionName}失败：当前路线版本 ${blockingCandidate.versionNo || blockingCandidate.id} ` +
        `处于${blockingCandidate.lifecycleStatus}，请先撤回、发布恢复或取消后再编辑。`
    )
  }
  const currentDraftCandidate = draftCandidates[0]
  if (
    currentDraftCandidate &&
    currentDraftCandidate.sourceRouteVersionId !== routeInfo.activeRouteVersionId
  ) {
    throw new Error(
      `${actionName}失败：草稿版本来源已不是当前生效版本，请取消后基于当前 ACTIVE 重新创建。`
    )
  }
  return currentDraftCandidate
}

export const ensureSameSourceDraftCandidateForProductionConfig = async (
  options: EnsureSameSourceDraftCandidateOptions
): Promise<EnsureSameSourceDraftCandidateResult | undefined> => {
  const { routeId, actionName } = options
  if (!Number.isFinite(routeId) || routeId <= 0) {
    throw new Error(`${actionName}失败：缺少有效路线编号，无法进入候选版本。`)
  }
  const [routeInfo, routeVersions] = await Promise.all([
    ProRouteApi.getRoute(routeId),
    ProRouteApi.getRouteVersionList(routeId)
  ])
  const existingDraftCandidate = resolveSameSourceDraftCandidateForProductionConfig(
    routeInfo,
    routeVersions,
    actionName
  )
  if (existingDraftCandidate) {
    const confirmed = await confirmRouteCandidateAction(
      options.confirm,
      options.existingConfirmMessage,
      options.existingConfirmTitle
    )
    if (!confirmed) return undefined
    if (options.success && options.existingSuccessMessage) {
      options.success(options.existingSuccessMessage)
    }
    return { candidate: requireDraftCandidateVersion(existingDraftCandidate, actionName), created: false }
  }
  const confirmed = await confirmRouteCandidateAction(
    options.confirm,
    options.createConfirmMessage,
    options.createConfirmTitle
  )
  if (!confirmed) return undefined
  const candidate = await ProRouteApi.createRouteCandidateVersion({
    routeId,
    sourceRouteVersionId: routeInfo.activeRouteVersionId,
    changeReason: options.changeReason
  })
  const draftCandidate = requireDraftCandidateVersion(candidate, actionName)
  if (options.success && options.createdSuccessMessage) {
    options.success(options.createdSuccessMessage)
  }
  return { candidate: draftCandidate, created: true }
}

const toRouteCandidateEditQueryValue = (value: unknown): string | string[] | undefined => {
  if (value === undefined || value === null || value === '') return undefined
  if (!Array.isArray(value)) return String(value)
  const normalized = value
    .filter((item) => item !== undefined && item !== null && item !== '')
    .map((item) => String(item))
  return normalized.length > 0 ? normalized : undefined
}

export const buildRouteCandidateEditQuery = (
  candidate: ProRouteVersionVO,
  extraQuery: Record<string, unknown> = {}
): RouteCandidateEditQuery => {
  const nextQuery: RouteCandidateEditQuery = {}
  Object.entries(extraQuery).forEach(([key, value]) => {
    const normalizedValue = toRouteCandidateEditQueryValue(value)
    if (normalizedValue !== undefined) {
      nextQuery[key] = normalizedValue
    }
  })
  return {
    ...nextQuery,
    routeVersionId: String(candidate.id),
    routeVersionNo: candidate.versionNo,
    routeVersionStatus: candidate.lifecycleStatus
  }
}
