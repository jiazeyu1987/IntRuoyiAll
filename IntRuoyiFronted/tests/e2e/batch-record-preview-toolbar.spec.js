const fs = require('fs')
const path = require('path')

const envPath = path.resolve(__dirname, '../../.env.batch-record-preview')
const envSource = fs.readFileSync(envPath, 'utf8')
if (!envSource.includes("VITE_PROXY_TARGET='http://127.0.0.1:48082'")) {
  throw new Error('batch-record preview mode must declare VITE_PROXY_TARGET for same-origin jmreport proxying')
}

const viteConfigPath = path.resolve(__dirname, '../../vite.config.ts')
const viteConfigSource = fs.readFileSync(viteConfigPath, 'utf8')
if (!viteConfigSource.includes("['/jmreport']")) {
  throw new Error('vite config must proxy /jmreport in batch-record preview mode')
}
if (!viteConfigSource.includes("'import.meta.env.VITE_BASE_URL': JSON.stringify(runtimeBaseUrl)")) {
  throw new Error('vite config must rewrite runtime VITE_BASE_URL for same-origin preview mode')
}

const iframePath = path.resolve(__dirname, '../../src/components/IFrame/src/IFrame.vue')
const iframeSource = fs.readFileSync(iframePath, 'utf8')
if (!iframeSource.includes("type SameOriginChromeMode = 'off' | 'jmreport-viewer' | 'jmreport-viewer-fit-width'")) {
  throw new Error('IFrame must expose an explicit same-origin chrome mode contract')
}
if (!iframeSource.includes('fitWidthMinHeight')) {
  throw new Error('IFrame must expose a minimum height contract for fit-width jmreport previews')
}
if (!iframeSource.includes('fitSameOriginViewerWidth')) {
  throw new Error('IFrame must scale same-origin jmreport previews to the container width')
}
if (!iframeSource.includes('ResizeObserver')) {
  throw new Error('IFrame fit-width mode must observe container resize events')
}
if (!iframeSource.includes('scheduleFitSameOriginViewerWidth')) {
  throw new Error('IFrame fit-width mode must reschedule proportional scaling after container width changes')
}
if (!iframeSource.includes('requestAnimationFrame')) {
  throw new Error('IFrame fit-width mode must batch resize recalculation in requestAnimationFrame')
}
if (!iframeSource.includes('transformOrigin')) {
  throw new Error('IFrame fit-width mode must scale proportionally from the top-left origin')
}
if (!iframeSource.includes('Math.max(scaledHeight, resolvedFitWidthMinHeight.value)')) {
  throw new Error('IFrame fit-width mode must keep iframe height proportional while honoring the minimum height gate')
}
if (!iframeSource.includes('overflow: auto;')) {
  throw new Error('IFrame shell must own preview scrolling instead of leaving scroll to the outer page')
}
if (!iframeSource.includes('overscroll-behavior: contain;')) {
  throw new Error('IFrame shell must block wheel scroll chaining to the outer page')
}
if (!iframeSource.includes('#jm-sheet-wrapper > .ty-bar-container')) {
  throw new Error('IFrame must suppress the vendor rpbar using the stable ty-bar-container selector')
}
if (!iframeSource.includes("display: none !important")) {
  throw new Error('IFrame must fully remove the hidden jmreport toolbar from layout instead of only making it transparent')
}
if (!iframeSource.includes("wrapper.style.setProperty('padding-top', '0px', 'important')")) {
  throw new Error('IFrame must clear jmreport wrapper top padding after hiding the toolbar to avoid covering the first content row')
}
if (!iframeSource.includes("wrapper.style.setProperty('margin-top', '0px', 'important')")) {
  throw new Error('IFrame must clear jmreport wrapper top margin after hiding the toolbar to avoid leaving an overlay gap')
}
if (!iframeSource.includes('#jm-sheet-wrapper > .jm-sheet > .jm-sheet-toolbar')) {
  throw new Error('IFrame must suppress the jm-sheet format toolbar in preview mode')
}
if (!iframeSource.includes('#fillFormView-app .btnArea')) {
  throw new Error('IFrame must hide the fill-form top button area in preview mode')
}
if (!iframeSource.includes("#fillFormView-app .area-content")) {
  throw new Error('IFrame must normalize fill-form area-content top offset after hiding the button area')
}
if (!iframeSource.includes("element.style.setProperty('top', '0px', 'important')")) {
  throw new Error('IFrame must force fill-form content back to the top after suppressing preview chrome')
}
if (!iframeSource.includes("rpViewInst.rpBar = false")) {
  throw new Error('IFrame must disable the jmreport rpBar flag after same-origin injection')
}
if (!iframeSource.includes("changeScrollBottom")) {
  throw new Error('IFrame must rebalance jmreport scrolling after hiding the viewer bar')
}
if (!iframeSource.includes('resolveSheetElement(doc)')) {
  throw new Error('IFrame must verify the jmreport sheet surface before declaring same-origin preview success')
}
if (!iframeSource.includes('没有找到可抑制的 JMReport viewer 工具条，但报表画布已加载')) {
  throw new Error('IFrame must accept already chrome-free jmreport viewers instead of falsely blocking them')
}

const wrapperPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue'
)
const wrapperSource = fs.readFileSync(wrapperPath, 'utf8')
if (!wrapperSource.includes("const isPreviewPath = (path: string) => path.includes('/jmreport/view/')")) {
  throw new Error('DesignerWrapper must still detect jmreport preview mode from /jmreport/view/ paths')
}
if (!wrapperSource.includes('ensureSameOriginPreviewSupport()')) {
  throw new Error('DesignerWrapper must fail fast when same-origin preview support is missing')
}
if (!wrapperSource.includes('appendToken(normalizePreviewPath(data.path), false)')) {
  throw new Error('DesignerWrapper preview mode must switch iframe src to a same-origin /jmreport/view/... path')
}
if (!wrapperSource.includes(':sameOriginChromeMode="sameOriginChromeMode"')) {
  throw new Error('DesignerWrapper must delegate viewer chrome suppression through the IFrame component')
}
if (!wrapperSource.includes('@preview-blocked="handlePreviewBlocked"')) {
  throw new Error('DesignerWrapper must surface same-origin preview blockers to the page')
}

console.log('PASS: batch-record preview uses same-origin jmreport view and suppresses the vendor viewer chrome')
