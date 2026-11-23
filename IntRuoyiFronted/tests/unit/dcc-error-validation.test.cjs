const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const Schema = createRequire(require.resolve('element-plus'))('async-validator').default
const base = path.join(__dirname, '../../src/views/dcc')
const { descriptor } = parse(fs.readFileSync(path.join(base, 'controlled-file/categories/components/CategoryForm.vue'), 'utf8'))
const script = ts.createSourceFile('form.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
let initializer
for (const statement of script.statements) {
  if (!ts.isVariableStatement(statement)) continue
  for (const declaration of statement.declarationList.declarations) {
    if (declaration.name.getText(script) === 'formRules') initializer = declaration.initializer
  }
}
assert.ok(initializer)
const rules = vm.runInNewContext(ts.transpileModule(`const rules = ${initializer.getText(script)}; rules`, {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText, { reactive: value => value })
for (const [field, max, label] of [['code', 64, '类别编码'], ['name', 128, '类别名称'], ['source', 32, '类别来源'], ['remark', 255, '备注'], ['description', 255, '类别说明']]) {
  test(`${label} validates the storage boundary in the real form rules`, async () => {
    const validator = new Schema({ [field]: rules[field] })
    await validator.validate({ [field]: '字'.repeat(max) })
    await assert.rejects(validator.validate({ [field]: '字'.repeat(max + 1) }), error => {
      assert.equal(error.errors[0].message, `${label}长度不能超过${max}个字符`)
      return true
    })
  })
}
const factsModule = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(base, 'registration-certificate/shared/facts.ts'), 'utf8'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText, { exports: factsModule.exports, Error })
const { validateFacts } = factsModule.exports
const ceFacts = () => ({ certificateType: 'CE', certificateStatus: 'VALID', ownProduct: false, classification: 'II', expiryDate: '2030-01-01', ceCertificates: [] })
for (const [field, max, label] of [['certificateNo', 128, '注册证号'], ['classification', 64, '产品分类'], ['remark', 1024, '备注']]) {
  test(`${label} returns an actionable registration certificate message`, () => {
    const facts = { ...ceFacts(), [field]: '字'.repeat(max) }
    assert.doesNotThrow(() => validateFacts(facts))
    facts[field] += '字'
    assert.throws(() => validateFacts(facts), { message: `${label}长度不能超过${max}个字符` })
  })
}
test('every CE certificate row is checked, including rows before the final projection', () => {
  const facts = ceFacts()
  facts.ceCertificates = [{ revision: 'Rev.0', certificateNo: '字'.repeat(128) }, { revision: 'Rev.1' }]
  assert.doesNotThrow(() => validateFacts(facts))
  facts.ceCertificates[0].certificateNo += '字'
  assert.throws(() => validateFacts(facts), { message: '第1条CE证书编号长度不能超过128个字符' })
})
