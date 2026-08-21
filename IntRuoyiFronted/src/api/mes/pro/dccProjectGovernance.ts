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

export interface DccProjectGovernanceStatusOptions {
  routeStatusRequired?: boolean
  mainBatchRecordStatusRequired?: boolean
  formSlotStatusRequired?: boolean
}

export const getDccProjectGovernanceStatus = async (
  projectNames: string[],
  options: DccProjectGovernanceStatusOptions = {}
): Promise<DccProjectGovernanceStatusVO[]> => {
  const params = new URLSearchParams()
  projectNames.forEach((projectName) => params.append('projectNames', projectName))
  if (options.routeStatusRequired !== undefined) {
    params.append('routeStatusRequired', String(options.routeStatusRequired))
  }
  if (options.mainBatchRecordStatusRequired !== undefined) {
    params.append('mainBatchRecordStatusRequired', String(options.mainBatchRecordStatusRequired))
  }
  if (options.formSlotStatusRequired !== undefined) {
    params.append('formSlotStatusRequired', String(options.formSlotStatusRequired))
  }
  return await request.get<DccProjectGovernanceStatusVO[]>({
    url: `/mes/pro/dcc-project-governance/status?${params.toString()}`
  })
}
