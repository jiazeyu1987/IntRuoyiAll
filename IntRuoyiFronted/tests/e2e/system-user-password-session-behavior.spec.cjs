const assert = require('node:assert/strict')
const { readFileSync, existsSync, statSync } = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const pinia = require('pinia')
const vueRouter = require('vue-router')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const strongPassword = ['Fixture', '9', '!a'].join('')
const sensitiveError = () => new Error(`request password=${strongPassword}`)

// Execute the complete production guard against a real memory router. Only
// browser/auth/bootstrap I/O is replaced; guard control flow is unchanged.
async function permissionHarness(initialUserInfo = false) {
  let authenticated = false
  let bootstrapCalls = 0
  const component = { render: () => null }
  const router = vueRouter.createRouter({
    history: vueRouter.createMemoryHistory(),
    routes: ['/login', '/', '/user/profile', '/sso/authorize', '/fixture/original', '/fixture/requested']
      .map((path) => ({ path, component }))
  })
  const userStore = {
    getIsSetUser: initialUserInfo,
    async setUserInfoAction() { bootstrapCalls++; this.getIsSetUser = true }
  }
  const permissionStore = { generateRoutes: async () => {}, getAddRouters: [] }
  const dependencies = {
    './router': { __esModule: true, default: router },
    '@/config/axios/service': { isRelogin: { show: false } },
    '@/utils/auth': { getAccessToken: () => authenticated ? 'fixture-token' : undefined },
    '@/hooks/web/useTitle': { useTitle() {} },
    '@/hooks/web/useNProgress': { useNProgress: () => ({ start() {}, done() {} }) },
    '@/hooks/web/usePageLoading': { usePageLoading: () => ({ loadStart() {}, loadDone() {} }) },
    '@/store/modules/dict': { useDictStoreWithOut: () => ({ getIsSetDict: true }) },
    '@/store/modules/user': { useUserStoreWithOut: () => userStore },
    '@/store/modules/permission': { usePermissionStoreWithOut: () => permissionStore }
  }
  const compiled = ts.transpileModule(readFileSync(path.join(root, 'src/permission.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
    reportDiagnostics: true
  })
  assert.deepEqual(compiled.diagnostics, [])
  const module = { exports: {} }
  new Function('require', 'module', 'exports', compiled.outputText)((name) => {
    assert.ok(Object.hasOwn(dependencies, name), `Unlisted guard dependency: ${name}`)
    return dependencies[name]
  }, module, module.exports)
  return {
    router,
    get bootstrapCalls() { return bootstrapCalls },
    async enterLogin(redirect) {
      await router.push({ path: '/login', query: { redirect } })
      authenticated = true
    }
  }
}

// Offline production-function contract, not browser/E2E evidence. Full SFC
// setup is compiled and executed; only rendering and external I/O are replaced.
function harness(options = {}) {
  const requests = []
  const messages = []
  const prompts = []
  const navigations = []
  const lifecycle = []
  const io = []
  const apps = []
  const initialRedirect = options.redirect || '/sso/authorize'
  const initialHref = `https://fixture.invalid/login?redirect=${initialRedirect}&type=1`
  const route = { query: { redirect: initialRedirect } }
  const currentRoute = vue.ref(route)
  const windowFixture = { location: { href: initialHref } }
  const cache = new Map()
  const storeRoot = pinia.createPinia()
  pinia.setActivePinia(storeRoot)
  let pageFailure = false
  let promptResult = { value: strongPassword }
  let passwordFailure
  let passwordPending
  let navigationFailure = false
  let formValid = true
  // Produce a genuine resolved NavigationFailure through Vue Router's guard,
  // and use its actual classifier in production imports; no invented marker.
  const failureRouter = vueRouter.createRouter({
    history: vueRouter.createMemoryHistory(),
    routes: [{ path: '/login', component: { render: () => null } }]
  })
  failureRouter.beforeEach(() => false)
  const messageIo = Object.fromEntries(['success', 'warning', 'error', 'info'].map((kind) => [
    kind, (content) => messages.push({ kind, content })
  ]))
  const router = {
    currentRoute,
    push: async (target) => {
      navigations.push(target)
      io.push('navigate')
      if (navigationFailure === 'resolve') {
        const failure = await failureRouter.push('/login')
        assert.equal(vueRouter.isNavigationFailure(failure, vueRouter.NavigationFailureType.aborted), true)
        return failure
      }
      if (navigationFailure) throw sensitiveError()
    }
  }
  const authIo = {
    getAccessToken: () => 'fixture-token',
    removeToken: () => io.push('removeToken'),
    setToken: () => io.push('setToken'),
    setTenantId: () => {}, getTenantId: () => 1,
    getLoginForm: () => undefined, getLoginFormHistory: () => [],
    getLoginFormByTenantName: () => undefined,
    setLoginForm: () => {}, removeLoginForm: () => {}
  }
  const useLoginIo = {
    LoginStateEnum: { LOGIN: 0, MOBILE: 1, REGISTER: 2 },
    useFormValid: () => ({ validForm: async () => formValid }),
    useLoginTenant: () => ({ resolveTenantId: async () => 1 }),
    useLoginState: () => ({ getLoginState: vue.ref(0), setLoginState: () => {}, handleBackLogin: () => {} }),
    resolveLoginErrorMessage: () => '登录失败，请重新登录'
  }
  const service = async (request) => {
    requests.push(request)
    const url = request.url
    if (url.includes('update-password')) {
      if (passwordPending) await passwordPending
      if (passwordFailure) throw passwordFailure
      return { data: true }
    }
    if (url === '/system/user/page') {
      if (pageFailure) throw sensitiveError()
      return { data: { list: [], total: 0 } }
    }
    if (url === '/system/auth/logout') throw new Error('Password flow must not call HTTP logout')
    if (url.includes('/system/auth/')) return { data: { passwordChangeRequired: options.changeRequired !== false } }
    if (url.includes('get-by-website')) return { data: null }
    if (url.includes('get-id-by-name')) return { data: 1 }
    throw new Error(`Unlisted HTTP fixture: ${url}`)
  }
  const dependencies = {
    vue, pinia,
    'element-plus': {
      ElMessage: messageIo,
      ElNotification: messageIo,
      ElMessageBox: {
        prompt: async (...args) => {
          prompts.push(args)
          if (promptResult instanceof Error || typeof promptResult === 'string') throw promptResult
          return promptResult
        }
      },
      ElLoading: { service: () => ({ close() {} }) }
    },
    'vue-router': {
      useRoute: () => route, useRouter: () => router,
      isNavigationFailure: vueRouter.isNavigationFailure
    },
    '@/router': { __esModule: true, default: router },
    '@/store': { store: storeRoot },
    '@/utils/auth': authIo,
    '@/hooks/web/useCache': {
      CACHE_KEY: { USER: 'user', ROLE_ROUTERS: 'routers' },
      useCache: () => ({ wsCache: { get: () => undefined, set() {} } }),
      deleteUserCache: () => io.push('deleteUserCache')
    },
    '@/hooks/web/useI18n': { useI18n: () => ({ t: (key) => key }) },
    '@/config/axios/service': { service },
    '@/utils/dict': { DICT_TYPE: { COMMON_STATUS: 'status' }, getIntDictOptions: () => [] },
    '@/utils/permission': { checkPermi: () => true },
    '@/utils/formatTime': { dateFormatter: () => '' },
    '@/utils/constants': { CommonStatusEnum: { ENABLE: 0, DISABLE: 1 } },
    '@/hooks/web/useUserTableColumns': {
      useUserTableColumns: () => ({ columns: vue.ref([]), saving: vue.ref(false) })
    },
    '@/hooks/web/useTableQuickFilter': { useTableQuickFilter: () => ({}) },
    '@/api/system/dept': { getDeptList: async () => [] },
    '@/api/system/post': { getPostPage: async () => ({ list: [], total: 0 }) },
    '@/api/system/role': { getRolePage: async () => ({ list: [], total: 0 }) },
    '@/api/system/permission': { getUserRoleList: async () => [] },
    '@/hooks/web/useIcon': { useIcon: () => ({}) },
    '@/store/modules/permission': { usePermissionStore: () => ({ addRouters: [] }) },
    '@/hooks/web/useDesign': { useDesign: () => ({ getPrefixCls: () => 'fixture' }) },
    '@/store/modules/app': { useAppStore: () => ({ getLogo: '', getTitle: '' }) },
    '@/utils': { underlineToHump: (value) => value }
  }
  const child = { render: () => null }
  for (const name of [
    '@/components/UnifiedListTemplate/index.vue', '@/components/InputPassword',
    '@/views/system/user/UserForm.vue', '@/views/system/user/UserImportForm.vue',
    '@/views/system/user/UserDingTalkImportForm.vue', '@/views/system/user/UserAssignRoleForm.vue',
    '@/views/system/user/UserLifecycleDeactivateForm.vue', '@/views/system/dept/components/DeptTreeSelect.vue',
    '@/views/Login/components/LoginFormTitle.vue', '@/layout/components/LocaleDropdown'
  ]) dependencies[name] = { __esModule: true, default: child, InputPassword: child, LocaleDropdown: child }

  // Only import.meta.env is transformed: handlers/control flow are untouched.
  const fixtureEnv = {
    VITE_BASE_URL: 'https://fixture.invalid', VITE_API_URL: '/admin-api',
    VITE_APP_TENANT_ENABLE: 'false', VITE_APP_CAPTCHA_ENABLE: 'false',
    VITE_APP_DEFAULT_LOGIN_TENANT: ''
  }
  function envTransformer(context) {
    const visit = (node) => {
      if (ts.isPropertyAccessExpression(node) && ts.isPropertyAccessExpression(node.expression) &&
          ts.isMetaProperty(node.expression.expression) && node.expression.name.text === 'env') {
        assert.ok(Object.hasOwn(fixtureEnv, node.name.text), `Unknown env ${node.name.text}`)
        return ts.factory.createStringLiteral(fixtureEnv[node.name.text])
      }
      return ts.visitEachChild(node, visit, context)
    }
    return (source) => ts.visitNode(source, visit)
  }
  function load(filename) {
    if (cache.has(filename)) return cache.get(filename).exports
    const module = { exports: {} }
    cache.set(filename, module)
    let code = readFileSync(filename, 'utf8')
    if (filename.endsWith('.vue')) {
      const parsed = parse(code, { filename })
      assert.deepEqual(parsed.errors, [])
      const script = compileScript(parsed.descriptor, { id: filename })
      const template = compileTemplate({
        source: parsed.descriptor.template.content, filename, id: filename,
        compilerOptions: { bindingMetadata: script.bindings }
      })
      assert.deepEqual(template.errors, [])
      code = script.content
    }
    const compiled = ts.transpileModule(code, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
      reportDiagnostics: true, transformers: { before: [envTransformer] }
    })
    assert.deepEqual(compiled.diagnostics, [])
    const localRequire = (name) => {
      if (Object.hasOwn(dependencies, name)) return dependencies[name]
      if (name === './useLogin' || name === './components/useLogin') return useLoginIo
      if (name === './LoginFormTitle.vue') return { __esModule: true, default: child }
      const base = name.startsWith('@/') ? path.join(root, 'src', name.slice(2))
        : name.startsWith('.') ? path.resolve(path.dirname(filename), name) : null
      if (!base) throw new Error(`Unlisted dependency: ${name}`)
      if (Object.hasOwn(dependencies, '@/'+ path.relative(path.join(root, 'src'), base).replaceAll('\\', '/'))) {
        return dependencies['@/'+ path.relative(path.join(root, 'src'), base).replaceAll('\\', '/')]
      }
      const candidates = [base, base + '.ts', path.join(base, 'index.ts')]
      const resolved = candidates.find((candidate) => existsSync(candidate) && statSync(candidate).isFile())
      assert.ok(resolved, `Missing source module: ${name}`)
      return load(resolved)
    }
    const autoImports = {
      ref: vue.ref, reactive: vue.reactive, computed: vue.computed, unref: vue.unref,
      watch: vue.watch, nextTick: vue.nextTick,
      onMounted: (callback) => vue.onMounted(() => { lifecycle.push(Promise.resolve().then(callback)) }),
      useRouter: () => router, useRoute: () => route,
      useI18n: dependencies['@/hooks/web/useI18n'].useI18n,
      useMessage: () => load(path.join(root, 'src/hooks/web/useMessage.ts')).useMessage(),
      required: { required: true }
    }
    const injected = filename.endsWith('.vue') ? autoImports : {}
    new Function('require', 'module', 'exports', 'window', 'location', ...Object.keys(injected), compiled.outputText)(
      localRequire, module, module.exports, windowFixture, windowFixture.location, ...Object.values(injected)
    )
    return module.exports
  }
  const userStore = load(path.join(root, 'src/store/modules/user.ts')).useUserStore(storeRoot)
  userStore.user = { id: 1, username: 'fixture', avatar: '', nickname: 'Fixture', deptId: 1 }
  userStore.roles = ['fixture-role']
  userStore.permissions = new Set(['fixture:permission'])
  userStore.isSetUser = true
  function mount(relative) {
    const component = { ...load(path.join(root, relative)).default, render: () => null }
    const renderer = vue.createRenderer({
      createElement: () => ({}), createText: () => ({}), createComment: () => ({}),
      insert() {}, remove() {}, setText() {}, setElementText() {}, parentNode: () => null,
      nextSibling: () => null, patchProp() {}
    })
    const app = renderer.createApp(component)
    app.use(storeRoot)
    app.mount({})
    apps.push(app)
    return app._instance.setupState
  }
  return {
    mount, load, requests, messages, prompts, navigations, io, userStore, windowFixture, initialHref,
    set prompt(value) { promptResult = value },
    set passwordFailure(value) { passwordFailure = value },
    set passwordPending(value) { passwordPending = value },
    set pageFailure(value) { pageFailure = value },
    set navigationFailure(value) { navigationFailure = value },
    set formValid(value) { formValid = value },
    async ready() {
      await vue.nextTick()
      await Promise.all(lifecycle)
      // SocialLogin starts tryLogin without returning its promise from onMounted.
      // Let that real asynchronous handler settle before starting the action under test.
      await new Promise((resolve) => setImmediate(resolve))
      requests.length = 0; io.length = 0
    },
    close() { apps.forEach((app) => app.unmount()); pinia.disposePinia(storeRoot); cache.clear() }
  }
}

