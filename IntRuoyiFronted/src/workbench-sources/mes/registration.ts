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
      actionLabel: (payload) => {
        const labels: Record<string, string> = {
          FILL: '去填写', REVIEW: '去复核', APPROVE: '去批准', REWORK: '去修改',
          ARCHIVE: '去归档', CLOSE: '去关闭', RELEASE_APPROVE: '去审批',
          PQC_PRODUCTION_RELEASE: '去放行', DOSSIER_COMPLETE: '去补齐',
          PQC_RELEASE_CORRECTION: '去补正', RECORD_CHANGE_CORRECTION: '去补正',
          RECORD_CHANGE_APPROVE: '去审批', DEVIATION_RECEIVE: '去调查',
          DEVIATION_CORRECT: '去纠正', DEVIATION_SIGN: '去签署',
          DEVIATION_APPROVE: '去审核', DEVIATION_CLOSE: '去关闭',
          QA_REVIEW: '去评审', QA_DECISION_HANDOFF: '去接手'
        }
        return labels[payload.taskType] ?? '查看任务'
      },
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
    }
  ]
}
export default registration
