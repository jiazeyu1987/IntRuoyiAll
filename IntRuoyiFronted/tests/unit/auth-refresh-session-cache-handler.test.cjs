const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const axios = require('axios')

const root = path.resolve(__dirname, '../..')
const oldTokens = { accessToken: 'unit-old-access', refreshToken: 'unit-old-refresh' }
const newTokens = { accessToken: 'unit-new-access', refreshToken: 'unit-new-refresh' }
const flush = () => new Promise((resolve) => setImmediate(resolve))
const deferred = () => {
  let resolve
  const promise = new Promise((done) => { resolve = done })
  return { promise, resolve }
}

// Compile complete production modules, without extracting/reimplementing the refresh handler.
// Only Vite's import.meta.env is supplied by the isolated module host.
const provideViteEnv = (context) => {
  const visit = (node) => {
    if (ts.isPropertyAccessExpression(node) && node.name.text === 'env'
        && ts.isMetaProperty(node.expression)
        && node.expression.keywordToken === ts.SyntaxKind.ImportKeyword) {
      return ts.factory.createIdentifier('__viteEnv')
    }
    return ts.visitEachChild(node, visit, context)
  }
  return (source) => ts.visitNode(source, visit)
}

function session({ visitTenant = true, authenticatedCache = true, refreshPayload, holdRefresh = false } = {}) {
  const values = new Map()
  const mutations = []
  const businessCalls = []
  const refreshCalls = []
  const prompts = []
  const refreshStarted = deferred()
  const refreshGate = deferred()
  const receivedByUrl = new Map()

  // Explicit browser storage port shared by the actual useCache/auth modules.
  class MemoryStorageCache {
    get(key) { return values.get(key) }
    set(key, value) { mutations.push({ operation: 'set', key, value }); values.set(key, value) }
    delete(key) { mutations.push({ operation: 'delete', key }); values.delete(key) }
  }

  const response = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} })
  const transport = axios.create({
    adapter: async (config) => {
      assert.equal(config.method, 'post')
      assert.equal(config.url, '/admin-api/system/auth/refresh-token?refreshToken=unit-old-refresh')
      refreshCalls.push({ tenant: config.headers.get('tenant-id') })
      refreshStarted.resolve()
      if (holdRefresh) await refreshGate.promise
      return response(config, refreshPayload === undefined ? { code: 0, data: newTokens } : refreshPayload)
    }
  })
  // Both clients are real Axios instances. Their only network boundary is a private adapter.
  transport.create = (config) => axios.create({
    ...config,
    adapter: async (request) => {
      assert.ok(request.url.startsWith('/session-fixture/'), 'unexpected business endpoint')
      const seen = receivedByUrl.get(request.url) || 0
      receivedByUrl.set(request.url, seen + 1)
      businessCalls.push({
        url: request.url,
        authorization: request.headers.get('Authorization'),
        tenant: request.headers.get('tenant-id'),
        visit: request.headers.get('visit-tenant-id')
      })
      return response(request, seen === 0
        ? { code: 401, msg: 'unit expired access token' }
        : { code: 0, data: { url: request.url } })
    }
  })

  const location = {
    href: 'https://session-fixture.invalid/dcc/controlled-file/view?id=186',
    pathname: '/dcc/controlled-file/view',
    search: '?id=186'
  }
  const notice = () => { throw new Error('unexpected toast/notification in refresh branch') }
  const modules = new Map()
  const sourceFiles = {
    '@/hooks/web/useCache': 'src/hooks/web/useCache.ts',
    '@/utils/auth': 'src/utils/auth.ts',
    '@/config/axios/config': 'src/config/axios/config.ts',
    './errorCode': 'src/config/axios/errorCode.ts'
  }
  function load(relativePath) {
    if (modules.has(relativePath)) return modules.get(relativePath).exports
    const module = { exports: {} }
    modules.set(relativePath, module)
    const output = ts.transpileModule(fs.readFileSync(path.join(root, relativePath), 'utf8'), {
      fileName: relativePath,
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
        esModuleInterop: true
      },
      transformers: { before: [provideViteEnv] }
    }).outputText
    vm.runInNewContext(output, {
      exports: module.exports,
      module,
      require: (id) => {
        if (sourceFiles[id]) return load(sourceFiles[id])
        if (id === 'axios') return transport
        if (id === 'qs') return require('qs')
        if (id === 'vue') return require('vue')
        if (id === 'web-storage-cache') return MemoryStorageCache
        if (id === '@/utils/jsencrypt') return { decrypt: () => { throw new Error('unexpected login-form decrypt') } }
        if (id === '@/utils/encrypt') return {
          ApiEncrypt: { getEncryptHeader: () => 'x-unit-encrypted' }
        }
        if (id === 'element-plus') return {
          ElMessage: { error: notice, warning: notice },
          ElNotification: { error: notice },
          ElMessageBox: {
            confirm: (...args) => {
              const decision = deferred()
              prompts.push({ args, decision })
              return decision.promise
            }
          }
        }
        throw new Error(`unexpected module dependency: ${id}`)
      },
      __viteEnv: { VITE_APP_TENANT_ENABLE: 'true', VITE_BASE_URL: '', VITE_API_URL: '/admin-api' },
      useI18n: () => ({ t: (key) => key }),
      window: { location },
      console,
      Response,
      crypto: require('node:crypto').webcrypto
    }, { filename: relativePath })
    return module.exports
  }

  const cacheApi = load('src/hooks/web/useCache.ts')
  const auth = load('src/utils/auth.ts')
  const cache = cacheApi.useCache().wsCache
  const keys = cacheApi.CACHE_KEY
  const protectedKeys = [keys.USER, keys.ROLE_ROUTERS, keys.VisitTenantId]
  const profile = {
    user: { id: '910412', username: 'unit-U' },
    roles: ['unit-author'],
    permissions: ['dcc:controlled-file:query']
  }
  const routes = [{ path: '/dcc', children: [{ path: 'controlled-file' }] }]
  cache.set('ACCESS_TOKEN', oldTokens.accessToken)
  cache.set('REFRESH_TOKEN', oldTokens.refreshToken)
  cache.set(keys.TenantId, '1')
  cache.set(keys.THEME, { isDark: false, marker: 'unit-theme' })
  cache.set(keys.LoginForm, { tenantName: 'unit-tenant', username: 'unit-U', rememberMe: false })
  if (authenticatedCache) {
    cache.set(keys.USER, profile)
    cache.set(keys.ROLE_ROUTERS, routes)
    if (visitTenant) cache.set(keys.VisitTenantId, '20000000000000017')
  }
  const before = new Map(values)
  mutations.length = 0
  const api = load('src/config/axios/service.ts')

  return {
    ...api, auth, cache, keys, protectedKeys, before, mutations, businessCalls, refreshCalls,
    prompts, location, refreshStarted, refreshGate,
    request: (name = 'protected') => api.service.get(`/session-fixture/${name}`)
  }
}

