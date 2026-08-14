import request from '@/config/axios'

export type MesRouteId = number | string

// MES 工艺路线 VO
export interface ProRouteVO {
  id?: number // 编号
  code: string // 工艺路线编码
  name: string // 工艺路线名称
  description?: string // 工艺路线说明
  ownerName?: string // 负责人
  keyProcessName?: string // 关键工序
  status?: number // 状态
  remark?: string // 备注
  lastProcessName?: string // 末道工序
  productCodes?: string // 关联产品编号
  flowGraphConfigured?: boolean // 关系图是否已设置
  activeRouteVersionId?: number // 当前激活路线版本编号
  activeRouteVersionNo?: string // 当前激活路线版本号
  pendingRouteVersionId?: number // 待发布路线版本编号
  pendingRouteVersionNo?: string // 待发布路线版本号
  pendingRouteVersionStatus?: ProRouteVersionLifecycleStatus // 待发布路线版本状态
  pendingRouteVersionCount?: number // 待发布候选版本数量
  scheduleRouteEnabled?: boolean // 工艺流程排产配置是否启用
  batchRouteEnabled?: boolean // 工艺流程批记录配置是否启用
  createTime?: Date // 创建时间
}

export interface ProRouteResourceCapacityBlockingIssueVO {
  code: string
  message?: string
  routeProcessId?: number
  workstationId?: number
  workstationCode?: string
  machineryId?: number
  machineryCode?: string
}

export interface ProRouteResourceCapacityMachineRowVO {
  workstationMachineId?: number
  machineryId?: number
  machineryCode?: string
  machineryName?: string
  quantity?: number
  standardHourlyCapacity?: number
  hourlyCapacity?: number
}

export interface ProRouteResourceCapacityWorkstationRowVO {
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  productionLineId?: number
  productionLineName?: string
  shiftHours?: number
  resourceType?: 'MACHINE' | 'WORKER' | 'UNCONFIGURED' | string
  hourlyCapacity?: number
  workerQuantity?: number
  singleStandardHourlyCapacity?: number
  machineRows?: ProRouteResourceCapacityMachineRowVO[]
  blockingIssues?: ProRouteResourceCapacityBlockingIssueVO[]
}

export interface ProRouteResourceCapacityPreviewVO {
  routeProcessId: number
  processId?: number
  resourceCapacityHourly?: number
  capacitySource?: 'MACHINE' | 'WORKER' | 'UNCONFIGURED' | string
  workstationRows: ProRouteResourceCapacityWorkstationRowVO[]
  blockingIssues: ProRouteResourceCapacityBlockingIssueVO[]
}
export interface ProRouteCopyReqVO {
  sourceRouteId: number
  targetCode: string
  targetName: string
}

export interface RouteDccProjectBindingVO {
  routeId: number
  dccProjectCodeId?: number | null
  version: number
  bound: boolean
}

export interface RouteDccProjectBindingSaveReqVO {
  routeId: number
  dccProjectCodeId: number
  expectedVersion?: number
}

export interface ProRouteScheduleConfigVO {
  id?: number
  routeVersionId: MesRouteId
  routeProcessId: number
  capacityMode: 'RESOURCE_CALCULATED' | 'MANUAL_OVERRIDE' | 'FINITE_HOURLY' | 'INFINITE_FORMULA'
  hourlyCapacity?: number
  shiftHours?: number
  standardShiftCapacity?: number
  infiniteDurationQuantityFactor?: number
  infiniteDurationBaseMinutes?: number
  nightShiftEnabled?: boolean
  calendarRuleId?: number
  configVersion?: string
  remark?: string
}

export type ProRouteVersionLifecycleStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'READY_TO_PUBLISH'
  | 'ACTIVE'
  | 'SUPERSEDED'
  | 'REJECTED'
  | 'CANCELLED'
  | string

export interface ProRouteVersionVO {
  id: number
  routeId: number
  versionNo: string
  active: boolean
  lifecycleStatus: ProRouteVersionLifecycleStatus
  sourceRouteVersionId?: number
  submittedBy?: number
  submittedTime?: string
  approvalProcessInstanceId?: string
  publishedBy?: number
  publishedTime?: string
  remark?: string
}

