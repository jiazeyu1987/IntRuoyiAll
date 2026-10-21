import type { ModuleRegistration } from '@/workbench/contracts'
import { requireExactJavaLong as requireIdentity } from '@/utils/exactJavaLong'

const registration: ModuleRegistration = {
  ownerModule: 'dcc', contractVersion: 1,
  sources: [
    {
      descriptor: { sourceId: 'DCC_DISTRIBUTION', ownerModule: 'dcc', contractVersion: 1, sortRank: 0,
        label: '文控分发', taskType: '文控', policyId: 'dcc.distribution.policy', actionKind: 'dcc.distribution.open', payloadVersion: 1 },
      open: async (router, payload) => router.push({ name: 'DccControlledFileDetail',
        params: { id: requireIdentity(payload.controlledFileId, '受控文件编号') },
        query: { from: 'profile-distribution',
          distributionId: requireIdentity(payload.distributionId, '分发编号'),
          recipientId: requireIdentity(payload.recipientId, '收件人编号') } })
    },
    {
      descriptor: { sourceId: 'DCC_TRAINING', ownerModule: 'dcc', contractVersion: 1, sortRank: 1,
        label: '文控培训', taskType: '文控', policyId: 'dcc.training.policy', actionKind: 'dcc.training.open', payloadVersion: 1 },
      open: async (router, payload) => {
        requireIdentity(payload.controlledFileId, '受控文件编号')
        return router.push({ name: 'DccTrainingTask', params: { progressId: requireIdentity(payload.progressId, '培训进度编号') } })
      }
    }
  ]
}
export default registration
