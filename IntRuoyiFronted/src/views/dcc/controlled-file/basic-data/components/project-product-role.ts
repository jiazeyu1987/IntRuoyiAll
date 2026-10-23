import type { DccProjectReviewerRole, DccProjectProductCreateRespVO } from '@/api/dcc/controlledFile/projectProductRequests'
import { canReviewProjectProduct as legacyCanReview, reviewerIdentity } from './project-reviewer'

export const PROJECT_ROLE_REVIEW_CONTRACT = 'ROLE_SINGLE_REVIEW_V1'
export const requireProjectReviewerRole = (value: DccProjectReviewerRole): DccProjectReviewerRole => {
  if (!value || value.reviewContract !== PROJECT_ROLE_REVIEW_CONTRACT || value.roleCode !== 'dcc_project_reviewer'
    || !value.roleName?.trim() || typeof value.canReview !== 'boolean' || !Array.isArray(value.candidates) || !value.candidates.length)
    throw new Error('项目审核员角色未就绪，请在角色管理中检查角色、权限及启用成员')
  const ids = new Set<string>()
  const candidates = value.candidates.map(row => {
    const id = reviewerIdentity(row.userId)
    if (ids.has(id) || !row.username?.trim() || !row.nickname?.trim()) throw new Error('项目审核员候选身份不完整或重复')
    ids.add(id); return { ...row, userId: id }
  })
  return { ...value, roleId: reviewerIdentity(value.roleId), candidates }
}
export const canReviewProjectProduct = (row: DccProjectProductCreateRespVO, actor: unknown): boolean => {
  if (row.status !== 'PENDING_REVIEW') return false
  if (row.reviewContract == null) return row.reviewerRoleId == null && row.reviewerRoleCode == null && legacyCanReview(row, actor)
  if (row.reviewContract !== PROJECT_ROLE_REVIEW_CONTRACT || row.reviewerRoleCode !== 'dcc_project_reviewer' || row.canReview !== true) return false
  try { reviewerIdentity(row.reviewerRoleId); reviewerIdentity(actor); return true } catch { return false }
}