const userPanel = 'src/views/system/user/index.vue'
const profilePanel = 'src/views/Profile/components/ResetPwd.vue'
const passwordRequests = (h) => h.requests.filter((request) => request.url.includes('update-password'))
const assertNoSecret = (h) => assert.ok(h.messages.every(({ content }) =>
  !content.includes(strongPassword) && !content.includes('OldFixture9!') && !content.includes('request password=')
))
const assertLocalClear = (h) => {
  assert.deepEqual(h.io, ['removeToken', 'deleteUserCache', 'navigate'])
  assert.equal(h.userStore.getUser.id, 0)
  assert.equal(h.userStore.getRoles.length, 0)
  assert.equal(h.userStore.getPermissions.size, 0)
  assert.equal(h.userStore.getIsSetUser, false)
}

for (const action of ['cancel', 'close']) test(`reset ${action} sends no request`, async () => {
  const h = harness(); const vm = h.mount(userPanel)
  try { await h.ready(); h.prompt = action; await vm.handleResetPwd({ id: 2, username: 'target' }); assert.equal(h.requests.length, 0); assert.deepEqual(h.messages, []); assert.deepEqual(h.io, []) }
  finally { h.close() }
})

for (const password of ['', 'weak']) test(`reset invalid input ${password || 'empty'} sends no request`, async () => {
  const h = harness(); const vm = h.mount(userPanel)
  try { await h.ready(); h.prompt = { value: password }; await vm.handleResetPwd({ id: 2, username: 'target' }); assert.equal(h.requests.length, 0); assert.equal(h.messages[0].kind, 'warning') }
  finally { h.close() }
})

