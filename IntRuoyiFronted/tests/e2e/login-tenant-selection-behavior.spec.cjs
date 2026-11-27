const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const descriptor = parse(fs.readFileSync(path.join(root, 'src/views/Login/components/LoginForm.vue'), 'utf8')).descriptor
const ast = ts.createSourceFile('login.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set(['loginData', 'applyLoginFormCache', 'handleTenantSelection', 'getLoginFormCache'])
const source = ast.statements.filter(statement =>
  (ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast))))
  || (ts.isExpressionStatement(statement) && statement.expression.expression?.getText(ast) === 'onMounted')
).map(statement => statement.getText(ast)).join('\n').replaceAll('import.meta.env.VITE_APP_TENANT_ENABLE', "'true'")
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText

function harness() {
  const values = new Map()
  const keys = { LoginForm: 'loginForm', LoginTenantHistory: 'loginTenantHistory' }
  const cache = { get: key => values.get(key), set: (key, value) => values.set(key, structuredClone(value)), delete: key => values.delete(key) }
  const authModule = { exports: {} }
  const authSource = fs.readFileSync(path.join(root, 'src/utils/auth.ts'), 'utf8')
  const uuidModule = { exports: {} }
  Function('exports', compile(fs.readFileSync(path.join(root, 'src/utils/uuid.ts'), 'utf8')))(uuidModule.exports)
  Function('require', 'exports', compile(authSource))(name => {
    if (name === '@/hooks/web/useCache') return { useCache: () => ({ wsCache: cache }), CACHE_KEY: keys }
    if (name === '@/utils/jsencrypt') return { decrypt: value => value }
    if (name === '@/utils/uuid') return uuidModule.exports
    if (name === 'vue') return vue
    throw new Error(`Unexpected import: ${name}`)
  }, authModule.exports)
  const auth = authModule.exports
  let scope
  const mount = () => {
    scope = vue.effectScope()
    return scope.run(() => Function('reactive', 'watch', 'onMounted', 'authUtil', 'loadTenantOptions', `${compile(source)}; return { loginData, handleTenantSelection }`)(vue.reactive, vue.watch, callback => callback(), auth, () => {}))
  }
  return { values, auth, mount, stop: () => scope?.stop() }
}
const form = (tenantName, username, rememberMe = false) => ({ tenantName, username, password: '', rememberMe })

test('first visit starts empty before loading the configured default tenant', () => {
  const h = harness()
  const page = h.mount()
  assert.deepEqual({ ...page.loginData.loginForm }, form('', ''))
  assert.doesNotMatch(source, /VITE_APP_DEFAULT_LOGIN_TENANT|getTenantByWebsite/)
  assert.doesNotMatch(descriptor.template.content, /@visible-change=/)
  h.stop()
})

const loadTenantSource = ast.statements.find(statement =>
  ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration =>
    declaration.name.getText(ast) === 'loadTenantOptions'
  )
).getText(ast)

async function loadTenants(tenantName, getTenantList) {
  const loginData = { tenantEnable: 'true', loginForm: form(tenantName, '') }
  const tenantOptions = vue.ref([])
  const tenantListLoading = vue.ref(false)
  const loginErrorMessage = vue.ref('')
  const load = Function('loginData', 'tenantOptions', 'tenantListLoading', 'loginErrorMessage', 'getTenantList', 'resolveLoginErrorMessage',
    compile(loadTenantSource) + '; return loadTenantOptions'
  )(loginData, tenantOptions, tenantListLoading, loginErrorMessage, getTenantList, error => error.message)
  await load()
  return { loginData, tenantOptions, tenantListLoading, loginErrorMessage }
}

test('fresh browser selects the configured default and leaves credentials empty', async () => {
  const result = await loadTenants('', async () => [
    { id: 2, name: '普通租户', defaultLogin: false },
    { id: 1, name: '芋道源码', defaultLogin: true }
  ])
  assert.deepEqual(result.loginData.loginForm, form('芋道源码', ''))
  assert.equal(result.tenantListLoading.value, false)
})

