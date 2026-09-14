<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'

defineOptions({ name: 'IFrame' })

type SameOriginChromeMode =
  | 'off'
  | 'jmreport-viewer'
  | 'jmreport-viewer-fit-width'
  | 'jmreport-designer-edit'

const JMREPORT_SUPPRESSION_STYLE_ID = 'codex-jmreport-viewer-clean-style'
const JMREPORT_DESIGNER_EDIT_ROW_HEIGHT_STYLE_ID = 'codex-jmreport-designer-edit-row-height-style'
const JMREPORT_RPBAR_SELECTORS = [
  '#jm-sheet-wrapper > .ty-bar-container',
  '#jm-sheet-wrapper .ty-bar-container'
]
const JMREPORT_SHEET_SELECTORS = ['#jm-sheet-wrapper .jm-sheet']
const JMREPORT_FILL_FORM_ROOT_SELECTORS = ['#fillFormView-app .viewApp']
const JMREPORT_FILL_FORM_AREA_SELECTORS = ['#fillFormView-app .area-content']
const JMREPORT_FILL_FORM_CONTAINER_SELECTORS = ['#fillFormView-app .area-container']
const HIDDEN_CHROME_ATTRIBUTE = 'data-codex-preview-hidden'
const CLEAN_DOCUMENT_ATTRIBUTE = 'data-codex-jmreport-clean'
const SUPPRESSION_TIMEOUT_MS = 4000
const SUPPRESSION_RETRY_INTERVAL_MS = 120
const DESIGNER_EDIT_TIMEOUT_MS = 8000
const DESIGNER_EDIT_RETRY_INTERVAL_MS = 160
const DESIGNER_EDIT_PAINT_STABLE_DELAY_MS = 5000
const DESIGNER_EDIT_MIN_FILL_ROW_HEIGHT = 40
const DESIGNER_EDIT_MIN_FILL_BOX_HEIGHT = 35
const DESIGNER_EDIT_MIN_CANVAS_NON_WHITE_PIXELS = 1000

const props = defineProps({
  src: propTypes.string.def(''),
  sameOriginChromeMode: propTypes.string.def('off'),
  fitWidthMinHeight: propTypes.oneOfType([String, Number]).def(0)
})

const emit = defineEmits<{
  previewBlocked: [message: string]
}>()

const loading = ref(true)
const blockerMessage = ref('')
const shellRef = ref<HTMLDivElement | null>(null)
const frameRef = ref<HTMLIFrameElement | null>(null)
const frameHeight = ref('')
const currentLoadToken = ref(0)
const currentFitFrameWindow = ref<Window | null>(null)
let fitWidthResizeObserver: ResizeObserver | undefined
let fitWidthRafId = 0
let fitWidthSettleTimeoutIds: number[] = []
let designerEditRafId = 0
let designerEditSettleTimeoutIds: number[] = []
let currentDesignerEditFrameWindow: Window | null = null

const isSameOriginChromeMode = (value: string): value is SameOriginChromeMode => {
  return (
    value === 'off' ||
    value === 'jmreport-viewer' ||
    value === 'jmreport-viewer-fit-width' ||
    value === 'jmreport-designer-edit'
  )
}

const shouldSuppressSameOriginChrome = () => {
  return isSameOriginChromeMode(props.sameOriginChromeMode)
    ? props.sameOriginChromeMode === 'jmreport-viewer' ||
        props.sameOriginChromeMode === 'jmreport-viewer-fit-width'
    : false
}

const shouldFitSameOriginViewerWidth = () => props.sameOriginChromeMode === 'jmreport-viewer-fit-width'

const shouldAdaptSameOriginDesignerEdit = () =>
  props.sameOriginChromeMode === 'jmreport-designer-edit'

const resolvedFitWidthMinHeight = computed(() => {
  const value = Number(props.fitWidthMinHeight)
  return Number.isFinite(value) && value > 0 ? value : 0
})

