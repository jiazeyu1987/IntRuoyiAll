// Component behavior tests; no browser, server, or production data is used.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const elementRequire = createRequire(require.resolve('element-plus'))
const Schema = elementRequire('async-validator').default

const filename = path.resolve(__dirname, '../src/views/infra/config/ConfigForm.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
const script = compileScript(descriptor, { id: 'password-max-age-form' })
const template = compileTemplate({
  source: descriptor.template.content, filename, id: 'password-max-age-form',
  compilerOptions: { bindingMetadata: script.bindings }
})
assert.deepEqual(template.errors, [])

const KEY = 'system.user.password-max-age-days'
const saved = [], messages = [], emitted = []
let row = { id: 7, category: 'security', name: '系统账号密码有效期（天）', key: KEY,
  value: '90', visible: false, remark: '' }
let saveError
const imports = {
  vue,
  '@/utils/dict': { DICT_TYPE: {}, getBoolDictOptions: () => [] },
  '@/api/infra/config': {
    getConfig: async () => ({ ...row }),
    updateConfig: async (value) => {
      if (saveError) throw saveError
      saved.push({ ...value })
    },
    createConfig: async () => { throw new Error('Unexpected create') }
  }
}
const evaluated = { exports: {} }
vm.runInNewContext(ts.transpileModule(script.content, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText, {
  ...vue, Error, exports: evaluated.exports, module: evaluated,
  useI18n: () => ({ t: (key) => key }),
  useMessage: () => ({ success: (message) => messages.push(message) }),
  require: (name) => {
    if (Object.hasOwn(imports, name)) return imports[name]
    throw new Error(`Unexpected import: ${name}`)
  }
})
const state = evaluated.exports.default.setup({}, {
  expose: () => {}, emit: (event) => emitted.push(event)
})
state.formRef.value = {
  resetFields() {},
  async validate() {
    // Element Plus removes the trigger metadata before calling async-validator.
    const rules = Object.fromEntries(Object.entries(state.formRules).map(([field, items]) =>
      [field, items.map(({ trigger, ...rule }) => rule)]))
    await new Schema(rules).validate(state.formData.value)
    return true
  }
}

async function main() {
  await state.open('update', 7)
  assert.equal(state.isPasswordMaxAge.value, true)
  assert.equal(state.passwordMaxAgeKeyLocked.value, true)
  assert.equal(state.formData.value.value, '90')
  for (const invalid of ['', '0', '-1', '1.5', ' 90', '90\n', '090', 'abc', '2147483648']) {
    state.formData.value.value = invalid
    await assert.rejects(state.submitForm)
  }
  assert.equal(saved.length, 0)
  state.formData.value.value = '180'
  await state.submitForm()
  assert.equal(saved[0].value, '180')
  assert.equal(saved[0].key, KEY)
  assert.equal(saved[0].visible, false)
  assert.deepEqual(emitted, ['success'])
  assert.equal(state.dialogVisible.value, false)

  await state.open('update', 7)
  saveError = new Error('Save failed')
  await assert.rejects(state.submitForm, (error) => error === saveError)
  assert.equal(state.dialogVisible.value, true)
  assert.equal(state.formLoading.value, false)
  assert.equal(messages.length, 1)
  assert.equal(saved.length, 1)

  saveError = undefined
  row = { ...row, key: 'unrelated.config', value: 'text-value' }
  await state.open('update', 7)
  assert.equal(state.isPasswordMaxAge.value, false)
  assert.equal(state.passwordMaxAgeKeyLocked.value, false)
  await state.submitForm()
  assert.equal(saved[1].value, 'text-value')
  console.log('PASS: password expiry form loads, validates, saves, surfaces failures, and preserves other config forms')
}
main().catch((error) => { console.error(error); process.exitCode = 1 })
