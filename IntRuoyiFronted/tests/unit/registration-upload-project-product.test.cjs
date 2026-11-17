const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const vue = require('vue')
const compiler = require('vue/compiler-sfc')
const ts = require('typescript')

const filename = path.resolve(__dirname, '../../src/views/dcc/registration-certificate/upload/UploadDialog.vue')
const { descriptor } = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
const script = compiler.compileScript(descriptor, { id: 'registration-upload-test' })
const template = compiler.compileTemplate({ source: descriptor.template.content, filename, id: 'registration-upload-test', compilerOptions: { bindingMetadata: script.bindings } })
assert.deepEqual(template.errors, [])

const node = (type, text = '') => ({ type, text, props: {}, children: [], parent: null })
const renderer = vue.createRenderer({
  createElement: node, createText: text => node('#text', text), createComment: text => node('#comment', text),
  setText: (el, text) => { el.text = text }, setElementText: (el, text) => { el.text = text; el.children = [] },
  patchProp: (el, key, old, value) => { el.props[key] = value }, parentNode: el => el.parent,
  nextSibling: el => el.parent?.children[el.parent.children.indexOf(el) + 1] ?? null,
  insert(el, parent, anchor = null) { if (el.parent) this.remove(el); el.parent = parent; const i = parent.children.indexOf(anchor); parent.children.splice(i < 0 ? parent.children.length : i, 0, el) },
  remove(el) { const i = el.parent?.children.indexOf(el); if (i >= 0) el.parent.children.splice(i, 1); el.parent = null }
})
const product = id => ({ projectCodeId: id, productCatalogId: `catalog-${id}`, productCode: '0313170202', productName: `DCC产品${id}`, bindings: [{ source: 'CONFIRMED', relationId: '56' }] })
const flush = async () => { await new Promise(setImmediate); await vue.nextTick() }

function mount(apiOverrides = {}, props = {}) {
  const api = { getUploadOwnerCompanies: async () => [], getUploadProjectCodes: async () => [], getUploadProjectProduct: async id => product(id), ...apiOverrides }
  const run = source => {
    const module = { exports: {} }
    const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
    vm.runInNewContext(js, { module, exports: module.exports, require: id => id === 'vue' ? vue : id.includes('registrationCertificate') ? api : id.includes('/utils') ? { generateUUID: () => 'test-id' } : id.includes('/facts') ? { emptyFacts: () => ({ certificateType: 'DOMESTIC', ownProduct: true }), validateFacts: () => {} } : { __esModule: true, default: { render: () => null } }, useMessage: () => ({ success() {}, error() {} }) })
    return module.exports
  }
  const component = run(script.content).default
  component.render = run(template.code).render
  const app = renderer.createApp(component, { modelValue: false, ...props })
  for (const name of ['el-dialog', 'el-form', 'el-row', 'el-col', 'el-form-item', 'el-select', 'el-option', 'el-input', 'el-button', 'el-alert', 'el-upload', 'el-checkbox']) {
    app.component(name, { inheritAttrs: false, setup: (_, { attrs, slots }) => () => vue.h(name, attrs, [slots.default?.(), slots.footer?.()]) })
  }
  const host = node('root')
  const instance = app.mount(host)
  return { state: instance.$.setupState, host, app, open: () => { instance.$.props.modelValue = true } }
}
function elements(host, type) { return [ ...(host.type === type ? [host] : []), ...host.children.flatMap(child => elements(child, type)) ] }

test('选定项目自动显示正式DCC产品，编码和名称只读', async () => {
  const { state, host, app } = mount()
  state.projectCodeId = '501'
  await flush()
  assert.equal(state.productName, 'DCC产品501')
  assert.equal(state.facts.projectProduct.productCode, '0313170202')
  const inputs = elements(host, 'el-input')
  assert.ok(inputs.some(input => input.props['model-value'] === '0313170202' && Object.hasOwn(input.props, 'readonly')))
  assert.ok(inputs.some(input => input.props.modelValue === 'DCC产品501' && input.props.readonly))
  app.unmount()
})

test('快速切换项目时，旧请求不能覆盖新项目产品', async () => {
  const pending = new Map()
  const { state, app } = mount({ getUploadProjectProduct: id => new Promise(resolve => pending.set(id, resolve)) })
  state.projectCodeId = '501'; await flush()
  state.projectCodeId = '502'; await flush()
  pending.get('502')(product('502')); await flush()
  pending.get('501')(product('501')); await flush()
  assert.equal(state.productName, 'DCC产品502')
  assert.equal(state.facts.projectProduct.projectCodeId, '502')
  app.unmount()
})

test('缺少绑定时清空旧产品并禁止提交', async () => {
  const { state, host, app } = mount({ getUploadProjectProduct: async () => { throw new Error('项目尚未绑定有效的DCC产品') } })
  state.productName = '旧产品'; state.projectCodeId = '501'; await flush()
  assert.equal(state.productName, '')
  assert.equal(state.facts.projectProduct, undefined)
  assert.match(state.productError, /项目尚未绑定/)
  assert.ok(elements(host, 'el-button').some(button => button.props.disabled === true))
  app.unmount()
})

test('清空项目后不会保留产品，非瑛泰产品仍可手填名称', async () => {
  const { state, app } = mount()
  state.projectCodeId = '501'; await flush()
  state.projectCodeId = undefined; await flush()
  assert.equal(state.productName, '')
  assert.equal(state.facts.projectProduct, undefined)
  state.facts.ownProduct = false; await flush()
  state.productName = '客户产品'
  assert.equal(state.productName, '客户产品')
  app.unmount()
})

test('任务入口预设项目后自动加载产品', async () => {
  const { state, app, open } = mount({}, { taskProject: { id: '501', projectCode: 'IDI' } })
  open(); await flush(); await flush()
  assert.equal(state.productName, 'DCC产品501')
  assert.equal(state.facts.projectProduct.projectCodeId, '501')
  assert.equal(state.hydrating, false)
  app.unmount()
})

test('驳回后重提非瑛泰产品时保留原产品名称', async () => {
  const { state, app, open } = mount({ getMyRegistrationCertificateUpload: async () => ({
    request: { detailJson: JSON.stringify({ draft: { productName: '客户产品', ownerCompanyId: '1' }, factsJson: JSON.stringify({ certificateType: 'DOMESTIC', ownProduct: false }) }) }, files: []
  }) }, { requestId: '101' })
  open(); await flush(); await flush()
  assert.equal(state.productName, '客户产品')
  assert.equal(state.facts.projectProduct, undefined)
  assert.equal(state.hydrating, false)
  app.unmount()
})