const delay = (ms: number) =>
  new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms)
  })

const buildPreviewBlocker = (reason: string) =>
  `JMReport 同源预览净化失败：${reason}。当前组件不会回退到带工具条的原始 viewer，请先修复同源预览链路。`

const buildDesignerEditBlocker = (reason: string) =>
  `JMReport 表单编辑行高适配失败：${reason}。当前组件不会静默降级到重叠的原始编辑页，请先修复同源编辑链路。`

const ensureSuppressionStyle = (doc: Document) => {
  if (doc.getElementById(JMREPORT_SUPPRESSION_STYLE_ID)) {
    return
  }
  const style = doc.createElement('style')
  style.id = JMREPORT_SUPPRESSION_STYLE_ID
  style.textContent = `
    html[${CLEAN_DOCUMENT_ATTRIBUTE}="true"],
    body[${CLEAN_DOCUMENT_ATTRIBUTE}="true"] {
      margin: 0 !important;
      padding: 0 !important;
      background: #fff !important;
      overflow-x: hidden !important;
    }
    #jm-sheet-wrapper .ty-bar-container[${HIDDEN_CHROME_ATTRIBUTE}="true"] {
      display: none !important;
      opacity: 0 !important;
      height: 0 !important;
      min-height: 0 !important;
      max-height: 0 !important;
      overflow: hidden !important;
      pointer-events: none !important;
      margin: 0 !important;
      padding: 0 !important;
      border: 0 !important;
    }
    #jm-sheet-wrapper .ty-bar-container[${HIDDEN_CHROME_ATTRIBUTE}="true"] > * {
      height: 0 !important;
      width: 0 !important;
      opacity: 0 !important;
      overflow: hidden !important;
      margin: 0 !important;
      padding: 0 !important;
      border: 0 !important;
    }
    #jm-sheet-wrapper {
      margin-top: 0 !important;
      padding-top: 0 !important;
    }
    ${JMREPORT_SHEET_SELECTORS.join(',\n    ')} {
      margin-top: 0 !important;
      height: 100% !important;
    }
    #jm-sheet-wrapper > .jm-sheet > .jm-sheet-toolbar,
    #fillFormView-app .btnArea {
      display: none !important;
      height: 0 !important;
      min-height: 0 !important;
      max-height: 0 !important;
      overflow: hidden !important;
      margin: 0 !important;
      padding: 0 !important;
      border: 0 !important;
    }
    #fillFormView-app .mainContent {
      top: 0 !important;
      margin-top: 0 !important;
      padding-top: 0 !important;
      overflow-x: hidden !important;
    }
    #fillFormView-app .area-content {
      top: 0 !important;
      overflow-x: hidden !important;
    }
  `
  doc.head.appendChild(style)
}

const ensureDesignerEditRowHeightStyle = (doc: Document) => {
  if (doc.getElementById(JMREPORT_DESIGNER_EDIT_ROW_HEIGHT_STYLE_ID)) {
    return
  }
  const style = doc.createElement('style')
  style.id = JMREPORT_DESIGNER_EDIT_ROW_HEIGHT_STYLE_ID
  style.textContent = `
    .fillForm-box {
      min-height: ${DESIGNER_EDIT_MIN_FILL_BOX_HEIGHT}px !important;
      overflow: visible !important;
      box-sizing: border-box !important;
      z-index: 12 !important;
    }
    .fillForm-box .ivu-form-item,
    .fillForm-box .ivu-form-item-content,
    .fillForm-box .ivu-input-wrapper {
      min-height: ${DESIGNER_EDIT_MIN_FILL_BOX_HEIGHT}px !important;
      height: 100% !important;
      box-sizing: border-box !important;
    }
    .fillForm-box input.ivu-input,
    .fillForm-box textarea.ivu-input,
    .fillForm-box .ivu-input {
      min-height: 32px !important;
      box-sizing: border-box !important;
    }
  `
  doc.head.appendChild(style)
}

