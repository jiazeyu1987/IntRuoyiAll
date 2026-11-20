// Runtime unit tests: real Vue component/watchers/template, PDF and browser host interfaces stubbed.
const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate, compileStyle } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/dcc/controlled-file/controlled-upload/ControlledUploadPreview.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
const script = compileScript(descriptor, { id: 'controlled-upload-preview-runtime' })
const template = compileTemplate({ source: descriptor.template.content, filename, id: 'controlled-upload-preview-runtime',
  compilerOptions: { bindingMetadata: script.bindings } })
assert.deepEqual(template.errors, [])
const compiled = ts.transpileModule(script.content.replace(/import\.meta\.env\.BASE_URL/g, "'/'") + '\n' + template.code,
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText

const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
async function flush() {
  for (let i = 0; i < 25; i++) await Promise.resolve()
  await vue.nextTick()
}
function pdfModel(texts, { manual = false } = {}) {
  const renders = [], reads = [], lookups = [], gates = new Map()
  const model = { renders, reads, lookups, gates, manual, destroyed: 0 }
  model.document = {
    numPages: texts.length,
    async getPage(number) {
      lookups.push(number)
      if (gates.has(number)) await gates.get(number).promise
      return {
        getViewport: ({ scale }) => ({ width: 600 * scale, height: 900 * scale }),
        async getTextContent() {
          reads.push(number)
          return { items: [{ str: texts[number - 1] }] }
        },
        render({ canvasContext, viewport }) {
          const completion = deferred()
          const task = { ...completion, number, viewport, target: canvasContext.canvas, cancellations: 0,
            cancel() { this.cancellations++ } }
          renders.push(task)
          if (!model.manual) completion.resolve()
          return task
        }
      }
    }
  }
  return model
}
async function mount(models = []) {
  const elements = [], revoked = [], destroyed = [], errors = [], pendingModels = [...models]
  function element(type, text = '') {
    const node = { type, text, children: [], props: {}, style: {}, parent: null, clientWidth: 632, scrollTop: 0,
      querySelector: () => null, querySelectorAll: () => [], focus() {} }
    if (type === 'canvas') {
      const context = { canvas: node, scale() {}, drawImage(source) { node.drawn = source } }
      node.getContext = () => context
    }
    elements.push(node)
    return node
  }
  const document = { createElement: element, activeElement: null }
  const context = { exports: {}, Error, TextDecoder, Uint8Array, console, document,
    window: { document, devicePixelRatio: 1 },
    ResizeObserver: class { observe() {} disconnect() { destroyed.push('observer') } },
    URL: { createObjectURL: () => `blob:unit-${revoked.length}`, revokeObjectURL: url => revoked.push(url) },
    require(name) {
      if (name === 'vue') return vue
      if (name.endsWith('pdf.min.mjs')) return { GlobalWorkerOptions: {}, getDocument() {
        const model = pendingModels.shift()
        assert(model, 'PDF loader requires an explicit unit fixture')
        return { promise: model.load ? model.load.promise : Promise.resolve(model.document),
          destroy() { model.destroyed++; return Promise.resolve() } }
      } }
      throw new Error('Unexpected dependency: ' + name)
    }
  }
  vm.runInNewContext(compiled, context, { filename })
  const component = context.exports.default
  component.render = context.exports.render
  const renderer = vue.createRenderer({
    createElement: element, createText: text => element('#text', text), createComment: text => element('#comment', text),
    insert(node, parent, anchor) {
      if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
      node.parent = parent
      const index = anchor ? parent.children.indexOf(anchor) : -1
      if (index < 0) parent.children.push(node)
      else parent.children.splice(index, 0, node)
    },
    remove(node) { if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1); node.parent = null },
    setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text; node.children = [] },
    parentNode: node => node.parent,
    nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1],
    patchProp(node, key, _old, value) { node.props[key] = value },
    querySelector: () => null
  })
  const props = vue.reactive({ file: undefined })
  const app = renderer.createApp({ render: () => vue.h(component, props) })
  app.config.errorHandler = error => errors.push(error)
  for (const name of ['el-button', 'el-input', 'el-input-number']) {
    app.component(name, { props: ['disabled', 'modelValue'], setup: (_props, { slots }) => () => vue.h(name, slots.default?.()) })
  }
  const host = element('root')
  const root = app.mount(host)
  const state = root.$.subTree.component.setupState
  await flush()
  const visit = (node, predicate) => [ ...(predicate(node) ? [node] : []), ...node.children.flatMap(child => visit(child, predicate)) ]
  return { app, props, state, host, elements, revoked, destroyed, errors,
    nodes: predicate => visit(host, predicate),
    text: () => visit(host, node => node.type !== '#comment').map(node => node.text).join(''),
    async select(file) { props.file = file; await flush() },
    async close() { app.unmount(); await flush(); assert.deepEqual(errors, []) }
  }
}
const file = (name, contents = 'unit fixture') => new File([contents], name)