export interface RouteVersionEditContext {
  routeVersionId: MesRouteId
  versionNo: string
  lifecycleStatus: ProRouteVersionLifecycleStatus
}

export interface ProRouteVersionCreateReqVO {
  routeId: MesRouteId
  sourceRouteVersionId?: MesRouteId
  changeReason?: string
}

export interface ProRouteVersionSubmitPublishReqVO {
  id: MesRouteId
}

export interface ProRouteVersionBlockerVO {
  routeVersionId: number
  publishable: boolean
  blockers: string[]
}

// MES 工艺路线导入结果
export interface ProRouteImportResultVO {
  routeCount: number
  processCreatedCount: number
  processReusedCount: number
  routeProcessCount: number
  routeCodes: string[]
}

// MES 工艺路线多 Sheet Excel 导入结果
export interface ProRouteWorkbookImportResultVO {
  routeCount: number
  routeProcessCount: number
  routeProductCount: number
  routeProductBomCount: number
  routeCodes: string[]
}

export interface RouteFlowNodeVO {
  routeProcessId: number
  processId: number
  processCode?: string
  processName?: string
  routeProcessWorkstationId?: number
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  sort?: number
  x?: number
  y?: number
  linkType?: number
  prepareTime?: number
  waitTime?: number
  colorCode?: string
  keyFlag?: boolean
  checkFlag?: boolean
  resourceStatus?: string
}

export interface RouteFlowEdgeVO {
  id?: number
  sourceRouteProcessId: number
  targetRouteProcessId: number
  relationType?: 'NORMAL'
}

export type RouteFlowBoundaryType = 'START' | 'END'

export interface RouteFlowBoundaryEdgeVO {
  boundaryType: RouteFlowBoundaryType
  routeProcessId: number
  sort?: number
}

export interface RouteFlowLayoutVO {
  routeProcessId: number
  x: number
  y: number
  width?: number
  height?: number
}

export interface RouteFlowValidationMessageVO {
  level: 'ERROR' | 'WARN' | string
  code: string
  message: string
  routeProcessIds?: number[]
  edgeIds?: number[]
}

export interface RouteFlowValidationVO {
  valid: boolean
  validationStatus: 'EMPTY_PROCESS' | 'UNINITIALIZED' | 'INVALID' | 'VALID' | string
  graphVersion: number
  validationMessages: RouteFlowValidationMessageVO[]
  cyclePaths?: number[][]
  invalidRouteProcessIds?: number[]
  invalidEdgeIds?: number[]
  routeProcessIdMap?: Record<string, number>
}

export interface RouteFlowGraphVO extends RouteFlowValidationVO {
  routeId: number
  nodes: RouteFlowNodeVO[]
  edges: RouteFlowEdgeVO[]
  boundaryEdges: RouteFlowBoundaryEdgeVO[]
}

export interface RouteFlowGraphSaveReqVO {
  routeId: number
  routeVersionId?: MesRouteId
  graphVersion: number
  edges: RouteFlowEdgeVO[]
  boundaryEdges: RouteFlowBoundaryEdgeVO[]
  layouts: RouteFlowLayoutVO[]
  routeProcessCreates?: RouteFlowRouteProcessCreateReqVO[]
  routeProcessUpdates?: RouteFlowRouteProcessUpdateReqVO[]
  routeProcessDeletes?: number[]
}

export interface RouteFlowRouteProcessCreateReqVO {
  clientRouteProcessId: number
  routeId: number
  processId: number
  sort: number
  linkType?: number
  prepareTime?: number
  waitTime?: number
  colorCode?: string
  keyFlag?: boolean
  checkFlag?: boolean
  remark?: string
}

export interface RouteFlowRouteProcessUpdateReqVO {
  id: number
  routeId: number
  processId: number
  sort: number
  workstationId?: number
  keyFlag?: boolean
  checkFlag?: boolean
}

