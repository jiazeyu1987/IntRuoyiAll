const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc'), path = require('node:path')
const root = path.resolve(__dirname, '../..'), prefix = root + '/src/views/dcc/controlled-file/'
const load = file => { const c = { exports: {}, Error, Date, Intl }; vm.runInNewContext(ts.transpileModule(fs.readFileSync(prefix + file, 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, c); return c.exports }
const facts = load('workflow/effective-facts.ts'), versions = load('shared/version-display.ts')
test('official epoch datetime displays in Shanghai while fractional signed string remains exact and invalid dates reject', () => {
  assert.equal(facts.formatDelayedEffectiveFact(Date.UTC(2026, 9, 15, 4, 30)), '2026-10-15 12:30:00')
  assert.equal(facts.formatDelayedEffectiveFact('2026-10-15 12:30:00.123456'), '2026-10-15 12:30:00.123456')
  assert.equal(facts.formatDelayedEffectiveFact(null), undefined)
  for(const invalid of [NaN, '2026-02-30 12:00:00', '2025-02-29 12:00:00', '2026-10-15 24:00:00', 'not-recorded'])
    assert.throws(() => facts.formatDelayedEffectiveFact(invalid))
})
function mount(file) {
  const descriptor = parse(fs.readFileSync(prefix + 'shared/ControlledFileBasicInfoPanel.vue', 'utf8')).descriptor
  const compiled = compileScript(descriptor, { id: 'delayed-basic', inlineTemplate: true })
  const c = { exports: {}, Error, require: id => {
    if (id === 'vue') return vue
    if (id === '../workflow/effective-facts') return facts
    if (id === './version-display') return versions
    if (id === '../detail/presentation') return { formatControlledFileDateTime: value => value == null ? '-' : String(value) }
    throw new Error(id)
  } }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}),
    insert: (node, parent) => parent.children.push(node), remove() {}, setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text },
    patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp({ render: () => vue.h(c.exports.default, { file }) })
  for(const name of ['el-descriptions', 'el-descriptions-item', 'el-link', 'el-button', 'Icon'])
    app.component(name, { setup: (_props, ctx) => () => vue.h('span', ctx.slots.default?.()) })
  const host = { children: [] }; app.mount(host)
  const text = node => (node.text || '') + (node.children || []).map(text).join('')
  return { app, text: () => text(host) }
}
test('actual basic info renders the selected version delayed facts and never manufactures missing signature or round', () => {
  const file = { id: '9223372036854775000', fileName: 'SOP.pdf', versionNo: 'B/1', effectiveMode: 'DELAYED',
    delayedEffectiveTime: '2026-10-16 12:00:00', delayedEffectiveConfirmedAt: '2026-10-15 12:00:00.123456', delayedEffectiveSignatureId: '700', delayedEffectiveProcessInstanceId: 'R' }
  const before = structuredClone(file), p = mount(file)
  try { assert.match(p.text(), /2026-10-16 12:00:00/); assert.match(p.text(), /2026-10-15 12:00:00\.123456/); assert.match(p.text(), /#700/); assert.match(p.text(), /R/); assert.deepEqual(file, before) }
  finally { p.app.unmount() }
  const absent = mount({ id: '20', versionNo: 'B/1', effectiveMode: 'DELAYED' })
  try { assert.match(absent.text(), /尚未确认/); assert.match(absent.text(), /尚未签名确认/); assert.match(absent.text(), /未记录/); assert.doesNotMatch(absent.text(), /#700/) }
  finally { absent.app.unmount() }
})