test('PDF cross-page search uses actual component state and navigates each match', async () => {
  const model = pdfModel(['first page', 'Needle and needle', 'last needle'])
  const h = await mount([model])
  try {
    await h.select(file('search.pdf'))
    assert.equal(h.state.ready, true)
    assert.equal(h.state.pageCount, 3)
    h.state.query = 'needle'
    await flush()
    await h.state.search()
    await flush()
    assert.deepEqual(model.reads, [1, 2, 3])
    assert.equal(h.state.matches.length, 3)
    assert.equal(h.state.pageNumber, 2)
    assert.match(h.text(), /第 1 \/ 3 处（第 2 页）/)
    h.state.navigateMatch(1); await flush()
    assert.equal(h.state.pageNumber, 2)
    h.state.navigateMatch(1); await flush()
    assert.equal(h.state.pageNumber, 3)
    h.state.navigateMatch(1); await flush()
    assert.equal(h.state.pageNumber, 2)
    h.state.query = 'absent'; await flush(); await h.state.search()
    assert.equal(h.state.matches.length, 0)
    assert.match(h.text(), /未找到匹配正文/)
  } finally { await h.close() }
})

test('rapid PDF page and zoom changes cancel older renders; late completion cannot overwrite the canvas', async () => {
  const model = pdfModel(['one', 'two', 'three'], { manual: true })
  const h = await mount([model])
  try {
    await h.select(file('race.pdf'))
    model.renders[0].resolve(); await flush()
    const visible = h.nodes(node => node.type === 'canvas')[0]
    h.state.pageNumber = 2; await flush()
    const second = model.renders.at(-1)
    h.state.changeZoom(25); await flush()
    const zoomed = model.renders.at(-1)
    h.state.pageNumber = 3; await flush()
    const latest = model.renders.at(-1)
    assert(second.cancellations > 0)
    assert(zoomed.cancellations > 0)
    assert.notEqual(second.target, latest.target)
    assert(zoomed.viewport.width > model.renders[0].viewport.width)
    latest.resolve(); await flush()
    assert.equal(visible.drawn, latest.target)
    second.resolve(); zoomed.reject(new Error('RenderingCancelledException')); await flush()
    assert.equal(visible.drawn, latest.target)
    assert.equal(h.state.pageNumber, 3)
    assert.equal(h.state.error, '')
    assert.equal(h.state.busy, false)
  } finally { await h.close() }
})

test('slow page lookup cannot start a render after a newer page has finished', async () => {
  const model = pdfModel(['one', 'two', 'three'])
  const h = await mount([model])
  try {
    await h.select(file('lookup.pdf'))
    model.gates.set(2, deferred())
    h.state.pageNumber = 2; await flush()
    h.state.pageNumber = 3; await flush()
    const count = model.renders.length
    const visible = h.nodes(node => node.type === 'canvas')[0], last = visible.drawn
    model.gates.get(2).resolve(); await flush()
    assert.equal(model.renders.length, count)
    assert.equal(visible.drawn, last)
  } finally { await h.close() }
})

