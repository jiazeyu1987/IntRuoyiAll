const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const vue = require('vue')
const pageFile = path.join(__dirname, '../production-release/PqcProductionReleasePage.vue')
const panelFile = path.join(__dirname, 'components/ActiveOrderSubmissionDetailPanel.vue')

function actualFunctions(file, names, ports, returned) {
  const { descriptor, errors } = parse(fs.readFileSync(file, 'utf8'))
  assert.deepEqual(errors, [])
  const ast = ts.createSourceFile('actual.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const selected = ast.statements.filter(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
  assert.equal(selected.length, names.length, 'execute every named production declaration')
  const code = ts.transpileModule(selected.map(s => s.getText(ast)).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.None }
  }).outputText
  return new Function(...Object.keys(ports), `${code}; return { ${returned.join(',')} };`)(...Object.values(ports))
}

const decision = { status: 'MANAGER_RELEASE_PENDING', signatureId: 88 }
function fact(overrides = {}) {
  return { signatureId: 88, verificationStatus: 'VALID', digestStatus: 'CONSISTENT',
    timeConsistencyStatus: 'CONSISTENT', auditBindingStatus: 'CONSISTENT',
    storedContentHash: 'content', calculatedContentHash: 'content', storedEvidenceHash: 'evidence', calculatedEvidenceHash: 'evidence',
    algorithm: 'SHA-256', keyVersion: 'v1', verificationScope: 'LOCAL_DIGEST_TIME_AND_CREATION_AUDIT',
    timeSource: 'SERVER_CLOCK', timeTrustStatus: 'NOT_BOUND', evidenceFormatVersion: 'V1', reasonCodes: [],
    verifiedAt: 1791324000000, ...overrides }
}
function detail(verification = fact(), signature = {}) {
  return { detail: { activeOrderId: '180', workOrderId: '93', pqcProductionRelease: {
    status: 'MANAGER_RELEASE_PENDING', signature: { signatureId: 88, role: 'PQC_RELEASE', signedAt: 1791324000000,
      signerName: '未记录', signerNameStatus: 'NOT_RECORDED', verification, ...signature }
  } } }
}
function harness(result) {
  const reads = []
  const state = Object.fromEntries(['detailApplicationId', 'detailReadOnly', 'detailVisible', 'detailLoading', 'detailError', 'orderDetail']
    .map(name => [name, vue.ref(name === 'detailError' ? '' : undefined)]))
  const page = actualFunctions(pageFile, ['isExactPositiveId', 'isFormalSignedTime', 'requireCompletedPqcReleaseDetail', 'openCompletedPqcReleaseDetail'], {
    ...state, getPqcProductionReleaseOrderDetail: async (...args) => { reads.push(args); return result },
    resolveErrorMessage: error => error.message
  }, ['openCompletedPqcReleaseDetail', 'requireCompletedPqcReleaseDetail'])
  return { ...state, ...page, reads }
}

test('actual initial PQC handler accepts missing time/audit and opens the exact read-only record', async () => {
  for (const missing of ['timeConsistencyStatus', 'auditBindingStatus']) {
    const result = detail(fact({ verificationStatus: 'INCOMPLETE', [missing]: 'MISSING',
      reasonCodes: [missing === 'auditBindingStatus' ? 'AUDIT_LINK_MISSING' : 'SIGNED_AT_MISSING'] }),
    { signerNameStatus: 'UNVERIFIED_SNAPSHOT', signerName: '签时姓名未核验', signedAt: missing === 'timeConsistencyStatus' ? null : 1791324000000 })
    const h = harness(result)
    await h.openCompletedPqcReleaseDetail({ applicationId: '95' }, decision, () => true)
    assert.equal(h.detailError.value, '')
    assert.equal(h.orderDetail.value.detail.pqcProductionRelease.signature.signedAt, missing === 'timeConsistencyStatus' ? null : 1791324000000)
    assert.equal(h.detailVisible.value, true)
    assert.equal(h.detailReadOnly.value, true)
    assert.equal(h.detailLoading.value, false)
    assert.equal(h.reads[0][0], '95')
    assert.equal(h.reads[0][1].ignoreErrorMessage, true)
  }
})

test('actual initial handler accepts mismatch without making snapshot name trusted', async () => {
  const h = harness(detail(fact({ verificationStatus: 'MISMATCH', auditBindingStatus: 'MISMATCH', reasonCodes: ['AUDIT_BINDING_MISMATCH'] }),
    { signerNameStatus: 'UNVERIFIED_SNAPSHOT', signerName: '签时姓名未核验' }))
  await h.openCompletedPqcReleaseDetail({ applicationId: '95' }, decision, () => true)
  assert.equal(h.detailError.value, '')
  assert.equal(h.orderDetail.value.detail.pqcProductionRelease.signature.verification.verificationStatus, 'MISMATCH')
})

