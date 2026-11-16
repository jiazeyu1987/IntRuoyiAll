const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('designer.ts', script, ts.ScriptTarget.Latest, true)
const names = [
  'createRouteStartProductionLeaderDraft', 'normalizeRouteStartProductionLeaders',
  'buildRouteStartProductionLeaderSaveItems', 'serializeRouteStartProductionLeaderSaveItems',
  'markRouteStartProductionLeadersClean', 'hasRouteStartProductionLeaderDraftChanges',
  'resolveRouteStartProductionLeaderReadRouteVersionId', 'resolveRouteStartProductionLeaderProductionLine',
  'requireRouteStartProductionLeaderSaveRouteVersionId', 'handleRouteStartProductionLeaderAdd',
  'handleRouteStartProductionLeaderProductionLineChange', 'loadRouteStartProductionLeaders',
  'saveRouteStartProductionLeaders'
]
const declarations = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
assert.equal(declarations.length, names.length)
const code = ts.transpileModule(declarations.map(statement => statement.getText(ast)).join('\n') +
  `\nexports.actions = { ${names.join(',')} };`, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

// Execute the page's actual handlers; only external services and unrelated candidate selectors are stubbed.
function page(routeId) {
  const versionId = '2116486795650920465'
  const ref = value => ({ value })
  const line = { productionLineId: routeId, productionLineCode: 'GYLC-0001', productionLineName: '解析生成路线' }
  let saved = []
  const requests = []
  const ctx = {
    exports: {},
    props: { routeId, routeVersionEditContext: { routeVersionId: versionId, lifecycleStatus: 'DRAFT' } },
    routeStartProductionLeaderProductionLines: ref([]), routeStartProductionLeaders: ref([]),
    routeStartProductionLeadersLoaded: ref(false), routeStartProductionLeadersLoading: ref(false),
    routeStartProductionLeadersSaving: ref(false), routeStartProductionLeadersLoadError: ref(''),
    routeStartProductionLeadersBaseline: ref(''), routeStartProductionLeaderControlsDisabled: ref(false),
    routeStartProductionLeaderDraftSequence: 0,
    normalizeRouteStartProductionLeaderCandidateSourceType: value => value || 'USERS',
    normalizeRecordBindingCandidateIds: values => values || [],
    normalizeRecordBindingCandidateNames: values => values || [],
    loadRecordBindingUserOptions: async () => {}, loadRecordBindingRoleOptions: async () => {},
    requireCandidateRouteVersionId: () => versionId,
    resolveErrorMessage: error => error.message,
    message: { success() {}, warning() {}, error(message) { throw new Error(message) } },
    ProRouteFlowConfigApi: {
      getRouteStartProductionLeaders: async () => saved,
      getRouteStartProductionLeaderProductionLines: async () => [line],
      saveRouteStartProductionLeaders: async request => {
        // Simulate the JSON transport and the server's exact current-route identity check.
        const payload = JSON.parse(JSON.stringify(request))
        assert.equal(payload.routeId, routeId)
        assert.equal(payload.routeVersionId, versionId)
        for (const item of payload.items) assert.equal(String(item.productionLineId), String(routeId))
        requests.push(payload)
        saved = payload.items
      }
    }
  }
  vm.runInNewContext(code, ctx)
  return { ...ctx.exports.actions, ctx, requests }
}

test('newly parsed route keeps its long ID through add, select, save and reload', async () => {
  const id = '2116486795512508425'
  const p = page(id)
  await p.loadRouteStartProductionLeaders()
  p.handleRouteStartProductionLeaderAdd()
  const leader = p.ctx.routeStartProductionLeaders.value[0]
  assert.equal(leader.productionLineId, id)
  p.handleRouteStartProductionLeaderProductionLineChange(leader, id)
  assert.equal(leader.productionLineName, '解析生成路线')
  assert.ok(p.resolveRouteStartProductionLeaderProductionLine(leader))
  leader.candidateSourceIds = [123]
  leader.candidateSourceNames = ['生产组长']
  await p.saveRouteStartProductionLeaders()
  assert.equal(p.requests[0].items[0].productionLineId, id)
  assert.equal(p.ctx.routeStartProductionLeaders.value[0].productionLineId, id)
  assert.equal(p.hasRouteStartProductionLeaderDraftChanges(), false)
  // The select must use the same string identity as the draft, rather than showing a raw unmatched ID.
  assert.match(source, /:value="String\(line.productionLineId\)"/)
})

test('different long IDs that collide as Numbers cannot pass the page scope check', async () => {
  const id = '2116486795512508425'
  const p = page(id)
  await p.loadRouteStartProductionLeaders()
  p.handleRouteStartProductionLeaderAdd()
  const leader = p.ctx.routeStartProductionLeaders.value[0]
  leader.candidateSourceIds = [123]
  for (const invalid of ['2116486795512508426', String(Number(id))]) {
    assert.equal(Number(invalid), Number(id))
    p.handleRouteStartProductionLeaderProductionLineChange(leader, invalid)
    assert.equal(p.resolveRouteStartProductionLeaderProductionLine(leader), undefined)
    await assert.rejects(p.saveRouteStartProductionLeaders(), /负责范围必须来自当前工艺路线/)
  }
  assert.equal(p.requests.length, 0)
})

test('small numeric route IDs normalize consistently with select values and save correctly', async () => {
  const p = page(123)
  await p.loadRouteStartProductionLeaders()
  p.handleRouteStartProductionLeaderAdd()
  const leader = p.ctx.routeStartProductionLeaders.value[0]
  p.handleRouteStartProductionLeaderProductionLineChange(leader, '123')
  leader.candidateSourceIds = [456]
  await p.saveRouteStartProductionLeaders()
  assert.equal(p.requests[0].items[0].productionLineId, '123')
  assert.equal(p.ctx.routeStartProductionLeaders.value[0].productionLineId, '123')
})
