// Component behavior verification; this script never opens a browser or contacts a server.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const { createRequire } = require('node:module')

const root = path.resolve(__dirname, '../../')
const elementRequire = createRequire(require.resolve('element-plus'))
const validatorModule = elementRequire('async-validator')
const Schema = validatorModule.default || validatorModule
const source = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const empty = () => ({ oldPassword: '', newPassword: '', confirmPassword: '' })
// Synthetic fixtures generated locally; no account credentials are used.
const strong = (suffix = '') => ['A', 'b', '1', '!', 'x'.repeat(5), suffix].join('')
const valid = () => ({ oldPassword: strong('old'), newPassword: strong('new'), confirmPassword: strong('new') })

function evaluate(text, imports, globals = {}) {
  const module = { exports: {} }
  const js = ts.transpileModule(text, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  vm.runInNewContext(js, {
    exports: module.exports, module, Error, Object, String,
    ...vue, ...globals,
    require: (name) => {
      if (Object.prototype.hasOwnProperty.call(imports, name)) return imports[name]
      throw new Error(`Unmapped production import: ${name}`)
    }
  })
  return module.exports
}

function sfc(relative, imports, globals) {
  const { descriptor, errors } = parse(source(relative), { filename: relative })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: relative })
  const template = compileTemplate({
    source: descriptor.template.content,
    filename: relative,
    id: relative,
    compilerOptions: { bindingMetadata: script.bindings }
  })
  assert.deepEqual(template.errors, [])
  const component = evaluate(script.content, { vue, ...imports }, globals).default
  component.render = evaluate(template.code, { vue }, globals).render
  return component
}

const propTypes = evaluate(source('src/utils/propTypes.ts'), {
  'vue-types': require('vue-types'), vue
}).propTypes
const policy = evaluate(source('src/views/system/user/systemPasswordPolicy.ts'), {})

