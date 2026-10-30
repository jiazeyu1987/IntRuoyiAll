const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '..')
const source = fs.readFileSync(path.join(root, 'src/views/erp/sync/syncRunProgress.ts'), 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const exportsObject = {}
vm.runInNewContext(compiled, { exports: exportsObject })
const { getSyncRunCount, getSyncRunProcessedCount, describeSyncRunProgress } = exportsObject
const run = (changes = {}) => ({
  id: 91, syncType: 'PRODUCTION_MATERIAL_LIST', triggerType: 'FULL', status: 10,
  createdCount: 0, updatedCount: 0, skippedCount: 0, failedCount: 0, ...changes
})

test('unreported running counters are pending rather than false zero progress', () => {
  for (const key of ['createdCount', 'updatedCount', 'skippedCount', 'failedCount']) {
    assert.equal(getSyncRunCount(run(), key), '待统计')
  }
  assert.equal(getSyncRunProcessedCount(run()), '待统计')
  assert.match(describeSyncRunProgress(run()), /计数待上报/)
})

test('an all-skipped full sync still shows its actual processed volume', () => {
  const value = run({ skippedCount: 730000 })
  assert.equal(getSyncRunCount(value, 'createdCount'), 0)
  assert.equal(getSyncRunCount(value, 'skippedCount'), 730000)
  assert.equal(getSyncRunProcessedCount(value), 730000)
  assert.match(describeSyncRunProgress(value), /部分计数/)
})

test('failed task preserves committed counts without inventing failed source rows', () => {
  const value = run({ status: 30, createdCount: 200, updatedCount: 30, skippedCount: 70 })
  assert.equal(getSyncRunProcessedCount(value), 300)
  assert.equal(getSyncRunCount(value, 'failedCount'), '未统计')
  assert.equal(getSyncRunCount(run({ status: 30, failedCount: 8 }), 'failedCount'), 8)
  assert.match(describeSyncRunProgress(value), /任务失败/)
})

test('successful empty run reports real zeros and no history remains distinct', () => {
  assert.equal(getSyncRunProcessedCount(run({ status: 20 })), 0)
  assert.equal(getSyncRunCount(run({ status: 20 }), 'failedCount'), 0)
  assert.equal(getSyncRunProcessedCount(undefined), '-')
  assert.equal(getSyncRunCount(undefined, 'createdCount'), '-')
})

test('missing or invalid counts never become zero or a fabricated total', () => {
  for (const invalid of [undefined, null, '12', NaN, Infinity, -1, 1.2]) {
    const value = run({ status: 20, createdCount: invalid })
    assert.equal(getSyncRunCount(value, 'createdCount'), '未统计')
    assert.equal(getSyncRunProcessedCount(value), '未统计')
  }
})

test('modified Vue component compiles with complete template expressions', () => {
  const filename = path.join(root, 'src/views/erp/sync/config/components/TableAutoSyncSetting.vue')
  const parsed = parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(parsed.errors, [])
  const script = compileScript(parsed.descriptor, { id: 'erp-sync-progress' })
  const template = compileTemplate({
    source: parsed.descriptor.template.content, filename, id: 'erp-sync-progress',
    compilerOptions: { bindingMetadata: script.bindings }
  })
  assert.deepEqual(template.errors, [])
})
