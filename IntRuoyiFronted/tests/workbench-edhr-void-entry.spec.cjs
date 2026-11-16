const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
function source() {
  const module = { exports: {} }
  const js = ts.transpileModule(fs.readFileSync('src/workbench-sources/bpm/registration.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(js, { module, exports: module.exports })
  return module.exports.default.sources[0]
}
test('batch void opens the same formal BPM process and exact engine task', async () => {
  const routes = []
  await source().open({ push: r => routes.push(r) }, { taskId: 'task-uuid', processInstanceId: 'process-uuid' }, () => true)
  assert.equal(routes.length, 1)
  assert.equal(routes[0].path, '/bpm/process-instance/detail')
  assert.equal(routes[0].query.id, 'process-uuid')
  assert.equal(routes[0].query.taskId, 'task-uuid')
})
test('missing engine identity and stale workbench context never navigate', async () => {
  for (const payload of [{ taskId: 'task' }, { processInstanceId: 'process' }, { taskId: ' task', processInstanceId: 'process' }]) {
    let calls = 0
    await assert.rejects(source().open({ push: () => { calls++ } }, payload, () => true))
    assert.equal(calls, 0)
  }
  await assert.rejects(source().open({ push: () => { throw new Error('Unexpected navigation') } }, { taskId: 'task', processInstanceId: 'process' }, () => false), /上下文/)
})
test('browser and server manifests register only the dedicated void source in BPM', () => {
  const browser = JSON.parse(fs.readFileSync('src/workbench/manifest.json', 'utf8'))
  const server = JSON.parse(fs.readFileSync('../IntRuoyiBackend/yudao-server/src/main/resources/workbench-manifest.json', 'utf8'))
  assert.deepEqual(browser, server)
  assert.deepEqual(browser.modules.find(m => m.ownerModule === 'bpm').sourceIds, ['BPM_EDHR_VOID'])
})