function mount({ realTransport = false } = {}) {
  const calls = [], success = [], errors = [], globalErrors = [], sessionIo = [], navigations = []
  let navigationBehavior = async () => undefined
  let sessionClears = 0
  let sessionBehavior = () => undefined, clearBehavior = () => undefined
  let requestBehavior = async () => ({})
  let validationBehavior
  let validations = 0, clears = 0, resets = 0
  let model, rules
  const request = {
    put: async (option) => {
      calls.push(JSON.parse(JSON.stringify(option)))
      try { return await requestBehavior(option) } catch (error) {
        if (!option.ignoreErrorMessage) globalErrors.push(error)
        throw error
      }
    }
  }
  let productionRequest = request
  let productionService
  if (realTransport) {
    const axios = require('axios')
    const unexpected = () => { throw new Error('Authentication/encryption branch is outside this test scope') }
    const env = { VITE_APP_TENANT_ENABLE: 'false', VITE_BASE_URL: 'https://controlled-adapter.invalid', VITE_API_URL: '/test' }
    const config = evaluate(source('src/config/axios/config.ts').replaceAll('import.meta.env', '__testEnv'), {}, { __testEnv: env })
    const errorCode = evaluate(source('src/config/axios/errorCode.ts'), {})
    const notices = {
      ElMessage: { error: (value) => globalErrors.push(value), warning: (value) => globalErrors.push(value) },
      ElNotification: { error: (value) => globalErrors.push(value.title) },
      ElMessageBox: { confirm: unexpected }
    }
    // Only external/UI/auth dependencies are replaced. Axios creation, interceptor
    // registration/execution and both production wrappers execute their real code.
    productionService = evaluate(source('src/config/axios/service.ts').replaceAll('import.meta.env', '__testEnv'), {
      axios,
      'element-plus': notices,
      qs: require('qs'),
      '@/config/axios/config': config,
      '@/utils/auth': {
        getAccessToken: () => undefined, getTenantId: () => undefined,
        getVisitTenantId: () => undefined, getRefreshToken: unexpected,
        removeToken: unexpected, setToken: unexpected
      },
      './errorCode': errorCode,
      '@/hooks/web/useCache': { deleteUserCache: unexpected },
      '@/utils/encrypt': { ApiEncrypt: { getEncryptHeader: () => 'test-encrypted', encryptRequest: unexpected, decryptResponse: unexpected } }
    }, {
      __testEnv: env, useI18n: () => ({ t: (key) => key }),
      // The real interceptor has a debug log; don't print synthetic transport errors.
      console: { log: () => {}, error: unexpected }
    }).service
    productionService.defaults.adapter = async (option) => {
      calls.push({
        url: option.url, ignoreErrorMessage: option.ignoreErrorMessage,
        data: JSON.parse(option.data), method: option.method,
        contentType: option.headers.get('Content-Type')
      })
      const result = await requestBehavior(option)
      if (result?.networkFailure) throw new axios.AxiosError('Network Error', 'ERR_NETWORK', option)
      return {
        config: option, status: 200, statusText: 'OK', headers: {},
        data: result?.code === undefined ? { code: 200, data: result } : result
      }
    }
    productionRequest = evaluate(source('src/config/axios/index.ts'), {
      './service': { service: productionService }, './config': config
    }).default
  }
  const api = evaluate(source('src/api/system/user/profile.ts'), { '@/config/axios': { default: productionRequest, __esModule: true } })
  const InputPassword = sfc('src/components/InputPassword/src/InputPassword.vue', {
    '@/utils/propTypes': { propTypes },
    '@/hooks/web/useConfigGlobal': { useConfigGlobal: () => ({ configGlobal: vue.ref({ size: 'default' }) }) },
    '@/hooks/web/useDesign': { useDesign: () => ({ getPrefixCls: (name) => name }) },
    '@zxcvbn-ts/core': { zxcvbn: () => ({ score: 4 }) }
  })
  const XButton = sfc('src/components/XButton/src/XButton.vue', { '@/utils/propTypes': { propTypes } })
  const ResetPwd = sfc('src/views/Profile/components/ResetPwd.vue', {
    '@/components/InputPassword': { InputPassword },
    '@/api/system/user/profile': api,
    '@/views/system/user/systemPasswordPolicy': policy,
    'vue-router': { isNavigationFailure: (failure) => failure?.type === 4 },
    '@/store/modules/user': { useUserStore: () => ({ clearSession: () => { sessionClears++; sessionIo.push('clearSession'); sessionBehavior() } }) }
  }, { useRouter: () => ({ push: async (target) => { sessionIo.push('navigate'); navigations.push(JSON.parse(JSON.stringify(target))); return navigationBehavior(target) } }), useI18n: () => ({ t: (key) => key }), useMessage: () => ({ success: (text) => success.push(text), error: (text) => errors.push(text) }) })

  const validateRules = async () => {
    try { await new Schema(rules).validate({ ...model }); return true } catch (error) { throw error.fields }
  }
  const Form = vue.defineComponent({
    props: ['model', 'rules', 'labelWidth'],
    setup(props, { slots, expose }) {
      model = props.model
      rules = props.rules
      expose({
        validate: () => { validations++; return validationBehavior ? validationBehavior(validateRules) : validateRules() },
        clearValidate: () => { clears++; clearBehavior() },
        resetFields: () => { resets++; Object.assign(model, empty()); clears++ }
      })
      return () => vue.h('form', {}, slots.default?.())
    }
  })
  const FormItem = vue.defineComponent({ setup: (_, { slots }) => () => vue.h('section', {}, slots.default?.()) })
  const Input = vue.defineComponent({
    props: ['modelValue', 'disabled', 'type'], emits: ['update:modelValue'],
    setup: (props, { emit }) => () => vue.h('input', {
      value: props.modelValue, disabled: props.disabled,
      edit: (value) => { if (!props.disabled) emit('update:modelValue', value) }
    })
  })
  const Button = vue.defineComponent({
    props: ['disabled', 'loading'], emits: ['click'],
    setup: (props, { slots, emit }) => () => vue.h('button', {
      disabled: props.disabled, loading: props.loading,
      click: () => { if (!props.disabled) emit('click', {}) }
    }, slots.default?.())
  })
  const node = (type) => ({ type, props: {}, children: [], parent: null })
  const renderer = vue.createRenderer({
    createElement: node, createText: (text) => ({ ...node('text'), text }),
    createComment: (text) => ({ ...node('comment'), text }),
    setText: (item, text) => { item.text = text },
    setElementText: (item, text) => { item.text = text; item.children = [] },
    patchProp: (item, key, _, value) => { item.props[key] = value },
    insert: (item, parent, anchor = null) => {
      if (item.parent) item.parent.children.splice(item.parent.children.indexOf(item), 1)
      item.parent = parent
      const index = anchor ? parent.children.indexOf(anchor) : -1
      if (index < 0) parent.children.push(item); else parent.children.splice(index, 0, item)
    },
    remove: (item) => { if (item.parent) item.parent.children.splice(item.parent.children.indexOf(item), 1) },
    parentNode: (item) => item.parent,
    nextSibling: (item) => item.parent?.children[item.parent.children.indexOf(item) + 1] || null
  })
  const app = renderer.createApp(ResetPwd)
  for (const [name, component] of Object.entries({ ElForm: Form, ElFormItem: FormItem, ElInput: Input, ElButton: Button, XButton, Icon: { render: () => vue.h('icon') } })) app.component(name, component)
  const container = node('root')
  app.mount(container)
  const state = app._instance.setupState
  const find = (type, item = container) => (item.type === type ? [item] : []).concat(item.children.flatMap((child) => find(type, child)))
  const controls = async (busy) => {
    await vue.nextTick()
    const inputs = find('input'), buttons = find('button')
    assert.equal(inputs.length, 3)
    assert.equal(buttons.length, 2)
    assert.ok(inputs.every((input) => input.props.disabled === busy))
    assert.ok(buttons.every((button) => button.props.disabled === busy))
    assert.equal(buttons[0].props.loading, busy)
    assert.equal(state.submitting, busy)
  }
  return {
    state, api, calls, success, errors, globalErrors, controls, find, sessionIo, navigations,
    sessionClears: () => sessionClears,
    session: (behavior) => { sessionBehavior = behavior },
    clear: (behavior) => { clearBehavior = behavior },
    navigation: (behavior) => { navigationBehavior = behavior },
    fill: (values = valid()) => Object.assign(model, values),
    snapshot: () => ({ ...model }),
    submit: () => state.submit(state.formRef), reset: () => state.reset(state.formRef),
    validation: (behavior) => { validationBehavior = behavior },
    request: (behavior) => { requestBehavior = behavior },
    counts: () => ({ validations, clears, resets }),
    stop: () => app.unmount()
  }
}

