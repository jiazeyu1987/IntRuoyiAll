const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { createPinia } = require('pinia')
const { createRouter, createMemoryHistory } = require('vue-router')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')

function loadSource(file, imports) {
  const exports = {}
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  })
  vm.runInNewContext(compiled.outputText, {
    exports, Error, URLSearchParams,
    console: { error() {} },
    require(id) {
      assert.ok(Object.hasOwn(imports, id), `Unexpected dependency: ${id}`)
      return imports[id]
    }
  }, { filename: file })
  return exports
}

function setup() {
  let menus = []
  let token = true
  let initializationFailure
  const pinia = createPinia()
  const user = {
    getIsSetUser: false, getRoles: [], getPermissions: new Set(),
    async setUserInfoAction() {
      if (initializationFailure) throw initializationFailure
      this.getIsSetUser = true
    },
    clearSession() { token = false; this.getIsSetUser = false }
  }
  const permission = loadSource('src/store/modules/permission.ts', {
    pinia: require('pinia'),
    './testMode': { filterTestModeMenus: x => x, useTestModeStore: () => ({ enabled: false }) },
    '@/store': { store: pinia },
    'lodash-es': require('lodash-es'),
    '@/router/modules/remaining': { default: [] },
    '@/router/modules/showroom': { default: [] },
    // These fixtures already have generated route paths; exercise the actual store's
    // migration validation, state transitions and promise behavior, independent of views.
    '@/utils/routerHelper': { flatMultiLevelRoutes: x => x, generateRoute: x => x },
    '@/hooks/web/useCache': { CACHE_KEY: { ROLE_ROUTERS: 'routes' }, useCache: () => ({ wsCache: { get: () => menus } }) },
    '@/utils/is': { isUrl: x => /^https?:/.test(x) },
    '@/utils/signatureAccess': { filterSignatureMenus: x => x },
    '@/utils/operationLogAccess': loadSource('src/utils/operationLogAccess.ts', {}),
    './user': { useUserStore: () => user }
  }).usePermissionStoreWithOut()
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/', redirect: '/home' },
    ...['/login', '/home', '/user/profile'].map(path => ({ path, component: {} }))
  ] })
  const loading = { page: false, progress: false }
  const relogin = { show: false }
  loadSource('src/permission.ts', {
    './router': { default: router },
    '@/config/axios/service': { isRelogin: relogin },
    '@/utils/auth': { getAccessToken: () => token },
    '@/utils/operationLogAccess': loadSource('src/utils/operationLogAccess.ts', {}),
    '@/hooks/web/useTitle': { useTitle() {} },
    '@/hooks/web/useNProgress': { useNProgress: () => ({ start: () => { loading.progress = true }, done: () => { loading.progress = false } }) },
    '@/hooks/web/usePageLoading': { usePageLoading: () => ({ loadStart: () => { loading.page = true }, loadDone: () => { loading.page = false } }) },
    '@/store/modules/dict': { useDictStoreWithOut: () => ({ getIsSetDict: true }) },
    '@/store/modules/user': { useUserStoreWithOut: () => user },
    '@/store/modules/permission': { usePermissionStoreWithOut: () => permission }
  })
  return {
    router, user, permission, loading, relogin,
    setMenus: value => { menus = value },
    signIn: () => { token = true },
    failUserInfo: error => { initializationFailure = error },
    hasToken: () => token
  }
}

const project = () => ({ path: '/mes/md/dcc-project-code', name: 'ProjectCode', component: {} })
const oldMenus = () => [{ path: '/mes', name: 'MES', component: {}, children: [project()] }]
const migratedMenus = () => [{ path: '/mdm', name: 'MasterData', component: {}, children: [project()] }]

test('missing master-data directory rejects instead of leaving a pending promise', async () => {
  const app = setup()
  app.setMenus(oldMenus())
  await assert.rejects(app.permission.generateRoutes(), /缺少基础数据目录授权/)
  assert.equal(app.permission.addRouters.length, 0)
})

test('menu failure completes initial navigation, clears partial identity and permits retry after repair', async () => {
  const app = setup()
  app.setMenus(oldMenus())
  await app.router.push('/user/profile')
  await app.router.isReady()
  assert.equal(app.router.currentRoute.value.path, '/login')
  assert.equal(app.router.currentRoute.value.query.redirect, '/user/profile')
  assert.match(app.permission.initializationError, /缺少基础数据目录授权/)
  assert.equal(app.user.getIsSetUser, false)
  assert.equal(app.hasToken(), false)
  assert.deepEqual(app.loading, { page: false, progress: false })
  assert.equal(app.relogin.show, false)

  app.setMenus(migratedMenus())
  app.signIn()
  await app.router.push('/user/profile')
  assert.equal(app.router.currentRoute.value.path, '/user/profile')
  assert.equal(app.permission.initializationError, '')
  assert.equal(app.user.getIsSetUser, true)
  assert.equal(app.hasToken(), true)
  assert.equal(app.router.resolve('/mes/md/dcc-project-code').name, 'ProjectCode')
  assert.deepEqual(app.loading, { page: false, progress: false })
})

test('user-info request failure returns its diagnostic and ends loading', async () => {
  const app = setup()
  app.failUserInfo(new Error('用户信息加载失败'))
  await app.router.push('/home')
  assert.equal(app.router.currentRoute.value.path, '/login')
  assert.equal(app.permission.initializationError, '用户信息加载失败')
  assert.equal(app.hasToken(), false)
  assert.equal(app.relogin.show, false)
  assert.deepEqual(app.loading, { page: false, progress: false })
})

test('unrelated route component failure still rejects and releases loading', async () => {
  const app = setup()
  app.user.getIsSetUser = true
  app.router.addRoute({ path: '/broken', component: () => Promise.reject(new Error('组件加载失败')) })
  await assert.rejects(app.router.push('/broken'), /组件加载失败/)
  assert.deepEqual(app.loading, { page: false, progress: false })
})

test('login form compiles with the shared initialization error alert', () => {
  const filename = 'src/views/Login/components/LoginForm.vue'
  const { descriptor, errors } = parse(fs.readFileSync(path.join(root, filename), 'utf8'), { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'login' })
  assert.ok(script.bindings.displayLoginError)
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'login', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
})
