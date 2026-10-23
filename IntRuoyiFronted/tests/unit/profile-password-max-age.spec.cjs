const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/infra/config/basic/components/IdleLogoutSetting.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
const script = compileScript(descriptor, { id: 'profile-password-max-age' })
const template = compileTemplate({
  source: descriptor.template.content,
  filename,
  id: 'profile-password-max-age',
  compilerOptions: { bindingMetadata: script.bindings }
})
assert.deepEqual(template.errors, [])
const code = ts.transpileModule(script.content, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText

const KEY = 'system.user.password-max-age-days'
const initialRow = {
  id: 7, category: 'security', name: '系统账号密码有效期（天）', key: KEY,
  value: '90', type: 1, visible: false, remark: '系统账号统一设置'
}

function createForm(canUpdate = true) {
  const result = {
    rows: [{ ...initialRow }], reads: [], saved: [], errors: [], successes: [], mounted: []
  }
  const imports = {
    vue,
    '@/api/infra/config': {
      getConfigPage: async (params) => {
        result.reads.push(params)
        if (result.readError) throw result.readError
        return { list: result.rows.map((row) => ({ ...row })) }
      },
      updateConfig: async (row) => {
        if (result.saveError) throw result.saveError
        if (result.pendingSave) await result.pendingSave
        result.saved.push({ ...row })
        result.rows = [{ ...row }]
      }
    },
    '@/store/modules/testMode': {
      useTestModeStore: () => ({ apply() {} }),
      parseTestMode: () => { throw new Error('Unrelated test mode must not be modified') },
      TEST_MODE_CONFIG_KEY: 'system.ui.test-mode-enabled'
    }
  }
  const exports = {}
  vm.runInNewContext(code, {
    ...vue, exports, Error,
    onMounted: (callback) => result.mounted.push(callback),
    useMessage: () => ({
      success: (message) => result.successes.push(message),
      error: (message) => result.errors.push(message)
    }),
    require: (name) => {
      if (Object.hasOwn(imports, name)) return imports[name]
      throw new Error(`Unexpected import: ${name}`)
    }
  })
  result.props = vue.reactive({ canUpdate })
  result.state = exports.default.setup(result.props, { expose() {} })
  return result
}

test('profile basic configuration loads the stored expiry and saves only that configuration', async () => {
  const form = createForm()
  const state = form.state
  assert.equal(state.passwordMaxAgeDays.value, '')
  assert.ok(form.mounted.includes(state.loadPasswordMaxAge))
  form.rows.unshift({ ...initialRow, id: 8, key: `${KEY}.unrelated`, value: 'text' })
  await state.loadPasswordMaxAge()
  assert.equal(state.passwordMaxAgeDays.value, '90')
  assert.equal(form.reads[0].key, KEY)
  state.passwordMaxAgeDays.value = '180'
  await state.savePasswordMaxAge()
  assert.deepEqual(form.saved, [{ ...initialRow, value: '180' }])
  assert.deepEqual(form.successes, ['密码有效期已保存'])
  await state.loadPasswordMaxAge()
  assert.equal(state.passwordMaxAgeDays.value, '180')
})

test('invalid expiry values are rejected without writes and valid boundaries are accepted', async () => {
  const { state, saved, errors } = createForm()
  await state.loadPasswordMaxAge()
  for (const value of ['', '0', '-1', '1.5', ' 90', '90\n', '090', 'abc', '1e2', '2147483648']) {
    state.passwordMaxAgeDays.value = value
    await state.savePasswordMaxAge()
    assert.match(state.passwordMaxAgeError.value, /正整数天数/)
  }
  assert.equal(saved.length, 0)
  assert.equal(errors.length, 10)
  for (const value of ['1', '2147483647']) {
    state.passwordMaxAgeDays.value = value
    await state.savePasswordMaxAge()
    assert.equal(saved.at(-1).value, value)
    assert.equal(state.passwordMaxAgeError.value, '')
  }
})

test('view-only users can read but cannot save, including when permission is revoked', async () => {
  const form = createForm(false)
  await form.state.loadPasswordMaxAge()
  assert.equal(form.state.passwordMaxAgeDays.value, '90')
  form.state.passwordMaxAgeDays.value = '30'
  await form.state.savePasswordMaxAge()
  assert.equal(form.saved.length, 0)
  assert.match(form.errors.at(-1), /没有基础配置修改权限/)
  form.props.canUpdate = true
  await form.state.savePasswordMaxAge()
  form.props.canUpdate = false
  await form.state.savePasswordMaxAge()
  assert.equal(form.saved.length, 1)
})

test('missing, duplicate, invalid and unavailable configuration clears loaded data and blocks saving', async () => {
  const form = createForm()
  for (const rows of [[], [initialRow, initialRow], [{ ...initialRow, value: '0' }]]) {
    form.rows = [{ ...initialRow }]
    await form.state.loadPasswordMaxAge()
    form.rows = rows
    await form.state.loadPasswordMaxAge()
    assert.equal(form.state.passwordMaxAgeRow.value, null)
    assert.equal(form.state.passwordMaxAgeDays.value, '')
    assert.ok(form.state.passwordMaxAgeError.value)
    await form.state.savePasswordMaxAge()
  }
  form.readError = new Error('配置服务不可用')
  await form.state.loadPasswordMaxAge()
  assert.equal(form.state.passwordMaxAgeError.value, '配置服务不可用')
  assert.equal(form.state.passwordMaxAgeLoading.value, false)
  assert.equal(form.saved.length, 0)
  assert.equal(form.successes.length, 0)
})

test('save failure remains visible, preserves the unsaved input and allows a retry', async () => {
  const form = createForm()
  await form.state.loadPasswordMaxAge()
  form.state.passwordMaxAgeDays.value = '365'
  form.saveError = { response: { data: { msg: '无参数修改权限' } } }
  await form.state.savePasswordMaxAge()
  assert.equal(form.state.passwordMaxAgeError.value, '无参数修改权限')
  assert.equal(form.state.passwordMaxAgeDays.value, '365')
  assert.equal(form.state.passwordMaxAgeRow.value.value, '90')
  assert.equal(form.state.passwordMaxAgeSaving.value, false)
  assert.equal(form.successes.length, 0)
  form.saveError = null
  await form.state.savePasswordMaxAge()
  assert.equal(form.saved[0].value, '365')
  assert.equal(form.state.passwordMaxAgeError.value, '')
})

test('pending saves prevent duplicate submissions and refresh races', async () => {
  const form = createForm()
  await form.state.loadPasswordMaxAge()
  let completeSave
  form.pendingSave = new Promise((resolve) => { completeSave = resolve })
  form.state.passwordMaxAgeDays.value = '180'
  const pending = form.state.savePasswordMaxAge()
  await form.state.savePasswordMaxAge()
  await form.state.loadPasswordMaxAge()
  assert.equal(form.reads.length, 1)
  assert.equal(form.state.passwordMaxAgeDays.value, '180')
  completeSave()
  await pending
  assert.equal(form.saved.length, 1)
  assert.equal(form.state.passwordMaxAgeSaving.value, false)
})