test('reset executes actual API wrapper, hides input and secret, refreshes other user', async () => {
  const h = harness(); const vm = h.mount(userPanel)
  try {
    await h.ready(); await vm.handleResetPwd({ id: 2, username: 'target' })
    assert.equal(h.prompts[0][2].inputType, 'password')
    assert.equal(passwordRequests(h).length, 1)
    assert.equal(passwordRequests(h)[0].ignoreErrorMessage, true)
    assert.equal(passwordRequests(h)[0].method, 'PUT')
    assert.deepEqual(passwordRequests(h)[0].data, { id: 2, password: strongPassword })
    assert.equal(h.requests.filter((request) => request.url === '/system/user/page').length, 1)
    assert.equal(h.requests.find((request) => request.url === '/system/user/page').ignoreErrorMessage, undefined)
    assert.equal(h.messages.filter((message) => message.kind === 'success').length, 1)
    assertNoSecret(h); assert.deepEqual(h.io, []); assert.equal(h.userStore.getUser.id, 1)
  } finally { h.close() }
})

for (const failure of ['business', 'network', 'prompt']) test(`reset ${failure} error is visible once and sanitized`, async () => {
  const h = harness(); const vm = h.mount(userPanel)
  try {
    await h.ready()
    const error = sensitiveError(); if (failure === 'business') error.code = 400
    if (failure === 'prompt') h.prompt = error; else h.passwordFailure = error
    await vm.handleResetPwd({ id: 2, username: 'target' })
    assert.equal(h.messages.filter((message) => message.kind === 'error').length, 1)
    assert.equal(h.messages.filter((message) => message.kind === 'success').length, 0)
    assertNoSecret(h); assert.deepEqual(h.io, [])
  } finally { h.close() }
})

