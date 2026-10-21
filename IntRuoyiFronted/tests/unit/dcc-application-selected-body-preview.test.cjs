const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { ref, computed } = require('vue')
const { parse } = require('vue/compiler-sfc')
const read = path => fs.readFileSync(path, 'utf8')
const { descriptor } = parse(read('src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue'))
const ast = ts.createSourceFile('panel.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
function declaration(name, source = ast) {
  const statement = source.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(source) === name))
  assert.ok(statement, `Production declaration ${name} must exist`)
  return statement.getText(source)
}
function execute(source, context = {}) {
  const env = { exports: {}, ...context }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, env)
  return env.exports
}
const body = execute(read('src/api/dcc/controlledFile/bodyReadContext.ts'), { TextEncoder, TextDecoder, btoa, atob })
const previewAst = ts.createSourceFile('preview.ts', read('src/api/common/filePreview.ts'), ts.ScriptTarget.Latest, true)
const builder = execute(declaration('buildDccControlledFilePreviewSource', previewAst), body).buildDccControlledFilePreviewSource
const ID = '2054545668044084323', OTHER = '2054545668044084326'
function panel(file = { id: ID, versionNo: 'A/1-1', status: 'WORKING' }, selected = ID) {
  const env = { selectedApplicationFile: ref(file), selectedIterationId: ref(selected), selectionLoading: ref(false), previewSource: ref(), error: ref(''), buildDccControlledFilePreviewSource: builder }
  const { openSelectedBodyPreview } = execute(declaration('openSelectedBodyPreview') + '\nexports.openSelectedBodyPreview = openSelectedBodyPreview', env)
  return { ...env, open: openSelectedBodyPreview }
}
test('rendered selected-body control passes the exact source to the protected viewer', () => {
  assert.match(descriptor.template.content, /@click="openSelectedBodyPreview\(bodyIterationId\)"/)
  const viewer = descriptor.template.content.match(/<ProtectedPdfViewer\b[^>]*>/)?.[0]
  assert.match(viewer, /:preview-source="previewSource"/)
  assert.doesNotMatch(viewer, /:controlled-file-id/)
})
for (const [id, versionNo] of [[ID, 'A/1-1'], [OTHER, 'A/2-1']]) {
  test(`selected draft ${id} uses its own exact AUTHORING version`, () => {
    const p = panel({ id, versionNo, status: 'WORKING' }, id)
    p.open(id)
    assert.deepEqual(JSON.parse(JSON.stringify(p.previewSource.value)), {
      type: 'DCC_CONTROLLED_FILE', controlledFileId: id,
      bodyReadContext: { intent: 'AUTHORING', controlledFileId: id, expectedVersionNo: versionNo }
    })
    assert.equal(p.error.value, '')
  })
}
for (const reason of ['loading', 'missing', 'foreign', 'stale-selection', 'missing-version', 'invalid-version']) {
  test(`${reason} fails visibly without retaining the previous preview`, () => {
    const p = panel(); p.open(ID); assert.ok(p.previewSource.value)
    if (reason === 'loading') p.selectionLoading.value = true
    if (reason === 'missing') p.selectedApplicationFile.value = undefined
    if (reason === 'foreign') p.selectedApplicationFile.value.id = OTHER
    if (reason === 'stale-selection') p.selectedIterationId.value = OTHER
    if (reason === 'missing-version') delete p.selectedApplicationFile.value.versionNo
    if (reason === 'invalid-version') p.selectedApplicationFile.value.versionNo = ' '
    p.open(ID)
    assert.equal(p.previewSource.value, undefined)
    assert.ok(p.error.value)
  })
}
test('daily builder still defaults to DAILY', () => {
  assert.equal(builder(ID).bodyReadContext.intent, 'DAILY')
})
test('successful body preview does not clear an existing application preparation blocker', () => {
  const p = panel(); p.error.value = '审批配置尚未就绪'
  p.open(ID)
  assert.ok(p.previewSource.value)
  assert.equal(p.error.value, '审批配置尚未就绪')
})
test('closing the actual dialog clears the source and selection reset clears it before async reads', () => {
  const p = panel(); p.open(ID)
  const visible = execute(declaration('previewVisible') + '\nexports.visible = previewVisible', { ...p, computed }).visible
  assert.equal(visible.value, true)
  visible.value = false
  assert.equal(p.previewSource.value, undefined)
  const select = declaration('selectIteration')
  assert.ok(select.indexOf('previewSource.value = undefined') < select.indexOf('await '))
  const reset = ast.statements.find(s => ts.isExpressionStatement(s) && ts.isCallExpression(s.expression)
    && s.expression.expression.getText(ast) === 'watch' && s.getText(ast).includes('props.file.status'))
  assert.ok(reset?.getText(ast).includes('previewSource.value = undefined'))
})
