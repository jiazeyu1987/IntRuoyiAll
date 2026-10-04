const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const exportsObject = {}
const source = fs.readFileSync(path.join(__dirname, '../../src/views/dcc/controlled-file/project-attributes/state.ts'), 'utf8')
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
  { exports: exportsObject, Error })
const { createAttributeState, loadProjectDefaults, restoreDefaults, editActual, buildSnapshot } = exportsObject
const defaults = market => ({ targetMarkets: [market], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' })
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
async function initialized() {
  const state = createAttributeState('UPLOAD')
  await loadProjectDefaults(state, '1', async () => defaults('NMPA'), async () => true)
  return state
}
async function run() {
  let failures = 0
  async function test(name, action) {
    try { await action(); console.log('PASS: ' + name) }
    catch (cause) { failures++; console.error('FAIL: ' + name + ': ' + cause.message) }
  }
  await test('默认读取期间新的手改不被晚返回覆盖', async () => {
    const state = await initialized(), response = deferred()
    const loading = loadProjectDefaults(state, '2', async () => response.promise, async () => true)
    editActual(state, defaults('CE'))
    response.resolve(defaults('FDA'))
    assert.equal(await loading, false)
    assert.equal(state.projectId, '1')
    assert.deepEqual(Array.from(state.actual.targetMarkets), ['CE'])
    assert.equal(state.dirty, true)
  })
  await test('恢复默认确认期间切换项目不能重置新项目', async () => {
    const state = await initialized(), confirmation = deferred()
    editActual(state, defaults('CE'))
    const restoring = restoreDefaults(state, async () => defaults('NMPA'), async () => confirmation.promise)
    await loadProjectDefaults(state, '2', async () => defaults('FDA'), async () => true)
    confirmation.resolve(true)
    assert.equal(await restoring, false)
    assert.equal(state.projectId, '2')
    assert.deepEqual(Array.from(state.actual.targetMarkets), ['FDA'])
  })
  await test('旧恢复请求失败不能污染成功的新项目', async () => {
    const state = await initialized(), response = deferred(), started = deferred()
    editActual(state, defaults('CE'))
    const restoring = restoreDefaults(state, async () => { started.resolve(); return response.promise }, async () => true)
    await started.promise
    await loadProjectDefaults(state, '2', async () => defaults('FDA'), async () => true)
    response.reject(new Error('旧请求失败'))
    assert.equal(await restoring, false)
    assert.equal(state.error, '')
    assert.equal(state.dirty, false)
    assert.equal(buildSnapshot(state).projectId, '2')
  })
  await test('当前恢复失败必须保留手改且错误可见', async () => {
    const state = await initialized()
    editActual(state, defaults('CE'))
    await assert.rejects(restoreDefaults(state, async () => { throw new Error('当前失败') }, async () => true), /当前失败/)
    assert.equal(state.error, '当前失败')
    assert.equal(state.dirty, true)
    assert.deepEqual(Array.from(state.actual.targetMarkets), ['CE'])
  })
  if (failures) throw new Error(failures + ' attribute race tests failed')
}
run().catch(error => { console.error(error); process.exitCode = 1 })