test('reset other user distinguishes committed success from refresh failure', async () => {
  const h = harness(); const vm = h.mount(userPanel)
  try {
    await h.ready(); h.pageFailure = true; await vm.handleResetPwd({ id: 2, username: 'target' })
    assert.equal(passwordRequests(h).length, 1)
    assert.deepEqual(h.messages.map((message) => message.kind), ['success', 'error'])
    assert.match(h.messages[1].content, /已重置.*刷新失败/)
    assertNoSecret(h); assert.deepEqual(h.io, [])
  } finally { h.close() }
})

for (const failNavigation of [false, 'reject', 'resolve']) test(`reset self clears real Pinia session before navigation ${failNavigation}`, async () => {
  const h = harness(); const vm = h.mount(userPanel)
  try {
    await h.ready(); h.navigationFailure = failNavigation; await vm.handleResetPwd({ id: 1, username: 'fixture' })
    assertLocalClear(h); assert.equal(h.requests.length, 1)
    assert.equal(h.navigations[0].path, '/login')
    if (failNavigation) {
      assert.equal(h.messages.filter((message) => message.kind === 'error').length, 1)
      assert.match(h.messages.at(-1).content, /重新登录/)
    }
    assertNoSecret(h)
  } finally { h.close() }
})
for (const failure of ['business', 'network']) test(`reset self ${failure} failure preserves session`, async () => {
  const h = harness(); const vm = h.mount(userPanel)
  try {
    await h.ready()
    const error = sensitiveError(); if (failure === 'business') error.code = 400
    h.passwordFailure = error
    await vm.handleResetPwd({ id: 1, username: 'fixture' })
    assert.equal(passwordRequests(h).length, 1)
    assert.deepEqual(h.io, []); assert.deepEqual(h.navigations, [])
    assert.equal(h.userStore.getUser.id, 1)
    assert.equal(h.userStore.getRoles.length, 1)
    assert.equal(h.userStore.getPermissions.size, 1)
    assert.equal(h.userStore.getIsSetUser, true)
    assert.equal(h.messages.filter((message) => message.kind === 'error').length, 1)
    assert.equal(h.messages.filter((message) => message.kind === 'success').length, 0)
    assertNoSecret(h)
  } finally { h.close() }
})

