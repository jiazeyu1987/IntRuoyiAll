import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'

const source = readFileSync(new URL('../src/views/mes/pro/route/routeTreePublishMatrix.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
} }).outputText
const scope = { exports: {} }
runInNewContext(compiled, scope)
const { buildRouteTreePublishMatrix } = scope.exports
const change = (field, before, after) => ({ field: `configSnapshots.${field}`, before, after })
const node = (routeId, changes = [], blocker) => ({ routeId, name: `流程${routeId}`, changes, blocker })

test('按配置行、子节点列分别保留每个子节点的覆盖前后值', () => {
  const rows = buildRouteTreePublishMatrix([
    node('101', [change('lossCoefficient', 1, 2)]),
    node('102', [change('lossCoefficient', 1.5, 2)]),
    node('103')
  ])
  assert.equal(rows.length, 1)
  assert.equal(rows[0].field, 'lossCoefficient')
  assert.equal(rows[0].cells.get('101')[0].before, 1)
  assert.equal(rows[0].cells.get('102')[0].before, 1.5)
  assert.equal(rows[0].cells.get('102')[0].after, 2)
  assert.equal(rows[0].cells.has('103'), false)
})

test('冲突节点不推测覆盖结果，也不产生虚假的无变化记录', () => {
  const rows = buildRouteTreePublishMatrix([
    node('101', [change('lossCoefficient', 1, 2)]),
    node('102', [change('lossCoefficient', 1.5, 2)], '工序缺失')
  ])
  assert.equal(rows[0].cells.has('102'), false)
  assert.equal(buildRouteTreePublishMatrix([node('102', [], '工序缺失')]).length, 0)
})

test('同名工序的多处修改全部保留，设备参数与排产配置独立成行', () => {
  const rows = buildRouteTreePublishMatrix([node('101', [
    change('productionProcessConfigs.检验.parameterRules.温度.upperLimit', 20, 30),
    change('productionProcessConfigs.检验.parameterRules.温度.upperLimit', 21, 31),
    change('scheduleConfigs.201.hourlyCapacity', 50, 60)
  ])])
  assert.equal(rows.length, 2)
  assert.equal(rows[0].field, 'parameterRules')
  assert.equal(rows[0].cells.get('101').length, 2)
  assert.equal(rows[1].field, 'scheduleConfigs')
})

test('保留工序新增、删除以及清空绑定的确认内容', () => {
  const rows = buildRouteTreePublishMatrix([node('101', [
    change('flowGraph.nodes.检验', undefined, { processName: '检验' }),
    change('flowGraph.nodes.清洗', { processName: '清洗' }, undefined),
    change('batchUseConfigs.检验.formBindings', [{ bindingKey: 'A' }], [])
  ])])
  assert.equal(rows.length, 2)
  assert.equal(rows[0].cells.get('101').length, 2)
  assert.equal(rows[0].cells.get('101')[1].after, undefined)
  assert.equal(rows[1].cells.get('101')[0].after.length, 0)
})
