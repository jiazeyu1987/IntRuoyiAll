interface FailurePresentation { reason: string; category: string; code?: number }
const record = (value: unknown): Record<string, unknown> | undefined =>
  value !== null && typeof value === 'object' ? value as Record<string, unknown> : undefined

// Only business responses with a numeric code can supply user-facing prose.
// Reject technical diagnostics and credential-bearing text instead of echoing it.
const safeBusinessReason = (value: unknown): string | undefined => {
  if (typeof value !== 'string') return undefined
  const text = value.trim()
  if (!text || text.length > 200 || !/[\u4e00-\u9fff]/.test(text)) return undefined
  if (/https?:|bearer|token|secret|password|api[ _-]?key|authorization|stack|exception|\bsql\b|select\s|insert\s|update\s|delete\s|\[object|[{}<>]|[a-z0-9_+\/=.-]{24,}|密码|令牌|密钥/i.test(text)) return undefined
  return text
}

export const presentUploadFailure = (error: unknown): FailurePresentation => {
  const failure = record(error)
  const response = record(failure?.response)
  const body = record(response?.data)
  const protocol = typeof body?.code === 'number' ? body : typeof failure?.code === 'number' ? failure : undefined
  const code = protocol?.code as number | undefined
  const reason = safeBusinessReason(protocol?.msg ?? protocol?.message)
  if (code !== undefined && Number.isFinite(code) && code !== 0 && code !== 200 && reason) return { reason, category: '业务校验未通过', code }
  const status = response?.status ?? code
  if (status === 413) return { reason: '文件超过服务允许的大小，请缩小文件后重试。', category: '文件大小不符合要求', code: 413 }
  if (status === 415) return { reason: '服务不支持本次文件类型，请确认文件格式。', category: '文件类型不受支持', code: 415 }
  if (status === 401) return { reason: '登录状态已失效，请重新登录后上传。', category: '登录校验失败', code: 401 }
  if (status === 403) return { reason: '没有上传权限，请联系管理员确认授权。', category: '权限校验失败', code: 403 }
  if (status === 429) return { reason: '上传请求受到服务限流，请稍后重试。', category: '服务限流', code: 429 }
  if (failure?.code === 'REQUEST_CONTEXT_EXPIRED' || (error instanceof Error && error.message === '登录或租户已切换，请在当前租户重新上传文件')) {
    return { reason: '登录或租户已切换，请在当前租户重新上传文件。', category: '上传上下文已变更' }
  }
  if (failure?.code === 'ECONNABORTED' || failure?.code === 'ETIMEDOUT') return { reason: '上传请求超时，请确认文件上传结果后再重试。', category: '请求超时' }
  if (failure?.code === 'ERR_NETWORK') return { reason: '无法连接上传服务，请检查网络连接后重试。', category: '网络连接失败' }
  return { reason: '上传未完成，请联系管理员核查本次上传。', category: '上传服务错误', ...(code !== undefined ? { code } : {}) }
}

export const presentImageFailure = (recordId: number) => ({
  reason: '图片生成失败，请联系管理员核查本条绘画记录。',
  category: '绘图服务生成失败',
  recordId
})
