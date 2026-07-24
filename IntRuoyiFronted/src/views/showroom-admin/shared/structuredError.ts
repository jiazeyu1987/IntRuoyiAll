export interface ShowroomStructuredErrorDetails {
  backendErrorCode?: string
  operation?: string
  targetType?: string
  targetId?: number | string | null
  targetCode?: string | null
  revisionId?: number | string | null
  sourceRevisionId?: number | string | null
  language?: string | null
  missingFields?: string[] | string | null
  fileId?: number | string | null
  assetId?: string | null
  contentHash?: string | null
  narrationVersionId?: number | string | null
  endpoint?: string | null
}

type ErrorRecord = Record<string, unknown>

const isRecord = (value: unknown): value is ErrorRecord => {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value))
}

const toDisplayValue = (value: unknown, emptyText = '未生成') => {
  if (value === undefined || value === null || value === '') {
    return emptyText
  }
  return String(value)
}

const toStringList = (value: unknown): string[] => {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).filter((item) => item.length > 0)
  }
  if (typeof value === 'string' && value.trim()) {
    return [value.trim()]
  }
  return []
}

const pickDetails = (value: unknown): ShowroomStructuredErrorDetails | null => {
  if (!isRecord(value)) {
    return null
  }
  if (isRecord(value.details)) {
    return value.details as ShowroomStructuredErrorDetails
  }
  if (isRecord(value.data)) {
    if (isRecord(value.data.details)) {
      return value.data.details as ShowroomStructuredErrorDetails
    }
    if (
      value.data.backendErrorCode ||
      value.data.operation ||
      value.data.targetType ||
      value.data.missingFields
    ) {
      return value.data as ShowroomStructuredErrorDetails
    }
  }
  if (isRecord(value.response)) {
    return pickDetails(value.response)
  }
  return null
}

const pickMessage = (error: unknown) => {
  if (error instanceof Error) {
    return error.message
  }
  if (isRecord(error)) {
    const message = error.message || error.msg
    if (typeof message === 'string') {
      return message
    }
  }
  return String(error)
}

const hasResponse = (error: unknown) => {
  return isRecord(error) && isRecord(error.response)
}

const joinRequestUrl = (baseURL: unknown, url: unknown) => {
  const requestUrl = typeof url === 'string' ? url.trim() : ''
  const requestBaseURL = typeof baseURL === 'string' ? baseURL.trim() : ''
  if (!requestUrl) {
    return ''
  }
  if (/^https?:\/\//i.test(requestUrl) || !requestBaseURL) {
    return requestUrl
  }
  return `${requestBaseURL.replace(/\/+$/, '')}/${requestUrl.replace(/^\/+/, '')}`
}

const pickRequestTarget = (error: unknown) => {
  if (!isRecord(error) || !isRecord(error.config)) {
    return ''
  }
  const method =
    typeof error.config.method === 'string' && error.config.method.trim()
      ? error.config.method.trim().toUpperCase()
      : 'REQUEST'
  const url = joinRequestUrl(error.config.baseURL, error.config.url)
  return url ? `${method} ${url}` : method
}

const isNetworkResponseUnavailable = (error: unknown, message: string) => {
  return message === 'Network Error' && !hasResponse(error)
}

const resolveBackendErrorCode = (
  message: string,
  details: ShowroomStructuredErrorDetails | null
) => {
  const detailCode = details?.backendErrorCode
  if (detailCode && String(detailCode).trim()) {
    return String(detailCode).trim()
  }
  const colonIndex = message.indexOf(':')
  const firstToken = colonIndex >= 0 ? message.slice(0, colonIndex) : message.split(/\s+/)[0]
  return /^[A-Z0-9_]+$/.test(firstToken) ? firstToken : ''
}

export const formatShowroomStructuredError = (error: unknown, operationLabel: string) => {
  const details = pickDetails(error)
  const message = pickMessage(error)
  if (isNetworkResponseUnavailable(error, message)) {
    const lines = [`${operationLabel}失败：NETWORK_RESPONSE_UNAVAILABLE`]
    const requestTarget = pickRequestTarget(error)
    if (requestTarget) {
      lines.push(`请求：${requestTarget}`)
    }
    lines.push('原因：浏览器没有收到后端响应；请检查后端服务、网络连通、CORS 或反向代理是否中断连接。')
    lines.push(`原始错误：${message}`)
    return lines.join('\n')
  }

  const backendErrorCode = resolveBackendErrorCode(message, details)
  const lines = [`${operationLabel}失败：${backendErrorCode || message}`]

  if (details) {
    const targetParts = [
      details.targetType,
      details.targetId !== undefined && details.targetId !== null
        ? `#${details.targetId}`
        : '',
      details.targetCode || ''
    ].filter(Boolean)
    const targetLineParts = [
      targetParts.length > 0 ? `目标：${targetParts.join(' ')}` : '',
      details.revisionId !== undefined && details.revisionId !== null
        ? `版本：${details.revisionId}`
        : '',
      details.language ? `语言：${details.language}` : ''
    ].filter(Boolean)
    if (targetLineParts.length > 0) {
      lines.push(targetLineParts.join('，'))
    }

    const missingFields = toStringList(details.missingFields)
    if (missingFields.length > 0) {
      lines.push(`缺失字段：${missingFields.join(', ')}`)
    }

    const resourceLine = [
      `fileId=${toDisplayValue(details.fileId)}`,
      `assetId=${toDisplayValue(details.assetId)}`,
      `contentHash=${toDisplayValue(details.contentHash)}`
    ]
    if (details.narrationVersionId !== undefined && details.narrationVersionId !== null) {
      resourceLine.push(`narrationVersionId=${details.narrationVersionId}`)
    }
    lines.push(`资源：${resourceLine.join('，')}`)

    if (details.endpoint) {
      lines.push(`接口：${details.endpoint}`)
    }
    if (details.operation) {
      lines.push(`操作：${details.operation}`)
    }
  }

  if (!details && backendErrorCode && message !== backendErrorCode) {
    lines.push(message)
  }

  return lines.join('\n')
}