const ensureSameOriginFrameWindow = () => {
  const frame = frameRef.value
  if (!frame?.contentWindow) {
    throw new Error(buildPreviewBlocker('iframe contentWindow 不可用'))
  }
  try {
    if (frame.contentWindow.location.origin !== window.location.origin) {
      throw new Error(`iframe 与页面不同源：${frame.contentWindow.location.origin}`)
    }
    return frame.contentWindow
  } catch (error: any) {
    throw new Error(buildPreviewBlocker(error?.message || '无法访问同源 iframe 文档'))
  }
}

const resolveSheetElement = (doc: Document) => {
  for (const selector of JMREPORT_SHEET_SELECTORS) {
    const element = doc.querySelector<HTMLElement>(selector)
    if (element) {
      return element
    }
  }
  return undefined
}

const resolveRenderedContentWidth = (doc: Document, sheet: HTMLElement) => {
  const sheetRect = sheet.getBoundingClientRect()
  const contentNodes = Array.from(
    doc.querySelectorAll<HTMLElement>('#jm-sheet-wrapper table, #jm-sheet-wrapper td, #jm-sheet-wrapper th')
  )
  const rightEdge = contentNodes.reduce((max, node) => {
    const rect = node.getBoundingClientRect()
    return Math.max(max, rect.right)
  }, sheetRect.right)
  const renderedWidth = Math.ceil(rightEdge - sheetRect.left)
  return Math.max(
    renderedWidth,
    Math.ceil(sheet.scrollWidth),
    Math.ceil(sheet.offsetWidth),
    Math.ceil(sheetRect.width)
  )
}

const resolveFillFormAreaElement = (doc: Document) => {
  for (const selector of JMREPORT_FILL_FORM_AREA_SELECTORS) {
    const element = doc.querySelector<HTMLElement>(selector)
    if (element) {
      return element
    }
  }
  return undefined
}

const resolveFillFormRenderedSize = (doc: Document) => {
  const areaContent = resolveFillFormAreaElement(doc)
  if (!areaContent) {
    return undefined
  }
  const areaRect = areaContent.getBoundingClientRect()
  const contentNodes = Array.from(areaContent.querySelectorAll<HTMLElement>('*'))
  const rightEdge = contentNodes.reduce((max, node) => {
    const rect = node.getBoundingClientRect()
    return Math.max(max, rect.right)
  }, areaRect.right)
  const bottomEdge = contentNodes.reduce((max, node) => {
    const rect = node.getBoundingClientRect()
    return Math.max(max, rect.bottom)
  }, areaRect.bottom)
  return {
    root: areaContent,
    width: Math.max(
      Math.ceil(rightEdge - areaRect.left),
      Math.ceil(areaContent.scrollWidth),
      Math.ceil(areaContent.offsetWidth),
      Math.ceil(areaRect.width)
    ),
    height: Math.max(
      Math.ceil(bottomEdge - areaRect.top),
      Math.ceil(areaContent.scrollHeight),
      Math.ceil(areaContent.offsetHeight),
      Math.ceil(areaRect.height)
    )
  }
}

const ensureSameOriginDesignerEditFrameWindow = () => {
  try {
    return ensureSameOriginFrameWindow()
  } catch (error: any) {
    throw new Error(buildDesignerEditBlocker(error?.message || '无法访问同源 iframe 文档'))
  }
}

