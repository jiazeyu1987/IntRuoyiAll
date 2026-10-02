const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')

function load(file, names, dependencies) {
  const source = fs.readFileSync(file, 'utf8').replace(/^import .*\n/gm, '').replace(/export /g, '')
  const code = ts.transpileModule(source + `\nreturn {${names.join(',')}}`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  return new Function(...Object.keys(dependencies), code)(...Object.values(dependencies))
}
const apiPath = path.resolve(__dirname, '../../../../api/mes/pro/edhr/activeOrderSignature.ts')
const helperPath = path.join(__dirname, 'components/activeOrderSignatureEvidenceViewer.ts')
const panelPath = path.join(__dirname, 'components/ActiveOrderSubmissionDetailPanel.vue')
const helper = () => load(helperPath, ['createActiveOrderSignatureEvidenceViewer'], { reactive: vue.reactive })
const context = { scope: 'TEAM', identity: 409, activeOrderId: 409 }
const result = (signatureId, activeOrderId = 409, status = 'VALID') => ({
  activeOrderId, signerName: '正式人员', evidence: { id: signatureId, verificationStatus: 'VALID' },
  verification: { signatureId, verificationStatus: status }
})
const deferred = () => { let resolve; let reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }

test('TEAM and PQC use their exact business source, never global governance', async () => {
  const calls = []
  const api = load(apiPath, ['getActiveOrderSignatureEvidence'], { request: { get: async cfg => { calls.push(cfg); return result(742) } } })
  await api.getActiveOrderSignatureEvidence('TEAM', 409, 742)
  await api.getActiveOrderSignatureEvidence('PQC', 900, 743)
  assert.deepEqual(calls[0], { url: '/mes/pro/process-pool/team-leader/active-order/signature/get', params: { activeOrderId: 409, signatureId: 742 } })
  assert.deepEqual(calls[1], { url: '/mes/pro/production-release/pqc/signature/get', params: { applicationId: 900, signatureId: 743 } })
})

test('BATCH preserves exact historic batch or active order Long strings', () => {
  const api = load(apiPath, ['buildActiveOrderSignatureRequest'], { request: {} })
  for (const identity of [{ batchExecutionId: '9223372036854775806' }, { activeOrderId: '409' }]) {
    const req = api.buildActiveOrderSignatureRequest('BATCH', identity, '9223372036854775805')
    assert.equal(req.url, '/mes/pro/edhr-batch-execution/signature/get')
    assert.deepEqual(req.params, { ...identity, signatureId: '9223372036854775805' })
  }
})

test('missing, ambiguous, imprecise and out-of-range identities cannot produce requests', () => {
  const api = load(apiPath, ['buildActiveOrderSignatureRequest'], { request: {} })
  for (const identity of [undefined, {}, 409, { batchExecutionId: 800, activeOrderId: 409 }, { batchExecutionId: '0' }]) {
    assert.throws(() => api.buildActiveOrderSignatureRequest('BATCH', identity, 742))
  }
  for (const signature of [0, -1, NaN, 9223372036854775806, '9223372036854775808', '1.5']) {
    assert.throws(() => api.buildActiveOrderSignatureRequest('TEAM', 409, signature))
  }
})

test('signed evidence mismatch remains visible even if stored record says VALID', async () => {
  const viewer = helper().createActiveOrderSignatureEvidenceViewer(() => context, async () => result(742, 409, 'MISMATCH'))
  await viewer.open(742)
  assert.equal(viewer.state.selected.evidence.verificationStatus, 'VALID')
  assert.equal(viewer.state.selected.verification.verificationStatus, 'MISMATCH')
  assert.equal(viewer.state.loading, false)
})

test('business failure is shown and old successful evidence is cleared', async () => {
  let fail = false
  const viewer = helper().createActiveOrderSignatureEvidenceViewer(() => context, async () => {
    if (fail) throw new Error('签名不属于当前订单正式记录')
    return result(742)
  })
  await viewer.open(742)
  fail = true
  await viewer.open(743)
  assert.equal(viewer.state.selected, undefined)
  assert.equal(viewer.state.error, '签名不属于当前订单正式记录')
  assert.equal(viewer.state.visible, true)
})

test('missing authorized context fails without calling endpoint', async () => {
  let called = false
  const viewer = helper().createActiveOrderSignatureEvidenceViewer(() => undefined, async () => { called = true })
  await viewer.open(742)
  assert.equal(called, false)
  assert.equal(viewer.state.error, '当前详情缺少正式签名查询身份')
})

test('closing dialog before response prevents stale evidence disclosure', async () => {
  const pending = deferred()
  const viewer = helper().createActiveOrderSignatureEvidenceViewer(() => context, () => pending.promise)
  const opening = viewer.open(742)
  assert.equal(viewer.state.loading, true)
  viewer.close()
  pending.resolve(result(742))
  await opening
  assert.equal(viewer.state.visible, false)
  assert.equal(viewer.state.selected, undefined)
})

test('changing business scope during request cannot leak prior order evidence', async () => {
  let current = context
  const pending = deferred()
  const viewer = helper().createActiveOrderSignatureEvidenceViewer(() => current, () => pending.promise)
  const opening = viewer.open(742)
  current = { scope: 'BATCH', identity: { activeOrderId: 500 }, activeOrderId: 500 }
  viewer.close()
  pending.resolve(result(742))
  await opening
  assert.equal(viewer.state.selected, undefined)
  assert.equal(viewer.state.error, '')
})

test('second signature click wins; first late failure does not overwrite it', async () => {
  const first = deferred()
  const second = deferred()
  const viewer = helper().createActiveOrderSignatureEvidenceViewer(() => context,
    (_scope, _identity, id) => id === 742 ? first.promise : second.promise)
  const a = viewer.open(742)
  const b = viewer.open(743)
  second.resolve(result(743))
  await b
  first.reject(new Error('late denied'))
  await a
  assert.equal(viewer.state.selected.evidence.id, 743)
  assert.equal(viewer.state.error, '')
})

test('wrong returned order or signature fails closed with visible error', async () => {
  for (const response of [result(742, 500), result(743), { ...result(742), verification: { signatureId: 743 } }]) {
    const viewer = helper().createActiveOrderSignatureEvidenceViewer(() => context, async () => response)
    await viewer.open(742)
    assert.equal(viewer.state.selected, undefined)
    assert.equal(viewer.state.error, '签名证据响应与当前业务记录不一致')
  }
})

test('shared panel signature context chooses formal batch identity rather than TEAM ACTIVE path', () => {
  const panel = fs.readFileSync(panelPath, 'utf8')
  const script = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === 'signatureEvidenceContext'))
  const code = ts.transpileModule(declaration.getText(ast) + '\nreturn signatureEvidenceContext', {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  for (const props of [
    { detail: { activeOrderId: 409 } },
    { detail: { activeOrderId: 409 }, auditScopeId: { batchExecutionId: '9223372036854775806' } }
  ]) {
    const computed = new Function('computed', 'props', 'auditScopeTypeValue', 'auditScopeIdValue', code)(
      vue.computed, props, vue.ref('BATCH'), vue.ref(undefined))
    assert.deepEqual(computed.value, { scope: 'BATCH', identity: props.auditScopeId || { activeOrderId: 409 }, activeOrderId: 409 })
  }
  const click = script.slice(script.indexOf('const openActiveOrderSignatureRecord'), script.indexOf('const toOperationFactSignature'))
  assert.match(click, /signatureEvidenceViewer\.open\(signature\.signatureId\)/)
  assert.doesNotMatch(click, /router\.push|signature-governance/)
  assert.match(panel, /data-active-order-signature-error/)
  assert.match(panel, /本次完整性核验/)
})