function assertPreservedIdentity(h) {
  for (const key of [h.keys.USER, h.keys.ROLE_ROUTERS, h.keys.VisitTenantId, h.keys.TenantId]) {
    assert.strictEqual(h.cache.get(key), h.before.get(key), `refresh must preserve exact cached ${key}`)
  }
  assert.deepEqual(h.mutations.filter((entry) => h.protectedKeys.includes(entry.key)), [],
    'same-session refresh must not clear and recreate authenticated identity caches')
}

function assertNewTokenPair(h) {
  assert.equal(h.auth.getAccessToken(), newTokens.accessToken)
  assert.equal(h.auth.getRefreshToken(), newTokens.refreshToken)
  assert.deepEqual(h.mutations.filter((entry) => entry.operation === 'set').map((entry) => entry.key).sort(),
    ['ACCESS_TOKEN', 'REFRESH_TOKEN'])
  assert.equal(h.refreshCalls.length, 1)
  assert.equal(h.refreshCalls[0].tenant, '1')
}

for (const code of [0, 200]) {
  for (const visitTenant of [false, true]) {
    test(`actual refresh handler code=${code} preserves USER/roles/routes and visit=${visitTenant}`, async () => {
      const h = session({ visitTenant, refreshPayload: { code, data: newTokens } })
      const result = await h.request()
      assert.equal(result.code, 0)
      assertNewTokenPair(h)
      assert.equal(h.businessCalls.length, 2)
      assert.equal(h.businessCalls[0].authorization, 'Bearer unit-old-access')
      assert.equal(h.businessCalls[1].authorization, 'Bearer unit-new-access')
      assert.equal(h.businessCalls[1].visit, visitTenant ? '20000000000000017' : undefined)
      assertPreservedIdentity(h)
      assert.equal(h.prompts.length, 0)
    })
  }
}