test('actual initial validator rejects foreign IDs, status, result aggregation and unverified names', async () => {
  const cases = [
    detail(fact({ signatureId: 89 })), detail(fact({ verificationStatus: 'INCOMPLETE' })),
    detail(fact({ verificationStatus: 'MISMATCH', digestStatus: 'MISMATCH' }), { signerNameStatus: 'VERIFIED_SNAPSHOT', signerName: '原名' }),
    detail(fact({ verificationStatus: 'INCOMPLETE', auditBindingStatus: 'MISSING' }), { signerNameStatus: 'UNVERIFIED_SNAPSHOT', signerName: '原名' }),
    detail(fact(), { signatureId: 89 }), detail(fact({ timeTrustStatus: 'BOUND' })),
    detail(fact({ calculatedEvidenceHash: 'foreign' })), detail(fact({ digestStatus: 'UNKNOWN' })),
    detail(fact(), { signedAt: null }),
    detail(fact({ signatureId: '9223372036854775808' }), { signatureId: '9223372036854775808' })
  ]
  const wrongStatus = detail(); wrongStatus.detail.pqcProductionRelease.status = 'RELEASED'; cases.push(wrongStatus)
  const unsafe = detail(); unsafe.detail.activeOrderId = 9007199254740992; cases.push(unsafe)
  for (const result of cases) {
    const h = harness(result)
    await h.openCompletedPqcReleaseDetail({ applicationId: '95' }, decision, () => true)
    assert.ok(h.detailError.value)
    assert.equal(h.orderDetail.value, undefined)
  }
})

test('actual initial handler propagates detail failure into visible error', async () => {
  const h = harness(undefined)
  await h.openCompletedPqcReleaseDetail({ applicationId: '95' }, decision, () => true)
  assert.match(h.detailError.value, /签名关联/)
  assert.equal(h.orderDetail.value, undefined)
})

test('actual Panel cell and click retain missing-time record ID with unverified state', () => {
  const opens = []
  const panel = actualFunctions(panelFile, ['formatActiveOrderSignatureCellText', 'openActiveOrderSignatureRecord'], {
    formatDateTime: value => value == null ? '-' : '原签署时间',
    signatureVerificationDisplay: verification => ({ label: verification.verificationStatus === 'INCOMPLETE' ? '证据未完整' : '不一致' }),
    signatureEvidenceViewer: { open: id => { opens.push(id); return Promise.resolve() } }
  }, ['formatActiveOrderSignatureCellText', 'openActiveOrderSignatureRecord'])
  const signature = detail(fact({ verificationStatus: 'INCOMPLETE', timeConsistencyStatus: 'MISSING' }),
    { signerNameStatus: 'UNVERIFIED_SNAPSHOT', signerName: '签时姓名未核验', signedAt: null }).detail.pqcProductionRelease.signature
  assert.match(panel.formatActiveOrderSignatureCellText(signature), /签名时间未记录/)
  assert.match(panel.formatActiveOrderSignatureCellText(signature), /证据未完整/)
  assert.match(panel.formatActiveOrderSignatureCellText(signature), /签时姓名未核验/)
  panel.openActiveOrderSignatureRecord(signature)
  panel.openActiveOrderSignatureRecord(undefined)
  assert.deepEqual(opens, [88])
})

test('actual initial detail to Panel click to unchanged evidence viewer reaches null-time evidence', async () => {
  const verification = fact({ verificationStatus: 'INCOMPLETE', timeConsistencyStatus: 'MISSING', timeSource: 'UNKNOWN', reasonCodes: ['TIME_EVIDENCE_MISSING'] })
  const result = detail(verification, { signedAt: null, signerNameStatus: 'UNVERIFIED_SNAPSHOT', signerName: '签时姓名未核验' })
  const page = harness(result)
  await page.openCompletedPqcReleaseDetail({ applicationId: '95' }, decision, () => true)
  assert.equal(page.detailError.value, '')
  const source = fs.readFileSync(path.join(__dirname, 'components/activeOrderSignatureEvidenceViewer.ts'), 'utf8')
    .replace(/^import [^\r\n]*\r?\n/gm, '').replace(/export /g, '')
  const code = ts.transpileModule(source + '\nreturn createActiveOrderSignatureEvidenceViewer', {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  const createViewer = new Function('reactive', code)(vue.reactive)
  const calls = []
  const evidence = { activeOrderId: '180', signerName: '签时姓名未核验', evidence: { id: 88, signedAt: null }, verification }
  const viewer = createViewer(() => page.orderDetail.value ? { scope: 'PQC', identity: '95', activeOrderId: '180' } : undefined,
    async (...args) => { calls.push(args); return evidence })
  const panel = actualFunctions(panelFile, ['openActiveOrderSignatureRecord'], { signatureEvidenceViewer: viewer }, ['openActiveOrderSignatureRecord'])
  panel.openActiveOrderSignatureRecord(page.orderDetail.value.detail.pqcProductionRelease.signature)
  await vue.nextTick(); await Promise.resolve()
  assert.deepEqual(calls, [['PQC', '95', 88]])
  assert.equal(viewer.state.visible, true)
  assert.equal(viewer.state.error, '')
  assert.equal(viewer.state.selected.evidence.signedAt, null)
  assert.equal(viewer.state.selected.verification.timeConsistencyStatus, 'MISSING')
})

test('changed SFC scripts and templates compile with their real binding metadata', () => {
  for (const file of [pageFile, panelFile]) {
    const { descriptor, errors } = parse(fs.readFileSync(file, 'utf8'), { filename: file })
    assert.deepEqual(errors, [])
    const script = compileScript(descriptor, { id: 'p1-readability' })
    const template = compileTemplate({ source: descriptor.template.content, filename: file, id: 'p1-readability',
      compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(template.errors, [])
  }
})
