import request from '@/config/axios'

export interface TemporaryRoleGrantVO {
  id: number
  userId: number
  roleId: number
  reason: string
  status: 'PENDING' | 'ACTIVE' | 'REVOKED' | 'EXPIRED'
  applyTime: string
  applicantUserId?: number
  applicantUsername?: string
  approveTime?: string
  approverUserId?: number
  approverUsername?: string
  effectiveTime?: string
  expireTime: string
  remindTime?: string
  reviewCategory?: 'ACTIVE' | 'EXPIRING_SOON' | 'OVERDUE'
  revokeTime?: string
  revokerUserId?: number
  revokerUsername?: string
  revokeReason?: string
  createTime?: string
}

export interface TemporaryRoleGrantCreateReqVO {
  userId: number
  roleId: number
  reason: string
  expireTime: string
}

export interface TemporaryRoleGrantRevokeReqVO {
  id: number
  reason: string
}

export interface TemporaryRoleGrantAuditVO {
  id: number
  grantId: number
  eventType: 'APPLY' | 'APPROVE' | 'REVOKE' | 'EXPIRE' | 'REMIND' | 'USE'
  userId: number
  roleId: number
  permissionCode?: string
  operatorUserId?: number
  operatorUsername?: string
  message?: string
  createTime: string
}

export interface TemporaryRoleGrantReviewSummaryVO {
  activeCount: number
  expiringSoonCount: number
  overdueCount: number
}

export const getTemporaryRoleGrantPage = async (params: PageParam) => {
  return await request.get({ url: '/system/temporary-role-grant/page', params })
}

export const getTemporaryRoleGrantReviewSummary = async () => {
  return await request.get({ url: '/system/temporary-role-grant/review-summary' })
}

export const createTemporaryRoleGrant = async (data: TemporaryRoleGrantCreateReqVO) => {
  return await request.post({ url: '/system/temporary-role-grant/create', data })
}

export const approveTemporaryRoleGrant = async (id: number) => {
  return await request.post({ url: '/system/temporary-role-grant/approve?id=' + id })
}

export const revokeTemporaryRoleGrant = async (data: TemporaryRoleGrantRevokeReqVO) => {
  return await request.post({ url: '/system/temporary-role-grant/revoke', data })
}

export const getTemporaryRoleGrantAuditList = async (grantId: number) => {
  return await request.get({ url: '/system/temporary-role-grant/audit-list?grantId=' + grantId })
}
