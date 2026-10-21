const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const viewerRoot = path.join(root, 'src/views/dcc/controlled-file/view')
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }

function loadTs(name) {
  const host = { exports: {}, Error, require: id => { throw new Error(id) } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(viewerRoot, name + '.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return host.exports
}
function loadViewer({ pages = ['first needle', 'second NEEDLE needle', 'last'], metadataError, textGate, renderGate, textError, missingCanvas, cleanupError } = {}) {
  const source = fs.readFileSync(path.join(viewerRoot, 'index.vue'), 'utf8')
  const descriptor = parse(source).descriptor
  const props = vue.reactive({ previewBlob: new Uint8Array([1, 2, 3]), previewKind: 'PDF', watermark: { text: '受控水印', traceCode: 'exact-version-trace' } })
  const events = [], lifecycle = {}, canvases = new Map(), scrolls = [], rendered = [], requests = []
  class Element {}
  class Canvas {
    style = {}
    getContext() { return { save() {}, restore() {}, strokeRect() {}, fillText() {} } }
  }
  let loadingCalls = 0
  const documents = []
  const getDocument = options => {
    loadingCalls++
    requests.push(options)
    const capturedPages = pages.slice()
    const document = { numPages: capturedPages.length, destroyed: false,
      getPage: async number => ({
        getViewport: ({ scale }) => ({ width: 400 * scale, height: 600 * scale }),
        getTextContent: async () => { if (textGate && loadingCalls === 1) await textGate.promise; if (textError) throw textError; return { items: [{ str: capturedPages[number - 1], hasEOL: true }] } },
        render: () => ({ promise: (async () => { if (renderGate) await renderGate.promise; rendered.push(number) })() }),
        cleanup() {}
      }), cleanup() { if (cleanupError) throw cleanupError }, destroy() { this.destroyed = true }
    }
    documents.push(document)
    return { promise: Promise.resolve(document), destroy: () => { document.destroyed = true } }
  }
  const watched = []
  let api
  const host = { exports: {}, ...vue, Uint8Array, Blob, TextDecoder, URL, Error, HTMLCanvasElement: Canvas,
    HTMLElement: Element, window: { devicePixelRatio: 1, addEventListener() {}, removeEventListener() {} },
    document: { visibilityState: 'hidden', hasFocus: () => false, addEventListener() {}, removeEventListener() {} },
    defineOptions() {}, defineProps: () => props, defineEmits: () => (...args) => events.push(args),
    watch: (read, callback, options) => { watched.push(callback); if (vue.isRef(read)) return vue.watch(read, callback, options) }, onMounted: callback => { lifecycle.mount = callback },
    onBeforeUnmount: callback => { lifecycle.unmount = callback },
    nextTick: async () => {
      for (const page of api.pageEntries.value) {
        if (missingCanvas) continue
        if (!canvases.has(page.pageNumber)) canvases.set(page.pageNumber, new Canvas())
        api.bindCanvasRef(page.pageNumber, canvases.get(page.pageNumber))
        api.bindPdfPageRef?.(page.pageNumber, Object.assign(new Element(), { offsetTop: (page.pageNumber - 1) * 800, offsetHeight: 800 }))
      }
    },
    require: id => {
      if (id === '@/utils/auth') return { getAccessToken: () => 'viewer-fixture-auth', getTenantId: () => '31', getVisitTenantId: () => undefined }
      if (id === '@/hooks/web/useCache') return { CACHE_KEY: { USER: 'USER' }, useCache: () => ({ wsCache: { get: () => ({ user: { id: '99' } }) } }) }
      if (id === './vendor/pdf.min.mjs') return { getDocument, GlobalWorkerOptions: {} }
      if (id === './presentation') return loadTs('presentation')
      if (id === './pdf-tools') return loadTs('pdf-tools')
      if (id === './reading-lifecycle') return loadTs('reading-lifecycle')
      if (id === '@/api/dcc/controlledFile/readingSession') return { createPreviewReadingApi: () => { throw new Error('local PDF fixture must not request reading capability') } }
      if (id.endsWith('.vue')) return {}
      if (id === '@/api/common/filePreview') return {
        buildDccControlledFilePreviewSource: id => ({ type: 'DCC_CONTROLLED_FILE', controlledFileId: id }),
        getOnlineFilePreviewMetadata: async () => { throw metadataError || new Error('unexpected metadata request') },
        previewOnlineFileWithWatermark: async () => { throw new Error('unexpected byte request') }
      }
      throw new Error(id)
    }
  }
  const names = ['loadPreview', 'pageEntries', 'bindCanvasRef', 'bindPdfPageRef', 'viewerFrameRef', 'pdfTextPages',
    'pdfSearchQuery', 'pdfSearchMatches', 'pdfSearchMatchIndex', 'pdfSearchMessage', 'pdfSearchError',
    'pdfCurrentPage', 'pdfPageInput', 'pdfNavigationError', 'handlePdfPageJump', 'goToPdfPage',
    'handlePdfSearch', 'handlePdfSearchNext', 'handlePdfSearchPrevious', 'handlePdfScroll',
    'handleZoomIn', 'handleZoomOut', 'handleRotateRight', 'getPdfPageViewportStyle', 'viewerZoomPercent',
    'viewerRotationDegrees', 'errorMessage', 'loading', 'resolvedWatermark', 'pdfDocument',
    'resolvedPreviewKind', 'resolvedWatermarkText', 'resolvedWatermarkLabel', 'watermarkDetail',
    'watermarkBackground', 'canTransformPreview', 'pdfPreviewTransformStyle', 'pdfCanSearch',
    'MIN_VIEWER_ZOOM_PERCENT', 'MAX_VIEWER_ZOOM_PERCENT', 'DEFAULT_VIEWER_ZOOM_PERCENT',
    'handleResetViewerTransform', 'blockPreviewInteraction', 'handleWindowKeydown', 'renderVersion', 'registeredPreviewVisible', 'registeredPreviewLabel',
    'readingSupported', 'readingSummary', 'readingError', 'readingCloseError', 'formatReadingOpenedAt']
  const expose = names.map(name => `${name}: typeof ${name} === 'undefined' ? undefined : ${name}`).join(',')
  const code = descriptor.scriptSetup.content.replaceAll('import.meta.env.BASE_URL', "'/'") + '\nexports.api = {' + expose + '}'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, host)
  api = host.exports.api
  api.viewerFrameRef.value = { clientWidth: 960, scrollTop: 0, scrollTo: value => scrolls.push(value), getBoundingClientRect: () => ({ top: 0 }) }
  return { api, props, source, descriptor, lifecycle, documents, events, requests, canvases, rendered, scrolls, watched, Element, Canvas }
}

test('actual viewer searches real page text, counts occurrences and navigates matched pages', async () => {
  const r = loadViewer()
  await r.api.loadPreview()
  assert.ok(r.api.pdfSearchQuery, 'viewer must expose a usable PDF search input')
  r.api.pdfSearchQuery.value = 'needle'
  r.api.handlePdfSearch()
  assert.deepEqual(Array.from(r.api.pdfSearchMatches.value, x => x.pageNumber), [1, 2, 2])
  assert.equal(r.api.pdfCurrentPage.value, 1)
  r.api.handlePdfSearchNext()
  assert.equal(r.api.pdfCurrentPage.value, 2)
  assert.equal(r.api.pdfSearchMatchIndex.value, 1)
  r.api.handlePdfSearchPrevious()
  assert.equal(r.api.pdfCurrentPage.value, 1)
  assert.equal(r.api.pdfSearchMessage.value.includes('3'), true)
  await vue.nextTick()
  assert.equal(r.api.pdfSearchMatches.value.length, 3, 'queued input invalidation must not erase a newly submitted search')
  r.api.pdfSearchQuery.value = 'absent'
  r.api.handlePdfSearch()
  assert.equal(r.api.pdfSearchMatches.value.length, 0)
  assert.match(r.api.pdfSearchMessage.value, /未找到/)
})

test('actual page jump and transforms preserve source bytes, watermark and rendered page geometry', async () => {
  const r = loadViewer()
  const original = Array.from(r.props.previewBlob)
  await r.api.loadPreview()
  assert.ok(r.api.pdfPageInput, 'viewer must accept an explicit page number')
  r.api.pdfPageInput.value = '3'
  r.api.handlePdfPageJump()
  assert.equal(r.api.pdfCurrentPage.value, 3)
  assert.equal(r.scrolls.at(-1).top, 1600)
  r.api.goToPdfPage(2)
  assert.equal(r.api.pdfCurrentPage.value, 2)
  for (const invalid of ['0', '4', '1.5', 'wrong', '']) {
    r.api.pdfPageInput.value = invalid
    r.api.handlePdfPageJump()
    assert.equal(r.api.pdfCurrentPage.value, 2)
    assert.match(r.api.pdfNavigationError.value, /页码/)
  }
  r.api.handleZoomIn()
  r.api.handleRotateRight()
  const page = r.api.pageEntries.value[0]
  assert.equal(r.api.getPdfPageViewportStyle(page).width, `${Math.ceil(page.height * 1.25)}px`)
  assert.equal(r.api.getPdfPageViewportStyle(page).height, `${Math.ceil(page.width * 1.25)}px`)
  assert.equal(r.api.viewerRotationDegrees.value, 90)
  assert.equal(r.api.resolvedWatermark.value.traceCode, 'exact-version-trace')
  assert.notEqual(r.requests[0].data, r.props.previewBlob)
  r.requests[0].data[0] = 99
  assert.deepEqual(Array.from(r.props.previewBlob), original)
  assert.deepEqual(r.rendered, [1, 2, 3])
})

test('scanned PDF has explicit no-text search state and never invents matches', async () => {
  const r = loadViewer({ pages: ['', '   '] })
  await r.api.loadPreview()
  assert.ok(r.api.pdfSearchMessage, 'viewer must distinguish a PDF without text')
  assert.match(r.api.pdfSearchMessage.value, /无可搜索文本/)
  r.api.pdfSearchQuery.value = '受控水印'
  r.api.handlePdfSearch()
  assert.equal(r.api.pdfSearchMatches.value.length, 0)
})

test('switching document and unmount invalidate late text and render updates', async () => {
  const gate = deferred()
  const pages = ['old needle', 'old other']
  const r = loadViewer({ pages, textGate: gate })
  const old = r.api.loadPreview()
  await new Promise(resolve => setImmediate(resolve))
  assert.ok(r.api.pdfCurrentPage, 'page state must be bound to the current PDF')
  r.props.previewBlob = new Uint8Array([4, 5, 6])
  pages.splice(0, pages.length, 'new-version unique')
  const current = r.api.loadPreview()
  gate.resolve()
  await Promise.all([old, current])
  assert.equal(r.api.pdfCurrentPage.value, 1)
  assert.equal(r.api.pdfSearchQuery.value, '')
  assert.deepEqual(Array.from(r.api.pdfTextPages.value), ['new-version unique'])
  assert.equal(r.documents[0].destroyed, true)
  assert.equal(r.documents[1].destroyed, false)
  r.lifecycle.unmount()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(r.api.pageEntries.value.length, 0)
  assert.equal(r.api.pdfSearchMatches.value.length, 0)
})

test('late text completion and late render completion after unmount cannot restore pages or search', async () => {
  for (const stage of ['textGate', 'renderGate']) {
    const gate = deferred()
    const r = loadViewer({ [stage]: gate })
    const load = r.api.loadPreview()
    await new Promise(resolve => setImmediate(resolve))
    r.lifecycle.unmount()
    gate.resolve()
    await load
    assert.equal(r.api.pageEntries.value.length, 0)
    assert.equal(r.api.pdfTextPages.value.length, 0)
    assert.equal(r.api.pdfSearchMatches.value.length, 0)
    assert.equal(r.api.pdfDocument.value, null)
  }
})

test('failed text extraction is a visible failure and not the scanned-document empty state', async () => {
  const r = loadViewer({ textError: new Error('TEXT_PERMISSION_DENIED') })
  await r.api.loadPreview()
  assert.equal(r.api.pdfSearchError.value, 'TEXT_PERMISSION_DENIED')
  assert.match(r.api.pdfSearchMessage.value, /不可用/)
  assert.doesNotMatch(r.api.pdfSearchMessage.value, /无可搜索文本/)
  assert.equal(r.api.pdfTextPages.value.length, 0)
})

test('render canvas failure cannot silently become a successful blank page', async () => {
  const r = loadViewer({ missingCanvas: true })
  await r.api.loadPreview()
  assert.match(r.api.errorMessage.value, /画布未准备好/)
  assert.equal(r.api.pdfTextPages.value.length, 0)
})

test('switching an active PDF destroys it directly without an unsafe rendering-time cleanup', async () => {
  const gate = deferred()
  const r = loadViewer({ renderGate: gate, cleanupError: new Error('Page is currently rendering') })
  const old = r.api.loadPreview()
  await new Promise(resolve => setImmediate(resolve))
  r.props.previewBlob = new Uint8Array([4, 5, 6])
  const current = r.api.loadPreview()
  gate.resolve()
  await Promise.all([old, current])
  assert.equal(r.documents[0].destroyed, true)
  assert.equal(r.api.errorMessage.value, '')
  assert.equal(r.api.pdfTextPages.value.length, 3)
})

test('scrolling tracks current page; invalidation clears search when the query changes', async () => {
  const r = loadViewer()
  await r.api.loadPreview()
  r.api.viewerFrameRef.value.scrollTop = 850
  r.api.handlePdfScroll({ target: r.api.viewerFrameRef.value })
  assert.equal(r.api.pdfCurrentPage.value, 2)
  r.api.pdfSearchQuery.value = 'needle'
  r.api.handlePdfSearch()
  assert.equal(r.api.pdfSearchMatches.value.length, 3)
  r.api.pdfSearchQuery.value = 'another query'
  assert.equal(r.api.pdfSearchMatches.value.length, 0)
})

test('the real rendered PDF template retains a watermark overlay on every page after tools act', async () => {
  const r = loadViewer()
  await r.api.loadPreview()
  const compiled = compileTemplate({ source: r.descriptor.template.content, filename: 'index.vue', id: 'g09-render-watermark' })
  const host = { exports: {}, require: () => vue }
  vm.runInNewContext(ts.transpileModule(compiled.code, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, host)
  const nodes = []
  let pageOffset = 0
  const renderer = vue.createRenderer({ createElement: tag => Object.assign(tag === 'canvas' ? new r.Canvas() : new r.Element(), {
    tag, children: [], props: {}, offsetTop: 0, offsetHeight: 800, scrollTop: 0,
    scrollTo(value) { this.scrollTop = value.top }
  }),
    createText: text => ({ text }), createComment: text => ({ text }),
    insert: (node, parent, anchor) => { node.parent = parent; const index = anchor ? parent.children.indexOf(anchor) : -1; if (index < 0) parent.children.push(node); else parent.children.splice(index, 0, node) },
    remove: node => { const index = node.parent?.children.indexOf(node); if (index >= 0) node.parent.children.splice(index, 1) },
    setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text },
    patchProp: (node, key, old, value) => { node.props[key] = value; if (key === 'class' && value === 'protected-viewer-page') { node.offsetTop = pageOffset; pageOffset += 800 } },
    parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1]
  })
  const app = renderer.createApp({ setup: () => ({ ...r.api, title: '正式正文' }), render: host.exports.render })
  for (const component of ['el-button', 'el-input', 'el-alert', 'el-empty', 'OnlyOfficeReadOnlyViewer']) app.component(component, { setup: (_props, ctx) => () => vue.h('control', ctx.attrs, ctx.slots.default?.()) })
  app.directive('loading', {})
  const container = { children: [] }
  app.mount(container)
  const walk = node => { nodes.push(node); for (const child of node.children || []) walk(child) }
  const count = () => { nodes.length = 0; walk(container); return nodes.filter(node => node.props?.['data-testid'] === 'protected-preview-watermark-overlay') }
  assert.equal(count().length, 3)
  r.api.pdfSearchQuery.value = 'needle'
  r.api.handlePdfSearch()
  r.api.handlePdfSearchNext()
  r.api.goToPdfPage(3)
  r.api.handleZoomIn()
  r.api.handleRotateRight()
  await vue.nextTick()
  assert.equal(count().length, 3)
  for (const overlay of count()) assert.match(decodeURIComponent(overlay.props.style.backgroundImage), /exact-version-trace/)
  const e = { prevented: false, preventDefault() { this.prevented = true }, stopPropagation() {} }
  r.api.handleWindowKeydown({ ...e, ctrlKey: true, key: 'p', preventDefault() { e.prevented = true } })
  assert.equal(e.prevented, true)
  app.unmount()
})

