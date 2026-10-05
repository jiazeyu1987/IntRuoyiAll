import type { Router } from 'vue-router'
import type { NotifyMessageVO } from '@/api/system/notify/message'
import { DCC_OFFLINE_TRAINING_RECORD, readOfflineTrainingManagementLocation, type OfflineTrainingRecordLocation } from './dccOfflineTrainingRecord'
import {
  EDHR_WORK_TASK_NOTIFY_PATHS,
  navigateToEdhrWorkTask
} from '@/utils/edhrWorkTaskNavigation'

export const SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY = 'showroomNotifyProductTarget'
export const BPM_PROCESS_DETAIL_PATH = '/bpm/process-instance/detail'
export const DCC_CONTROLLED_FILE_DETAIL_PATH_PATTERN = /^\/dcc\/controlled-file\/detail\/(\d+)$/

export const NOTIFY_MESSAGE_NAVIGATION_PARAM_KEYS = new Set([
  'detailUrl',
  'actionUrl',
  'notifyTargetType',
  'targetType',
  'notifyTargetId',
  'targetId',
  'notifyChangeRequestId',
  'changeRequestId',
  'notifyOpen',
  'workTaskId',
  'followupUrl',
  'notifyProcessInstanceId'
])

type NotifyMessageLike = Pick<NotifyMessageVO, 'templateParams'> & Partial<Pick<NotifyMessageVO, 'templateCode'>>

export type ShowroomProductNotifyTarget = {
  type: 'showroomProduct'
  label: '查看关联产品'
  targetId: number
  changeRequestId: number | null
  notifyOpen: 'approval' | 'edit' | 'detail'
}

export type BpmApprovalNotifyTarget = {
  type: 'bpmApproval'
  label: '去审批'
  query: Record<string, string>
}

export type EdhrWorkTaskNotifyTarget = {
  type: 'edhrWorkTask'
  label: '处理批记录任务'
  actionUrl: string
  path: string
  query: Record<string, string>
}

export type DccPublicationNotifyTarget = {
  type: 'dccPublication'
  label: '查看发布文件'
  targetId: string
}

export type DccProjectProductNotifyTarget = {
  type: 'dccProjectProduct'
  label: '查看项目及产品申请'
  targetId: string
  query: { requestId: string; requestOpen: 'records'; from: 'notification' }
}
export type DccOfflineTrainingNotifyTarget = {
  type: 'dccOfflineTraining'
  label: '上传线下培训记录'
  location: OfflineTrainingRecordLocation
}
export type DccRelationRemediationNotifyTarget = {
  type: 'dccRelationRemediation'
  label: '查看来源受控申请'
  targetId: string
  relatedMasterId: string
  dueAt: string
}

export type NotifyMessageTarget =
  | ShowroomProductNotifyTarget
  | BpmApprovalNotifyTarget
  | EdhrWorkTaskNotifyTarget
  | DccPublicationNotifyTarget
  | DccProjectProductNotifyTarget
  | DccOfflineTrainingNotifyTarget
  | DccRelationRemediationNotifyTarget

const normalizeTemplateParams = (value: unknown) => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null
  }
  return value as Record<string, unknown>
}

const toOptionalRouteNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const resolveShowroomProductTarget = (
  templateParams: Record<string, unknown>
): ShowroomProductNotifyTarget | null => {
  if (String(templateParams.notifyTargetType || templateParams.targetType || '').toUpperCase() !== 'PRODUCT') {
    return null
  }
  const targetId = toOptionalRouteNumber(templateParams.notifyTargetId ?? templateParams.targetId)
  if (!targetId) {
    return null
  }
  const changeRequestId = toOptionalRouteNumber(
    templateParams.notifyChangeRequestId ?? templateParams.changeRequestId
  )
  const rawNotifyOpen = String(templateParams.notifyOpen || '').toLowerCase()
  return {
    type: 'showroomProduct',
    label: '查看关联产品',
    targetId,
    changeRequestId,
    notifyOpen: rawNotifyOpen === 'approval' ? 'approval' : rawNotifyOpen === 'edit' ? 'edit' : 'detail'
  }
}

const resolveBpmApprovalTarget = (
  templateParams: Record<string, unknown>
): BpmApprovalNotifyTarget | null => {
  if (typeof templateParams.detailUrl !== 'string') {
    return null
  }
  let url: URL
  try {
    url = new URL(templateParams.detailUrl, window.location.origin)
  } catch (error) {
    console.warn('BPM审批站内信 detailUrl 解析失败', error)
    return null
  }
  if (url.pathname !== BPM_PROCESS_DETAIL_PATH) {
    return null
  }
  return {
    type: 'bpmApproval',
    label: '去审批',
    query: Object.fromEntries(url.searchParams.entries())
  }
}

