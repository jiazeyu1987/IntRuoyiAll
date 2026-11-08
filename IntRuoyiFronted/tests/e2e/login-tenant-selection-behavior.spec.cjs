const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const file = path.resolve(__dirname, '../../src/views/Login/components/LoginForm.vue')
const descriptor = parse(fs.readFileSync(file, 'utf8')).descriptor
const ast = ts.createSourceFile('login.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set(['tenantSelectedByUser', 'handleTenantSelection', 'getTenantByWebsite'])
const source = ast.statements.filter(statement => ts.isVariableStatement(statement)
  && statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast))))
  .map(statement => statement.getText(ast)).join('\n')
const code = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function harness() {
  let resolveWebsite
  const website = new Promise(resolve => { resolveWebsite = resolve })
  const state = {
    loginData: { tenantEnable: 'true', loginForm: { tenantName: '默认租户' } },
    loginLoading: { value: false }, passwordChangeRequired: { value: false },
    LoginApi: { getTenantByWebsite: () => website }, location: { host: 'localhost:8081' },
    handleTenantNameChange: () => {}, authUtil: { setTenantId: id => { state.tenantId = id } }
  }
  const api = Function(...Object.keys(state), `${code}; return { handleTenantSelection, getTenantByWebsite }`)(...Object.values(state))
  return { state, api, resolveWebsite }
}

test('late website response preserves an explicit tenant selection', async () => {
  const h = harness()
  const pending = h.api.getTenantByWebsite()
  h.state.loginData.loginForm.tenantName = '测试5'
  h.api.handleTenantSelection('测试5')
  h.resolveWebsite({ name: '域名默认租户', id: 1 })
  await pending
  assert.equal(h.state.loginData.loginForm.tenantName, '测试5')
  assert.equal(h.state.tenantId, undefined)
})

for (const phase of ['loginLoading', 'passwordChangeRequired']) {
  test(`late website response cannot replace tenant during ${phase}`, async () => {
    const h = harness()
    const pending = h.api.getTenantByWebsite()
    h.state[phase].value = true
    h.resolveWebsite({ name: '域名默认租户', id: 1 })
    await pending
    assert.equal(h.state.loginData.loginForm.tenantName, '默认租户')
    assert.equal(h.state.tenantId, undefined)
  })
}

test('website tenant still initializes an untouched login form', async () => {
  const h = harness()
  const pending = h.api.getTenantByWebsite()
  h.resolveWebsite({ name: '域名默认租户', id: 1 })
  await pending
  assert.equal(h.state.loginData.loginForm.tenantName, '域名默认租户')
  assert.equal(h.state.tenantId, 1)
})
