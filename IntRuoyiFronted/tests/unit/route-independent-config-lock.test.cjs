const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('designer.ts', script, ts.ScriptTarget.Latest, true)
const names = ['refreshProductionProcessConfigLock', 'handleBatchRecordAttachmentOwnerInit',
  'handleBatchRecordAttachmentOwnerSave', 'saveRouteStartProductionLeaders',
  'saveSelectedProductionProcessConfig']
const declarations = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(declarations.length, names.length)
const code = ts.transpileModule(declarations.map(s => s.getText(ast)).join('\n') +
  `\nexports.actions = { ${names.join(',')} };`, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup() {
  const ref = value => ({ value })
  const versionId = '2121453182031057263'
  const processId = '2121453182219802725'
  let revision = 0
  const hash = () => `revision-${revision}`
  const config = { routeProcessId: processId, overagePercent: 5, deviceSelectionGroups: [] }
  const snapshot = () => ({ routeVersionId: versionId, routeSnapshotSha256: hash(),
    productionProcessConfigs: [config] })
  const cache = { readableRouteVersionId: versionId, productionProcessConfigSnapshot: snapshot() }
  const writes = [], successes = [], errors = []
  const ctx = {
    exports: {}, props: { routeId: '2121453182031056996' },
    selectedProcessRouteConfigCache: ref(cache), selectedNode: ref({ routeProcessId: processId }),
    operationError: ref(''), productionConfigSavedMessage: ref(''),
    batchRecordAttachmentOwners: ref([{ attachmentName: '报告', candidateSourceIds: [1] }]),
    batchRecordAttachmentOwnersLoaded: ref(false), batchRecordAttachmentOwnersInitializing: ref(false),
    batchRecordAttachmentOwnersSaving: ref(false), batchRecordAttachmentOwnersLoadError: ref(''),
    routeStartProductionLeadersSaving: ref(false), routeStartProductionLeadersLoadError: ref(''),
    productionProcessConfigJsonDrafts: { [processId]: 'unsaved draft' },
    productionProcessConfigJsonBaselines: { [processId]: 'original' },
    requireCandidateRouteVersionId: () => versionId,
    ensureSelectedProcessRouteConfigCache: async () => {
      ctx.selectedProcessRouteConfigCache.value = cache
      return cache
    },
    requireRouteStartProductionLeaderSaveRouteVersionId: () => versionId,
    buildRouteStartProductionLeaderSaveItems: () => [{ candidateSourceIds: [1] }],
    normalizeBatchRecordAttachmentOwners: items => items,
    loadBatchRecordAttachmentOwners: async () => {}, loadRouteStartProductionLeaders: async () => {},
    hasRouteProcessStructureChanges: () => false,
    parseProductionProcessConfigDraft: () => ({ ...config, overagePercent: 6 }),
    sameRouteProcessId: (a, b) => String(a) === String(b),
    normalizeProductionProcessConfig: c => c, routeProcessIdKey: String,
    resetSelectedRouteProcessDeviceParameterConfig() {},
    resolveErrorMessage: error => error.message,
    message: { success: text => successes.push(text), error: text => errors.push(text) },
    ProRouteFlowConfigApi: {
      initBatchRecordAttachmentOwners: async () => { revision++; return [] },
      saveBatchRecordAttachmentOwners: async () => { revision++ },
      saveRouteStartProductionLeaders: async () => { revision++ }
    },
    ProRouteApi: {
      getRouteProductionProcessConfig: async id => {
        assert.equal(id, versionId)
        return snapshot()
      },
      saveRouteProductionProcessConfig: async body => {
        assert.equal(body.expectedRouteSnapshotSha256, hash(), 'must use the latest candidate lock')
        writes.push(body)
        revision++
      }
    }
  }
  vm.runInNewContext(code, ctx)
  return { ctx, cache, writes, successes, errors, processId, versionId, ...ctx.exports.actions }
}

for (const action of ['handleBatchRecordAttachmentOwnerInit', 'handleBatchRecordAttachmentOwnerSave',
  'saveRouteStartProductionLeaders']) {
  test(`${action} refreshes the lock and preserves device/config drafts before the next save`, async () => {
    const t = setup()
    const priorConfigs = t.cache.productionProcessConfigSnapshot.productionProcessConfigs
    // Start-node actions can run before any process detail has lazily loaded the cache.
    t.ctx.selectedProcessRouteConfigCache.value = undefined
    await t[action]()
    assert.equal(t.cache.productionProcessConfigSnapshot.routeSnapshotSha256, 'revision-1')
    assert.equal(t.cache.productionProcessConfigSnapshot.productionProcessConfigs, priorConfigs)
    assert.equal(t.ctx.productionProcessConfigJsonDrafts[t.processId], 'unsaved draft')
    await t.saveSelectedProductionProcessConfig()
    assert.equal(t.writes.length, 1)
    assert.equal(t.writes[0].productionProcessConfigs[0].overagePercent, 6)
    assert.equal(t.ctx.operationError.value, '')
  })
}

test('lock refresh failure reports an error instead of success and retains unsaved drafts', async () => {
  const t = setup()
  t.ctx.ProRouteApi.getRouteProductionProcessConfig = async () => { throw new Error('读取版本锁失败') }
  await t.handleBatchRecordAttachmentOwnerSave()
  assert.match(t.ctx.batchRecordAttachmentOwnersLoadError.value, /读取版本锁失败/)
  assert.equal(t.successes.length, 0)
  assert.equal(t.ctx.productionProcessConfigJsonDrafts[t.processId], 'unsaved draft')
  assert.equal(t.writes.length, 0)
})

test('an obsolete version response cannot refresh a different candidate cache', async () => {
  const t = setup()
  t.cache.readableRouteVersionId = 'another-candidate'
  await assert.rejects(t.refreshProductionProcessConfigLock(t.versionId), /版本锁缓存不可用/)
  assert.equal(t.cache.productionProcessConfigSnapshot.routeSnapshotSha256, 'revision-0')
})