test('administrator can change the default without overriding browser history', async () => {
  const options = async () => [{ id: 2, name: '新默认租户', defaultLogin: true }]
  assert.equal((await loadTenants('', options)).loginData.loginForm.tenantName, '新默认租户')
  assert.equal((await loadTenants('历史租户', options)).loginData.loginForm.tenantName, '历史租户')
})

test('missing default or failed tenant request shows an error without inventing a tenant', async () => {
  const missing = await loadTenants('', async () => [{ id: 2, name: '普通租户', defaultLogin: false }])
  assert.equal(missing.loginData.loginForm.tenantName, '')
  assert.match(missing.loginErrorMessage.value, /未配置登录默认租户/)
  const failed = await loadTenants('', async () => { throw new Error('服务不可达') })
  assert.equal(failed.loginData.loginForm.tenantName, '')
  assert.equal(failed.loginErrorMessage.value, '服务不可达')
  assert.equal(failed.tenantListLoading.value, false)
})

for (const username of ['admin', 'aoteman', 'production-user']) {
  test(`restores last tenant and ${username} without remember me and never stores password`, () => {
    const h = harness()
    const page = h.mount()
    page.loginData.loginForm.tenantName = '上次选择的租户'
    page.handleTenantSelection()
    page.loginData.loginForm.username = username
    page.loginData.loginForm.password = 'unit-secret'
    // Also exercise the successful-login cache writer.
    h.auth.setLoginForm({ ...page.loginData.loginForm })
    assert.equal(JSON.stringify([...h.values.values()]).includes('unit-secret'), false)
    h.stop()
    const reopened = h.mount()
    assert.deepEqual({ ...reopened.loginData.loginForm }, form('上次选择的租户', username))
    h.stop()
  })
}

test('switching to a tenant with another historical account preserves the current username', () => {
  const h = harness()
  h.auth.setLoginForm(form('租户乙', 'old-user', true))
  h.auth.setLoginForm(form('租户甲', 'current-user'))
  const page = h.mount()
  page.loginData.loginForm.username = 'edited-user'
  page.loginData.loginForm.password = 'unit-secret'
  page.loginData.loginForm.tenantName = '租户乙'
  page.handleTenantSelection()
  assert.equal(page.loginData.loginForm.username, 'edited-user')
  assert.equal(page.loginData.loginForm.password, '')
  assert.equal(h.auth.getLoginForm().tenantName, '租户乙')
  assert.equal(h.auth.getLoginForm().username, 'edited-user')
  page.loginData.loginForm.tenantName = ''
  page.handleTenantSelection()
  assert.equal(page.loginData.loginForm.username, 'edited-user')
  assert.equal(h.auth.getLoginForm().tenantName, '')
  h.stop()
  assert.equal(h.mount().loginData.loginForm.tenantName, '')
  h.stop()
})

test('clearing username remains cleared after reopening', () => {
  const h = harness()
  h.auth.setLoginForm(form('租户甲', 'admin'))
  h.mount().loginData.loginForm.username = ''
  h.stop()
  assert.equal(h.mount().loginData.loginForm.username, '')
  h.stop()
})

test('tenant and username edits survive reopening without submitting login', () => {
  const h = harness()
  const page = h.mount()
  page.loginData.loginForm.tenantName = '租户乙'
  page.loginData.loginForm.username = 'admin'
  h.stop()
  assert.deepEqual({ ...h.mount().loginData.loginForm }, form('租户乙', 'admin'))
  h.stop()
})

test('legacy cached passwords are removed while retaining the selected account', () => {
  const h = harness()
  h.values.set('loginForm', { ...form('租户甲', 'admin'), password: 'unit-legacy-secret' })
  assert.deepEqual(h.auth.getLoginForm(), form('租户甲', 'admin'))
  assert.equal(JSON.stringify(h.values.get('loginForm')).includes('unit-legacy-secret'), false)
  h.stop()
})