test('explicit local clear action does not call remote logout', async () => {
  const h = harness()
  try {
    assert.equal(typeof h.userStore.clearSession, 'function')
    h.userStore.clearSession()
    assert.deepEqual(h.io, ['removeToken', 'deleteUserCache'])
    assert.equal(h.userStore.getUser.id, 0); assert.equal(h.requests.length, 0)
  } finally { h.close() }
})
test('ordinary remote logout still preserves session when server logout fails', async () => {
  const h = harness()
  try {
    await assert.rejects(h.userStore.loginOut(), /must not call HTTP logout/)
    assert.equal(h.requests.length, 1)
    assert.deepEqual(h.io, [])
    assert.equal(h.userStore.getUser.id, 1)
  } finally { h.close() }
})

async function submitProfile(vm, valid) {
  const form = profileForm(valid)
  await vm.submit(form)
  await form.settled()
}
const deferred = () => {
  let resolve
  const promise = new Promise((done) => { resolve = done })
  return { promise, resolve }
}
// ElForm supports its production Promise validation contract.
function profileForm(valid = true, validationPending) {
  const completions = []
  return {
    calls: 0,
    validate() {
      this.calls++
      const completion = (async () => {
        if (validationPending) await validationPending
        return valid
      })()
      completions.push(completion)
      return completion
    },
    clearValidate() {},
    settled: () => Promise.all(completions)
  }
}
for (const stage of ['synchronous', 'validation', 'request']) test(`profile single submission blocks repeated handler during ${stage}`, async () => {
  const h = harness(); const vm = h.mount(profilePanel)
  const pending = deferred()
  const form = profileForm(true, stage === 'validation' ? pending.promise : undefined)
  let first, second
  try {
    await h.ready()
    if (stage === 'request') h.passwordPending = pending.promise
    first = vm.submit(form)
    if (stage === 'request') await new Promise((resolve) => setImmediate(resolve))
    second = vm.submit(form)
    let resetCalls = 0
    vm.reset({ resetFields() { resetCalls++ } })
    const pendingState = { validations: form.calls, requests: passwordRequests(h).length, submitting: vm.submitting }
    pending.resolve()
    await Promise.all([first, second, form.settled()])
    assert.equal(pendingState.validations, 1, 'One action must start only one validation')
    assert.equal(pendingState.requests, stage === 'request' ? 1 : 0)
    assert.equal(pendingState.submitting, true)
    assert.equal(resetCalls, 0, 'Pending submission must preserve its inputs')
    assert.equal(vm.submitting, false)
    vm.reset({ resetFields() { resetCalls++ } })
    assert.equal(resetCalls, 1)
    assert.equal(passwordRequests(h).length, 1)
    assert.equal(h.messages.filter(({ kind }) => kind === 'success').length, 1)
    assertLocalClear(h)
  } finally {
    pending.resolve()
    await Promise.allSettled([first, second, form.settled()])
    h.close()
  }
})
for (const failure of ['invalid', 'request']) test(`profile releases submission lock after ${failure} and permits retry`, async () => {
  const h = harness(); const vm = h.mount(profilePanel)
  try {
    await h.ready()
    if (failure === 'request') h.passwordFailure = sensitiveError()
    await submitProfile(vm, failure !== 'invalid')
    assert.equal(vm.submitting, false)
    assert.equal(passwordRequests(h).length, failure === 'invalid' ? 0 : 1)
    assert.deepEqual(h.io, []); assert.equal(h.userStore.getUser.id, 1)
    h.passwordFailure = undefined
    await submitProfile(vm, true)
    assert.equal(passwordRequests(h).length, failure === 'invalid' ? 1 : 2)
    assert.equal(vm.submitting, false)
    assertLocalClear(h); assertNoSecret(h)
  } finally { h.close() }
})
test('profile releases submission lock and propagates sanitized validation infrastructure exceptions', async () => {
  const h = harness(); const vm = h.mount(profilePanel)
  try {
    await h.ready()
    const error = new Error('Validation infrastructure failed')
    await assert.rejects(async () => vm.submit({ validate() { throw error } }), (actual) => actual.message === '密码表单校验程序异常，请联系管理员')
    assert.deepEqual(h.messages, [])
    assert.equal(vm.submitting, false)
    assert.equal(passwordRequests(h).length, 0); assert.deepEqual(h.io, [])
    await submitProfile(vm, true)
    assert.equal(passwordRequests(h).length, 1); assertLocalClear(h)
  } finally { h.close() }
})
test('profile shared password validator rejects weak input without submission', async () => {
  const h = harness(); const vm = h.mount(profilePanel)
  try {
    await h.ready(); vm.password.newPassword = 'weak'
    const policy = h.load(path.join(root, 'src/views/system/user/systemPasswordPolicy.ts'))
    const rule = vm.rules.newPassword.find((entry) => entry.validator === policy.systemPasswordRule.validator)
    assert.ok(rule, 'Profile must use the shared production password rule')
    let failure
    rule.validator({}, vm.password.newPassword, (error) => { failure = error })
    assert.ok(failure instanceof Error)
    await submitProfile(vm, !failure)
    assert.equal(h.requests.length, 0); assert.deepEqual(h.io, [])
  } finally { h.close() }
})
for (const scenario of ['invalid', 'business', 'network', 'success', 'navigation', 'resolvedNavigation']) test(`profile actual submit ${scenario}`, async () => {
  const h = harness(); const vm = h.mount(profilePanel)
  try {
    await h.ready()
    vm.password.oldPassword = 'OldFixture9!'; vm.password.newPassword = strongPassword; vm.password.confirmPassword = strongPassword
    const originalForm = { ...vm.password }
    if (scenario === 'business' || scenario === 'network') {
      const error = sensitiveError(); if (scenario === 'business') error.code = 400
      h.passwordFailure = error
    }
    h.navigationFailure = scenario === 'resolvedNavigation' ? 'resolve' : scenario === 'navigation'
    await submitProfile(vm, scenario !== 'invalid')
    if (scenario === 'success' || scenario === 'navigation' || scenario === 'resolvedNavigation') {
      assertLocalClear(h); assert.equal(passwordRequests(h)[0].ignoreErrorMessage, true)
      assert.deepEqual(passwordRequests(h)[0].data, { oldPassword: 'OldFixture9!', newPassword: strongPassword })
      assert.equal(h.navigations[0].path, '/login')
      if (scenario !== 'success') {
        assert.equal(h.messages.filter((message) => message.kind === 'error').length, 1)
        assert.match(h.messages.at(-1).content, /重新登录/)
      }
    } else {
      assert.deepEqual(h.io, []); assert.equal(h.userStore.getUser.id, 1)
      assert.deepEqual({ ...vm.password }, originalForm)
      assert.equal(h.messages.filter((message) => message.kind === 'success').length, 0)
      if (scenario === 'invalid') assert.equal(h.requests.length, 0)
      else assert.equal(h.messages.filter((message) => message.kind === 'error').length, 1)
    }
    assertNoSecret(h)
  } finally { h.close() }
})

