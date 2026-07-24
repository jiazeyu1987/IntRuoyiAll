import request from '@/config/axios'
import type {
  SignatureGovernanceBlocker,
  SignatureGovernanceModuleCode
} from './shared'
import type { SignatureGovernancePolicyModuleRespVO } from './policy'

export type SignatureGovernancePortalStatus = 'READY' | 'BLOCKED'
export type SignatureGovernancePortalAuthorizationStatus =
  | 'ENABLED'
  | 'DISABLED'
  | 'LOCKED'
  | 'UNAUTHORIZED'

export interface SignatureGovernancePortalAuthorizationRespVO {
  status: SignatureGovernancePortalAuthorizationStatus
  enabled: boolean
  blockers: SignatureGovernanceBlocker[]
}

export interface SignatureGovernancePortalSummaryRespVO {
  moduleTotal: number
  readyModuleTotal: number
  blockedModuleTotal: number
  pendingTotal: number
  signatureTotal: number
}

export interface SignatureGovernancePortalMetricsRespVO {
  pendingCount: number
  signatureCount: number
}

export interface SignatureGovernancePortalRouteRespVO {
  primaryLabel: string
  primaryPath: string
  secondaryLabel?: string | null
  secondaryPath?: string | null
}

export interface SignatureGovernancePortalModuleRespVO {
  moduleCode: SignatureGovernanceModuleCode
  moduleName: string
  moduleDescription: string
  status: SignatureGovernancePortalStatus
  ready: boolean
  authorization: SignatureGovernancePortalAuthorizationRespVO
  policy: SignatureGovernancePolicyModuleRespVO
  metrics: SignatureGovernancePortalMetricsRespVO
  routes: SignatureGovernancePortalRouteRespVO
  blockers: SignatureGovernanceBlocker[]
}

export interface SignatureGovernancePortalOverviewRespVO {
  status: SignatureGovernancePortalStatus
  ready: boolean
  authorization: SignatureGovernancePortalAuthorizationRespVO
  summary: SignatureGovernancePortalSummaryRespVO
  modules: SignatureGovernancePortalModuleRespVO[]
  blockers: SignatureGovernanceBlocker[]
}

export const getSignatureGovernancePortalOverview =
  (): Promise<SignatureGovernancePortalOverviewRespVO> => {
    return request.get({
      url: '/signature-governance/portal/overview'
    })
  }
