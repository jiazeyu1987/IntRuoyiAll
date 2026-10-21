const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate, compileStyle } = require('vue/compiler-sfc')

const viewer = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/index.vue'), 'utf8')

// Executes the production SFC script with isolated request/DOM adapters. This is not browser E2E.
function loadViewer({ kind = 'TEXT', text = 'selected controlled body', unavailableReason, deny } = {}) {
  const descriptor = parse(viewer).descriptor
  const bytes = new TextEncoder().encode(text)
  const props = vue.reactive({ controlledFileId: 'selected-version', title: 'selected controlled preview' })
  const calls = [], lifecycle = {}, revoked = [], observers = []
  const metadata = { previewKind: kind, fileName: 'selected.txt', previewRegisteredAt: '2026-10-15T00:30:10Z',
    previewUnavailableReason: unavailableReason, watermarkTraceCode: 'selected-trace',
    watermark: { text: 'controlled watermark', traceCode: 'selected-trace' } }
  class Element {
    dataset = {}
    clientWidth = 640
    offsetWidth = 640
    offsetHeight = 420
    scrollIntoView() { this.scrolled = true }
  }
  class ResizeObserver {
    constructor(callback) { this.callback = callback; this.disconnected = false; observers.push(this) }
    observe() {}
    disconnect() { this.disconnected = true }
  }
  const host = { exports: {}, ...vue, Blob, Uint8Array, TextDecoder, Intl, Date, Error, HTMLElement: Element, HTMLCanvasElement: class {}, ResizeObserver,
    URL: { createObjectURL: () => 'blob:selected-body', revokeObjectURL: value => revoked.push(value) },
    window: { devicePixelRatio: 1, addEventListener() {}, removeEventListener() {} },
    document: { visibilityState: 'hidden', hasFocus: () => false, addEventListener() {}, removeEventListener() {} },
    defineOptions() {}, defineProps: () => props, defineEmits: () => () => {},
    watch: (source, callback, options) => vue.isRef(source) || Array.isArray(source) ? vue.watch(source, callback, options) : () => {},
    onMounted: callback => { lifecycle.mount = callback }, onBeforeUnmount: callback => { lifecycle.unmount = callback },
    require: id => {
      if (id === '@/utils/auth') return { getAccessToken: () => 'viewer-fixture-auth', getTenantId: () => '31', getVisitTenantId: () => undefined }
      if (id === '@/hooks/web/useCache') return { CACHE_KEY: { USER: 'USER' }, useCache: () => ({ wsCache: { get: () => ({ user: { id: '99' } }) } }) }
      if (id === './vendor/pdf.min.mjs') return { GlobalWorkerOptions: {}, getDocument: () => { throw new Error('unexpected PDF conversion') } }
      if (id.endsWith('.vue')) return {}
      if (id === './presentation' || id === './pdf-tools' || id === './reading-lifecycle') {
        const c = { exports: {}, Error }
        vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/view', id + '.ts'), 'utf8'), {
          compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
        }).outputText, c)
        return c.exports
      }
      if (id === '@/api/common/filePreview') return {
        buildDccControlledFilePreviewSource: id => ({ type: 'DCC_CONTROLLED_FILE', controlledFileId: id }),
        getOnlineFilePreviewMetadata: async source => { calls.push(['metadata', source]); if (deny) throw new Error(deny); return metadata },
        previewOnlineFileWithWatermark: async (source, suppliedMetadata) => {
          calls.push(['preview', source, suppliedMetadata])
          return { blob: new Blob([bytes]), watermark: metadata.watermark }
        }
      }
      if (id === '@/api/dcc/controlledFile/readingSession') return {
        createPreviewReadingApi: () => ({ summary: async () => ({ firstOpenedAt: null, lastOpenedAt: null, accumulatedSeconds: 0, openedSessionCount: 0 }) })
      }
      throw new Error('Unexpected import: ' + id)
    } }
  const sourceAst = ts.createSourceFile('viewer.ts', descriptor.scriptSetup.content, ts.ScriptTarget.ES2022, true)
  const names = sourceAst.statements.filter(ts.isVariableStatement).flatMap(statement => Array.from(statement.declarationList.declarations)
    .filter(declaration => ts.isIdentifier(declaration.name)).map(declaration => declaration.name.text))
  const code = descriptor.scriptSetup.content.replaceAll('import.meta.env.BASE_URL', "'/'") + '\nexports.api={' + names.join(',') + '}'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, host)
  return { api: host.exports.api, props, calls, metadata, bytes, lifecycle, revoked, observers, Element, descriptor }
}