test('switch/remove releases images and PDF tasks, resets errors/search/zoom, and ignores stale PDF loads', async () => {
  const pending = pdfModel(['old']); pending.load = deferred()
  const broken = pdfModel([]); broken.load = deferred()
  const h = await mount([pending, broken])
  try {
    await h.select(file('old.pdf'))
    await h.select(file('new.txt', 'new safe body'))
    pending.load.resolve(pending.document); await flush()
    assert.equal(pending.destroyed, 1)
    assert.equal(pending.renders.length, 0)
    assert.equal(h.state.kind, 'text')
    assert.equal(h.state.ready, true)
    await h.select(file('broken.pdf'))
    broken.load.reject(new Error('Invalid PDF structure')); await flush()
    assert.match(h.text(), /正文预览失败：Invalid PDF structure/)
    assert.equal(h.state.ready, false)
    await h.select(file('safe.txt', 'safe'))
    assert.equal(h.state.error, '')
    h.state.query = 'safe'; await flush(); await h.state.search(); h.state.changeZoom(25)
    await h.select(file('image.png'))
    const url = h.state.imageUrl
    await h.select(undefined)
    assert.deepEqual(h.revoked, [url])
    assert.equal(h.state.pageCount, 0)
    assert.equal(h.state.pageNumber, 1)
    assert.equal(h.state.zoom, 100)
    assert.equal(h.state.query, '')
    assert.equal(h.state.matches.length, 0)
    assert.equal(h.state.error, '')
    assert.equal(h.state.ready, false)
    assert.equal(h.state.busy, false)
    assert.match(h.text(), /选择受控主文件后查看正文/)
  } finally { await h.close() }
})

test('TXT renders markup as safe text, searches across real pages, highlights matches and scales', async () => {
  const h = await mount()
  try {
    const lines = Array.from({ length: 121 }, (_, i) => i === 75 ? '<script>alert(1)</script> needle' : `line ${i}`)
    await h.select(file('body.txt', lines.join('\r\n')))
    assert.equal(h.state.pageCount, 3)
    h.state.query = 'needle'; await flush(); await h.state.search(); await flush()
    assert.equal(h.state.pageNumber, 2)
    assert.match(h.text(), /<script>alert\(1\)<\/script>/)
    assert.equal(h.nodes(node => node.type === 'script').length, 0)
    assert.equal(h.nodes(node => node.type === 'mark').length, 1)
    assert.equal(h.nodes(node => node.type === 'mark')[0].text, 'needle')
    h.state.changeZoom(25); await flush()
    assert.equal(h.nodes(node => node.type === 'pre')[0].props.style.fontSize, '17.5px')
    h.state.fitWidth(); await flush()
    assert.equal(h.state.zoom, 100)
    await h.select(file('bad.txt', Uint8Array.of(0x81, 0x81)))
    assert.match(h.text(), /TXT 编码无效/)
    await h.select(file('valid.txt', 'valid again'))
    assert.equal(h.state.error, '')
    assert.match(h.text(), /valid again/)
  } finally { await h.close() }
})

