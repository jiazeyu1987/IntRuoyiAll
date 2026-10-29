const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')

function loadAuth(crypto = { getRandomValues: webcrypto.getRandomValues.bind(webcrypto) }) {
  const values = new Map()
  const cache = { get: key => values.get(key), set: (key, value) => values.set(key, value), delete: key => values.delete(key) }
  const module = { exports: {} }
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/utils/auth.ts'), 'utf8')
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
  vm.runInNewContext(compiled, {
    module, exports: module.exports, crypto, Uint8Array,
    require: name => {
      if (name === '@/hooks/web/useCache') return { useCache: () => ({ wsCache: cache }), CACHE_KEY: { USER: 'USER', ROLE_ROUTERS: 'ROLE_ROUTERS', VisitTenantId: 'VISIT_TENANT_ID' } }
      if (name === '@/utils/jsencrypt') return { decrypt: value => value }
      if (name === 'vue') return { readonly: value => value, ref: value => ({ value }) }
      throw Error('Unexpected import: ' + name)
    }
  })
  return { auth: module.exports, values }
}

test('HTTP browser without randomUUID can log in, refresh and start a new session', () => {
  const { auth, values } = loadAuth()
  auth.setToken({ accessToken: 'first', refreshToken: 'refresh' })
  const initial = auth.getAuthSessionId()
  assert.match(initial, /^[a-f0-9]{32}$/)
  assert.equal(auth.getAuthSessionId(), initial)
  auth.setRefreshedToken({ accessToken: 'renewed', refreshToken: 'renewed-refresh' })
  assert.equal(auth.getAuthSessionId(), initial)
  assert.equal(auth.getAccessToken(), 'renewed')
  auth.setToken({ accessToken: 'second', refreshToken: 'second-refresh' })
  assert.notEqual(auth.getAuthSessionId(), initial)
  auth.removeToken()
  assert.equal(auth.getAuthSessionId(), undefined)
  assert.equal(values.has('AUTH_SESSION_ID'), false)
})

test('existing authenticated session can create a missing local identity over HTTP', () => {
  const { auth, values } = loadAuth()
  values.set('ACCESS_TOKEN', 'existing')
  assert.match(auth.getAuthSessionId(), /^[a-f0-9]{32}$/)
})

test('missing secure random generator fails before changing existing login state', () => {
  const { auth, values } = loadAuth({})
  values.set('ACCESS_TOKEN', 'existing')
  values.set('USER', 'existing-user')
  assert.throws(() => auth.setToken({ accessToken: 'new', refreshToken: 'new-refresh' }), /getRandomValues/)
  assert.equal(values.get('ACCESS_TOKEN'), 'existing')
  assert.equal(values.get('USER'), 'existing-user')
})
