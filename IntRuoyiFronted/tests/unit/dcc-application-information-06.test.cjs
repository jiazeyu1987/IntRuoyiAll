const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const base = 'src/views/dcc/controlled-file/'
function load(source, dependencies = {}) {
  const context = { exports: {}, require: id => {
    if (id === 'vue') return vue
    if (id in dependencies) return dependencies[id]
    if (id.endsWith('.vue')) return {}
    if (id.startsWith('./')) return load(fs.readFileSync(base + 'detail/' + id.slice(2) + '.ts', 'utf8'))
    if (id === '../shared/version-display') return load(fs.readFileSync(base + 'shared/version-display.ts', 'utf8'))
    if (id.endsWith('bodyReadContext')) return load(fs.readFileSync('src/api/dcc/controlledFile/bodyReadContext.ts', 'utf8'))
    throw Error(id)
  }, Error, JSON, Set, BigInt, Number, String }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
async function render({ source = 'C/3', recorded = true, libraryScope, description = '1', attributes = {} } = {}) {
  const file = base + 'detail/DetailApplicationHistory.vue'
  const { descriptor } = parse(fs.readFileSync(file, 'utf8'))
  const script = compileScript(descriptor, { id: file })
  const template = compileTemplate({ source: descriptor.template.content, filename: file, id: file, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N', ...(libraryScope ? { libraryScope } : {}), ...attributes }
  const round = { controlledFileId: '10', applicationType: 'REVISION', bpmRound: 'own-round', attributeRound: 1,
    applicationTime: '2026-11-12 17:10:57', revisionChangeType: 'PARTIAL', revisionSourceVersionNo: source, changeDescription: description }
  const component = load(script.content, {
    '@/api/dcc/controlledFile/applicationRead': { getControlledFileApplicationRounds: async () => [round],
      getControlledFileApplicationEvidence: async () => ({ ...round, versionNo: 'C/4', recorded, attributeRound: recorded ? 1 : null,
        actualAttributes: recorded ? attrs : null, defaultSource: recorded ? attrs : null, unavailableReason: recorded ? null : 'NOT_RECORDED', signatures: [] }) },
    './presentation': { formatControlledFileDateTime: v => v }
  }).default
  component.render = load(template.code).render
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({ text: '' }),
    insert: (child, parent) => parent.children.push(child), remove() {}, setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text },
    parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const app = renderer.createApp(component, { fileId: '10', expectedVersionNo: 'C/4', primaryBpmRound: 'own-round', lockPrimaryRound: true })
  app.directive('loading', {})
  for (const name of ['ContentWrap', 'el-descriptions', 'el-descriptions-item', 'el-alert', 'el-select', 'el-option']) {
    app.component(name, { props: ['label', 'title'], setup: (props, { slots }) => () => vue.h('div', [props.label || props.title || '', slots.default?.()]) })
  }
  const host = { children: [] }
  app.mount(host)
  for (let n = 0; n < 20; n++) await Promise.resolve()
  await vue.nextTick()
  const texts = node => [node.text || '', ...(node.children || []).map(texts)].join('')
  const text = texts(host)
  app.unmount()
  return text
}
test('renders nine independent application facts for C/4 from C/3', async () => {
  const text = await render()
  for (const fact of ['申请类型局部变更', '申请时间2026-11-12 17:10:57', '目标市场CE 欧盟', '注册人身份是', '生产方身份否',
    '文件转移否', '变更类型局部变更', '来源文件版本C/3', '变更说明1']) assert.ok(text.includes(fact), fact + ': ' + text)
})
test('missing source is explicit without deriving a version', async () => {
  const text = await render({ source: null })
  assert.ok(text.includes('来源文件版本未记录')); assert.ok(!text.includes('C/3'))
})
test('missing attributes retain all labels and the real revision context', async () => {
  const text = await render({ recorded: false })
  for (const label of ['目标市场', '注册人身份', '生产方身份', '文件转移']) assert.ok(text.includes(label + '未记录'))
  assert.ok(text.includes('历史申请属性未记录')); assert.ok(text.includes('来源文件版本C/3'))
})
test('library scope explicitly marks project attributes inapplicable', async () => {
  const text = await render({ libraryScope: 'QMS' })
  assert.ok(text.includes('QMS 文件库，项目属性不适用')); assert.ok(!text.includes('目标市场CE'))
})
test('long multiline change descriptions remain complete', async () => {
  const description = '第一行说明\n' + '变更证据'.repeat(200)
  assert.ok((await render({ description })).includes(description))
})
test('additional market and transfer details follow the nine primary facts', async () => {
  const text = await render({ attributes: { targetMarkets: ['CE', 'OTHER'], otherMarket: '其他目标市场', documentTransfer: 'Y', transferTo: '接收部门' } })
  assert.ok(text.includes('目标市场CE 欧盟、其他'))
  assert.ok(text.includes('文件转移是'))
  assert.ok(text.includes('其他市场说明其他目标市场'))
  assert.ok(text.includes('转移至接收部门'))
  assert.ok(text.indexOf('变更说明1') < text.indexOf('其他市场说明'))
})