const resolveEdhrWorkTaskTarget = (
  templateParams: Record<string, unknown>
): EdhrWorkTaskNotifyTarget | null => {
  if (typeof templateParams.actionUrl !== 'string') {
    return null
  }
  let url: URL
  try {
    url = new URL(templateParams.actionUrl, window.location.origin)
  } catch (error) {
    console.warn('eDHR工作任务站内信 actionUrl 解析失败', error)
    return null
  }
  if (url.origin !== window.location.origin) {
    return null
  }
  if (!EDHR_WORK_TASK_NOTIFY_PATHS.has(url.pathname)) {
    return null
  }
  const workTaskId = toOptionalRouteNumber(templateParams.workTaskId)
  if (workTaskId) {
    url.searchParams.set('workTaskId', String(workTaskId))
  }
  return {
    type: 'edhrWorkTask',
    label: '处理批记录任务',
    actionUrl: `${url.pathname}${url.search}${url.hash}`,
    path: url.pathname,
    query: Object.fromEntries(url.searchParams.entries())
  }
}

const resolveDccPublicationTarget = (
  templateParams: Record<string, unknown>
): DccPublicationNotifyTarget | null => {
  if (typeof templateParams.followupUrl !== 'string') {
    return null
  }
  let url: URL
  try {
    url = new URL(templateParams.followupUrl, window.location.origin)
  } catch (error) {
    console.warn('DCC发布站内信 followupUrl 解析失败', error)
    return null
  }
  const match = DCC_CONTROLLED_FILE_DETAIL_PATH_PATTERN.exec(url.pathname)
  const allowedKeys = new Set(['viewer', 'from'])
  if (
    url.origin !== window.location.origin ||
    !match ||
    url.searchParams.get('viewer') !== '1' ||
    url.searchParams.get('from') !== 'notification' ||
    Array.from(url.searchParams.keys()).some((key) => !allowedKeys.has(key))
  ) {
    return null
  }
  return { type: 'dccPublication', label: '查看发布文件', targetId: match[1] }
}

const resolveDccProjectProductTarget = (
  templateParams: Record<string, unknown>
): DccProjectProductNotifyTarget | null => {
  if (templateParams.notifyTargetType !== 'DCC_PROJECT_PRODUCT_REQUEST'
    || typeof templateParams.notifyTargetId !== 'string'
    || !/^[1-9][0-9]*$/.test(templateParams.notifyTargetId)
    || BigInt(templateParams.notifyTargetId) > 9223372036854775807n
    || typeof templateParams.actionUrl !== 'string') return null
  let url: URL
  try { url = new URL(templateParams.actionUrl, window.location.origin) }
  catch { return null }
  const allowedKeys = new Set(['requestId', 'requestOpen', 'from'])
  if (url.origin !== window.location.origin || url.pathname !== '/mdm/product-catalog'
    || url.hash || url.username || url.password
    || Array.from(url.searchParams.keys()).some(key => !allowedKeys.has(key))
    || Array.from(allowedKeys).some(key => url.searchParams.getAll(key).length !== 1)
    || url.searchParams.get('requestId') !== templateParams.notifyTargetId
    || url.searchParams.get('requestOpen') !== 'records'
    || url.searchParams.get('from') !== 'notification') return null
  return {
    type: 'dccProjectProduct', label: '查看项目及产品申请', targetId: templateParams.notifyTargetId,
    query: { requestId: templateParams.notifyTargetId, requestOpen: 'records', from: 'notification' }
  }
}

