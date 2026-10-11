const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const panel = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const source = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const declaration = ast.statements.find(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(value => value.name.getText(ast) === 'formatProductionMaterialTabLabel'))
assert.ok(declaration, 'exercise the actual production page formatter')
const code = ts.transpileModule(declaration.getText(ast) + '\nreturn formatProductionMaterialTabLabel',
  { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
const material = (key, materialCode, materialName = '压力表') => ({ key, materialCode, materialName })
function formatter(materials, selected = '', draft = {}, saved = {}) {
  return new Function('configuredProductionMaterials', 'selectedProductionMaterialKey',
    'productionDraft', 'productionDefectDraft', 'productionMaterialDrafts', code)(
    { value: materials }, { value: selected }, draft, { damage: 1, contamination: 2 }, saved)
}
test('unique material names keep the existing employee label and quantity summary', () => {
  const value = material('one', 'A001')
  assert.equal(formatter([value])(value), '压力表')
  assert.equal(formatter([value], 'one', { outputQuantity: 10 })(value), '压力表(10/3)')
})
test('same-name outputs remain distinguishable before and after entering quantities', () => {
  const one = material('one', 'A001.02.033.106'), two = material('two', 'A001.02.033.107')
  const empty = formatter([one, two])
  assert.equal(empty(one), '压力表 / A001.02.033.106')
  assert.equal(empty(two), '压力表 / A001.02.033.107')
  const filled = formatter([one, two], 'one', { outputQuantity: 10 },
    { two: { outputQuantity: 8, defectQuantities: { damage: 2 } } })
  assert.equal(filled(one), '压力表 / A001.02.033.106(10/3)')
  assert.equal(filled(two), '压力表 / A001.02.033.107(8/2)')
})
test('an ambiguous output without its formal code is rejected instead of sharing a label', () => {
  const one = material('one', ''), two = material('two', 'A002')
  assert.throws(() => formatter([one, two])(one), /同名输出物料缺少正式物料编码/)
})
