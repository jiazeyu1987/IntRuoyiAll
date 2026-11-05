import request from '@/config/axios'
import type { MesRouteId } from '@/api/mes/pro/route'

export interface SimpleParserDevice {
  deviceCode: string
  deviceName: string
  enabled: boolean
}

export interface SimpleParserRequest {
  requestId: string
  projectId: number
  workshopId: number
  routeName: string
  recognitionJson: Record<string, unknown>
  processSelections: Array<{
    processIndex: number
    processId?: number
    craftSelections: Array<{ groupIndex: number; craftId: number }>
  }>
  previewToken?: string
}

export interface SimpleParserRow {
  id?: number
  index?: number
  processIndex?: number
  groupIndex?: number
  code?: string
  name: string
  action: 'CREATE' | 'REUSE'
  parameterCount?: number
  machineryId?: number
  machineryAction?: 'CREATE' | 'REUSE'
  machineryTypeId?: number
  machineryTypeName?: string
}

export interface SimpleParserResult {
  requestId: string
  projectId: number
  projectCode: string
  projectName: string
  workshopId: number
  workshopName: string
  previewToken: string
  routeName: string
  processCount: number
  processRows: SimpleParserRow[]
  deviceRows: SimpleParserRow[]
  craftRows: SimpleParserRow[]
  route: { name: string; action: 'CREATE' }
  blockers: string[]
  warnings?: string[]
  unmatchedMaterials?: string[]
  materialRows?: Array<{
    processIndex: number
    direction: 'inputs' | 'outputs'
    index: number
    id: number
    code: string
    name: string
    sourceName?: string
  }>
  routeId?: MesRouteId
  routeCode?: string
  routeVersionId?: MesRouteId
  routeVersionNo?: string
  routeCandidateVersionId?: MesRouteId
  routeCandidateVersionNo?: string
}

const base = '/mes/pro/batch-record-report/simple-parser'
export const SimpleParserApi = {
  devices: () => request.get<SimpleParserDevice[]>({ url: `${base}/devices` }),
  preview: (data: SimpleParserRequest) =>
    request.post<SimpleParserResult>({ url: `${base}/preview`, data }),
  publish: (data: SimpleParserRequest) =>
    request.post<SimpleParserResult>({ url: `${base}/publish`, data })
}
