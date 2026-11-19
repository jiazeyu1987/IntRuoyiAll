const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/edhr/ExecutionPage.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('execution.ts', script, ts.ScriptTarget.Latest, true)
const names = ['createFieldAuditIdempotencyKey', 'buildFieldAuditChangeRequest', 'handleSaveFieldAuditChanges']
const selected = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
assert.equal(selected.length, names.length)
const code = ts.transpileModule(selected.map(statement => statement.getText(ast)).join('\n') +
  '\nthis.save = handleSaveFieldAuditChanges', {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const writes = [], errors = []
  let fail = false
  const state = {
    ...loadUuid(crypto),
    fieldAuditSaveGateError: { value: '' }, workTaskId: { value: 19 },
    fieldAuditIdempotencyKey: { value: '' }, fieldAuditSaveLoading: { value: false },
    fieldAuditSaveError: { value: '' }, fieldAuditLastResult: { value: null },
    execution: { value: { id: 28, cellValuesHash: 'cells-hash', fieldAuditRevision: 4, fieldAuditHeadHash: 'head-hash' } },
    isRecordbookUnrestrictedMode: { value: false },
    pendingFieldChanges: { value: [{ fieldPath: 'value', fieldKey: 'field', rowIndex: 1,
      columnIndex: 2, valueType: 'STRING', newValueJson: '"new"', newValueDisplay: 'new',
      expectedOldValueJson: '"old"', expectedOldValueHash: 'old-hash' }] },
    pendingAttachmentChanges: { value: [] },
    buildAttachmentChangeRequest: change => change,
    resolveFieldAuditDraftReasonCategory: () => 'CORRECTION',
    resolveFieldAuditDraftReasonText: () => '真实修改原因',
    resolveHashStatusLabel: value => value,
    resolveErrorMessage: error => error.message,
    message: { error: text => errors.push(text) },
    showFillActionResultDialog() {}, loadExecution: async () => {},
    saveEdhrFieldChanges: async request => {
      writes.push(request)
      if (fail) throw new Error('save rejected')
      return { hashVerification: { status: 'VALID' } }
    }
  }
  vm.createContext(state)
  vm.runInContext(code, state)
  return { state, writes, errors, rejectSave: value => { fail = value } }
}

test('HTTP field audit saves use UUID v4 and reuse the original key when retrying a failed save', async () => {
  const h = setup()
  h.rejectSave(true)
  await h.state.save()
  const request = h.writes[0]
  assert.match(request.idempotencyKey, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.equal(request.executionId, 28)
  assert.equal(request.workTaskId, 19)
  assert.equal(request.baseCellValuesHash, 'cells-hash')
  assert.equal(request.baseFieldAuditRevision, 4)
  assert.equal(request.baseFieldAuditHeadHash, 'head-hash')
  assert.equal(request.changes[0].expectedOldValueHash, 'old-hash')
  assert.equal(h.state.fieldAuditIdempotencyKey.value, request.idempotencyKey)
  assert.equal(h.state.fieldAuditSaveLoading.value, false)
  assert.deepEqual(h.errors, ['save rejected'])
  h.rejectSave(false)
  await h.state.save()
  assert.equal(h.writes[1].idempotencyKey, request.idempotencyKey)
  assert.equal(h.state.fieldAuditIdempotencyKey.value, '')
  await h.state.save()
  assert.notEqual(h.writes[2].idempotencyKey, request.idempotencyKey)
})

test('missing or failed entropy stops field audit before any write; business gates run first', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy rejected') } }]) {
    const h = setup(crypto)
    await assert.rejects(h.state.save(), /不支持安全随机数|entropy rejected/)
    assert.equal(h.writes.length, 0)
    assert.equal(h.state.fieldAuditIdempotencyKey.value, '')
    assert.equal(h.state.fieldAuditSaveLoading.value, false)
    h.state.fieldAuditSaveGateError.value = '禁止修改'
    await h.state.save()
    assert.deepEqual(h.errors, ['禁止修改'])
    assert.equal(h.writes.length, 0)
  }
})