async function renderViewer(r) {
  const { code, errors } = compileTemplate({ source: r.descriptor.template.content, filename: 'index.vue', id: 'full-format-render' })
  assert.deepEqual(errors, [])
  const c = { exports: {}, require: () => vue }
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, c)
  const renderer = vue.createRenderer({
    createElement: tag => Object.assign(new r.Element(), { tag, children: [], props: {} }),
    createText: text => ({ text }), createComment: text => ({ comment: text }),
    insert: (node, parent, anchor) => {
      if (node.parent) { const old = node.parent.children.indexOf(node); if (old >= 0) node.parent.children.splice(old, 1) }
      node.parent = parent
      const index = anchor ? parent.children.indexOf(anchor) : -1
      if (index < 0) parent.children.push(node); else parent.children.splice(index, 0, node)
    },
    remove: node => { const index = node.parent?.children.indexOf(node); if (index >= 0) node.parent.children.splice(index, 1) },
    setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text; node.children = [] },
    parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1],
    patchProp: (node, key, old, value) => { node.props[key] = value; if (key === 'data-active-match') node.dataset.activeMatch = value }
  })
  const app = renderer.createApp({ setup: () => ({ ...r.api, ...r.props }), render: c.exports.render })
  for (const name of ['el-button', 'el-input', 'el-alert', 'el-empty']) app.component(name, { setup: (props, { slots }) => () => vue.h('span', slots.default?.()) })
  app.component('OnlyOfficeReadOnlyViewer', { setup: () => { throw new Error('Office editor must be verified by the Office suite') } })
  app.directive('loading', {})
  const root = { children: [] }; app.mount(root); await vue.nextTick(); await vue.nextTick()
  const find = predicate => {
    const results = []
    const walk = node => { if (predicate(node)) results.push(node); for (const child of node.children || []) walk(child) }
    walk(root); return results
  }
  return { app, root, find }
}

test('actual text tools navigate literal matches and find a phrase spanning a view page boundary', async () => {
  const lines = Array.from({ length: 161 }, (_, index) => 'line-' + index)
  lines[79] = 'boundary-first'; lines[80] = 'second-boundary'
  lines[160] = '<script>literal + needle needle</script>'
  const r = loadViewer({ text: lines.join('\n') })
  const original = Buffer.from(r.bytes)
  await r.api.loadPreview()
  assert.equal(r.api.textPageEntries.value.length, 3)
  r.api.textSearchQuery.value = 'boundary-first\nsecond-boundary'
  r.api.handleTextSearch()
  assert.equal(r.api.textSearchMatches.value.length, 1, 'pagination must not remove genuine source-text matches')
  assert.equal(r.api.textCurrentPage.value, 1)
  r.api.textSearchQuery.value = 'needle'; r.api.handleTextSearch()
  assert.equal(r.api.textSearchMatches.value.length, 2)
  assert.equal(r.api.textCurrentPage.value, 3)
  const first = r.api.textPageSegments.value.find(segment => segment.active)
  r.api.moveTextSearchMatch(1)
  const second = r.api.textPageSegments.value.find(segment => segment.active)
  assert.notEqual(first.startIndex, second.startIndex, 'next occurrence on the same page must target different characters')
  assert.equal(r.api.textPageSegments.value.map(segment => segment.text).join(''), lines[160])
  assert.equal(Buffer.compare(Buffer.from(r.bytes), original), 0)
  assert.deepEqual(r.calls.map(call => call[0]), ['metadata', 'preview'])
  assert.equal(r.api.registeredPreviewVisible.value, true)
})

