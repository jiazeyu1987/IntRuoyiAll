const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { computed, reactive } = require('vue')
const { parse } = require('vue/compiler-sfc')

const source = parse(fs.readFileSync('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const names = ['normalizeProductionMaterialCode', 'parseProductionMaterialListQuantity', 'inputMaterialSources', 'productionMaterialListActualUsageByCode', 'pickListMaterials', 'pickListDocuments']
const body = names.map(name => {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, name)
  return statement.getText(ast)
}).join('\n')
const js = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const setup = detail => Function('props', 'computed', `${js}; return { pickListMaterials, pickListDocuments, productionMaterialListActualUsageByCode }`)({ detail }, computed)
const material = (overrides = {}) => ({ materialCode: 'MAT-1', actualQuantity: 10, baseActualQuantity: 10, batchCodes: ['LOT-1'], sourcePickListNos: ['PICK-1'], sourcePickListDocuments: [{ id: '2118566488787140609', billNo: 'PICK-1', productionOrderNos: ['WO-1'] }], ...overrides })

test('order inputs render without process bindings and retain document and batch evidence', () => {
  const detail = { inputMaterialUsages: [material()], processes: [{ processName: '初包装', inputMaterials: [] }] }
  const s = setup(detail)
  assert.deepEqual(s.pickListMaterials.value[0].sourceProcessNames, ['订单级领料'])
  assert.deepEqual(s.pickListMaterials.value[0].batchCodes, ['LOT-1'])
  assert.equal(s.pickListDocuments.value[0].id, '2118566488787140609')
  assert.equal(s.pickListDocuments.value[0].rows.length, 1)
  assert.equal(s.productionMaterialListActualUsageByCode.value.get('MAT-1'), 10)
  assert.deepEqual(detail.processes[0].inputMaterials, [])
})

test('explicit process and order references to the same issue do not duplicate quantities or rows', () => {
  const s = setup({ inputMaterialUsages: [material()], processes: [{ processName: '初包装', inputMaterials: [material()] }] })
  assert.equal(s.pickListMaterials.value.length, 1)
  assert.equal(s.pickListDocuments.value[0].rows.length, 1)
  assert.deepEqual(s.pickListMaterials.value[0].sourceProcessNames, ['订单级领料', '初包装'])
  assert.equal(s.productionMaterialListActualUsageByCode.value.get('MAT-1'), 10)
})

test('distinct batches remain separate and absent document metadata is not invented', () => {
  const s = setup({ inputMaterialUsages: [material(), material({ batchCodes: ['LOT-2'], sourcePickListNos: [], sourcePickListDocuments: [] })] })
  assert.equal(s.pickListMaterials.value.length, 2)
  assert.equal(s.pickListDocuments.value.length, 1)
  assert.equal(s.pickListDocuments.value[0].rows.length, 1)
})

test('empty input stays empty and reactive detail changes discard previous order evidence', () => {
  const detail = reactive({ inputMaterialUsages: [material()], processes: [] })
  const s = setup(detail)
  assert.equal(s.pickListMaterials.value.length, 1)
  detail.inputMaterialUsages = []
  assert.deepEqual(s.pickListMaterials.value, [])
  assert.deepEqual(s.pickListDocuments.value, [])
  assert.equal(s.productionMaterialListActualUsageByCode.value.size, 0)
  assert.deepEqual(setup(undefined).pickListMaterials.value, [])
})
