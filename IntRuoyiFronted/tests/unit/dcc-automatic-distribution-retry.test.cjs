const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const root = path.resolve(__dirname, '../..')
const load = (file, resolve) => {
  const context = { exports: {}, Error, BigInt, Set, require: resolve }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const fileId = '9223372036854775000', batchId = '9223372036854775001'
const command = () => ({ controlledFileId: fileId, batchId, versionNo: 'A/1', approvalProcessInstanceId: 'real-bpm' })
const result = () => ({ ...command(), status: 'FAILED', errorCode: 'ROSTER_POLICY_NOT_READY', errorMessage: '正式政策未就绪', failureAt: '2026-10-13 00:00:00' })
const apiFor = post => load('src/api/dcc/controlledFile/distribution.ts', () => ({ default: { post } }))
const facts = load('src/views/dcc/controlled-file/workflow/distribution-facts.ts', id => { throw new Error(id) })
const readerModule = load('src/views/dcc/controlled-file/detail/distribution-receipt-reader.ts', id => id === 'vue' ? vue : facts)
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }
const readerFor = current => readerModule.createDistributionReceiptReader(current, async () => [], async () => result())

test('retry posts only exact four identities and preserves a real FAILED result without pretending delivery', async () => {
  const calls = [], api = apiFor(async input => { calls.push(input); return result() })
  const value = await api.retryAutomaticDistribution(command())
  assert.equal(calls.length, 1)
  assert.equal(calls[0].url, '/dcc/distribution-tasks/automatic-retry')
  assert.equal(calls[0].ignoreErrorMessage, true)
  assert.deepEqual(JSON.parse(JSON.stringify(calls[0].data)), command())
  assert.equal(value.status, 'FAILED')
  assert.equal(value.errorCode, 'ROSTER_POLICY_NOT_READY')
})
test('partial, lossy, malformed or client roster policy input never reaches the retry endpoint', async () => {
  let calls = 0; const api = apiFor(async () => { calls++; return result() })
  for (const patch of [{ batchId: undefined }, { controlledFileId: 9007199254740992 }, { batchId: '01' },
    { versionNo: '' }, { approvalProcessInstanceId: ' real-bpm' }, { recipients: ['5'] }, { rosterPolicyDecided: true }])
    await assert.rejects(() => api.retryAutomaticDistribution({ ...command(), ...patch }))
  assert.equal(calls, 0)
})
test('retry rejects another batch, version, round, impossible success or incomplete failure evidence', async () => {
  for (const patch of [{ batchId: '8' }, { controlledFileId: '9' }, { versionNo: 'B/1' },
    { approvalProcessInstanceId: 'old-bpm' }, { status: 'NOT_CREATED' }, { status: 'SENT', sentAt: null },
    { errorCode: '' }, { errorMessage: '' }, { failureAt: null }]) {
    const api = apiFor(async () => ({ ...result(), ...patch }))
    await assert.rejects(() => api.retryAutomaticDistribution(command()))
  }
})
test('reader retries only its verified batch, blocks duplicates and retains exact failure facts', async () => {
  const context = { fileId, versionNo: 'A/1', processInstanceId: 'real-bpm', authorityKey: 'tenant1:actor99:allowed' }
  const reader = readerFor(() => context); await reader.load()
  const wait = deferred(); let calls = 0
  const pending = reader.retry(command(), async () => { calls++; return wait.promise }, () => true)
  await assert.rejects(() => reader.retry(command(), async () => { calls++; return result() }, () => true))
  wait.resolve(result()); assert.equal((await pending).status, 'FAILED')
  assert.equal(calls, 1); assert.equal(reader.state.retrying, false)
  assert.equal(reader.state.automaticResult.errorCode, 'ROSTER_POLICY_NOT_READY')
})
test('changed actor, tenant, version or revoked permission cannot accept a late retry response', async () => {
  for (const change of ['authority', 'version', 'permission']) {
    let context = { fileId, versionNo: 'A/1', processInstanceId: 'real-bpm', authorityKey: 'tenant1:actor99:allowed' }, allowed = true
    const reader = readerFor(() => context); await reader.load()
    const wait = deferred(), pending = reader.retry(command(), () => wait.promise, () => allowed)
    if (change === 'authority') context = { ...context, authorityKey: 'tenant2:actor88:allowed' }
    if (change === 'version') context = { ...context, versionNo: 'B/1' }
    if (change === 'permission') allowed = false
    wait.resolve({ ...result(), status: 'SENT', sentAt: '2026-10-13 00:01:00' })
    await assert.rejects(() => pending)
    assert.notEqual(reader.state.automaticResult?.status, 'SENT')
  }
})
test('missing authority, unread or mismatching batch is refused before any retry write', async () => {
  for (const authorityKey of [undefined, 'tenant1:actor99']) {
    const reader = readerFor(() => ({ fileId, versionNo: 'A/1', processInstanceId: 'real-bpm', authorityKey }))
    let calls = 0
    await assert.rejects(() => reader.retry(command(), async () => { calls++; return result() }, () => true))
    await reader.load()
    await assert.rejects(() => reader.retry({ ...command(), batchId: '8' }, async () => { calls++; return result() }, () => true))
    if (authorityKey === undefined) await assert.rejects(() => reader.retry(command(), async () => { calls++; return result() }, () => true))
    assert.equal(calls, 0)
  }
})

