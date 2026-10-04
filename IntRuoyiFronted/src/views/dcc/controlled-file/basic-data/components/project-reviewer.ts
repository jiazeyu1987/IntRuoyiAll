import type { DccProjectReviewerConfiguration } from '@/api/dcc/controlledFile/projectProductRequests'

const validIdentity = (value: unknown): value is string | number => {
  if (typeof value !== 'string' && typeof value !== 'number') return false
  if (typeof value === 'number' && !Number.isSafeInteger(value)) return false
  return /^[1-9][0-9]*$/.test(String(value)) && BigInt(value) <= 9223372036854775807n
}

export const reviewerIdentity = (value: unknown): string => {
  if (!validIdentity(value)) throw new Error('审核账号身份无效')
  return String(value)
}

export const parseReviewerConfiguration = (value: DccProjectReviewerConfiguration): DccProjectReviewerConfiguration => {
  if (!value || typeof value.configured !== 'boolean' || typeof value.enabled !== 'boolean')
    throw new Error('审核人配置响应不完整')
  if (!value.configured) {
    if (value.enabled || value.reviewerUserId !== null || value.reviewerUsername !== null || value.reviewerNickname !== null)
      throw new Error('审核人配置状态不一致')
    return { ...value }
  }
  if (typeof value.reviewerUsername !== 'string' || !value.reviewerUsername.trim() ||
      typeof value.reviewerNickname !== 'string' || !value.reviewerNickname.trim())
    throw new Error('审核人配置缺少正式账号信息')
  return { ...value, reviewerUserId: reviewerIdentity(value.reviewerUserId) }
}

export const requireConfiguredReviewer = (value: DccProjectReviewerConfiguration): DccProjectReviewerConfiguration => {
  const result = parseReviewerConfiguration(value)
  if (!result.configured) throw new Error('请先配置项目及产品创建审核人')
  if (!result.enabled) throw new Error('配置的审核人账号未启用，请重新配置')
  return result
}

export const canReviewProjectProduct = (
  row: { status: string; configuredReviewerUserId?: string | number | null },
  actorId: unknown
): boolean => row.status === 'PENDING_REVIEW' && validIdentity(actorId) &&
  validIdentity(row.configuredReviewerUserId) && String(actorId) === String(row.configuredReviewerUserId)
