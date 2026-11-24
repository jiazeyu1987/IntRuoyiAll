const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const filename = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const source = fs.readFileSync(filename, 'utf8')
const descriptor = parse(source).descriptor
const ast = ts.createSourceFile('designer.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = [
  'isRouteGroupTemplate', 'isProductionFlow', 'routeStartProductionLeaderControlsDisabled',
  'createRouteStartProductionLeaderDraft', 'normalizeRouteStartProductionLeaders',
  'buildRouteStartProductionLeaderSaveItems', 'serializeRouteStartProductionLeaderSaveItems',
  'markRouteStartProductionLeadersClean', 'hasRouteStartProductionLeaderDraftChanges',
  'resolveRouteStartProductionLeaderReadRouteVersionId', 'resolveRouteStartProductionLeaderProductionLine',
  'requireRouteStartProductionLeaderSaveRouteVersionId', 'handleRouteStartProductionLeaderAdd',
  'loadRouteStartProductionLeaders', 'saveRouteStartProductionLeaders', 'saveRouteStartProductionLeadersIfChanged'
]
const declarations = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(declarations.length, names.length)
const code = ts.transpileModule(declarations.map(s => s.getText(ast)).join('\n') +
  `\nexports.actions = { ${names.join(',')} };`, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function page(nodeType, { draft = true } = {}) {
  const requests = [], saved = []
  const ref = value => ({ value })
  const ctx = {
    exports: {}, computed: fn => ({ get value() { return fn() } }),
    props: { routeNodeType: nodeType, routeId: '922039',
      routeVersionEditContext: { routeVersionId: '48', lifecycleStatus: draft ? 'DRAFT' : 'ACTIVE' } },
    routeFlowWriteControlsDisabled: ref(false), isDraftCandidateEdit: ref(draft),
    routeStartProductionLeaderProductionLines: ref([]), routeStartProductionLeaders: ref([]),
    routeStartProductionLeadersLoaded: ref(false), routeStartProductionLeadersLoading: ref(false),
    routeStartProductionLeadersSaving: ref(false), routeStartProductionLeadersLoadError: ref(''),
    routeStartProductionLeadersBaseline: ref(''), routeStartProductionLeaderDraftSequence: 0,
    normalizeRouteStartProductionLeaderCandidateSourceType: value => value || 'USERS',
    normalizeRecordBindingCandidateIds: values => values || [],
    normalizeRecordBindingCandidateNames: values => values || [],
    loadRecordBindingUserOptions: async () => { requests.push('users') },
    loadRecordBindingRoleOptions: async () => { requests.push('roles') },
    requireCandidateRouteVersionId: () => {
      if (!draft) throw new Error('仅草稿可配置生产组长')
      return '48'
    },
    refreshProductionProcessConfigLock: async () => {}, ensureSelectedProcessRouteConfigCache: async () => {},
    resolveErrorMessage: error => error.message,
    message: { success() {}, warning() {}, error(message) { throw new Error(message) } },
    ProRouteFlowConfigApi: {
      getRouteStartProductionLeaders: async () => { requests.push('leaders'); return saved },
      getRouteStartProductionLeaderProductionLines: async () => {
        requests.push('ranges')
        return [{ productionLineId: '922039', productionLineName: 'IDPR' }]
      },
      saveRouteStartProductionLeaders: async payload => {
        requests.push('save')
        saved.splice(0, saved.length, ...JSON.parse(JSON.stringify(payload.items)))
      }
    }
  }
  vm.runInNewContext(code, ctx)
  return { ...ctx.exports.actions, ctx, requests, saved }
}

test('GROUP with legacy copied scope neither loads nor saves any production authorization', async () => {
  const p = page('GROUP')
  p.ctx.routeStartProductionLeaders.value = [{ productionLineId: '922039', candidateSourceIds: [913913] }]
  p.ctx.routeStartProductionLeadersLoaded.value = true
  p.ctx.routeStartProductionLeadersBaseline.value = 'old polluted snapshot'
  await p.loadRouteStartProductionLeaders(true)
  await p.saveRouteStartProductionLeadersIfChanged()
  p.handleRouteStartProductionLeaderAdd()
  assert.equal(p.ctx.routeStartProductionLeaders.value.length, 1)
  assert.equal(p.hasRouteStartProductionLeaderDraftChanges(), false)
  assert.equal(p.routeStartProductionLeaderControlsDisabled.value, true)
  await assert.rejects(p.saveRouteStartProductionLeaders(), /目录模板不参与生产授权/)
  assert.deepEqual(p.requests, [])
})

test('missing and unknown node types fail closed instead of behaving as FLOW', async () => {
  for (const nodeType of [undefined, null, '', 'INVALID']) {
    const p = page(nodeType)
    await p.loadRouteStartProductionLeaders()
    assert.equal(p.isProductionFlow.value, false)
    assert.equal(p.isRouteGroupTemplate.value, false)
    assert.equal(p.routeStartProductionLeaderControlsDisabled.value, true)
    await assert.rejects(p.saveRouteStartProductionLeaders(), /节点类型缺失或无效/)
    assert.deepEqual(p.requests, [])
  }
})

test('FLOW still loads, requires a selected leader, and saves only its own current scope', async () => {
  const p = page('FLOW')
  await p.loadRouteStartProductionLeaders()
  p.handleRouteStartProductionLeaderAdd()
  const leader = p.ctx.routeStartProductionLeaders.value[0]
  await assert.rejects(p.saveRouteStartProductionLeaders(), /请先选择/)
  leader.candidateSourceIds = [913913]
  leader.productionLineId = '922044'
  await assert.rejects(p.saveRouteStartProductionLeaders(), /负责范围必须来自当前工艺路线/)
  leader.productionLineId = '922039'
  await p.saveRouteStartProductionLeaders()
  assert.equal(p.saved[0].productionLineId, '922039')
  assert.deepEqual(p.saved[0].candidateSourceIds, [913913])
  assert.equal(p.requests.filter(r => r === 'save').length, 1)
  assert.equal(p.hasRouteStartProductionLeaderDraftChanges(), false)
})

test('published FLOW remains read only and cannot save authorization as a draft', async () => {
  const p = page('FLOW', { draft: false })
  await p.loadRouteStartProductionLeaders()
  assert.equal(p.routeStartProductionLeaderControlsDisabled.value, true)
  p.handleRouteStartProductionLeaderAdd()
  assert.equal(p.ctx.routeStartProductionLeaders.value.length, 0)
  await assert.rejects(p.saveRouteStartProductionLeaders(), /仅草稿/)
  assert.equal(p.requests.includes('save'), false)
})

test('real SFC compiles and parent passes the actual node type to the designer', () => {
  const compiled = compileTemplate({ source: descriptor.template.content, filename, id: 'group-leader-scope' })
  assert.deepEqual(compiled.errors, [])
  const form = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFormContent.vue'), 'utf8')
  assert.match(form, /:route-node-type="formData.nodeType"/)
  assert.match(source, /v-if="isRouteGroupTemplate"/)
  assert.match(source, /v-else-if="!isProductionFlow"/)
})