for (const [component, handler] of [
  ['components/LoginForm.vue', 'handleLogin'], ['components/MobileForm.vue', 'signIn'],
  ['components/RegisterForm.vue', 'handleRegister'], ['SocialLogin.vue', 'handleLogin'], ['SocialLogin.vue', 'tryLogin']
]) for (const required of [true, false]) test(`${component}.${handler} required=${required} takes priority over SSO/redirect`, async () => {
  const h = harness({ changeRequired: required }); const vm = h.mount('src/views/Login/' + component)
  try {
    await h.ready(); h.navigations.length = 0
    await vm[handler]({ captchaVerification: '' })
    if (required) {
      assert.deepEqual(h.navigations.at(-1), { path: '/user/profile', query: { tab: 'resetPwd' } })
      assert.equal(h.windowFixture.location.href, h.initialHref)
    } else if (component === 'components/MobileForm.vue' || handler === 'tryLogin') {
      assert.equal(h.navigations.at(-1).path, '/sso/authorize')
    } else {
      assert.equal(h.navigations.length, 0)
      assert.notEqual(h.windowFixture.location.href, h.initialHref)
    }
  } finally { h.close() }
})

for (const redirect of ['/sso/authorize', '/fixture/original?keep=1']) test(`actual guard first bootstrap preserves forced password target over ${redirect}`, async () => {
  const h = await permissionHarness()
  await h.enterLogin(redirect)
  await h.router.push({ path: '/user/profile', query: { tab: 'resetPwd', source: 'fixture', value: 'a b' } })
  assert.equal(h.router.currentRoute.value.path, '/user/profile')
  assert.deepEqual(h.router.currentRoute.value.query, { tab: 'resetPwd', source: 'fixture', value: 'a b' })
  assert.equal(h.bootstrapCalls, 1)
})

