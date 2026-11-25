const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate, compileScript } = require('vue/compiler-sfc')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function load(file) {
  const out = {}
  new Function('exports', 'require', transpile(fs.readFileSync(file, 'utf8')))(out, id => load(id.startsWith('@/') ? 'src/' + id.slice(2) + '.ts' : path.join(path.dirname(file), id + '.ts')))
  return out
}
const { formatListCertificateType } = load('src/views/dcc/registration-certificate/shared/listDisplay.ts')
const source = fs.readFileSync('src/views/dcc/registration-certificate/index/index.vue', 'utf8')
const order = ['certificateType', 'certificateNo', 'productName', 'ownerCompanyName', 'status', 'expiryDate', 'reminder', 'classification', 'projectCode', 'hasProjectCode', 'versionNo', 'approvalDate', 'effectiveDate', 'hasRegistrationFile', 'remark', 'actions']

test('list uses only saved type and existing labels, including missing historical facts', () => {
  for (const [value, label] of [['DOMESTIC', '国内'], ['FDA', 'FDA'], ['CE', 'CE']]) {
    assert.equal(formatListCertificateType(JSON.stringify({ certificateType: value, classification: 'unrelated', certificateNo: 'CE-FDA' })), label)
  }
  for (const facts of [undefined, null, '', '{}', 'null', '{"certificateType":null}', '{"classification":"FDA","certificateNo":"CE"}']) assert.equal(formatListCertificateType(facts), '—')
  assert.throws(() => formatListCertificateType('{bad json'))
})
test('headers and settings share required order, preserve table key, and compile', () => {
  const headers = [...source.split('<el-table-column type="selection"')[1].split('</el-table>')[0].matchAll(/isCurrentColumnVisible\('([^']+)'\)/g)].map(m => m[1])
  assert.deepEqual(headers, order)
  assert.ok(source.includes("const CURRENT_TABLE_KEY = 'dcc.registrationCertificate.current.actionsDoubleWidthV1'"))
  const definitions = source.match(/const currentColumnDefinitions[^=]*= (\[[\s\S]*?\n\])/)[1]
  assert.deepEqual([...definitions.matchAll(/key: '([^']+)'/g)].map(m => m[1]), order)
  const descriptor = parse(source).descriptor
  const script = compileScript(descriptor, { id: 'certificate-list' })
  assert.deepEqual(compileTemplate({ id: 'certificate-list', source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
})
test('old visibility and widths survive; new type defaults visible and persists changes', async () => {
  const definitions = new Function('return ' + source.match(/const currentColumnDefinitions[^=]*= (\[[\s\S]*?\n\])/)[1])()
  const saved = [{ key: 'certificateNo', visible: true, width: 235 }, { key: 'classification', visible: false, width: 145 }]
  let persisted
  const exports = {}
  new Function('exports', 'require', transpile(fs.readFileSync('src/hooks/web/useUserTableColumns.ts', 'utf8')))(exports, id => id === 'vue' ? vue : id === 'element-plus' ? { ElMessage: { success() {} } } : {
    getUserTableColumnConfig: async () => ({ columns: saved }),
    saveUserTableColumnConfig: async body => { persisted = body },
  })
  let state
  const renderer = vue.createRenderer({ createElement: () => ({}), insert() {}, remove() {}, createComment: () => ({}), parentNode() {}, nextSibling() {} })
  const app = renderer.createApp({ setup() { state = exports.useUserTableColumns('existing-key', definitions); return () => vue.h('div') } })
  app.mount({})
  try {
    await state.loadConfig()
    assert.equal(state.isColumnVisible('certificateType'), true)
    assert.equal(state.isColumnVisible('classification'), false)
    assert.equal(state.getColumnWidth('certificateNo'), 235)
    state.columns.value.find(c => c.key === 'certificateType').visible = false
    await state.saveConfig()
    assert.equal(persisted.columns.find(c => c.key === 'certificateType').visible, false)
    saved.push({ key: 'certificateType', visible: false, width: 155 })
    await state.loadConfig()
    assert.equal(state.isColumnVisible('certificateType'), false)
    state.columns.value.find(c => c.key === 'certificateType').visible = true
    await state.saveConfig()
    assert.equal(persisted.columns.find(c => c.key === 'certificateType').visible, true)
  } finally { app.unmount() }
})
