const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate, compileStyle } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue')
const source = fs.readFileSync(filename, 'utf8')

// Production SFC with isolated SDK callbacks/DOM metrics; no external Office/browser/API is started.
function office({ pendingScript = false, duringConstruction } = {}) {
  const descriptor = parse(source).descriptor
  const props = vue.reactive({ baseUrl: 'https://office.example.test', documentUrl: 'https://preview.example.test/exact.docx?ticket=a', documentTitle: 'exact.docx', unavailableReason: '' })
  const configs = [], created = [], destroyed = [], emitted = [], observers = [], scripts = [], lifecycle = {}
  class ResizeObserver {
    constructor(callback) { this.callback = callback; this.disconnected = false; observers.push(this) }
    observe(element) { this.element = element }
    disconnect() { this.disconnected = true }
  }
  const DocsAPI = { DocEditor: class {
    constructor(id, config) { this.id = created.length + 1; created.push(this); configs.push(config); duringConstruction?.(config) }
    destroyEditor() { destroyed.push(this.id) }
  } }
  const host = { exports: {}, ...vue, Error, ResizeObserver,
    window: { ...(pendingScript ? {} : { DocsAPI, __dccOnlyOfficeScriptBaseUrl: props.baseUrl }), addEventListener() {}, removeEventListener() {} },
    document: { createElement: () => ({}), head: { appendChild: element => scripts.push(element) } },
    defineProps: () => props, defineEmits: () => (...args) => emitted.push(args),
    watch: (input, callback, options) => { if (vue.isRef(input)) return vue.watch(input, callback, options) },
    onMounted() {}, onBeforeUnmount: callback => { lifecycle.unmount = callback }
  }
  const ast = ts.createSourceFile('office.ts', descriptor.scriptSetup.content, ts.ScriptTarget.ES2022, true)
  const names = ast.statements.filter(ts.isVariableStatement).flatMap(statement => Array.from(statement.declarationList.declarations)
    .filter(declaration => ts.isIdentifier(declaration.name) && declaration.name.text !== 'editorInstance').map(declaration => declaration.name.text))
  const code = descriptor.scriptSetup.content + '\nexports.api={' + names.join(',') + ',get editorInstance(){return editorInstance}}'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, host)
  return { api: host.exports.api, props, configs, created, destroyed, emitted, observers, scripts, lifecycle, descriptor,
    finishScript: () => { host.window.DocsAPI = DocsAPI; scripts[0].onload() } }
}

test('Office preserves a fixed native viewport and nonnegative scroll bounds for narrow/wide 720-high stages', async () => {
  for (const width of [320, 321, 1000]) {
    const r = office(); await r.api.mountEditor()
    r.api.stageRef.value = { clientWidth: width }; await vue.nextTick()
    assert.ok(r.api.viewportStyle, 'zoom/rotation need an explicit scrollable viewport')
    for (const zoom of [60, 100, 110, 200]) for (const degrees of [0, 90, 180, 270]) {
      r.api.previewZoomValue.value = zoom; r.api.previewRotationDegrees.value = degrees
      assert.equal(r.api.scalerStyle.value.width, `${width}px`, 'CSS zoom must not squeeze the native editor viewport')
      assert.equal(r.api.scalerStyle.value.height, '720px')
      const viewport = r.api.viewportStyle.value
      const quarterTurn = degrees % 180 !== 0
      assert.equal(viewport.width, `${Math.ceil((quarterTurn ? 720 : width) * zoom / 100)}px`)
      assert.equal(viewport.height, `${Math.ceil((quarterTurn ? width : 720) * zoom / 100)}px`)
      const paintedWidth = Number.parseFloat(viewport.width), paintedHeight = Number.parseFloat(viewport.height)
      const scale = zoom / 100, angle = degrees * Math.PI / 180
      const corners = [-1, 1].flatMap(x => [-1, 1].map(y => ({
        x: paintedWidth / 2 + scale * (x * width / 2 * Math.cos(angle) - y * 720 / 2 * Math.sin(angle)),
        y: paintedHeight / 2 + scale * (x * width / 2 * Math.sin(angle) + y * 720 / 2 * Math.cos(angle))
      })))
      const minX = Math.min(...corners.map(p => p.x)), minY = Math.min(...corners.map(p => p.y))
      const maxX = Math.max(...corners.map(p => p.x)), maxY = Math.max(...corners.map(p => p.y))
      assert.ok(minX >= -1e-8 && minX <= 0.5 + 1e-8, `${width}/${zoom}/${degrees}: unreachable left edge`)
      assert.ok(minY >= -1e-8 && minY <= 0.5 + 1e-8, `${width}/${zoom}/${degrees}: unreachable top edge`)
      assert.ok(maxX <= paintedWidth + 1e-8 && paintedWidth - maxX <= 0.5 + 1e-8)
      assert.ok(maxY <= paintedHeight + 1e-8 && paintedHeight - maxY <= 0.5 + 1e-8)
      assert.match(r.api.scalerStyle.value.transform, /translate\(-50%, -50%\)/)
      assert.match(r.api.scalerStyle.value.transform, new RegExp(`rotate\\(${degrees}deg\\)`))
    }
    assert.equal(r.configs.length, 1, 'view transforms must not remount, save or request another document')
    r.lifecycle.unmount()
  }
})

