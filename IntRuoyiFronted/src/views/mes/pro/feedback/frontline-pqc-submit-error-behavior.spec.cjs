const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const axios = require('axios')
const { ref } = require('vue')
const panel = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const source = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const statements = ast.statements.filter((s) => ts.isVariableStatement(s) &&
  s.declarationList.declarations.every((d) => /^(handleConfirmPqcSubmit|recoverPqcSubmitReceiptAfterUncertainError|isPqcSubmitTimeout)$/.test(d.name.getText(ast))))
const executable = ts.transpileModule(statements.map((s) => s.getText(ast)).join('\n') +
  '\nreturn { handleConfirmPqcSubmit }', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
function harness(error, receipt = { pqcTaskId: 1, pqcEventId: 88 }, payloads = [{ pqcTaskId: 1 }]) {
  const observed = { reads: 0, resets: 0, successes: [], errors: [], submits: [] }
  const dependencies = {
    isAxiosError: axios.isAxiosError,
    activePqcTaskOption: ref({ pqcTaskId: 1 }),
    deviceState: { selectedProcess: {} }, isFrontlinePqcProcess: () => true,
    pqcSubmitResultUncertain: ref(false), pqcSignatureDialogVisible: ref(true),
    payloadLoading: ref(false), pqcSignaturePassword: ref('test-only'),
    assertPqcSignatureAndQuantityReady: () => {}, assertPqcCurrentProcessAllMethodSubmissionReady: () => {},
    buildPqcInspectionSubmitPayloads: () => payloads,
    resetPqcSubmissionDraft: () => { observed.resets++ },
    resetPqcSubmissionDrafts: () => { observed.resets++ },
    clearFrontlineError: () => { observed.errors = [] },
    resolveErrorMessage: (e) => e instanceof Error ? e.message : String(e),
    showFrontlineError: (e) => observed.errors.push(e instanceof Error ? e.message : String(e)),
    message: { success: (text) => observed.successes.push(text) },
    ProFeedbackApi: {
      submitFrontlinePqcInspection: async (payload) => {
        observed.submits.push(payload.pqcTaskId)
        const failure = typeof error === 'function' ? error(payload) : error
        if (failure) throw failure
        return { pqcTaskId: payload.pqcTaskId, pqcEventId: 99 }
      },
      getFrontlinePqcSubmitReceipt: async () => {
        observed.reads++
        if (receipt instanceof Error) throw receipt
        return receipt
      }
    }
  }
  return { ...dependencies, observed, ...new Function(...Object.keys(dependencies), executable)(...Object.values(dependencies)) }
}
for (const error of [Object.assign(new Error('content conflict'), { code: 409 }),
  Object.assign(new Error('audit failed'), { code: 100900 }), new Error('timeout is business text'),
  new axios.AxiosError('network', 'ERR_NETWORK'), new axios.CanceledError('cancelled'),
  new axios.AxiosError('server rejected', 'ECONNABORTED', undefined, undefined, { status: 409 }),
  Object.assign(new Error('not axios'), { code: 'ECONNABORTED' })]) {
  test(`explicit failure ${error.message} never reads old receipt or succeeds`, async () => {
    const h = harness(error); await h.handleConfirmPqcSubmit()
    assert.equal(h.observed.reads, 0)
    assert.deepEqual(h.observed.successes, [])
    assert.equal(h.observed.resets, 0)
    assert.deepEqual(h.observed.errors, [error.message])
    assert.equal(h.payloadLoading.value, false)
    assert.equal(h.pqcSignaturePassword.value, '')
  })
}
for (const code of ['ECONNABORTED', 'ETIMEDOUT']) {
  for (const receipt of [{ pqcTaskId: 1, pqcEventId: 88 },
    { pqcTaskId: 1, pqcEventId: 88, sourceRevision: 3, payloadHash: 'server-canonical-hash' },
    null, new Error('receipt read failed')]) {
    test(`${code} without matching request evidence stays uncertain (${receipt === null ? 'absent' : receipt instanceof Error ? 'read failed' : 'old receipt'})`, async () => {
      const h = harness(new axios.AxiosError('structured transport failure', code), receipt)
      await h.handleConfirmPqcSubmit()
      assert.equal(h.observed.reads, 1)
      assert.equal(h.pqcSubmitResultUncertain.value, true)
      assert.equal(h.observed.resets, 0)
      assert.deepEqual(h.observed.successes, [])
      assert.match(h.observed.errors[0], /结果不确定/)
      assert.equal(h.pqcSignatureDialogVisible.value, false)
      assert.equal(h.payloadLoading.value, false)
      await h.handleConfirmPqcSubmit()
      assert.equal(h.observed.submits.length, 1)
    })
  }
}
test('formal success retains normal completion', async () => {
  const h = harness(undefined); await h.handleConfirmPqcSubmit()
  assert.equal(h.observed.resets, 1)
  assert.equal(h.observed.reads, 0)
  assert.equal(h.observed.successes.length, 1)
  assert.match(h.observed.successes[0], /99/)
  assert.deepEqual(h.observed.errors, [])
})

test('actual Axios response rejection preserves timeout identity and code', async () => {
  const service = fs.readFileSync(path.resolve(__dirname, '../../../../config/axios/service.ts'), 'utf8')
  const serviceAst = ts.createSourceFile('service.ts', service, ts.ScriptTarget.Latest, true)
  const registration = serviceAst.statements.find((s) => ts.isExpressionStatement(s) &&
    ts.isCallExpression(s.expression) && s.expression.expression.getText(serviceAst) === 'service.interceptors.response.use')
  assert.ok(registration)
  const rejectSource = registration.expression.arguments[1].getText(serviceAst)
  const code = ts.transpileModule(`const reject = ${rejectSource}; return reject`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  const reject = new Function('useI18n', 'ElMessage', 'console', code)(
    () => ({ t: (key) => key }), { error() {} }, { log() {} })
  const original = new axios.AxiosError('transport timeout', 'ECONNABORTED', { ignoreErrorMessage: true })
  await assert.rejects(reject(original), (error) => error === original &&
    axios.isAxiosError(error) && error.code === 'ECONNABORTED' && !error.response)
})
test('second method failure does not claim all methods successful', async () => {
  const h = harness((p) => p.pqcTaskId === 2 ? new Error('audit failed') : undefined,
    { pqcTaskId: 2, pqcEventId: 88 }, [{ pqcTaskId: 1 }, { pqcTaskId: 2 }, { pqcTaskId: 3 }])
  await h.handleConfirmPqcSubmit()
  assert.deepEqual(h.observed.submits, [1, 2])
  assert.deepEqual(h.observed.successes, [])
  assert.equal(h.observed.resets, 0)
  assert.deepEqual(h.observed.errors, ['audit failed'])
})

test('historical missing performedBy snapshot fails without receipt lookup or draft reset', async () => {
  const error = Object.assign(new Error('历史提交缺少 performedBy 身份快照，无法通过审计校验'), {
    code: 100900, details: { field: 'performedBy' }
  })
  const h = harness(error, { pqcTaskId: 1, pqcEventId: 88, sourceRevision: 1 })
  await h.handleConfirmPqcSubmit()
  assert.equal(h.observed.reads, 0)
  assert.equal(h.observed.resets, 0)
  assert.deepEqual(h.observed.successes, [])
  assert.deepEqual(h.observed.errors, [error.message])
  assert.equal(h.pqcSubmitResultUncertain.value, false)
})

test('second method timeout stops third and preserves uncertainty instead of overwriting it', async () => {
  const h = harness((p) => p.pqcTaskId === 2
    ? new axios.AxiosError('transport timeout', 'ETIMEDOUT') : undefined,
  { pqcTaskId: 2, pqcEventId: 88 }, [{ pqcTaskId: 1 }, { pqcTaskId: 2 }, { pqcTaskId: 3 }])
  await h.handleConfirmPqcSubmit()
  assert.deepEqual(h.observed.submits, [1, 2])
  assert.equal(h.observed.reads, 1)
  assert.equal(h.observed.resets, 0)
  assert.deepEqual(h.observed.successes, [])
  assert.equal(h.observed.errors.length, 1)
  assert.match(h.observed.errors[0], /结果不确定/)
  assert.match(h.observed.errors[0], /88/)
  assert.equal(h.pqcSubmitResultUncertain.value, true)
})
