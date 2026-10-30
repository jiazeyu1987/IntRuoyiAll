import type { ModuleRegistration } from '@/workbench/contracts'

const registration: ModuleRegistration = {
  ownerModule: 'bpm', contractVersion: 1,
  sources: [{
    descriptor: { sourceId: 'BPM_EDHR_VOID', ownerModule: 'bpm', contractVersion: 1, sortRank: 5,
      label: '批次作废审批', taskType: '批记录', policyId: 'bpm.edhrVoid.policy', actionKind: 'bpm.edhrVoid.open', payloadVersion: 1 },
    actionLabel: () => '查看任务',
    open: async (router, payload, isCurrent) => {
      const { taskId, processInstanceId } = payload
      if (!taskId || taskId.trim() !== taskId || taskId.length > 80 ||
          !processInstanceId || processInstanceId.trim() !== processInstanceId || processInstanceId.length > 128) {
        throw new Error('批次作废审批缺少正式流程任务身份。')
      }
      if (isCurrent && !isCurrent()) throw new Error('工作台导航上下文已变化。')
      return router.push({ path: '/bpm/process-instance/detail', query: { id: processInstanceId, taskId } })
    }
  }]
}
export default registration
