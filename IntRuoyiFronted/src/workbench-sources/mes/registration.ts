import type { ModuleRegistration } from '@/workbench/contracts'
import { requireExactJavaLong as requireIdentity } from '@/utils/exactJavaLong'
import { resolveActiveOrderHandoffTarget, navigateToActiveOrderHandoff } from '@/utils/activeOrderHandoffNavigation'
import { navigateToEdhrWorkTask } from '@/utils/edhrWorkTaskNavigation'

const registration: ModuleRegistration = {
  ownerModule: 'mes', contractVersion: 1,
  sources: [
    {
      descriptor: { sourceId: 'EDHR_WORK_TASK', ownerModule: 'mes', contractVersion: 1, sortRank: 2,
        label: 'eDHR工作任务', taskType: '批记录', policyId: 'mes.edhr.policy', actionKind: 'mes.edhr.open', payloadVersion: 1 },
      open: async (router, payload, isCurrent) => {
        if (payload.blockedReason) throw new Error(payload.blockedReason)
        if (payload.taskType === 'QA_DECISION_HANDOFF' || payload.taskType === 'QA_REVIEW') {
          const id = payload.id?.match(/^handoff:([1-9][0-9]*)$/)?.[1]
          requireIdentity(id, '交接任务编号')
          const target = resolveActiveOrderHandoffTarget({ handoffTaskId: id, handoffType: payload.taskType,
            activeOrderId: payload.businessScopeId, actionUrl: payload.actionUrl })
          if (!target) throw new Error('QA任务缺少正式交接入口。')
          if (isCurrent && !isCurrent()) throw new Error('工作台导航上下文已变化。')
          return navigateToActiveOrderHandoff(router, target, isCurrent)
        }
        requireIdentity(payload.id, '工作任务编号')
        if (!payload.taskType) throw new Error('eDHR 工作任务缺少正式任务类型。')
        return navigateToEdhrWorkTask(router, payload, window.location.origin, isCurrent)
      }
    },
    {
      descriptor: { sourceId: 'WORK_ORDER', ownerModule: 'mes', contractVersion: 1, sortRank: 3,
        label: '待排产工单', taskType: '排产', policyId: 'mes.workOrder.policy', actionKind: 'mes.workOrder.open', payloadVersion: 1 },
      open: async (router, payload) => router.push({ path: '/mes/pro/work-order',
        query: payload.code ? { code: payload.code } : undefined })
    }
  ]
}
export default registration
