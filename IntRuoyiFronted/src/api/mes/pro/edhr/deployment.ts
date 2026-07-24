import request from '@/config/axios'

const EDHR_DEPLOYMENT_PAGE_URL = '/mes/pro/edhr-deployment/page'
const EDHR_DEPLOYMENT_CREATE_URL = '/mes/pro/edhr-deployment/create'
const EDHR_DEPLOYMENT_DETAIL_URL = '/mes/pro/edhr-deployment/detail'
const EDHR_DEPLOYMENT_UPDATE_EVIDENCE_URL = '/mes/pro/edhr-deployment/update-evidence'
const EDHR_DEPLOYMENT_PRECHECK_URL = '/mes/pro/edhr-deployment/precheck'

export const EDHR_DEPLOYMENT_QUERY_PERMISSION = 'mes:pro-edhr-deployment:query'
export const EDHR_DEPLOYMENT_CREATE_PERMISSION = 'mes:pro-edhr-deployment:create'
export const EDHR_DEPLOYMENT_UPDATE_PERMISSION = 'mes:pro-edhr-deployment:update'
export const EDHR_DEPLOYMENT_PRECHECK_PERMISSION = 'mes:pro-edhr-deployment:precheck'

export type EdhrDeploymentStatus =
  | 'DELIVERY_DRAFT'
  | 'ENVIRONMENT_CHECKED'
  | 'INSTALLED'
  | 'INTEGRATED'
  | 'DELIVERY_BLOCKED'
  | string
export type EdhrDeploymentGateStatus = 'PASSED' | 'BLOCKED' | string

export interface EdhrDeploymentCreateReqVO {
  projectId: number
  deploymentName: string
  customerProjectName: string
  targetEnvironment: string
  environmentAuthorized?: boolean
  environmentCheckSummary?: string
  serverSummary?: string
  networkSummary?: string
  objectStorageSummary?: string
  capacitySummary?: string
  permissionSummary?: string
  releaseTag: string
  artifactVersion?: string
  artifactChecksum?: string
  schemaVersion: string
  migrationManifest?: string
  requiredSqlManifest?: string
  appImportResult?: string
  remark?: string
}

export interface EdhrDeploymentPageReqVO extends PageParam {
  projectId?: number
  deploymentCode?: string
  deploymentName?: string
  deploymentStatus?: EdhrDeploymentStatus
  releaseTag?: string
  targetEnvironment?: string
}

export interface EdhrDeploymentUpdateReqVO {
  deploymentId: number
  targetEnvironment?: string
  environmentAuthorized?: boolean
  environmentCheckSummary?: string
  serverSummary?: string
  networkSummary?: string
  objectStorageSummary?: string
  capacitySummary?: string
  permissionSummary?: string
  releaseTag?: string
  artifactVersion?: string
  artifactChecksum?: string
  schemaVersion?: string
  migrationManifest?: string
  requiredSqlManifest?: string
  appImportResult?: string
  licenseScope?: string
  licenseValidUntil?: string
  licenseFileEvidence?: string
  licenseCheckResult?: string
  customerLicenseConfirmation?: string
  interfaceScope?: string
  interfaceVersion?: string
  integrationEnvironment?: string
  requestEvidence?: string
  responseEvidence?: string
  interfaceFailureCount?: number
  remediationAction?: string
  retestEvidence?: string
  interfaceConfirmedBy?: string
}

export interface EdhrDeploymentGateItemRespVO {
  id: number
  deploymentId: number
  gateCode: string
  gateName: string
  gateStatus: EdhrDeploymentGateStatus
  evidenceSource: string
  missingEvidence: string
  ownerName: string
  nextAction: string
  signoffImpact: string
}

export interface EdhrDeploymentRespVO {
  id: number
  projectId: number
  deploymentCode: string
  deploymentName: string
  customerProjectName: string
  targetEnvironment: string
  environmentAuthorized: boolean
  environmentCheckSummary?: string
  serverSummary?: string
  networkSummary?: string
  objectStorageSummary?: string
  capacitySummary?: string
  permissionSummary?: string
  releaseTag: string
  artifactVersion?: string
  artifactChecksum?: string
  schemaVersion: string
  migrationManifest?: string
  requiredSqlManifest?: string
  appImportResult?: string
  licenseScope?: string
  licenseValidUntil?: string
  licenseFileEvidence?: string
  licenseCheckResult?: string
  customerLicenseConfirmation?: string
  interfaceScope?: string
  interfaceVersion?: string
  integrationEnvironment?: string
  requestEvidence?: string
  responseEvidence?: string
  interfaceFailureCount?: number
  remediationAction?: string
  retestEvidence?: string
  interfaceConfirmedBy?: string
  deploymentStatus: EdhrDeploymentStatus
  blockedReason: string
  nextAction: string
  gatePassed: boolean
  gateCheckedAt?: string
  evidenceSnapshotChecksum?: string
  gateItems: EdhrDeploymentGateItemRespVO[]
  remark?: string
  createTime?: string
}

export interface EdhrDeploymentPrecheckRespVO {
  deploymentId: number
  deploymentCode: string
  deploymentStatus: EdhrDeploymentStatus
  gatePassed: boolean
  blockedReason: string
  nextAction: string
  gateCheckedAt?: string
  evidenceSnapshotChecksum?: string
  gateItems: EdhrDeploymentGateItemRespVO[]
}

export const getEdhrDeploymentPage = async (params: EdhrDeploymentPageReqVO) => {
  return await request.get<PageResult<EdhrDeploymentRespVO[]>>({
    url: EDHR_DEPLOYMENT_PAGE_URL,
    params
  })
}

export const createEdhrDeploymentEvidence = async (data: EdhrDeploymentCreateReqVO) => {
  return await request.post<EdhrDeploymentRespVO>({
    url: EDHR_DEPLOYMENT_CREATE_URL,
    data
  })
}

export const getEdhrDeploymentDetail = async (id: number) => {
  return await request.get<EdhrDeploymentRespVO>({
    url: EDHR_DEPLOYMENT_DETAIL_URL,
    params: { id }
  })
}

export const updateEdhrDeploymentEvidence = async (data: EdhrDeploymentUpdateReqVO) => {
  return await request.post<EdhrDeploymentRespVO>({
    url: EDHR_DEPLOYMENT_UPDATE_EVIDENCE_URL,
    data
  })
}

export const precheckEdhrDeploymentEvidence = async (deploymentId: number) => {
  return await request.post<EdhrDeploymentPrecheckRespVO>({
    url: EDHR_DEPLOYMENT_PRECHECK_URL,
    params: { deploymentId }
  })
}
