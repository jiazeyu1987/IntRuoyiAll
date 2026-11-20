const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')

function actions(api = {}) {
  const source = read('src/views/system/role/index.vue')
  const script = source.slice(source.indexOf('const exportDialogVisible'), source.indexOf('/** 初始化 */'))
  const messages = [], downloads = []
  const revision = { value: 0 }, exporting = { value: false }, importing = { value: false }
  let refreshed = 0
  const context = {
    exports: {}, Error, String,
    ref: value => ({ value }),
    FormData: class { append(name, value) { this[name] = value } },
    RoleApi: api, authContextRevision: revision,
    exportLoading: exporting, importLoading: importing, importInputRef: { value: undefined },
    download: { json: (data, name) => downloads.push({ data, name }) },
    message: { success: value => messages.push(['success', value]), warning: value => messages.push(['warning', value]), error: value => messages.push(['error', value]) },
    refreshCategories: async () => { refreshed++ }
  }
  vm.runInNewContext(ts.transpileModule(script + '\nexports.actions = {handleExport, handleImportFileChange, exportError};', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return { ...context.exports.actions, revision, exporting, importing, messages, downloads, get refreshed() { return refreshed } }
}
const event = () => ({ target: { value: 'selected', files: [{ name: '权限角色与审批策略配置包.json' }] } })

test('only the role page exposes the combined package and all changed Vue templates compile', () => {
  for (const file of ['src/views/system/role/index.vue', 'src/views/system/role/EdhrPermissionTransfer.vue',
    'src/views/bpm/model/index.vue', 'src/views/bpm/businessApprovalPolicy/index.vue']) {
    const { descriptor, errors } = parse(read(file), { filename: file })
    assert.deepEqual(errors, [])
    assert.ok(compileScript(descriptor, { id: file }).content)
    assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: file }).errors, [])
  }
  for (const file of ['src/views/bpm/model/index.vue', 'src/views/bpm/businessApprovalPolicy/index.vue']) {
    assert.doesNotMatch(read(file), /PolicyTransferActions|导出审批策略|导入审批策略/)
  }
  assert.match(read('src/views/system/role/EdhrPermissionTransfer.vue'), /权限角色与审批策略配置包（JSON）/)
  assert.doesNotMatch(read('src/views/system/role/index.vue'), /exportCategoryIds/)
  assert.doesNotMatch(read('src/api/bpm/businessApprovalPolicy/index.ts'), /exportPolicies:|importPolicies:/)
})

test('export downloads the complete server package without any category filter', async () => {
  const data = { combined: true }
  const p = actions({ exportRoleConfigPackage: async guard => { assert.equal(guard(), true); return data } })
  await p.handleExport()
  assert.deepEqual(p.downloads, [{ data, name: '权限角色与审批策略配置包.json' }])
  assert.equal(p.exporting.value, false)
})

test('combined entry requires every role and approval permission and keeps the eDHR entry independent', () => {
  const source = read('src/views/system/role/EdhrPermissionTransfer.vue')
  const script = source.slice(source.indexOf('const canExportRole'), source.indexOf('const canExport ='))
  const granted = new Set()
  const context = { exports: {}, computed: fn => ({ get value() { return fn() } }), checkPermi: rights => rights.every(right => granted.has(right)) }
  vm.runInNewContext(ts.transpileModule(script + '\nexports.permissions = {canExportRole, canImportRole};', {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText, context)
  const required = [
    ['canExportRole', ['system:role:export', 'bpm:business-approval-policy:query', 'bpm:model:query']],
    ['canImportRole', ['system:role:create', 'system:role:update', 'system:role-category:create',
      'system:permission:assign-role-menu', 'system:permission:assign-role-data-scope',
      'bpm:business-approval-policy:create', 'bpm:business-approval-policy:publish', 'bpm:business-approval-policy:disable',
      'bpm:model:create', 'bpm:model:deploy', 'bpm:category:create']]
  ]
  for (const [key, rights] of required) {
    granted.clear(); rights.forEach(right => granted.add(right))
    assert.equal(context.exports.permissions[key].value, true)
    for (const right of rights) {
      granted.delete(right)
      assert.equal(context.exports.permissions[key].value, false, right)
      granted.add(right)
    }
  }
  assert.match(source, /eDHR 权限（Excel）/)
})

test('import submits the original file and refreshes roles only after server success', async () => {
  const input = event()
  const p = actions({ importRoleConfigPackage: async (data, guard) => {
    assert.equal(data.file, input.target.files[0]); assert.equal(guard(), true)
  } })
  await p.handleImportFileChange(input)
  assert.equal(input.target.value, '')
  assert.equal(p.refreshed, 1)
  assert.equal(p.messages[0][0], 'success')
  assert.equal(p.messages[1][0], 'warning')
  assert.equal(p.importing.value, false)
})

test('failed imports expose the server error without reporting success or refreshing', async () => {
  const p = actions({ importRoleConfigPackage: async () => { throw new Error('审批角色不存在，整包未导入') } })
  await p.handleImportFileChange(event())
  assert.deepEqual(p.messages, [['error', '审批角色不存在，整包未导入']])
  assert.equal(p.refreshed, 0)
  assert.equal(p.importing.value, false)
})

test('tenant changes invalidate pending import and export responses', async () => {
  const p = actions({
    importRoleConfigPackage: async (data, guard) => { p.revision.value++; assert.equal(guard(), false) },
    exportRoleConfigPackage: async guard => { p.revision.value++; assert.equal(guard(), false); return {} }
  })
  await p.handleImportFileChange(event())
  await p.handleExport()
  assert.equal(p.refreshed, 0)
  assert.equal(p.downloads.length, 0)
  assert.match(p.messages[0][1], /租户状态已变更/)
  assert.match(p.exportError.value, /租户或登录状态已变更/)
})

test('pending import blocks duplicate submissions and cancellation sends no request', async () => {
  let finish, calls = 0
  const p = actions({ importRoleConfigPackage: () => { calls++; return new Promise(resolve => { finish = resolve }) } })
  const pending = p.handleImportFileChange(event())
  await p.handleImportFileChange(event())
  assert.equal(calls, 1)
  finish()
  await pending
  await p.handleImportFileChange({ target: { value: '', files: [] } })
  assert.equal(calls, 1)
  assert.equal(p.refreshed, 1)
})
