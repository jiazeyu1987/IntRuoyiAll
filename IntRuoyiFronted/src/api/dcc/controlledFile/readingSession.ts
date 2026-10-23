import request from '@/config/axios'
import { buildDccBodyReadHeaders, type DccBodyReadContext } from './bodyReadContext'

export type PreviewReadingScope =
  | { kind: 'CONTROLLED_FILE'; fileId: string; bodyReadContext: DccBodyReadContext }
  | { kind: 'ONLINE_FILE'; fileId: string }
export interface PreviewReadingEvidence {
  viewerToken: string
  accessEventCode: string
  watermarkTraceCode: string
  viewerTokenId: string
  viewerTokenNonce: string
}
export interface PreviewReadingSession {
  sessionId: string
  openedAt: string
  startedAt: string
  lastHeartbeatAt: string
  endedAt: string | null
  accumulatedSeconds: number
  heartbeatIntervalSeconds: number
  maxHeartbeatGapSeconds: number
}
export interface PreviewReadingSummary {
  firstOpenedAt: string | null
  lastOpenedAt: string | null
  accumulatedSeconds: number
  openedSessionCount: number
}
const text = (value: unknown, max: number): string => {
  if (typeof value !== 'string' || !value || value !== value.trim() || value.length > max) throw new Error('阅读会话缺少正式访问证据')
  return value
}
const id = (value: unknown): string => {
  if (typeof value === 'number' && Number.isSafeInteger(value)) value = String(value)
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value) || BigInt(value) > 9223372036854775807n) throw new Error('阅读会话对象身份不合法')
  return value
}
const count = (value: unknown): number => {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw new Error('阅读会话统计响应不合法')
  return value
}
const time = (value: unknown): string => {
  if (typeof value !== 'string' || !value.endsWith('Z') || !Number.isFinite(Date.parse(value))) throw new Error('阅读会话缺少服务器 UTC 时间')
  return value
}
const session = (value: PreviewReadingSession): PreviewReadingSession => {
  if (!value || value.heartbeatIntervalSeconds !== 10 || value.maxHeartbeatGapSeconds !== 15) throw new Error('阅读会话响应缺少正式心跳策略')
  return { ...value, sessionId: id(value.sessionId), openedAt: time(value.openedAt),
    startedAt: time(value.startedAt), lastHeartbeatAt: time(value.lastHeartbeatAt),
    endedAt: value.endedAt === null ? null : time(value.endedAt), accumulatedSeconds: count(value.accumulatedSeconds) }
}

/** Capture capability and exact source now; delayed requests must never use the next viewer's metadata. */
export const createPreviewReadingApi = (scope: PreviewReadingScope, metadata: PreviewReadingEvidence) => {
  if (typeof scope.fileId !== 'string') throw new Error('阅读会话文件 ID 必须保持字符串精度')
  const fileId = id(scope.fileId)
  if (scope.kind !== 'CONTROLLED_FILE' && scope.kind !== 'ONLINE_FILE') throw new Error('不支持此正文阅读来源')
  const root = scope.kind === 'CONTROLLED_FILE' ? '/dcc/controlled-files' : '/dcc/file-preview/files'
  const base = root + '/' + fileId
  const evidence = Object.freeze({
    accessEventCode: text(metadata.accessEventCode, 64),
    watermarkTraceCode: text(metadata.watermarkTraceCode, 64),
    viewerTokenId: text(metadata.viewerTokenId, 64),
    viewerTokenNonce: text(metadata.viewerTokenNonce, 64)
  })
  const headers = Object.freeze({
    'X-DCC-Viewer-Token': text(metadata.viewerToken, 8192),
    ...(scope.kind === 'CONTROLLED_FILE' ? buildDccBodyReadHeaders(fileId, scope.bodyReadContext) : {})
  })
  const update = async (action: 'heartbeat' | 'stop', sessionId: string) => session(await request.post({
    url: base + '/reading-session/' + action, headers, data: { ...evidence, sessionId: id(sessionId) }
  }))
  return {
    start: async (clientSessionId: string) => session(await request.post({
      url: base + '/reading-session/start', headers, data: { ...evidence, clientSessionId: text(clientSessionId, 64) }
    })),
    heartbeat: (sessionId: string) => update('heartbeat', sessionId),
    stop: (sessionId: string) => update('stop', sessionId),
    summary: async (): Promise<PreviewReadingSummary> => {
      const value = await request.get<PreviewReadingSummary>({ url: base + '/reading-summary', headers: {
        ...headers, 'X-DCC-Access-Event-Code': evidence.accessEventCode,
        'X-DCC-Watermark-Trace-Code': evidence.watermarkTraceCode,
        'X-DCC-Viewer-Token-Id': evidence.viewerTokenId, 'X-DCC-Viewer-Token-Nonce': evidence.viewerTokenNonce
      } })
      if (!value) throw new Error('阅读会话统计响应缺失')
      return { firstOpenedAt: value.firstOpenedAt === null ? null : time(value.firstOpenedAt),
        lastOpenedAt: value.lastOpenedAt === null ? null : time(value.lastOpenedAt),
        accumulatedSeconds: count(value.accumulatedSeconds), openedSessionCount: count(value.openedSessionCount) }
    }
  }
}
