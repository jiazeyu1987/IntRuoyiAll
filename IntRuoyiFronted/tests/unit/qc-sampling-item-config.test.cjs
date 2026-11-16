const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const ctx = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/mes/pro/processpool/qaRegulationItemInspection.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, ctx)
const api = ctx.exports
const plain = obj => JSON.parse(JSON.stringify(obj))
test('QA editor preserves first quantity, inspection level and AQL as independent item fields', () => {
  const i = api.createQaItemInspectionState({ applicableInspectionTypes: ['FIRST', 'PATROL'], firstInspectionQuantity: 13,
    inspectionLevel: 'I', aql: 0.4 })
  const s = api.createQaItemInspectionState({ applicableInspectionTypes: ['FIRST', 'PATROL'], firstInspectionQuantity: 5,
    inspectionLevel: 'S3', aql: 0.4 })
  assert.deepEqual(plain(api.resolveQaItemInspectionPayload(i, false, '外观')), {
    applicableInspectionTypes: ['FIRST', 'PATROL'], firstInspectionQuantity: 13, inspectionLevel: 'I', aql: 0.4 })
  assert.deepEqual(plain(api.resolveQaItemInspectionPayload(s, false, '强度')), {
    applicableInspectionTypes: ['FIRST', 'PATROL'], firstInspectionQuantity: 5, inspectionLevel: 'S3', aql: 0.4 })
})
test('missing or numeric percentage cannot stand in for inspection level', () => {
  for (const value of [undefined, '', 'S-', '0.4', 0.4]) {
    const state = { patrolInspectionEnabled: true, firstInspectionEnabled: false, inspectionLevel: value }
    assert.equal(api.isQaItemInspectionConfigurationComplete(state, false), false)
    assert.throws(() => api.resolveQaItemInspectionPayload(state, false, '强度'), /检验水平/)
  }
})