test('Office Word/cell/slide native tool configuration and read-only URL survive transforms', async () => {
  for (const [extension, type] of [['docx', 'word'], ['rtf', 'word'], ['xlsx', 'cell'], ['ods', 'cell'], ['pptx', 'slide'], ['odp', 'slide']]) {
    const r = office(); r.props.documentTitle = 'selected.' + extension; await r.api.mountEditor()
    const config = r.configs[0], before = JSON.stringify(config)
    assert.equal(config.documentType, type)
    assert.equal(config.document.fileType, extension)
    assert.equal(config.document.url, r.props.documentUrl)
    assert.equal(config.editorConfig.mode, 'view')
    for (const key of ['edit', 'comment', 'review', 'download', 'print', 'copy']) assert.equal(config.document.permissions[key], false)
    assert.equal(config.editorConfig.callbackUrl, undefined)
    assert.equal(config.events.onRequestSaveAs, undefined)
    assert.equal(config.editorConfig.customization.toolbar, undefined, 'native navigation/find toolbar must not be disabled')
    assert.equal(config.editorConfig.customization.leftMenu, undefined)
    r.api.increaseZoom(); r.api.rotatePreview(); r.api.resetView()
    assert.equal(JSON.stringify(config), before)
    assert.equal(r.configs.length, 1)
    r.lifecycle.unmount()
  }
})

test('old SDK rejection before a source watcher runs cannot overwrite the new source error state', async () => {
  for (const change of [props => { props.documentUrl += '-next' }, props => { props.baseUrl += '-next' }, props => { props.documentTitle = 'next.xlsx' }]) {
    const r = office({ pendingScript: true }), pending = r.api.mountEditor()
    change(r.props); r.scripts[0].onerror(); await pending
    assert.equal(r.api.errorMessage.value, '', 'old script error must not poison a new source before remount')
    assert.equal(r.configs.length, 0)
    assert.equal(r.emitted.length, 0)
  }
  const current = office({ pendingScript: true }), pending = current.api.mountEditor()
  current.scripts[0].onerror(); await pending
  assert.match(current.api.errorMessage.value, /脚本加载失败/)
})

test('base/title/unavailable changes invalidate late opened/error and awaited initialization before remount', async () => {
  const changes = [props => { props.baseUrl += '-next' }, props => { props.documentTitle = 'next.xlsx' }, props => { props.unavailableReason = 'denied' }]
  for (const change of changes) {
    const r = office(); await r.api.mountEditor(); change(r.props)
    r.configs[0].events.onDocumentReady()
    r.configs[0].events.onError({ data: { errorDescription: 'old source failure' } })
    assert.equal(r.emitted.length, 0, 'a changed exact source must not start/stop current parent reading')
    assert.equal(r.api.errorMessage.value, '')
    const pendingOffice = office({ pendingScript: true }), pending = pendingOffice.api.mountEditor()
    change(pendingOffice.props); pendingOffice.finishScript(); await pending
    assert.equal(pendingOffice.configs.length, 0, 'awaited old source cannot create a new editor')
  }
})

