const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const { webcrypto } = require('node:crypto')
const loadAudit = require('../helpers/load-permission-audit.cjs')

// Model the actual HTTP browser capability: secure random values, no randomUUID.
const httpCrypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }
const uuidPattern = '[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}'

test('HTTP crypto generates distinct audit keys with UUID v4 version and variant', () => {
  const { createPermissionAuditIdempotencyKey: create } = loadAudit(httpCrypto)
  const keys = Array.from({ length: 1000 }, () => create('USER-ROLE', 910506))
  assert.equal(new Set(keys).size, keys.length)
  for (const key of keys) assert.match(key, new RegExp(`^SYSTEM-PERM-USER-ROLE-910506-${uuidPattern}$`))
  const fixed = loadAudit({ getRandomValues: bytes => bytes.fill(255) })
  assert.equal(fixed.createPermissionAuditIdempotencyKey('ROLE-MENU', 9),
    'SYSTEM-PERM-ROLE-MENU-9-ffffffff-ffff-4fff-bfff-ffffffffffff')
})

test('missing or failing secure random source fails explicitly and preserves its cause', () => {
  for (const crypto of [null, {}]) {
    const audit = loadAudit(crypto)
    assert.throws(() => audit.createPermissionAuditIdempotencyKey('DATA-SCOPE', 9),
      error => error instanceof audit.PermissionAuditKeyError && /不支持安全随机数/.test(error.message))
  }
  const cause = new Error('entropy unavailable')
  const audit = loadAudit({ getRandomValues() { throw cause } })
  assert.throws(() => audit.createPermissionAuditIdempotencyKey('USER-ROLE', 9),
    error => error instanceof audit.PermissionAuditKeyError && error.cause === cause)
})

function component(filename, crypto, options) {
  const audit = loadAudit(crypto)
  const { descriptor, errors } = parse(fs.readFileSync(path.resolve(__dirname, '../../src/views/system', filename), 'utf8'))
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: filename,
    compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  const source = ts.transpileModule(script.content, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const exports = {}, writes = [], messages = [], events = [], confirmations = []
  const save = async data => {
    writes.push(data)
    if (options.pendingSave) await options.pendingSave
    if (options.saveError) throw options.saveError
  }
  const imports = {
    vue,
    '@/utils/permissionAudit': audit,
    '@/utils/tree': { defaultProps: {}, handleTree: x => x },
    '@/utils/constants': { SystemDataScopeEnum: { DEPT_CUSTOM: 2 } },
    '@/utils/dict': { DICT_TYPE: {}, getIntDictOptions: () => [] },
    '@/api/system/permission': {
      getUserRoleList: async () => [], getRoleMenuList: async () => [],
      assignUserRole: save, assignRoleMenu: save, assignRoleDataScope: save
    },
    '@/api/system/user': {},
    '@/api/system/role': { getSimpleRoleList: async () => [{ id: 991325, name: '项目代码管理员' }] },
    '@/api/system/menu': { getSimpleMenusList: async () => [] },
    '@/api/system/dept': { getSimpleDeptList: async () => [] },
    './role-option-label': { roleOptionLabel: role => role.name }
  }
  const confirmation = {}
  new Function('exports', ts.transpileModule(fs.readFileSync(path.resolve(__dirname,
    '../../src/views/system/user/user-change-confirmation.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText)(confirmation)
  imports['./user-change-confirmation'] = confirmation
  const globals = { ...vue, useI18n: () => ({ t: key => key }), useMessage: () => ({
    success: m => messages.push(m), error: m => messages.push(m),
    confirm: async content => { confirmations.push(content) }
  }) }
  new Function('exports', 'require', ...Object.keys(globals), source)(exports,
    id => { assert.ok(Object.hasOwn(imports, id), id); return imports[id] }, ...Object.values(globals))
  const state = exports.default.setup({}, { expose() {}, emit: (...args) => events.push(args) })
  state.formRef.value = { validate: async () => true, resetFields() {} }
  state.treeRef && (state.treeRef.value = {
    getCheckedKeys: () => [42], getHalfCheckedKeys: () => [], setChecked() {}, setCheckedNodes() {}, setCheckedKeys() {}
  })
  return { state, writes, messages, events, confirmations, audit }
}

for (const [filename, operation] of [
  ['user/UserAssignRoleForm.vue', 'USER-ROLE'],
  ['role/RoleAssignMenuForm.vue', 'ROLE-MENU'],
  ['role/RoleDataPermissionForm.vue', 'DATA-SCOPE']
]) {
  const prepare = async (crypto, options = {}) => {
    const f = component(filename, crypto, options)
    await f.state.open({ id: 910506, username: 'wangyiru', nickname: '王艺茹', name: '角色', code: 'role', dataScope: 2, dataScopeDeptIds: [42] })
    const data = vue.unref(f.state.formData)
    data.reason = ' 岗位职责调整 '
    if (operation === 'USER-ROLE') data.roleIds = [991325]
    return f
  }
  test(`${filename}: HTTP confirmation submits audit key and closes only after save`, async () => {
    const f = await prepare(httpCrypto)
    await f.state.submitForm()
    assert.equal(f.writes.length, 1)
    assert.equal(f.writes[0].reason, '岗位职责调整')
    assert.match(f.writes[0].idempotencyKey, new RegExp(`^SYSTEM-PERM-${operation}-910506-${uuidPattern}$`))
    assert.equal(f.state.dialogVisible.value, false)
    assert.equal(f.events.length, 1)
    if (operation === 'USER-ROLE') {
      assert.deepEqual(f.writes[0].roleIds, [991325])
      assert.equal(f.confirmations.length, 1)
    }
  })
  test(`${filename}: random failure shows error, retains input and prevents save`, async () => {
    const f = await prepare({})
    await assert.rejects(f.state.submitForm(), f.audit.PermissionAuditKeyError)
    assert.equal(f.writes.length, 0)
    assert.equal(f.events.length, 0)
    assert.equal(f.state.dialogVisible.value, true)
    assert.equal(f.state.formLoading.value, false)
    assert.equal(vue.unref(f.state.formData).reason, ' 岗位职责调整 ')
    assert.equal(f.messages.length, 1)
    assert.match(f.messages[0], /不支持安全随机数/)
  })
  test(`${filename}: failed save keeps the dialog and changes without emitting success`, async () => {
    const saveError = new Error('save rejected')
    const f = await prepare(httpCrypto, { saveError })
    await assert.rejects(f.state.submitForm(), error => error === saveError)
    assert.equal(f.writes.length, 1)
    assert.equal(f.events.length, 0)
    assert.equal(f.messages.length, 0)
    assert.equal(f.state.dialogVisible.value, true)
    assert.equal(f.state.formLoading.value, false)
    assert.equal(vue.unref(f.state.formData).reason, ' 岗位职责调整 ')
    if (operation === 'USER-ROLE') assert.deepEqual(f.state.formData.value.roleIds, [991325])
  })
  test(`${filename}: pending save cannot close the dialog or announce success`, async () => {
    let resolve
    const f = await prepare(httpCrypto, { pendingSave: new Promise(yes => { resolve = yes }) })
    const pending = f.state.submitForm()
    await new Promise(yes => setImmediate(yes))
    assert.equal(f.writes.length, 1)
    assert.equal(f.state.dialogVisible.value, true)
    assert.equal(f.state.formLoading.value, true)
    assert.equal(f.events.length, 0)
    assert.equal(f.messages.length, 0)
    resolve()
    await pending
    assert.equal(f.state.dialogVisible.value, false)
    assert.equal(f.events.length, 1)
  })
}
