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
    userStore:{getUser:{id:'71',deptId:'7'}},
    ...require('./dcc-upload-test-loader.cjs').loadUploadMatrix(), approvalMatrixState: ref(), approvalMatrixScopeKey: '',
    getTenantId: () => '1', getVisitTenantId: () => undefined, referenceIdentity: String, uploadFileTypeIdentity: String, createControlledFileUploadSessionId: () => 'scope',
    formData: { categoryId: category, dccProjectCodeId: '3', fileTypeTaxonomyId: '4', selectedDistributionDepartmentIds: [department], processType: 'CONTROLLED_FILE', selectedSignoffDepartmentIds: selection },
    isCustomApprovalSource: ref(false), selectedFileTypeTaxonomyPath: ref({approvalSourceMode:'PUBLIC_MATRIX'}),
    isExternalReview: ref(false), routeReadinessRequestSeq: 0, routeReadiness: ref(undefined),
    routeReadinessLoading: ref(false), routeReadinessError: ref(''), routeReadinessSelectionKey: ref(''),
    signoffSelectionCategoryKey: helpers.signoffCategoryKey(category, 'CONTROLLED_FILE'),
    resolveUploadErrorMessage: error => error.message,
    checkControlledFileRouteReadiness: async request => { calls.push(request); return require('./dcc-upload-test-loader.cjs').matrixReadiness(await response(), { categoryId: category }) }, exports: {}
  }
  const from = page.indexOf('const ensureUploadApprovalMatrix ='), to = page.indexOf('const loadCurrentVersionByFileNumber =', from)
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
  assert.equal(state.context.routeReadinessError.value, '请至少选择一个会签部门')
  assert.equal(state.context.routeReadiness.value, undefined)
})

test('G04 first default preview adopts formal mandatory and optional departments into independent signoff and distribution arrays', async () => {
  const state = preview(undefined)
  state.context.formData.selectedDistributionDepartmentIds = undefined
  state.context.checkControlledFileRouteReadiness = async request => {
    state.calls.push(request)
    const selected = request.selectedSignoffDepartmentIds || ['51', '52']
    const r = require('./dcc-upload-test-loader.cjs').matrixReadiness(ready(selected), { categoryId: category })
    for (const baseline of new Set([r.matrixBefore, r.matrixAfter, r.approvalMatrix])) {
      baseline.nodes[0].candidateSourceId = '51'; baseline.nodes[0].candidateSourceIds = '51'; baseline.nodes[0].subjectId = '51'
      baseline.nodes.push({ ...baseline.nodes[0], id: '42', candidateSourceId: '52', candidateSourceIds: '52', subjectId: '52', marker: '◎' })
    }
    return r
  }
  await state.refresh()
  assert.ok(state.context.routeReadiness.value, state.context.routeReadinessError.value)
  assert.equal(state.context.routeReadiness.value.ready, true, state.context.routeReadinessError.value)
  assert.equal(Object.hasOwn(state.calls[0], 'selectedSignoffDepartmentIds'), false)
  assert.deepEqual(Array.from(state.context.formData.selectedSignoffDepartmentIds), ['51', '52'])
  assert.deepEqual(Array.from(state.context.formData.selectedDistributionDepartmentIds), ['51', '52'])
  assert.notEqual(state.context.formData.selectedSignoffDepartmentIds, state.context.formData.selectedDistributionDepartmentIds)
  state.context.formData.selectedSignoffDepartmentIds = ['52', '53']
  await state.refresh()
  assert.equal(state.context.routeReadiness.value.ready, true, state.context.routeReadinessError.value)
  assert.deepEqual(Array.from(state.calls[1].selectedSignoffDepartmentIds), ['52', '53'])
  assert.deepEqual(Array.from(state.context.formData.selectedSignoffDepartmentIds), ['52', '53'])
  assert.deepEqual(Array.from(state.context.formData.selectedDistributionDepartmentIds), ['51', '52'])
  assert.equal(state.context.routeReadiness.value.matrixAfter.nodes.find(node => node.id === '42').marker, '◎')
})

test('G04 actual upload preview preserves the edited B/C roster and leaves formal source and distribution defaults intact', async () => {
  const state = preview(['52', '53'], async () => ready(['52', '53']))
  await state.refresh()
  assert.equal(state.context.routeReadiness.value.ready, true)
  assert.deepEqual(Array.from(state.calls[0].selectedSignoffDepartmentIds), ['52', '53'])
  assert.deepEqual(Array.from(state.context.formData.selectedSignoffDepartmentIds), ['52', '53'])
  assert.deepEqual(Array.from(state.context.formData.selectedDistributionDepartmentIds), [department])
  assert.equal(state.context.routeReadiness.value.matrixAfter.nodes[0].candidateSourceIds, '7')
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

test('IC31 late actual readiness cannot publish an earlier supervisor source after the actor or tenant changed',async()=>{
  for(const scope of ['ACTOR','TENANT']){
    let release,tenant='1'
    const state=preview([department],()=>new Promise(r=>release=r));state.context.getTenantId=()=>tenant
    const pending=state.refresh()
    if(scope==='ACTOR')state.context.userStore.getUser.id='72';else tenant='2'
    release(ready([department]));await pending
    assert.equal(state.context.routeReadiness.value,undefined);assert.equal(state.context.routeReadinessLoading.value,false)
    assert.match(state.context.routeReadinessError.value,/账号或租户已变化/)
  }
})
test('IC31 switching applicant creates a new matrix application context and never reuses the earlier prepared context',async()=>{
  const state=preview([department]);await state.refresh()
  const original=state.context.approvalMatrixState.value
  state.context.userStore.getUser.id='72';await state.refresh()
  assert.notEqual(state.context.approvalMatrixState.value,original)
  assert.equal(state.calls.length,2)
})