export const PRO_ROUTE_IMPORT_INTGY_MD_URL = '/mes/pro/route/import-intgy-md'
export const PRO_ROUTE_IMPORT_SHEET1_XLSX_URL = '/mes/pro/route/import-sheet1-xlsx'
export const PRO_ROUTE_IMPORT_WORKBOOK_XLSX_URL = '/mes/pro/route/import-workbook-xlsx'
export const PRO_ROUTE_VERSION_BASE_URL = '/mes/pro/route-version'

// MES 工艺路线 API
export const ProRouteApi = {
  // 查询工艺路线分页
  getRoutePage: async (params: any) => {
    return await request.get({ url: `/mes/pro/route/page`, params })
  },

  // 查询工艺路线精简列表
  getRouteSimpleList: async () => {
    return await request.get({ url: `/mes/pro/route/simple-list` })
  },

  // 查询产品侧工艺路线绑定选择列表
  getRouteItemBindingList: async () => {
    return await request.get({ url: `/mes/pro/route/item-binding-list` })
  },

  // 查询工艺路线详情
  getRoute: async (id: MesRouteId) => {
    return await request.get({ url: `/mes/pro/route/get?id=` + id })
  },

  // 新增工艺路线
  createRoute: async (data: ProRouteVO, options: Record<string, unknown> = {}) => {
    return await request.post({ url: `/mes/pro/route/create`, data, ...options })
  },

  // 复制基础工艺路线并继承排产/批记录子配置
  copyRoute: async (data: ProRouteCopyReqVO, options: Record<string, unknown> = {}) => {
    return await request.post({ url: `/mes/pro/route/copy`, data, ...options })
  },

  // 修改工艺路线
  updateRoute: async (data: ProRouteVO) => {
    return await request.put({ url: `/mes/pro/route/update`, data })
  },

  // 修改工艺路线状态
  updateRouteStatus: async (id: number, status: number) => {
    return await request.put({ url: `/mes/pro/route/update-status?id=` + id + `&status=` + status })
  },

  // 删除工艺路线
  deleteRoute: async (id: number) => {
    return await request.delete({ url: `/mes/pro/route/delete?id=` + id })
  },

  // 查询工艺路线 DCC 项目代码关系
  getRouteDccProjectBinding: async (routeId: MesRouteId) => {
    return await request.get<RouteDccProjectBindingVO>({
      url: `/mes/pro/route/dcc-project-binding`,
      params: { routeId }
    })
  },

  // 保存工艺路线 DCC 项目代码关系
  saveRouteDccProjectBinding: async (data: RouteDccProjectBindingSaveReqVO) => {
    return await request.put<RouteDccProjectBindingVO>({
      url: `/mes/pro/route/dcc-project-binding`,
      data
    })
  },

  // 解除工艺路线 DCC 项目代码关系
  deleteRouteDccProjectBinding: async (routeId: MesRouteId, expectedVersion?: number) => {
    return await request.delete<RouteDccProjectBindingVO>({
      url: `/mes/pro/route/dcc-project-binding`,
      params: { routeId, expectedVersion }
    })
  },

  // 导出工艺路线 Excel
  exportRoute: async (params: any) => {
    return await request.download({ url: `/mes/pro/route/export-excel`, params })
  },

  // 导出可导入的多 Sheet 工艺路线 Excel
  exportRouteImportWorkbook: async (params: any = {}) => {
    return await request.download({ url: `/mes/pro/route/export-import-xlsx`, params })
  },

  // 导入 IntGY Markdown 工艺路线
  importIntGyMarkdown: async (data: FormData) => {
    const result = await request.upload<{ data: ProRouteImportResultVO }>({
      url: PRO_ROUTE_IMPORT_INTGY_MD_URL,
      data
    })
    return result.data
  },

  // 导入 Sheet1 Excel 工艺路线
  importSheet1Excel: async (data: FormData) => {
    const result = await request.upload<{ data: ProRouteImportResultVO }>({
      url: PRO_ROUTE_IMPORT_SHEET1_XLSX_URL,
      data
    })
    return result.data
  },

  // 导入多 Sheet 工艺路线 Excel
  importRouteWorkbookExcel: async (data: FormData) => {
    const result = await request.upload<{ data: ProRouteWorkbookImportResultVO }>({
      url: PRO_ROUTE_IMPORT_WORKBOOK_XLSX_URL,
      data
    })
    return result.data
  },

  // 查询路线排产配置
  getScheduleConfigListByRouteVersion: async (routeVersionId: MesRouteId) => {
    return await request.get<ProRouteScheduleConfigVO[]>({
      url: `/mes/pro/route-schedule-config/list-by-route-version?routeVersionId=${routeVersionId}`
    })
  },

  // 预览路线工序资源计算产能
  getScheduleResourcePreview: async (routeProcessId: number) => {
    return await request.get<ProRouteResourceCapacityPreviewVO>({
      url: `/mes/pro/route-schedule-config/resource-preview`,
      params: { routeProcessId }
    })
  },
  // 保存路线排产配置
  saveScheduleConfig: async (
    data: ProRouteScheduleConfigVO,
    options: Record<string, unknown> = {}
  ) => {
    return await request.post({ url: `/mes/pro/route-schedule-config/save`, data, ...options })
  },

  // 查询工艺路线工序流转关系图
  getRouteProcessFlowGraph: async (routeId: MesRouteId, routeVersionId?: MesRouteId) => {
    return await request.get<RouteFlowGraphVO>({
      url: `/mes/pro/route-process-flow/get`,
      params: { routeId, routeVersionId }
    })
  },

  // 校验工艺路线工序流转关系图
  validateRouteProcessFlowGraph: async (
    data: RouteFlowGraphSaveReqVO,
    options: Record<string, unknown> = {}
  ) => {
    return await request.post<RouteFlowValidationVO>({
      url: `/mes/pro/route-process-flow/validate`,
      data,
      ...options
    })
  },

  // 保存工艺路线工序流转关系图
  saveRouteProcessFlowGraph: async (
    data: RouteFlowGraphSaveReqVO,
    options: Record<string, unknown> = {}
  ) => {
    return await request.post<RouteFlowValidationVO>({
      url: `/mes/pro/route-process-flow/save`,
      data,
      ...options
    })
  },

  // 查询工艺路线版本列表
  getRouteVersionList: async (routeId: MesRouteId) => {
    return await request.get<ProRouteVersionVO[]>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/list-by-route`,
      params: { routeId }
    })
  },

  // 查询工艺路线版本详情
  getRouteVersion: async (id: MesRouteId) => {
    return await request.get<ProRouteVersionVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/get`,
      params: { id }
    })
  },

  // 创建工艺路线候选版本
  createRouteCandidateVersion: async (data: ProRouteVersionCreateReqVO) => {
    return await request.post<ProRouteVersionVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/create-candidate`,
      data
    })
  },

  // 查询候选版本发布阻断项
  getRouteVersionBlockers: async (id: MesRouteId) => {
    return await request.get<ProRouteVersionBlockerVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/blockers`,
      params: { id }
    })
  },

  // 提交工艺路线候选版本发布审批
  submitRouteCandidateVersion: async (id: MesRouteId) => {
    return await request.post<ProRouteVersionVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/submit?id=${id}`
    })
  },

  // 撤回工艺路线候选版本审核
  withdrawRouteCandidateVersion: async (id: MesRouteId) => {
    return await request.post<ProRouteVersionVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/withdraw?id=${id}`
    })
  },

  // 重新打开已驳回工艺路线候选版本
  reopenRouteCandidateVersion: async (id: MesRouteId) => {
    return await request.post<ProRouteVersionVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/reopen?id=${id}`
    })
  },

  // 取消工艺路线候选版本
  cancelRouteCandidateVersion: async (id: MesRouteId) => {
    return await request.post<ProRouteVersionVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/cancel?id=${id}`
    })
  },

  // 提交工艺路线候选版本并按发布策略执行
  submitAndPublishRouteCandidateVersion: async (data: ProRouteVersionSubmitPublishReqVO) => {
    return await request.post<ProRouteVersionVO>({
      url: `${PRO_ROUTE_VERSION_BASE_URL}/submit-publish?id=${data.id}`
    })
  }
}