test('transformed image, video and text reserve scrollable width and height for quarter turns', async () => {
  for (const kind of ['TEXT', 'IMAGE', 'VIDEO']) {
    const r = loadViewer({ kind })
    await r.api.loadPreview()
    assert.ok(r.api.flatPreviewContainerRef, `${kind} needs a measured scrollable view surface`)
    r.api.flatPreviewContainerRef.value = Object.assign(new r.Element(), { clientWidth: 640 })
    r.api.flatPreviewStageRef.value = Object.assign(new r.Element(), { offsetWidth: 600, offsetHeight: 900 })
    await vue.nextTick()
    r.api.handleZoomIn(); r.api.handleRotateRight()
    assert.equal(r.api.flatPreviewViewportStyle.value.width, '1125px')
    assert.equal(r.api.flatPreviewViewportStyle.value.height, '750px')
    assert.match(r.api.flatPreviewTransformStyle.value.transform, /translate\(-50%, -50%\)/)
    assert.match(r.api.flatPreviewTransformStyle.value.transform, /90deg/)
    r.api.handleResetViewerTransform()
    assert.equal(r.api.viewerZoomPercent.value, 100)
    assert.equal(r.api.viewerRotationDegrees.value, 0)
    const oldObserver = r.observers.at(-1)
    r.props.controlledFileId = 'next-version'; await r.api.loadPreview()
    oldObserver.callback()
    assert.equal(r.api.flatPreviewViewportStyle.value.height, '0px', 'a queued old-size callback must not restore old geometry')
    r.api.flatPreviewStageRef.value = Object.assign(new r.Element(), { offsetWidth: 500, offsetHeight: 700 })
    await vue.nextTick()
    oldObserver.callback()
    assert.equal(r.api.flatPreviewViewportStyle.value.height, '700px')
    r.lifecycle.unmount(); await vue.nextTick()
    assert.equal(r.observers.every(observer => observer.disconnected), true)
  }
})

test('fresh text sources clear search and paging, validate jumps and never fetch a download', async () => {
  const r = loadViewer({ text: Array.from({ length: 162 }, (_, index) => `line ${index}`).join('\n') })
  await r.api.loadPreview()
  r.api.goToTextPage(3)
  for (const value of ['0', '4', '1.5', 'oops']) {
    r.api.textPageInput.value = value; r.api.handleTextPageJump()
    assert.equal(r.api.textCurrentPage.value, 3)
    assert.match(r.api.textNavigationError.value, /页码/)
  }
  r.api.textSearchQuery.value = 'line'; r.api.handleTextSearch()
  assert.equal(r.api.textSearchMatches.value.length, 162)
  r.api.textSearchQuery.value = 'absent'
  assert.equal(r.api.textSearchMatches.value.length, 0, 'input changes synchronously invalidate stale hits')
  r.api.handleTextSearch(); assert.match(r.api.textSearchMessage.value, /未找到/)
  r.props.controlledFileId = 'another-version'
  await r.api.loadPreview()
  assert.equal(r.api.textCurrentPage.value, 1)
  assert.equal(r.api.textSearchMatches.value.length, 0)
  assert.equal(r.api.textSearchQuery.value, '')
  for (const call of r.calls) assert.ok(['metadata', 'preview'].includes(call[0]))
  const denied = loadViewer({ deny: 'permission denied' }); await denied.api.loadPreview()
  assert.match(denied.api.errorMessage.value, /permission denied/)
  assert.deepEqual(denied.calls.map(call => call[0]), ['metadata'])
  const unsupported = loadViewer({ kind: 'DOWNLOAD_ONLY' }); await unsupported.api.loadPreview()
  assert.deepEqual(unsupported.calls.map(call => call[0]), ['metadata'])
  assert.equal(unsupported.api.registeredPreviewVisible.value, false)
})

test('production template and scoped styles compile without injecting text as HTML', () => {
  const { descriptor, errors } = parse(viewer)
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'full-format-viewer' })
  const template = compileTemplate({ source: descriptor.template.content, filename: 'index.vue', id: 'full-format-viewer', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const style = compileStyle({ source: descriptor.styles[0].content, id: 'full-format-viewer', scoped: true })
  assert.deepEqual(style.errors, [])
  assert.equal(viewer.includes('v-html'), false)
})

test('real text template renders escaped text, distinct active marks and a transformed watermark', async () => {
  const r = loadViewer({ text: '<script>needle NEEDLE a+b [x]</script>' })
  await r.api.loadPreview()
  r.api.textSearchQuery.value = 'needle'; r.api.handleTextSearch()
  const view = await renderViewer(r)
  assert.equal(view.find(node => node.tag === 'script').length, 0)
  assert.equal(view.find(node => node.tag === 'mark').length, 2)
  assert.equal(view.find(node => node.tag === 'mark' && node.dataset.activeMatch === 'true').length, 1)
  assert.equal(view.find(node => node.tag === 'mark' && node.dataset.activeMatch === 'true')[0].text, 'needle')
  r.api.moveTextSearchMatch(1); await vue.nextTick()
  assert.equal(view.find(node => node.tag === 'mark' && node.dataset.activeMatch === 'true')[0].text, 'NEEDLE')
  r.api.textSearchQuery.value = 'a+b'; r.api.handleTextSearch(); await vue.nextTick()
  assert.equal(r.api.textSearchMatches.value.length, 1, 'regex metacharacters remain literal')
  r.api.handleZoomIn(); r.api.handleRotateRight(); await vue.nextTick()
  const overlay = view.find(node => node.props?.['data-testid'] === 'protected-preview-watermark-overlay')
  assert.equal(overlay.length, 1)
  assert.match(overlay[0].parent.props.style.transform, /90deg/)
  view.app.unmount(); r.lifecycle.unmount()
})

