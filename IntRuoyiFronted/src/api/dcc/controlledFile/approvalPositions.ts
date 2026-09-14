import request from '@/config/axios'

export interface ControlledFilePositionAssignmentVO {
  id?: number
  positionId?: number
  assignmentType: string
  systemPostId?: number | null
  userId?: number | null
  active: boolean
  changeReason?: string
}

export interface ControlledFileApprovalPositionVO {
  id: number
  code: string
  name: string
  active: boolean
  source?: string
  remark?: string
  createTime?: number
  assignments: ControlledFilePositionAssignmentVO[]
}

export interface ControlledFileApprovalPositionCreateReqVO {
  name: string
  changeReason: string
}

export const getApprovalPositionList = async (): Promise<ControlledFileApprovalPositionVO[]> => {
  return await request.get({ url: '/dcc/approval-positions' })
}

export const createApprovalPosition = async (
  data: ControlledFileApprovalPositionCreateReqVO
): Promise<ControlledFileApprovalPositionVO> => {
  return await request.post({ url: '/dcc/approval-positions', data })
}

export const saveApprovalPositionAssignments = async (
  id: number,
  data: ControlledFilePositionAssignmentVO[]
): Promise<ControlledFilePositionAssignmentVO[]> => {
  return await request.put({ url: `/dcc/approval-positions/${id}/assignments`, data })
}

export const exportApprovalPositionConfigPackage = async (): Promise<Blob> => {
  return await request.download({ url: '/dcc/approval-positions/config-package/export' })
}

export const importApprovalPositionConfigPackage = async (data: FormData) => {
  return await request.upload({ url: '/dcc/approval-positions/config-package/import', data })
}
