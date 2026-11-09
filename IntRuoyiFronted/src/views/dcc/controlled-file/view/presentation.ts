import type { ControlledPreviewWatermark } from '@/api/dcc/controlledFile/workflow'

export type ControlledFileTraceabilityScope = 'trace' | 'signature'

export const buildControlledFileViewerPath = (
  id: number | string,
  from?: string,
  returnTo?: string,
  bodyReadContext?: string
) => {
  const query = new URLSearchParams({ viewer: '1' })
  if (bodyReadContext !== undefined) query.set('bodyReadContext', bodyReadContext)
  if (from) {
    query.set('from', from)
  }
  const normalizedReturnTo = String(returnTo || '').trim()
  if (normalizedReturnTo) {
    query.set('returnTo', encodeURIComponent(normalizedReturnTo))
  }
  return `/dcc/controlled-file/detail/${id}?${query.toString()}`
}

export const buildControlledFileTraceabilityPath = (
  id: number | string,
  from?: string,
  returnTo?: string,
  scope: ControlledFileTraceabilityScope = 'trace'
) => {
  const query = new URLSearchParams({ traceability: '1' })
  query.set('traceScope', scope)
  if (from) {
    query.set('from', from)
  }
  const normalizedReturnTo = String(returnTo || '').trim()
  if (normalizedReturnTo) {
    query.set('returnTo', encodeURIComponent(normalizedReturnTo))
  }
  return `/dcc/controlled-file/detail/${id}?${query.toString()}`
}

export const isControlledFileViewerMode = (query: Record<string, unknown>) => {
  return String(query.viewer || '') === '1'
}

export const resolveControlledFileTraceabilityScope = (
  query: Record<string, unknown>
): ControlledFileTraceabilityScope => {
  return String(query.traceScope || '') === 'signature' ? 'signature' : 'trace'
}

export const resolveControlledFileViewerReturnTo = (value: unknown) => {
  const rawValue = String(value || '').trim()
  const normalized = rawValue ? decodeURIComponent(rawValue) : ''
  if (!normalized || !normalized.startsWith('/')) {
    return ''
  }
  if (normalized.startsWith('//') || /^[a-z][a-z0-9+\-.]*:/i.test(normalized)) {
    return ''
  }
  if (!normalized.startsWith('/dcc/controlled-file/')) {
    return ''
  }
  return normalized
}

export const getProtectedViewerRenderScale = (containerWidth?: number) => {
  const width = Math.max(containerWidth || 960, 720)
  return Math.max(1.15, Math.min(1.5, width / 840))
}

const DEFAULT_WATERMARK_OVERLAY = Object.freeze({
  textColor: '#6b7280',
  opacity: 0.18,
  rotationDeg: -24,
  gapX: 260,
  gapY: 180,
  fontSize: 18
})

const escapeXml = (value: string) =>
  String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

export const resolvePreviewWatermarkTraceCode = (watermark?: ControlledPreviewWatermark | null) =>
  String(watermark?.traceCode || '').trim()

const uniqueVisibleParts = (values: string[]) => {
  const seen = new Set<string>()
  return values
    .map((value) => String(value || '').trim())
    .filter((value) => {
      if (!value || seen.has(value)) {
        return false
      }
      seen.add(value)
      return true
    })
}

export const buildPreviewWatermarkText = (watermark?: ControlledPreviewWatermark | null) => {
  const text = String(watermark?.text || '').trim()
  const traceCode = resolvePreviewWatermarkTraceCode(watermark)
  return uniqueVisibleParts([text, traceCode]).join(' | ')
}

export const getResolvedWatermarkOverlay = (watermark?: ControlledPreviewWatermark | null) => ({
  textColor: watermark?.overlay?.textColor || DEFAULT_WATERMARK_OVERLAY.textColor,
  opacity:
    typeof watermark?.overlay?.opacity === 'number'
      ? Math.min(Math.max(watermark.overlay.opacity, 0.08), 0.4)
      : DEFAULT_WATERMARK_OVERLAY.opacity,
  rotationDeg:
    typeof watermark?.overlay?.rotationDeg === 'number'
      ? watermark.overlay.rotationDeg
      : DEFAULT_WATERMARK_OVERLAY.rotationDeg,
  gapX:
    typeof watermark?.overlay?.gapX === 'number'
      ? Math.max(180, watermark.overlay.gapX)
      : DEFAULT_WATERMARK_OVERLAY.gapX,
  gapY:
    typeof watermark?.overlay?.gapY === 'number'
      ? Math.max(120, watermark.overlay.gapY)
      : DEFAULT_WATERMARK_OVERLAY.gapY,
  fontSize:
    typeof watermark?.overlay?.fontSize === 'number'
      ? Math.max(12, watermark.overlay.fontSize)
      : DEFAULT_WATERMARK_OVERLAY.fontSize
})

export const buildPreviewWatermarkBackground = (
  watermark?: ControlledPreviewWatermark | null
): string => {
  const watermarkText = buildPreviewWatermarkText(watermark)
  if (!watermarkText) {
    return ''
  }
  const overlay = getResolvedWatermarkOverlay(watermark)
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${overlay.gapX}" height="${overlay.gapY}" viewBox="0 0 ${overlay.gapX} ${overlay.gapY}">
      <g transform="translate(${Math.round(overlay.gapX * 0.14)} ${Math.round(overlay.gapY * 0.62)}) rotate(${overlay.rotationDeg})">
        <text
          x="0"
          y="0"
          font-size="${overlay.fontSize}"
          font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
          fill="${overlay.textColor}"
          fill-opacity="${overlay.opacity}"
        >${escapeXml(watermarkText)}</text>
      </g>
    </svg>
  `
  return `url("data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}")`
}

export const buildPreviewWatermarkDetail = (watermark?: ControlledPreviewWatermark | null) => {
  const traceCode = resolvePreviewWatermarkTraceCode(watermark)
  const actor = String(watermark?.actorAccount || watermark?.actorName || '').trim()
  const timestamp = String(watermark?.timestamp || '').trim()
  const detail = uniqueVisibleParts([traceCode, actor, timestamp]).join(' | ')
  return detail || buildPreviewWatermarkText(watermark)
}

export const getPreviewWatermarkBadgeLabel = (watermark?: ControlledPreviewWatermark | null) =>
  String(watermark?.label || '受控预览').trim() || '受控预览'

export const shouldBlockPreviewShortcut = (event: KeyboardEvent) => {
  const key = String(event.key || '').toLowerCase()
  const withCtrlOrMeta = Boolean(event.ctrlKey || event.metaKey)
  const copyLike = withCtrlOrMeta && ['c', 'x', 'a', 's', 'p'].includes(key)
  const shiftInsert = Boolean(event.shiftKey) && key === 'insert'
  return copyLike || shiftInsert
}

export const resolveViewerErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}
