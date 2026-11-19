const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { ref, reactive } = require('vue')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const names = ['releaseForm', 'marketReleaseRouteGeneration', 'marketReleaseWrites',
  'releaseResultUncertain', 'uncertainPrepareId', 'confirmedMarketReceipt',
  'marketReleaseWriteScope', 'resetMarketReleaseDialog', 'openReleaseDialog']
const code = ts.transpileModule(ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
  .map(statement => statement.getText(ast)).join('\n') +
  `\nObject.assign(exports, { ${names.join(', ')} });`, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const reads = []
  const context = { batchExecutionId: '11', releaseTransactionId: '21',
    releaseApprovalWorkTaskId: '31', releaseStatus: 'PENDING_APPROVAL' }
  const runtime = { exports: {}, ref, reactive, ...loadUuid(crypto),
    approvalInvalidation: { syncSession: () => ({ key: 'tenant:user' }), isCurrent: () => true },
    route: { fullPath: '/batches' }, message: { error() {} }, resolveErrorMessage: e => e.message,
    getEdhrReleasePage: async () => { reads.push('list'); return { list: [context] } },
    getEdhrRelease: async () => { reads.push('task'); return context },
    getApprovalReviewPreview: async () => { reads.push('preview'); return {
      prepareId: 'prepare1', content: { canonicalBusinessContentJson: '{}', allowedActions: ['APPROVE'] }
    } }
  }
  for (const name of ['selectedReleaseBatch', 'releaseContext', 'releasePreview']) runtime[name] = ref()
  for (const name of ['releaseDialogVisible', 'releaseCompletedReadOnly', 'releaseTransactionMissing',
    'releaseContextLoading', 'releaseLoading']) runtime[name] = ref(false)
  for (const name of ['releaseError', 'releaseAccessError']) runtime[name] = ref('')
  vm.runInNewContext(code, runtime)
  return { ...runtime.exports, runtime, context, reads,
    row: { id: '11', batchExecutionCode: 'B11' },
    task: { batchExecutionId: '11', releaseTransactionId: '21', workTaskId: '31' } }
}

test('HTTP list and task entry points generate UUID v4 at the existing dialog boundaries', async () => {
  const h = setup()
  await h.openReleaseDialog(h.row)
  assert.match(h.releaseForm.idempotencyKey, uuid)
  const previous = h.releaseForm.idempotencyKey
  assert.equal(h.runtime.releasePreview.value.prepareId, 'prepare1')
  await h.openReleaseDialog(h.row, h.task)
  assert.match(h.releaseForm.idempotencyKey, uuid)
  assert.notEqual(h.releaseForm.idempotencyKey, previous)
  assert.deepEqual(h.reads, ['list', 'preview', 'task', 'preview'])
})

test('an uncertain signature reuses its original key and never prepares a second decision', async () => {
  const h = setup()
  h.marketReleaseWrites.set(h.marketReleaseWriteScope(h.context), {
    status: 'UNCERTAIN', idempotencyKey: 'original-signature-key', prepareId: 'original-prepare'
  })
  await h.openReleaseDialog(h.row, h.task)
  assert.equal(h.releaseForm.idempotencyKey, 'original-signature-key')
  assert.equal(h.uncertainPrepareId.value, 'original-prepare')
  assert.equal(h.releaseResultUncertain.value, true)
  assert.deepEqual(h.reads, ['task'])
  h.resetMarketReleaseDialog()
  assert.equal(h.releaseForm.idempotencyKey, '')
})

test('missing HTTP random source fails before list requests and blocks task preview', async () => {
  const h = setup({})
  await assert.rejects(h.openReleaseDialog(h.row), /不支持安全随机数/)
  assert.equal(h.releaseForm.idempotencyKey, '')
  assert.equal(h.runtime.releaseDialogVisible.value, false)
  assert.deepEqual(h.reads, [])
  await h.openReleaseDialog(h.row, h.task)
  assert.match(h.runtime.releaseError.value, /不支持安全随机数/)
  assert.equal(h.runtime.releasePreview.value, undefined)
  assert.equal(h.runtime.releaseContextLoading.value, false)
  assert.deepEqual(h.reads, ['task'])
})
