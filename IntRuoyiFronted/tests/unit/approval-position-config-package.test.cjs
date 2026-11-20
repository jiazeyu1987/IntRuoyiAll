const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/dcc/controlled-file/positions/index.vue')
const source = fs.readFileSync(filename, 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const script = compileScript(descriptor, { id: 'positions-test' })
assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'positions-test', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
function setup(fail = false) {
  const calls = [], exported = new Blob(['{}'])
  const state = { exportLoading: { value: false }, importLoading: { value: false },
    importInputRef: { value: { click: () => calls.push('choose') } }, FormData,
    exportApprovalPositionConfigPackage: async () => { if (fail) throw new Error('export denied'); return exported },
    importApprovalPositionConfigPackage: async data => { calls.push(data.get('file').name); if (fail) throw new Error('import rejected') },
    download: { json: (blob, name) => calls.push([blob, name]) },
    message: { success: () => calls.push('success') }, loadData: async () => calls.push('reload') }
  const handlers = descriptor.scriptSetup.content.split('const handleExport =')[1].split('onMounted(')[0]
  vm.runInNewContext(ts.transpileModule('const handleExport =' + handlers + ';globalThis.handlers = { handleExport, openImport, handleImportFileChange }', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, state)
  return { state, calls, exported, handlers: state.handlers }
}
test('approval position export downloads the real JSON result and opens the file chooser', async () => {
  const { handlers, calls, exported, state } = setup()
  handlers.openImport()
  await handlers.handleExport()
  assert.equal(calls[0], 'choose')
  assert.equal(calls[1][0], exported)
  assert.equal(calls[1][1], '审批角色配置包.json')
  assert.equal(state.exportLoading.value, false)
})
test('successful import sends the selected file, resets the input and reloads roles', async () => {
  const { handlers, calls, state } = setup()
  const input = { files: [new File(['{}'], 'roles.json')], value: 'roles.json' }
  await handlers.handleImportFileChange({ target: input })
  assert.equal(input.value, '')
  assert.deepEqual(calls, ['roles.json', 'success', 'reload'])
  assert.equal(state.importLoading.value, false)
})
test('failed requests propagate errors, clear loading and never report import success', async () => {
  const { handlers, calls, state } = setup(true)
  await assert.rejects(handlers.handleExport(), /export denied/)
  await assert.rejects(handlers.handleImportFileChange({ target: { files: [new File(['{}'], 'roles.json')], value: 'roles.json' } }), /import rejected/)
  assert.deepEqual(calls, ['roles.json'])
  assert.equal(state.exportLoading.value, false)
  assert.equal(state.importLoading.value, false)
})