test('actual login setToken clears all three old identity caches before storing another account token', () => {
  const h = session()
  const loginPair = { accessToken: 'unit-other-access', refreshToken: 'unit-other-refresh', userId: 991 }
  h.auth.setToken(loginPair)
  const firstTokenWrite = h.mutations.findIndex((entry) => entry.operation === 'set')
  for (const key of h.protectedKeys) {
    assert.equal(h.cache.get(key), undefined)
    const deletionIndex = h.mutations.findIndex((entry) => entry.operation === 'delete' && entry.key === key)
    assert.ok(deletionIndex >= 0 && deletionIndex < firstTokenWrite)
  }
  assert.equal(h.auth.getAccessToken(), loginPair.accessToken)
  assert.equal(h.auth.getRefreshToken(), loginPair.refreshToken)
  for (const key of [h.keys.TenantId, h.keys.THEME, h.keys.LoginForm]) {
    assert.strictEqual(h.cache.get(key), h.before.get(key))
  }
  assert.equal(h.refreshCalls.length, 0)
})

test('two actual 401 handlers share one refresh and replay both requests without losing identity', { timeout: 10000 }, async () => {
  const h = session({ holdRefresh: true })
  const results = Promise.all([h.request('first'), h.request('second')])
  try {
    await h.refreshStarted.promise
    await flush()
    assert.equal(h.businessCalls.length, 2)
    assert.equal(h.refreshCalls.length, 1)
  } finally {
    h.refreshGate.resolve()
  }
  assert.deepEqual((await results).map((result) => result.data.url).sort(),
    ['/session-fixture/first', '/session-fixture/second'])
  assertNewTokenPair(h)
  assert.deepEqual(h.businessCalls.slice(2).map((call) => call.authorization),
    ['Bearer unit-new-access', 'Bearer unit-new-access'])
  assertPreservedIdentity(h)
})

for (const [name, payload] of [
  ['business failure even with token-shaped data', { code: 1002001, msg: 'unit refresh rejected', data: newTokens }],
  ['success missing refreshToken', { code: 0, data: { accessToken: newTokens.accessToken } }],
  ['success missing accessToken', { code: 200, data: { refreshToken: newTokens.refreshToken } }]
]) {
  test(`actual refresh ${name} writes no tokens and uses existing relogin confirmation`, async () => {
    const h = session({ refreshPayload: payload })
    await assert.rejects(h.request(), (reason) => reason === 'sys.api.timeoutMessage')
    assert.equal(h.refreshCalls.length, 1)
    assert.equal(h.businessCalls.length, 1, 'the failed current request must not be replayed')
    assert.deepEqual(h.mutations, [], 'rejected refresh must not persist token-shaped payload or clear identity early')
    assert.equal(h.auth.getAccessToken(), oldTokens.accessToken)
    assert.equal(h.auth.getRefreshToken(), oldTokens.refreshToken)
    assert.equal(h.isRelogin.show, true)
    assert.equal(h.prompts.length, 1)
    assertPreservedIdentity(h)
    h.prompts[0].decision.resolve()
    await flush()
    for (const key of [...h.protectedKeys, 'ACCESS_TOKEN', 'REFRESH_TOKEN']) assert.equal(h.cache.get(key), undefined)
    assert.equal(h.isRelogin.show, false)
    assert.strictEqual(h.cache.get(h.keys.TenantId), h.before.get(h.keys.TenantId))
    assert.equal(h.mutations.filter((entry) => entry.operation === 'set').length, 0)
  })
}

test('already active relogin still clears invalid session and redirects on refresh business failure', async () => {
  const h = session({ refreshPayload: { code: 1002001, msg: 'unit refresh rejected', data: newTokens } })
  h.isRelogin.show = true
  await assert.rejects(h.request(), (reason) => reason === 'sys.api.timeoutMessage')
  for (const key of [...h.protectedKeys, 'ACCESS_TOKEN', 'REFRESH_TOKEN']) assert.equal(h.cache.get(key), undefined)
  assert.equal(h.mutations.filter((entry) => entry.operation === 'set').length, 0)
  assert.equal(h.businessCalls.length, 1)
  assert.equal(h.prompts.length, 0)
  assert.equal(h.location.href, '/login?redirect=' + encodeURIComponent(h.location.pathname + h.location.search))
})

test('successful refresh never fabricates absent user identity or permissions', async () => {
  const h = session({ authenticatedCache: false })
  await h.request()
  assertNewTokenPair(h)
  for (const key of h.protectedKeys) assert.equal(h.cache.get(key), undefined)
})
