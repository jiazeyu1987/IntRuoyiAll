import request from '@/config/axios'
import type { SignatureGovernanceBlocker, SignatureGovernanceModuleCode } from './shared'

export type SignatureGovernancePolicyCurrentStatus = 'READY' | 'BLOCKED'

export interface SignatureGovernancePolicyModuleRespVO {
  moduleCode: SignatureGovernanceModuleCode
  policySourcePresent: boolean
  authorityConfirmed: boolean
  adapterRegistered: boolean
  policyVersion?: string | null
  policySourceCode?: string | null
  adapterCode?: string | null
  adapterVersion?: string | null
  evidenceSchemaVersion?: string | null
  blockers: SignatureGovernanceBlocker[]
}

export interface SignatureGovernancePolicyCurrentRespVO {
  status: SignatureGovernancePolicyCurrentStatus
  ready: boolean
  modules: SignatureGovernanceModuleCode[]
  moduleStatuses: SignatureGovernancePolicyModuleRespVO[]
  blockers: SignatureGovernanceBlocker[]
}

export const getCurrentSignatureGovernancePolicy =
  (): Promise<SignatureGovernancePolicyCurrentRespVO> => {
    return request.get({
      url: '/signature-governance/policies/current'
    })
  }