test('synchronous SDK onError during construction destroys the returned failed instance exactly once', async () => {
  const r = office({ duringConstruction: config => config.events.onError({ data: { errorDescription: 'constructor callback failed' } }) })
  await r.api.mountEditor()
  assert.match(r.api.errorMessage.value, /constructor callback failed/)
  assert.deepEqual(r.destroyed, [1], 'constructor result must not remain alive after its generation failed')
  assert.equal(r.api.editorInstance, null)
  r.configs[0].events.onDocumentReady(); r.configs[0].events.onError({ data: { errorDescription: 'late failure' } })
  assert.equal(r.emitted.length, 1)
  assert.equal(r.emitted[0][0], 'error')
  r.lifecycle.unmount(); assert.deepEqual(r.destroyed, [1])
})

test('old resize observations cannot change the replacement stage or unmounted view', async () => {
  const r = office(); await r.api.mountEditor()
  const oldStage = { clientWidth: 1000 }, currentStage = { clientWidth: 320 }
  r.api.stageRef.value = oldStage; await vue.nextTick()
  const oldObserver = r.observers[0]
  r.api.stageRef.value = currentStage; await vue.nextTick()
  assert.equal(r.api.stageWidth.value, 320)
  oldStage.clientWidth = 2400; oldObserver.callback()
  assert.equal(r.api.stageWidth.value, 320, 'queued old callback cannot replace current width')
  const currentObserver = r.observers.at(-1)
  r.lifecycle.unmount(); currentStage.clientWidth = 900; currentObserver.callback()
  assert.equal(r.api.stageWidth.value, 320, 'unmounted observer cannot restore state')
  assert.ok(r.observers.every(observer => observer.disconnected))
})

test('Office SFC compiles and places the native mount inside a scrollable virtual viewport under the watermark', () => {
  const descriptor = parse(source).descriptor, script = compileScript(descriptor, { id: 'office-review' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'office-review', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  assert.deepEqual(compileStyle({ source: descriptor.styles[0].content, id: 'office-review', scoped: true }).errors, [])
  const elements = []
  const walk = (node, ancestors = []) => { if (node.type === 1) elements.push({ node, ancestors }); for (const child of node.children || []) walk(child, node.type === 1 ? [...ancestors, node] : ancestors) }
  walk(descriptor.template.ast)
  const hasClass = (node, name) => node.props.some(prop => prop.type === 6 && prop.name === 'class' && prop.value?.content.split(' ').includes(name))
  const mount = elements.find(item => hasClass(item.node, 'onlyoffice-viewer-frame__mount'))
  for (const name of ['onlyoffice-viewer-frame__stage', 'onlyoffice-viewer-frame__scroll', 'onlyoffice-viewer-frame__viewport', 'onlyoffice-viewer-frame__scaler']) {
    assert.ok(mount.ancestors.some(node => hasClass(node, name)), 'native mount missing geometry ancestor: ' + name)
  }
  const scalerCss = /\.onlyoffice-viewer-frame__scaler\s*\{([^}]+)\}/.exec(descriptor.styles[0].content)?.[1]
  assert.ok(scalerCss)
  for (const rule of [/position:\s*absolute/, /top:\s*50%/, /left:\s*50%/]) assert.match(scalerCss, rule)
  assert.match(source, /\.onlyoffice-viewer-frame__scroll\s*\{[^}]*overflow:\s*auto/)
  assert.ok(elements.some(item => hasClass(item.node, 'onlyoffice-viewer-frame__overlay')))
  assert.ok(elements.some(item => hasClass(item.node, 'onlyoffice-viewer-frame__watermark')))
})
