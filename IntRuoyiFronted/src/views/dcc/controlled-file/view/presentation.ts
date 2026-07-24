import type { ControlledPreviewWatermark } from '@/api/dcc/controlledFile/workflow'

export const buildControlledFileViewerPath = (
  id: number | string,
  from?: string,
  returnTo?: string
) => {
  const query = new URLSearchParams({ viewer: '1' })
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

export const CONTROLLED_PREVIEW_STAMP_TEXT = '受控'

const CONTROLLED_PREVIEW_STAMP_COLOR = 'rgba(210, 37, 37, 0.96)'

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

export const deriveStampShortText = (watermark?: ControlledPreviewWatermark | null) => {
  const label = getPreviewWatermarkBadgeLabel(watermark)
  if (label.includes('受控')) {
    return CONTROLLED_PREVIEW_STAMP_TEXT
  }
  const normalized = label.replace(/\s+/g, '')
  return normalized.slice(0, Math.min(4, normalized.length)) || CONTROLLED_PREVIEW_STAMP_TEXT
}

export const drawControlledPreviewStamp = (
  context: CanvasRenderingContext2D,
  viewport: { width: number; height: number },
  watermark?: ControlledPreviewWatermark | null,
  outputScale = 1
) => {
  const pageWidth = viewport.width * outputScale
  const pageHeight = viewport.height * outputScale
  const sizeFactor = Math.max(0.85, Math.min(1.2, Math.min(pageWidth, pageHeight) / 1200))
  const boxWidth = Math.round(132 * outputScale * sizeFactor)
  const boxHeight = Math.round(72 * outputScale * sizeFactor)
  const paddingRight = Math.round(34 * outputScale)
  const paddingTop = Math.round(26 * outputScale)
  const borderWidth = Math.max(2, Math.round(3 * outputScale))
  const textSize = Math.round(42 * outputScale * sizeFactor)
  const textBaselineOffset = Math.round(8 * outputScale * sizeFactor)
  const x = Math.max(Math.round(18 * outputScale), pageWidth - boxWidth - paddingRight)
  const y = Math.max(Math.round(18 * outputScale), paddingTop)

  context.save()
  context.lineWidth = borderWidth
  context.strokeStyle = CONTROLLED_PREVIEW_STAMP_COLOR
  context.strokeRect(x, y, boxWidth, boxHeight)
  context.fillStyle = CONTROLLED_PREVIEW_STAMP_COLOR
  context.globalAlpha = 1
  context.font = `700 ${textSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.fillText(
    deriveStampShortText(watermark),
    x + boxWidth / 2,
    y + boxHeight / 2 + textBaselineOffset
  )
  context.restore()
}

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