const fitSameOriginViewerWidth = (frameWindow: Window) => {
  const frame = frameRef.value
  const doc = frameWindow.document
  const sheet = resolveSheetElement(doc)
  if (!frame || !sheet) {
    return false
  }

  sheet.style.removeProperty('transform')
  sheet.style.removeProperty('transform-origin')
  sheet.style.removeProperty('width')
  sheet.style.removeProperty('height')

  const fillFormSize = resolveFillFormRenderedSize(doc)
  const sourceWidth = fillFormSize?.width || resolveRenderedContentWidth(doc, sheet)
  const sourceHeight =
    fillFormSize?.height ||
    Math.ceil(Math.max(sheet.scrollHeight, sheet.offsetHeight, sheet.getBoundingClientRect().height))
  const availableWidth = Math.floor(frame.clientWidth || frameWindow.innerWidth || 0)
  if (sourceWidth <= 0 || sourceHeight <= 0 || availableWidth <= 0) {
    return false
  }

  const scale = availableWidth / sourceWidth
  const scaledHeight = Math.ceil(sourceHeight * scale)
  if (fillFormSize) {
    sheet.style.setProperty('transform-origin', 'top left', 'important')
    sheet.style.setProperty('transform', 'scale(1)', 'important')
    sheet.style.setProperty('width', `${availableWidth}px`, 'important')
    sheet.style.setProperty('height', `${scaledHeight}px`, 'important')
    sheet.style.setProperty('overflow', 'hidden', 'important')
  } else {
    sheet.style.transformOrigin = 'top left'
    sheet.style.setProperty('transform-origin', 'top left', 'important')
    sheet.style.setProperty('transform', `scale(${scale})`, 'important')
    sheet.style.setProperty('width', `${sourceWidth}px`, 'important')
    sheet.style.setProperty('height', `${sourceHeight}px`, 'important')
  }

  const wrapper = sheet.closest<HTMLElement>('#jm-sheet-wrapper')
  if (wrapper) {
    wrapper.style.setProperty('margin-top', '0px', 'important')
    wrapper.style.setProperty('padding-top', '0px', 'important')
    wrapper.style.setProperty('width', `${availableWidth}px`, 'important')
    wrapper.style.setProperty('height', `${scaledHeight}px`, 'important')
    wrapper.style.setProperty('overflow', 'hidden', 'important')
  }
  if (fillFormSize?.root) {
    fillFormSize.root.style.setProperty('width', `${sourceWidth}px`, 'important')
    fillFormSize.root.style.setProperty('height', `${sourceHeight}px`, 'important')
    fillFormSize.root.style.setProperty('overflow', 'hidden', 'important')
  }
  doc.documentElement.style.setProperty('overflow', 'hidden', 'important')
  doc.body.style.setProperty('overflow', 'hidden', 'important')
  doc.documentElement.style.setProperty('overflow-x', 'hidden', 'important')
  doc.body.style.setProperty('overflow-x', 'hidden', 'important')
  doc.documentElement.style.setProperty('height', `${scaledHeight}px`, 'important')
  doc.body.style.setProperty('height', `${scaledHeight}px`, 'important')
  JMREPORT_FILL_FORM_ROOT_SELECTORS.forEach((selector) => {
    doc.querySelectorAll<HTMLElement>(selector).forEach((element) => {
      element.style.setProperty('margin-top', '0px', 'important')
      element.style.setProperty('padding-top', '0px', 'important')
    })
  })
  doc.querySelectorAll<HTMLElement>('#fillFormView-app .mainContent').forEach((element) => {
    element.style.setProperty('top', '0px', 'important')
    element.style.setProperty('margin-top', '0px', 'important')
    element.style.setProperty('padding-top', '0px', 'important')
    element.style.setProperty('width', `${availableWidth}px`, 'important')
    element.style.setProperty('height', `${scaledHeight}px`, 'important')
    element.style.setProperty('overflow', 'hidden', 'important')
    element.style.setProperty('overflow-x', 'hidden', 'important')
  })
  doc.querySelectorAll<HTMLElement>('#fillFormView-app .area-content').forEach((element) => {
    element.style.setProperty('top', '0px', 'important')
    element.style.setProperty('left', '0px', 'important')
    element.style.setProperty('transform-origin', 'top left', 'important')
    element.style.setProperty('transform', `scale(${scale})`, 'important')
    element.style.setProperty('overflow', 'hidden', 'important')
    element.style.setProperty('overflow-x', 'hidden', 'important')
  })
  JMREPORT_FILL_FORM_CONTAINER_SELECTORS.forEach((selector) => {
    doc.querySelectorAll<HTMLElement>(selector).forEach((element) => {
      element.style.setProperty('left', '0px', 'important')
      element.style.setProperty('top', '0px', 'important')
      element.style.setProperty('width', `${availableWidth}px`, 'important')
      element.style.setProperty('height', `${scaledHeight}px`, 'important')
      element.style.setProperty('overflow', 'hidden', 'important')
      element.style.setProperty('overflow-x', 'hidden', 'important')
    })
  })
  frameHeight.value = `${Math.max(scaledHeight, resolvedFitWidthMinHeight.value)}px`
  currentFitFrameWindow.value = frameWindow
  return true
}

