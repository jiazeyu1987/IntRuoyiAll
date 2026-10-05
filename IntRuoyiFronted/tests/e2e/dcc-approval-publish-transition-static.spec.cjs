const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
const start = source.indexOf('const actionDialogSubmitFlowText = computed(')
const end = source.indexOf('const getStageRouteSnapshot =', start)
assert.ok(start >= 0 && end > start, '审批提交后流转说明必须存在')
const expression = source.slice(start, end).replace('const actionDialogSubmitFlowText =', '')

const flowText = (mode, stage) => vm.runInNewContext(expression, {
  computed: (getter) => getter(), actionDialog: { mode }, currentStageLabel: { value: stage }
})

for (const stage of ['审核会签', '文控审核']) {
  test(`${stage}应分别说明受控、生效、下发和作废流程`, () => {
    const message = flowText('approve', stage)
    assert.match(message, /上传、升版：会签→批准→培训（如需）→文控审核→受控→下发/)
    assert.match(message, /按预设生效日期生效，新版生效时旧版自动作废/)
    assert.match(message, /作废：会签→批准，批准通过即完成作废并结束流程/)
    assert.doesNotMatch(message, /直接生效|末级文控批准后|默认目录|发布为 ACTIVE/)
  })
}
test('the actual reject wording remains unchanged', () => {
  assert.equal(flowText('reject', '文控审核'), '提交后流转：当前节点驳回，流程回到发起人或按后端路线规则处理。')
})
test('the actual signature descriptions subtree renders the two distinct flows as text', () => {
  const { descriptor } = parse(source)
  const find = node => {
    if (node.type === 1 && node.tag === 'el-descriptions-item' && node.props.some(p => p.type === 6 && p.name === 'label' && p.value?.content === '提交后流转')) return node
    for (const child of node.children || []) { const found = find(child); if (found) return found }
  }
  const node = find(descriptor.template.ast); assert.ok(node)
  const compiled = compileScript(parse('<template>'+node.loc.source+'</template><script setup>const actionDialogSubmitFlowText=__text;</script>').descriptor, { id: 'signature-flow-copy', inlineTemplate: true })
  const c = { exports: {}, require: () => vue, __text: flowText('approve', '文控审核') }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, c)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}), insert: (n,p) => p.children.push(n), remove() {}, setText: (n,t) => {n.text=t}, setElementText: (n,t) => {n.text=t}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(c.exports.default); app.component('el-descriptions-item', { setup: (_p,ctx) => () => vue.h('section', ctx.slots.default?.()) })
  const root = { children: [] }; app.mount(root)
  const text = node => (node.text || '') + (node.children || []).map(text).join('')
  try { const shown=text(root); assert.ok(shown.includes('按预设生效日期生效')); assert.ok(shown.includes('作废：会签→批准')); assert.equal(shown.includes('直接生效'),false) }
  finally { app.unmount() }
})
