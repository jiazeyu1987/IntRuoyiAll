import request from '@/config/axios'

export interface CraftParameter {
  parameterCode: string
  parameterName: string
  valueType: string
  unit?: string | null
  standardText: string
  lowerLimit?: number | null
  upperLimit?: number | null
  defaultValue?: number | null
  optionValuesJson?: string | null
  defaultText?: string | null
  decimalScale?: number | null
  sort?: number | null
}
export interface Craft {
  id: number
  code: string
  name: string
  processId: number
  parametersJson: string
  fingerprint: string
}
export interface CraftSave {
  id?: number
  name: string
  processId?: number
  parameters: CraftParameter[]
  expectedFingerprint?: string
  impactToken?: string
  reason: string
}
export interface CraftImpact {
  impactToken: string
  blockers: string[]
  routes?: Array<{ routeId: number; routeName: string; routeCode: string; versionNo: string }>
  sources?: Array<{
    routeId: number
    routeName: string
    routeProcessId: number
    processName: string
    parameters: CraftParameter[]
  }>
  before?: CraftParameter[]
  after?: CraftParameter[]
  beforeName?: string
  afterName?: string
  emptyNodeCount?: number
}
export interface CraftChange {
  id: number
  craftId?: number
  changeType: string
  status: string
  reason: string
  beforeJson?: string
  afterJson?: string
  candidateIdsJson: string
  createTime: string
}
export const CraftApi = {
  list: (processId?: number): Promise<Craft[]> =>
    request.get({ url: '/mes/pro/craft/list', params: { processId } }),
  create: (data: CraftSave): Promise<Craft> => request.post({ url: '/mes/pro/craft/create', data }),
  preview: (data: CraftSave): Promise<CraftImpact> =>
    request.post({ url: '/mes/pro/craft/preview', data }),
  updateAll: (data: CraftSave): Promise<CraftChange> =>
    request.post({ url: '/mes/pro/craft/update-all', data }),
  initializationPreview: (): Promise<CraftImpact> =>
    request.get({ url: '/mes/pro/craft/initialization-preview' }),
  initialize: (impactToken: string): Promise<CraftChange> =>
    request.post({ url: '/mes/pro/craft/initialize', params: { impactToken } }),
  changes: (): Promise<CraftChange[]> => request.get({ url: '/mes/pro/craft/changes' }),
  cancelChange: (id: number) =>
    request.post({ url: '/mes/pro/craft/cancel-change', params: { id } }),
  bind: (data: {
    routeVersionId: number | string
    routeProcessId: number
    craftId: number
    expectedRouteSnapshotSha256: string
  }) => request.post({ url: '/mes/pro/craft/bind', data })
}