test('refresh during a pending retry releases its guard after the stale response is rejected', async () => {
  const current = () => ({ fileId, versionNo: 'A/1', processInstanceId: 'real-bpm', authorityKey: 'tenant1:actor99' })
  const reader = readerFor(current); await reader.load()
  const wait = deferred(), pending = reader.retry(command(), () => wait.promise, () => true)
  await reader.load(); wait.resolve(result()); await assert.rejects(() => pending)
  assert.equal(reader.state.retrying, false)
  assert.equal((await reader.retry(command(), async () => result(), () => true)).status, 'FAILED')
})
test('an old verified result cannot authorize a retry after actor or file identity changes', async () => {
  for (const kind of ['actor', 'file']) {
    let context = { fileId, versionNo: 'A/1', processInstanceId: 'real-bpm', authorityKey: 'tenant1:actor99' }
    const reader = readerFor(() => context); await reader.load()
    context = kind === 'actor' ? { ...context, authorityKey: 'tenant1:actor88' } : { ...context, fileId: '8', processInstanceId: 'new-bpm' }
    let calls = 0
    await assert.rejects(() => reader.retry({ ...command(), controlledFileId: context.fileId,
      approvalProcessInstanceId: context.processInstanceId }, async () => { calls++; return result() }, () => true))
    assert.equal(calls, 0)
  }
})

test('parent keeps a received retry outcome and shows a later detail-refresh failure inline', async () => {
  const page = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
  const start = page.indexOf('const handleAutomaticDistributionRetried =')
  const end = page.indexOf('const openDistributionReceipts =', start)
  assert.ok(start > 0 && end > start)
  for (const changed of [false, true]) {
    const state = { automaticResult: result(), automaticResultError: '' }
    const fileDetail = { value: { id: fileId, versionNo: 'A/1', processInstanceId: 'real-bpm' } }
    const context = { exports: {}, Error, workflowDistributionVisible: { value: true }, canReadDistributionReceipts: { value: true },
      fileDetail, distributionReceiptReader: { state }, readDistributionAuthorityKey: () => 'tenant1:actor99',
      resolveReadSideErrorMessage: error => error.message,
      reloadAll: async () => { if (changed) fileDetail.value = { id: '8', versionNo: 'B/1', processInstanceId: 'new-bpm' }; throw new Error('详情读取失败') } }
    vm.runInNewContext(ts.transpileModule(page.slice(start, end) + '\nexports.handler = handleAutomaticDistributionRetried',
      { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
    await context.exports.handler(result())
    assert.equal(state.automaticResult.status, 'FAILED')
    if (changed) assert.equal(state.automaticResultError, '')
    else assert.match(state.automaticResultError, /详情读取失败/)
  }
})
