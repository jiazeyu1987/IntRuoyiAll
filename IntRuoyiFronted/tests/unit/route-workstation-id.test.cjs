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
const names = ['normalizeRouteProcessWorkstationId', 'normalizePositiveProcessId', 'numericValue',
  'getBoundRouteProcessWorkstationId', 'buildGlobalWorkstationRepairSourceOptions',
  'buildRouteProcessRepairSourceOptions', 'applyCapacityWorkstationBindingLocally',
  'clearRouteProcessWorkstationIdBaselines', 'resetRouteProcessWorkstationIdBaselines', 'getChangedRouteProcessWorkstationNodes',
  'buildRouteProcessUpdatePayload', 'createCapacityWorkstationForRouteProcess']
const statements = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(statements.length, names.length)
const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') +
  `\nexports.actions = { ${names.join(',')} };`, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup() {
  const processId = '2117828464390516872'
  const routeProcessId = '2121453182219802725'
  const oldId = '2121333283992494166'
  const newId = '2121333283992494168'
  const row = { id: routeProcessId, processId, sort: 1, routeProcessWorkstationId: oldId }
  const ctx = {
    exports: {}, props: { routeVersionEditContext: {}, routeId: '2121453182031056996' },
    CommonStatusEnum: { ENABLE: 0 },
    routeNodes: { value: [{ ...row, routeProcessId }] },
    routeProcessRows: { value: [row] }, candidateAwareRouteProcessRows: { value: [row] },
    capacityWorkstationRepairWorkstationOptions: { value: [oldId, newId].map(id =>
      ({ id, processId, code: id, status: 0, shiftHours: 8, todayCapacity: 120 })) },
    capacityOverrideRouteProcessId: { value: routeProcessId }, selectedProcessAttributes: {},
    routeProcessWorkstationIdBaselines: {}, isLoadedRouteProcessId: () => true,
    requireRouteProcessId: id => id, sameRouteProcessId: (a, b) => String(a) === String(b),
    getChangedRouteProcessUpdateNodes: () => ctx.exports.actions.getChangedRouteProcessWorkstationNodes(),
    capacityWorkstationRepairForm: { workshopId: 1 },
    resolveCapacityWorkstationRepairShiftHoursForCreate: () => 8,
    buildCapacityWorkstationName: () => 'SIMULATION',
    AutoCodeRecordApi: { generateAutoCode: async () => 'SIMULATION-WS' },
    MesAutoCodeRuleCode: { MD_WORKSTATION_CODE: 'WORKSTATION' },
    MdWorkstationApi: { createWorkstation: async () => newId }
  }
  vm.runInNewContext(code, ctx)
  return { ctx, routeProcessId, oldId, newId, ...ctx.exports.actions }
}

test('adjacent Long workstation IDs remain distinct from option to serialized graph update', () => {
  const t = setup()
  assert.equal(Number(t.oldId), Number(t.newId), 'fixture reproduces JavaScript precision collision')
  const options = t.buildGlobalWorkstationRepairSourceOptions()
  assert.deepEqual(Array.from(options, x => x.workstationId), [t.oldId, t.newId])
  assert.notEqual(options[0].value, options[1].value)
  t.resetRouteProcessWorkstationIdBaselines()
  t.applyCapacityWorkstationBindingLocally(t.ctx.routeProcessRows.value[0], options[1])
  const payload = JSON.parse(JSON.stringify(t.buildRouteProcessUpdatePayload()))
  assert.equal(payload.length, 1, 'different Long IDs must produce a persisted node update')
  assert.equal(payload[0].id, t.routeProcessId)
  assert.equal(payload[0].workstationId, t.newId)
})

test('reuse excludes only the selected route process even when adjacent Long IDs collide as numbers', () => {
  const t = setup()
  const otherId = '2121453182219802726'
  t.ctx.candidateAwareRouteProcessRows.value.push({
    ...t.ctx.routeProcessRows.value[0], id: otherId
  })
  const options = t.buildRouteProcessRepairSourceOptions()
  assert.equal(options.length, 1)
  assert.equal(options[0].sourceRouteProcessId, otherId)
})

test('newly created workstation keeps full returned ID; unsafe numeric IDs are rejected', async () => {
  const t = setup()
  assert.equal((await t.createCapacityWorkstationForRouteProcess(t.ctx.routeProcessRows.value[0])).workstationId,
    t.newId)
  t.ctx.MdWorkstationApi.createWorkstation = async () => Number(t.newId)
  await assert.rejects(t.createCapacityWorkstationForRouteProcess(t.ctx.routeProcessRows.value[0]),
    /未返回有效编号/)
})

test('candidate binding requires explicit ID and equivalent safe number/string IDs remain unchanged', () => {
  const t = setup()
  assert.equal(t.getBoundRouteProcessWorkstationId({ workstationId: t.newId }), undefined)
  assert.equal(t.normalizeRouteProcessWorkstationId(Number(t.newId)), null)
  t.ctx.routeNodes.value[0].routeProcessWorkstationId = 123
  t.resetRouteProcessWorkstationIdBaselines()
  t.ctx.routeNodes.value[0].routeProcessWorkstationId = '123'
  assert.equal(t.getChangedRouteProcessWorkstationNodes().length, 0)
})
