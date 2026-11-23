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
function initializer(file, variable, context) {
  const { descriptor } = parse(fs.readFileSync(path.join(base, file), 'utf8'))
  const script = ts.createSourceFile('form.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  for (const statement of script.statements) {
    if (!ts.isVariableStatement(statement)) continue
    for (const declaration of statement.declarationList.declarations) {
      if (declaration.name.getText(script) !== variable) continue
      return vm.runInNewContext(ts.transpileModule(`const result = ${declaration.initializer.getText(script)}; result`, {
        compilerOptions: { target: ts.ScriptTarget.ES2022 }
      }).outputText, context)
    }
  }
  assert.fail(`Missing ${variable}`)
}
const rules = initializer('controlled-file/directories/components/DirectoryForm.vue', 'formRules', { reactive: value => value })
for (const [field, max, label] of [['code', 64, '目录编码'], ['name', 128, '目录名称'], ['remark', 255, '备注']]) {
  test(`${label} validates the real form storage boundary`, async () => {
    const validator = new Schema({ [field]: rules[field] })
    await validator.validate({ [field]: '字'.repeat(max) })
    await assert.rejects(validator.validate({ [field]: '字'.repeat(max + 1) }), error => {
      assert.equal(error.errors[0].message, `${label}长度不能超过${max}个字符`)
      return true
    })
  })
}
const validateNames = initializer('registration-certificate/change/ChangeDialog.vue', 'validateChangeTextValues', { Error })
for (const [field, label] of [['PRODUCT_NAME', '产品名称'], ['REGISTRANT_NAME', '注册人名称']]) {
  test(`${label} rejects oversized registration changes with a specific message`, () => {
    assert.doesNotThrow(() => validateNames({ [field]: '  ' + '字'.repeat(255) + '  ' }))
    assert.throws(() => validateNames({ [field]: '字'.repeat(256) }), { message: `${label}长度不能超过255个字符` })
  })
}
test('unrelated certificate fields retain their existing limits', () => {
  assert.doesNotThrow(() => validateNames({ PRODUCT_STRUCTURE: '字'.repeat(4096) }))
})
