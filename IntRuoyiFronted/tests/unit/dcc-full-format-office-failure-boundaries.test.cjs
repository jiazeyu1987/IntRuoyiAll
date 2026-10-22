const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

// Executes the actual Office SFC with isolated SDK/script callbacks; no browser/service/files are opened.
function office({ pendingScript = false, failDestroy = false, duringDestroy, scriptWithoutDocsApi = false } = {}) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue'), 'utf8')
  const script = parse(source).descriptor.scriptSetup.content
  const props = vue.reactive({ baseUrl: 'https://office-a.example.test', documentUrl: 'https://preview.example.test/exact-a.docx?ticket=a', documentTitle: 'exact-a.docx', unavailableReason: '' })
  const configs = [], scripts = [], emitted = [], destroyed = [], lifecycle = {}
  const DocsAPI = { DocEditor: class {
    constructor(id, config) { this.id = configs.length + 1; configs.push(config) }
    destroyEditor() { destroyed.push(this.id); duringDestroy?.(props); if (failDestroy) throw new Error('SDK_CLOSE_FAILED') }
  } }
  const browserWindow = { ...(pendingScript ? {} : { DocsAPI, __dccOnlyOfficeScriptBaseUrl: props.baseUrl }), addEventListener() {}, removeEventListener() {} }
  const host = { exports: {}, ...vue, Error, window: browserWindow,
    document: { createElement: () => ({}), head: { appendChild: element => scripts.push(element) } },
    defineProps: () => props, defineEmits: () => (...args) => emitted.push(args),
    watch() {}, onMounted() {}, onBeforeUnmount: callback => { lifecycle.unmount = callback }
  }
  vm.runInNewContext(ts.transpileModule(script + '\nexports.api={mountEditor,errorMessage,get editorInstance(){return editorInstance}}', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return { api: host.exports.api, props, configs, scripts, emitted, destroyed, lifecycle, browserWindow,
    finishScript(index = scripts.length - 1, withoutDocsApi = scriptWithoutDocsApi) { if (!withoutDocsApi) browserWindow.DocsAPI = DocsAPI; scripts[index].onload() },
    installDocsApi() { browserWindow.DocsAPI = DocsAPI } }
}

test('remount SDK destruction failure becomes an explicit current error and cannot retain a dead editor', async () => {
  const r = office({ failDestroy: true })
  await r.api.mountEditor(); r.configs[0].events.onDocumentReady()
  r.props.documentUrl = 'https://preview.example.test/exact-b.docx?ticket=b'
  await assert.doesNotReject(r.api.mountEditor(), 'cleanup rejection must not escape the visible Office failure path')
  assert.match(r.api.errorMessage.value, /SDK_CLOSE_FAILED/)
  assert.equal(r.configs.length, 1, 'failed cleanup cannot be followed by a successful new editor')
  assert.equal(r.api.editorInstance, null)
  const errors = r.emitted.filter(event => event[0] === 'error')
  assert.equal(errors.length, 1, 'current initialization failure must reach the existing parent error handler')
  assert.equal(errors[0][1].documentUrl, r.props.documentUrl)
  assert.equal(errors[0][1].message, r.api.errorMessage.value)
  r.configs[0].events.onDocumentReady()
  assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 1)
  r.lifecycle.unmount()
  assert.deepEqual(r.destroyed, [1], 'already detached failed editor must not be destroyed twice')
})

test('an SDK loaded from service A cannot silently initialize a document configured for service B', async () => {
  const r = office({ pendingScript: true })
  const first = r.api.mountEditor(); r.finishScript(); await first
  assert.equal(r.configs.length, 1)
  r.props.baseUrl = 'https://office-b.example.test'
  r.props.documentUrl = 'https://preview.example.test/exact-b.docx?ticket=b'
  await r.api.mountEditor()
  assert.equal(r.configs.length, 1, 'SDK reuse must retain the real script service identity')
  assert.match(r.api.errorMessage.value, /服务.*(不一致|冲突|变化)/)
  assert.equal(r.api.editorInstance, null)
  assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 0)
})

test('a pending service A script cannot be reused as service B before either ready callback', async () => {
  const r = office({ pendingScript: true })
  const first = r.api.mountEditor()
  r.props.baseUrl = 'https://office-b.example.test'
  r.props.documentUrl = 'https://preview.example.test/exact-b.docx?ticket=b'
  const second = r.api.mountEditor()
  r.finishScript(); await Promise.all([first, second])
  assert.equal(r.configs.length, 0, 'old script readiness cannot become service B document success')
  assert.match(r.api.errorMessage.value, /服务.*(不一致|冲突|变化)/)
  assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 0)
  assert.equal(r.emitted.length, 1)
  assert.equal(r.emitted[0][0], 'error')
  assert.equal(r.emitted[0][1].documentUrl, r.props.documentUrl, 'only the current service conflict may notify the parent')
})