const resolveDccRelationRemediationTarget = (params: Record<string, unknown>): DccRelationRemediationNotifyTarget | null => {
  const identity = (value: unknown): value is string => typeof value === 'string' && /^[1-9]\d*$/.test(value)
    && BigInt(value) <= 9223372036854775807n
  if (!identity(params.sourceControlledFileId) || !identity(params.relatedMasterId)
    || typeof params.fileNumber !== 'string' || !params.fileNumber.trim()
    || typeof params.versionNo !== 'string' || !params.versionNo.trim()
    || typeof params.dueAt !== 'string' || typeof params.detailUrl !== 'string') return null
  const dateParts = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.exec(params.dueAt)
  if (!dateParts) return null
  const [year, month, day, hour, minute, second] = dateParts.slice(1).map(Number)
  if (year < 1 || month < 1 || month > 12 || day < 1 || hour > 23 || minute > 59 || second > 59) return null
  const date = new Date(0); date.setUTCFullYear(year, month - 1, day); date.setUTCHours(hour, minute, second, 0)
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null
  let url: URL
  try { url = new URL(params.detailUrl, window.location.origin) } catch { return null }
  if (url.origin !== window.location.origin || url.username || url.password || url.hash
    || url.pathname !== `/dcc/controlled-file/detail/${params.sourceControlledFileId}`
    || Array.from(url.searchParams.keys()).some(key => !['viewer', 'from'].includes(key))
    || url.searchParams.getAll('viewer').length !== 1 || url.searchParams.get('viewer') !== '1'
    || url.searchParams.getAll('from').length !== 1 || url.searchParams.get('from') !== 'notification') return null
  return { type: 'dccRelationRemediation', label: '查看来源受控申请', targetId: params.sourceControlledFileId,
    relatedMasterId: params.relatedMasterId, dueAt: params.dueAt }
}
export const getNotifyMessageTargets = (message?: NotifyMessageLike | null): NotifyMessageTarget[] => {
  const templateParams = normalizeTemplateParams(message?.templateParams)
  if (!templateParams) {
    return []
  }
  if (message?.templateCode === 'dcc_relation_remediation') {
    const target = resolveDccRelationRemediationTarget(templateParams)
    return target ? [target] : []
  }
  if (templateParams.notifyTargetType === DCC_OFFLINE_TRAINING_RECORD) {
    try {
      const urls = [templateParams.detailUrl, templateParams.actionUrl]
      if (urls.some(value => typeof value !== 'string')) return []
      const locations = urls.map(value => {
        const url = new URL(value as string, window.location.origin)
        if (url.origin !== window.location.origin || url.hash || url.username || url.password
          || Array.from(url.searchParams.keys()).some(key => url.searchParams.getAll(key).length !== 1)) throw new Error('培训通知入口无效')
        return readOfflineTrainingManagementLocation(templateParams.notifyTargetId, templateParams.notifyProcessInstanceId, url.pathname, Object.fromEntries(url.searchParams))
      })
      if (JSON.stringify(locations[0]) !== JSON.stringify(locations[1])) return []
      return [{ type: 'dccOfflineTraining', label: '上传线下培训记录', location: locations[0] }]
    } catch { return [] }
  }
  if (templateParams.notifyTargetType === 'DCC_PROJECT_PRODUCT_REQUEST') {
    const target = resolveDccProjectProductTarget(templateParams)
    return target ? [target] : []
  }
  const targets: Array<NotifyMessageTarget | null> = [
    resolveDccPublicationTarget(templateParams), resolveShowroomProductTarget(templateParams),
    resolveBpmApprovalTarget(templateParams), resolveEdhrWorkTaskTarget(templateParams)
  ]
  return targets.filter(
    (target): target is NotifyMessageTarget => Boolean(target)
  )
}

export const getNotifyMessageTarget = (message?: NotifyMessageLike | null) =>
  getNotifyMessageTargets(message)[0] ?? null

export const hasNotifyMessageTarget = (message?: NotifyMessageLike | null) =>
  Boolean(getNotifyMessageTarget(message))

export const navigateToNotifyMessageTarget = async (
  router: Router,
  target: NotifyMessageTarget,
  options: {
    beforeNavigate?: () => void | Promise<void>
    delayMs?: number
  } = {}
) => {
  await options.beforeNavigate?.()
  if (target.type === 'showroomProduct') {
    sessionStorage.setItem(
      SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY,
      JSON.stringify({
        targetId: target.targetId,
        changeRequestId: target.changeRequestId,
        notifyOpen: target.notifyOpen
      })
    )
    if (options.delayMs) {
      await new Promise((resolve) => window.setTimeout(resolve, options.delayMs))
    }
    const targetRoute = router.resolve({
      name: 'ShowroomAdminProduct',
      query: {
        notifyTargetType: 'PRODUCT',
        notifyTargetId: String(target.targetId),
        notifyChangeRequestId: target.changeRequestId ? String(target.changeRequestId) : undefined,
        notifyOpen: target.notifyOpen
      }
    })
    window.location.assign(targetRoute.href)
    return
  }
  if (target.type === 'edhrWorkTask') {
    await navigateToEdhrWorkTask(router, {
      id: target.query.workTaskId,
      actionUrl: target.actionUrl,
      batchExecutionId: target.query.batchExecutionId,
      batchTaskId: target.query.batchTaskId,
      executionId: target.query.executionId
    })
    return
  }
  if (target.type === 'dccPublication' || target.type === 'dccRelationRemediation') {
    await router.push({
      name: 'DccControlledFileDetail',
      params: { id: target.targetId },
      query: { viewer: '1', from: 'notification' }
    })
    return
  }
  if (target.type === 'dccProjectProduct') {
    await router.push({ path: '/mdm/product-catalog', query: target.query })
    return
  }
  if (target.type === 'dccOfflineTraining') {
    await router.push(target.location)
    return
  }
  await router.push({
    path: BPM_PROCESS_DETAIL_PATH,
    query: target.query
  })
}
