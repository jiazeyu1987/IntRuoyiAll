import type { ModuleRegistration } from '@/workbench/contracts'
import { requireExactJavaLong as requireIdentity } from '@/utils/exactJavaLong'

const registration: ModuleRegistration = {
  ownerModule: 'showroom', contractVersion: 1,
  sources: [{
    descriptor: { sourceId: 'SHOWROOM_ASSIGNMENT', ownerModule: 'showroom', contractVersion: 1, sortRank: 4,
      label: '展厅补充指派', taskType: '展厅', policyId: 'showroom.assignment.policy', actionKind: 'showroom.assignment.open', payloadVersion: 1 },
    actionLabel: () => '查看任务',
    open: async (router, payload) => router.push({ name: 'ShowroomAdminAssignment',
      query: { assignmentId: requireIdentity(payload.assignmentId, '指派编号') } })
  }]
}
export default registration