test('a genuinely failed script releases its cache so a later explicit service load can retry', async () => {
  const r = office({ pendingScript: true })
  const first = r.api.mountEditor(); r.scripts[0].onerror(); await first
  assert.match(r.api.errorMessage.value, /脚本加载失败/)
  assert.equal(r.browserWindow.__dccOnlyOfficeScriptPromise, undefined)
  r.props.baseUrl = 'https://office-b.example.test'
  const second = r.api.mountEditor()
  assert.equal(r.scripts.length, 2)
  assert.equal(r.scripts[1].src, 'https://office-b.example.test/web-apps/apps/api/documents/api.js')
  r.finishScript(); await second
  assert.equal(r.api.errorMessage.value, '')
  assert.equal(r.configs.length, 1)
  for (const key of ['edit', 'comment', 'review', 'download', 'print', 'copy']) assert.equal(r.configs[0].document.permissions[key], false)
  r.configs[0].events.onDocumentReady()
  const opened = r.emitted.filter(event => event[0] === 'opened')
  assert.equal(opened.length, 1)
  assert.equal(opened[0][1].documentUrl, r.props.documentUrl)
  assert.equal(r.emitted.filter(event => event[0] === 'error').length, 1, 'the original real script failure remains an error event')
  r.lifecycle.unmount()
})

test('same-service canonical URL reuse preserves the new exact document without loading another SDK', async () => {
  const r = office({ pendingScript: true })
  const first = r.api.mountEditor(); r.finishScript(); await first
  const old = r.configs[0]
  r.props.baseUrl += '/'
  r.props.documentUrl = 'https://preview.example.test/exact-next.docx?ticket=next'
  await r.api.mountEditor()
  assert.equal(r.scripts.length, 1)
  assert.equal(r.configs.length, 2)
  assert.equal(r.api.errorMessage.value, '')
  old.events.onDocumentReady(); assert.equal(r.emitted.length, 0)
  r.configs[1].events.onDocumentReady()
  assert.equal(r.emitted.length, 1)
  assert.equal(r.emitted[0][0], 'opened')
  assert.equal(r.emitted[0][1].documentUrl, r.props.documentUrl)
  assert.equal(r.configs[1].document.url, r.props.documentUrl)
  r.lifecycle.unmount(); assert.deepEqual(r.destroyed, [1, 2])
})

test('unidentified legacy ready SDK or in-flight promise cannot impersonate the requested service', async () => {
  for (const state of ['ready', 'pending']) {
    const r = office()
    delete r.browserWindow.__dccOnlyOfficeScriptBaseUrl
    if (state === 'pending') {
      const unidentifiedSdk = r.browserWindow.DocsAPI
      r.browserWindow.DocsAPI = undefined
      r.browserWindow.__dccOnlyOfficeScriptPromise = Promise.resolve().then(() => { r.browserWindow.DocsAPI = unidentifiedSdk })
    }
    await r.api.mountEditor()
    assert.equal(r.configs.length, 0, state + ' cache without provenance must not become a document success')
    assert.match(r.api.errorMessage.value, /服务.*(未记录|无法核验|缺少)/)
    assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 0)
    assert.equal(r.scripts.length, 0, 'do not silently load over an unidentified cached SDK')
  }
})

test('asynchronous SDK onError retains service and cleanup failures in one visible parent event', async () => {
  const r = office({ failDestroy: true })
  await r.api.mountEditor()
  const config = r.configs[0]
  config.events.onDocumentReady()
  assert.doesNotThrow(() => config.events.onError({ data: { errorCode: -4, errorDescription: 'CURRENT_SERVICE_FAILED' } }), 'post-mount SDK cleanup failure must not escape the callback')
  assert.match(r.api.errorMessage.value, /CURRENT_SERVICE_FAILED/)
  assert.match(r.api.errorMessage.value, /SDK_CLOSE_FAILED/)
  const errors = r.emitted.filter(event => event[0] === 'error')
  assert.equal(errors.length, 1)
  assert.equal(errors[0][1].documentUrl, r.props.documentUrl)
  assert.equal(errors[0][1].message, r.api.errorMessage.value)
  assert.equal(r.api.editorInstance, null)
  config.events.onDocumentReady(); config.events.onError({ data: { errorDescription: 'DUPLICATE_OLD_ERROR' } })
  assert.equal(r.emitted.length, 2, 'one opened then one failure only')
  r.lifecycle.unmount(); assert.deepEqual(r.destroyed, [1])
})

test('a source changed during asynchronous error cleanup cannot receive the old failure', async () => {
  const r = office({ failDestroy: true, duringDestroy: props => { props.documentUrl = 'https://preview.example.test/new-source.docx?ticket=new' } })
  await r.api.mountEditor()
  const config = r.configs[0]; config.events.onDocumentReady()
  assert.doesNotThrow(() => config.events.onError({ data: { errorDescription: 'OLD_SOURCE_FAILED' } }))
  assert.equal(r.api.errorMessage.value, '')
  assert.equal(r.emitted.filter(event => event[0] === 'error').length, 0)
  assert.equal(r.api.editorInstance, null)
  r.lifecycle.unmount(); assert.deepEqual(r.destroyed, [1])
})

