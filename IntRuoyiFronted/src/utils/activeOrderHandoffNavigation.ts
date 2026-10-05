import type { Router } from 'vue-router'
import { handoffNavigationContext } from '@/api/mes/pro/handoff'

const positiveId = (value: string | null): value is string => !!value && /^[1-9][0-9]*$/.test(value)
const paths: Record<string, readonly string[]> = {
  PRODUCTION_HANDOFF: ['/mes/pro/feedback/edhr-batch-production-fill'],
  PRODUCTION_RETURN: ['/mes/pro/feedback/edhr-batch-production-fill'],
  PQC_HANDOFF: ['/mes/pro/feedback/edhr-batch-pqc-fill'],
  PQC_RETURN: ['/mes/pro/feedback/edhr-batch-pqc-fill'],
  PRODUCTION_REVIEW: ['/mes/pro/process-pool/production-leader'],
  PQC_REVIEW: ['/mes/pro/process-pool/pqc-leader'],
  QA_REVIEW: ['/mes/pro/feedback/edhr-nonconformance-review'],
  QA_DECISION_HANDOFF: ['/mes/pro/process-pool/production-leader', '/user/profile']
}
export interface ActiveOrderHandoffTarget {
  type: 'activeOrderHandoff'; label: '处理当轮交接'; actionUrl: string; path: string; query: Record<string, string>
}
export const resolveActiveOrderHandoffTarget = (params: Record<string, unknown>): ActiveOrderHandoffTarget | null => {
  if (params.handoffTaskId === undefined) return null
  if (typeof params.actionUrl !== 'string' || typeof params.handoffType !== 'string') throw new Error('交接通知缺少正式入口或类型')
  const url = new URL(params.actionUrl, window.location.origin)
  const type = params.handoffType
  if (url.origin !== window.location.origin || url.hash || !paths[type]?.includes(url.pathname)) throw new Error('交接通知页面与类型不匹配')
  const sourceKey = type === 'PRODUCTION_HANDOFF' ? 'cycleId' : type === 'PQC_HANDOFF' ? 'pqcTaskId' : type.startsWith('QA_') ? 'reviewId' : 'eventId'
  const allowed = new Set(['activeOrderId', sourceKey, 'handoffTaskId', 'roundId', 'handoffType'])
  if (type === 'QA_DECISION_HANDOFF' && url.pathname === '/user/profile') allowed.add('tab')
  if (type.endsWith('_RETURN')) { allowed.add('returnTaskId'); allowed.add('rejectedReviewId') }
  const keys = Array.from(url.searchParams.keys())
  if (keys.length !== allowed.size || new Set(keys).size !== keys.length || keys.some(key => !allowed.has(key))) throw new Error('交接通知参数不匹配或重复')
  for (const key of allowed) if (!['handoffType', 'tab'].includes(key) && !positiveId(url.searchParams.get(key))) throw new Error('交接通知身份无效')
  if (url.searchParams.get('handoffType') !== type || url.searchParams.get('handoffTaskId') !== String(params.handoffTaskId)
    || url.searchParams.get('activeOrderId') !== String(params.activeOrderId)) throw new Error('交接通知与冻结周期不一致')
  if (type.endsWith('_RETURN') && (url.searchParams.get('returnTaskId') !== String(params.handoffTaskId)
    || url.searchParams.get('rejectedReviewId') !== url.searchParams.get('roundId'))) throw new Error('本人退回通知与原轮次不一致')
  if (url.pathname === '/user/profile' && url.searchParams.get('tab') !== 'notifyMessage') throw new Error('作废结果必须打开本人通知记录')
  return { type: 'activeOrderHandoff', label: '处理当轮交接', actionUrl: params.actionUrl, path: url.pathname, query: Object.fromEntries(url.searchParams.entries()) }
}
export const navigateToActiveOrderHandoff = async (router: Router, target: ActiveOrderHandoffTarget) => {
  const context = await handoffNavigationContext(target.query.handoffTaskId)
  if (!context || context.task.actionUrl !== target.actionUrl || String(context.task.id) !== target.query.handoffTaskId
    || String(context.task.activeOrderId) !== target.query.activeOrderId || String(context.task.roundId) !== target.query.roundId
    || context.task.taskType !== target.query.handoffType) throw new Error('交接通知与正式任务不一致，请刷新')
  if (context.task.status === 'CANCELED' || (!context.current && !(context.task.taskType === 'QA_DECISION_HANDOFF' && context.task.reason.startsWith('void：') && context.task.status === 'DONE')))
    throw new Error('旧周期交接已失效，禁止办理新周期')
  if (target.path === '/user/profile' && (context.task.status !== 'DONE' || !context.task.reason.startsWith('void：'))) throw new Error('作废交接结果未正式完成')
  await router.push({ path: target.path, query: { ...target.query, ...(context.processable ? {} : { handoffReadOnly: '1' }) } })
}
