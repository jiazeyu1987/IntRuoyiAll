const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const page = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', page.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)[1], ts.ScriptTarget.Latest, true)
const names = ['reviewValidationErrors', 'handleDispose']
const declarations = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
function harness() {
  const messages = [], writes = []
  const context = {
    computed, reviewTabOpen: ref(true), disposeLoading: ref(false), canDisposeSelectedReview: ref(true),
    selectedReviewReadOnly: ref(false), selectedReview: ref({ id: 109 }), reviewContextGeneration: 1,
    materialUploadsPending: ref(0), materialSelectionError: ref(''), reviewValidationRequested: ref(false),
    disposeForm: reactive({ reviewMaterialUrls: [], reviewOpinion: '', signaturePassword: '' }),
    errorText: ref(''),
    resolveErrorMessage: error => error.message,
    approvalInvalidation: { captureHandoffSourceSuccess: () => ({ session: { key: 'test' } }), syncSession: () => ({ key: 'test' }), isCurrent: () => true },
    buildReviewMaterials: () => [],
    message: { error: value => messages.push(value) }, disposeNonconformanceReview: async request => { writes.push(request); throw Error('test request captured') }
  }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(declarations.map(d => d.getText(ast)).join('\n') + '\nglobalThis.dispose = handleDispose;globalThis.errors = reviewValidationErrors', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
  return { context, messages, writes }
}
test('each disposition reports missing fields at their corresponding inputs without business writes', async () => {
  for (const disposition of ['concession_release', 'rework', 'void']) {
    const h = harness()
    await h.context.dispose(disposition)
    assert.equal(h.context.errors.value.materials, '')
    assert.match(h.context.errors.value.opinion, /评审意见/)
    assert.match(h.context.errors.value.signature, /签名凭据/)
    h.context.disposeForm.reviewOpinion = 'reviewed'
    h.context.disposeForm.reviewMaterialUrls.push('formal-file')
    assert.equal(h.context.errors.value.materials, '')
    assert.equal(h.context.errors.value.opinion, '')
    assert.notEqual(h.context.errors.value.signature, '')
    assert.equal(h.writes.length, 0)
  }
})

test('each disposition submits without attachments when opinion and signature are provided', async () => {
  for (const disposition of ['concession_release', 'rework', 'void']) {
    const h = harness()
    h.context.disposeForm.reviewOpinion = 'reviewed without attachments'
    h.context.disposeForm.signaturePassword = 'test-pin'
    h.context.disposeForm.reviewMaterialEvents = []
    await h.context.dispose(disposition)
    assert.equal(h.writes.length, 1)
    assert.equal(h.writes[0].disposition, disposition)
    assert.equal(h.writes[0].reviewMaterialUrl, '')
    assert.equal(h.writes[0].reviewMaterials.length, 0)
    assert.equal(h.context.errors.value.materials, '')
  }
})
test('in-flight uploads prevent disposition and retain the draft', async () => {
  const h = harness()
  h.context.materialUploadsPending.value = 1
  h.context.disposeForm.reviewOpinion = 'draft'
  await h.context.dispose('void')
  assert.match(h.messages[0], /等待材料上传完成/)
  assert.equal(h.context.disposeForm.reviewOpinion, 'draft')
  assert.equal(h.writes.length, 0)
})
test('readonly, unauthorized, closed and submitting workspaces cannot submit', async () => {
  for (const [state, value] of [['selectedReviewReadOnly', true], ['canDisposeSelectedReview', false], ['reviewTabOpen', false], ['disposeLoading', true]]) {
    const h = harness()
    h.context[state].value = value
    await h.context.dispose('void')
    assert.equal(h.writes.length, 0)
  }
})