test('unmount during asynchronous SDK cleanup rejects late failure state and duplicate destruction', async () => {
  let r
  r = office({ failDestroy: true, duringDestroy: () => r.lifecycle.unmount() })
  await r.api.mountEditor()
  const config = r.configs[0]; config.events.onDocumentReady()
  assert.doesNotThrow(() => config.events.onError({ data: { errorDescription: 'UNMOUNTED_SOURCE_FAILED' } }))
  assert.equal(r.api.errorMessage.value, '')
  assert.equal(r.emitted.filter(event => event[0] === 'error').length, 0)
  config.events.onDocumentReady()
  assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 1)
  assert.equal(r.api.editorInstance, null)
  assert.deepEqual(r.destroyed, [1])
})

test('script onload without DocsAPI is an explicit parent error, releases cache, and permits same-URL retry', async () => {
  const r = office({ pendingScript: true, scriptWithoutDocsApi: true })
  const first = r.api.mountEditor(); r.finishScript(0); await first
  assert.equal(r.configs.length, 0)
  assert.match(r.api.errorMessage.value, /接口未就绪/)
  assert.equal(r.emitted.length, 1)
  assert.equal(r.emitted[0][0], 'error')
  assert.equal(r.emitted[0][1].documentUrl, r.props.documentUrl)
  assert.equal(r.browserWindow.__dccOnlyOfficeScriptPromise, undefined)
  assert.equal(r.browserWindow.__dccOnlyOfficeScriptBaseUrl, undefined)
  const retry = r.api.mountEditor()
  assert.equal(r.scripts.length, 2)
  r.finishScript(1, false); await retry
  assert.equal(r.configs.length, 1)
  assert.equal(r.api.errorMessage.value, '')
  r.configs[0].events.onDocumentReady()
  assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 1)
  r.lifecycle.unmount()
})

test('script onload with a non-constructible SDK interface releases that invalid cache before same-URL retry', async () => {
  for (const invalid of [{}, () => ({})]) {
    const r = office({ pendingScript: true, scriptWithoutDocsApi: true })
    const first = r.api.mountEditor()
    r.browserWindow.DocsAPI = { DocEditor: invalid }
    r.finishScript(0); await first
    assert.equal(r.configs.length, 0)
    assert.equal(r.emitted[0][0], 'error')
    assert.equal(r.browserWindow.__dccOnlyOfficeScriptPromise, undefined)
    assert.equal(r.browserWindow.__dccOnlyOfficeScriptBaseUrl, undefined)
    assert.equal(r.browserWindow.DocsAPI, undefined, 'invalid owned SDK must not poison the next explicit load')
    const retry = r.api.mountEditor()
    assert.equal(r.scripts.length, 2)
    r.installDocsApi(); r.finishScript(1); await retry
    assert.equal(r.configs.length, 1)
    assert.equal(r.configs[0].document.url, r.props.documentUrl)
    assert.equal(r.configs[0].editorConfig.mode, 'view')
    r.lifecycle.unmount()
  }
})

test('unmounted script onload failure releases the shared cache without an old success or error event', async () => {
  const r = office({ pendingScript: true, scriptWithoutDocsApi: true })
  const pending = r.api.mountEditor()
  r.lifecycle.unmount()
  r.finishScript(0); await pending
  assert.equal(r.configs.length, 0)
  assert.equal(r.emitted.length, 0)
  assert.equal(r.api.errorMessage.value, '')
  assert.equal(r.browserWindow.__dccOnlyOfficeScriptPromise, undefined)
  assert.equal(r.browserWindow.__dccOnlyOfficeScriptBaseUrl, undefined)
})

test('same-URL pending remount reports only the current SDK readiness failure and then retries the exact URL', async () => {
  const r = office({ pendingScript: true, scriptWithoutDocsApi: true })
  const first = r.api.mountEditor(), current = r.api.mountEditor()
  assert.equal(r.scripts.length, 1)
  r.finishScript(0); await Promise.all([first, current])
  assert.equal(r.configs.length, 0)
  assert.equal(r.emitted.length, 1, 'only the current generation can report shared loader failure')
  assert.equal(r.emitted[0][0], 'error')
  assert.equal(r.emitted[0][1].documentUrl, r.props.documentUrl)
  assert.equal(r.browserWindow.__dccOnlyOfficeScriptPromise, undefined)
  const retry = r.api.mountEditor()
  r.finishScript(1, false); await retry
  r.configs[0].events.onDocumentReady()
  assert.equal(r.configs[0].document.url, r.props.documentUrl)
  assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 1)
  r.lifecycle.unmount()
})
