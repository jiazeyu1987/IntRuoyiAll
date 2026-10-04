const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const file = 'src/views/dcc/controlled-file/project-attributes/ApplicationEvidencePanel.vue'
const attributes = market => ({ targetMarkets: [market], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' })
const evidence = (id, round) => ({ controlledFileId: id, versionNo: 'B/1', applicationType: 'REVISION', bpmRound: round,
  attributeRound: 2, recorded: true, unavailableReason: null, actualAttributes: attributes('CE'), defaultSource: attributes('NMPA'), signatures: [] })
async function mount(loader) {
  assert.ok(fs.existsSync(file), 'readonly formal evidence panel must exist')
  const { descriptor } = parse(fs.readFileSync(file, 'utf8'))
  const source = compileScript(descriptor, { id: 'evidence-panel' }).content
  const context = { exports: {}, Error, require: name => {
    if (name === 'vue') return vue
    if (name.endsWith('applicationRead')) return { getControlledFileApplicationEvidence: loader }
    if (name.endsWith('ProjectAttributesFields.vue')) return { default: { render: () => null } }
    throw new Error('Unexpected dependency: ' + name)
  }}
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, context)
  const panel = context.exports.default
  panel.render = () => vue.h('section')
  const props = vue.reactive({ fileId: '10', applicationType: 'REVISION', bpmRound: 'round-1' })
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }),
    createComment: text => ({ text }), insert: (child, parent) => parent.children.push(child), remove() {}, setText() {},
    setElementText() {}, parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const app = renderer.createApp({ render: () => vue.h(panel, props) })
  const root = app.mount({ children: [] })
  await vue.nextTick(); await Promise.resolve()
  return { app, props, state: root.$.subTree.component.setupState }
}
test('readonly panel retains the same round actual and its separate default source', async () => {
  const mounted = await mount(async (id, _type, round) => evidence(id, round))
  assert.deepEqual(JSON.parse(JSON.stringify(mounted.state.evidence.actualAttributes)), attributes('CE'))
  assert.deepEqual(JSON.parse(JSON.stringify(mounted.state.evidence.defaultSource)), attributes('NMPA'))
  assert.equal(mounted.state.error, '')
  mounted.app.unmount()
})
test('an older version response cannot overwrite a newly selected round', async () => {
  let finish
  const mounted = await mount((id, _type, round) => round === 'round-1'
    ? new Promise(resolve => { finish = () => resolve(evidence(id, round)) }) : Promise.resolve(evidence(id, round)))
  mounted.props.fileId = '20'; mounted.props.bpmRound = 'round-2'
  await vue.nextTick(); await Promise.resolve()
  finish(); await Promise.resolve(); await vue.nextTick()
  assert.equal(mounted.state.evidence.controlledFileId, '20'); assert.equal(mounted.state.evidence.bpmRound, 'round-2')
  mounted.app.unmount()
})
test('missing history remains explicit and never loads current project defaults', async () => {
  const mounted = await mount(async () => ({ ...evidence('10', 'round-1'), recorded: false,
    unavailableReason: 'NOT_RECORDED', actualAttributes: null, defaultSource: null }))
  assert.equal(mounted.state.evidence.recorded, false); assert.equal(mounted.state.evidence.actualAttributes, null)
  mounted.app.unmount()
})
test('read rejection is visible and clears preceding evidence', async () => {
  const mounted = await mount(async () => { throw new Error('没有本轮正文读取权限') })
  assert.match(mounted.state.error, /本轮正文读取权限/); assert.equal(mounted.state.evidence, undefined)
  mounted.app.unmount()
})
