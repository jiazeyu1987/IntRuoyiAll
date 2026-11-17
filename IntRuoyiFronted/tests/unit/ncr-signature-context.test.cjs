const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
function load(file, imports = {}) {
  const ast = ts.createSourceFile(file, read(file), ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(n => !ts.isImportDeclaration(n)).map(n => n.getText(ast)).join('\n')
  const js = ts.transpileModule(body, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const exports = {}; Function('exports', ...Object.keys(imports), js)(exports, ...Object.values(imports)); return exports
}
const { buildActiveOrderSignatureRequest: build } = load('src/api/mes/pro/edhr/activeOrderSignature.ts')
const { createActiveOrderSignatureEvidenceViewer: viewer } = load('src/views/mes/pro/processpool/components/activeOrderSignatureEvidenceViewer.ts', { reactive: value => value })
function context(props) {
  const { descriptor } = require('vue/compiler-sfc').parse(read('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'))
  const ast = ts.createSourceFile('panel.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.filter(ts.isVariableStatement).flatMap(s => [...s.declarationList.declarations]).find(n => n.name.getText(ast) === 'signatureEvidenceContext')
  const js = ts.transpileModule('const result = ' + declaration.initializer.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function('props', 'computed', 'auditScopeTypeValue', 'auditScopeIdValue', js + '\nreturn result')(props, cb => cb(), { value: 'TEAM' }, { value: props.detail?.activeOrderId })
}
test('review requests preserve exact IDs and reject missing or malformed identities', () => {
  assert.deepEqual(build('NCR_REVIEW', '9223372036854775806', '742'), { url: '/mes/pro/edhr-nonconformance-review/signature/get', params: { reviewId: '9223372036854775806', signatureId: '742' } })
  for (const id of [undefined, null, 0, -1, '1.2', {}, { activeOrderId: 409 }, 9007199254740992]) assert.throws(() => build('NCR_REVIEW', id, 742))
})
test('review page supplies explicit signature context independent of audit context', () => {
  const source = read('src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
  assert.match(source, /signature-scope-type="NCR_REVIEW"/)
  assert.match(source, /:signature-scope-id="orderDetailReview\?\.id"/)
  assert.deepEqual(context({ detail: { activeOrderId: 409 }, signatureScopeType: 'NCR_REVIEW', signatureScopeId: 501 }), { scope: 'NCR_REVIEW', identity: 501, activeOrderId: 409 })
})
test('missing review identity does not send a team signature request', async () => {
  const props = { detail: { activeOrderId: 409 }, signatureScopeType: 'NCR_REVIEW' }
  assert.equal(context(props), undefined)
  let calls = 0; const state = viewer(() => context(props), async () => { calls++ })
  await state.open(742); assert.equal(calls, 0); assert.match(state.state.error, /缺少正式签名查询身份/)
})
test('another cycle and stale review responses cannot populate evidence', async () => {
  let current = { scope: 'NCR_REVIEW', identity: 501, activeOrderId: 409 }; let resolve
  const state = viewer(() => current, () => new Promise(r => { resolve = r }))
  const pending = state.open(742); current = { ...current, identity: 502, activeOrderId: 410 }
  resolve({ activeOrderId: 409, evidence: { id: 742 }, verification: { signatureId: 742 } }); await pending
  assert.equal(state.state.selected, undefined)
  const wrong = viewer(() => current, async () => ({ activeOrderId: 409, evidence: { id: 742 }, verification: { signatureId: 742 } }))
  await wrong.open(742); assert.match(wrong.state.error, /不一致/)
})
