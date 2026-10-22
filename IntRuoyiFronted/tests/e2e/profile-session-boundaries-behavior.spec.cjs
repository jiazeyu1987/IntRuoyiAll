// Controlled production Vue/Pinia/Axios execution; no browser, server or real credentials.
const assert = require('node:assert/strict')
const nodeTest = require('node:test')
const test = (name, fn) => nodeTest(name, { timeout: 5000 }, fn)
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const pinia = require('pinia')
const axios = require('axios')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const compile = text => ts.transpileModule(text, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
function evaluate(text, imports, globals = {}) {
  const module = { exports: {} }
  Function('require', 'module', 'exports', ...Object.keys(globals), compile(text))(id => {
    assert.ok(Object.hasOwn(imports, id), `unexpected production dependency ${id}`)
    return imports[id]
  }, module, module.exports, ...Object.values(globals))
  return module.exports
}
function script(file, globals, exports) {
  const source = parse(read(file)).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  return Function(...Object.keys(globals), compile(body) + `; return { ${exports} };`)(...Object.values(globals))
}
function harness() {
  const cache = new Map()
  const CACHE_KEY = Object.fromEntries(['USER', 'ROLE_ROUTERS', 'VisitTenantId', 'TenantId'].map(key => [key, key]))
  const wsCache = { get: key => cache.get(key), set: (key, value) => cache.set(key, value), delete: key => cache.delete(key) }
  const cacheModule = { CACHE_KEY, useCache: () => ({ wsCache }), deleteUserCache: () => ['USER', 'ROLE_ROUTERS', 'VisitTenantId'].forEach(key => cache.delete(key)) }
  const auth = evaluate(read('src/utils/auth.ts'), { '@/hooks/web/useCache': cacheModule, '@/utils/jsencrypt': { encrypt: x => x, decrypt: x => x } })
  const store = pinia.createPinia()
  pinia.setActivePinia(store)
  const userModule = evaluate(read('src/store/modules/user.ts'), { '@/store': { store }, pinia, '@/utils/auth': auth, '@/hooks/web/useCache': cacheModule, '@/api/login': { getInfo: async () => { throw new Error('unexpected getInfo') }, loginOut: async () => {} } })
  const user = userModule.useUserStore(store)
  const identity = { id: 42, nickname: 'before', avatar: '', username: 'fixture', deptId: 1 }
  user.user = { ...identity }
  user.permissions = new Set(['mes:pro-work-order:query'])
  user.roles = ['operator']
  user.isSetUser = true
  auth.setToken({ accessToken: 'old-access', refreshToken: 'old-refresh' })
  cache.set('USER', { user: { ...identity }, permissions: [...user.permissions], roles: user.roles })
  cache.set('ROLE_ROUTERS', [{ name: 'ExistingRoute' }])
  auth.setTenantId(1)
  auth.setVisitTenantId(2)
  let refreshes = 0
  let refreshBehavior = async () => ({ data: { code: 0, data: { accessToken: 'renewed-access', refreshToken: 'renewed-refresh' } } })
  const controlledAxios = Object.assign({}, axios, { create: axios.create.bind(axios), post: async () => {
    refreshes++
    return refreshBehavior()
  } })
  const location = { href: 'https://controlled.invalid/login', pathname: '/login', search: '' }
  const transport = evaluate(read('src/config/axios/service.ts').replaceAll('import.meta.env', '__env'), {
    axios: controlledAxios, 'element-plus': { ElMessage: { error() {}, warning() {} }, ElNotification: { error() {} }, ElMessageBox: { confirm: async () => {} } },
    qs: require('qs'), '@/config/axios/config': { config: { result_code: 0, base_url: 'https://controlled.invalid', request_timeout: 100 } },
    '@/utils/auth': auth, './errorCode': { default: { default: 'error' } }, '@/hooks/web/useCache': cacheModule,
    '@/utils/encrypt': { ApiEncrypt: { getEncryptHeader: () => 'encrypted' } }
  }, { __env: { VITE_APP_TENANT_ENABLE: 'true' }, useI18n: () => ({ t: x => x }), window: { location } })
  return { cache, auth, user, userModule, store, cacheModule, service: transport.service, refreshes: () => refreshes,
    setRefreshBehavior: fn => { refreshBehavior = fn } }
}
const reply = (config, data) => ({ config, data, headers: {}, status: 200, statusText: 'OK' })
test('new login removes the preceding authenticated identity and visited tenant', () => {
  const h = harness()
  h.auth.setToken({ accessToken: 'new-login', refreshToken: 'new-login-refresh' })
  for (const key of ['USER', 'ROLE_ROUTERS', 'VisitTenantId']) assert.equal(h.cache.has(key), false)
})
test('BasicInfo save and read-back renew access while preserving real Pinia nickname cache and visited tenant', async () => {
  for (const expiredMethod of ['put', 'get']) {
    const h = harness(), requests = [], events = []
    let expired = false
    h.service.defaults.adapter = async config => {
      requests.push({ method: config.method, tenant: config.headers['visit-tenant-id'] })
      if (config.method === expiredMethod && !expired) { expired = true; return reply(config, { code: 401 }) }
      return reply(config, { code: 0, data: config.method === 'get' ? { nickname: 'saved', sex: 1, avatar: '' } : true })
    }
    const component = script('src/views/Profile/components/BasicInfo.vue', {
      ...vue, onMounted() {}, defineOptions() {}, defineEmits: () => value => events.push(value), useI18n: () => ({ t: x => x }),
      useMessage: () => ({ success() {} }), useUserStore: () => h.user,
      updateUserProfile: async data => h.service.put('/system/user/profile/update', data),
      getUserProfile: async () => (await h.service.get('/system/user/profile/get')).data
    }, 'formRef, submit')
    const model = vue.reactive({ nickname: 'saved', sex: 1, avatar: '' })
    component.formRef.value = { formModel: model, setValues: values => Object.assign(model, values), getElFormRef: () => ({ validate: async fn => fn(true) }) }
    await component.submit()
    assert.equal(h.refreshes(), 1)
    assert.equal(h.user.getUser.nickname, 'saved')
    assert.equal(h.cache.get('USER').user.nickname, 'saved')
    assert.ok(h.cache.has('ROLE_ROUTERS'))
    assert.equal(h.auth.getVisitTenantId(), 2)
    assert.ok(requests.every(request => Number(request.tenant) === 2))
    assert.deepEqual(events, ['success'])
  }
})
test('anonymous login-page 401 rejects and leaves later authenticated refresh available', async () => {
  const h = harness()
  h.auth.removeToken()
  h.service.defaults.adapter = async config => reply(config, { code: 401 })
  await assert.rejects(h.service.get('/protected'))
  h.auth.setToken({ accessToken: 'next-login', refreshToken: 'next-refresh' })
  h.service.defaults.adapter = async config => reply(config, config.headers.Authorization === 'Bearer renewed-access' ? { code: 0, data: 'ok' } : { code: 401 })
  const result = await h.service.get('/protected')
  assert.equal(result.data, 'ok')
  assert.equal(h.refreshes(), 1)
})
test('parallel expired requests share successful renewal and settle with preserved session headers', async () => {
  const h = harness(), headers = []
  let release
  h.setRefreshBehavior(() => new Promise(resolve => { release = resolve }))
  h.service.defaults.adapter = async config => {
    headers.push(Number(config.headers['visit-tenant-id']))
    return reply(config, config.headers.Authorization === 'Bearer renewed-access' ? { code: 0, data: config.url } : { code: 401 })
  }
  const pending = Promise.all([h.service.get('/first'), h.service.get('/second')])
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(h.refreshes(), 1)
  release({ data: { code: 0, data: { accessToken: 'renewed-access', refreshToken: 'renewed-refresh' } } })
  const results = await pending
  assert.deepEqual(results.map(result => result.data), ['/first', '/second'])
  assert.equal(h.refreshes(), 1)
  assert.ok(headers.every(tenant => tenant === 2))
  assert.ok(h.cache.has('USER'))
})
test('parallel refresh business failure rejects every request and later renewal can recover', async () => {
  const h = harness()
  let release
  h.setRefreshBehavior(() => h.refreshes() === 1 ? new Promise(resolve => { release = resolve }) : Promise.resolve({ data: { code: 401, msg: 'controlled refresh rejection' } }))
  h.service.defaults.adapter = async config => reply(config, { code: 401 })
  const pending = Promise.allSettled([h.service.get('/first'), h.service.get('/second')])
  await new Promise(resolve => setImmediate(resolve))
  release({ data: { code: 401, msg: 'controlled refresh rejection' } })
  const results = await pending
  assert.ok(results.every(result => result.status === 'rejected' && result.reason === 'sys.api.timeoutMessage'))
  h.auth.setToken({ accessToken: 'next-login', refreshToken: 'next-refresh' })
  h.setRefreshBehavior(async () => ({ data: { code: 0, data: { accessToken: 'renewed-access', refreshToken: 'renewed-refresh' } } }))
  h.service.defaults.adapter = async config => reply(config, config.headers.Authorization === 'Bearer renewed-access' ? { code: 0, data: 'recovered' } : { code: 401 })
  assert.equal((await h.service.get('/recovered')).data, 'recovered')
})
function workbench(h) {
  const permission = vue.reactive({ getRouters: [] }), requests = [], unmount = []
  const checkPermi = permissions => permissions.some(permission => h.user.getPermissions.has(permission))
  const badgeModule = evaluate(read('src/store/modules/profileWorkbenchTodoBadge.ts'), {
    pinia, vue, '../index': { store: h.store }, '@/api/system/profileWorkbenchTodo': { getProfileWorkbenchTodoCount: async () => { throw new Error('unexpected separate badge request') } },
    '@/store/modules/permission': { usePermissionStoreWithOut: () => permission }, '@/store/modules/user': h.userModule,
    '@/utils/auth': h.auth, '@/utils/permission': { checkPermi }
  })
  const badge = badgeModule.useProfileWorkbenchTodoBadgeStore(h.store)
  const scope = vue.effectScope()
  const component = scope.run(() => script('src/views/Profile/components/ProfileWorkbench.vue', {
    ...vue, ...h.auth, ...badgeModule, checkPermi, ...h.cacheModule,
    defineOptions() {}, useRouter: () => ({ push() {} }), useUserStore: () => h.user, usePermissionStore: () => permission,
    useUserTableColumns: () => ({}), useTableQuickFilter: () => ({}), onMounted() {}, onBeforeUnmount: fn => unmount.push(fn),
    getProfileWorkbenchTodoPage: query => new Promise((resolve, reject) => requests.push({ query, resolve, reject })),
    ElMessage: { error() {} }, ElMessageBox: { confirm: async () => {} }
  }, 'loadWorkbench, todoRows, total, loading, loadErrorMessages'))
  const page = { list: [{ taskKey: 'fixture' }], total: 1, hiddenTotal: 0, businessTotal: 1, effectivePageNo: 1 }
  return { component, badge, requests, page, dispose: () => { unmount.forEach(fn => fn()); scope.stop() } }
}
test('mounted workbench never reloads anonymously after clearSession and discards previous page/badge results', async () => {
  const h = harness(), w = workbench(h)
  const pending = w.component.loadWorkbench()
  assert.equal(w.requests.length, 1)
  h.user.clearSession()
  assert.equal(w.requests.length, 1, 'synchronous logout watchers must not send anonymous requests')
  w.requests[0].resolve(w.page)
  assert.equal(await pending, false)
  assert.equal(w.component.total.value, undefined)
  assert.equal(w.badge.loaded, false)
  assert.equal(await w.component.loadWorkbench(), false)
  w.dispose()
})
test('valid permission-scope changes reload; disposal blocks requests and stale result/error commits', async () => {
  const h = harness(), w = workbench(h)
  const initial = w.component.loadWorkbench()
  h.user.permissions = new Set(['dcc:controlled-file:query'])
  assert.equal(w.requests.length, 2)
  assert.deepEqual(w.requests[1].query.enabledSources, ['DCC_DISTRIBUTION'])
  w.requests[0].resolve(w.page)
  assert.equal(await initial, false)
  w.requests[1].resolve(w.page)
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(w.component.total.value, 1)
  const pending = w.component.loadWorkbench()
  w.dispose()
  const afterDispose = w.component.loadWorkbench()
  assert.equal(w.requests.length, 3)
  assert.equal(await afterDispose, false)
  w.requests[2].reject(new Error('stale failure'))
  assert.equal(await pending, false)
  assert.deepEqual(w.component.loadErrorMessages.value, [])
  assert.equal(w.badge.loaded, false)
})
test('workbench rejects missing identity and token removal prevents in-flight error badge commits', async () => {
  const h = harness(), w = workbench(h)
  const pending = w.component.loadWorkbench()
  h.auth.removeToken()
  w.requests[0].reject(new Error('late unauthorized response'))
  assert.equal(await pending, false)
  assert.deepEqual(w.component.loadErrorMessages.value, [])
  assert.equal(w.badge.error, '')
  assert.equal(await w.component.loadWorkbench(), false)
  h.user.resetState()
  h.auth.setToken({ accessToken: 'identity-not-loaded', refreshToken: 'identity-not-loaded-refresh' })
  assert.equal(await w.component.loadWorkbench(), false)
  assert.equal(w.requests.length, 1)
  w.dispose()
})
