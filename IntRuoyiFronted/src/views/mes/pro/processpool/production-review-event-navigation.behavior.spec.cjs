const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref } = require('vue')

const source = fs.readFileSync(path.join(__dirname, 'TeamLeaderWorkbenchPage.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const names = ['productionEventNavigationRequestId', 'parseProductionEventQuery', 'navigateToProductionEvent']
const declarations = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.every(declaration => names.includes(declaration.name.getText(ast))))
const code = ts.transpileModule(declarations.map(statement => statement.getText(ast)).join('\n') +
  '\nreturn {parseProductionEventQuery,navigateToProductionEvent}', {
  compilerOptions: { target: ts.ScriptTarget.ES2020 }
}).outputText

function harness(loader, options = {}) {
  const state = {
    showProductionModuleTabs: ref(options.productionTabs !== false),
    isProductionLeader: ref(options.productionLeader !== false),
    detailVisible: ref(false), detail: ref(), detailLoading: ref(false),
    activeProductionModuleTab: ref('report'), pqcDetailQuery: { pageNo: 3 }
  }
  const calls = [], errors = []
  const deps = {
    ...state,
    getTeamLeaderSubmissionDetail: async (...args) => { calls.push(args); return loader(...args) },
    ElMessage: { error: message => errors.push(message) },
    resolveErrorMessage: error => error.message
  }
  return { ...state, calls, errors, ...new Function(...Object.keys(deps), code)(...Object.values(deps)) }
}

test('exact authorized event detail opens reviewed history and retains its identity', async () => {
  for (const submissionReviewStatus of ['APPROVED', 'REJECTED']) {
    const selected = { id: 282240, submissionReviewStatus }
    const h = harness(async () => selected)
    await h.navigateToProductionEvent('282240')
    assert.deepEqual(h.calls, [[282240, 'PRODUCTION']])
    assert.equal(h.detail.value.id, 282240)
    assert.equal(h.activeProductionModuleTab.value, 'reportHistory')
    assert.equal(h.detailVisible.value, true)
    assert.equal(h.detailLoading.value, false)
    assert.deepEqual(h.errors, [])
  }
})

test('pending event opens report details without automatic review or legacy approval', async () => {
  const h = harness(async id => ({ id, submissionReviewStatus: 'PENDING' }))
  await h.navigateToProductionEvent('282241')
  assert.deepEqual(h.calls, [[282241, 'PRODUCTION']])
  assert.equal(h.activeProductionModuleTab.value, 'report')
  assert.equal(h.detailVisible.value, true)
  assert.equal(h.pqcDetailQuery.pageNo, 1)
  const navigation = declarations.map(statement => statement.getText(ast)).join('\n')
  assert.doesNotMatch(navigation, /openAllocation\(|openProductionReject\(|submitReview\(|approveFeedback\(|reviewTeamLeaderSubmission\(/)
})

test('missing query is idle; ambiguous, malformed, unsafe or empty identities never request a substitute', async () => {
  for (const value of [undefined, null, '', '0', '-1', '2.5', '282240x', '9007199254740993', ['282240'], 282240]) {
    const h = harness(async () => { throw new Error('must not query') })
    await h.navigateToProductionEvent(value)
    assert.equal(h.calls.length, 0)
    assert.equal(h.detailVisible.value, false)
    assert.equal(h.detailLoading.value, false)
    assert.equal(h.errors.length, value === undefined ? 0 : 1)
  }
})

test('permission failure and mismatched detail fail visibly without opening any event', async () => {
  for (const loader of [async () => { throw new Error('无权限访问指定工序') }, async () => ({ id: 282241 })]) {
    const h = harness(loader)
    await h.navigateToProductionEvent('282240')
    assert.equal(h.errors.length, 1)
    assert.equal(h.detail.value, undefined)
    assert.equal(h.detailVisible.value, false)
    assert.equal(h.detailLoading.value, false)
    assert.deepEqual(h.calls, [[282240, 'PRODUCTION']])
  }
})

test('same component route changes cannot display a stale event response', async () => {
  const pending = new Map()
  const h = harness(id => new Promise(resolve => pending.set(id, resolve)))
  const older = h.navigateToProductionEvent('282240')
  const newer = h.navigateToProductionEvent('282241')
  pending.get(282241)({ id: 282241, submissionReviewStatus: 'APPROVED' })
  await newer
  pending.get(282240)({ id: 282240 })
  await older
  assert.equal(h.detail.value.id, 282241)
  assert.equal(h.activeProductionModuleTab.value, 'reportHistory')
  assert.deepEqual(h.errors, [])
})

test('removing event query cancels an in-flight navigation', async () => {
  let resolve
  const h = harness(() => new Promise(done => { resolve = done }))
  const loading = h.navigateToProductionEvent('282240')
  await h.navigateToProductionEvent(undefined)
  resolve({ id: 282240 })
  await loading
  assert.equal(h.detail.value, undefined)
  assert.equal(h.detailVisible.value, false)
  assert.equal(h.detailLoading.value, false)
})

test('shared PQC or legacy component never handles this production navigation query', async () => {
  for (const options of [{ productionTabs: false }, { productionLeader: false }]) {
    const h = harness(async () => ({ id: 282240 }), options)
    await h.navigateToProductionEvent('282240')
    assert.equal(h.calls.length, 0)
    assert.equal(h.detailVisible.value, false)
  }
})

test('navigation watches initial and changed query; manual actions use existing formal handlers', () => {
  assert.match(source, /watch\(\s*\(\) => route\.query\.eventId,[\s\S]*?navigateToProductionEvent\(value\)[\s\S]*?immediate: true/)
  assert.match(source, /v-if="detail && isProductionLeader && canAllocateSubmission\(detail\)"[\s\S]*?@click="openAllocation\(detail\)"/)
  assert.match(source, /v-if="detail && isProductionLeader && canRejectProductionSubmission\(detail\)"[\s\S]*?@click="openProductionReject\(detail\)"/)
})