test('image/video/audio templates keep watermarks and show registration only for matching media events', async () => {
  for (const kind of ['IMAGE', 'VIDEO', 'AUDIO']) {
    const r = loadViewer({ kind }); await r.api.loadPreview()
    const view = await renderViewer(r)
    assert.equal(r.api.registeredPreviewVisible.value, false)
    const tag = { IMAGE: 'img', VIDEO: 'video', AUDIO: 'audio' }[kind]
    const element = view.find(node => node.tag === tag)[0]
    assert.ok(element, kind + ' needs its actual template element')
    assert.equal(view.find(node => node.props?.['data-testid'] === 'protected-preview-watermark-overlay').length, 1)
    const eventElement = { dataset: { previewGeneration: String(r.api.renderVersion.value) }, getAttribute: () => r.api.mediaObjectUrl.value, currentSrc: r.api.mediaObjectUrl.value }
    r.api.handleMediaLoaded({ currentTarget: { ...eventElement, dataset: { previewGeneration: '0' } } })
    assert.equal(r.api.registeredPreviewVisible.value, false)
    r.api.handleMediaLoaded({ currentTarget: eventElement }); assert.equal(r.api.registeredPreviewVisible.value, true)
    r.api.handleMediaError({ currentTarget: eventElement }); assert.equal(r.api.registeredPreviewVisible.value, false)
    assert.match(r.api.errorMessage.value, /加载失败/)
    if (kind === 'VIDEO') {
      assert.match(element.props.controlslist, /nofullscreen/)
      assert.ok(Object.hasOwn(element.props, 'disablepictureinpicture'))
      assert.ok(Object.hasOwn(element.props, 'disableremoteplayback'))
    }
    assert.equal(r.api.canTransformPreview.value, kind !== 'AUDIO')
    view.app.unmount(); r.lifecycle.unmount()
    assert.deepEqual(r.revoked, ['blob:selected-body'])
  }
})

test('full-format viewer exposes text search and line pagination', () => {
  assert.match(viewer, /protected-viewer-text-tools/)
  assert.match(viewer, /文本正文关键字/)
  assert.match(viewer, /上一页/)
  assert.match(viewer, /下一页/)
  assert.match(viewer, /textSearchMatches/)
  assert.match(viewer, /textCurrentPage/)
})

test('video view keeps a protected transform surface', () => {
  const videoBranch = viewer.slice(viewer.indexOf("resolvedPreviewKind === 'VIDEO'"), viewer.indexOf("resolvedPreviewKind === 'AUDIO'"))
  assert.match(videoBranch, /protected-viewer-transform-controls/)
  assert.match(videoBranch, /handleZoomIn/)
  assert.match(videoBranch, /handleRotateRight/)
  assert.match(videoBranch, /watermarkBackground/)
})

test('all preview branches preserve explicit controlled states', () => {
  for (const kind of ['PDF', 'IMAGE', 'VIDEO', 'AUDIO', 'TEXT', 'OFFICE']) {
    assert.match(viewer, new RegExp(`resolvedPreviewKind === '${kind}'`), `${kind} branch`)
  }
  assert.match(viewer, /当前文件类型仅支持下载，不提供在线预览/)
  assert.match(viewer, /previewRegisteredAt/)
  assert.match(viewer, /protected-preview-corner-watermark/)
})

test('existing protection transform assertions execute in isolation without starting the E2E entrypoint', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../e2e/dcc-controlled-file-protection.contract.test.js'), 'utf8')
  const ast = ts.createSourceFile('protection-contract.js', source, ts.ScriptTarget.ES2022, true)
  const name = 'testPreviewTransformControlsAreBoundedAndTypeScoped'
  const declaration = ast.statements.find(node => ts.isFunctionDeclaration(node) && node.name?.text === name)
  assert.ok(declaration)
  vm.runInNewContext(declaration.getText(ast) + '\n' + name + '()', {
    readUtf8: file => fs.readFileSync(file, 'utf8'), viewerPath: path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/index.vue'),
    assert, assertContains: (text, token) => assert.ok(text.includes(token), 'Missing transform contract: ' + token)
  })
})
