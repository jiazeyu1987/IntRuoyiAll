const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

// Read-only review of the reading thread's current SFC; never imports its API/lifecycle or shared harness.
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/index.vue'), 'utf8')
const descriptor = parse(source).descriptor
const ast = ts.createSourceFile('current-viewer.ts', descriptor.scriptSetup.content, ts.ScriptTarget.ES2022, true)
const fingerprint = crypto.createHash('sha256').update(source.replace(/\r\n?/g, '\n'), 'utf8').digest('hex')
const declarations = new Map(ast.statements.filter(ts.isVariableStatement).flatMap(statement => Array.from(statement.declarationList.declarations)
  .filter(declaration => ts.isIdentifier(declaration.name)).map(declaration => [declaration.name.text, statement.getText(ast)])))
const get = name => { assert.ok(declarations.has(name), 'production declaration missing: ' + name); return declarations.get(name) }
const elements = root => {
  const result = []
  const walk = node => { if (node.type === 1) result.push(node); for (const child of node.children || []) walk(child) }
  walk(root); return result
}
const branch = kind => {
  const element = elements(descriptor.template.ast).find(node => node.tag === 'template' && node.props.some(prop => prop.type === 7 && ['if', 'else-if'].includes(prop.name) && prop.exp?.content === `resolvedPreviewKind === '${kind}'` + (kind === 'DOWNLOAD_ONLY' ? ' && !errorMessage' : '')))
  assert.ok(element, kind + ' branch missing'); return element
}
const has = (node, attr, value) => node.props.some(prop => prop.type === 6 && prop.name === attr && prop.value?.content === value)

test('current reading integration preserves pure display tool handlers and PDF byte copies', () => {
  const names = ['MIN_VIEWER_ZOOM_PERCENT', 'MAX_VIEWER_ZOOM_PERCENT', 'VIEWER_ZOOM_STEP_PERCENT', 'DEFAULT_VIEWER_ZOOM_PERCENT',
    'viewerZoomPercent', 'viewerRotationDegrees', 'viewerScale', 'previewTransform', 'resetViewerTransformState',
    'handleZoomOut', 'handleZoomIn', 'normalizeRotationDegrees', 'handleRotateRight', 'handleResetViewerTransform',
    'getPdfPageViewportStyle', 'clonePdfBytesForWorker']
  const c = { exports: {}, ...vue }
  vm.runInNewContext(ts.transpileModule(names.map(get).join('\n') + '\nexports.api={' + names.join(',') + '}', {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText, c)
  const api = c.exports.api
  for (let i = 0; i < 20; i++) api.handleZoomIn()
  assert.equal(api.viewerZoomPercent.value, 250)
  api.handleRotateRight()
  assert.equal(api.getPdfPageViewportStyle({ width: 400, height: 600 }).width, '1500px')
  assert.equal(api.getPdfPageViewportStyle({ width: 400, height: 600 }).height, '1000px')
  for (let i = 0; i < 20; i++) api.handleZoomOut()
  assert.equal(api.viewerZoomPercent.value, 50)
  api.handleResetViewerTransform(); assert.equal(api.viewerRotationDegrees.value, 0)
  assert.equal(api.viewerZoomPercent.value, 100)
  const original = new Uint8Array([1, 2, 3]), copy = api.clonePdfBytesForWorker(original)
  copy[0] = 99; assert.deepEqual(Array.from(original), [1, 2, 3])
})

test('all actual current format templates keep their watermarks and appropriate tools', () => {
  for (const kind of ['PDF', 'IMAGE', 'TEXT', 'AUDIO', 'VIDEO']) {
    assert.ok(elements(branch(kind)).some(node => has(node, 'data-testid', 'protected-preview-watermark-overlay')), kind + ' watermark removed')
    assert.ok(elements(branch(kind)).some(node => has(node, 'data-testid', 'protected-preview-corner-watermark')), kind + ' corner watermark removed')
  }
  for (const kind of ['PDF', 'IMAGE', 'TEXT', 'VIDEO']) assert.ok(elements(branch(kind)).some(node => has(node, 'data-testid', 'protected-viewer-transform-controls')), kind + ' view controls removed')
  assert.ok(elements(branch('PDF')).some(node => node.props.some(prop => prop.type === 7 && prop.name === 'on' && prop.exp?.content === 'handlePdfSearch')))
  assert.ok(elements(branch('TEXT')).some(node => node.props.some(prop => prop.type === 7 && prop.name === 'on' && prop.exp?.content === 'handleTextSearch')))
  const office = elements(branch('OFFICE')).find(node => node.tag === 'OnlyOfficeReadOnlyViewer')
  for (const name of ['watermark-background', 'watermark-detail', 'unavailable-reason']) assert.ok(office.props.some(prop => prop.type === 7 && prop.name === 'bind' && prop.arg?.content === name))
  assert.ok(office.props.some(prop => prop.type === 7 && prop.name === 'bind' && prop.arg?.content === 'key' && prop.exp?.content === 'renderVersion'))
  assert.equal(elements(branch('AUDIO')).some(node => has(node, 'data-testid', 'protected-viewer-transform-controls')), false)
})

test('video and unavailable rules survive reading wiring without turning downloads into previews', () => {
  const video = elements(branch('VIDEO')).find(node => node.tag === 'video')
  assert.ok(video.props.some(prop => prop.type === 6 && prop.name === 'controlslist' && prop.value?.content.includes('nofullscreen')))
  assert.ok(video.props.some(prop => prop.type === 6 && prop.name === 'disablepictureinpicture'))
  assert.ok(video.props.some(prop => prop.type === 6 && prop.name === 'disableremoteplayback'))
  const load = get('loadPreview')
  assert.ok(load.indexOf('if (resolvedPreviewUnavailableReason.value)') < load.indexOf('await resolvePreviewBlob('))
  assert.match(load, /resolvedPreviewKind\.value === 'OFFICE' \|\| resolvedPreviewKind\.value === 'DOWNLOAD_ONLY'[\s\S]*?return/)
  assert.ok(elements(branch('DOWNLOAD_ONLY')).some(node => has(node, 'description', '当前文件类型仅支持下载，不提供在线预览')))
  assert.doesNotMatch(descriptor.template.content, /v-html/)
})

test('reading is attached to successful content marking and existing failure cleanup, not display transforms', () => {
  const mark = get('markRegisteredPreviewVisible')
  assert.ok(mark.indexOf('registeredPreviewVisible.value = true') < mark.indexOf('startPreviewReading(generation)'))
  assert.match(get('clearRegisteredPreview'), /closePreviewReading\(\)/)
  assert.match(get('handleOfficeError'), /clearRegisteredPreview\(\)/)
  assert.match(get('handleMediaError'), /clearRegisteredPreview\(\)/)
  assert.match(get('handleMediaLoaded'), /previewGeneration/)
  for (const name of ['handleZoomIn', 'handleZoomOut', 'handleRotateRight', 'handleResetViewerTransform']) {
    assert.doesNotMatch(get(name), /reading|Reading|save|Save|fetch|request|\.post\(|\.put\(/)
  }
  assert.ok(elements(descriptor.template.ast).some(node => has(node, 'data-testid', 'preview-registered-at')))
})

test('records the exact read-only source fingerprint reviewed by this independent suite', t => {
  assert.match(fingerprint, /^[a-f0-9]{64}$/)
  t.diagnostic('Read-only viewer UTF-8/LF SHA256: ' + fingerprint)
})