test('authorization failure remains visible and does not attempt to render bytes', async () => {
  const r = loadViewer({ metadataError: new Error('READ_PERMISSION_DENIED') })
  r.props.previewKind = undefined
  r.props.previewBlob = undefined
  r.props.controlledFileId = '9007199254740993'
  await r.api.loadPreview()
  assert.equal(r.api.errorMessage.value, 'READ_PERMISSION_DENIED')
  assert.equal(r.requests.length, 0)
})

test('viewer SFC compiles and its actual controls bind to the tested behavior', () => {
  const r = loadViewer()
  const script = compileScript(r.descriptor, { id: 'g09-viewer' })
  const template = compileTemplate({ source: r.descriptor.template.content, filename: 'index.vue', id: 'g09-viewer', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const elements = []
  const walk = node => { if (node.type === 1) elements.push(node); for (const child of node.children || []) walk(child) }
  walk(r.descriptor.template.ast)
  const control = label => elements.find(node => node.props.some(p => p.type === 6 && p.name === 'aria-label' && p.value?.content === label))
  for (const [label, handler] of [['搜索 PDF 正文', 'handlePdfSearch'], ['下一处匹配', 'handlePdfSearchNext'], ['上一处匹配', 'handlePdfSearchPrevious'], ['跳转页码', 'handlePdfPageJump']]) {
    const node = control(label)
    assert.ok(node, label + ' must exist in the rendered template')
    assert.ok(node.props.some(p => p.type === 7 && p.name === 'on' && p.exp?.content === handler), label + ' must invoke tested behavior')
  }
})

module.exports = { loadTs, loadViewer, deferred }
