import request from '@/config/axios'
import type { ControlledFileVO } from './workflow'

export const DCC_PROJECT_CODE_ASSIGNMENT_STATUS_ACTIVE = 'ACTIVE'
export const DCC_PROJECT_CODE_ASSIGNMENT_STATUS_REVOKED = 'REVOKED'
export const DCC_PROJECT_CODE_ASSIGNMENT_STATUS_EXPIRED = 'EXPIRED'

export const DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_ALL = 'PROJECT_CODE_CURRENT_FILES'
export const DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED = 'SELECTED_FILES'

export interface DccProjectCodeAssignmentCreateReqVO {
  assigneeUserId: number
  fileIds?: Array<number | string>
  scopeMode: 'PROJECT_CODE_CURRENT_FILES' | 'SELECTED_FILES'
  expireTime?: string | null
  assignmentReason?: string | null
}

export interface DccProjectCodeAssignmentPageReqVO extends PageParam {
  assigneeUserId?: number
  status?: string
  keyword?: string
  createdTime?: string[]
}

export interface DccProjectCodeAssignmentRespVO {
  id: number
  assignmentNo: string
  projectCodeId: number
  projectName?: string | null
  projectCode?: string | null
  scopeMode: string
  assigneeUserId: number
  assigneeNickname?: string | null
  assignedBy?: number | null
  assignedTime?: number | null
  expireTime?: number | null
  status: string
  assignmentReason?: string | null
  fileCount: number
  changedFileCount: number
  changedFieldCount: number
  revokedBy?: number | null
  revokedTime?: number | null
  revokeReason?: string | null
  createTime?: number | null
  updateTime?: number | null
}

export interface DccProjectCodeAssignmentFilePageReqVO extends PageParam {
  keyword?: string
  changed?: boolean
  categoryId?: number
  fileTypeLevel2?: string
  fileTypeLevel3?: string
}

export interface DccProjectCodeAssignmentFileRespVO extends ControlledFileVO {
  metadataEditable: boolean
  metadataEditAssignmentId: number
  changedFieldCount: number
  lastChangedTime?: number | null
}

export interface DccProjectCodeAssignmentAuditPageReqVO extends PageParam {
  projectCodeId?: number
  assignmentId?: number
  controlledFileId?: number
  operatorUserId?: number
  fieldName?: string
  source?: string
  changedTime?: string[]
}

export interface DccProjectCodeAssignmentAuditRespVO {
  id: number
  changeId: number
  assignmentId?: number | null
  assignmentNo?: string | null
  projectCodeId?: number | null
  projectName?: string | null
  projectCode?: string | null
  controlledFileId: number
  fileNumber?: string | null
  fileName?: string | null
  operatorUserId: number
  operatorNickname?: string | null
  fieldName: string
  fieldLabel: string
  oldValueText?: string | null
  newValueText?: string | null
  source: string
  changeReason?: string | null
  changedTime: number
}

export const createProjectCodeAssignment = async (
  projectCodeId: number | string,
  data: DccProjectCodeAssignmentCreateReqVO
): Promise<DccProjectCodeAssignmentRespVO> => {
  return await request.post({ url: `/dcc/project-codes/${projectCodeId}/assignments`, data })
}

export const getProjectCodeAssignmentPage = async (
  projectCodeId: number | string,
  params: DccProjectCodeAssignmentPageReqVO
): Promise<PageResult<DccProjectCodeAssignmentRespVO[]>> => {
  return await request.get({ url: `/dcc/project-codes/${projectCodeId}/assignments/page`, params })
}

export const getMyProjectCodeAssignmentPage = async (
  params: DccProjectCodeAssignmentPageReqVO
): Promise<PageResult<DccProjectCodeAssignmentRespVO[]>> => {
  return await request.get({ url: '/dcc/project-code-assignments/my/page', params })
}

export const getProjectCodeAssignmentFilePage = async (
  assignmentId: number | string,
  params: DccProjectCodeAssignmentFilePageReqVO
): Promise<PageResult<DccProjectCodeAssignmentFileRespVO[]>> => {
  return await request.get({ url: `/dcc/project-code-assignments/${assignmentId}/files/page`, params })
}

export const revokeProjectCodeAssignment = async (
  assignmentId: number | string,
  revokeReason: string
): Promise<boolean> => {
  return await request.put({
    url: `/dcc/project-code-assignments/${assignmentId}/revoke`,
    data: { revokeReason }
  })
}

export const getProjectCodeAssignmentAuditPage = async (
  params: DccProjectCodeAssignmentAuditPageReqVO
): Promise<PageResult<DccProjectCodeAssignmentAuditRespVO[]>> => {
  return await request.get({ url: '/dcc/project-code-assignment-audits/page', params })
}

export const getProjectCodeAssignmentAuditItems = async (
  changeId: number | string
): Promise<DccProjectCodeAssignmentAuditRespVO[]> => {
  return await request.get({ url: `/dcc/project-code-assignment-audits/${changeId}/items` })
}