const resolveDesignerEditSheetRuntime = (frameWindow: Window) => {
  const xs = (frameWindow as any).xs
  const sheet = xs?.sheet
  const data = sheet?.data
  const rows = data?.rows?._
  if (!xs || !sheet || !data || !rows) {
    return undefined
  }
  return {
    xs,
    sheet,
    data,
    rows: rows as Record<string, any>
  }
}

const isDesignerEditFillCell = (cell: any) => Boolean(cell?.fillForm)

const isDesignerEditFillRow = (row: any) =>
  Object.values(row?.cells || {}).some((cell) => isDesignerEditFillCell(cell))

const setDesignerEditRowHeight = (
  runtime: NonNullable<ReturnType<typeof resolveDesignerEditSheetRuntime>>,
  rowIndex: number,
  rowHeight: number,
  row: any
) => {
  if (typeof runtime.data.setRowHeight === 'function') {
    runtime.data.setRowHeight(rowIndex, rowHeight)
    return
  }
  if (typeof runtime.data.rows?.setHeight === 'function') {
    runtime.data.rows.setHeight(rowIndex, rowHeight)
    return
  }
  row.height = rowHeight
}

const applyDesignerEditFillBoxHeight = (doc: Document) => {
  let count = 0
  doc.querySelectorAll<HTMLElement>('.fillForm-box').forEach((box) => {
    const currentHeight = box.getBoundingClientRect().height
    if (currentHeight < DESIGNER_EDIT_MIN_FILL_BOX_HEIGHT) {
      box.style.setProperty('height', `${DESIGNER_EDIT_MIN_FILL_BOX_HEIGHT}px`, 'important')
    }
    if (box.style.getPropertyValue('min-height') !== `${DESIGNER_EDIT_MIN_FILL_BOX_HEIGHT}px`) {
      box.style.setProperty('min-height', `${DESIGNER_EDIT_MIN_FILL_BOX_HEIGHT}px`, 'important')
    }
    if (box.style.getPropertyValue('overflow') !== 'visible') {
      box.style.setProperty('overflow', 'visible', 'important')
    }
    count += 1
  })
  return count
}

const applyDesignerEditFillRowHeights = (frameWindow: Window) => {
  const runtime = resolveDesignerEditSheetRuntime(frameWindow)
  if (!runtime) {
    return false
  }
  let changed = 0
  Object.entries(runtime.rows).forEach(([rowKey, row]) => {
    const rowIndex = Number(rowKey)
    if (!Number.isInteger(rowIndex) || !isDesignerEditFillRow(row)) {
      return
    }
    const currentRowHeight = Number(row?.height || runtime.data.rows?.height || 0)
    const rowHeight = Math.max(currentRowHeight, DESIGNER_EDIT_MIN_FILL_ROW_HEIGHT)
    if (rowHeight > currentRowHeight) {
      setDesignerEditRowHeight(runtime, rowIndex, rowHeight, row)
      changed += 1
    }
  })
  if (changed > 0) {
    if (typeof runtime.sheet.reload === 'function') {
      runtime.sheet.reload()
    } else if (typeof runtime.sheet.renderTable === 'function') {
      runtime.sheet.renderTable()
    }
    if (typeof runtime.sheet.fillFormDesign?.reload === 'function') {
      runtime.sheet.fillFormDesign.reload()
    }
    if (typeof runtime.xs.render === 'function') {
      runtime.xs.render()
    }
  }
  return true
}

