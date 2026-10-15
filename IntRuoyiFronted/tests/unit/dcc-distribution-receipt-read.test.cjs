const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
function load(file, resolve) {
  const context = { exports: {}, Error, BigInt, Set, require: resolve }
  vm.runInNewContext(ts.transpileModule(read(file), { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, context)
  return context.exports
}
const facts = load('src/views/dcc/controlled-file/workflow/distribution-facts.ts', id => { throw new Error(id) })
const { createDistributionReceiptReader } = load('src/views/dcc/controlled-file/detail/distribution-receipt-reader.ts', id => {
  if (id === 'vue') return vue
  if (id === '../workflow/distribution-facts') return facts
  throw new Error(id)
})
const fileId = '9223372036854775000'
const context = () => ({ fileId, processInstanceId: 'round-1', versionNo: 'A/1' })
const result = () => ({ controlledFileId: fileId, versionNo: 'A/1', approvalProcessInstanceId: 'round-1',
  batchId: null, status: 'NOT_CREATED' })
const readerFor = (current, read) => createDistributionReceiptReader(current, read, async () => result())
const row = () => ({ controlledFileId: fileId, versionNo: 'A/1', distributionId: '2',
  recipientId: '3', departmentId: '4', userId: '5', currentUserName: '接收人', messageStatus: 'PENDING' })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
test('formal receipt GET keeps the complete decimal selected version identity', async () => {
  const calls = []
  const api = load('src/api/dcc/controlledFile/distribution.ts', id => {
    assert.equal(id, '@/config/axios')
    return { default: { get: async input => { calls.push(input); return [row()] } } }
  })
  const result = await api.getDistributionRecipientReceipts(fileId)
  assert.equal(calls[0].url, '/dcc/distribution-tasks/recipient-receipts')
  assert.equal(calls[0].params.controlledFileId, fileId)
  assert.equal(calls[0].ignoreErrorMessage, true)
  assert.equal(result[0].controlledFileId, fileId)
})
test('invalid receipt version identity never sends a query', async () => {
  let requests = 0
  const api = load('src/api/dcc/controlledFile/distribution.ts', () => ({ default: {
    get: async () => { requests++; return [] }
  } }))
  for (const invalid of ['0', 'bad', '9223372036854775808', 9007199254740992]) {
    await assert.rejects(async () => api.getDistributionRecipientReceipts(invalid))
  }
  assert.equal(requests, 0)
})
test('exact unsent and unsigned facts stay absent rather than gaining a time', async () => {
  const reader = readerFor(context, async () => [row()])
  await reader.load()
  assert.equal(reader.state.loading, false)
  assert.equal(reader.state.error, '')
  assert.equal(reader.state.rows[0].sentAt, undefined)
  assert.equal(reader.state.rows[0].signedAt, undefined)
})
test('foreign version, duplicate and invented signature facts fail visibly', async () => {
  for (const rows of [[{ ...row(), controlledFileId: '9' }], [row(), row()], [{ ...row(), signedAt: '2026-10-11 12:00:00' }]]) {
    const reader = readerFor(context, async () => rows)
    await reader.load()
    assert.equal(reader.state.rows, undefined)
    assert.ok(reader.state.error)
    assert.equal(reader.state.loading, false)
  }
})
test('late responses and errors cannot change a cleared new round', async () => {
  for (const reject of [false, true]) {
    const waiting = deferred()
    let actual = context()
    const reader = readerFor(() => actual, () => waiting.promise)
    const first = reader.load()
    actual = { ...actual, processInstanceId: 'round-2' }
    reader.clear()
    if (reject) waiting.reject(new Error('旧轮失败'))
    else waiting.resolve([row()])
    await first
    assert.equal(reader.state.rows, undefined)
    assert.equal(reader.state.error, '')
    assert.equal(reader.state.loading, false)
  }
})
test('the latest refresh wins even when both reads share the same identity', async () => {
  const earlier = deferred(), later = deferred()
  let requests = 0
  const reader = readerFor(context, () => ++requests === 1 ? earlier.promise : later.promise)
  const first = reader.load(), second = reader.load()
  later.resolve([{ ...row(), currentUserName: '新响应' }]); await second
  earlier.resolve([{ ...row(), currentUserName: '旧响应' }]); await first
  assert.equal(reader.state.rows[0].currentUserName, '新响应')
})
test('detail connects actual read facts and refresh without invoking manual distribution', () => {
  const source = read('src/views/dcc/controlled-file/detail/index.vue')
  const panel = source.match(/<WorkflowDistributionPanel\b[\s\S]*?\/>/)[0]
  assert.match(panel, /:receipts="distributionReceiptReader\.state\.rows"/)
  assert.match(panel, /:receipts-loading="distributionReceiptReader\.state\.loading"/)
  assert.match(panel, /:receipt-error="distributionReceiptReader\.state\.error"/)
  assert.match(panel, /@refresh="distributionReceiptReader\.load"/)
  assert.match(panel, /:automatic-result="distributionReceiptReader\.state\.automaticResult"/)
  assert.match(panel, /effectiveMode: fileDetail\.effectiveMode/)
  assert.match(panel, /effectiveTime: fileDetail\.effectiveTime/)
  assert.doesNotMatch(panel, /effectiveDate: fileDetail\.effectiveDate \|\|/)
  assert.doesNotMatch(panel, /:save=|:load-recipients=|fileDetail\.effectiveDate"/)
  assert.doesNotMatch(source.match(/<template>[\s\S]*?<script/)[0], /@click="openDistributionSignDialog\(row\)"/)
})
test('an empty recipient list preserves an actual failed automatic batch rather than showing delivery success', async () => {
  const failure = { ...result(), batchId: '6', status: 'FAILED', errorCode: 'ROSTER_POLICY_UNDECIDED',
    errorMessage: '人员名单冻结策略尚未确定', failureAt: '2026-10-11 12:00:00' }
  const reader = createDistributionReceiptReader(context, async () => [], async () => failure)
  await reader.load()
  assert.equal(reader.state.rows.length, 0)
  assert.equal(reader.state.automaticResult.status, 'FAILED')
  assert.equal(reader.state.automaticResult.errorMessage, failure.errorMessage)
})
test('foreign batch context and summary read failure do not hide valid independent recipient facts', async () => {
  for (const readSummary of [async () => ({ ...result(), approvalProcessInstanceId: 'another-round' }),
    async () => { throw new Error('自动下发结果读取失败') }]) {
    const reader = createDistributionReceiptReader(context, async () => [row()], readSummary)
    await reader.load()
    assert.equal(reader.state.rows.length, 1)
    assert.equal(reader.state.automaticResult, undefined)
    assert.ok(reader.state.automaticResultError)
  }
})
test('lossy or out-of-range automatic batch identity is rejected without losing recipient evidence', async () => {
  for (const batchId of [9007199254740992, '9223372036854775808']) {
    const reader = createDistributionReceiptReader(context, async () => [row()], async () => ({
      ...result(), batchId, status: 'PENDING'
    }))
    await reader.load()
    assert.equal(reader.state.rows.length, 1)
    assert.equal(reader.state.automaticResult, undefined)
    assert.ok(reader.state.automaticResultError)
  }
})