test('real DOCX renderer displays document pages locally, supports navigation/search/zoom and reports corrupt files', async () => {
  const { createRequire } = require('node:module')
  const { build } = createRequire(require.resolve('vite'))('esbuild')
  const JSZip = createRequire(require.resolve('docx-preview'))('jszip')
  const { chromium } = require('playwright')
  const zip = new JSZip()
  zip.file('[Content_Types].xml', '<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>')
  zip.file('_rels/.rels', '<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>')
  zip.file('word/document.xml', '<?xml version="1.0"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>第一页真实正文</w:t></w:r></w:p><w:p><w:r><w:br w:type="page"/></w:r></w:p><w:p><w:r><w:t>第二页可搜索证据</w:t></w:r></w:p><w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body></w:document>')
  const bytes = await zip.generateAsync({ type: 'nodebuffer' })
  const bundle = await build({
    stdin: { contents: `import {createApp,h,ref} from 'vue'; import ElementPlus from 'element-plus'; import Preview from ${JSON.stringify(filename)};
      const file=ref(); createApp({setup:()=>()=>h('main',[h('input',{type:'file',onChange:event=>file.value=event.target.files[0]}),h('button',{onClick:()=>file.value=undefined},'移除主文件'),h(Preview,{file:file.value})])}).use(ElementPlus).mount('#app')`,
      resolveDir: path.resolve(__dirname, '../..'), loader: 'js' },
    bundle: true, write: false, format: 'iife', platform: 'browser', define: { 'import.meta.env.BASE_URL': "'/'", 'import.meta.url': '"about:blank"', 'process.env.NODE_ENV': '"production"' },
    plugins: [{ name: 'real-preview-sfc', setup(builder) {
      builder.onLoad({ filter: /ControlledUploadPreview\.vue$/ }, () => ({
        contents: compileScript(descriptor, { id: 'browser-docx-unit', inlineTemplate: true }).content,
        loader: 'ts', resolveDir: path.dirname(filename)
      }))
    } }]
  })
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage({ viewport: { width: 1200, height: 900 } })
    const network = [], browserErrors = []
    page.on('request', request => { if (!request.url().startsWith('data:')) network.push(request.url()) })
    page.on('pageerror', error => browserErrors.push(error.message))
    await page.setContent('<div id="app"></div>')
    await page.addStyleTag({ content: fs.readFileSync(require.resolve('element-plus/dist/index.css'), 'utf8') +
      compileStyle({ source: descriptor.styles[0].content, filename, id: 'browser-docx-unit' }).code })
    await page.addScriptTag({ content: bundle.outputFiles[0].text })
    await page.locator('input[type="file"]').setInputFiles({ name: '真实两页.docx', mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', buffer: bytes })
    await page.locator('section.upload-docx').first().waitFor()
    assert.equal(await page.locator('section.upload-docx').count(), 2)
    assert.equal(await page.locator('section.upload-docx').first().isVisible(), true)
    assert.match(await page.locator('section.upload-docx').first().textContent(), /第一页真实正文/)
    assert.equal(await page.locator('section.upload-docx').nth(1).isVisible(), false)
    await page.getByRole('button', { name: '下一页', exact: true }).click()
    assert.equal(await page.locator('section.upload-docx').nth(1).isVisible(), true)
    await page.getByRole('button', { name: '上一页', exact: true }).click()
    await page.getByRole('textbox', { name: '正文搜索关键词' }).fill('证据')
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await page.getByText('第 1 / 1 处（第 2 页）', { exact: true }).waitFor({timeout: 3000}).catch(async error => {
      throw new Error(JSON.stringify({text: await page.locator('.preview-search').textContent(), value: await page.getByRole('textbox', { name: '正文搜索关键词' }).inputValue(), html: await page.locator('.preview-search').innerHTML(), errors: browserErrors}), {cause: error})
    })
    assert.equal(await page.locator('section.upload-docx').nth(1).isVisible(), true)
    await page.getByRole('textbox', { name: '正文搜索关键词' }).fill('第一页')
    await page.getByRole('textbox', { name: '正文搜索关键词' }).press('Enter')
    await page.getByText('第 1 / 1 处（第 1 页）', { exact: true }).waitFor()
    assert.equal(await page.locator('section.upload-docx').first().isVisible(), true)
    const initial = await page.locator('.word-document').evaluate(node => node.style.zoom)
    await page.getByRole('button', { name: '放大正文', exact: true }).click()
    assert(Number(await page.locator('.word-document').evaluate(node => node.style.zoom)) > Number(initial))
    await page.getByRole('button', { name: '移除主文件', exact: true }).click()
    await page.getByText('选择受控主文件后查看正文', { exact: true }).waitFor()
    assert.equal(await page.locator('section.upload-docx').count(), 0)
    await page.locator('input[type="file"]').setInputFiles({ name: '损坏.docx', mimeType: 'application/octet-stream', buffer: Buffer.from('not a zip') })
    await page.getByRole('alert').waitFor()
    assert.match(await page.getByRole('alert').textContent(), /正文预览失败/)
    await page.locator('input[type="file"]').setInputFiles({ name: '恢复.docx', mimeType: 'application/octet-stream', buffer: bytes })
    await page.locator('section.upload-docx').first().waitFor()
    assert.equal(await page.getByRole('alert').count(), 0)
    await page.locator('input[type="file"]').setInputFiles({ name: '旧格式.doc', mimeType: 'application/msword', buffer: Buffer.from('legacy') })
    await page.getByText('旧版 DOC 格式不支持本地预览，请另存为 DOCX 后选择文件。', { exact: true }).waitFor()
    assert.deepEqual(network, [], 'Local preview must not upload or request document bytes')
    assert.deepEqual(browserErrors, [])
  } finally { await browser.close() }
})
