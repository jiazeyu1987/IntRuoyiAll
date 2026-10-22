const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require(require.resolve('@vue/compiler-sfc', { paths: [path.dirname(require.resolve('vue'))] }))
const validatorModule = require(require.resolve('async-validator', { paths: [path.dirname(require.resolve('element-plus'))] }))
const Schema = validatorModule.default || validatorModule
const root = path.resolve(__dirname, '../..')
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
function moduleFrom(file, dependencies) {
  const module = { exports: {} }
  Function('require', 'module', 'exports', compile(fs.readFileSync(path.join(root, file), 'utf8')))(id => {
    assert.ok(Object.hasOwn(dependencies, id), `unexpected dependency: ${id}`)
    return dependencies[id]
  }, module, module.exports)
  return module.exports
}
async function harness(original = {}) {
  const puts = [], errors = [], successes = [], names = [], events = []
  let saved = { nickname: 'original', sex: 1, avatar: 'https://example.test/avatar.png', ...original }
  const axios = moduleFrom('src/config/axios/index.ts', {
    './config': { config: { default_headers: 'application/json' } },
    './service': { service: async options => {
      if (options.method === 'GET') return { data: { ...saved } }
      const payload = JSON.parse(JSON.stringify(options.data))
      puts.push({ url: options.url, data: payload })
      saved = { ...saved, ...payload }
      return { data: true }
    } }
  }).default
  const api = moduleFrom('src/api/system/user/profile.ts', { '@/config/axios': axios })
  const descriptor = parse(fs.readFileSync(path.join(root, 'src/views/Profile/components/BasicInfo.vue'), 'utf8')).descriptor
  const ast = ts.createSourceFile('BasicInfo.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const source = ast.statements.filter(statement => !ts.isImportDeclaration(statement)).map(statement => statement.getText(ast)).join('\n')
  const context = {
    ...vue, defineOptions() {}, defineEmits: () => event => events.push(event),
    useI18n: () => ({ t: key => key }), useMessage: () => ({ success: value => successes.push(value) }),
    useUserStore: () => ({ getUser: { avatar: '' }, setUserNicknameAction: async value => names.push(value) }),
    onMounted() {}, ...api
  }
  const component = Function(...Object.keys(context), `${compile(source)}; return { rules, submit, init, formRef };`)(...Object.values(context))
  const formModel = vue.reactive({})
  component.formRef.value = {
    formModel, setValues: values => Object.assign(formModel, values),
    getElFormRef: () => ({ validate: async callback => {
      errors.length = 0
      try { await new Schema(component.rules).validate({ ...formModel }) }
      catch (failure) {
        assert.ok(Array.isArray(failure.errors), 'unexpected validator failure must not become a validation result')
        errors.push(...failure.errors)
        await callback(false)
        return false
      }
      await callback(true)
      return true
    } })
  }
  await component.init()
  return { component, formModel, puts, errors, successes, names, events }
}
test('missing contacts permit nickname-only save; JSON omits empty contacts and unrelated profile fields', async () => {
  for (const mobile of [undefined, null, '', '   ']) {
    for (const email of [undefined, null, '', '   ']) {
      const h = await harness({ mobile, email, id: 42, roles: [{ id: 1 }], dept: { id: 2 } })
      h.formModel.nickname = 'updated'
      await h.component.submit()
      assert.equal(h.puts.length, 1)
      assert.deepEqual(h.puts[0], { url: '/system/user/profile/update', data: { nickname: 'updated', sex: 1, avatar: 'https://example.test/avatar.png' } })
      assert.deepEqual(h.names, ['updated'])
      assert.deepEqual(h.events, ['success'])
      assert.equal(h.formModel.nickname, 'updated')
    }
  }
})
test('only one missing contact is omitted; valid nonempty values are trimmed', async () => {
  for (const missing of ['email', 'mobile']) {
    const h = await harness({ mobile: '13800138000', email: 'old@example.test', [missing]: null })
    h.formModel.nickname = 'updated'
    h.formModel[missing] = ' '
    await h.component.submit()
    assert.equal(h.puts.length, 1)
    assert.equal(Object.hasOwn(h.puts[0].data, missing), false)
  }
  const h = await harness()
  h.formModel.email = '  new@example.test  '
  h.formModel.mobile = '  13900139000  '
  await h.component.submit()
  assert.equal(h.puts[0].data.email, 'new@example.test')
  assert.equal(h.puts[0].data.mobile, '13900139000')
})
test('existing contact clearing produces field error, zero requests, and allows correction', async () => {
  for (const field of ['mobile', 'email']) {
    for (const empty of ['', '   ', null, undefined]) {
      const h = await harness({ mobile: '13800138000', email: 'old@example.test' })
      h.formModel[field] = empty
      await h.component.submit()
      assert.equal(h.puts.length, 0)
      assert.equal(h.successes.length, 0)
      assert.ok(h.errors.some(error => error.field === field && error.message.includes('不支持清空')))
      h.formModel[field] = field === 'mobile' ? '13900139000' : 'new@example.test'
      await h.component.submit()
      assert.equal(h.puts.length, 1)
    }
  }
})
test('real rules reject illegal new or preexisting nonempty contacts and required nickname', async () => {
  const invalid = { mobile: ['1380013800', '138001380000', '12800138000', '1380013800a', 'abcdefghijk'], email: ['bad', 'a@', `${'a'.repeat(40)}@example.test`] }
  for (const [field, values] of Object.entries(invalid)) {
    for (const value of values) {
      for (const oldValue of [false, true]) {
        const h = await harness(oldValue ? { [field]: value } : {})
        h.formModel[field] = value
        await h.component.submit()
        assert.equal(h.puts.length, 0)
        assert.ok(h.errors.some(error => error.field === field))
      }
    }
  }
  const h = await harness()
  h.formModel.nickname = ''
  await h.component.submit()
  assert.equal(h.puts.length, 0)
  assert.ok(h.errors.some(error => error.field === 'nickname'))
})
