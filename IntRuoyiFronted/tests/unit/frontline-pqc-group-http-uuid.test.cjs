const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true)
const names = ['buildPqcInspectionSubmitPayloads', 'handleConfirmPqcSubmit']
const statements = ast.statements.filter(node => ts.isFunctionDeclaration(node)
  ? names.includes(node.name.text)
  : ts.isVariableStatement(node) && node.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(statements.length, names.length)
const code = ts.transpileModule(statements.map(node => node.getText(ast)).join('\n') +
  '\nexports.actions = { buildPqcInspectionSubmitPayloads, handleConfirmPqcSubmit };', {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const plain = value => JSON.parse(JSON.stringify(value))
const pattern = /^PQC-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }, single = false) {
  const options = single ? [{ pqcTaskId: 11 }] : [{ pqcTaskId: 11 }, { pqcTaskId: 12 }]
  const payloads = options.map(({ pqcTaskId }) => ({ pqcTaskId, rawPayload: { original: pqcTaskId } }))
  const errors = [], writes = []
  let singleBuilds = 0
  const context = {
    exports: {}, ...loadUuid(crypto),
    persistCurrentPqcTaskDraft() {}, getPqcCurrentSubmitTaskOptions: () => options,
    buildPqcInspectionSubmitPayloadForTask: option => payloads.find(p => p.pqcTaskId === option.pqcTaskId),
    activePqcTaskOption: { value: options[0] },
    buildPqcInspectionSubmitPayload: () => { singleBuilds++; return { ...payloads[0], itemResults: ['single-task-items'] } },
    hasReturnNotification: { value: false }, isInitialHandoffBlocked: { value: false },
    payloadLoading: { value: false }, pqcSubmitResultUncertain: { value: false },
    pqcSignatureCredentialType: { value: 'PASSWORD' }, pqcSignaturePassword: { value: 'test-signature' },
    pqcSubmitGuard: { capture: () => () => true }, approvalInvalidation: { syncSession: () => ({ key: 'session' }) },
    assertPqcFormalSubmissionReady() {}, assertPqcSignatureAndQuantityReady() {},
    assertPqcCurrentProcessAllMethodSubmissionReady() {}, cloneDeep: plain,
    showFrontlineError: error => errors.push(error.message || String(error)),
    ProFeedbackApi: { submitFrontlinePqcInspection: payload => writes.push(payload) }
  }
  vm.runInNewContext(code, context)
  return { ...context.exports.actions, errors, writes, payloads, singleBuilds: () => singleBuilds }
}

test('HTTP PQC method payloads share one UUID v4 group and a new submission creates a fresh group', () => {
  const h = setup()
  const payloads = h.buildPqcInspectionSubmitPayloads()
  const key = payloads[0].rawPayload.pqcSubmissionGroupId
  assert.match(key, pattern)
  assert.equal(payloads.length, 2)
  assert.equal(payloads[1].rawPayload.pqcSubmissionGroupId, key)
  assert.deepEqual(plain(payloads.map(p => p.rawPayload.original)), [11, 12])
  assert.equal(h.payloads[0].rawPayload.pqcSubmissionGroupId, undefined)
  assert.notEqual(h.buildPqcInspectionSubmitPayloads()[0].rawPayload.pqcSubmissionGroupId, key)
})

test('single active PQC task keeps its specialized payload and receives the same group format', () => {
  const h = setup(undefined, true)
  const payloads = h.buildPqcInspectionSubmitPayloads()
  assert.equal(h.singleBuilds(), 1)
  assert.deepEqual(plain(payloads[0].itemResults), ['single-task-items'])
  assert.match(payloads[0].rawPayload.pqcSubmissionGroupId, pattern)
})

test('missing or failing entropy stops actual formal submit before any API write', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy failed') } }]) {
    const h = setup(crypto)
    await h.handleConfirmPqcSubmit()
    assert.equal(h.writes.length, 0)
    assert.equal(h.errors.length, 1)
    assert.match(h.errors[0], /不支持安全随机数|entropy failed/)
  }
})
