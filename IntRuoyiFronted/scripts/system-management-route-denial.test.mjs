import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'

const source = readFileSync(new URL('../src/permission.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

async function navigate(path, permissions, initialized = true) {
  let guard
  let nextValue
  const user = {
    getIsSetUser: initialized,
    getPermissions: new Set(permissions),
    setUserInfoAction: async () => { user.getIsSetUser = true }
  }
  const dependencies = {
    './router': { default: { beforeEach: fn => { guard = fn }, afterEach() {}, addRoute() {}, removeRoute() {} } },
    '@/config/axios/service': { isRelogin: { show: false } },
    '@/utils/auth': { getAccessToken: () => 'test-token' },
    '@/hooks/web/useTitle': { useTitle() {} },
    '@/hooks/web/useNProgress': { useNProgress: () => ({ start() {}, done() {} }) },
    '@/hooks/web/usePageLoading': { usePageLoading: () => ({ loadStart() {}, loadDone() {} }) },
    '@/store/modules/dict': { useDictStoreWithOut: () => ({ getIsSetDict: true }) },
    '@/store/modules/user': { useUserStoreWithOut: () => user },
    '@/store/modules/permission': { usePermissionStoreWithOut: () => ({
      generateRoutes: async () => {}, staticRouteNamesToRemove: [], getAddRouters: []
    }) }
  }
  vm.runInNewContext(compiled, {
    exports: {}, require: name => {
      assert.ok(dependencies[name], 'unexpected dependency ' + name)
      return dependencies[name]
    }, URLSearchParams
  })
  await guard({ path, fullPath: path, query: {} }, { query: {} }, value => { nextValue = value })
  return JSON.parse(JSON.stringify(nextValue ?? null))
}

for (const [path, permission] of [
  ['/system/user', 'system:user:query'],
  ['/system/role/organization-role', 'system:post:query']
]) {
  test('explicit denial for an unauthorized existing management entry: ' + path, async () => {
    assert.deepEqual(await navigate(path, []), {
      path: '/403', query: { redirect: path }, replace: true
    })
    assert.equal(await navigate(path, [permission]), null)
    assert.equal(await navigate(path, ['*:*:*']), null)
  })
}
test('unrelated missing pages remain 404 candidates and identity loads before permission checks', async () => {
  assert.equal(await navigate('/system/not-a-real-page', []), null)
  assert.equal((await navigate('/system/user', [], false)).path, '/system/user')
  assert.equal(await navigate('/403', []), null)
})
