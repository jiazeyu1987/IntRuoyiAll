const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { reactive, watch } = require('vue')
const { test } = require('node:test')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', source.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)[1], ts.ScriptTarget.Latest, true)
const names = ['handleMaterialUpload', 'removeReviewMaterial', 'buildReviewMaterials', 'resolveErrorMessage']
const parts = ast.statements.filter(s => ts.isVariableStatement(s)
  && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
const materialWatcher = ast.statements.find(s => ts.isExpressionStatement(s) && s.getText(ast).startsWith('watch(')
  && s.getText(ast).includes('() => [...disposeForm.reviewMaterialUrls]'))
assert.equal(parts.length, names.length)
assert(materialWatcher)
function harness() {
  const pending = []
  const errors = []
  const context = { watch, Promise, Error, materialContextGeneration: 1, materialTrackingEnabled: true, materialEventSequence: 0,
    selectedReview: { value: { id: '1001' } }, reviewTabOpen: { value: true }, disposeLoading: { value: false },
    materialUploadsPending: { value: 0 }, selectedReviewReadOnly: { value: false }, canDisposeSelectedReview: { value: true },
    materialSelectionError: { value: '' }, materialUploadAttempts: { value: [] }, materialUploadSequence: 0,
    disposeForm: reactive({ reviewMaterialUrls: [], reviewMaterialNames: {}, reviewMaterialIds: {}, reviewMaterialEvents: [] }),
    message: { error: value => errors.push(value) },
    uploadNonconformanceReviewMaterial: (reviewId, file) => new Promise((resolve, reject) => pending.push({ reviewId, file, resolve, reject })) }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(parts.map(p => p.getText(ast)).join('\n') + '\n' + materialWatcher.getText(ast)
    + '\n' + names.map(n => `globalThis.${n} = ${n};`).join('\n'),
    { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
  return { context, pending, errors, upload: (files = [{ name: 'same.pdf' }, { name: 'same.pdf' }]) => context.handleMaterialUpload(files) }
}
test('one failed upload preserves successful material for submission', async () => {
  const h = harness()
  const work = h.upload()
  h.pending[0].resolve({ fileId: '9007199254740993', url: 'a', fileName: ' same.pdf ' })
  h.pending[1].reject(new Error('storage failed'))
  await work
  const materials = h.context.buildReviewMaterials()
  assert.equal(materials.length, 1)
  assert.equal(materials[0].fileId, '9007199254740993')
  assert.equal(h.context.materialUploadsPending.value, 0)
  assert.equal(h.errors.length, 1)
  assert.equal(h.context.materialUploadAttempts.value.length, 1)
  assert.equal(h.context.materialUploadAttempts.value[0].status, 'failed')
  assert.equal(h.context.materialUploadAttempts.value[0].error, 'storage failed')
  assert.equal(h.context.disposeForm.reviewMaterialEvents[0].fileId, '9007199254740993')
})
test('empty files show a specific failure without uploading or occupying capacity', async () => {
  const h = harness()
  await h.upload([{ name: 'empty.txt', size: 0 }])
  assert.equal(h.pending.length, 0)
  assert.equal(h.context.materialUploadsPending.value, 0)
  assert.equal(h.context.disposeForm.reviewMaterialUrls.length, 0)
  assert.equal(h.context.materialUploadAttempts.value[0].status, 'failed')
  assert.match(h.context.materialUploadAttempts.value[0].error, /文件为空/)
})

test('old success failure and finally cannot mutate the next review', async () => {
  const h = harness()
  const work = h.upload()
  h.context.materialContextGeneration++
  h.context.selectedReview.value = { id: '1002' }
  h.context.materialUploadsPending.value = 4
  h.pending[0].resolve({ fileId: '70001', url: 'a', fileName: 'same.pdf' })
  h.pending[1].reject(new Error('old failure'))
  await work
  assert.equal(h.context.disposeForm.reviewMaterialUrls.length, 0)
  assert.equal(h.context.disposeForm.reviewMaterialEvents.length, 0)
  assert.equal(h.context.materialUploadsPending.value, 4)
  assert.equal(h.errors.length, 0)
})
test('removing the second same-name material records only its formal ID', async () => {
  const h = harness()
  const work = h.upload()
  h.pending[0].resolve({ fileId: '9007199254740993', url: 'a', fileName: 'same.pdf' })
  h.pending[1].resolve({ fileId: '9007199254740994', url: 'b', fileName: 'same.pdf' })
  await work
  h.context.removeReviewMaterial('b')
  const events = h.context.disposeForm.reviewMaterialEvents
  assert.equal(events.at(-1).action, 'DELETE')
  assert.equal(events.at(-1).fileId, '9007199254740994')
  assert.equal(h.context.buildReviewMaterials()[0].fileId, '9007199254740993')
})

test('pending uploads reserve capacity and oversized selection is rejected before upload', async () => {
  const h = harness()
  h.context.materialUploadsPending.value = 4
  await h.upload()
  assert.equal(h.pending.length, 0)
  assert.match(h.context.materialSelectionError.value, /最多上传5份/)
  assert.equal(h.context.materialUploadsPending.value, 4)
})

test('cancelled, unauthorized, readonly and submitting upload requests have no effects', async () => {
  const h = harness()
  await h.upload([])
  for (const [state, value] of [['canDisposeSelectedReview', false], ['selectedReviewReadOnly', true], ['disposeLoading', true], ['reviewTabOpen', false]]) {
    const c = harness()
    c.context[state].value = value
    await c.upload()
    assert.equal(c.pending.length, 0)
    assert.equal(c.context.materialUploadAttempts.value.length, 0)
  }
  assert.equal(h.pending.length, 0)
})

test('uploading rows become successful materials and failed rows do not occupy capacity', async () => {
  const h = harness()
  const work = h.upload()
  assert.equal(h.context.materialUploadAttempts.value.filter(x => x.status === 'uploading').length, 2)
  assert.equal(h.context.materialUploadsPending.value, 2)
  h.pending[0].resolve({ fileId: '1', url: 'a', fileName: 'same.pdf' })
  h.pending[1].reject(new Error('failed'))
  await work
  const retry = h.upload([{ name: 'same.pdf' }])
  h.pending[2].resolve({ fileId: '2', url: 'b', fileName: 'same.pdf' })
  await retry
  assert.equal(h.context.buildReviewMaterials().length, 2)
  assert.equal(h.context.materialUploadsPending.value, 0)
})
