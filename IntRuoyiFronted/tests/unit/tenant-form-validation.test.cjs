const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const elementRequire = createRequire(require.resolve('element-plus'))
const Schema = elementRequire('async-validator').default

// Execute the forms' real rules through the validator used by Element Plus.
function loadRules(file) {
  const source = fs.readFileSync(path.join(__dirname, '../../src/views/system', file), 'utf8')
  const { descriptor } = parse(source)
  const script = ts.createSourceFile('form.ts', descriptor.scriptSetup.content,
    ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  let initializer
  for (const statement of script.statements) {
    if (!ts.isVariableStatement(statement)) continue
    for (const declaration of statement.declarationList.declarations) {
      if (declaration.name.getText(script) === 'formRules') initializer = declaration.initializer
    }
  }
  assert.ok(initializer, 'formRules must exist')
  const code = ts.transpileModule(`const rules = ${initializer.getText(script)}; rules`, {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  return vm.runInNewContext(code, { reactive: value => value, Error })
}

const tenant = loadRules('tenant/TenantForm.vue')
const tenantPackage = loadRules('tenantPackage/TenantPackageForm.vue')

for (const [rules, field, limit, message] of [
  [tenant, 'name', 30, '租户名长度不能超过30个字符'],
  [tenant, 'contactName', 30, '联系人长度不能超过30个字符'],
  [tenantPackage, 'name', 30, '套餐名长度不能超过30个字符'],
  [tenantPackage, 'remark', 256, '备注长度不能超过256个字符']
]) {
  test(`${message}: accepts the limit and rejects one extra character`, async () => {
    const schema = new Schema({ [field]: rules[field] })
    await schema.validate({ [field]: '字'.repeat(limit) })
    await assert.rejects(schema.validate({ [field]: '字'.repeat(limit + 1) }), error => {
      assert.equal(error.errors[0].message, message)
      return true
    })
  })
}

test('website limit includes stored separators and permits an omitted list', async () => {
  const schema = new Schema({ websites: tenant.websites })
  for (const websites of [undefined, null, [], ['a'.repeat(1024)], ['a'.repeat(511), 'b'.repeat(512)]]) {
    await schema.validate({ websites })
  }
  for (const websites of [['a'.repeat(1025)], ['a'.repeat(512), 'b'.repeat(512)]]) {
    await assert.rejects(schema.validate({ websites }), error => {
      assert.equal(error.errors[0].message, '绑定域名总长度不能超过1024个字符（含分隔逗号）')
      return true
    })
  }
})

test('optional remark remains optional and required names remain required', async () => {
  const remark = new Schema({ remark: tenantPackage.remark })
  for (const value of [undefined, null, '']) await remark.validate({ remark: value })
  for (const [rules, field] of [[tenant, 'name'], [tenant, 'contactName'], [tenantPackage, 'name']]) {
    await assert.rejects(new Schema({ [field]: rules[field] }).validate({ [field]: '' }))
  }
})
