const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const path = require('node:path')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/process/ProcessCraftBindingPanel.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true)
const code = ts.transpileModule(ast.statements.filter(n => !ts.isImportDeclaration(n)).map(n => n.getText(ast)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const active = { id: 11, routeId: 7, versionNo: '1', active: true, lifecycleStatus: 'ACTIVE' }
const draft = { id: 12, routeId: 7, versionNo: '2', active: false, lifecycleStatus: 'DRAFT' }
const plain = value => JSON.parse(JSON.stringify(value))
function setup({ list = [active, draft], readonly = false, permission = true, getConfig, migrate } = {}) {
  const reads = [], creates = [], notices = [], migrations = []
  const state = {
    defineProps: () => ({ processId: 5, routeId: 7, routeProcessId: 9, readonly }),
    ref: value => ({ value }), computed: get => ({ get value() { return get() } }),
    watch: () => {}, onBeforeUnmount: () => {}, checkPermi: () => permission,
    defineExpose: () => {},
    useMessage: () => ({ success: text => notices.push(text) }),
    ProRouteApi: {
      getRouteVersionList: async () => list,
      createRouteCandidateVersion: async input => { creates.push(plain(input)); list.push(draft); return draft },
      migrateProductionConfig: async input => {
        migrations.push(plain(input))
        if (migrate) await migrate(input)
        list = list.map(version => version.id === input.routeVersionId ? { ...version, legacyProductionConfigMissing: false } : version)
      }
    },
    ProRouteFlowConfigApi: { getRouteProcessDeviceParameterConfig: async (versionId, processId) => {
      reads.push([versionId, processId])
      return getConfig ? getConfig(versionId) : { processId: 5, routeVersionId: versionId, routeSnapshotSha256: 'hash', craftBindings: [{ craftId: 20, craftCode: 'GY-0020', craftDeviceIds: [4] }], devices: [] }
    } }
  }
  vm.createContext(state)
  vm.runInContext(`${code}\nthis.panel={versions,selectedVersionId,config,loading,error,editable,changeReason,loadVersions,loadConfig,createDraft,handleSaved,canMigrate,migrationPercent,migrateProductionConfig}`, state)
  return { panel: state.panel, reads, creates, notices, migrations }
}

test('legacy version is explicitly missing, never treated as empty craft bindings', async () => {
  const { panel, reads } = setup({ list: [{ ...active, legacyProductionConfigMissing: true }] })
  await panel.loadVersions()
  assert.equal(reads.length, 0)
  assert.equal(panel.config.value, undefined)
  assert.equal(panel.editable.value, false)
  assert.equal(panel.canMigrate.value, false)
})

test('explicit draft migration sends exact version and hash then reloads newly available bindings', async () => {
  const { panel, migrations, notices } = setup({ list: [{ ...draft, legacyProductionConfigMissing: true, routeSnapshotSha256: 'before-migration' }] })
  await panel.loadVersions()
  assert.equal(panel.canMigrate.value, true)
  panel.migrationPercent.value = 5
  await panel.migrateProductionConfig()
  assert.deepEqual(migrations, [{ routeVersionId: 12, expectedRouteSnapshotSha256: 'before-migration', missingOveragePercent: 5 }])
  assert.equal(panel.config.value.craftBindings[0].craftId, 20)
  assert.equal(panel.canMigrate.value, false)
  assert.equal(notices.length, 1)
})

test('migration failure stays visible without inventing a business percentage or emitting success', async () => {
  const { panel, migrations, notices } = setup({
    list: [{ ...draft, legacyProductionConfigMissing: true, routeSnapshotSha256: 'before-migration' }],
    migrate: () => { throw new Error('工序未配置超量比例') }
  })
  await panel.loadVersions()
  await panel.migrateProductionConfig()
  assert.equal(Object.hasOwn(migrations[0], 'missingOveragePercent'), false)
  assert.match(panel.error.value, /未配置超量比例/)
  assert.equal(notices.length, 0)
  assert.equal(panel.config.value, undefined)
})

test('read-only and missing permission block migration without a write', async () => {
  for (const options of [{ readonly: true }, { permission: false }]) {
    const { panel, migrations } = setup({ ...options, list: [{ ...draft, legacyProductionConfigMissing: true, routeSnapshotSha256: 'sha' }] })
    await panel.loadVersions()
    await panel.migrateProductionConfig()
    assert.equal(migrations.length, 0)
  }
})
test('shows active version binding without permitting edits to the active version', async () => {
  const { panel, reads } = setup()
  await panel.loadVersions()
  assert.deepEqual(reads, [[11, 9]])
  assert.equal(panel.config.value.craftBindings[0].craftId, 20)
  assert.equal(panel.editable.value, false)
  panel.selectedVersionId.value = 12
  await panel.loadConfig()
  assert.equal(panel.editable.value, true)
})
test('multiple drafts require explicit selection, with no arbitrary version chosen', async () => {
  const { panel, reads } = setup({ list: [draft, { ...draft, id: 13 }] })
  await panel.loadVersions()
  assert.equal(panel.selectedVersionId.value, undefined)
  assert.equal(reads.length, 0)
})
test('view mode and missing update permission keep draft bindings read only', async () => {
  for (const options of [{ readonly: true }, { permission: false }]) {
    const { panel } = setup({ ...options, list: [draft] })
    await panel.loadVersions()
    assert.equal(panel.editable.value, false)
  }
})
test('rejects version config belonging to another process or missing snapshot hash', async () => {
  for (const config of [{ processId: 99 }, { processId: 5 }]) {
    const { panel } = setup({ list: [draft], getConfig: () => config })
    await panel.loadVersions()
    assert.ok(panel.error.value)
    assert.equal(panel.config.value, undefined)
    assert.equal(panel.editable.value, false)
  }
})
test('creates a draft from explicitly selected active version and selects the returned draft', async () => {
  const { panel, creates } = setup({ list: [active] })
  await panel.loadVersions()
  panel.changeReason.value = ' 增加工艺 '
  await panel.createDraft()
  assert.deepEqual(creates, [{ routeId: 7, sourceRouteVersionId: 11, changeReason: '增加工艺' }])
  assert.equal(panel.selectedVersionId.value, 12)
  assert.equal(panel.editable.value, true)
})
test('save refreshes binding data and optimistic concurrency hash', async () => {
  let count = 0
  const { panel } = setup({ list: [draft], getConfig: () => ({ processId: 5, routeSnapshotSha256: `hash-${++count}`, craftBindings: [] }) })
  await panel.loadVersions()
  await panel.handleSaved()
  assert.equal(panel.config.value.routeSnapshotSha256, 'hash-2')
})
test('late config response cannot overwrite a subsequently selected version', async () => {
  let resolveFirst
  let started
  const ready = new Promise(resolve => { started = resolve })
  const { panel } = setup({ getConfig: id => id === 11 ? new Promise(resolve => { resolveFirst = resolve; started() }) : { processId: 5, routeVersionId: id, routeSnapshotSha256: 'draft-hash' } })
  const first = panel.loadVersions()
  await ready
  panel.selectedVersionId.value = 12
  await panel.loadConfig()
  resolveFirst({ processId: 5, routeVersionId: 11, routeSnapshotSha256: 'old-hash' })
  await first
  assert.equal(panel.config.value.routeVersionId, 12)
})

function setupBinding(disabled = false, fail = false) {
  const text = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/route/components/RouteCraftBinding.vue'), 'utf8')
  const script = text.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const parsed = ts.createSourceFile('binding.ts', script, ts.ScriptTarget.Latest, true)
  const compiled = ts.transpileModule(parsed.statements.filter(n => !ts.isImportDeclaration(n)).map(n => n.getText(parsed)).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  const writes = [], events = []
  const state = {
    defineProps: () => ({ processId: 5, routeVersionId: 12, routeProcessId: 9, snapshotSha256: 'current-hash', bindings: [], devices: [], disabled }),
    defineEmits: () => name => events.push(name), ref: value => ({ value }),
    computed: get => ({ get value() { return get() } }), watch: () => {},
    CraftApi: { bind: async data => { if (fail) throw new Error('版本已更新，请刷新'); writes.push(plain(data)) } }
  }
  vm.createContext(state)
  vm.runInContext(`${compiled}\nthis.binding={rows,error,bindCraft}`, state)
  return { binding: state.binding, writes, events }
}
test('binding preserves existing crafts while adding a craft with exact version, devices and hash', async () => {
  const { binding, writes, events } = setupBinding()
  binding.rows.value = [{ key: 1, craftId: 20, deviceIds: [4] }, { key: 2, craftId: 21, deviceIds: [6] }]
  await binding.bindCraft()
  assert.deepEqual(writes, [{ routeVersionId: 12, routeProcessId: 9, expectedRouteSnapshotSha256: 'current-hash', bindings: [{ craftId: 20, deviceIds: [4] }, { craftId: 21, deviceIds: [6] }] }])
  assert.deepEqual(events, ['saved'])
})
test('duplicate crafts, duplicate devices and readonly binding never write', async () => {
  for (const [disabled, rows] of [
    [false, [{ craftId: 20, deviceIds: [4] }, { craftId: 20, deviceIds: [6] }]],
    [false, [{ craftId: 20, deviceIds: [4] }, { craftId: 21, deviceIds: [4] }]],
    [true, [{ craftId: 20, deviceIds: [4] }]]
  ]) {
    const { binding, writes, events } = setupBinding(disabled)
    binding.rows.value = rows
    await binding.bindCraft()
    assert.equal(writes.length, 0)
    assert.equal(events.length, 0)
  }
})
test('failed binding keeps the error visible and does not emit success', async () => {
  const { binding, events } = setupBinding(false, true)
  binding.rows.value = [{ craftId: 20, deviceIds: [4] }]
  await binding.bindCraft()
  assert.match(binding.error.value, /版本已更新/)
  assert.equal(events.length, 0)
})