const isDesignerEditCanvasPainted = (frameWindow: Window) => {
  const canvas = frameWindow.document.querySelector<HTMLCanvasElement>('canvas.jm-sheet-table')
  if (!canvas?.width || !canvas?.height) {
    return false
  }
  try {
    const context = canvas.getContext('2d')
    if (!context) {
      return false
    }
    const imageData = context.getImageData(0, 0, Math.min(canvas.width, 800), Math.min(canvas.height, 400)).data
    let nonWhitePixels = 0
    for (let index = 0; index < imageData.length; index += 16) {
      const red = imageData[index]
      const green = imageData[index + 1]
      const blue = imageData[index + 2]
      const alpha = imageData[index + 3]
      if (alpha > 0 && !(red > 248 && green > 248 && blue > 248)) {
        nonWhitePixels += 1
        if (nonWhitePixels >= DESIGNER_EDIT_MIN_CANVAS_NON_WHITE_PIXELS) {
          return true
        }
      }
    }
    return false
  } catch (error: any) {
    throw new Error(buildDesignerEditBlocker(error?.message || '无法读取 JMReport 编辑画布绘制状态'))
  }
}

const applyDesignerEditRowHeightPatch = (frameWindow: Window) => {
  const doc = frameWindow.document
  if (!doc?.head || !doc?.body) {
    return false
  }
  ensureDesignerEditRowHeightStyle(doc)
  const runtimeReady = applyDesignerEditFillRowHeights(frameWindow)
  applyDesignerEditFillBoxHeight(doc)
  return runtimeReady
}

const cancelDesignerEditRaf = () => {
  if (designerEditRafId) {
    window.cancelAnimationFrame(designerEditRafId)
    designerEditRafId = 0
  }
}

const cancelDesignerEditSettling = () => {
  designerEditSettleTimeoutIds.forEach((id) => window.clearTimeout(id))
  designerEditSettleTimeoutIds = []
}

const resetDesignerEditState = () => {
  cancelDesignerEditRaf()
  cancelDesignerEditSettling()
  currentDesignerEditFrameWindow = null
}

const scheduleDesignerEditRowHeightPatch = () => {
  if (!shouldAdaptSameOriginDesignerEdit() || !currentDesignerEditFrameWindow) {
    return
  }
  cancelDesignerEditRaf()
  designerEditRafId = window.requestAnimationFrame(() => {
    designerEditRafId = 0
    if (currentDesignerEditFrameWindow) {
      applyDesignerEditRowHeightPatch(currentDesignerEditFrameWindow)
    }
  })
}

const scheduleDesignerEditSettling = () => {
  if (!shouldAdaptSameOriginDesignerEdit()) {
    return
  }
  cancelDesignerEditSettling()
  ;[600, 1500].forEach((delayMs) => {
    const timeoutId = window.setTimeout(() => {
      scheduleDesignerEditRowHeightPatch()
    }, delayMs)
    designerEditSettleTimeoutIds.push(timeoutId)
  })
}

const adaptSameOriginDesignerEditRowHeight = async (loadToken: number) => {
  const frameWindow = ensureSameOriginDesignerEditFrameWindow()
  const deadline = Date.now() + DESIGNER_EDIT_TIMEOUT_MS
  while (Date.now() <= deadline) {
    if (loadToken !== currentLoadToken.value) {
      return
    }
    if (isDesignerEditCanvasPainted(frameWindow)) {
      loading.value = false
      await delay(DESIGNER_EDIT_PAINT_STABLE_DELAY_MS)
      if (loadToken !== currentLoadToken.value) {
        return
      }
      if (!applyDesignerEditRowHeightPatch(frameWindow)) {
        throw new Error(buildDesignerEditBlocker('JMReport 行高模型已加载，但未能应用填写控件行高'))
      }
      currentDesignerEditFrameWindow = frameWindow
      scheduleDesignerEditSettling()
      return
    }
    await delay(DESIGNER_EDIT_RETRY_INTERVAL_MS)
  }
  throw new Error(buildDesignerEditBlocker('在限定时间内没有找到可调整的 JMReport x-spreadsheet 行高模型'))
}