for (const redirect of ['/sso/authorize', '/fixture/original?keep=1']) test(`actual guard first bootstrap retains ordinary redirect ${redirect}`, async () => {
  const h = await permissionHarness()
  await h.enterLogin(redirect)
  await h.router.push({ path: '/fixture/requested' })
  assert.equal(h.router.currentRoute.value.path, redirect.split('?')[0])
  assert.deepEqual(h.router.currentRoute.value.query, redirect.includes('?') ? { keep: '1' } : {})
  assert.equal(h.bootstrapCalls, 1)
})

for (const target of [
  { path: '/user/profile', query: { tab: 'resetPwd', source: 'fixture' } },
  { path: '/fixture/requested', query: { source: 'fixture' } }
]) test(`actual guard existing user info retains requested target ${target.path}`, async () => {
  const h = await permissionHarness(true)
  await h.enterLogin('/sso/authorize')
  await h.router.push(target)
  assert.equal(h.router.currentRoute.value.path, target.path)
  assert.deepEqual(h.router.currentRoute.value.query, target.query)
  assert.equal(h.bootstrapCalls, 0)
})

for (const [component, handler] of [
  ['components/LoginForm.vue', 'handleLogin'], ['components/MobileForm.vue', 'signIn'],
  ['components/RegisterForm.vue', 'handleRegister'], ['SocialLogin.vue', 'handleLogin'], ['SocialLogin.vue', 'tryLogin']
]) test(`${component}.${handler} preserves ordinary redirect when change is not required`, async () => {
  const h = harness({ changeRequired: false, redirect: '/fixture/landing' })
  const vm = h.mount('src/views/Login/' + component)
  try {
    await h.ready(); h.navigations.length = 0
    await vm[handler]({ captchaVerification: '' })
    assert.equal(h.navigations.at(-1).path, '/fixture/landing')
    assert.equal(h.windowFixture.location.href, h.initialHref)
  } finally { h.close() }
})
