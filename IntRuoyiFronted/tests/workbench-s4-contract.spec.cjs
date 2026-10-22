const test = require('node:test')
const assert = require('node:assert/strict')
const { fixture, read, compile, dir, descriptor } = require('./helpers/workbench-s3-fixture.cjs')
const loadContract = value => {
  const m = { exports: {} }
  const source = read('src/workbench/queryContract.ts').replace('import.meta.env.VITE_WORKBENCH_QUERY_CONTRACT', JSON.stringify(value))
  Function('module', 'exports', compile(source))(m, m.exports)
  return m.exports
}
test('deployment explicitly selects 3 or 4; malformed values fail before requests', () => {
  assert.equal(loadContract(undefined).workbenchQueryContractVersion, 3)
  assert.equal(loadContract('4').workbenchQueryContractVersion, 4)
  for (const value of ['', 'auto', '04', '5']) assert.throws(() => loadContract(value), /配置无效/)
})
test('S4 context is accepted only by a deployment configured for S4', async () => {
  for (const version of ['3', '4']) {
    const f = fixture({ queryContract: version })
    f.setTransport((r, next) => r.url.endsWith('/context')
      ? Promise.resolve({ ...r.data, contractVersion: 4, visibilityVersion: '1', queryVersion: 'a'.repeat(43), expiresAt: '2099-01-01T00:00:00Z' })
      : next(r))
    const call = () => f.api.initializeWorkbenchContext(dir.registryRevision, [descriptor], 'epoch', { visibility: 'visible' })
    if (version === '4') assert.equal((await call()).contractVersion, 4)
    else await assert.rejects(call(), /上下文合同不一致/)
    f.scope.stop()
  }
})
test('mixed-engine count response fails; an S4 request carries only its token and epoch', async () => {
  const f = fixture({ queryContract: '4' })
  f.setTransport(r => Promise.resolve(r.url.endsWith('/context')
    ? { ...r.data, contractVersion: 4, visibilityVersion: '1', queryVersion: 'b'.repeat(43), expiresAt: '2099-01-01T00:00:00Z' }
    : { contractVersion: 3, registryRevision: dir.registryRevision, ...r.params, visibilityVersion: '1', filteredTotal: 0, businessTotal: 0, hiddenTotal: 0, readAt: '2026-10-10T12:00:00' }))
  const context = await f.api.initializeWorkbenchContext(dir.registryRevision, [descriptor], 'epoch', { visibility: 'visible' })
  await assert.rejects(() => f.api.getProfileWorkbenchTodoCount(context), /读取归属合同不一致/)
  assert.deepEqual(Object.keys(f.requests.at(-1).params).sort(), ['contextEpoch', 'queryVersion'])
  f.scope.stop()
})
