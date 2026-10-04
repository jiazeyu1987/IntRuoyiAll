const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const page = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
const model = () => {
  const exports = {}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/signoff-departments.ts', 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, { exports, Error })
  return exports
}
const category = '9223372036854775700', department = '9223372036854775701'
const ref = value => ({ value })
const ready = ids => ({ ready: true, nodes: [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ids }], blockers: [] })
const preview = (selection, response = async () => ready([department])) => {
  const helpers = model(), calls = []
  const context = { ...helpers, Error, JSON, Promise, Number,
    formData: { categoryId: category, processType: 'CONTROLLED_FILE', selectedSignoffDepartmentIds: selection },
    isExternalReview: ref(false), routeReadinessRequestSeq: 0, routeReadiness: ref(undefined),
    routeReadinessLoading: ref(false), routeReadinessError: ref(''), routeReadinessSelectionKey: ref(''),
    signoffSelectionCategoryKey: helpers.signoffCategoryKey(category, 'CONTROLLED_FILE'),
    resolveUploadErrorMessage: error => error.message,
    checkControlledFileRouteReadiness: async request => { calls.push(request); return response() }, exports: {}
  }
  const from = page.indexOf('const refreshRouteReadiness ='), to = page.indexOf('const loadCurrentVersionByFileNumber =', from)
  assert.ok(from > 0 && to > from)
  vm.runInNewContext(ts.transpileModule(page.slice(from, to) + '\nexports.refresh=refreshRouteReadiness',
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { context, calls, refresh: context.exports.refresh }
}

test('department identity validation preserves strings and rejects empty/duplicate/unsafe selections', () => {
  const helpers = model()
  assert.deepEqual(JSON.parse(JSON.stringify(helpers.normalizeSignoffDepartments([department]))), [department])
  assert.equal(helpers.normalizeSignoffDepartments(undefined, true), undefined)
  for (const value of [[], [department, department], [9007199254740992], ['0'], ['9223372036854775808']])
    assert.throws(() => helpers.normalizeSignoffDepartments(value))
})

test('actual public preview sends the edited department set and exact category Long', async () => {
  const state = preview([department]); await state.refresh()
  assert.equal(state.calls[0].categoryId, category)
  assert.deepEqual(JSON.parse(JSON.stringify(state.calls[0].selectedSignoffDepartmentIds)), [department])
  assert.equal(state.context.routeReadiness.value.ready, true)
})

test('first preview adopts only the formally returned default departments', async () => {
  const state = preview(undefined); await state.refresh()
  assert.equal(state.calls[0].selectedSignoffDepartmentIds, undefined)
  assert.deepEqual(JSON.parse(JSON.stringify(state.context.formData.selectedSignoffDepartmentIds)), [department])
})

test('empty explicit selection cannot silently restore the default matrix', async () => {
  const state = preview([]); await state.refresh()
  assert.equal(state.calls.length, 0)
  assert.match(state.context.routeReadinessError.value, /会签部门/)
  assert.equal(state.context.routeReadiness.value, undefined)
})

test('a late readiness response cannot authorize a selection changed while it was loading', async () => {
  let release
  const state = preview([department], () => new Promise(resolve => { release = resolve }))
  const pending = state.refresh()
  state.context.formData.selectedSignoffDepartmentIds = ['51']
  release(ready([department])); await pending
  assert.equal(state.context.routeReadiness.value, undefined)
  assert.match(state.context.routeReadinessError.value, /变化/)
})

test('public upload has an editable department control wired to real preview', () => {
  assert.match(page, /data-testid="dcc-upload-signoff-departments"/)
  assert.match(page, /v-model="formData\.selectedSignoffDepartmentIds"/)
  assert.match(page, /getSimpleDeptList/)
})