async function run(name, test, options) {
  const page = mount(options)
  try { await test(page); console.log(`PASS ${name}`) } finally { page.stop() }
}

async function main() {
  await run('AC2 missing form is a no-op; validation false releases lock with zero request', async (p) => {
    p.fill(); await p.state.submit(undefined)
    assert.equal(p.counts().validations, 0)
    p.validation(() => Promise.resolve(false)); await p.submit()
    assert.equal(p.calls.length, 0); assert.deepEqual(p.snapshot(), valid())
    await p.controls(false)
    p.validation(() => { throw new Error('synchronous validator failure') })
    await assert.rejects(p.submit(), /密码表单校验程序异常/); assert.deepEqual(p.errors, [])
    await p.controls(false)
    p.validation(undefined); await p.submit(); assert.equal(p.calls.length, 1)
  })
  await run('AC1 lock begins before deferred validation; real input/button forwarding', async (p) => {
    p.fill()
    const gate = deferred(), request = deferred()
    p.validation(async (validate) => { const accepted = await validate(); await gate.promise; return accepted })
    p.request(() => request.promise)
    const submitted = p.submit()
    assert.equal(p.state.submitting, true)
    await p.submit(); p.reset()
    await p.controls(true)
    p.find('input')[0].props.edit('blocked edit')
    p.find('button')[1].props.click()
    assert.deepEqual(p.snapshot(), valid())
    assert.equal(p.counts().validations, 1)
    assert.equal(p.counts().resets, 0)
    assert.equal(p.calls.length, 0)
    gate.resolve()
    await new Promise((resolve) => setImmediate(resolve))
    await p.submit(); p.reset()
    await p.controls(true)
    assert.equal(p.calls.length, 1)
    assert.deepEqual(p.calls[0].data, { oldPassword: valid().oldPassword, newPassword: valid().newPassword })
    p.fill({ newPassword: strong('later') })
    assert.equal(p.calls[0].data.newPassword, valid().newPassword)
    request.resolve()
    await submitted
    await p.controls(false)
  })
  await run('AC1 changed model during deferred validation sends no unverified value', async (p) => {
    p.fill()
    const gate = deferred()
    p.validation(async (validate) => { await validate(); await gate.promise; return true })
    const submitted = p.submit()
    p.fill({ newPassword: strong('changed'), confirmPassword: strong('changed') })
    gate.resolve(); await submitted
    assert.equal(p.calls.length, 0)
    assert.equal(p.errors.length, 1)
    assert.match(p.errors[0], /重新提交/)
    await p.controls(false)
    p.validation(undefined)
    await p.submit()
    assert.equal(p.calls[0].data.newPassword, strong('changed'))
  })
  for (const [name, values] of [
    ['required', empty()],
    ['weak password', { ...valid(), newPassword: 'a'.repeat(8), confirmPassword: 'a'.repeat(8) }],
    ['confirmation mismatch', { ...valid(), confirmPassword: strong('other') }]
  ]) await run(`AC2 actual rules reject ${name} with zero request and retain fields`, async (p) => {
    p.fill(values); await p.submit()
    assert.equal(p.calls.length, 0)
    assert.deepEqual(p.snapshot(), values)
    assert.equal(p.success.length, 0)
    assert.equal(p.errors.length, 0)
    await p.controls(false)
    p.fill(); await p.submit()
    assert.equal(p.calls.length, 1)
  })
  for (const failure of [new Error('validation program error'), { oldPassword: [] }, { otherField: [{ field: 'otherField', message: 'unexpected' }] }]) {
    await run('AC2 validation program failure propagates sanitized evidence and retry unlocks', async (p) => {
      p.fill(); p.validation(() => Promise.reject(failure)); await assert.rejects(p.submit(), (error) => error.message === '密码表单校验程序异常，请联系管理员')
      assert.equal(p.calls.length, 0); assert.equal(p.errors.length, 0)
      assert.equal(p.success.length, 0); assert.deepEqual(p.snapshot(), valid())
      await p.controls(false)
      p.validation(undefined); await p.submit(); assert.equal(p.calls.length, 1)
    })
  }
  for (const failure of [new Error('old password rejected'), new Error('password history rejected'), new Error('Network Error'), 'business failure text']) {
    await run('AC2 business/network rejection displays once, preserves fields and retries', async (p) => {
      p.fill(); p.request(() => Promise.reject(failure)); await p.submit()
      assert.equal(p.calls.length, 1); assert.equal(p.calls[0].ignoreErrorMessage, true)
      assert.equal(p.sessionClears(), 0); assert.deepEqual(p.navigations, [])
      assert.deepEqual(p.errors, ['密码修改失败，请检查输入后重试'])
      assert.equal(p.globalErrors.length, 0); assert.equal(p.success.length, 0)
      assert.deepEqual(p.snapshot(), valid()); await p.controls(false)
      p.fill({ oldPassword: strong('corrected') }); p.request(async () => ({}))
      await p.submit(); assert.equal(p.calls.length, 2); assert.equal(p.success.length, 1)
    })
  }
  await run('AC3 success explicitly clears all fields/validation; empty retry is rejected; normal reset', async (p) => {
    p.fill(); await p.submit()
    assert.deepEqual(p.snapshot(), empty()); assert.equal(p.counts().clears, 1)
    assert.equal(p.counts().resets, 0); assert.equal(p.success.length, 1)
    assert.equal(p.sessionClears(), 1); assert.deepEqual(p.sessionIo, ['clearSession', 'navigate'])
    assert.deepEqual(p.navigations, [{ path: '/login' }])
    await p.controls(false); await p.submit(); assert.equal(p.calls.length, 1)
    p.fill(); p.reset()
    assert.deepEqual(p.snapshot(), empty()); assert.equal(p.counts().resets, 1)
    assert.equal(p.counts().clears, 2)
  })
  await run('upstream two-argument wrapper suppresses global errors and propagates rejection', async (p) => {
    await p.api.updateUserPassword('synthetic-old', 'synthetic-new')
    assert.equal(p.calls[0].url, '/system/user/profile/update-password')
    assert.equal(p.calls[0].ignoreErrorMessage, true)
    assert.deepEqual(p.calls[0].data, { oldPassword: 'synthetic-old', newPassword: 'synthetic-new' })
    p.request(() => Promise.reject(new Error('default caller failure')))
    await assert.rejects(p.api.updateUserPassword('synthetic-old', 'synthetic-new'), /default caller failure/)
    assert.equal(p.globalErrors.length, 0)
  })
  for (const [name, response, expectedLocal] of [
    ['old password business error', { code: 1002003001, msg: 'old password rejected' }, 'old password rejected'],
    ['history business error', { code: 1002003002, msg: 'password history rejected' }, 'password history rejected'],
    ['ordinary 500 response', { code: 500, msg: 'password update rejected' }, 'password update rejected'],
    ['network adapter rejection', { networkFailure: true }, 'Network Error']
  ]) await run(`AC2 real axios wrapper/interceptors: ${name}, generic local once and default global suppressed`, async (p) => {
    p.fill(); p.request(async () => response)
    await p.submit()
    assert.equal(p.calls.length, 1)
    assert.equal(p.calls[0].url, '/system/user/profile/update-password')
    assert.equal(p.calls[0].method, 'put')
    assert.equal(p.calls[0].contentType, 'application/json')
    assert.equal(p.calls[0].ignoreErrorMessage, true)
    assert.deepEqual(p.calls[0].data, { oldPassword: valid().oldPassword, newPassword: valid().newPassword })
    assert.deepEqual(p.errors, ['密码修改失败，请检查输入后重试']); assert.deepEqual(p.globalErrors, [])
    assert.equal(p.success.length, 0); assert.deepEqual(p.snapshot(), valid())
    await p.controls(false)
    // Upstream wrapper suppresses interceptor notification but still propagates failure.
    await assert.rejects(p.api.updateUserPassword(valid().oldPassword, valid().newPassword), (error) => error.message === expectedLocal)
    assert.equal(p.calls[1].ignoreErrorMessage, true)
    assert.deepEqual(p.globalErrors, [])
    assert.deepEqual(p.errors, ['密码修改失败，请检查输入后重试'])
    p.request(async () => ({ code: 200, data: true })); await p.submit()
    assert.deepEqual(p.snapshot(), empty()); assert.equal(p.success.length, 1)
    assert.deepEqual(p.globalErrors, []); await p.controls(false)
  }, { realTransport: true })
  for (const failure of ['reject', 'resolved']) await run(`AC3 committed password change survives ${failure} navigation failure without repeated write`, async (p) => {
    p.fill()
    p.navigation(async () => {
      assert.deepEqual(p.snapshot(), empty())
      assert.equal(p.sessionClears(), 1)
      if (failure === 'reject') throw new Error('synthetic navigation failure')
      return { type: 4 }
    })
    await p.submit()
    assert.equal(p.calls.length, 1); assert.equal(p.success.length, 1)
    assert.deepEqual(p.snapshot(), empty()); assert.equal(p.counts().clears, 1)
    assert.deepEqual(p.sessionIo, ['clearSession', 'navigate'])
    assert.deepEqual(p.errors, ['密码已修改，请重新登录'])
    await p.controls(false); await p.submit(); assert.equal(p.calls.length, 1)
  })
  await run('AC2 sensitive request error displays only fixed generic text and preserves session', async (p) => {
    const input = valid(); p.fill(input)
    p.request(() => Promise.reject(new Error(`request oldPassword=${input.oldPassword} newPassword=${input.newPassword}`)))
    await p.submit()
    assert.deepEqual(p.errors, ['密码修改失败，请检查输入后重试'])
    assert.equal(p.globalErrors.length, 0); assert.equal(p.sessionClears(), 0)
    assert.deepEqual(p.navigations, []); assert.deepEqual(p.snapshot(), input)
    assert.ok(p.errors.every((text) => !text.includes(input.oldPassword) && !text.includes(input.newPassword)))
  })

  for (const stage of ['session', 'validation']) await run(`AC3 committed change reports ${stage} cleanup failure accurately and unlocks`, async (p) => {
    p.fill()
    const fail = () => { throw new Error(`synthetic cleanup failure containing ${valid().newPassword}`) }
    if (stage === 'session') p.session(fail); else p.clear(fail)
    await p.submit()
    assert.equal(p.calls.length, 1); assert.deepEqual(p.snapshot(), empty())
    assert.equal(p.sessionClears(), 1); assert.deepEqual(p.navigations, [])
    assert.deepEqual(p.errors, [stage === 'session' ? '密码已修改，但本地会话清理失败，请重新登录' : '密码已修改，但表单校验状态清理失败，请重新登录'])
    assert.equal(p.success.length, 0); assert.equal(p.globalErrors.length, 0)
    assert.ok(p.errors.every((text) => !text.includes(valid().newPassword) && !text.includes('修改失败')))
    await p.controls(false); await p.submit(); assert.equal(p.calls.length, 1)
  })

}
main().catch((error) => { console.error(error); process.exitCode = 1 })