const cancelFitWidthRaf = () => {
  if (fitWidthRafId) {
    window.cancelAnimationFrame(fitWidthRafId)
    fitWidthRafId = 0
  }
}

const cancelFitWidthSettling = () => {
  fitWidthSettleTimeoutIds.forEach((id) => window.clearTimeout(id))
  fitWidthSettleTimeoutIds = []
}

const resetFitWidthState = () => {
  cancelFitWidthRaf()
  cancelFitWidthSettling()
  currentFitFrameWindow.value = null
}

const scheduleFitSameOriginViewerWidth = () => {
  if (!shouldFitSameOriginViewerWidth() || !currentFitFrameWindow.value) {
    return
  }
  cancelFitWidthRaf()
  fitWidthRafId = window.requestAnimationFrame(() => {
    fitWidthRafId = 0
    const frameWindow = currentFitFrameWindow.value
    if (!frameWindow) {
      return
    }
    fitSameOriginViewerWidth(frameWindow)
  })
}

const scheduleFitWidthSettling = () => {
  if (!shouldFitSameOriginViewerWidth()) {
    return
  }
  cancelFitWidthSettling()
  ;[80, 180, 360, 720, 1200, 2000].forEach((delayMs) => {
    const timeoutId = window.setTimeout(() => {
      scheduleFitSameOriginViewerWidth()
    }, delayMs)
    fitWidthSettleTimeoutIds.push(timeoutId)
  })
}

const ensureFitWidthResizeObserver = () => {
  if (fitWidthResizeObserver || typeof ResizeObserver === 'undefined') {
    return
  }
  fitWidthResizeObserver = new ResizeObserver(() => {
    scheduleFitSameOriginViewerWidth()
  })
}

const bindFitWidthResizeObserver = () => {
  if (!shouldFitSameOriginViewerWidth()) {
    return
  }
  ensureFitWidthResizeObserver()
  shellRef.value && fitWidthResizeObserver?.observe(shellRef.value)
}

const unbindFitWidthResizeObserver = () => {
  fitWidthResizeObserver?.disconnect()
}

const markChromeAsHidden = (doc: Document, frameWindow: Window) => {
  ensureSuppressionStyle(doc)
  doc.documentElement.setAttribute(CLEAN_DOCUMENT_ATTRIBUTE, 'true')
  doc.body.setAttribute(CLEAN_DOCUMENT_ATTRIBUTE, 'true')

  const hiddenBars = new Set<HTMLElement>()
  const rpbarEl = (frameWindow as any).xs?.sheet?.rpbar?.el as HTMLElement | undefined
  if (rpbarEl?.setAttribute) {
    hiddenBars.add(rpbarEl)
  }
  JMREPORT_RPBAR_SELECTORS.forEach((selector) => {
    doc.querySelectorAll<HTMLElement>(selector).forEach((element) => hiddenBars.add(element))
  })
  if (hiddenBars.size === 0) {
    return false
  }

  hiddenBars.forEach((element) => {
    element.setAttribute(HIDDEN_CHROME_ATTRIBUTE, 'true')
  })

  const rpViewInst = (frameWindow as any).rpViewInst
  if (rpViewInst) {
    rpViewInst.rpBar = false
  }
  const changeScrollBottom = (frameWindow as any).changeScrollBottom
  if (typeof changeScrollBottom === 'function') {
    changeScrollBottom()
  }
  return true
}

const viewerSurfaceReadyWithoutChrome = (doc: Document) => {
  return Boolean(resolveSheetElement(doc))
}

