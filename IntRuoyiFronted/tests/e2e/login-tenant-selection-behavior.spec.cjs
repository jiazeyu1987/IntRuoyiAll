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
  Function('require', 'exports', compile(authSource))(name => {
    if (name === '@/hooks/web/useCache') return { useCache: () => ({ wsCache: cache }), CACHE_KEY: keys }
    if (name === '@/utils/jsencrypt') return { decrypt: value => value }
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

test('first visit has no default tenant, username or password', () => {
  const h = harness()
  const page = h.mount()
  assert.deepEqual({ ...page.loginData.loginForm }, form('', ''))
  assert.doesNotMatch(source, /VITE_APP_DEFAULT_LOGIN_TENANT|getTenantByWebsite/)
  assert.doesNotMatch(descriptor.template.content, /@visible-change=/)
  h.stop()
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
