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

// Keep the encoded request target below the server's request-header limit, including
// room for the API prefix and authentication headers. Chinese names expand in URLs.
const MAX_STATUS_URL_LENGTH = 4000

const buildStatusUrl = (
  projectNames: string[],
  options: DccProjectGovernanceStatusOptions
): string => {
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
  return `/mes/pro/dcc-project-governance/status?${params.toString()}`
}

export const getDccProjectGovernanceStatus = async (
  projectNames: string[],
  options: DccProjectGovernanceStatusOptions = {}
): Promise<DccProjectGovernanceStatusVO[]> => {
  const batches: string[][] = []
  let batch: string[] = []
  for (const projectName of projectNames) {
    if (buildStatusUrl([projectName], options).length > MAX_STATUS_URL_LENGTH) {
      throw new Error('项目名称过长，无法查询治理状态')
    }
    if (buildStatusUrl([...batch, projectName], options).length > MAX_STATUS_URL_LENGTH) {
      batches.push(batch)
      batch = []
    }
    batch.push(projectName)
  }
  if (batch.length > 0) batches.push(batch)
  const results = await Promise.all(
    batches.map((names) =>
      request.get<DccProjectGovernanceStatusVO[]>({ url: buildStatusUrl(names, options) })
    )
  )
  return results.flat()
}
