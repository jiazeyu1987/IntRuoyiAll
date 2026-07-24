import request from '@/config/axios'

export type ControlledFileRouteCandidateSourceType = 'USER' | 'POSITION'
export type ControlledFileRouteApproveMethod = 'ANY' | 'ALL'

export interface ControlledFileApprovalRouteNodeVO {
  id?: number
  routeId?: number
  stageNo: number
  stageCode?: string
  stageName: string
  stageOrder?: number
  candidateSourceType: ControlledFileRouteCandidateSourceType
  candidateSourceId?: number
  candidateSourceIds: number[]
  approveMethod: ControlledFileRouteApproveMethod
  approveRatio?: number | null
  requireAllApprovals?: boolean
  required: boolean
  sort: number
  stageType?: string
  subjectLabel?: string
  marker?: string
  subjectType?: string
  subjectId?: number
  subjectName?: string
  subjectDepartmentPath?: string
  ruleRemark?: string
}

export interface ControlledFileApprovalRouteVO {
  id?: number
  categoryId?: number
  categoryName?: string
  versionNo?: number
  active?: boolean
  statusLabel?: string
  effectiveTime: string
  remark?: string
  nodeCount?: number
  nodeSummary?: string
  nodes: ControlledFileApprovalRouteNodeVO[]
}

export interface ControlledFileApprovalRoutePreviewReqVO {
  categoryId: number
}

export interface ControlledFileApprovalRoutePreviewVO {
  stageNo: number
  stageCode?: string
  stageName: string
  stageOrder?: number
  candidateSourceType: ControlledFileRouteCandidateSourceType
  candidateSourceIds: number[]
  approvalMode: number
  requireAllApprovals?: boolean
  resolvedUserIds: number[]
}

export interface ControlledFileApprovalRouteSaveReqVO {
  effectiveTime: string
  remark?: string
  nodes: Array<{
    stageNo: number
    stageName: string
    candidateSourceType: ControlledFileRouteCandidateSourceType
    candidateSourceId: number
    approveMethod: ControlledFileRouteApproveMethod
    approveRatio?: number
    required: boolean
    sort: number
  }>
}

export interface ControlledFileApprovalRoutePageReqVO {
  pageNo: number
  pageSize: number
  categoryId?: number
}

export const getApprovalRoutePage = async (
  params: ControlledFileApprovalRoutePageReqVO
): Promise<PageResult<ControlledFileApprovalRouteVO[]>> => {
  return await request.get({ url: '/dcc/approval-routes/page', params })
}

export const getApprovalRoutes = async (
  categoryId: number
): Promise<ControlledFileApprovalRouteVO[]> => {
  return await request.get({ url: '/dcc/approval-routes', params: { categoryId } })
}

export const saveApprovalRoute = async (
  categoryId: number,
  data: ControlledFileApprovalRouteSaveReqVO
): Promise<number> => {
  return await request.put({ url: `/dcc/approval-routes/${categoryId}`, data })
}

export const deleteApprovalRoute = async (routeId: number) => {
  return await request.delete({ url: `/dcc/approval-routes/${routeId}` })
}

export const previewApprovalRoute = async (
  data: ControlledFileApprovalRoutePreviewReqVO
): Promise<ControlledFileApprovalRoutePreviewVO[]> => {
  return await request.post({ url: '/dcc/approval-routes/preview', data })
}
