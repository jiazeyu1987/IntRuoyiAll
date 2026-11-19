const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const code = ts.transpileModule(fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/edhr-batch/reverseTraceState.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const user = { getUser: { id: 7 }, getPermissions: new Set(['mes:pro-edhr-batch-execution:query']) }
  const scope = { tenant: 1, visitTenant: undefined, now: 1000 }
  const exports = {}
  vm.runInNewContext(code, { exports, Date: { now: () => scope.now }, require: id => {
    if (id === '@/utils/uuid') return loadUuid(crypto)
    if (id === '@/utils/auth') return { getTenantId: () => scope.tenant, getVisitTenantId: () => scope.visitTenant }
    if (id === '@/store/modules/user') return { useUserStore: () => user }
    throw new Error(`Unexpected module: ${id}`)
  } })
  const state = () => ({ origin: { path: exports.reverseTraceHistoryPath, query: {} },
    anchorBatchExecutionId: '123', catalogVersion: 'v1', conditions: [],
    queryResponse: { queryHash: 'hash-1' } })
  return { ...exports, user, scope, state }
}

test('HTTP reverse trace snapshots use independent UUID v4 keys and preserve copy and revocation semantics', () => {
  const p = setup()
  const original = p.state()
  const first = p.saveReverseTraceState(original)
  const second = p.saveReverseTraceState(p.state())
  assert.match(first, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.notEqual(second, first)
  original.conditions.push({ value: 'changed' })
  assert.equal(p.readReverseTraceState(first).conditions.length, 0)
  const read = p.readReverseTraceState(first)
  read.conditions.push({ value: 'changed' })
  assert.equal(p.readReverseTraceState(first).conditions.length, 0)
  p.revokeReverseTraceSnapshot({ anchorBatchExecutionId: '123', catalogVersion: 'v1' }, 'hash-1')
  assert.equal(p.readReverseTraceState(first), undefined)
  assert.equal(p.readReverseTraceState(second), undefined)
})

test('reverse trace ownership and expiry still invalidate retained snapshots', () => {
  const p = setup()
  const first = p.saveReverseTraceState(p.state())
  p.scope.visitTenant = 2
  assert.equal(p.readReverseTraceState(first), undefined)
  p.scope.visitTenant = undefined
  const second = p.saveReverseTraceState(p.state())
  p.scope.now += 30 * 60 * 1000
  assert.equal(p.readReverseTraceState(second), undefined)
  const third = p.saveReverseTraceState(p.state())
  p.user.getPermissions.clear()
  assert.equal(p.readReverseTraceState(third), undefined)
})

test('missing or failing entropy cannot create a reverse trace key', () => {
  const p = setup({})
  assert.throws(() => p.saveReverseTraceState(p.state()), /不支持安全随机数/)
  const cause = new Error('entropy unavailable')
  const failing = setup({ getRandomValues() { throw cause } })
  assert.throws(() => failing.saveReverseTraceState(failing.state()), error => error === cause)
})
