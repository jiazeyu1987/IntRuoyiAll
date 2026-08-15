import request from '@/config/axios'

export type DccProjectGovernanceStatus = 'OK' | 'MISSING' | 'DUPLICATE'

export interface DccProjectGovernanceStatusVO {
  projectName: string
  dccProjectCodeCount?: number
  routeStatus?: DccProjectGovernanceStatus
  routeCount?: number
  routeCodes?: string[]
  routeVersionNos?: string[]
  mainBatchRecordStatus?: DccProjectGovernanceStatus
  mainBatchRecordCount?: number
  mainBatchRecordVersionNos?: string[]
  lossReportStatus?: DccProjectGovernanceStatus
  lossReportCount?: number
  lossReportCodes?: string[]
  lossReportVersionNos?: string[]
  processInspectionStatus?: DccProjectGovernanceStatus
  processInspectionCount?: number
  processInspectionCodes?: string[]
  processInspectionVersionNos?: string[]
  parameterRecordStatus?: DccProjectGovernanceStatus
  parameterRecordCount?: number
  parameterRecordCodes?: string[]
  parameterRecordVersionNos?: string[]
  blockerMessages?: string[]
}

export const getDccProjectGovernanceStatus = async (
  projectNames: string[]
): Promise<DccProjectGovernanceStatusVO[]> => {
  const params = new URLSearchParams()
  projectNames.forEach((projectName) => params.append('projectNames', projectName))
  return await request.get<DccProjectGovernanceStatusVO[]>({
    url: `/mes/pro/dcc-project-governance/status?${params.toString()}`
  })
}