const suppressSameOriginViewerChrome = async (loadToken: number) => {
  const frameWindow = ensureSameOriginFrameWindow()
  const deadline = Date.now() + SUPPRESSION_TIMEOUT_MS
  while (Date.now() <= deadline) {
    if (loadToken !== currentLoadToken.value) {
      return
    }
    const doc = frameWindow.document
    if (doc?.head && doc?.body && markChromeAsHidden(doc, frameWindow)) {
      if (shouldFitSameOriginViewerWidth()) {
        fitSameOriginViewerWidth(frameWindow)
        scheduleFitWidthSettling()
      }
      return
    }
    if (doc?.head && doc?.body && viewerSurfaceReadyWithoutChrome(doc)) {
      if (shouldFitSameOriginViewerWidth()) {
        fitSameOriginViewerWidth(frameWindow)
        scheduleFitWidthSettling()
      }
      return
    }
    await delay(SUPPRESSION_RETRY_INTERVAL_MS)
  }
  const doc = frameWindow.document
  if (doc?.head && doc?.body && viewerSurfaceReadyWithoutChrome(doc)) {
    if (shouldFitSameOriginViewerWidth()) {
      fitSameOriginViewerWidth(frameWindow)
      scheduleFitWidthSettling()
    }
    return
  }
  throw new Error(
    buildPreviewBlocker('在限定时间内没有找到可抑制的 JMReport viewer 工具条，但报表画布已加载前的就绪信号也不存在')
  )
}

const handleFrameLoad = async (loadToken: number) => {
  try {
    blockerMessage.value = ''
    if (shouldAdaptSameOriginDesignerEdit()) {
      await adaptSameOriginDesignerEditRowHeight(loadToken)
    } else if (shouldSuppressSameOriginChrome()) {
      await suppressSameOriginViewerChrome(loadToken)
    }
  } catch (error: any) {
    const message = error?.message || buildPreviewBlocker('未知错误')
    blockerMessage.value = message
    emit('previewBlocked', message)
  } finally {
    if (loadToken === currentLoadToken.value) {
      loading.value = false
    }
  }
}

const init = () => {
  nextTick(() => {
    const frame = frameRef.value
    const loadToken = currentLoadToken.value + 1
    currentLoadToken.value = loadToken
    loading.value = true
    blockerMessage.value = ''
    frameHeight.value = ''
    resetFitWidthState()
    resetDesignerEditState()
    unbindFitWidthResizeObserver()
    bindFitWidthResizeObserver()
    if (!frame) {
      loading.value = false
      return
    }
    frame.onload = () => {
      void handleFrameLoad(loadToken)
    }
  })
}

onMounted(() => {
  init()
})

onBeforeUnmount(() => {
  resetFitWidthState()
  resetDesignerEditState()
  unbindFitWidthResizeObserver()
})

watch(
  () => [props.src, props.sameOriginChromeMode],
  () => {
    init()
  }
)
</script>

<template>
  <div
    ref="shellRef"
    v-loading="loading"
    class="iframe-shell w-full h-[calc(100vh-var(--top-tool-height)-var(--tags-view-height)-var(--app-content-padding)-var(--app-content-padding)-2px)]"
  >
    <el-alert
      v-if="blockerMessage"
      :title="blockerMessage"
      type="error"
      :closable="false"
      show-icon
      class="iframe-shell__blocker"
    />
    <iframe
      v-show="!blockerMessage"
      ref="frameRef"
      :src="props.src"
      frameborder="0"
      scrolling="no"
      :style="{ height: frameHeight || '100%' }"
      width="100%"
      allowfullscreen="true"
      webkitallowfullscreen="true"
      mozallowfullscreen="true"
    ></iframe>
  </div>
</template>

<style scoped>
.iframe-shell {
  position: relative;
  overflow: auto;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.iframe-shell__blocker {
  margin: 16px;
}
</style>
